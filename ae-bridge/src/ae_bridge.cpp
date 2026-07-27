#include <GL/EGLLoader.h>
#include "ae_bridge.h"
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <string.h>
#include <time.h>
#include <math.h>
#include <mutex>
#include <thread>
#include <vector>
#include <unistd.h>
#include <dlfcn.h>
#include <m64p_frontend.h>
#include <m64p_debugger.h>
#include <rc_client.h>
#include <rc_consoles.h>

extern "C" void ra_set_rich_presence_enabled(rc_client_t* client, int enabled);

#define RA_TAG "RetroAchievements"
#define RALOGI(...) __android_log_print(ANDROID_LOG_INFO,  RA_TAG, __VA_ARGS__)
#define RALOGW(...) __android_log_print(ANDROID_LOG_WARN,  RA_TAG, __VA_ARGS__)
#define RALOGE(...) __android_log_print(ANDROID_LOG_ERROR, RA_TAG, __VA_ARGS__)

EGLDisplay display = EGL_NO_DISPLAY;
EGLConfig config;
EGLContext context = EGL_NO_CONTEXT;
EGLSurface surface = EGL_NO_SURFACE;
ANativeWindow* native_window = nullptr;
std::mutex nativeWindowAccess;
int isGLES2 = 1;
bool new_surface = false;
int FPSRecalcPeriod = 0;
uint32_t frameCount = 0;
int64_t oldTime;
int vsync = 0;
int oldVsync = 1;
bool isPaused = false;
static bool detachOnQuitCore = false;

m64p_dynlib_handle CoreHandle = NULL;
ptr_CoreOverrideVidExt  CoreOverrideVidExt = NULL;
ptr_DebugMemGetPointer  DebugMemGetPointer = NULL;

void (*fpsCounterCallback)(int);

// ---------- RetroAchievements ----------
// All functions below use plain C calling convention (no JNIEnv/jclass params)
// so they can be called via JNA from Java, matching the pattern used for
// other ae-bridge functions like overrideAeVidExtFuncs().

extern JavaVM* mJavaVM;  // defined in JNI_OnLoad below

static rc_client_t*  g_rc_client  = nullptr;
static uint8_t*      g_rdram      = nullptr;

static jclass      g_ra_class                  = nullptr;
static jmethodID   g_ra_server_call            = nullptr;
static jmethodID   g_ra_achievement_triggered  = nullptr;
static jmethodID   g_ra_game_loaded            = nullptr;
static jmethodID   g_ra_game_completed         = nullptr;
static jmethodID   g_ra_leaderboard_started    = nullptr;
static jmethodID   g_ra_leaderboard_submitted  = nullptr;
static jmethodID   g_ra_leaderboard_tracker    = nullptr;
static jmethodID   g_ra_challenge_indicator    = nullptr;
static jmethodID   g_ra_progress_indicator     = nullptr;
static jmethodID   g_ra_leaderboard_scoreboard = nullptr;
static jmethodID   g_ra_server_error           = nullptr;
static jmethodID   g_ra_login_success          = nullptr;

static std::string g_achievements_json;

static std::string g_ra_host_override;

// Restore requested before the game finished identifying; applied on game load.
static std::string g_pending_progress_path;
static bool        g_has_pending_progress = false;
static void ra_apply_progress_file(const char* path);

struct PendingServerCall {
    rc_client_server_callback_t callback;
    void* callback_data;
};

static JNIEnv* ra_get_env(bool* attached) {
    JNIEnv* env;
    *attached = false;
    if (mJavaVM->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        mJavaVM->AttachCurrentThread(&env, nullptr);
        *attached = true;
    }
    return env;
}

static void ra_log_callback(const char* message, const rc_client_t*) {
    RALOGI("%s", message);
}

static uint32_t ra_read_memory(uint32_t address, uint8_t* buffer, uint32_t num_bytes, rc_client_t*) {
    if (!g_rdram && DebugMemGetPointer)
        g_rdram = (uint8_t*)DebugMemGetPointer(M64P_DBG_PTR_RDRAM);
    if (!g_rdram) return 0;
    if (address >= 0x800000) return 0;
    uint32_t i;
    for (i = 0; i < num_bytes && (address + i) < 0x800000; i++)
        buffer[i] = g_rdram[address + i];
    return i;
}

static void ra_server_call(const rc_api_request_t* request,
                           rc_client_server_callback_t callback,
                           void* callback_data, rc_client_t*) {
    if (!g_ra_class || !g_ra_server_call) {
        rc_api_server_response_t resp = {};
        resp.http_status_code = RC_API_SERVER_RESPONSE_CLIENT_ERROR;
        callback(&resp, callback_data);
        return;
    }
    bool attached;
    JNIEnv* env = ra_get_env(&attached);
    auto* pending = new PendingServerCall{callback, callback_data};
    jstring url   = env->NewStringUTF(request->url);
    jstring body  = request->post_data ? env->NewStringUTF(request->post_data) : nullptr;
    env->CallStaticVoidMethod(g_ra_class, g_ra_server_call, url, body, (jlong)(uintptr_t)pending);
    env->DeleteLocalRef(url);
    if (body) env->DeleteLocalRef(body);
    if (attached) mJavaVM->DetachCurrentThread();
}

