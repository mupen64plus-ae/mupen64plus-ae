package emulator.android.mupen64plusae.jni;

import com.sun.jna.Library;

/**
 * Library used to interface with the audio plugin
 */

@SuppressWarnings({"unused", "RedundantSuppression"})
public interface AndroidAudioLibrary extends Library {
    // Notify that emulation has been paused
    void pauseEmulator();

    // Notify that emulation has been resumed
    void resumeEmulator();

    // Change volume
    void setVolume(int volume);

    // Change volume
    void usingNetplay(int volume);
}