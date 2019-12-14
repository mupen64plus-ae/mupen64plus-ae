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
}