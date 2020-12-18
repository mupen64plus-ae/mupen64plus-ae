package paulscode.android.mupen64plusae.persistent;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.input.map.PlayerMap;
import paulscode.android.mupen64plusae.jni.CoreTypes;
import paulscode.android.mupen64plusae.preference.MultiSelectListPreference;
import paulscode.android.mupen64plusae.profile.ControllerProfile;
import paulscode.android.mupen64plusae.profile.Profile;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Plugin;

@SuppressWarnings("WeakerAccess")
public class GamePrefs
{
    static public class CheatSelection
    {
        private int index;
        private int option;

        // Constructor
        CheatSelection(int index, int option){
            this.index = index;
            this.option = option;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getOption() {
            return option;
        }

        public void setOption(int option) {
            this.option = option;
        }

        @NonNull
        @Override
        public String toString() {
            return "CheatSelection{" +
                    "index='" + index + '\'' +
                    ", option='" + option + '\'' +
                    '}';
        }
    }

    /** The name of the game-specific {@link SharedPreferences} object.*/
    private final String mSharedPrefsName;

    /** App data */
    private final AppData mAppData;

    /** Global prefs */
    private final GlobalPrefs mGlobalPrefs;

    /** The name of the parent directory containing all game-specific data files. */
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

    /** The subdirectory returned from the core's ConfigGetUserConfigPath() method. Location of core config file. */
    private String coreUserConfigDir;

    /** The path of the Mupen64Plus base configuration file. */
    private String mupen64plus_cfg;

    /** The emulation profile. */
    public final Profile emulationProfile;

    /** The touchscreen profile. */
    public final Profile touchscreenProfile;

    public static final int NUM_CONTROLLERS = 4;

    /** The input profiles for all player. */
    public final ControllerProfile[] controllerProfile = new ControllerProfile[NUM_CONTROLLERS];

    /** The player map for multi-player gaming. */
    public final PlayerMap playerMap;

    /** The selected R4300 emulator. */
    public final String r4300Emulator;

    /** The selected R4300 emulator. */
    public final boolean disableExpansionPak;

    /** True if we want the RSP to be in HLE video mode, false if LLE */
    public final boolean rspHleVideo;

    public final AppData.VideoPlugin videoPluginLib;

    public final AppData.AudioPlugin audioPluginLib;

    public final AppData.RspPlugin rspPluginLib;

    /** The selected video plug-in. */
    public final Plugin videoPlugin;

    public final Glide64mk2Prefs glide64mk2Prefs;

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

    public final GLideN64Prefs glideN64Prefs;

    public final AngrylionPlusPrefs angrylionPlusPrefs;

    /** True if the touchscreen is enabled. */
    public final boolean isTouchscreenEnabled;

    /** The set of NOT auto-holdable button commands. */
    public final Set<Integer> touchscreenNotAutoHoldables;

    /** Invert the touch controller X axis */
    public final boolean invertTouchXAxis;

    /** Invert the touch controller Y axis */
    public final boolean invertTouchYAxis;

    /** True if the touchscreen joystick is relative. */
    public final boolean isTouchscreenAnalogRelative;

    /** True if the touchscreen overlay is hidden. */
    public final boolean isTouchscreenHidden;

    /** True to activate sensor on game start */
    public final boolean sensorActivateOnStart;

    /** The sensor values used for X axis emulation */
    public final String sensorAxisX;

    /** The sensor's X axis sensitivity (%), may be negative to invert axes */
    public final int sensorSensitivityX;

    /** The sensor values used for Y axis emulation */
    public final String sensorAxisY;

    /** The sensor's Y axis sensitivity (%), may be negative to invert axes */
    public final int sensorSensitivityY;

    /** True if we want use default player mapping*/
    final boolean useDefaultPlayerMapping;

    /** True if a specific controller is enabled. */
    public final boolean[] isControllerEnabled = new boolean[NUM_CONTROLLERS];

    /** True if any type of AbstractController is enabled for all players. */
    public final boolean[] isPlugged = new boolean[NUM_CONTROLLERS];

    /** True if one controller can control multiple players */
    public final boolean isControllerShared;

    /** True if the touchscreen joystick is hidden when sensor is enabled. */
    public final boolean isAnalogHiddenWhenSensor;

    /** Selectd display scale for this specific game */
    public final GlobalPrefs.DisplayScaling displayScaling;

