package paulscode.android.mupen64plusae.persistent;

import android.content.Context;

import paulscode.android.mupen64plusae.profile.Profile;

import static paulscode.android.mupen64plusae.persistent.GamePrefs.getSafeInt;

public class AngrylionPlusPrefs {
    /** Distribute rendering between multiple processors if True */
    public final boolean parallel;

    /** Rendering Workers (0=Use all logical processors) */
    public final int numWorkers;

    /**Use busyloop while waiting for work */
    public final boolean busyLoop;

    /** VI mode (0=Filtered, 1=Unfiltered, 2=Depth, 3=Coverage) */
    public final int viMode;

    /** Scaling interpolation type (0=NN, 1=Linear) */
    public final int viInterpolation;

    /** Use anamorphic 16:9 output mode if True */
    public final boolean viWidescreen;

    /** Hide overscan area in filteded mode if True */
    public final boolean viHideOverscan;

    /** Display upscaled pixels as groups of 1x1, 2x2, 3x3, etc. if True */
    public final boolean viIntegerScaling;

    /** Compatibility mode (0=Fast 1=Moderate 2=Slow */
    public final int dpCompatibilityMode;


    AngrylionPlusPrefs(Context context, final Profile emulationProfile)
    {
        parallel = emulationProfile.get( "ParallelMode", "True" ).equals( "True" );
        numWorkers = getSafeInt( emulationProfile, "WorkerThreads", 1);
        busyLoop = emulationProfile.get( "BusyLoop", "False" ).equals( "True" );
        viMode = getSafeInt( emulationProfile, "viMode", 0);
        viInterpolation = getSafeInt( emulationProfile, "viInterpolation", 0);
        viWidescreen = false;
        viHideOverscan = emulationProfile.get( "viHideOverscan", "False" ).equals( "True" );
        viIntegerScaling = emulationProfile.get( "viIntegerScaling", "True" ).equals( "True" );
        dpCompatibilityMode = getSafeInt(emulationProfile, "DpCompat", 0);;
    }
}