static void ra_event_handler(const rc_client_event_t* event, rc_client_t* client) {
    switch (event->type) {
        case RC_CLIENT_EVENT_ACHIEVEMENT_TRIGGERED: {
            if (!g_ra_class || !g_ra_achievement_triggered) break;
            bool attached; JNIEnv* env = ra_get_env(&attached);
            jstring title      = env->NewStringUTF(event->achievement->title);
            jstring desc       = env->NewStringUTF(event->achievement->description);
            jstring badgeUrl   = env->NewStringUTF(event->achievement->badge_url ? event->achievement->badge_url : "");
            jboolean unofficial = (jboolean)(event->achievement->category == RC_CLIENT_ACHIEVEMENT_CATEGORY_UNOFFICIAL);
            env->CallStaticVoidMethod(g_ra_class, g_ra_achievement_triggered,
                                      title, desc, (jint)event->achievement->points, badgeUrl, unofficial);
            env->DeleteLocalRef(title); env->DeleteLocalRef(desc); env->DeleteLocalRef(badgeUrl);
            if (attached) mJavaVM->DetachCurrentThread();
            break;
        }
        case RC_CLIENT_EVENT_GAME_COMPLETED: {
            if (!g_ra_class || !g_ra_game_completed) break;
            const rc_client_game_t* game = rc_client_get_game_info(client);
            bool attached; JNIEnv* env = ra_get_env(&attached);
            jstring title    = env->NewStringUTF(game ? game->title : "");
            jboolean hardcore = (jboolean)rc_client_get_hardcore_enabled(client);
            jstring badgeUrl = env->NewStringUTF(game && game->badge_url ? game->badge_url : "");
            env->CallStaticVoidMethod(g_ra_class, g_ra_game_completed, title, hardcore, badgeUrl);
            env->DeleteLocalRef(title); env->DeleteLocalRef(badgeUrl);
            if (attached) mJavaVM->DetachCurrentThread();
            break;
        }
        case RC_CLIENT_EVENT_LEADERBOARD_STARTED: {
            if (!g_ra_class || !g_ra_leaderboard_started || !event->leaderboard) break;
            bool attached; JNIEnv* env = ra_get_env(&attached);
            jstring title = env->NewStringUTF(event->leaderboard->title);
            env->CallStaticVoidMethod(g_ra_class, g_ra_leaderboard_started, title);
            env->DeleteLocalRef(title);
            if (attached) mJavaVM->DetachCurrentThread();
            break;
        }
        case RC_CLIENT_EVENT_LEADERBOARD_SUBMITTED: {
            if (!g_ra_class || !g_ra_leaderboard_submitted || !event->leaderboard) break;
            bool attached; JNIEnv* env = ra_get_env(&attached);
            jstring title = env->NewStringUTF(event->leaderboard->title);
            jstring value = env->NewStringUTF(event->leaderboard->tracker_value
                                               ? event->leaderboard->tracker_value : "");
            env->CallStaticVoidMethod(g_ra_class, g_ra_leaderboard_submitted, title, value);
            env->DeleteLocalRef(title); env->DeleteLocalRef(value);
            if (attached) mJavaVM->DetachCurrentThread();
            break;
        }
        case RC_CLIENT_EVENT_LEADERBOARD_SCOREBOARD: {
            if (!g_ra_class || !g_ra_leaderboard_scoreboard || !event->leaderboard_scoreboard) break;
            bool attached; JNIEnv* env = ra_get_env(&attached);
            const rc_client_leaderboard_scoreboard_t* sb = event->leaderboard_scoreboard;
            jstring submitted = env->NewStringUTF(sb->submitted_score);
            jstring best      = env->NewStringUTF(sb->best_score);
            env->CallStaticVoidMethod(g_ra_class, g_ra_leaderboard_scoreboard,
                                      submitted, best,
                                      (jint)sb->new_rank, (jint)sb->num_entries);
            env->DeleteLocalRef(submitted); env->DeleteLocalRef(best);
            if (attached) mJavaVM->DetachCurrentThread();
            break;
        }
        case RC_CLIENT_EVENT_RESET:
            rc_client_reset(client);
            break;
        case RC_CLIENT_EVENT_SERVER_ERROR:
            if (event->server_error) {
                RALOGE("Server error [%s]: %s", event->server_error->api,
                       event->server_error->error_message);
                if (g_ra_class && g_ra_server_error) {
                    bool attached; JNIEnv* env = ra_get_env(&attached);
                    jstring api = env->NewStringUTF(event->server_error->api ? event->server_error->api : "");
                    jstring msg = env->NewStringUTF(event->server_error->error_message ? event->server_error->error_message : "unknown error");
                    env->CallStaticVoidMethod(g_ra_class, g_ra_server_error, api, msg);
                    env->DeleteLocalRef(api); env->DeleteLocalRef(msg);
                    if (attached) mJavaVM->DetachCurrentThread();
                }
            }
            break;
        case RC_CLIENT_EVENT_LEADERBOARD_FAILED:
            if (event->leaderboard)
                RALOGI("Leaderboard failed: %s", event->leaderboard->title);
            break;
        case RC_CLIENT_EVENT_LEADERBOARD_TRACKER_SHOW:
        case RC_CLIENT_EVENT_LEADERBOARD_TRACKER_UPDATE:
        case RC_CLIENT_EVENT_LEADERBOARD_TRACKER_HIDE: {
            if (!g_ra_class || !g_ra_leaderboard_tracker || !event->leaderboard_tracker) break;
            bool attached; JNIEnv* env = ra_get_env(&attached);
            // type: 0=show, 1=update, 2=hide  (matches Java constants)
            jint type = (event->type == RC_CLIENT_EVENT_LEADERBOARD_TRACKER_SHOW)  ? 0 :
                        (event->type == RC_CLIENT_EVENT_LEADERBOARD_TRACKER_UPDATE) ? 1 : 2;
            jint id   = (jint)event->leaderboard_tracker->id;
            jstring display = env->NewStringUTF(event->leaderboard_tracker->display);
            env->CallStaticVoidMethod(g_ra_class, g_ra_leaderboard_tracker, type, id, display);
            env->DeleteLocalRef(display);
            if (attached) mJavaVM->DetachCurrentThread();
            break;
        }
        case RC_CLIENT_EVENT_ACHIEVEMENT_CHALLENGE_INDICATOR_SHOW:
        case RC_CLIENT_EVENT_ACHIEVEMENT_CHALLENGE_INDICATOR_HIDE: {
            if (!g_ra_class || !g_ra_challenge_indicator || !event->achievement) break;
            bool attached; JNIEnv* env = ra_get_env(&attached);
            jint type     = (event->type == RC_CLIENT_EVENT_ACHIEVEMENT_CHALLENGE_INDICATOR_SHOW) ? 0 : 1;
            jint id       = (jint)event->achievement->id;
            jstring title    = env->NewStringUTF(event->achievement->title);
            // Pass badge_url for SHOW so the indicator can display the achievement icon
            const char* burl = (type == 0) ? event->achievement->badge_url : nullptr;
            jstring badgeUrl = env->NewStringUTF(burl ? burl : "");
            env->CallStaticVoidMethod(g_ra_class, g_ra_challenge_indicator, type, id, title, badgeUrl);
            env->DeleteLocalRef(title); env->DeleteLocalRef(badgeUrl);
            if (attached) mJavaVM->DetachCurrentThread();
            break;
        }
        case RC_CLIENT_EVENT_ACHIEVEMENT_PROGRESS_INDICATOR_SHOW:
        case RC_CLIENT_EVENT_ACHIEVEMENT_PROGRESS_INDICATOR_UPDATE: {
            if (!g_ra_class || !g_ra_progress_indicator || !event->achievement) break;
            bool attached; JNIEnv* env = ra_get_env(&attached);
            jint type     = (event->type == RC_CLIENT_EVENT_ACHIEVEMENT_PROGRESS_INDICATOR_SHOW) ? 0 : 1;
            jstring title    = env->NewStringUTF(event->achievement->title ? event->achievement->title : "");
            jstring prog     = env->NewStringUTF(event->achievement->measured_progress ? event->achievement->measured_progress : "");
            // Use badge_locked_url so the indicator shows the locked (in-progress) icon
            const char* burl = event->achievement->badge_locked_url;
            jstring badgeUrl = env->NewStringUTF(burl ? burl : "");
            env->CallStaticVoidMethod(g_ra_class, g_ra_progress_indicator, type, title, prog, badgeUrl);
            env->DeleteLocalRef(title); env->DeleteLocalRef(prog); env->DeleteLocalRef(badgeUrl);
            if (attached) mJavaVM->DetachCurrentThread();
            break;
        }
        case RC_CLIENT_EVENT_ACHIEVEMENT_PROGRESS_INDICATOR_HIDE: {
            if (!g_ra_class || !g_ra_progress_indicator) break;
            bool attached; JNIEnv* env = ra_get_env(&attached);
            jstring empty = env->NewStringUTF("");
            env->CallStaticVoidMethod(g_ra_class, g_ra_progress_indicator, (jint)2, empty, empty, empty);
            env->DeleteLocalRef(empty);
            if (attached) mJavaVM->DetachCurrentThread();
            break;
        }
        case RC_CLIENT_EVENT_SUBSET_COMPLETED: {
            if (!g_ra_class || !g_ra_game_completed) break;
            bool attached; JNIEnv* env = ra_get_env(&attached);
            jstring title    = env->NewStringUTF(event->subset ? event->subset->title : "");
            jboolean hardcore = (jboolean)rc_client_get_hardcore_enabled(client);
            jstring badgeUrl = env->NewStringUTF(
                event->subset && event->subset->badge_url ? event->subset->badge_url : "");
            env->CallStaticVoidMethod(g_ra_class, g_ra_game_completed, title, hardcore, badgeUrl);
            env->DeleteLocalRef(title); env->DeleteLocalRef(badgeUrl);
            if (attached) mJavaVM->DetachCurrentThread();
            break;
        }
        case RC_CLIENT_EVENT_DISCONNECTED:
            RALOGW("Disconnected — pending unlocks will be retried");
            break;
        case RC_CLIENT_EVENT_RECONNECTED:
            RALOGI("Reconnected — pending unlocks delivered");
            break;
        default:
            break;
    }
}

