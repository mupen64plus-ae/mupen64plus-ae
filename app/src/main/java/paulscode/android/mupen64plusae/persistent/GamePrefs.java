package paulscode.android.mupen64plusae.persistent;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import paulscode.android.mupen64plusae.input.map.PlayerMap;
import paulscode.android.mupen64plusae.preference.MultiSelectListPreference;
import paulscode.android.mupen64plusae.profile.ControllerProfile;
import paulscode.android.mupen64plusae.profile.Profile;
import paulscode.android.mupen64plusae.util.Plugin;
import paulscode.android.mupen64plusae.util.SafeMethods;

public class GamePrefs
{
    /** The name of the game-specific {@link SharedPreferences} object.*/
    public final String sharedPrefsName;

    /** The parent directory containing all game-specific data files. */
    public final String gameDataDir;

    /** The subdirectory containing SRAM/EEPROM data (in-game saves). */
    public final String sramDataDir;

    /** The subdirectory containing auto save files. */
    public final String autoSaveDir;

    /** The subdirectory containing slot save files. */
    public final String slotSaveDir;

    /** The subdirectory containing manual save files. */
    public final String userSaveDir;

    /** Game header name */
    public final String gameHeaderName;

    /** Game good name */
    public final String gameGoodName;
    
    /** Legacy save file name */
    public final String legacySaveFileName;
    
    /** The subdirectory containing user screenshots. */
    public final String screenshotDir;

    /** The subdirectory returned from the core's ConfigGetUserConfigPath() method. Location of core config file. */
    public final String coreUserConfigDir;

    /** The path of the Mupen64Plus base configuration file. */
    public final String mupen64plus_cfg;

    /** The emulation profile. */
    public final Profile emulationProfile;

    /** The touchscreen profile. */
    public final Profile touchscreenProfile;

    /** The input profile for Player 1. */
    public final ControllerProfile controllerProfile1;

    /** The input profile for Player 2. */
    public final ControllerProfile controllerProfile2;

    /** The input profile for Player 3. */
    public final ControllerProfile controllerProfile3;

    /** The input profile for Player 4. */
    public final ControllerProfile controllerProfile4;

    /** The player map for multi-player gaming. */
    public final PlayerMap playerMap;

    /** True if the cheats category should be shown in the menu. */
    public final boolean isCheatOptionsShown;

    /** The selected R4300 emulator. */
    public final String r4300Emulator;

    /** The selected R4300 emulator. */
    public final boolean disableExpansionPak;

    /** The selected RSP Plugin. */
    public final String rspPluginPath;

    /** True if we want the RSP to be in HLE video mode, false if LLE */
    public final boolean rspHleVideo;

    /** The selected video plug-in. */
    public final Plugin videoPlugin;

    /** True if gln64 video plug-in is enabled. */
    public final boolean isGln64Enabled;

    /** True if rice video plug-in is enabled. */
    public final boolean isRiceEnabled;

    /** True if glide64 video plug-in is enabled. */
    public final boolean isGlide64Enabled;

    /** True if gliden64 video plug-in is enabled. */
    public final boolean isGliden64Enabled;

    /** True if angrylion video plug-in is enabled. */
    public final boolean isAngrylionEnabled;

    /** The maximum frameskip in the gln64 library. */
    public final int gln64MaxFrameskip;

    /** True if auto-frameskip is enabled in the gln64 library. */
    public final boolean isGln64AutoFrameskipEnabled;

    /** True if fog is enabled in the gln64 library. */
    public final boolean isGln64FogEnabled;

    /** True if SaI texture filtering is enabled in the gln64 library. */
    public final boolean isGln64SaiEnabled;

    /** True if force screen clear is enabled in the gln64 library. */
    public final boolean isGln64ScreenClearEnabled;

    /** True if alpha test is enabled in the gln64 library. */
    public final boolean isGln64AlphaTestEnabled;

    /** True if depth coordinates hack is enabled in the gln64 library. */
    public final boolean isGln64HackDepthEnabled;

    /** True if auto-frameskip is enabled in the rice library. */
    public final boolean isRiceAutoFrameskipEnabled;

    /** True if fast texture loading is enabled in the rice library. */
    public final boolean isRiceFastTextureLoadingEnabled;

    /** True if force texture filter is enabled in the rice library. */
    public final boolean isRiceForceTextureFilterEnabled;

    /** The screen update setting to use in rice */
    public final String riceScreenUpdateType;

    /** The texture enhancement algorithm to be used in the rice library */
    public final String riceTextureEnhancement;

    /** True if hi-resolution textures are enabled in the rice library. */
    public final boolean isRiceHiResTexturesEnabled;

    /** True if fog is enabled in the rice library. */
    public final boolean isRiceFogEnabled;

    /** True if VI Overlay is enabled in Angrylion */
    public final boolean angrylionVIOverlayEnabled;

