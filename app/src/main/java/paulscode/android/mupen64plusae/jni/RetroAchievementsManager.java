package paulscode.android.mupen64plusae.jni;

import android.content.Context;
import android.os.Environment;
import paulscode.android.mupen64plusae.BuildConfig;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Bridges between the rcheevos rc_client (native) and Android (Java).
 *
 * Native side calls onServerCall / onAchievementTriggered as static methods.
 * Java side calls init / loadGameData / shutdown via JNA (AeBridgeLibrary).
 */
public class RetroAchievementsManager {

    private static final String TAG = "RetroAchievements";

    public interface GameLoadListener {
        void onRaGameLoaded(String title, int total, int earned, int unsupported, String gameBadgeUrl);
        void onRaAchievementTriggered(String title, String description, int points, String badgeUrl, boolean unofficial);
        void onRaGameCompleted(String title, boolean hardcore, String badgeUrl);
        void onRaLeaderboardTracker(int type, int id, String display);
        void onRaChallengeIndicator(int type, int id, String title, String badgeUrl);
        void onRaProgressIndicator(int type, String title, String progress, String badgeUrl);
    }

    // Leaderboard tracker types
    public static final int TRACKER_SHOW   = 0;
    public static final int TRACKER_UPDATE = 1;
    public static final int TRACKER_HIDE   = 2;

    // Challenge indicator types
    public static final int CHALLENGE_SHOW = 0;
    public static final int CHALLENGE_HIDE = 1;

    // Progress indicator types
    public static final int PROGRESS_SHOW   = 0;
    public static final int PROGRESS_UPDATE = 1;
    public static final int PROGRESS_HIDE   = 2;

    private static AeBridgeLibrary sAeBridge;
    private static Context         sAppContext;
    private static Handler         sMainHandler;
    private static WeakReference<GameLoadListener> sGameLoadListener;
    private static String          sUsername;