static void ra_login_callback(int result, const char* error_message,
                              rc_client_t* client, void*) {
    if (result != RC_OK) {
        RALOGW("Login failed: %s", error_message ? error_message : "unknown");
        return;
    }
    const rc_client_user_t* user = rc_client_get_user_info(client);
    RALOGI("Logged in as %s (%u softcore pts)", user ? user->display_name : "?",
           user ? user->score_softcore : 0);
    if (!g_ra_class || !g_ra_login_success || !user) return;
    bool attached; JNIEnv* env = ra_get_env(&attached);
    jstring name = env->NewStringUTF(user->display_name);
    env->CallStaticVoidMethod(g_ra_class, g_ra_login_success, name, (jint)user->score_softcore);
    env->DeleteLocalRef(name);
    if (attached) mJavaVM->DetachCurrentThread();
}

// JNA-callable: create client and log in with a saved token
extern "C" DECLSPEC void rcheevosInit(const char* username, const char* token) {
    if (g_rc_client) rc_client_destroy(g_rc_client);
    g_rc_client = rc_client_create(ra_read_memory, ra_server_call);
    rc_client_enable_logging(g_rc_client, RC_CLIENT_LOG_LEVEL_INFO, ra_log_callback);
    rc_client_set_hardcore_enabled(g_rc_client, 0);
    if (!g_ra_host_override.empty()) {
        rc_client_set_host(g_rc_client, g_ra_host_override.c_str());
        RALOGI("Host override: %s", g_ra_host_override.c_str());
    }
    rc_client_set_event_handler(g_rc_client, ra_event_handler);
    // Rich presence disabled by default until explicitly enabled via rcheevosSetRichPresenceEnabled
    ra_set_rich_presence_enabled(g_rc_client, 0);
    RALOGI("Logging in as %s", username);
    rc_client_begin_login_with_token(g_rc_client, username, token, ra_login_callback, nullptr);
}

extern "C" DECLSPEC void rcheevosSetHost(const char* host) {
    if (host && host[0] != '\0') {
        g_ra_host_override = host;
    } else {
        g_ra_host_override.clear();
    }
    if (g_rc_client) {
        rc_client_set_host(g_rc_client, g_ra_host_override.empty() ? nullptr
                                                                   : g_ra_host_override.c_str());
    }
    RALOGI("Host override set to '%s'", g_ra_host_override.empty() ? "(default)"
                                                                   : g_ra_host_override.c_str());
}

// JNA-callable: enable or disable rich presence reporting to the RA server
extern "C" DECLSPEC void rcheevosSetRichPresenceEnabled(int enabled) {
    ra_set_rich_presence_enabled(g_rc_client, enabled);
    RALOGI("Rich presence %s", enabled ? "enabled" : "disabled");
}