    /** The maximum frameskip in the glide64 library. */
    public final int glide64MaxFrameskip;

    /** True if auto-frameskip is enabled in the glide64 library. */
    public final boolean isGlide64AutoFrameskipEnabled;

    /** Crop resulted image (
     * 0=disable,
     * 1=auto crop,
     * 2=user defined crop) */
    public final int gliden64CropMode;

    /** Enable/Disable MultiSampling (
     * 0=off,
     * 2,4,8,16=quality) */
    public final int gliden64MultiSampling;

    /** Bilinear filtering mode (
     * 0=N64 3point
     * 1=standard) */
    public final int gliden64BilinearMode;

    /** Max level of Anisotropic Filtering, 0 for off */
    public final int gliden64MaxAnisotropy;

    /** Size of texture cache in megabytes. Good value is VRAM*3/4 */
    public final int gliden64CacheSize;

    /** Enable color noise emulation. */
    public final boolean gliden64EnableNoise;

    /** Enable LOD emulation. */
    public final boolean gliden64EnableLOD;

    /** Enable hardware per-pixel lighting. */
    public final boolean gliden64EnableHWLighting;

    /** Use persistent storage for compiled shaders. */
    public final boolean gliden64EnableShadersStorage;

    /** Make texrect coordinates continuous to avoid black lines between them
     * 0=Off
     * 1=Auto
     * 2=Force */
    public final int gliden64CorrectTexrectCoords;

    /** Render 2D texrects in native resolution to fix misalignment between parts of 2D image */
    public final boolean gliden64EnableNativeResTexrects;

    /** Do not use shaders to emulate N64 blending modes. Works faster on slow GPU. Can cause glitches. */
    public final boolean gliden64EnableLegacyBlending;

    /** Enable writing of fragment depth. Some mobile GPUs do not support it, thus it made optional. Leave enabled. */
    public final boolean gliden64EnableFragmentDepthWrite;

    /** Enable frame and|or depth buffer emulation. */
    public final boolean gliden64EnableFBEmulation;

    /** Swap frame buffers (
     * 0=On VI update call
     * 1=On VI origin change
     * 2=On buffer update) */
    public final int gliden64BufferSwapMode;

    /** Enable color buffer copy to RDRAM
     * 0=do not copy
     * 1=copy in sync mode
     * 2=copy in async mode */
    public final int gliden64EnableCopyColorToRDRAM;

    /** Copy auxiliary buffers to RDRAM */
    public final boolean gliden64EnableCopyAuxiliaryToRDRAM;

    /** Enable depth buffer copy to RDRAM
     * 0=do not copy
     * 1=copy from video memory
     * 2=use software render */
    public final int gliden64EnableCopyDepthToRDRAM;

    /** Enable color buffer copy from RDRAM. */
    public final boolean gliden64EnableCopyColorFromRDRAM;

    /** Enable N64 depth compare instead of OpenGL standard one. Experimental. */
    public final boolean gliden64EnableN64DepthCompare;

    /** Frame buffer size is the factor of N64 native resolution. */
    public final int gliden64UseNativeResolutionFactor;

    /** Disable buffers read/write with FBInfo. Use for games, which do not work with FBInfo.. */
    public final boolean gliden64DisableFBInfo;

    /** Read color buffer by 4kb chunks (strict follow to FBRead specification). */
    public final boolean gliden64FBInfoReadColorChunk;

    /** Read depth buffer by 4kb chunks (strict follow to FBRead specification). */
    public final boolean gliden64FBInfoReadDepthChunk;

    /** Texture filter (
     * 0=none
     * 1=Smooth filtering 1
     * 2=Smooth filtering 2
     * 3=Smooth filtering 3
     * 4=Smooth filtering 4
     * 5=Sharp filtering 1
     * 6=Sharp filtering 2). */
    public final int gliden64TxFilterMode;

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
    public final int gliden64TxEnhancementMode;

    /** Deposterize texture before enhancement.. */
    public final boolean gliden64TxDeposterize;

    /** Don't filter background textures. */
    public final boolean gliden64TxFilterIgnoreBG;

    /** Size of filtered textures cache in megabytes. */
    public final int gliden64TxCacheSize;

    /** Use high-resolution texture packs if available. */
    public final boolean gliden64TxHiresEnable;

    /** Allow to use alpha channel of high-res texture fully. */
    public final boolean gliden64TxHiresFullAlphaChannel;

    /** Use alternative method of paletted textures CRC calculation. */
    public final boolean gliden64TxHresAltCRC;

    /** Zip textures cache. */
    public final boolean gliden64TxCacheCompression;

    /** Force use 16bit texture formats for HD textures. */
    public final boolean gliden64TxForce16bpp;

    /** Save texture cache to hard disk. */
    public final boolean gliden64TxSaveCache;

    /** Enable bloom filter */
    public final boolean gliden64EnableBloom;

