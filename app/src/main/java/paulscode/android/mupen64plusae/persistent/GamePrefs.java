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

    public final Glide64mk2Prefs glide64mk2Prefs;

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

    /** The method used for auto holding buttons. */
    public final int touchscreenAutoHold;

    /** Game CRC */
    public final String gameCrc;

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

        mSharedPrefsName = romMd5.replace(' ', '_' ) + "_preferences";
        mPreferences = context.getSharedPreferences( mSharedPrefsName, Context.MODE_PRIVATE );

        // Game-specific data
        gameDataDir = getGameDataPath( romMd5, headerName, countrySymbol, appData);
        setGameDirs(appData, globalPrefs, gameDataDir);

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
        glide64mk2Prefs = new Glide64mk2Prefs(emulationProfile);

        // Video prefs - GLideN64, this is a more broad search because there used to be more than one GLideN64 version
        isGliden64Enabled = videoPlugin.name.contains( "libmupen64plus-video-gliden64" );
        glideN64Prefs = new GLideN64Prefs(context, emulationProfile);

        //Video preferences for angrylion
        isAngrylionEnabled = videoPlugin.name.equals( "libmupen64plus-video-angrylion.so" );
        angrylionVIOverlayEnabled = emulationProfile.get( "VIOverlay", "False" ).equals( "True" );

        final String scaling = mPreferences.getString( "displayScaling", "default" );

        mStretch = scaling.equals("default") ? globalPrefs.stretchScreen : scaling.equals( "stretch" );
        boolean gliden64Widescreenhack = emulationProfile.get( "WidescreenHack", "False" ).equals("True") && isGliden64Enabled;

        //Stretch screen if the GLideN64 wide screen hack is enabled and the current video plugin is GLideN64
        final int hResolution = getSafeInt( mPreferences, DISPLAY_RESOLUTION, -1 );

        videoSurfaceWidth = globalPrefs.getResolutionWidth(mStretch, gliden64Widescreenhack, 0);
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

    private void setGameDirs(AppData appData, GlobalPrefs globalPrefs, String baseDir)
    {
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
    }

    public String getSharedPrefsName()
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

    public static int getSafeInt( Profile profile, String key, int defaultValue )
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

    public static int getSafeInt( SharedPreferences preferences, String key, int defaultValue )
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