    /** Selected horizontal game resolution */
    public final int verticalRenderResolution;

    /** True if we want use default zoom */
    final boolean useDefaultZoom;

    /** The zoom value applied to the viewing surface, in percent. */
    public final int videoSurfaceZoom;

    /** Core CountPerOp setting */
    public final int countPerOp;

    /** Force alignment of PI DMA */
    public final int forceAlignmentOfPiDma;

    /** True if we should ignore TLB exceptions */
    public final boolean ignoreTlbExceptions;

    /** The method used for auto holding buttons. */
    public final int touchscreenAutoHold;

    /** Enable 64DD support */
    public final boolean enable64DdSupport;

    /** 64DD IDL path */
    public final String idlPath64Dd;

    /** 64DD Disk path */
    public final String diskPath64Dd;

    /** Current state slot */
    public final int currentStateSlot;

    /** This is true if this game uses the D-pad */
    final boolean isDpadGame;

    /** This is true if this game accepts 64DD expansion disks */
    final boolean is64DdGame;

    /** Game CRC */
    private final String gameCrc;

    /** ROM MD5 */
    private final String romMd5;

    private final SharedPreferences mPreferences;

    /** Profile keys */
    private static final String DISPLAY_RESOLUTION = "displayResolutionGame";
    static final String EMULATION_PROFILE = "emulationProfile";
    static final String TOUCHSCREEN_PROFILE = "touchscreenProfileGame";
    static final String CONTROLLER_PROFILE1 = "controllerProfile1Game";
    static final String CONTROLLER_PROFILE2 = "controllerProfile2Game";
    static final String CONTROLLER_PROFILE3 = "controllerProfile3Game";
    static final String CONTROLLER_PROFILE4 = "controllerProfile4Game";
    static final String PLAYER_MAP = "playerMapGame";
    static final String DISPLAY_ZOOM = "displayZoomSeekGame";
    static final String PLAY_SHOW_CHEATS = "playShowCheats";
    static final String SUPPORT_64DD = "support64dd";
    static final String IDL_PATH_64DD = "idlPath64dd";
    static final String DISK_PATH_64DD = "diskPath64dd";
    static final String TRANSFER_PAK = "transferPak";
    static final String CURRENT_SLOT = "currentSlot";
    static final String CHANGE_COVERT_ART = "changeCoverArt";
    static final String CLEAR_COVERT_ART = "clearCoverArt";

    /**
     * Directories and file names
     */
    private static final String SRAM_DATA_DIR = "SramData";
    public static final String AUTO_SAVES_DIR = "AutoSaves";
    private static final String SLOT_SAVES_DIR = "SlotSaves";
    private static final String USER_SAVES_DIR = "UserSaves";
    private static final String CORE_CONFIG_DIR = "CoreConfig";
    private static final String MUPEN_CONFIG_FILE = "mupen64plus.cfg";


    private static final String KEYTEMPLATE_PAK_TYPE = "inputPakType";
    private static final int DEFAULT_PAK_TYPE = CoreTypes.PakType.MEMORY.ordinal();

