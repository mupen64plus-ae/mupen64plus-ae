package paulscode.android.mupen64plusae.persistent;

import android.content.Context;

import paulscode.android.mupen64plusae.profile.Profile;

import static paulscode.android.mupen64plusae.persistent.GamePrefs.getSafeInt;

public class GLideN64Prefs {

    public static final int VERSION = 26;

    /** Enable/Disable Fast Approximate Anti-Aliasing FXAA */
    public final boolean fxaa;

    /** Bilinear filtering mode (
     * False=N64 3point
     * True=standard) */
    public final boolean bilinearMode;

    /** Remove halos around filtered textures. */
    public final boolean enableHalosRemoval;

    /** Max level of Anisotropic Filtering, 0 for off */
    public final int maxAnisotropy;

    /** Enable color noise emulation. */
    public final boolean enableNoise;

    /** Enable LOD emulation. */
    public final boolean enableLOD;

    /** Enable hardware per-pixel lighting. */
    public final boolean enableHWLighting;

    /** Do not use shaders to emulate N64 blending modes. Works faster on slow GPU. Can cause glitches. */
    public final boolean enableLegacyBlending;

    /** Enable writing of fragment depth. Some mobile GPUs do not support it, thus it made optional. Leave enabled. */
    public final boolean enableFragmentDepthWrite;

    /** Make texrect coordinates continuous to avoid black lines between them
     * 0=Off
     * 1=Auto
     * 2=Force */
    public final int correctTexrectCoords;

    /** Render 2D texrects in native resolution to fix misalignment between parts of 2D image */
    public final boolean enableNativeResTexrects;

    /** Render backgrounds mode (HLE only). (0=One piece (fast), 1=Stripped (precise)) */
    public final int backgroundMode;

    /** Enable frame and|or depth buffer emulation. */
    public final boolean enableFBEmulation;

    /** Swap frame buffers (
     * 0=On VI update call
     * 1=On VI origin change
     * 2=On buffer update) */
    public final int bufferSwapMode;

    /** Enable color buffer copy to RDRAM
     * 0=do not copy
     * 1=copy in sync mode
     * 2=copy in async mode (2 buffers)
     * 3=copy in async mode (3 buffers)*/
    public final int enableCopyColorToRDRAM;

    /** Copy auxiliary buffers to RDRAM */
    public final boolean enableCopyAuxiliaryToRDRAM;

    /** Enable depth buffer copy to RDRAM
     * 0=do not copy
     * 1=copy from video memory
     * 2=use software render */
    public final int enableCopyDepthToRDRAM;

    /** Enable color buffer copy from RDRAM. */
    public final boolean enableCopyColorFromRDRAM;

    /** Enable N64 depth compare instead of OpenGL standard one. Experimental. */
    public final boolean enableN64DepthCompare;

    /** Force depth buffer clear. Hack. Needed for Eikou no Saint Andrews. */
    public final boolean forceDepthBufferClear;

    /** Frame buffer size is the factor of N64 native resolution. */
    public final int useNativeResolutionFactor;

    /** Texture filter (
     * 0=none
     * 1=Smooth filtering 1
     * 2=Smooth filtering 2
     * 3=Smooth filtering 3
     * 4=Smooth filtering 4
     * 5=Sharp filtering 1
     * 6=Sharp filtering 2). */
    public final int txFilterMode;

    /** Texture Enhancement (
     * 0=none
     * 1=store as is
     * 2=X2
     * 3=X2SAI
     * 4=HQ2X
     * 5=HQ2XS
     * 6=LQ2X
     * 7=LQ2XS
     * 8=HQ4X
     * 9=2xBRZ
     * 10=3xBRZ
     * 11=4xBRZ
     * 12=5xBRZ
     * 13=6xBRZ) */
    public final int txEnhancementMode;

    /** Deposterize texture before enhancement.. */
    public final boolean txDeposterize;

    /** Don't filter background textures. */
    public final boolean txFilterIgnoreBG;

    /** Size of filtered textures cache in megabytes. */
    public final int txCacheSize;

    /** Use high-resolution texture packs if available. */
    public final boolean txHiresEnable;

    /** Allow to use alpha channel of high-res texture fully. */
    public final boolean txHiresFullAlphaChannel;

    /** Use alternative method of paletted textures CRC calculation. */
    public final boolean txHresAltCRC;