    // File logging — debug builds only, so testers can share /sdcard/M64Plus/retroachievements_log.txt
    private static PrintWriter sLogWriter;
    private static final Object sLogLock = new Object();
    private static final SimpleDateFormat LOG_FMT =
            BuildConfig.DEBUG ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US) : null;

    private static void raLog(String msg) {
        Log.i(TAG, msg);
        if (!BuildConfig.DEBUG) return;
        synchronized (sLogLock) {
            if (sLogWriter != null) {
                sLogWriter.println(LOG_FMT.format(new Date()) + "  " + msg);
                sLogWriter.flush();
            }
        }
    }

    private static void raLogW(String msg) {
        Log.w(TAG, msg);
        if (!BuildConfig.DEBUG) return;
        synchronized (sLogLock) {
            if (sLogWriter != null) {
                sLogWriter.println(LOG_FMT.format(new Date()) + "  [W] " + msg);
                sLogWriter.flush();
            }
        }
    }

    private static void openLogFile() {
        if (!BuildConfig.DEBUG) return;
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "M64Plus");
            if (!dir.exists()) dir.mkdirs();
            File logFile = new File(dir, "retroachievements_log.txt");
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
            pw.println();
            pw.println("=== Session " + LOG_FMT.format(new Date()) + " ===");
            pw.flush();
            synchronized (sLogLock) { sLogWriter = pw; }
        } catch (Exception e) {
            Log.w(TAG, "Could not open RA log file: " + e.getMessage());
        }
    }

    private static void closeLogFile() {
        if (!BuildConfig.DEBUG) return;
        synchronized (sLogLock) {
            if (sLogWriter != null) { sLogWriter.close(); sLogWriter = null; }
        }
    }

    // Called from CoreInterface when credentials are available
    public static void init(AeBridgeLibrary bridge, Context context,
                            String username, String token) {
        sAeBridge   = bridge;
        sAppContext = context.getApplicationContext();
        sMainHandler = new Handler(Looper.getMainLooper());
        sUsername    = username;
        openLogFile();
        bridge.rcheevosInit(username, token);
        android.content.SharedPreferences prefs =
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        boolean rpEnabled       = prefs.getBoolean("retroAchievementsRichPresence", false);
        boolean unofficialEnabled = prefs.getBoolean("retroAchievementsUnofficialEnabled", false);
        bridge.rcheevosSetRichPresenceEnabled(rpEnabled ? 1 : 0);
        bridge.rcheevosSetUnofficialEnabled(unofficialEnabled ? 1 : 0);
        raLog("Initialized for user: " + username
                + ", rich presence: " + rpEnabled
                + ", unofficial: " + unofficialEnabled);
    }

    public static String getUsername() { return sUsername; }

    public static void setGameLoadListener(GameLoadListener listener) {
        sGameLoadListener = listener != null ? new WeakReference<>(listener) : null;
    }

    // Pass the raw ROM buffer so rcheevos can hash it and identify the game
    public static void loadGameData(byte[] romData) {
        if (sAeBridge == null) {
            Log.d(TAG, "loadGameData: RA not initialized (disabled or not logged in)");
            return;
        }
        Log.i(TAG, "loadGameData: sending " + romData.length + " bytes to native");
        sAeBridge.rcheevosLoadGameData(romData, romData.length);
    }

    public static void reset() {
        if (sAeBridge != null) sAeBridge.rcheevosReset();
    }

    public static void shutdown() {
        if (sAeBridge != null) {
            sAeBridge.rcheevosShutdown();
            sAeBridge = null;
        }
        closeLogFile();
    }

    // ---- Static callbacks invoked from native (ra_server_call / ra_event_handler) ----

    /**
     * Native calls this to make an HTTP request on behalf of rcheevos.
     * We do the request on a background thread, then call rcheevosServerResponse
     * with the result.
     *
     * @param url      Request URL
     * @param postData POST body, or null for GET
     * @param handle   Opaque handle to pass back to rcheevosServerResponse
     */
    public static void onServerCall(final String url, final String postData, final long handle) {
        if (sAeBridge == null) return;
        new Thread(() -> {
            int httpStatus = 0;
            String body = "";
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);
                conn.setRequestProperty("User-Agent", "mupen64plus-ae/3.0 (Android)");
                if (postData != null && !postData.isEmpty()) {
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type",
                            "application/x-www-form-urlencoded");
                    byte[] bytes = postData.getBytes(StandardCharsets.UTF_8);
                    conn.setFixedLengthStreamingMode(bytes.length);
                    conn.getOutputStream().write(bytes);
                }
                httpStatus = conn.getResponseCode();
                InputStream is = httpStatus < 400
                        ? conn.getInputStream() : conn.getErrorStream();
                if (is != null) body = new String(readAll(is), StandardCharsets.UTF_8);
                String shortUrl = url.split("\\?")[0];
                String snippet  = body.substring(0, Math.min(body.length(), 200));
                if (httpStatus >= 400 || body.contains("\"Success\":false"))
                    raLogW("HTTP " + httpStatus + " " + shortUrl + " -> " + snippet);
                else
                    Log.d(TAG, "HTTP " + httpStatus + " " + shortUrl + " -> " + snippet);
            } catch (Exception e) {
                raLogW("HTTP request failed for " + url.split("\\?")[0] + ": " + e.getMessage());
                // RC_API_SERVER_RESPONSE_RETRYABLE_CLIENT_ERROR = -2
                // Tells rcheevos this is a transient failure it should retry
                httpStatus = -2;
            }
            sAeBridge.rcheevosServerResponse(handle, httpStatus, body);
        }, "RA-HTTP").start();
    }

    /**
     * Native calls this when the game is identified and achievements are loaded.
     */
    public static void onGameLoaded(String title, int total, int earned, int unsupported, String gameBadgeUrl) {
        raLog("Game loaded: " + title + " | " + earned + "/" + total
                + " achievements (" + unsupported + " unsupported)");
        if (sMainHandler == null) return;
        sMainHandler.post(() -> {
            GameLoadListener listener = sGameLoadListener != null ? sGameLoadListener.get() : null;
            if (listener != null) {
                listener.onRaGameLoaded(title, total, earned, unsupported, gameBadgeUrl);
            }
        });
    }

    /**
     * Returns the current achievement list as a JSON string. Call only while a game is loaded.
     */
    public static String getAchievementsJson() {
        if (sAeBridge == null) return "[]";
        return sAeBridge.rcheevosGetAchievementsJson();
    }

    /**
     * Called before saving a save state — persists rcheevos runtime state alongside it.
     * companion path = savePath + ".rchv"
     */
    public static void saveProgress(String savePath) {
        if (sAeBridge == null) return;
        sAeBridge.rcheevosSaveProgress(savePath + ".rchv");
    }

    /**
     * Called after loading a save state — restores rcheevos runtime state.
     * Passes null if companion file doesn't exist so rcheevos resets its runtime.
     */
    public static void loadProgress(String savePath) {
        if (sAeBridge == null) return;
        java.io.File f = new java.io.File(savePath + ".rchv");
        sAeBridge.rcheevosLoadProgress(f.exists() ? f.getAbsolutePath() : null);
    }

    // ---- Native-to-Java event callbacks ----

    public static void onAchievementTriggered(String title, String description, int points, String badgeUrl, boolean unofficial) {
        raLog((unofficial ? "Unofficial achievement" : "Achievement") + " unlocked: " + title + " (" + points + " pts)");
        if (sMainHandler == null) return;
        sMainHandler.post(() -> {
            GameLoadListener listener = sGameLoadListener != null ? sGameLoadListener.get() : null;
            if (listener != null) {
                listener.onRaAchievementTriggered(title, description, points, badgeUrl, unofficial);
            } else if (sAppContext != null) {
                String header = unofficial ? "Unofficial Achievement Unlocked!" : "Achievement Unlocked!";
                Toast.makeText(sAppContext,
                        header + "\n" + title + " (" + points + " pts)\n" + description,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void onGameCompleted(String gameTitle, boolean hardcore, String badgeUrl) {
        raLog("Game completed (hardcore=" + hardcore + "): " + gameTitle);
        if (sMainHandler == null) return;
        sMainHandler.post(() -> {
            GameLoadListener listener = sGameLoadListener != null ? sGameLoadListener.get() : null;
            if (listener != null) {
                listener.onRaGameCompleted(gameTitle, hardcore, badgeUrl);
            } else if (sAppContext != null) {
                String verb = hardcore ? "Mastered" : "Completed";
                Toast.makeText(sAppContext,
                        verb + ": " + gameTitle + "\nAll achievements unlocked!",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void onLeaderboardTracker(int type, int id, String display) {
        if (sMainHandler == null) return;
        sMainHandler.post(() -> {
            GameLoadListener listener = sGameLoadListener != null ? sGameLoadListener.get() : null;
            if (listener != null) listener.onRaLeaderboardTracker(type, id, display);
        });
    }

    public static void onChallengeIndicator(int type, int id, String title, String badgeUrl) {
        if (sMainHandler == null) return;
        sMainHandler.post(() -> {
            GameLoadListener listener = sGameLoadListener != null ? sGameLoadListener.get() : null;
            if (listener != null) listener.onRaChallengeIndicator(type, id, title, badgeUrl);
        });
    }

    public static void onProgressIndicator(int type, String title, String progress, String badgeUrl) {
        if (sMainHandler == null) return;
        sMainHandler.post(() -> {
            GameLoadListener listener = sGameLoadListener != null ? sGameLoadListener.get() : null;
            if (listener != null) listener.onRaProgressIndicator(type, title, progress, badgeUrl);
        });
    }

    public static void onLoginSuccess(String displayName, int score) {
        raLog("Logged in as " + displayName + " (" + score + " softcore pts)");
        if (sMainHandler == null || sAppContext == null) return;
        sMainHandler.post(() ->
            Toast.makeText(sAppContext,
                    "RetroAchievements: Logged in as " + displayName + " (" + score + " softcore pts)",
                    Toast.LENGTH_SHORT).show()
        );
    }

    public static void onServerError(String api, String message) {
        raLogW("Server error [" + api + "]: " + message);
        if (sMainHandler == null || sAppContext == null) return;
        sMainHandler.post(() ->
            Toast.makeText(sAppContext,
                    "RetroAchievements error: " + message,
                    Toast.LENGTH_LONG).show()
        );
    }

    public static String getRichPresence() {
        if (sAeBridge == null) return null;
        return sAeBridge.rcheevosGetRichPresence();
    }

    public static void onLeaderboardScoreboard(String submitted, String best, int rank, int total) {
        raLog("Scoreboard: submitted=" + submitted + " best=" + best
                + " rank=" + rank + "/" + total);
        if (sMainHandler == null || sAppContext == null) return;
        sMainHandler.post(() ->
            Toast.makeText(sAppContext,
                    "Score: " + submitted + "  |  Best: " + best
                    + "\nRank #" + rank + " of " + total,
                    Toast.LENGTH_LONG).show()
        );
    }

    public static void onLeaderboardStarted(String title) {
        raLog("Leaderboard started: " + title);
        if (sMainHandler == null || sAppContext == null) return;
        sMainHandler.post(() ->
            Toast.makeText(sAppContext, "Leaderboard: " + title, Toast.LENGTH_SHORT).show()
        );
    }

    public static void onLeaderboardSubmitted(String title, String value) {
        raLog("Leaderboard submitted: " + title + " = " + value);
        if (sMainHandler == null || sAppContext == null) return;
        sMainHandler.post(() ->
            Toast.makeText(sAppContext, "Score submitted: " + value + "\n" + title,
                    Toast.LENGTH_LONG).show()
        );
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) out.write(buf, 0, n);
        return out.toByteArray();
    }
}
