package paulscode.android.mupen64plusae.persistent;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import paulscode.android.mupen64plusae.input.map.PlayerMap;
import paulscode.android.mupen64plusae.jni.NativeConstants;
import paulscode.android.mupen64plusae.preference.MultiSelectListPreference;
import paulscode.android.mupen64plusae.profile.ControllerProfile;
import paulscode.android.mupen64plusae.profile.Profile;
import paulscode.android.mupen64plusae.util.Plugin;
import paulscode.android.mupen64plusae.util.SafeMethods;

public class GamePrefs
{
    //Pak Type
    public enum PakType {
        NONE(NativeConstants.PAK_TYPE_NONE, R.string.menuItem_pak_empty),
        MEMORY(NativeConstants.PAK_TYPE_MEMORY, R.string.menuItem_pak_mem),
        RAMBLE(NativeConstants.PAK_TYPE_RUMBLE, R.string.menuItem_pak_rumble),
        TRANSFER(NativeConstants.PAK_TYPE_TRANSFER, R.string.menuItem_pak_transfer);

        private final int mNativeValue;
        private final int mResourceStringName;

        PakType(int nativeValue, int resourceStringName)
        {
            mNativeValue = nativeValue;
            mResourceStringName = resourceStringName;
        }

        public int getNativeValue()
        {
            return mNativeValue;
        }

        public static PakType getPakTypeFromNativeValue(int nativeValue)
        {
            switch (nativeValue)
            {
                case NativeConstants.PAK_TYPE_NONE:
                    return NONE;
                case NativeConstants.PAK_TYPE_MEMORY:
                    return MEMORY;
                case NativeConstants.PAK_TYPE_RUMBLE:
                    return RAMBLE;
                case NativeConstants.PAK_TYPE_TRANSFER:
                    return RAMBLE;
                default:
                    return NONE;

            }
        }

        public int getResourceString()
        {
            return mResourceStringName;
        }
    }

    /** The name of the game-specific {@link SharedPreferences} object.*/
    private final String mSharedPrefsName;

    /** App data */
    private AppData mAppData;

    /** Global prefs */
    private GlobalPrefs mGlobalPrefs;

    /** The parent directory containing all game-specific data files. */
    private String gameDataDir;

    /** The subdirectory containing SRAM/EEPROM data (in-game saves). */
    private String sramDataDir;

    /** The subdirectory containing auto save files. */
    private String autoSaveDir;

    /** The subdirectory containing slot save files. */
    private String slotSaveDir;

    /** The subdirectory containing manual save files. */
    private String userSaveDir;

    /** Game header name */
    public final String gameHeaderName;

    /** Game header name */
    private final String gameCountrySymbol;

    /** Game good name */
    public final String gameGoodName;
    
    /** Legacy save file name */
    public final String legacySaveFileName;
    
    /** The subdirectory containing user screenshots. */
    private String screenshotDir;

    /** The subdirectory returned from the core's ConfigGetUserConfigPath() method. Location of core config file. */
    private String coreUserConfigDir;

    /** The path of the Mupen64Plus base configuration file. */
    private String mupen64plus_cfg;

    /** The emulation profile. */
    public final Profile emulationProfile;

    /** The touchscreen profile. */
    public final Profile touchscreenProfile;

    private static final int NUM_CONTROLLERS = 4;

    /** The input profiles for all player. */
    public final ControllerProfile[] controllerProfile = new ControllerProfile[NUM_CONTROLLERS];

    /** The player map for multi-player gaming. */
    public final PlayerMap playerMap;

    /** True if the cheats category should be shown in the menu. */
    final boolean isCheatOptionsShown;

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

    /** True if glide64 video plug-in is enabled. */
    public final boolean isGlide64Enabled;

    public final Glide64mk2Prefs glide64mk2Prefs;

    /** True if gliden64 video plug-in is enabled. */
    public final boolean isGliden64Enabled;

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

    public final GLideN64Prefs glideN64Prefs;

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

    /** True if we want use default player mapping*/
    final boolean useDefaultPlayerMapping;

    /** True if a specific controller is enabled. */
    public final boolean[] isControllerEnabled = new boolean[NUM_CONTROLLERS];

    /** True if any type of AbstractController is enabled for all players. */
    public final boolean[] isPlugged = new boolean[NUM_CONTROLLERS];

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