// JNA-callable: enable or disable loading of unofficial achievements
extern "C" DECLSPEC void rcheevosSetUnofficialEnabled(int enabled) {
    if (!g_rc_client) return;
    rc_client_set_unofficial_enabled(g_rc_client, enabled);
    RALOGI("Unofficial achievements %s", enabled ? "enabled" : "disabled");
}

static void ra_game_loaded_callback(int result, const char* error_message,
                                    rc_client_t* client, void*) {
    if (result != RC_OK) {
        RALOGW("Game load failed: %s", error_message ? error_message : "unknown");
        return;
    }
    if (!g_ra_class || !g_ra_game_loaded) return;

    const rc_client_game_t* game = rc_client_get_game_info(client);
    if (!game) return;

    // Apply a restore that arrived before the game was ready. Done before the
    // achievement list is built below so the counts sent to Java reflect the
    // restored runtime rather than a fresh one.
    if (g_has_pending_progress) {
        const std::string pending = g_pending_progress_path;
        g_has_pending_progress = false;
        g_pending_progress_path.clear();
        ra_apply_progress_file(pending.empty() ? nullptr : pending.c_str());
    }

    auto esc = [](const char* s) -> std::string {
        std::string out;
        if (!s) return out;
        for (; *s; ++s) {
            if (*s == '"') out += "\\\"";
            else if (*s == '\\') out += "\\\\";
            else out += *s;
        }
        return out;
    };

    // Create the achievement list first so we can read the primary subset ID.
    // game->id is the website game ID; the primary subset has its own internal ID
    // that only appears in bucket->subset_id — it is not the same value.
    rc_client_achievement_list_t* list = rc_client_create_achievement_list(
        client,
        RC_CLIENT_ACHIEVEMENT_CATEGORY_CORE_AND_UNOFFICIAL,
        RC_CLIENT_ACHIEVEMENT_LIST_GROUPING_LOCK_STATE);

    uint32_t primary_id = (list && list->num_buckets > 0) ? list->buckets[0].subset_id : 0;

    // Summary API filters warning achievements (ID >= 101000001), giving the correct counts.
    rc_client_user_game_summary_t summary = {};
    if (primary_id != 0)
        rc_client_get_user_subset_summary(client, primary_id, &summary);
    else
        rc_client_get_user_game_summary(client, &summary);
    int total       = (int)summary.num_core_achievements;
    int earned      = (int)summary.num_unlocked_achievements;
    int unsupported = (int)summary.num_unsupported_achievements;

    // Build JSON for the primary bucket, skipping warning achievements so the
    // list count matches the summary counts above.
    g_achievements_json.clear();
    g_achievements_json += '[';

    if (list) {
        bool first = true;
        for (uint32_t b = 0; b < list->num_buckets; b++) {
            const rc_client_achievement_bucket_t* bucket = &list->buckets[b];
            if (bucket->subset_id != primary_id) continue;
            for (uint32_t a = 0; a < bucket->num_achievements; a++) {
                const rc_client_achievement_t* ach = bucket->achievements[a];
                // RC_CLIENT_ACHIEVEMENT_WARNING_ID = 101000001 (rc_client.c, not public header).
                // The summary API already excludes these; skip them here to keep counts consistent.
                if (ach->id >= 101000001u) continue;
                bool unlocked = (ach->state == RC_CLIENT_ACHIEVEMENT_STATE_UNLOCKED);
                if (!first) g_achievements_json += ',';
                first = false;
                char buf[32];
                snprintf(buf, sizeof(buf), "%u", ach->points);
                const char* badge = unlocked ? ach->badge_url : ach->badge_locked_url;
                g_achievements_json += "{\"t\":\"" + esc(ach->title) + "\""
                    + ",\"d\":\"" + esc(ach->description) + "\""
                    + ",\"p\":" + buf
                    + ",\"b\":\"" + esc(badge ? badge : "") + "\""
                    + ",\"u\":" + (unlocked ? "1" : "0") + "}";
            }
        }
        rc_client_destroy_achievement_list(list);
    }
    g_achievements_json += ']';

    bool attached;
    JNIEnv* env = ra_get_env(&attached);
    jstring title        = env->NewStringUTF(game->title);
    jstring gameBadgeUrl = env->NewStringUTF(game->badge_url ? game->badge_url : "");
    env->CallStaticVoidMethod(g_ra_class, g_ra_game_loaded,
                              title, (jint)total, (jint)earned,
                              (jint)unsupported, gameBadgeUrl);
    env->DeleteLocalRef(title); env->DeleteLocalRef(gameBadgeUrl);
    if (attached) mJavaVM->DetachCurrentThread();
}

// JNA-callable: identify ROM from its raw bytes and load achievements
extern "C" DECLSPEC void rcheevosLoadGameData(const uint8_t* data, uint32_t size) {
    if (!g_rc_client) return;
    RALOGI("Loading game data (%u bytes)", size);
    rc_client_begin_identify_and_load_game(g_rc_client,
        RC_CONSOLE_NINTENDO_64, nullptr, data, size, ra_game_loaded_callback, nullptr);
}

// JNA-callable: returns the achievement list as a JSON array (valid until next call)
extern "C" DECLSPEC const char* rcheevosGetAchievementsJson() {
    return g_achievements_json.empty() ? "[]" : g_achievements_json.c_str();
}

// JNA-callable: Java delivers the HTTP response for a pending server call
extern "C" DECLSPEC void rcheevosServerResponse(jlong handle, jint httpStatus, const char* body) {
    auto* pending = (PendingServerCall*)(uintptr_t)handle;
    rc_api_server_response_t resp;
    resp.body             = body ? body : "";
    resp.body_length      = body ? (uint32_t)strlen(body) : 0;
    resp.http_status_code = (int)httpStatus;
    pending->callback(&resp, pending->callback_data);
    delete pending;
}