    public GamePrefs( Context context, String md5, String crc, String headerName, String goodName,
        String countrySymbol, AppData appData, GlobalPrefs globalPrefs)
    {
        mAppData = appData;
        mGlobalPrefs = globalPrefs;
        gameHeaderName = headerName;
        gameGoodName = goodName;
        gameCountrySymbol = countrySymbol;
        romMd5 = md5;
        gameCrc = crc;
        mSharedPrefsName = romMd5.replace(' ', '_' ) + "_preferences";
        mPreferences = context.getSharedPreferences( mSharedPrefsName, Context.MODE_PRIVATE );

        // Game-specific data
        gameDataDir = getGameDataPath( romMd5, headerName, countrySymbol);
        setGameDirs(appData, globalPrefs, getGameDataDir());

        isDpadGame = isDpadGame(headerName, goodName);
        is64DdGame = is64ddGame(headerName);

        // Emulation profile
        Profile tempEmulationProfile = loadProfile( mPreferences, EMULATION_PROFILE,
                globalPrefs.getEmulationProfileDefault(), GlobalPrefs.DEFAULT_EMULATION_PROFILE_DEFAULT,
                globalPrefs.GetEmulationProfilesConfig(), appData.GetEmulationProfilesConfig() );

        if (tempEmulationProfile == null) {
            //This is a bad situation, app data must be corrupt
            actionReloadAssets(context);

            //Try again
            tempEmulationProfile = loadProfile( mPreferences, EMULATION_PROFILE,
                    globalPrefs.getEmulationProfileDefault(), GlobalPrefs.DEFAULT_EMULATION_PROFILE_DEFAULT,
                    globalPrefs.GetEmulationProfilesConfig(), appData.GetEmulationProfilesConfig() );
        }
        // This should never happen
        if (tempEmulationProfile == null) {
            Log.e("GamePrefs", "This should never happen!");
            tempEmulationProfile = new Profile(true, "None", "Profile not found");
        }

        emulationProfile = tempEmulationProfile;
        
        Log.i("GamePrefs", "emulation profile found: " + emulationProfile.getName());

        // Touchscreen profile
        if(globalPrefs.isBigScreenMode)
        {
            touchscreenProfile =  null;
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
                globalPrefs.getControllerProfileDefault(1),
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile[1] = loadControllerProfile( mPreferences, CONTROLLER_PROFILE2,
                globalPrefs.getControllerProfileDefault(2),
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile[2] = loadControllerProfile( mPreferences, CONTROLLER_PROFILE3,
                globalPrefs.getControllerProfileDefault(3),
                globalPrefs.GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile[3] = loadControllerProfile( mPreferences, CONTROLLER_PROFILE4,
                globalPrefs.getControllerProfileDefault(4),
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

        if( useDefaultPlayerMapping) {
            playerMapString = globalPrefs.getString(GlobalPrefs.PLAYER_MAP, "");
            Log.i("GamePrefs", "Using default player mapping");
        }

        playerMap = new PlayerMap( globalPrefs.autoPlayerMapping, playerMapString );

        // Emulation prefs
        r4300Emulator = emulationProfile.get( "r4300Emulator", "2" );
        disableExpansionPak = emulationProfile.get( "DisableExtraMem", "False" ).equals( "True" );
        String rspSetting = emulationProfile.get( "rspSetting", "rsp-hle" );

        rspPluginLib = AppData.RspPlugin.getPlugin(rspSetting);
        rspHleVideo = rspPluginLib.isHle();
        videoPlugin = new Plugin( emulationProfile, "videoPlugin" );
        videoPluginLib = AppData.VideoPlugin.getPlugin(videoPlugin.name);
        audioPluginLib = AppData.AudioPlugin.getPlugin(mGlobalPrefs);

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
        glide64mk2Prefs = new Glide64mk2Prefs(emulationProfile);

        // Video prefs - GLideN64, this is a more broad search because there used to be more than one GLideN64 version
        glideN64Prefs = new GLideN64Prefs(context, emulationProfile);

        //Video preferences for angrylion
        angrylionPlusPrefs = new AngrylionPlusPrefs(context, emulationProfile);

        boolean gliden64Widescreenhack = emulationProfile.get( "WidescreenHack", "False" ).equals("True") && videoPluginLib == AppData.VideoPlugin.GLIDEN64;

        final String scaling = mPreferences.getString( "displayScalingGame", "default" );
        //Stretch screen if the GLideN64 wide screen hack is enabled and the current video plugin is GLideN64
        displayScaling = gliden64Widescreenhack ? GlobalPrefs.DisplayScaling.STRETCH :
                scaling.equals("default") ? globalPrefs.displayScaling : GlobalPrefs.DisplayScaling.getScaling(scaling);

        //Angrylion only supports 640x480
        verticalRenderResolution = videoPluginLib == AppData.VideoPlugin.ANGRYLION ? 480 : getSafeInt( mPreferences, DISPLAY_RESOLUTION, -1 );

        useDefaultZoom = mPreferences.getBoolean( "useDefaultZoom", true );
        videoSurfaceZoom = useDefaultZoom ? mGlobalPrefs.videoSurfaceZoom :
                mPreferences.getInt( DISPLAY_ZOOM, 100 );

        // Touchscreen prefs
        isTouchscreenEnabled = touchscreenProfile != null;

        if ( isTouchscreenEnabled )
        {
            // Determine the touchscreen auto-holdables
            touchscreenNotAutoHoldables = getSafeIntSet( touchscreenProfile, "touchscreenNotAutoHoldables" );

            //Axis inversion
            invertTouchXAxis = touchscreenProfile.get( "invertTouchXAxis", "False" ).equals( "True" );
            invertTouchYAxis = touchscreenProfile.get( "invertTouchYAxis", "False" ).equals( "True" );

            // Sensor prefs
            isAnalogHiddenWhenSensor = touchscreenProfile.get("touchscreenHideAnalogWhenSensor", "False").equals( "True" );
            sensorActivateOnStart = touchscreenProfile.get("sensorActivateOnStart", "False").equals( "True" );
            sensorAxisX = touchscreenProfile.get("sensorAxisX", "");
            int sensitivity;
            try {
                sensitivity = Integer.parseInt(touchscreenProfile.get("sensorSensitivityX"));
            } catch (final NumberFormatException ex) {
                sensitivity = 100;
            }
            if (Boolean.parseBoolean(touchscreenProfile.get("sensorInvertX"))) {
                sensitivity = -sensitivity;
            }
            sensorSensitivityX = sensitivity;
            sensorAxisY = touchscreenProfile.get("sensorAxisY", "");
            try {
                sensitivity = Integer.parseInt(touchscreenProfile.get("sensorSensitivityY"));
            } catch (final NumberFormatException ex) {
                sensitivity = 100;
            }
            if (Boolean.parseBoolean(touchscreenProfile.get("sensorInvertY"))) {
                sensitivity = -sensitivity;
            }
            sensorSensitivityY = sensitivity;
        }
        else
        {
            touchscreenNotAutoHoldables = null;
            invertTouchXAxis = false;
            invertTouchYAxis = false;

            isAnalogHiddenWhenSensor = false;
            sensorActivateOnStart = false;
            sensorAxisX = "";
            sensorSensitivityX = 100;
            sensorAxisY = null;
            sensorSensitivityY = 100;
        }

        isTouchscreenHidden = !isTouchscreenEnabled || globalPrefs.touchscreenTransparency == 0;

        int tmpTouchscreenAutoHold = getSafeInt( mPreferences, "touchscreenAutoHoldGame", -1 );

        if(tmpTouchscreenAutoHold == -1)
        {
            tmpTouchscreenAutoHold = globalPrefs.touchscreenAutoHold;
        }

        touchscreenAutoHold = tmpTouchscreenAutoHold;

        enable64DdSupport = mPreferences.getBoolean( SUPPORT_64DD, false );

        if (enable64DdSupport) {
            String tempIdlPath64dd = mPreferences.getString(IDL_PATH_64DD, "");

            if (!TextUtils.isEmpty(tempIdlPath64dd) && new File(tempIdlPath64dd).isDirectory()) {
                tempIdlPath64dd = "";
            }

            idlPath64Dd = tempIdlPath64dd;

            String temp64DdDiskPath = mPreferences.getString(DISK_PATH_64DD, "");

            if (!TextUtils.isEmpty(temp64DdDiskPath) && new File(temp64DdDiskPath).isDirectory()) {
                temp64DdDiskPath = "";
            }

            diskPath64Dd = temp64DdDiskPath;
        } else {
            idlPath64Dd = "";
            diskPath64Dd = "";
        }

        currentStateSlot = mPreferences.getInt(CURRENT_SLOT, 0);

        // Relative touchscreen joystick
        final String tmpTouchscreenAnalogRelative = mPreferences.getString( "touchscreenAnalogRelative_game", "default" );
        isTouchscreenAnalogRelative = tmpTouchscreenAnalogRelative.equals("default") ? globalPrefs.isTouchscreenAnalogRelative : tmpTouchscreenAnalogRelative.equals( "Yes" );

        // Peripheral share mode
        final String tmpControllerShared = mPreferences.getString( "inputShareController2", "default" );
        isControllerShared = tmpControllerShared.equals("default") ? globalPrefs.isControllerShared : tmpControllerShared.equals( "Yes" );

        // Determine which peripheral controllers are enabled
        for(int index = 0; index < NUM_CONTROLLERS; ++index) {

            isControllerEnabled[index] = controllerProfile[index] != null;
        }

        playerMap.setEnabled(!isControllerShared);

        // Determine which players are "plugged in", player 1 will be enabled by default, although, it can
        // become unplugged later after it's mapped for the first time
        isPlugged[0] = isControllerEnabled[0] || isTouchscreenEnabled;
        isPlugged[1] = isControllerEnabled[1] && (playerMap.isPlayerAvailable(2) || isControllerShared || globalPrefs.allEmulatedControllersPlugged);
        isPlugged[2] = isControllerEnabled[2] && (playerMap.isPlayerAvailable(3) || isControllerShared || globalPrefs.allEmulatedControllersPlugged);
        isPlugged[3] = isControllerEnabled[3] && (playerMap.isPlayerAvailable(4) || isControllerShared || globalPrefs.allEmulatedControllersPlugged);

        //A value of zero means default for the game as specified in mupen64plus.ini
        countPerOp = mPreferences.getInt( "screenAdvancedCountPerOp", 0 );

        forceAlignmentOfPiDma = mPreferences.getBoolean( "screenAdvancedforceAlignmentOfPiDma", true ) ? -1 : 0;
        ignoreTlbExceptions = headerName.toLowerCase().contains("zelda") || mPreferences.getBoolean( "screenAdvancedignoreTlbExceptions", false );
    }

    private void actionReloadAssets(Context context)
    {
        FileUtil.deleteFolder(new File(mAppData.coreSharedDataDir));
        mAppData.putAssetCheckNeeded(true);
        ActivityHelper.startSplashActivity(context);
    }

    private boolean isDpadGame(String headerName, String gameGoodName) {

        String headerNameLowerCase = !TextUtils.isEmpty(headerName) ? headerName.toLowerCase() : "";
        String gameGoodNameLowerCase = !TextUtils.isEmpty(gameGoodName) ? gameGoodName.toLowerCase() : "";

        return headerNameLowerCase.equals("Body Harvest".toLowerCase()) ||
                headerNameLowerCase.equals("BOMBERMAN64".toLowerCase()) ||
                headerNameLowerCase.equals("DARK RIFT".toLowerCase()) ||
                headerNameLowerCase.equals("DR.MARIO 64".toLowerCase()) ||
                headerNameLowerCase.equals("DUKE NUKEM".toLowerCase()) ||
                headerNameLowerCase.equals("DUKE".toLowerCase()) ||
                headerNameLowerCase.contains("F1 POLE POSITION 64".toLowerCase()) ||
                headerNameLowerCase.equals("Forsaken".toLowerCase()) ||
                headerNameLowerCase.contains("I.S.S.".toLowerCase()) ||
                headerNameLowerCase.contains("I S S".toLowerCase()) ||
                headerNameLowerCase.equals("Kirby64".toLowerCase()) ||
                headerNameLowerCase.equals("MGAH VOL1".toLowerCase()) ||
                headerNameLowerCase.equals("MISCHIEF MAKERS".toLowerCase()) ||
                headerNameLowerCase.equals("MS. PAC-MAN MM".toLowerCase()) ||
                headerNameLowerCase.contains("POKEMON STADIUM".toLowerCase()) ||
                headerNameLowerCase.equals("PUZZLE LEAGUE N64".toLowerCase()) ||
                headerNameLowerCase.contains("TETRIS".toLowerCase()) ||
                headerNameLowerCase.contains("TONY HAWK".toLowerCase()) ||
                headerNameLowerCase.contains("THPS".toLowerCase()) ||
                headerNameLowerCase.contains("Turok".toLowerCase()) ||
                headerNameLowerCase.equals("VIOLENCEKILLER".toLowerCase()) ||
                headerNameLowerCase.equals("NBA HANGTIME".toLowerCase()) ||
                headerNameLowerCase.contains("WWF".toLowerCase()) ||
                gameGoodNameLowerCase.contains("WRESTL".toLowerCase()) ||
                gameGoodNameLowerCase.contains("WCW".toLowerCase()) ||
                headerNameLowerCase.equals("wetrix".toLowerCase());
    }

    private boolean is64ddGame(String headerName) {

        String headerNameLowerCase = !TextUtils.isEmpty(headerName) ? headerName.toLowerCase() : "";

        return headerNameLowerCase.equals("MarioParty".toLowerCase()) ||
                headerNameLowerCase.contains("POKEMON STADIUM".toLowerCase()) ||
                headerNameLowerCase.contains("F-ZERO".toLowerCase()) ||
                headerNameLowerCase.equals("DEZAEMON3D".toLowerCase()) ||
                headerNameLowerCase.equals("THE LEGEND OF ZELDA".toLowerCase());
    }

    public String getGameDataDir()
    {
        return mAppData.gameDataDir + "/" + gameDataDir;
    }

    public String getGameDataDirName()
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

    public void useAlternateGameDataDir()
    {
        gameDataDir = getAlternateGameDataPath( romMd5, gameHeaderName, gameCountrySymbol);
        setGameDirs(mAppData, mGlobalPrefs, getGameDataDir());
    }

    public void useSecondAlternateGameDataDir()
    {
        gameDataDir = getSecondAlternateGameDataPath( romMd5);
        setGameDirs(mAppData, mGlobalPrefs, getGameDataDir());
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
            userSaveDir = appData.gameDataDir;
        }
        else
        {
            sramDataDir = baseDir + "/" + SRAM_DATA_DIR;
            slotSaveDir = baseDir + "/" + SLOT_SAVES_DIR;
            userSaveDir = baseDir + "/" + USER_SAVES_DIR;
        }
    }

    String getSharedPrefsName()
    {
        return mSharedPrefsName;
    }

    public ArrayList<CheatSelection> getEnabledCheats()
    {
        ArrayList<CheatSelection> cheatSelection = new ArrayList<>();

        final Pattern pattern = Pattern.compile( "^" + gameCrc + " Cheat(\\d+)" );
        final Map<String, ?> map = mPreferences.getAll();
        for (final String key : map.keySet())
        {
            final Matcher matcher = pattern.matcher( key );

            if ( matcher.matches() && matcher.groupCount() > 0 )
            {
                final int value = mPreferences.getInt( key, 0 );
                if (value > 0)
                {
                    String indexString = matcher.group(1);

                    if (!TextUtils.isEmpty(indexString) && indexString != null)
                    {
                        final int index = Integer.parseInt( indexString );
                        cheatSelection.add(new CheatSelection(index, value-1));
                    }
                }
            }
        }
        return cheatSelection;
    }

    public static String getGameDataPath( String romMd5, String headerName, String countrySymbol)
    {
        headerName = TextUtils.isEmpty(headerName) ? "" : headerName;
        return String.format( "%s %s %s", headerName.replace("/", ""), countrySymbol, romMd5 );
    }

    public static String getAlternateGameDataPath( String romMd5, String headerName, String countrySymbol)
    {
        return String.format( "%s %s %s", headerName, countrySymbol, romMd5 ).replace(":", "");
    }

    public static String getSecondAlternateGameDataPath( String romMd5)
    {
        return String.format( "%s", romMd5 );
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
                                                            ConfigFile custom, ConfigFile builtin )
    {
        final String name = prefs.getString( key, defaultName );

        Log.i("GamePrefs", "Profile: " +
                " key=" + key +
                " defaultName=" + defaultName +
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
            String stringReturn = preferences.getString( key, String.valueOf( defaultValue ));
            return Integer.parseInt(stringReturn);
        }
        catch( final NumberFormatException ex )
        {
            return defaultValue;
        }
    }

    @SuppressWarnings("SameParameterValue")
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

    public CoreTypes.PakType getPakType(int player )
    {
        return CoreTypes.PakType.getPakTypeFromNativeValue(
                Integer.parseInt(getString( KEYTEMPLATE_PAK_TYPE + player, String.valueOf(DEFAULT_PAK_TYPE) )));
    }

    public String getTransferPakRomKey(int player)
    {
        return TRANSFER_PAK + player + "Rom_v2";
    }

    public String getTransferPakRamKey(int player)
    {
        return TRANSFER_PAK + player + "Ram_v2";
    }

    public String getTransferPakRom(int player)
    {
        String romPath = getString( getTransferPakRomKey(player), "" );

        if (!TextUtils.isEmpty(romPath) && new File(romPath).isDirectory()) {
            romPath = "";
        }
        return romPath;
    }

    public String getTransferPakRam(int player)
    {
        String ramPath = getString( getTransferPakRamKey(player), "" );

        if (!TextUtils.isEmpty(ramPath) && new File(ramPath).isDirectory()) {
            ramPath = "";
        }
        return ramPath;
    }

    public void putPakType( int player, CoreTypes.PakType pakType )
    {
        putString( KEYTEMPLATE_PAK_TYPE + player, String.valueOf(pakType.ordinal()) );
    }

    public String getString( String key, String defaultValue )
    {
        return mPreferences.getString( key, defaultValue );
    }

    public void putString( String key, String value )
    {
        mPreferences.edit().putString( key, value ).apply();
    }

    public void putCurrentSlot(int slot) {
        mPreferences.edit().putInt( CURRENT_SLOT, slot ).apply();
    }
}