    /** Brightness threshold level for bloom. Values [2, 6] */
    public final int gliden64BloomThresholdLevel;

    /** Bloom blend mode (
     * 0=Strong
     * 1=Mild
     * 2=Light) */
    public final int gliden64BloomBlendMode;

    /** Blur radius. Values [2, 10] */
    public final int gliden64BlurAmount;

    /** Blur strength. Values [10, 100] */
    public final int gliden64BlurStrength;

    /** Force gamma correction. */
    public final boolean gliden64ForceGammaCorrection;

    /** Gamma correction value. */
    public final float gliden64GammaCorrectionLevel;

    /** True if the touchscreen is enabled. */
    public final boolean isTouchscreenEnabled;

    /** The set of NOT auto-holdable button commands. */
    public final Set<Integer> touchscreenNotAutoHoldables;

    /** Invert the touch controller X axis */
    public final boolean invertTouchXAxis;

    /** Invert the touch controller Y axis */
    public final boolean invertTouchYAxis;

    /** True if the touchscreen overlay is hidden. */
    public final boolean isTouchscreenHidden;

    /** The directory of the selected touchscreen skin. */
    public final String touchscreenSkin;

    /** True to activate sensor on game start */
    public final boolean sensorActivateOnStart;

    /** The sensor values used for X axis emulation */
    public final String sensorAxisX;

    /** The phone's orientation angle for X axis value=0 */
    public final float sensorAngleX;

    /** The sensor's X axis sensitivity (%), may be negative to invert axes */
    public final int sensorSensitivityX;

    /** The sensor values used for Y axis emulation */
    public final String sensorAxisY;

    /** The phone's orientation angle for Y axis value=0 */
    public final float sensorAngleY;

    /** The sensor's Y axis sensitivity (%), may be negative to invert axes */
    public final int sensorSensitivityY;

    /** True if Player 1's controller is enabled. */
    public final boolean isControllerEnabled1;

    /** True if Player 2's controller is enabled. */
    public final boolean isControllerEnabled2;

    /** True if Player 3's controller is enabled. */
    public final boolean isControllerEnabled3;

    /** True if Player 4's controller is enabled. */
    public final boolean isControllerEnabled4;

    /** True if any type of AbstractController is enabled for Player 1. */
    public final boolean isPlugged1;

    /** True if any type of AbstractController is enabled for Player 2. */
    public final boolean isPlugged2;

    /** True if any type of AbstractController is enabled for Player 3. */
    public final boolean isPlugged3;

    /** True if any type of AbstractController is enabled for Player 4. */
    public final boolean isPlugged4;

    /** True if a single peripheral device can control multiple players concurrently. */
    public final boolean isControllerShared;

    /** True if the touchscreen joystick is hidden when sensor is enabled. */
    public final boolean isAnalogHiddenWhenSensor;

    /** The width of the OpenGL rendering context, in pixels. */
    public final int videoRenderWidth;

    /** The height of the OpenGL rendering context, in pixels. */
    public final int videoRenderHeight;

    /** The zoom value applied to the viewing surface, in percent. */
    public final int videoSurfaceZoom;

    /** The width of the viewing surface, in pixels. */
    public final int videoSurfaceWidth;

    /** The height of the viewing surface, in pixels. */
    public final int videoSurfaceHeight;

    /** If display mode is stretch*/
    public final boolean mStretch;

    /** Core CountPerOp setting */
    public final int countPerOp;

    /** Core CountPerScanline setting */
    public final int countPerScanline;

    /** The method used for auto holding buttons. */
    public final int touchscreenAutoHold;

    /** Game CRC */
    public final String gameCrc;

    private final SharedPreferences mPreferences;

    /** Profile keys */
    public static final String DISPLAY_RESOLUTION = "displayResolution";
    public static final String EMULATION_PROFILE = "emulationProfile";
    public static final String TOUCHSCREEN_PROFILE = "touchscreenProfile";
    public static final String CONTROLLER_PROFILE1 = "controllerProfile1";
    public static final String CONTROLLER_PROFILE2 = "controllerProfile2";
    public static final String CONTROLLER_PROFILE3 = "controllerProfile3";
    public static final String CONTROLLER_PROFILE4 = "controllerProfile4";
    public static final String PLAYER_MAP = "playerMapV2";
    public static final String PLAY_SHOW_CHEATS = "playShowCheats";

    /**
     * Directories and file names
     */
    public static final String SRAM_DATA_DIR = "SramData";
    public static final String AUTO_SAVES_DIR = "AutoSaves";
    public static final String SLOT_SAVES_DIR = "SlotSaves";
    public static final String USER_SAVES_DIR = "UserSaves";
    public static final String SCREENSHOTS_DIR = "Screenshots";
    public static final String CORE_CONFIG_DIR = "CoreConfig";
    public static final String MUPEN_CONFIG_FILE = "mupen64plus.cfg";