// JNA-callable: serialize rcheevos runtime state alongside a save state file
extern "C" DECLSPEC void rcheevosSaveProgress(const char* path) {
    if (!g_rc_client || !path) return;
    size_t sz = rc_client_progress_size(g_rc_client);
    if (sz == 0) return;
    std::vector<uint8_t> buf(sz);
    if (rc_client_serialize_progress_sized(g_rc_client, buf.data(), sz) != RC_OK) {
        RALOGW("rcheevosSaveProgress: serialize failed");
        return;
    }
    FILE* f = fopen(path, "wb");
    if (!f) { RALOGW("rcheevosSaveProgress: cannot open %s", path); return; }
    uint32_t sz32 = (uint32_t)sz;
    fwrite("RCHV", 1, 4, f);
    fwrite(&sz32, 1, sizeof(sz32), f);
    fwrite(buf.data(), 1, sz, f);
    fclose(f);
    RALOGI("rcheevosSaveProgress: wrote %zu bytes to %s", sz, path);
}

// Reads a companion file and applies it to the runtime. The caller must have
// already confirmed a game is loaded -- rc_client returns RC_NO_GAME_LOADED
// otherwise, silently discarding the restore.
static void ra_apply_progress_file(const char* path) {
    if (!g_rc_client) return;
    if (!path) { rc_client_deserialize_progress_sized(g_rc_client, nullptr, 0); return; }
    FILE* f = fopen(path, "rb");
    if (!f) {
        RALOGW("rcheevosLoadProgress: no companion file %s, resetting", path);
        rc_client_deserialize_progress_sized(g_rc_client, nullptr, 0);
        return;
    }
    char marker[4] = {};
    uint32_t stored_sz = 0;
    if (fread(marker, 1, 4, f) != 4 || memcmp(marker, "RCHV", 4) != 0) {
        RALOGW("rcheevosLoadProgress: no RCHV marker in %s, resetting", path);
        fclose(f);
        rc_client_deserialize_progress_sized(g_rc_client, nullptr, 0);
        return;
    }
    if (fread(&stored_sz, 1, sizeof(stored_sz), f) != sizeof(stored_sz) || stored_sz == 0) {
        RALOGW("rcheevosLoadProgress: bad header in %s, resetting", path);
        fclose(f);
        rc_client_deserialize_progress_sized(g_rc_client, nullptr, 0);
        return;
    }
    std::vector<uint8_t> buf(stored_sz);
    size_t n = fread(buf.data(), 1, stored_sz, f);
    fclose(f);
    if (n != stored_sz) {
        RALOGW("rcheevosLoadProgress: truncated data in %s, resetting", path);
        rc_client_deserialize_progress_sized(g_rc_client, nullptr, 0);
        return;
    }
    if (rc_client_deserialize_progress_sized(g_rc_client, buf.data(), stored_sz) != RC_OK)
        RALOGW("rcheevosLoadProgress: deserialize failed for %s", path);
    else
        RALOGI("rcheevosLoadProgress: restored %u bytes from %s", stored_sz, path);
}

// JNA-callable: restore rcheevos runtime state from a save state companion file
extern "C" DECLSPEC void rcheevosLoadProgress(const char* path) {
    if (!g_rc_client) return;

    // Game identification is asynchronous (it needs a server round trip), so on
    // startup this is called while the client still has no game and rc_client
    // would reject the restore with RC_NO_GAME_LOADED. Hold the path and let
    // ra_game_loaded_callback apply it once the game is actually available.
    if (!rc_client_is_game_loaded(g_rc_client)) {
        g_pending_progress_path = path ? path : "";
        g_has_pending_progress  = true;
        RALOGI("rcheevosLoadProgress: game not loaded yet, deferring restore of %s",
               path ? path : "(reset)");
        return;
    }

    ra_apply_progress_file(path);
}

// JNA-callable: called when the emulator resets (user-initiated restart)
extern "C" DECLSPEC void rcheevosReset() {
    if (g_rc_client) rc_client_reset(g_rc_client);
}

// JNA-callable: returns the current rich presence message, or null if none
static char g_rp_buf[256];
extern "C" DECLSPEC const char* rcheevosGetRichPresence() {
    if (!g_rc_client || !rc_client_has_rich_presence(g_rc_client)) return nullptr;
    size_t n = rc_client_get_rich_presence_message(g_rc_client, g_rp_buf, sizeof(g_rp_buf));
    return n > 0 ? g_rp_buf : nullptr;
}

// JNA-callable: clean up on emulator shutdown
extern "C" DECLSPEC void rcheevosShutdown() {
    if (g_rc_client) { rc_client_destroy(g_rc_client); g_rc_client = nullptr; }
    g_rdram = nullptr;
    g_has_pending_progress = false;
    g_pending_progress_path.clear();
}

// ----------------------------------------


EGLint const defaultAttributeList[] = {
        EGL_BUFFER_SIZE, 0,
        EGL_BLUE_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_RED_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 16,
        EGL_SAMPLE_BUFFERS, 0,
        EGL_SAMPLES, 0,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_NONE
};

EGLint const defaultContextAttribs[] = {
        EGL_CONTEXT_MAJOR_VERSION_KHR, 2,
        EGL_CONTEXT_MINOR_VERSION_KHR, 0,
        EGL_NONE
};

EGLint const defaultGlEsContextAttribs[] = {
		EGL_CONTEXT_CLIENT_VERSION, 2,
		EGL_NONE
};

EGLint const defaultWindowAttribs[] = {
        EGL_RENDER_BUFFER, EGL_BACK_BUFFER,
        EGL_NONE
};

EGLint attribList[sizeof(defaultAttributeList) / sizeof(EGLint)];
EGLint windowAttribList[sizeof(defaultWindowAttribs) / sizeof(EGLint)];
EGLint contextAttribs[sizeof(defaultContextAttribs) / sizeof(EGLint)];

size_t FindIndex( const EGLint a[], size_t size, int value )
{
    size_t index = 0;

    while ( index < (size/sizeof(EGLint)) && a[index] != value ) ++index;

    return ( index == (size/sizeof(EGLint)) ? -1 : index );
}


JavaVM* mJavaVM;