    /** Core CountPerOp setting */
    public final int countPerOp;

    /** The method used for auto holding buttons. */
    public final int touchscreenAutoHold;

    /** This is true if this game uses the D-pad */
    final boolean isDpadGame;

    /** Game CRC */
    private final String gameCrc;

    /** ROM MD5 */
    private final String romMd5;

    private final SharedPreferences mPreferences;

    /** Profile keys */
    static final String DISPLAY_RESOLUTION = "displayResolution";
    static final String EMULATION_PROFILE = "emulationProfile";
    static final String TOUCHSCREEN_PROFILE = "touchscreenProfile";
    static final String CONTROLLER_PROFILE1 = "controllerProfile1";
    static final String CONTROLLER_PROFILE2 = "controllerProfile2";
    static final String CONTROLLER_PROFILE3 = "controllerProfile3";
    static final String CONTROLLER_PROFILE4 = "controllerProfile4";
    static final String PLAYER_MAP = "playerMapV2";
    static final String PLAY_SHOW_CHEATS = "playShowCheats";

    /**
     * Directories and file names
     */
    private static final String SRAM_DATA_DIR = "SramData";
    public static final String AUTO_SAVES_DIR = "AutoSaves";
    private static final String SLOT_SAVES_DIR = "SlotSaves";
    private static final String USER_SAVES_DIR = "UserSaves";
    private static final String SCREENSHOTS_DIR = "Screenshots";
    private static final String CORE_CONFIG_DIR = "CoreConfig";
    private static final String MUPEN_CONFIG_FILE = "mupen64plus.cfg";


    private static final String KEYTEMPLATE_PAK_TYPE = "inputPakType";
    private static final int DEFAULT_PAK_TYPE = NativeConstants.PAK_TYPE_MEMORY;

