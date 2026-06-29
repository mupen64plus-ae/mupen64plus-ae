package paulscode.android.mupen64plusae.jni;

import com.sun.jna.Callback;
import com.sun.jna.JNIEnv;
import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * Library used to interface with AE Vid Ext implementation
 */

@SuppressWarnings("unused")
public interface AeBridgeLibrary extends Library {

    interface FpsCounterCallback extends Callback {
        void invoke(int fps);
    }
    // Enable or disble VSYNC
    void vsyncEnabled(int enabled);

    // Notify that emulation has been paused
    void pauseEmulator();

    // Notify that emulation has been resumed
    void resumeEmulator();

    // Set FPS recalculation period
    void FPSEnabled(int recalc);

    // Set Native window
    void setNativeWindow(JNIEnv env, Object arg);

    // Unset native window
    void unsetNativeWindow();

    // Destroy surface
    void emuDestroySurface();

    void overrideAeVidExtFuncs();

    void registerFpsCounterCallback(FpsCounterCallback fpsCounterCallback);

    // Load a library using dlopen
    Pointer loadLibrary(String libName);

    // Unload library using dlclose
    int unloadLibrary(Pointer handle, String libName);

    // RetroAchievements: create rc_client and log in with a saved API token
    void rcheevosInit(String username, String token);

    // RetroAchievements: identify ROM from raw bytes and load achievement set
    void rcheevosLoadGameData(byte[] data, int size);

    // RetroAchievements: deliver HTTP response for a pending server call
    void rcheevosServerResponse(long handle, int httpStatus, String body);

    // RetroAchievements: destroy rc_client on emulator shutdown
    void rcheevosShutdown();

    // RetroAchievements: returns JSON array of achievements (valid until next call)
    String rcheevosGetAchievementsJson();

    // RetroAchievements: notify rcheevos of a user-initiated emulator reset
    void rcheevosReset();

    // RetroAchievements: serialize rcheevos runtime state to a companion file
    void rcheevosSaveProgress(String path);

    // RetroAchievements: restore rcheevos runtime state from a companion file (null to reset)
    void rcheevosLoadProgress(String path);

    // RetroAchievements: returns current rich presence message, or null if none/unsupported
    String rcheevosGetRichPresence();

    // RetroAchievements: enable (1) or disable (0) rich presence reporting to the server
    void rcheevosSetRichPresenceEnabled(int enabled);

    // RetroAchievements: enable (1) or disable (0) unofficial achievement loading
    void rcheevosSetUnofficialEnabled(int enabled);
}