// Library init
extern jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	mJavaVM = vm;
	JNIEnv* env;
	vm->GetEnv((void**)&env, JNI_VERSION_1_6);
	jclass clazz = env->FindClass(
        "paulscode/android/mupen64plusae/jni/RetroAchievementsManager");
	if (clazz) {
		g_ra_class = (jclass)env->NewGlobalRef(clazz);
		g_ra_server_call = env->GetStaticMethodID(clazz, "onServerCall",
            "(Ljava/lang/String;Ljava/lang/String;J)V");
		g_ra_achievement_triggered = env->GetStaticMethodID(clazz,
            "onAchievementTriggered", "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Z)V");
		g_ra_game_loaded = env->GetStaticMethodID(clazz,
            "onGameLoaded", "(Ljava/lang/String;IIILjava/lang/String;)V");
		g_ra_game_completed = env->GetStaticMethodID(clazz,
            "onGameCompleted", "(Ljava/lang/String;ZLjava/lang/String;)V");
		g_ra_leaderboard_started = env->GetStaticMethodID(clazz,
            "onLeaderboardStarted", "(Ljava/lang/String;)V");
		g_ra_leaderboard_submitted = env->GetStaticMethodID(clazz,
            "onLeaderboardSubmitted", "(Ljava/lang/String;Ljava/lang/String;)V");
		g_ra_leaderboard_tracker = env->GetStaticMethodID(clazz,
            "onLeaderboardTracker", "(IILjava/lang/String;)V");
		g_ra_challenge_indicator = env->GetStaticMethodID(clazz,
            "onChallengeIndicator", "(IILjava/lang/String;Ljava/lang/String;)V");
		g_ra_progress_indicator = env->GetStaticMethodID(clazz,
            "onProgressIndicator", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
		g_ra_leaderboard_scoreboard = env->GetStaticMethodID(clazz,
            "onLeaderboardScoreboard", "(Ljava/lang/String;Ljava/lang/String;II)V");
		g_ra_server_error = env->GetStaticMethodID(clazz,
            "onServerError", "(Ljava/lang/String;Ljava/lang/String;)V");
		g_ra_login_success = env->GetStaticMethodID(clazz,
            "onLoginSuccess", "(Ljava/lang/String;I)V");
		env->DeleteLocalRef(clazz);
	}
	return JNI_VERSION_1_6;
}