    public GamePrefs( Context context, String md5, String crc, String headerName, String goodName,
        String countrySymbol, AppData appData, GlobalPrefs globalPrefs, String legacySave)
    {
        mAppData = appData;
        mGlobalPrefs = globalPrefs;
        gameHeaderName = headerName;
        gameGoodName = goodName;
        gameCountrySymbol = countrySymbol;
        legacySaveFileName = legacySave;
        romMd5 = md5;
        gameCrc = crc;
        mSharedPrefsName = romMd5.replace(' ', '_' ) + "_preferences";
        mPreferences = context.getSharedPreferences( mSharedPrefsName, Context.MODE_PRIVATE );

        // Game-specific data
        gameDataDir = getGameDataPath( romMd5, headerName, countrySymbol, appData);
        setGameDirs(appData, globalPrefs, gameDataDir);

        isDpadGame = isDpadGame(headerName, goodName);

        // Emulation profile
        emulationProfile = loadProfile( mPreferences, EMULATION_PROFILE,
                globalPrefs.getEmulationProfileDefault(), GlobalPrefs.DEFAULT_EMULATION_PROFILE_DEFAULT,
                globalPrefs.GetEmulationProfilesConfig(), appData.GetEmulationProfilesConfig() );

        if(emulationProfile == null) {
            throw new RuntimeException("Emulation profile is NULL");
        }
        
        Log.i("GamePrefs", "emulation profile found: " + emulationProfile.getName());

        // Touchscreen profile
        if(globalPrefs.isBigScreenMode)
        {
            touchscreenProfile =  new Profile( true, appData.GetTouchscreenProfilesConfig().get( "None" ) );
        }
        else
        {
            if(isDpadGame) {
                touchscreenProfile = loadProfile( mPreferences, TOUCHSCREEN_PROFILE,
                        globalPrefs.getTouchscreenDpadProfileDefault(), GlobalPrefs.DEFAULT_TOUCHSCREEN_DPAD_PROFILE_DEFAULT,
                        globalPrefs.GetTouchscreenProfilesConfig(), appData.GetTouchscreenProfilesConfig() );
            } else {
                touchscreenProfile = loadProfile( mPreferences, TOUCHSCREEN_PROFILE,
                        globalPrefs.getTouchscreenProfileDefault(), GlobalPrefs.DEFAULT_TOUCHSCREEN_PROFILE_DEFAULT,
                        globalPrefs.GetTouchscreenProfilesConfig(), appData.GetTouchscreenProfilesConfig() );
            }
        }

        // Controller profiles
        controllerProfile[0] = loadControllerProfile( mPreferences, CONTROLLER_PROFILE1,
                globalPrefs.getControllerProfileDefault(1), "",
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile[1] = loadControllerProfile( mPreferences, CONTROLLER_PROFILE2,
                globalPrefs.getControllerProfileDefault(2), "",
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile[2] = loadControllerProfile( mPreferences, CONTROLLER_PROFILE3,
                globalPrefs.getControllerProfileDefault(3), "",
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile[3] = loadControllerProfile( mPreferences, CONTROLLER_PROFILE4,
                globalPrefs.getControllerProfileDefault(4), "",
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );

        for(int index = 0; index < NUM_CONTROLLERS; ++index) {

            if(controllerProfile[index] != null) {
                Log.i("GamePrefs", "controler " + index + " profile found: " + controllerProfile[index].getName());
            }else  {
                Log.i("GamePrefs", "controler " + index + " profile NOT found");
            }
        }

        // Player map
        useDefaultPlayerMapping = mPreferences.getBoolean( "useDefaultPlayerMapping", true );
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

        switch (rspSetting) {
            case "rsp-hle":
                rspPluginPath = appData.libsDir + "/libmupen64plus-rsp-hle.so";
                rspHleVideo = true;
                break;
            case "rsp-cxd4-hle":
                rspPluginPath = appData.libsDir + "/libmupen64plus-rsp-cxd4.so";
                rspHleVideo = true;
                break;
            default:
                rspPluginPath = appData.libsDir + "/libmupen64plus-rsp-cxd4.so";
                rspHleVideo = false;
        }


        videoPlugin = new Plugin( emulationProfile, appData.libsDir, "videoPlugin" );

        // Video prefs - gln64
        int maxFrameskip = getSafeInt( emulationProfile, "gln64Frameskip", 0 );
        isGln64AutoFrameskipEnabled = maxFrameskip < 0;
        gln64MaxFrameskip = Math.abs( maxFrameskip );
        isGln64FogEnabled = emulationProfile.get( "gln64Fog", "0" ).equals( "1" );
        isGln64SaiEnabled = emulationProfile.get( "gln64Sai", "0" ).equals( "1" );
        isGln64ScreenClearEnabled = emulationProfile.get( "gln64ScreenClear", "1" ).equals( "1" );
        isGln64AlphaTestEnabled = emulationProfile.get( "gln64AlphaTest", "1" ).equals( "1" );
        isGln64HackDepthEnabled = emulationProfile.get( "gln64HackDepth", "1" ).equals( "1" );

        // Video prefs - rice
        isRiceAutoFrameskipEnabled = emulationProfile.get( "riceAutoFrameskip", "False" ).equals( "True" );
        isRiceFastTextureLoadingEnabled = emulationProfile.get( "riceFastTexture", "False" ).equals( "True" );
        isRiceForceTextureFilterEnabled = emulationProfile.get( "riceForceTextureFilter", "False" ).equals( "True" );
        riceScreenUpdateType = emulationProfile.get( "riceScreenUpdate", "4" );
        riceTextureEnhancement = emulationProfile.get( "riceTextureEnhancement", "0" );
        isRiceHiResTexturesEnabled = emulationProfile.get( "riceHiResTextures", "True" ).equals( "True" );
        isRiceFogEnabled = emulationProfile.get( "riceFog", "False" ).equals( "True" );

        // Video prefs - glide64
        isGlide64Enabled = videoPlugin.name.equals( "libmupen64plus-video-glide64mk2.so" );
        glide64mk2Prefs = new Glide64mk2Prefs(emulationProfile);

        // Video prefs - GLideN64, this is a more broad search because there used to be more than one GLideN64 version
        isGliden64Enabled = videoPlugin.name.contains( "libmupen64plus-video-gliden64" );
        glideN64Prefs = new GLideN64Prefs(context, emulationProfile);

        //Video preferences for angrylion
        boolean isAngrylionEnabled = videoPlugin.name.equals( "libmupen64plus-video-angrylion.so" );
        angrylionVIOverlayEnabled = emulationProfile.get( "VIOverlay", "False" ).equals( "True" );

        final String scaling = mPreferences.getString( "displayScaling", "default" );

        boolean stretch = scaling.equals("default") ? globalPrefs.stretchScreen : scaling.equals( "stretch" );
        boolean gliden64Widescreenhack = emulationProfile.get( "WidescreenHack", "False" ).equals("True") && isGliden64Enabled;

        //Stretch screen if the GLideN64 wide screen hack is enabled and the current video plugin is GLideN64
        final int hResolution = getSafeInt( mPreferences, DISPLAY_RESOLUTION, -1 );

        videoSurfaceWidth = globalPrefs.getResolutionWidth(stretch, gliden64Widescreenhack, 0);
        videoSurfaceHeight = globalPrefs.getResolutionHeight(false, gliden64Widescreenhack, 0);

        //Angrylion only supports 640x480
        videoRenderWidth = isAngrylionEnabled ? 640 : globalPrefs.getResolutionWidth(gliden64Widescreenhack, gliden64Widescreenhack, hResolution);
        videoRenderHeight = isAngrylionEnabled ? 480 : globalPrefs.getResolutionHeight(false, gliden64Widescreenhack, hResolution);

        Log.i("GamePrefs", "render_width=" + videoRenderWidth + " render_height=" + videoRenderHeight);

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
        boolean isControllerShared = mPreferences.getBoolean( "inputShareController", false );

        // Determine which peripheral controllers are enabled
        for(int index = 0; index < NUM_CONTROLLERS; ++index) {

            isControllerEnabled[index] = controllerProfile[index] != null;
        }

        playerMap.setEnabled(!isControllerShared);

        // Determine which players are "plugged in"
        isPlugged[0] = isControllerEnabled[0] || isTouchscreenEnabled;
        isPlugged[1] = isControllerEnabled[1] && (playerMap.isPlayerAvailable(2) || isControllerShared);
        isPlugged[2] = isControllerEnabled[2] && (playerMap.isPlayerAvailable(3) || isControllerShared);
        isPlugged[3] = isControllerEnabled[3] && (playerMap.isPlayerAvailable(4) || isControllerShared);

        //A value of zero means default for the game as specified in mupen64plus.ini
        countPerOp = mPreferences.getInt( "screenAdvancedCountPerOp", 0 );
    }

    private boolean isDpadGame(String headerName, String gameGoodName) {

        String headerNameLowerCase = !TextUtils.isEmpty(headerName) ? headerName.toLowerCase() : "";
        String gameGoodNameLowerCase = !TextUtils.isEmpty(gameGoodName) ? gameGoodName.toLowerCase() : "";

        return headerNameLowerCase.equals("Body Harvest".toLowerCase()) ||
                headerNameLowerCase.equals("DR.MARIO 64".toLowerCase()) ||
                headerNameLowerCase.equals("DUKE NUKEM".toLowerCase()) ||
                headerNameLowerCase.equals("DUKE".toLowerCase()) ||
                headerNameLowerCase.contains("F1 POLE POSITION 64".toLowerCase()) ||
                headerNameLowerCase.equals("Forsaken".toLowerCase()) ||
                headerNameLowerCase.contains("I.S.S.".toLowerCase()) ||
                headerNameLowerCase.contains("I S S".toLowerCase()) ||
                headerNameLowerCase.equals("Kirby64".toLowerCase()) ||
                headerNameLowerCase.equals("MISCHIEF MAKERS".toLowerCase()) ||
                headerNameLowerCase.equals("MS. PAC-MAN MM".toLowerCase()) ||
                headerNameLowerCase.contains("POKEMON STADIUM".toLowerCase()) ||
                headerNameLowerCase.equals("PUZZLE LEAGUE N64".toLowerCase()) ||
                headerNameLowerCase.contains("TONY HAWK".toLowerCase()) ||
                headerNameLowerCase.contains("THPS".toLowerCase()) ||
                headerNameLowerCase.contains("Turok".toLowerCase()) ||
                headerNameLowerCase.equals("VIOLENCEKILLER".toLowerCase()) ||
                headerNameLowerCase.equals("NBA HANGTIME".toLowerCase()) ||
                headerNameLowerCase.contains("WWF".toLowerCase()) ||
                gameGoodNameLowerCase.contains("WRESTL".toLowerCase()) ||
                headerNameLowerCase.equals("wetrix".toLowerCase());
    }

    public String getGameDataDir()
    {
        return gameDataDir;
    }

    public String getAutoSaveDir()
    {
        return autoSaveDir;
    }

    public String getUserSaveDir()
    {
        return userSaveDir;
    }

    public String getCoreUserConfigDir()
    {
        return coreUserConfigDir;
    }

    public String getMupen64plusCfg()
    {
        return mupen64plus_cfg;
    }

    String getMupen64plusCfgAlt()
    {
        return mupen64plus_cfg.replace(":", "");
    }

    public String getSramDataDir()
    {
        return sramDataDir;
    }

    public String getSlotSaveDir()
    {
        return slotSaveDir;
    }

    public String getScreenshotDir()
    {
        return screenshotDir;
    }

    public void useAlternateGameDataDir()
    {
        gameDataDir = getAlternateGameDataPath( romMd5, gameHeaderName, gameCountrySymbol, mAppData);
        setGameDirs(mAppData, mGlobalPrefs, gameDataDir);
    }

    public void useSecondAlternateGameDataDir()
    {
        gameDataDir = getSecondAlternateGameDataPath( romMd5, gameHeaderName, gameCountrySymbol, mAppData);
        setGameDirs(mAppData, mGlobalPrefs, gameDataDir);
    }

    private void setGameDirs(AppData appData, GlobalPrefs globalPrefs, String baseDir)
    {
        autoSaveDir = baseDir + "/" + AUTO_SAVES_DIR;
        coreUserConfigDir = baseDir + "/" + CORE_CONFIG_DIR;
        mupen64plus_cfg = coreUserConfigDir + "/" + MUPEN_CONFIG_FILE;

        if(globalPrefs.useFlatGameDataPath)
        {
            sramDataDir = appData.gameDataDir;
            slotSaveDir = appData.gameDataDir;
            screenshotDir = appData.gameDataDir;
            userSaveDir = appData.gameDataDir;
        }
        else
        {
            sramDataDir = baseDir + "/" + SRAM_DATA_DIR;
            slotSaveDir = baseDir + "/" + SLOT_SAVES_DIR;
            screenshotDir = baseDir + "/" + SCREENSHOTS_DIR;
            userSaveDir = baseDir + "/" + USER_SAVES_DIR;
        }
    }

    String getSharedPrefsName()
    {
        return mSharedPrefsName;
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

    public static String getAlternateGameDataPath( String romMd5, String headerName, String countrySymbol,
                                          AppData appData)
    {
        return String.format( "%s/%s %s %s", appData.gameDataDir, headerName, countrySymbol, romMd5 ).replace(":", "");
    }

    public static String getSecondAlternateGameDataPath( String romMd5, String headerName, String countrySymbol,
                                                   AppData appData)
    {
        return String.format( "%s/%s", appData.gameDataDir, romMd5 );
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

    static int getSafeInt( Profile profile, String key, int defaultValue )
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

    static int getSafeInt( SharedPreferences preferences, String key, int defaultValue )
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
        final Set<Integer> mutableSet = new HashSet<>();
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

    public PakType getPakType(int player )
    {
        return PakType.getPakTypeFromNativeValue(
                Integer.parseInt(getString( KEYTEMPLATE_PAK_TYPE + player, String.valueOf(DEFAULT_PAK_TYPE) )));
    }

    public String getTransferPakRom(int player)
    {
        String romPath = getString( "transferPak" + player + "Rom", "" );

        if (!TextUtils.isEmpty(romPath) && new File(romPath).isDirectory()) {
            romPath = "";
        }
        return romPath;
    }

    public String getTransferPakRam(int player)
    {
        String ramPath = getString( "transferPak" + player + "Ram", "" );

        if (!TextUtils.isEmpty(ramPath) && new File(ramPath).isDirectory()) {
            ramPath = "";
        }
        return ramPath;
    }

    public void putPakType( int player, PakType pakType )
    {
        putString( KEYTEMPLATE_PAK_TYPE + player, String.valueOf(pakType.getNativeValue()) );
    }

    public String getString( String key, String defaultValue )
    {
        return mPreferences.getString( key, defaultValue );
    }

    private void putString( String key, String value )
    {
        mPreferences.edit().putString( key, value ).apply();
    }
}