    /** Zip textures cache. */
    public final boolean txCacheCompression;

    /** Force use 16bit texture formats for HD textures. */
    public final boolean txForce16bpp;

    /** Save texture cache to hard disk. */
    public final boolean txSaveCache;

    /** Force gamma correction. */
    public final boolean forceGammaCorrection;

    /** Gamma correction value. */
    public final float gammaCorrectionLevel;

    GLideN64Prefs(Context context, final Profile emulationProfile)
    {
        String glesVersion = AppData.getOpenGlEsVersion(context);
        bilinearMode = emulationProfile.get( "bilinearMode", "True" ).equals( "True" );
        enableHalosRemoval = emulationProfile.get( "enableHalosRemoval", "False" ).equals( "True" );
        maxAnisotropy = getSafeInt( emulationProfile, "MaxAnisotropy", 0);
        enableNoise = emulationProfile.get( "EnableNoise", "True" ).equals( "True" );
        enableLOD = emulationProfile.get( "EnableLOD", "True" ).equals( "True" );
        enableHWLighting = emulationProfile.get( "EnableHWLighting", "False" ).equals( "True" );
        correctTexrectCoords = getSafeInt( emulationProfile, "CorrectTexrectCoords", 0);
        backgroundMode = getSafeInt( emulationProfile, "BackgroundsMode", 0);;

        enableLegacyBlending = emulationProfile.get( "EnableLegacyBlending", "True" ).equals( "True" );
        enableFragmentDepthWrite = emulationProfile.get( "EnableFragmentDepthWrite", "False" ).equals( "True" );
        enableFBEmulation = emulationProfile.get( "EnableFBEmulation", "True" ).equals( "True" );
        bufferSwapMode = getSafeInt( emulationProfile, "BufferSwapMode", 2);
        enableCopyColorToRDRAM = getSafeInt( emulationProfile, "EnableCopyColorToRDRAM", 0);
        enableCopyAuxiliaryToRDRAM = emulationProfile.get( "EnableCopyAuxiliaryToRDRAM", "False" ).equals( "True" );
        enableCopyDepthToRDRAM = getSafeInt( emulationProfile, "EnableCopyDepthToRDRAM", 2 );
        enableCopyColorFromRDRAM = emulationProfile.get( "EnableCopyColorFromRDRAM", "False" ).equals( "True" );
        enableN64DepthCompare = emulationProfile.get( "EnableN64DepthCompare", "False" ).equals( "True" );
        forceDepthBufferClear = emulationProfile.get( "ForceDepthBufferClear", "False" ).equals( "True" );

        fxaa = emulationProfile.get( "FXAA", "False" ).equals( "True" );

        enableNativeResTexrects = emulationProfile.get( "EnableNativeResTexrects", "False" ).equals( "True" );
        useNativeResolutionFactor = enableNativeResTexrects ?
                0 : getSafeInt( emulationProfile, "UseNativeResolutionFactor", 0);

        txFilterMode = getSafeInt( emulationProfile, "txFilterMode", 0);
        txEnhancementMode = enableNativeResTexrects ? 0 :getSafeInt( emulationProfile, "txEnhancementMode", 0);
        txDeposterize = emulationProfile.get( "txDeposterize", "False" ).equals( "True" );
        txFilterIgnoreBG = emulationProfile.get( "txFilterIgnoreBG", "True" ).equals( "True" );
        txCacheSize = getSafeInt( emulationProfile, "txCacheSize", 128);
        txHiresEnable = emulationProfile.get( "txHiresEnable", "False" ).equals( "True" );
        txHiresFullAlphaChannel = emulationProfile.get( "txHiresFullAlphaChannel", "False" ).equals( "True" );
        txHresAltCRC = emulationProfile.get( "txHresAltCRC", "False" ).equals( "True" );
        txCacheCompression = emulationProfile.get( "txCacheCompression", "True" ).equals( "True" );
        txForce16bpp = emulationProfile.get( "txForce16bpp", "False" ).equals( "True" );
        txSaveCache = emulationProfile.get( "txSaveCache", "False" ).equals( "True" );
        forceGammaCorrection = emulationProfile.get( "ForceGammaCorrection", "False" ).equals( "True" );
        gammaCorrectionLevel = getSafeInt( emulationProfile, "GammaCorrectionLevel", 10)/10.0f;
    }
}
