package paulscode.android.mupen64plusae.persistent;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;

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

    /** The selected video plug-in. */
    public final Plugin videoPlugin;

    /** True if gln64 video plug-in is enabled. */
    public final boolean isGln64Enabled;

    /** True if rice video plug-in is enabled. */
    public final boolean isRiceEnabled;

    /** True if glide64 video plug-in is enabled. */
    public final boolean isGlide64Enabled;

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

    /** The maximum frameskip in the glide64 library. */
    public final int glide64MaxFrameskip;

    /** True if auto-frameskip is enabled in the glide64 library. */
    public final boolean isGlide64AutoFrameskipEnabled;

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

    /** Enable depth buffer copy to RDRAM. */
    public final boolean gliden64EnableCopyDepthToRDRAM;

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

    /** The set of auto-holdable button commands. */
    public final Set<Integer> touchscreenAutoHoldables;

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

    /** True if the touchscreen joystick is animated. */
    public final boolean isTouchscreenAnimated;

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

    /** The width of the viewing surface, in pixels with the correct aspect ratio. */
    public final int videoSurfaceWidthOriginal;

    /** The height of the viewing surface, in pixels with the correct aspect ratio. */
    public final int videoSurfaceHeightOriginal;

    /** Screen width ratio from 16:9 to 4:3*/
    public final float widthRatio;

    /** Screen width ratio from 16:9 to 4:3*/
    public final float heightRatio;

    /** If display mode is stretch*/
    public final boolean mStretch;

    /** Game CRC */
    public final String crc;

    private final SharedPreferences mPreferences;

    /** Profile keys */
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

    @TargetApi( 17 )
    public GamePrefs( Context context, String romMd5, String crc, String headerName, String goodName,
        String countrySymbol, AppData appData, GlobalPrefs globalPrefs, String legacySave)
    {
        gameHeaderName = headerName;
        gameGoodName = goodName;
        legacySaveFileName = legacySave;
        
        sharedPrefsName = romMd5.replace(' ', '_' ) + "_preferences";
        mPreferences = context.getSharedPreferences( sharedPrefsName, Context.MODE_PRIVATE );

        // Game-specific data
        gameDataDir = getGameDataPath( romMd5, headerName, countrySymbol, globalPrefs);
        sramDataDir = gameDataDir + "/" + SRAM_DATA_DIR;
        autoSaveDir = gameDataDir + "/" + AUTO_SAVES_DIR;
        slotSaveDir = gameDataDir + "/" + SLOT_SAVES_DIR;
        userSaveDir = gameDataDir + "/" + USER_SAVES_DIR;
        screenshotDir = gameDataDir + "/" + SCREENSHOTS_DIR;
        coreUserConfigDir = gameDataDir + "/" + CORE_CONFIG_DIR;
        mupen64plus_cfg = coreUserConfigDir + "/" + MUPEN_CONFIG_FILE;

        // Emulation profile
        emulationProfile = loadProfile( mPreferences, EMULATION_PROFILE,
                globalPrefs.getEmulationProfileDefault(), GlobalPrefs.DEFAULT_EMULATION_PROFILE_DEFAULT,
                globalPrefs.GetEmulationProfilesConfig(), appData.GetEmulationProfilesConfig() );

        // Touchscreen profile
        if(globalPrefs.isBigScreenMode)
        {
            touchscreenProfile =  new Profile( true, appData.GetTouchscreenProfilesConfig().get( "FPS-Only" ) );
        }
        else
        {
            touchscreenProfile = loadProfile( mPreferences, TOUCHSCREEN_PROFILE,
                globalPrefs.getTouchscreenProfileDefault(), GlobalPrefs.DEFAULT_TOUCHSCREEN_PROFILE_DEFAULT,
                globalPrefs.GetTouchscreenProfilesConfig(), appData.GetTouchscreenProfilesConfig() );
        }

        // Controller profiles
        controllerProfile1 = loadControllerProfile( mPreferences, globalPrefs, CONTROLLER_PROFILE1,
                globalPrefs.getControllerProfileDefault(1),
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile2 = loadControllerProfile( mPreferences, globalPrefs, CONTROLLER_PROFILE2,
                globalPrefs.getControllerProfileDefault(2),
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile3 = loadControllerProfile( mPreferences, globalPrefs, CONTROLLER_PROFILE3,
                globalPrefs.getControllerProfileDefault(3),
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile4 = loadControllerProfile( mPreferences, globalPrefs, CONTROLLER_PROFILE4,
                globalPrefs.getControllerProfileDefault(4),
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );

        // Player map
        String playerMapString = mPreferences.getString( PLAYER_MAP, "" );

        if( playerMapString.isEmpty() )
            playerMapString = globalPrefs.getString( PLAYER_MAP, "" );

        playerMap = new PlayerMap( playerMapString );

        // Cheats menu
        isCheatOptionsShown = mPreferences.getBoolean( PLAY_SHOW_CHEATS, false );

        // Emulation prefs
        r4300Emulator = emulationProfile.get( "r4300Emulator", "2" );
        final Plugin tempVideoPlugin = new Plugin( emulationProfile, appData.libsDir, "videoPlugin" );
        if( tempVideoPlugin.name.contains( "%1$s" ))
            videoPlugin = new Plugin( emulationProfile, appData.libsDir, "videoPlugin", "videoSubPlugin" );
        else
            videoPlugin = tempVideoPlugin;

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


        // Video prefs - GLideN64
        gliden64MultiSampling = getSafeInt( emulationProfile, "MultiSampling", 0);
        gliden64BilinearMode = getSafeInt( emulationProfile, "bilinearMode", 1);
        gliden64MaxAnisotropy = getSafeInt( emulationProfile, "MaxAnisotropy", 0);
        gliden64CacheSize = getSafeInt( emulationProfile, "CacheSize", 256);
        gliden64EnableNoise = emulationProfile.get( "EnableNoise", "True" ).equals( "True" );
        gliden64EnableLOD = emulationProfile.get( "EnableLOD", "True" ).equals( "True" );
        gliden64EnableHWLighting = emulationProfile.get( "EnableHWLighting", "False" ).equals( "True" );
        gliden64EnableShadersStorage = emulationProfile.get( "EnableShadersStorage", "True" ).equals( "True" );
        gliden64CorrectTexrectCoords = getSafeInt( emulationProfile, "CorrectTexrectCoords", 1);
        gliden64EnableNativeResTexrects = emulationProfile.get( "EnableNativeResTexrects", "True" ).equals( "True" );
        gliden64EnableFBEmulation = emulationProfile.get( "EnableFBEmulation", "True" ).equals( "True" );
        gliden64BufferSwapMode = getSafeInt( emulationProfile, "BufferSwapMode", 2);
        gliden64EnableCopyColorToRDRAM = getSafeInt( emulationProfile, "EnableCopyColorToRDRAM", 2);
        gliden64EnableCopyAuxiliaryToRDRAM = emulationProfile.get( "EnableCopyAuxiliaryToRDRAM", "False" ).equals( "True" );
        gliden64EnableCopyDepthToRDRAM = emulationProfile.get( "EnableCopyDepthToRDRAM", "False" ).equals( "True" );
        gliden64EnableCopyColorFromRDRAM = emulationProfile.get( "EnableCopyColorFromRDRAM", "False" ).equals( "True" );
        gliden64EnableN64DepthCompare = emulationProfile.get( "EnableN64DepthCompare", "False" ).equals( "True" );
        gliden64UseNativeResolutionFactor = getSafeInt( emulationProfile, "UseNativeResolutionFactor", 0);
        gliden64DisableFBInfo = emulationProfile.get( "DisableFBInfo", "True" ).equals( "True" );
        gliden64FBInfoReadColorChunk = emulationProfile.get( "FBInfoReadColorChunk", "False" ).equals( "True" );
        gliden64FBInfoReadDepthChunk = emulationProfile.get( "FBInfoReadDepthChunk", "True" ).equals( "True" );
        gliden64TxFilterMode = getSafeInt( emulationProfile, "txFilterMode}]", 0);
        gliden64TxEnhancementMode = getSafeInt( emulationProfile, "txEnhancementMode", 0);
        gliden64TxDeposterize = emulationProfile.get( "txDeposterize", "False" ).equals( "True" );
        gliden64TxFilterIgnoreBG = emulationProfile.get( "txFilterIgnoreBG", "False" ).equals( "True" );
        gliden64TxCacheSize = getSafeInt( emulationProfile, "txCacheSize", 256);
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

        // Determine the pixel dimensions of the rendering context and view surface
        {
            // Screen size
            final WindowManager windowManager = (WindowManager) context.getSystemService(android.content.Context.WINDOW_SERVICE);
            final Display display = windowManager.getDefaultDisplay();
            int stretchWidth;
            int stretchHeight;
            if( display == null )
            {
                stretchWidth = stretchHeight = 0;
            }
            //Kit Kat (19) adds support for immersive mode
            else if( AppData.IS_KITKAT && globalPrefs.isImmersiveModeEnabled )
            {
                final Point dimensions = new Point();
                display.getRealSize(dimensions);
                stretchWidth = dimensions.x;
                stretchHeight = dimensions.y;
            }
            else
            {
                final Point dimensions = new Point();
                display.getSize(dimensions);
                stretchWidth = dimensions.x;
                stretchHeight = dimensions.y;
            }

            final float aspect = 0.75f; // TODO: Handle PAL
            final boolean isLetterboxed = ( (float) stretchHeight / (float) stretchWidth ) > aspect;
            final int originalWidth = isLetterboxed ? stretchWidth : Math.round( stretchHeight / aspect );
            final int originalHeight = isLetterboxed ? Math.round( stretchWidth * aspect ) : stretchHeight;

            final String scaling = mPreferences.getString( "displayScaling", "original" );
            widthRatio = (float)stretchWidth/(float)originalWidth;
            heightRatio = (float)stretchHeight/(float)originalHeight;

            videoSurfaceWidthOriginal = originalWidth;
            videoSurfaceHeightOriginal = originalHeight;

            mStretch = scaling.equals( "stretch" );

            // Native resolution
            if( mStretch )
            {
                videoSurfaceWidth = stretchWidth;
                videoSurfaceHeight = stretchHeight;
            }
            else // scaling.equals( "original")
            {
                videoSurfaceWidth = videoSurfaceWidthOriginal;
                videoSurfaceHeight = videoSurfaceHeightOriginal;
            }
        }

        // Display prefs
        final int hResolution = getSafeInt( mPreferences, "displayResolution", 0 );
        int tempVideoRenderWidth = 0;
        int tempVideoRenderHeight = 0;

        switch( hResolution )
        {
            case 720:
                tempVideoRenderWidth = 960;
                tempVideoRenderHeight = 720;
                break;
            case 600:
                tempVideoRenderWidth = 800;
                tempVideoRenderHeight = 600;
                break;
            case 480:
                tempVideoRenderWidth = 640;
                tempVideoRenderHeight = 480;
                break;
            case 360:
                tempVideoRenderWidth = 480;
                tempVideoRenderHeight = 360;
                break;
            case 240:
                tempVideoRenderWidth = 320;
                tempVideoRenderHeight = 240;
                break;
            case 120:
                tempVideoRenderWidth = 160;
                tempVideoRenderHeight = 120;
                break;
            default:
                tempVideoRenderWidth = videoSurfaceWidthOriginal;
                tempVideoRenderHeight = videoSurfaceHeightOriginal;
                break;
        }

        if(mStretch)
        {
            //If we are in stretch mode we have to increase the approppriate dimension by the corresponding
            //ratio to make it full screen
            if(globalPrefs.displayOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                globalPrefs.displayOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
            {

                final float newWidth = tempVideoRenderHeight * heightRatio;
                tempVideoRenderHeight = Math.round(newWidth);
            }
            else
            {
                final float newWidth = tempVideoRenderWidth * widthRatio;
                tempVideoRenderWidth = Math.round(newWidth);
            }
        }

        videoRenderWidth = tempVideoRenderWidth;
        videoRenderHeight = tempVideoRenderHeight;

        videoSurfaceZoom = mPreferences.getInt( "displayZoomSeek", 100 );

        // Touchscreen prefs
        isTouchscreenEnabled = touchscreenProfile != null;

        if ( isTouchscreenEnabled )
        {
            isTouchscreenAnimated = touchscreenProfile.get( "touchscreenAnimated", "False" ).equals( "True" );

            // Determine the touchscreen auto-holdables
            touchscreenAutoHoldables = getSafeIntSet( touchscreenProfile,
                    "touchscreenAutoHoldables" );

            // Determine the touchscreen layout
            final String layout = touchscreenProfile.get( "touchscreenSkin", "Outline" );
            if( layout.equals( "Custom" ) )
                touchscreenSkin =  touchscreenProfile.get( "touchscreenCustomSkinPath", "" );
            else
                touchscreenSkin = appData.touchscreenSkinsDir + layout;

            // Sensor prefs
            isAnalogHiddenWhenSensor = Boolean.valueOf(touchscreenProfile.get("touchscreenHideAnalogWhenSensor"));
            sensorActivateOnStart = Boolean.valueOf(touchscreenProfile.get("sensorActivateOnStart"));
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
            isTouchscreenAnimated = false;
            touchscreenAutoHoldables = null;
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
        playerMap.setEnabled( numControllers > 1 && !globalPrefs.isControllerShared );

        // Determine which players are "plugged in"
        isPlugged1 = isControllerEnabled1 || isTouchscreenEnabled;
        isPlugged2 = isControllerEnabled2;
        isPlugged3 = isControllerEnabled3;
        isPlugged4 = isControllerEnabled4;
        this.crc = crc;
    }

    public String getCheatArgs()
    {
        if( !isCheatOptionsShown )
            return "";

        final Pattern pattern = Pattern.compile( "^" + crc + " Cheat(\\d+)" );
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
        GlobalPrefs globalPrefs)
    {
        return String.format( "%s/GameData/%s %s %s", globalPrefs.userDataDir, headerName, countrySymbol, romMd5 );
    }

    private static Profile loadProfile( SharedPreferences prefs, String key, String defaultName,
        String appDefault, ConfigFile custom, ConfigFile builtin )
    {
        final String name = prefs.getString( key, defaultName );

        if( TextUtils.isEmpty( name ) )
            return null;
        else if( custom.keySet().contains( name ) )
            return new Profile( false, custom.get( name ) );
        else if( builtin.keySet().contains( name ) )
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

    private static ControllerProfile loadControllerProfile( SharedPreferences prefs, GlobalPrefs globalPrefs,
            String key, String defaultName, ConfigFile custom, ConfigFile builtin )
    {
        final String globalName = globalPrefs.getString( key, defaultName );
        final String name = prefs.getString( key, globalName );

        if( custom.keySet().contains( name ) )
            return new ControllerProfile( false, custom.get( name ) );
        else if( builtin.keySet().contains( name ) )
            return new ControllerProfile( true, builtin.get( name ) );
        else if( custom.keySet().contains( defaultName ) )
            return new ControllerProfile( false, custom.get( defaultName ) );
        else if( builtin.keySet().contains( defaultName ) )
            return new ControllerProfile( true, builtin.get( defaultName ) );
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