extern DECLSPEC m64p_error VidExtFuncInit()
{
	std::unique_lock<std::mutex> guard(nativeWindowAccess);

    frameCount = 0;
    surface = EGL_NO_SURFACE;
    context = EGL_NO_CONTEXT;
    display = EGL_NO_DISPLAY;
    memcpy(attribList, defaultAttributeList, sizeof(defaultAttributeList));
    memcpy(windowAttribList, defaultWindowAttribs, sizeof(defaultWindowAttribs));
    memcpy(contextAttribs, defaultContextAttribs, sizeof(defaultContextAttribs));

    if ((display = eglGetDisplay(EGL_DEFAULT_DISPLAY)) == EGL_NO_DISPLAY) {
        LOGE("eglGetDisplay() returned error %d", eglGetError());
        return M64ERR_INVALID_STATE;
    }
    if (!eglInitialize(display, 0, 0)) {
        LOGE("eglInitialize() returned error %d", eglGetError());
        return M64ERR_INVALID_STATE;
    }

    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncListModes(m64p_2d_size *SizeArray, int *NumSizes)
{
    return M64ERR_SUCCESS;
}


extern DECLSPEC m64p_error VidExtFuncListRates(m64p_2d_size, int *, int *)
{
	return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncSetMode(int Width, int Height, int BitsPerPixel, int ScreenMode, int Flags)
{
	{
		std::unique_lock<std::mutex> guard(nativeWindowAccess);

		EGLint num_config;
		if (!eglChooseConfig(display, attribList, &config, 1, &num_config)) {
			LOGE("eglChooseConfig() returned error %d", eglGetError());
			return M64ERR_INVALID_STATE;
		}
		if (num_config == 0) {
			//Try to fallback to GLES context
			eglBindAPI(EGL_OPENGL_ES_API);
			attribList[FindIndex(attribList, sizeof(attribList), EGL_RENDERABLE_TYPE) + 1] = EGL_OPENGL_ES2_BIT;
			if (!eglChooseConfig(display, attribList, &config, 1, &num_config)) {
				LOGE("eglChooseConfig() returned error %d", eglGetError());
				return M64ERR_INVALID_STATE;
			}
		}

		if (!(context = eglCreateContext(display, config, EGL_NO_CONTEXT, contextAttribs))) {
			//If creating the context failed, just try to create a GLES2/3 context
			//This is useful because GLideN64 requests an OpenGL 3.3 core context.
			if (!(context = eglCreateContext(display, config, EGL_NO_CONTEXT, defaultGlEsContextAttribs))) {
				LOGE("eglCreateContext() returned error %d", eglGetError());
				return M64ERR_INVALID_STATE;
			}
		}
	}

	// Wait for the native window to be set before continuing
	while (native_window == nullptr) {
		usleep(1000);
	}

	{
		std::unique_lock<std::mutex> guard(nativeWindowAccess);
		if(new_surface && native_window != nullptr)
		{
			LOGI("VidExtFuncSetMode: Initializing surface");

			if (!(surface = eglCreateWindowSurface(display, config, (EGLNativeWindowType)native_window, windowAttribList)))
			{
				LOGE("eglCreateWindowSurface() returned error %d", eglGetError());
				return M64ERR_INVALID_STATE;
			}

			if (!eglMakeCurrent(display, surface, surface, context))
			{
				LOGE("eglMakeCurrent() returned error %d", eglGetError());
				return M64ERR_INVALID_STATE;
			}

			new_surface = false;
		} else {
			LOGE("VidExtFuncSetMode called before surface has been set");
			return M64ERR_INVALID_STATE;
		}

		EGLLoader::loadEGLFunctions();

		const char * strVersion = reinterpret_cast<const char*>(g_glGetString(GL_VERSION));
		isGLES2 = strstr(strVersion, "OpenGL ES 2") != nullptr;
	}

    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncSetModeWithRate(int, int, int, int, int, int)
{
	return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncSetCaption(const char *Title)
{
    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncToggleFS()
{
    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncResizeWindow(int Width, int Height)
{
    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_function VidExtFuncGLGetProc(const char* Proc)
{
    return reinterpret_cast<m64p_function>(eglGetProcAddress(Proc));
}

extern DECLSPEC m64p_error VidExtFuncGLSetAttr(m64p_GLattr Attr, int Value)
{
	std::unique_lock<std::mutex> guard(nativeWindowAccess);

    int my_index;
    switch (Attr) {
        case M64P_GL_DOUBLEBUFFER:
            my_index = FindIndex(windowAttribList, sizeof(windowAttribList), EGL_RENDER_BUFFER);
            if (Value == 0)
                windowAttribList[my_index + 1] = EGL_SINGLE_BUFFER;
            else
                windowAttribList[my_index + 1] = EGL_BACK_BUFFER;
            break;
        case M64P_GL_BUFFER_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_BUFFER_SIZE);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_DEPTH_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_DEPTH_SIZE);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_RED_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_RED_SIZE);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_GREEN_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_GREEN_SIZE);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_BLUE_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_BLUE_SIZE);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_ALPHA_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_ALPHA_SIZE);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_SWAP_CONTROL:
            break;
        case M64P_GL_MULTISAMPLEBUFFERS:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_SAMPLE_BUFFERS);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_MULTISAMPLESAMPLES:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_SAMPLES);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_CONTEXT_MAJOR_VERSION:
            my_index = FindIndex(contextAttribs, sizeof(contextAttribs), EGL_CONTEXT_MAJOR_VERSION_KHR);
            contextAttribs[my_index + 1]= Value;
            break;
        case M64P_GL_CONTEXT_MINOR_VERSION:
            my_index = FindIndex(contextAttribs, sizeof(contextAttribs), EGL_CONTEXT_MINOR_VERSION_KHR);
            contextAttribs[my_index + 1]= Value;
            break;
        case M64P_GL_CONTEXT_PROFILE_MASK:
            switch (Value) {
                case M64P_GL_CONTEXT_PROFILE_ES:
                    eglBindAPI(EGL_OPENGL_ES_API);
                    my_index = FindIndex(attribList, sizeof(attribList), EGL_RENDERABLE_TYPE);
                    //attribList[my_index + 1] = EGL_OPENGL_ES2_BIT;
                    attribList[my_index + 1] = EGL_OPENGL_ES3_BIT;
                    break;
                case M64P_GL_CONTEXT_PROFILE_CORE:
                case M64P_GL_CONTEXT_PROFILE_COMPATIBILITY:
                    if (eglBindAPI(EGL_OPENGL_API)) {
                        my_index = FindIndex(attribList, sizeof(attribList), EGL_RENDERABLE_TYPE);
                        attribList[my_index + 1] = EGL_OPENGL_BIT;
                    }
                    break;
            }
            break;
    }
    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncGLGetAttr(m64p_GLattr Attr, int *pValue)
{
	std::unique_lock<std::mutex> guard(nativeWindowAccess);

    int value;
    switch (Attr) {
        case M64P_GL_DOUBLEBUFFER:
            eglQueryContext(display, context, EGL_RENDER_BUFFER, &value);
            if (value == EGL_SINGLE_BUFFER)
                *pValue = 0;
            else
                *pValue = 1;
            break;
        case M64P_GL_BUFFER_SIZE:
            eglGetConfigAttrib(display, config, EGL_BUFFER_SIZE, pValue);
            break;
        case M64P_GL_DEPTH_SIZE:
            eglGetConfigAttrib(display, config, EGL_DEPTH_SIZE, pValue);
            break;
        case M64P_GL_RED_SIZE:
            eglGetConfigAttrib(display, config, EGL_RED_SIZE, pValue);
            break;
        case M64P_GL_GREEN_SIZE:
            eglGetConfigAttrib(display, config, EGL_GREEN_SIZE, pValue);
            break;
        case M64P_GL_BLUE_SIZE:
            eglGetConfigAttrib(display, config, EGL_BLUE_SIZE, pValue);
            break;
        case M64P_GL_ALPHA_SIZE:
            eglGetConfigAttrib(display, config, EGL_ALPHA_SIZE, pValue);
            break;
        case M64P_GL_SWAP_CONTROL:
            break;
        case M64P_GL_MULTISAMPLEBUFFERS:
            eglGetConfigAttrib(display, config, EGL_SAMPLE_BUFFERS, pValue);
            break;
        case M64P_GL_MULTISAMPLESAMPLES:
            eglGetConfigAttrib(display, config, EGL_SAMPLES, pValue);
            break;
        case M64P_GL_CONTEXT_MAJOR_VERSION:
            if (!isGLES2)
                g_glGetIntegerv(GL_MAJOR_VERSION, pValue);
            else
                *pValue = 2;
            break;
        case M64P_GL_CONTEXT_MINOR_VERSION:
            if (!isGLES2)
                g_glGetIntegerv(GL_MINOR_VERSION, pValue);
            else
                *pValue = 0;
            break;
        case M64P_GL_CONTEXT_PROFILE_MASK:
            eglQueryContext(display, context, EGL_CONTEXT_CLIENT_TYPE, &value);
            if (value != EGL_OPENGL_ES_API) {
                g_glGetIntegerv(GL_CONTEXT_PROFILE_MASK, &value);
                if (value == GL_CONTEXT_CORE_PROFILE_BIT)
                    *pValue = M64P_GL_CONTEXT_PROFILE_CORE;
                else
                    *pValue = M64P_GL_CONTEXT_PROFILE_COMPATIBILITY;
            } else
                *pValue = M64P_GL_CONTEXT_PROFILE_ES;
            break;
    }
    return M64ERR_SUCCESS;
}

extern "C" DECLSPEC void registerFpsCounterCallback(void (*callback)(int))
{
	fpsCounterCallback = callback;
}

void FPSCounter(int fps)
{
	JNIEnv *env;
	if (mJavaVM->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
		mJavaVM->AttachCurrentThread(&env, nullptr);
		detachOnQuitCore = true;
		return;
	}

	fpsCounterCallback(fps);
}

