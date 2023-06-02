package paulscode.android.mupen64plusae_mpn.persistent;

import android.content.Context;

import paulscode.android.mupen64plusae_mpn.profile.Profile;

import static paulscode.android.mupen64plusae_mpn.persistent.GamePrefs.getSafeInt;

public class ParallelRdpPrefs {

    /** Use fullscreen mode if True, or windowed mode if False */
    public final boolean fullscreen;

    /** Amount of rescaling (1=None, 2=2x, 4=4x, 8=8x) */
    public final int upscaling;

    /** Enable superscaling of readbacks when upsampling */
    public final boolean ssReadbacks;

    /** Enable superscaling of dithering when upsampling */
    public final boolean ssDither;

    /** Enable synchronizing RDP and CPU */
    public final boolean synchronous;

    /** Deinterlacing method. (0=Bob, 1=Weave) */
    public final int deinterlace;

    /** Amount of overscan pixels to crop */
    public final int overscanCrop;

    /** VI anti-aliasing, smooths polygon edges. */
    public final boolean viAntiAliasing;

    /** Allow VI divot filter, cleans up stray black pixels. */
    public final boolean viDivotFilter;

    /** Allow VI gamma dither */
    public final boolean viGammaDither;

    /** Allow VI bilinear scaling */
    public final boolean viBilinearScaling;

    /** Allow VI dedither */
    public final boolean viDeDither;

    /** Downsampling factor, Downscales output after VI, equivalent to SSAA. (0=disabled, 1=1/2, 2=1/4, 3=1/8") */
    public final int downscale;

    /** Use native texture LOD computation when upscaling, effectively a LOD bias */
    public final boolean nativeTextLod;

    /** Native resolution TEX_RECT. TEX_RECT primitives should generally be rendered at native resolution to avoid seams */
    public final boolean nativeResTextRect;

    ParallelRdpPrefs(Context context, final Profile emulationProfile)
    {
        fullscreen = true;
        upscaling = getSafeInt( emulationProfile, "parallelUpscaling", 1);
        ssReadbacks = emulationProfile.get( "parallelSuperscaledReads", "False" ).equals( "True" );
        ssDither = emulationProfile.get( "parallelSuperscaledDither", "True" ).equals( "True" );
        synchronous = emulationProfile.get( "parallelSynchronousRDP", "False" ).equals( "True" );
        deinterlace = getSafeInt( emulationProfile, "parallelDeinterlaceMode", 0);
        overscanCrop = getSafeInt( emulationProfile, "parallelCropOverscan", 0);
        viAntiAliasing = emulationProfile.get( "parallelVIAA", "True" ).equals( "True" );
        viDivotFilter = emulationProfile.get( "parallelDivot", "True" ).equals( "True" );
        viGammaDither = emulationProfile.get( "parallelGammaDither", "True" ).equals( "True" );
        viBilinearScaling = emulationProfile.get( "parallelVIBilerp", "True" ).equals( "True" );
        viDeDither = emulationProfile.get( "parallelVIDither", "True" ).equals( "True" );
        downscale = getSafeInt( emulationProfile, "parallelDownScale", 0);
        nativeTextLod = emulationProfile.get( "parallelNativeTextLOD", "False" ).equals( "True" );
        nativeResTextRect = emulationProfile.get( "parallelNativeTextRECT", "True" ).equals( "True" );

    }
}