    public GamePrefs( Context context, String romMd5, String crc, String headerName, String goodName,
        String countrySymbol, AppData appData, GlobalPrefs globalPrefs, String legacySave)
    {
        gameHeaderName = headerName;
        gameGoodName = goodName;
        legacySaveFileName = legacySave;
        gameCrc = crc;
        
        sharedPrefsName = romMd5.replace(' ', '_' ) + "_preferences";
        mPreferences = context.getSharedPreferences( sharedPrefsName, Context.MODE_PRIVATE );

        // Game-specific data
        gameDataDir = getGameDataPath( romMd5, headerName, countrySymbol, appData);
        autoSaveDir = gameDataDir + "/" + AUTO_SAVES_DIR;
        userSaveDir = gameDataDir + "/" + USER_SAVES_DIR;
        coreUserConfigDir = gameDataDir + "/" + CORE_CONFIG_DIR;
        mupen64plus_cfg = coreUserConfigDir + "/" + MUPEN_CONFIG_FILE;

        if(globalPrefs.useFlatGameDataPath)
        {
            sramDataDir = appData.gameDataDir;
            slotSaveDir = appData.gameDataDir;
            screenshotDir = appData.gameDataDir;
        }
        else
        {
            sramDataDir = gameDataDir + "/" + SRAM_DATA_DIR;
            slotSaveDir = gameDataDir + "/" + SLOT_SAVES_DIR;
            screenshotDir = gameDataDir + "/" + SCREENSHOTS_DIR;
        }

        // Emulation profile
        emulationProfile = loadProfile( mPreferences, EMULATION_PROFILE,
                globalPrefs.getEmulationProfileDefault(), GlobalPrefs.DEFAULT_EMULATION_PROFILE_DEFAULT,
                globalPrefs.GetEmulationProfilesConfig(), appData.GetEmulationProfilesConfig() );

        Log.i("GamePrefs", "emulation profile found: " + emulationProfile.getName());

        // Touchscreen profile
        if(globalPrefs.isBigScreenMode)
        {
            touchscreenProfile =  new Profile( true, appData.GetTouchscreenProfilesConfig().get( "None" ) );
        }
        else
        {
            touchscreenProfile = loadProfile( mPreferences, TOUCHSCREEN_PROFILE,
                globalPrefs.getTouchscreenProfileDefault(), GlobalPrefs.DEFAULT_TOUCHSCREEN_PROFILE_DEFAULT,
                globalPrefs.GetTouchscreenProfilesConfig(), appData.GetTouchscreenProfilesConfig() );
        }

        // Controller profiles
        controllerProfile1 = loadControllerProfile( mPreferences, CONTROLLER_PROFILE1,
                globalPrefs.getControllerProfileDefault(1), GlobalPrefs.DEFAULT_CONTROLLER_PROFILE_DEFAULT,
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile2 = loadControllerProfile( mPreferences, CONTROLLER_PROFILE2,
                globalPrefs.getControllerProfileDefault(2), "",
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile3 = loadControllerProfile( mPreferences, CONTROLLER_PROFILE3,
                globalPrefs.getControllerProfileDefault(3), "",
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile4 = loadControllerProfile( mPreferences, CONTROLLER_PROFILE4,
                globalPrefs.getControllerProfileDefault(4), "",
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );

        if(controllerProfile1 != null) {
            Log.i("GamePrefs", "controler 1 profile found: " + controllerProfile1.getName());
        }else  {
            Log.i("GamePrefs", "controler 1 profile NOT found");
        }
        if(controllerProfile2 != null) {
            Log.i("GamePrefs", "controler 2 profile found: " + controllerProfile2.getName());
        }else  {
            Log.i("GamePrefs", "controler 2 profile NOT found");
        }
        if(controllerProfile3 != null) {
            Log.i("GamePrefs", "controler 3 profile found: " + controllerProfile3.getName());
        }else  {
            Log.i("GamePrefs", "controler 3 profile NOT found");
        }
        if(controllerProfile4 != null) {
            Log.i("GamePrefs", "controler 4 profile found: " + controllerProfile4.getName());
        }else  {
            Log.i("GamePrefs", "controler 4 profile NOT found");
        }

        // Player map
        boolean useDefaultPlayerMapping = mPreferences.getBoolean( "useDefaultPlayerMapping", true );
        String playerMapString = mPreferences.getString( PLAYER_MAP, "" );

        if( playerMapString.isEmpty() || useDefaultPlayerMapping) {
            playerMapString = globalPrefs.getString(PLAYER_MAP, "");
            Log.i("GamePrefs", "Using default player mapping");
        }

        playerMap = new PlayerMap( playerMapString );

        // Cheats menu
        isCheatOptionsShown = mPreferences.getBoolean( PLAY_SHOW_CHEATS, false );

        // Emulation prefs
        r4300Emulator = emulationProfile.get( "r4300Emulator", "2" );
        disableExpansionPak = emulationProfile.get( "DisableExtraMem", "False" ).equals( "True" );
        String rspSetting = emulationProfile.get( "rspSetting", "rsp-hle" );

        if(rspSetting.equals("rsp-hle"))
        {
            rspPluginPath = appData.libsDir + "/libmupen64plus-rsp-hle.so";
            rspHleVideo = true;
        }
        else if(rspSetting.equals("rsp-cxd4-hle"))
        {
            rspPluginPath = appData.libsDir + "/libmupen64plus-rsp-cxd4.so";
            rspHleVideo = true;
        }
        else
        {
            rspPluginPath = appData.libsDir + "/libmupen64plus-rsp-cxd4.so";
            rspHleVideo = false;
        }


        videoPlugin = new Plugin( emulationProfile, appData.libsDir, "videoPlugin" );

        // Video prefs - gln64
        isGln64Enabled = videoPlugin.name.equals( "libmupen64plus-video-gln64.so" );
        int maxFrameskip = getSafeInt( emulationProfile, "gln64Frameskip", 0 );
        isGln64AutoFrameskipEnabled = maxFrameskip < 0;
        gln64MaxFrameskip = Math.abs( maxFrameskip );
        isGln64FogEnabled = emulationProfile.get( "gln64Fog", "0" ).equals( "1" );
        isGln64SaiEnabled = emulationProfile.get( "gln64Sai", "0" ).equals( "1" );
        isGln64ScreenClearEnabled = emulationProfile.get( "gln64ScreenClear", "1" ).equals( "1" );
        isGln64AlphaTestEnabled = emulationProfile.get( "gln64AlphaTest", "1" ).equals( "1" );
        isGln64HackDepthEnabled = emulationProfile.get( "gln64HackDepth", "1" ).equals( "1" );

        // Video prefs - rice
        isRiceEnabled = videoPlugin.name.equals( "libmupen64plus-video-rice.so" );
        isRiceAutoFrameskipEnabled = emulationProfile.get( "riceAutoFrameskip", "False" ).equals( "True" );
        isRiceFastTextureLoadingEnabled = emulationProfile.get( "riceFastTexture", "False" ).equals( "True" );
        isRiceForceTextureFilterEnabled = emulationProfile.get( "riceForceTextureFilter", "False" ).equals( "True" );
        riceScreenUpdateType = emulationProfile.get( "riceScreenUpdate", "4" );
        riceTextureEnhancement = emulationProfile.get( "riceTextureEnhancement", "0" );
        isRiceHiResTexturesEnabled = emulationProfile.get( "riceHiResTextures", "True" ).equals( "True" );
        isRiceFogEnabled = emulationProfile.get( "riceFog", "False" ).equals( "True" );

        // Video prefs - glide64
        isGlide64Enabled = videoPlugin.name.equals( "libmupen64plus-video-glide64mk2.so" );
        maxFrameskip = getSafeInt( emulationProfile, "glide64Frameskip", 0 );
        isGlide64AutoFrameskipEnabled = maxFrameskip < 0;
        glide64MaxFrameskip = Math.abs( maxFrameskip );

        // Video prefs - GLideN64, this is a more broad search because there used to be more than one GLideN64 version
        isGliden64Enabled = videoPlugin.name.contains( "libmupen64plus-video-gliden64" );

        gliden64CropMode = getSafeInt( emulationProfile, "CropMode", 1);
        gliden64MultiSampling = getSafeInt( emulationProfile, "MultiSampling", 0);
        gliden64BilinearMode = getSafeInt( emulationProfile, "bilinearMode", 0);
        gliden64MaxAnisotropy = getSafeInt( emulationProfile, "MaxAnisotropy", 0);
        gliden64CacheSize = getSafeInt( emulationProfile, "CacheSize", 128);
        gliden64EnableNoise = emulationProfile.get( "EnableNoise", "True" ).equals( "True" );
        gliden64EnableLOD = emulationProfile.get( "EnableLOD", "True" ).equals( "True" );
        gliden64EnableHWLighting = emulationProfile.get( "EnableHWLighting", "False" ).equals( "True" );
        gliden64EnableShadersStorage = emulationProfile.get( "EnableShadersStorage", "True" ).equals( "True" );
        gliden64CorrectTexrectCoords = getSafeInt( emulationProfile, "CorrectTexrectCoords", 0);
        gliden64EnableNativeResTexrects = emulationProfile.get( "EnableNativeResTexrects", "False" ).equals( "True" );
        gliden64EnableLegacyBlending = emulationProfile.get( "EnableLegacyBlending", "True" ).equals( "True" );
        gliden64EnableFragmentDepthWrite = emulationProfile.get( "EnableFragmentDepthWrite", "False" ).equals( "True" );
        gliden64EnableFBEmulation = emulationProfile.get( "EnableFBEmulation", "True" ).equals( "True" );
        gliden64BufferSwapMode = getSafeInt( emulationProfile, "BufferSwapMode", 2);
        gliden64EnableCopyColorToRDRAM = getSafeInt( emulationProfile, "EnableCopyColorToRDRAM", 0);
        gliden64EnableCopyAuxiliaryToRDRAM = emulationProfile.get( "EnableCopyAuxiliaryToRDRAM", "False" ).equals( "True" );
        gliden64EnableCopyDepthToRDRAM = getSafeInt( emulationProfile, "EnableCopyDepthToRDRAM", 2 );
        gliden64EnableCopyColorFromRDRAM = emulationProfile.get( "EnableCopyColorFromRDRAM", "False" ).equals( "True" );
        gliden64EnableN64DepthCompare = emulationProfile.get( "EnableN64DepthCompare", "False" ).equals( "True" );
        gliden64UseNativeResolutionFactor = gliden64EnableNativeResTexrects ?
                0 :getSafeInt( emulationProfile, "UseNativeResolutionFactor", 0);
        gliden64DisableFBInfo = emulationProfile.get( "DisableFBInfo", "True" ).equals( "True" );
        gliden64FBInfoReadColorChunk = emulationProfile.get( "FBInfoReadColorChunk", "False" ).equals( "True" );
        gliden64FBInfoReadDepthChunk = emulationProfile.get( "FBInfoReadDepthChunk", "True" ).equals( "True" );
        gliden64TxFilterMode = getSafeInt( emulationProfile, "txFilterMode", 0);
        gliden64TxEnhancementMode = gliden64EnableNativeResTexrects ? 0 :getSafeInt( emulationProfile, "txEnhancementMode", 0);
        gliden64TxDeposterize = emulationProfile.get( "txDeposterize", "False" ).equals( "True" );
        gliden64TxFilterIgnoreBG = emulationProfile.get( "txFilterIgnoreBG", "True" ).equals( "True" );
        gliden64TxCacheSize = getSafeInt( emulationProfile, "txCacheSize", 128);
        gliden64TxHiresEnable = emulationProfile.get( "txHiresEnable", "False" ).equals( "True" );
        gliden64TxHiresFullAlphaChannel = emulationProfile.get( "txHiresFullAlphaChannel", "False" ).equals( "True" );
        gliden64TxHresAltCRC = emulationProfile.get( "txHresAltCRC", "False" ).equals( "True" );
        gliden64TxCacheCompression = emulationProfile.get( "txCacheCompression", "True" ).equals( "True" );
        gliden64TxForce16bpp = emulationProfile.get( "txForce16bpp", "False" ).equals( "True" );
        gliden64TxSaveCache = emulationProfile.get( "txSaveCache", "False" ).equals( "True" );
        gliden64EnableBloom = emulationProfile.get( "EnableBloom", "False" ).equals( "True" );
        gliden64BloomThresholdLevel = getSafeInt( emulationProfile, "bloomThresholdLevel", 4);
        gliden64BloomBlendMode = getSafeInt( emulationProfile, "bloomBlendMode", 0);
        gliden64BlurAmount = getSafeInt( emulationProfile, "blurAmount", 10);
        gliden64BlurStrength = getSafeInt( emulationProfile, "blurStrength", 20);
        gliden64ForceGammaCorrection = emulationProfile.get( "ForceGammaCorrection", "False" ).equals( "True" );
        gliden64GammaCorrectionLevel = getSafeInt( emulationProfile, "GammaCorrectionLevel", 10)/10.0f;

        //Video preferences for angrylion
        isAngrylionEnabled = videoPlugin.name.equals( "libmupen64plus-video-angrylion.so" );
        angrylionVIOverlayEnabled = emulationProfile.get( "VIOverlay", "False" ).equals( "True" );

        final String scaling = mPreferences.getString( "displayScaling", "default" );

        boolean stretchScreen = scaling.equals("default") ? globalPrefs.stretchScreen : scaling.equals( "stretch" );

        //Stretch screen if the GLideN64 wide screen hack is enabled and the current video plugin is GLideN64
        //Do not stretch the screen if the current video plugin is Angrylion
        mStretch = (stretchScreen ||
                (emulationProfile.get( "WidescreenHack", "False" ).equals("True") && isGliden64Enabled))
                && !isAngrylionEnabled;
        final int hResolution = getSafeInt( mPreferences, DISPLAY_RESOLUTION, -1 );

        videoSurfaceWidth = globalPrefs.getResolutionWidth(mStretch, 0);
        videoSurfaceHeight = globalPrefs.getResolutionHeight(mStretch, 0);
        videoRenderWidth = isAngrylionEnabled ? 640 : globalPrefs.getResolutionWidth(mStretch, hResolution);
        videoRenderHeight = isAngrylionEnabled ? 480 : globalPrefs.getResolutionHeight(mStretch, hResolution);

        videoSurfaceZoom = mPreferences.getInt( "displayZoomSeek", 100 );

        // Touchscreen prefs
        isTouchscreenEnabled = touchscreenProfile != null;

        if ( isTouchscreenEnabled )
        {
            // Determine the touchscreen auto-holdables
            touchscreenNotAutoHoldables = getSafeIntSet( touchscreenProfile, "touchscreenNotAutoHoldables" );

            //Axis inversion
            invertTouchXAxis = touchscreenProfile.get( "invertTouchXAxis", "False" ).equals( "True" );
            invertTouchYAxis = touchscreenProfile.get( "invertTouchYAxis", "False" ).equals( "True" );

            // Determine the touchscreen layout
            final String layout = touchscreenProfile.get( "touchscreenSkin", "Outline" );
            if( layout.equals( "Custom" ) )
                touchscreenSkin =  touchscreenProfile.get( "touchscreenCustomSkinPath", "" );
            else
                touchscreenSkin = appData.touchscreenSkinsDir + layout;

            // Sensor prefs
            isAnalogHiddenWhenSensor = touchscreenProfile.get("touchscreenHideAnalogWhenSensor", "False").equals( "True" );
            sensorActivateOnStart = touchscreenProfile.get("sensorActivateOnStart", "False").equals( "True" );
            sensorAxisX = touchscreenProfile.get("sensorAxisX", "");
            sensorAngleX = SafeMethods.toFloat(touchscreenProfile.get("sensorAngleX"), 0);
            int sensitivity;
            try {
                sensitivity = Integer.valueOf(touchscreenProfile.get("sensorSensitivityX"));
            } catch (final NumberFormatException ex) {
                sensitivity = 100;
            }
            if (Boolean.valueOf(touchscreenProfile.get("sensorInvertX"))) {
                sensitivity = -sensitivity;
            }
            sensorSensitivityX = sensitivity;
            sensorAxisY = touchscreenProfile.get("sensorAxisY", "");
            sensorAngleY = SafeMethods.toFloat(touchscreenProfile.get("sensorAngleY"), 0);
            try {
                sensitivity = Integer.valueOf(touchscreenProfile.get("sensorSensitivityY"));
            } catch (final NumberFormatException ex) {
                sensitivity = 100;
            }
            if (Boolean.valueOf(touchscreenProfile.get("sensorInvertY"))) {
                sensitivity = -sensitivity;
            }
            sensorSensitivityY = sensitivity;
        }
        else
        {
            touchscreenNotAutoHoldables = null;
            invertTouchXAxis = false;
            invertTouchYAxis = false;
            touchscreenSkin = "";

            isAnalogHiddenWhenSensor = false;
            sensorActivateOnStart = false;
            sensorAxisX = null;
            sensorAngleX = 0;
            sensorSensitivityX = 100;
            sensorAxisY = null;
            sensorAngleY = 0;
            sensorSensitivityY = 100;
        }

        isTouchscreenHidden = !isTouchscreenEnabled || globalPrefs.touchscreenTransparency == 0;

        int tmpTouchscreenAutoHold = getSafeInt( mPreferences, "touchscreenAutoHoldV2", -1 );

        if(tmpTouchscreenAutoHold == -1)
        {
            tmpTouchscreenAutoHold = globalPrefs.touchscreenAutoHold;
        }

        touchscreenAutoHold = tmpTouchscreenAutoHold;

        // Peripheral share mode
        isControllerShared = mPreferences.getBoolean( "inputShareController", false );

        // Determine which peripheral controllers are enabled
        isControllerEnabled1 = controllerProfile1 != null;
        isControllerEnabled2 = controllerProfile2 != null;
        isControllerEnabled3 = controllerProfile3 != null;
        isControllerEnabled4 = controllerProfile4 != null;

        // Determine whether controller deconfliction is needed
        int numControllers = 0;
        numControllers += isControllerEnabled1 ? 1 : 0;
        numControllers += isControllerEnabled2 ? 1 : 0;
        numControllers += isControllerEnabled3 ? 1 : 0;
        numControllers += isControllerEnabled4 ? 1 : 0;

        boolean playerMappingEnabled = (numControllers > 1 || playerMap.getNumberOfMappedPlayers() !=0) && !isControllerShared;

        if(playerMappingEnabled)
        {
            Log.i("GamePrefs", "Player mapping is enabled!");
        }
        playerMap.setEnabled( playerMappingEnabled);

        // Determine which players are "plugged in"
        isPlugged1 = isControllerEnabled1 || isTouchscreenEnabled;
        isPlugged2 = isControllerEnabled2;
        isPlugged3 = isControllerEnabled3;
        isPlugged4 = isControllerEnabled4;

        //A value of zero means default for the game as specified in mupen64plus.ini
        countPerOp = mPreferences.getInt( "screenAdvancedCountPerOp", 0 );
        countPerScanline = mPreferences.getInt( "screenAdvancedCountPerScanline", 0 );
    }

    public String getCheatArgs()
    {
        if( !isCheatOptionsShown )
            return "";

        final Pattern pattern = Pattern.compile( "^" + gameCrc + " Cheat(\\d+)" );
        StringBuilder builder = null;
        final Map<String, ?> map = mPreferences.getAll();
        for (final String key : map.keySet())
        {
            final Matcher matcher = pattern.matcher( key );
            if ( matcher.matches() && matcher.groupCount() > 0 )
            {
                final int value = mPreferences.getInt( key, 0 );
                if (value > 0)
                {
                    final int index = Integer.parseInt( matcher.group( 1 ) );

                    if (builder == null)
                        builder = new StringBuilder();
                    else
                        builder.append( ',' );
                    builder.append( index );
                    builder.append( '-' );
                    builder.append( value - 1 );
                }
            }
        }
        return builder == null ? "" : builder.toString();
    }

    public static String getGameDataPath( String romMd5, String headerName, String countrySymbol,
        AppData appData)
    {
        return String.format( "%s/%s %s %s", appData.gameDataDir, headerName, countrySymbol, romMd5 );
    }

    private static Profile loadProfile( SharedPreferences prefs, String key, String defaultName,
        String appDefault, ConfigFile custom, ConfigFile builtin )
    {
        final String name = prefs.getString( key, defaultName );

        Log.i("GamePrefs", "Profile: " +
                " key=" + key +
                " defaultName=" + defaultName +
                " appDefault=" + appDefault +
                " name=" + (name==null?"null":name)
        );

        if( !TextUtils.isEmpty( name ) && custom.keySet().contains( name ) )
            return new Profile( false, custom.get( name ) );
        else if( !TextUtils.isEmpty( name ) && builtin.keySet().contains( name ) )
            return new Profile( true, builtin.get( name ) );
        else if( custom.keySet().contains( defaultName ) )
            return new Profile( false, custom.get( defaultName ) );
        else if( builtin.keySet().contains( defaultName ) )
            return new Profile( true, builtin.get( defaultName ) );
        else if( custom.keySet().contains( appDefault ) )
            return new Profile( false, custom.get( appDefault ) );
        else if( builtin.keySet().contains( appDefault ) )
            return new Profile( true, builtin.get( appDefault ) );
        else
            return null;
    }

    private static ControllerProfile loadControllerProfile( SharedPreferences prefs, String key, String defaultName,
                                        String appDefault, ConfigFile custom, ConfigFile builtin )
    {
        final String name = prefs.getString( key, defaultName );

        Log.i("GamePrefs", "Profile: " +
                " key=" + key +
                " defaultName=" + defaultName +
                " appDefault=" + appDefault +
                " name=" + (name==null?"null":name)
        );

        //Length zero profile is the "disabled" profile
        if(name != null && name.length() == 0)
            return null;
        else if( name != null && custom.keySet().contains( name ) )
            return new ControllerProfile( false, custom.get( name ) );
        else if( name != null && builtin.keySet().contains( name ) )
            return new ControllerProfile( true, builtin.get( name ) );
        else if( custom.keySet().contains( defaultName ) )
            return new ControllerProfile( false, custom.get( defaultName ) );
        else if( builtin.keySet().contains( defaultName ) )
            return new ControllerProfile( true, builtin.get( defaultName ) );
        else if( custom.keySet().contains( appDefault ) )
            return new ControllerProfile( false, custom.get( appDefault ) );
        else if( builtin.keySet().contains( appDefault ) )
            return new ControllerProfile( true, builtin.get( appDefault ) );
        else
            return null;
    }

    private static int getSafeInt( Profile profile, String key, int defaultValue )
    {
        try
        {
            return Integer.parseInt( profile.get( key, String.valueOf( defaultValue ) ) );
        }
        catch( final NumberFormatException ex )
        {
            return defaultValue;
        }
    }

    private static int getSafeInt( SharedPreferences preferences, String key, int defaultValue )
    {
        try
        {
            return Integer.parseInt( preferences.getString( key, String.valueOf( defaultValue ) ) );
        }
        catch( final NumberFormatException ex )
        {
            return defaultValue;
        }
    }

    private static Set<Integer> getSafeIntSet( Profile profile, String key )
    {
        final Set<Integer> mutableSet = new HashSet<Integer>();
        final String elements = profile.get( key, "" );
        for( final String element : MultiSelectListPreference.deserialize( elements ) )
        {
            try
            {
                mutableSet.add( Integer.valueOf( element ) );
            }
            catch( final NumberFormatException ignored )
            {
            }
        }
        return Collections.unmodifiableSet( mutableSet );
    }
}