extern DECLSPEC m64p_error VidExtFuncGLSwapBuf()
{
	std::unique_lock<std::mutex> guard(nativeWindowAccess);

	if(native_window != nullptr)
	{
		if (new_surface) {

			new_surface = false;

			LOGI("VidExtFuncGLSwapBuf: New surface has been detected");

			if (!(surface = eglCreateWindowSurface(display, config, (EGLNativeWindowType)native_window, windowAttribList))) {
				LOGE("eglCreateWindowSurface() returned error %d", eglGetError());
				return M64ERR_INVALID_STATE;
			}

			// This first eglMakeCurrent is needed for badly behaving GPU drivers
			if (!eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)) {
				LOGE("eglMakeCurrent() returned error %d", eglGetError());
				return M64ERR_INVALID_STATE;
			}

			if (!eglMakeCurrent(display, surface, surface, context)) {
				LOGE("eglMakeCurrent() returned error %d", eglGetError());
				return M64ERR_INVALID_STATE;
			}

			eglSwapInterval(display, vsync);
		}

		if(surface != EGL_NO_SURFACE)
		{
			if (vsync != oldVsync) {
				eglSwapInterval(display, vsync);
				oldVsync = vsync;
			}

			if (!isPaused) {
				eglSwapBuffers(display, surface);
			}
		}
	}

	if (g_rc_client) {
		if (isPaused)
			rc_client_idle(g_rc_client);
		else
			rc_client_do_frame(g_rc_client);
	}

	if (FPSRecalcPeriod > 0) {
		frameCount++;
		if (frameCount >= FPSRecalcPeriod) {
			struct timespec spec;
			clock_gettime(CLOCK_MONOTONIC, &spec);
			int64_t currentTime = (int64_t) spec.tv_sec * 1000000000LL + spec.tv_nsec;
			float fFPS = ((float) frameCount / (float) (currentTime - oldTime)) * 1000000000.0f;
			FPSCounter(lround(fFPS));
			frameCount = 0;
			oldTime = currentTime;
		}
	}

    return M64ERR_SUCCESS;
}

extern "C" DECLSPEC void setNativeWindow(JNIEnv* env, jobject native_surface)
{
	std::unique_lock<std::mutex> guard(nativeWindowAccess);

	LOGI("setNativeWindow: New surface has been set");

	native_window = ANativeWindow_fromSurface(env, native_surface);
	new_surface = true;
}

extern "C" DECLSPEC void unsetNativeWindow(void)
{
	std::unique_lock<std::mutex> guard(nativeWindowAccess);

	LOGI("unsetNativeWindow: Native window has been unset");

	if(native_window != nullptr)
	{
		ANativeWindow_release(native_window);
		native_window = nullptr;

		//sleep for 50 ms to allow all queued swap buffer calls to finish
		usleep(50000);
	}
}

extern "C" DECLSPEC void emuDestroySurface(void)
{
	LOGI("emuDestroySurface: Deleting surface");

	std::unique_lock<std::mutex> guard(nativeWindowAccess);

	if(native_window != nullptr)
	{
		ANativeWindow_release(native_window);
		native_window = nullptr;

		//sleep for 50 ms to allow all queued swap buffer calls to finish
		usleep(50000);
	}

    if (display != EGL_NO_DISPLAY && surface != EGL_NO_SURFACE)
        eglDestroySurface(display, surface);
    surface = EGL_NO_SURFACE;

	native_window = nullptr;
}

extern DECLSPEC m64p_error VidExtFuncQuit()
{
	std::unique_lock<std::mutex> guard(nativeWindowAccess);

	LOGI("VidExtFuncQuit");

	eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);

	if (surface != EGL_NO_SURFACE) {
		eglDestroySurface(display, surface);
		surface = EGL_NO_SURFACE;
	}

	if(native_window != nullptr) {
		ANativeWindow_release(native_window);
		native_window = nullptr;
	}

	if (context != EGL_NO_CONTEXT) {
		eglDestroyContext(display, context);
		context = EGL_NO_CONTEXT;
	}

	if (display != EGL_NO_DISPLAY) {
		eglTerminate(display);
		display = EGL_NO_DISPLAY;
	}

	if (detachOnQuitCore) {
        mJavaVM->DetachCurrentThread();
	}

	return M64ERR_SUCCESS;
}

extern DECLSPEC uint32_t VidExtFuncGLGetDefaultFramebuffer(void)
{
    return 0;
}

extern "C" DECLSPEC void FPSEnabled(int recalc)
{
    FPSRecalcPeriod = recalc;
}

extern DECLSPEC void vsyncEnabled(int enabled)
{
    //vsync = enabled;
}

extern DECLSPEC void pauseEmulator()
{
    isPaused = true;
}

extern DECLSPEC void resumeEmulator()
{
    isPaused = false;
}

extern "C" DECLSPEC void overrideAeVidExtFuncs(void)
{
	CoreHandle = dlopen("libmupen64plus-core.so", RTLD_NOW);
	CoreOverrideVidExt    = (ptr_CoreOverrideVidExt)   dlsym(CoreHandle, "CoreOverrideVidExt");
	DebugMemGetPointer    = (ptr_DebugMemGetPointer)    dlsym(CoreHandle, "DebugMemGetPointer");
	CoreOverrideVidExt(&vidExtFunctions);
}

void checkLibraryError(const char* message)
{
	const char* error = dlerror();
	if (error)
		LOGE("%s: %s", message, error);
}

extern "C" DECLSPEC void* loadLibrary(const char* libName)
{
	char path[256];
	sprintf(path, "lib%s.so", libName);
	void* handle = dlopen(path, RTLD_NOW);
	if (!handle)
		LOGE("Failed to load lib%s.so", libName);
	checkLibraryError(libName);

	return handle;
}

extern "C" DECLSPEC int unloadLibrary(void* handle, const char* libName)
{
	if (!handle)
		return 0;

	int code = dlclose(handle);
	if (code)
		LOGE("Failed to unload lib%s.so", libName);
	checkLibraryError(libName);
	return code;
}
