/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 *
 * Copyright (C) 2013 Paul Lamb
 *
 * This file is part of Mupen64PlusAE.
 *
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.persistent;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import androidx.preference.PreferenceManager;

import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.WordUtils;
import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.game.ShaderLoader;
import paulscode.android.mupen64plusae.persistent.AppData.HardwareInfo;
import paulscode.android.mupen64plusae.profile.ControllerProfile;
import paulscode.android.mupen64plusae.profile.ManageControllerProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageEmulationProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageTouchscreenProfilesActivity;
import paulscode.android.mupen64plusae.util.CountryCode;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;
import paulscode.android.mupen64plusae.util.SafeMethods;

import static java.lang.Integer.parseInt;

/**
 * A convenience class for quickly, safely, and consistently retrieving typed user preferences.
 * <p>
 * <b>Developers:</b> After creating a preference in /res/xml/preferences.xml, you are encouraged to
 * provide convenient access to it by expanding this class. Although this adds an extra step to
 * development, it simplifies code maintenance later since all maintenance can be consolidated to a
 * single file. For example, if you change the name of a key, you only need to update one line in
 * this class:
 *
 * <pre>
 * {@code
 * myPreference = mPreferences.getString( "myOldKey", "myFallbackValue" );
 *            --> mPreferences.getString( "myNewKey", "myFallbackValue" );
 * }
 * </pre>
 *
 * Without this class, you would need to search through the entire code base for every call to
 * getString( "myOldKey", ... ) and update each one. This class also ensures that the same fallback
 * value will be used everywhere. A third advantage is that you can easily provide frequently-used
 * "derived" preferences, as in
 *
 * <pre>
 * {@code
 * isMyPreferenceValid = ( myPreference != null ) && ( myPreference.field != someBadValue );
 * }
 * </pre>
 *
 * Finally, the cost of looking up a preference value is made up front in this class's constructor,
 * rather than at the point of use. This could improve application performance if the value is used
 * often, such as the frame refresh loop of a game.
 */
@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal"})
public class GlobalPrefs
{
    public enum DisplayScaling
    {
        ORIGINAL("original"),
        STRETCH("stretch"),
        STRETCH_169("stretch169");

        private final String text;

        DisplayScaling(String text) {
            this.text = text;
        }

        public String getValue() {
            return text;
        }

        public static DisplayScaling getScaling(String input)
        {
            DisplayScaling[] values = DisplayScaling.values();
            for (DisplayScaling value : values)
                if (value.getValue().equals(input))
                    return value;
            return DisplayScaling.ORIGINAL;
        }
    }

    /** The subdirectory containing cover art files. */
    public final String coverArtDir;

    /** The subdirectory containing legacy cover art files. */
    public final String legacyCoverArtDir;

    /** Location of user generated configuration data */
    public final String profilesDir;

    /** Legacy location of user generated configuration data */
    public final String legacyProfilesDir;

    /** The subdirectory containing screenshot files. */
    public final String screenshotsDir;

    /** The subdirectory returned from the core's ConfigGetUserDataPath() method. */
    public final String coreUserDataDir;

    /** The subdirectory returned from the core's ConfigGetUserCachePath() method. */
    public final String coreUserCacheDir;

    /** The subdirectory where hi-res textures must be unzipped. */
    public final String hiResTextureDir;

    /** Directory where texture cache htc files are stored */
    public final String textureCacheDir;

    /** Directory where shader cache files are stored */
    public final String shaderCacheDir;

    /** The directory containing all custom touchscreen skin folders. */
    public final String touchscreenCustomSkinsDir;

    /** Directory where user can place ROMs if system doesn't have SAF */
    public final String externalRomsDirNoSaf;

    /** The legacy directory containing all custom touchscreen skin folders. */
    public final String legacyTouchscreenCustomSkinsDir;

    /** Legacy core config folder */
    public final String legacyCoreConfigDir;

    /** The path of the rom info cache for the gallery. */
    public final String romInfoCacheCfg;

    /** The path of the legacy rom info cache for the gallery. */
    public final String legacyRomInfoCacheCfg;

    /** The path of the custom controller profiles file. */
    public final String controllerProfiles_cfg;

    /** The path of the custom touchscreen profiles file. */
    public final String touchscreenProfiles_cfg;

    /** The path of the custom emulation profiles file. */
    public final String emulationProfiles_cfg;

    /** The controller profiles config */
    private ConfigFile mControllerProfilesConfig = null;

    /** The touchscreen profiles config */
    private ConfigFile mTouchscreenProfilesConfig = null;

    /** The emulation profiles config */
    private ConfigFile mEmulationProfilesConfig = null;

    /** The path of the user's custom cheat files. */
    public final String customCheats_txt;

    /** True if the touchscreen feedback is enabled. */
    public final boolean isTouchscreenFeedbackEnabled;

    /** The touchscreen transparency value. */
    public final int touchscreenTransparency;

    /** Factor applied to the final calculated visible touchmap scale. */
    public final float touchscreenScale;

    /** The method used for auto holding buttons. */
    final int touchscreenAutoHold;

    /** Enable touchscreen auto-hide */
    public final boolean touchscreenAutoHideEnabled;

    /** How long before auto hiding touchscreen buttons */
    public final int touchscreenAutoHideSeconds;

    /** True if the touchscreen joystick is animated. */
    public final boolean isTouchscreenAnimated;

    /** True if the touchscreen joystick is relative. */
    public final boolean isTouchscreenAnalogRelative;

    /** Current touchscreen skin selection. */
    public final String touchscreenSkin;

    /** The directory of the selected touchscreen skin. */
    public final String touchscreenSkinPath;

    /** True if we are using a custom touchscreen skin */
    public final boolean isCustomTouchscreenSkin;

    /** The set of key codes that are not allowed to be mapped. **/
    public final List<Integer> unmappableKeyCodes;

    /** True if the recently played section of the gallery should be shown. */
    public final boolean isRecentShown;

    /** True if we are sorting by ROM name */
    public final boolean sortByRomName;

    /** True if the full ROM rip info should be shown. */
    public final boolean isFullNameShown;

    /** Factor applied to the cover art scale */
    public final float coverArtScale;

    /** Which country codes we are allow to show */
    private final LinkedHashSet<CountryCode> allowedCountryCodes = new LinkedHashSet<>();

    /** Default resolution */
    public final int displayResolution;

    /** The zoom value applied to the viewing surface, in percent. */
    public final int videoSurfaceZoom;

    /** Surface scale facor for shaders */
    public final int shaderScaleFactor;

    /** User selected shader passes */
    private final ArrayList<String> shaderPasses;

    /** Display scaling */
    final DisplayScaling displayScaling;

    /** The screen orientation for the game activity. */
    public final int displayOrientation;

    /** The action bar transparency value. */
    public final int displayActionBarTransparency;

    /** True if the FPS indicator is displayed. */
    public final boolean isFpsEnabled;

    /** FPS display x position */
    public final int fpsXPosition;

    /** FPS display y position */
    public final int fpsYPosition;

    /** True if immersive mode should be used (KitKat only). */
    public final boolean isImmersiveModeEnabled;

    /** True if framelimiter is used. */
    public final boolean isFramelimiterEnabled;

    /** True if Android polygon offset hack is enabled. **/
    public final boolean isPolygonOffsetHackEnabled;

    /** The manually-overridden hardware type, used for flicker reduction. */
    final int videoHardwareType;

    /** The polygon offset to use. */
    public final float videoPolygonOffset;

    /** Enable threading in GLideN64*/
    public final boolean threadedGLideN64;

    /** Enable GLideN64 hybrid texture filter*/
    public final boolean hybridTextureFilterGLideN64;

    /** True if the left and right audio channels are swapped. */
    public final boolean audioSwapChannels;

    /** Stretch audio to prevent crackling in audio plugin */
    public final boolean enableAudioTimeSretching;

    /** Size of hardware buffer in output samples. This is a hardware buffer size, which directly affects latency. */
    public final int audioHardwareBufferSize;

    /** Audio volume percent */
    public final int audioVolume;

    /** Audio buffer size in milliseconds. */
    public final int audioBufferSizeMs;

    /** Audio sampling rate. */
    public final int audioSamplingRate;

    /** Audio sampling type, 0=trivial, 1=soundtouch. */
    public final int audioSamplingType;

    /** Use audio floating point samples */
    public final boolean audioFloatingPoint;

    /** Use low performance audio mode, for slow devices */
    public final boolean lowPerformanceAudio;

    /** True if big-screen navigation mode is enabled. */
    public final boolean isBigScreenMode;

    /** True if we are using the swipe gesture for the in-game menu, false if we are using the back key */
    public final boolean inGameMenuIsSwipGesture;

    /** Maximum number of auto saves */
    public final int maxAutoSaves;

    /** True if specific game data should be saved in a flat file structure */
    final boolean useFlatGameDataPath;

    /** True if the app should use external storage to save game data */
    public final boolean useExternalStorge;

    /** Where to store external game data */
    public final String externalFileStoragePath;

    /** Japanese IPL ROM path */
    public final String japanIplPath;

    /** True of volume keys are mappable*/
    public final boolean volKeysMappable;

    /** The input profile for Player 1. */
    final ControllerProfile controllerProfile1;

    /** The input profile for Player 2. */
    final ControllerProfile controllerProfile2;

    /** The input profile for Player 3. */
    final ControllerProfile controllerProfile3;

    /** The input profile for Player 4. */
    final ControllerProfile controllerProfile4;

    /** True if auto player mapping is enabled */
    final boolean autoPlayerMapping;

    /** True if we want to tell the cores 4 N64 controllers are always plugged in
     * regardless if 4 controllers are actually attached.
     */
    public final boolean allEmulatedControllersPlugged;

    /** True if one controller can control multiple players */
    final boolean isControllerShared;

    /** True if we want to show built in emulation profiles */
    final boolean showBuiltInEmulationProfiles;

    /** True if we want to show built in touchscreen profiles */
    final boolean showBuiltInTouchscreenProfiles;

    /** True if we want to show built in touchscreen profiles */
    final boolean showBuiltInControllerProfiles;

    /** True to use a high priority thread for the core */
    public final boolean useHighPriorityThread;

    /** True if we should use Raphnet devices if available */
    public final boolean useRaphnetDevicesIfAvailable;

    /** True if we should hold controller buttons for a certain amount of time for
     * some functions to take effect */
    public final boolean holdControllerBottons;

    /** True if we should use UPnP or NAT/PMP to map external ports */
    public final boolean useUpnpToMapNetplayPorts;

    /** Room TCP port number */
    public final int netplayRoomTcpPort;

    /** Server UDP/TCP port number */
    public final int netplayServerUdpTcpPort;

    // Shared preferences keys and key templates
    static final String KEY_EMULATION_PROFILE_DEFAULT = "emulationProfileDefault";
    static final String KEY_TOUCHSCREEN_PROFILE_DEFAULT = "touchscreenProfileDefault";
    static final String KEY_TOUCHSCREEN_DPAD_PROFILE_DEFAULT = "touchscreenProfileDpadDefault";
    public static final String KEY_LOCALE_OVERRIDE = "localeOverride";
    public static final String KEY_TOUCHSCREEN_SKIN_CUSTOM_PATH = "touchscreenCustomSkin";
    public static final String KEY_SHADER_PASS = "shaderPass";
    public static final String CONTROLLER_PROFILE1 = "controllerProfile1";
    public static final String CONTROLLER_PROFILE2 = "controllerProfile2";
    public static final String CONTROLLER_PROFILE3 = "controllerProfile3";
    public static final String CONTROLLER_PROFILE4 = "controllerProfile4";
    public static final String PLAYER_MAP = "playerMap";
    public static final String GAME_DATA_STORAGE_TYPE = "gameDataStorageType";
    public static final String PATH_GAME_SAVES = "gameDataStoragePath";
    public static final String PATH_JAPAN_IPL_ROM = "japanIdlPath64dd";

    public static final String ROOM_TCP_PORT = "roomTcpPort";
    public static final String SERVER_UDP_TCP_PORT = "serverTcpUdpPort";

    public static final String AUDIO_SAMPLING_TYPE = "audioSamplingType";
    public static final String AUDIO_LOW_PERFORMANCE_MODE = "lowPerformanceMode";
    // ... add more as needed

    // Shared preferences default values
    static final String DEFAULT_EMULATION_PROFILE_DEFAULT = "Glide64-Fast";
    public static final String DEFAULT_TOUCHSCREEN_PROFILE_DEFAULT = "Analog";
    public static final String DEFAULT_TOUCHSCREEN_DPAD_PROFILE_DEFAULT = "Everything";
    static final String DEFAULT_CONTROLLER_PROFILE_DEFAULT = "Android Gamepad";
    public static final String DEFAULT_LOCALE_OVERRIDE = "";
    // ... add more as needed

    private final SharedPreferences mPreferences;
    private final String mLocaleCode;
    private final String[] mLocaleNames;
    private final String[] mLocaleCodes;

    public final String supportedGlesVersion;
    public final String gpuRenderer;

    /**
     * Instantiates a new user preferences wrapper.
     *
     * @param context
     *            The application context.
     */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public GlobalPrefs( Context context, AppData appData )
    {
        mPreferences = PreferenceManager.getDefaultSharedPreferences( context );

        // Locale
        mLocaleCode = mPreferences.getString( KEY_LOCALE_OVERRIDE, DEFAULT_LOCALE_OVERRIDE );
        LocaleContextWrapper.setLocaleCode(mLocaleCode);
        final Locale[] availableLocales = Locale.getAvailableLocales();
        String[] values = context.getResources().getStringArray( R.array.localeOverride_values );
        String[] entries = new String[values.length];
        for( int i = values.length - 1; i > 0; i-- )
        {
            final Locale locale = createLocale( values[i] );

            // Get intersection of languages (available on device) and (translated for Mupen)
            if( locale != null && ArrayUtils.contains( availableLocales, locale ) )
            {
                // Get the name of the language, as written natively
                entries[i] = WordUtils.capitalize( locale.getDisplayName( locale ) );
            }
            else
            {
                // Remove the item from the list
                entries = ArrayUtils.remove( entries, i );
                values = ArrayUtils.remove( values, i );
            }
        }
        entries[0] = context.getString( R.string.localeOverride_entrySystemDefault );
        values[0] = Resources.getSystem().getConfiguration().locale.getLanguage();

        mLocaleNames = entries;
        mLocaleCodes = values;

        // Files
        String galleryCacheDir = appData.legacyUserDataDir + "/GalleryCache";
        legacyRomInfoCacheCfg = galleryCacheDir + "/romInfoCache.cfg";
        legacyCoverArtDir = galleryCacheDir + "/CoverArt";
        legacyProfilesDir = appData.legacyUserDataDir + "/Profiles";
        legacyTouchscreenCustomSkinsDir = appData.legacyUserDataDir + "/CustomSkins";
        legacyCoreConfigDir = appData.legacyUserDataDir + "/CoreConfig";

        final String coreConfigDir = context.getFilesDir().getAbsolutePath() + "/CoreConfig";
        coreUserDataDir = coreConfigDir + "/UserData";
        coreUserCacheDir = coreConfigDir + "/UserCache";
        hiResTextureDir = coreUserDataDir + "/mupen64plus/hires_texture/"; // MUST match what rice assumes natively
        textureCacheDir = coreUserCacheDir + "/mupen64plus/cache";
        shaderCacheDir = coreUserCacheDir + "/mupen64plus/shaders";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            screenshotsDir = context.getCacheDir().getAbsolutePath() + "/" + AppData.CORE_WORKING_DIR_NAME;
        } else {
            screenshotsDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                    Environment.DIRECTORY_PICTURES + File.separator + "mupen64plus";
            FileUtil.makeDirs(screenshotsDir);
        }
        romInfoCacheCfg = context.getFilesDir().getAbsolutePath() + "/romInfoCache.cfg";
        coverArtDir = context.getFilesDir().getAbsolutePath() + "/CoverArt";
        profilesDir = context.getFilesDir().getAbsolutePath() + "/Profiles";
        controllerProfiles_cfg = profilesDir + "/controller.cfg";
        touchscreenProfiles_cfg = profilesDir + "/touchscreen.cfg";
        emulationProfiles_cfg = profilesDir + "/emulation.cfg";
        customCheats_txt = profilesDir + "/customCheats.txt";
        touchscreenCustomSkinsDir = context.getFilesDir().getAbsolutePath() + "/CustomSkins";

        File externalRomsDir = context.getExternalFilesDir(null);
        if (externalRomsDir != null && !externalRomsDir.mkdirs()) {
            Log.e("GlobalPrefs", "Unable to make path " + externalRomsDir.getAbsolutePath());
        }

        externalRomsDirNoSaf = externalRomsDir != null ? externalRomsDir.getAbsolutePath() : null;

        // Library prefs
        isRecentShown = mPreferences.getBoolean( "showRecentlyPlayed", true );
        sortByRomName = mPreferences.getString( "sortingMethod", "romName" ).equals("romName");
        isFullNameShown = mPreferences.getBoolean( "showFullNames", true );
        coverArtScale = ( mPreferences.getInt( "libraryArtScale", 100 ) ) / 100.0f;
        fillAllowedCountryCodes();

        // Touchscreen prefs
        isTouchscreenFeedbackEnabled = mPreferences.getBoolean( "touchscreenFeedback", false );
        touchscreenScale = ( mPreferences.getInt( "touchscreenScaleV2", 100 ) ) / 100.0f;
        touchscreenTransparency = ( 255 * mPreferences.getInt( "touchscreenTransparencyV2", 60 ) ) / 100;
        touchscreenAutoHold = getSafeInt( mPreferences, "touchscreenAutoHoldV2", 0 );
        touchscreenAutoHideEnabled = mPreferences.getBoolean( "touchscreenAutoHideEnabled", true );
        touchscreenAutoHideSeconds = mPreferences.getInt( "touchscreenAutoHideSeconds", 5 );
        isTouchscreenAnimated = mPreferences.getBoolean( "touchscreenAnimated_v2", true );
        isTouchscreenAnalogRelative = mPreferences.getBoolean( "touchscreenAnalogRelative_global", false );
        // Determine the touchscreen layout
        touchscreenSkin = mPreferences.getString( "touchscreenSkin_v2", "WiiU" );
        boolean tempIsCustomTouchscreenSkin = touchscreenSkin.equals( "Custom" );

        String tempTouchscreenPath;
        if(tempIsCustomTouchscreenSkin)
            tempTouchscreenPath = touchscreenCustomSkinsDir;
        else
            tempTouchscreenPath = appData.touchscreenSkinsDir + touchscreenSkin;

        // Verify that at least a single image exists for the touchscreen style,
        // and if not, then revert to the Outline style
        try {
            Bitmap bitmap;

            if(tempIsCustomTouchscreenSkin) {
                bitmap = BitmapFactory.decodeFile( tempTouchscreenPath + "/analog-fore.png" );
            } else {
                InputStream inputStream = context.getAssets().open(tempTouchscreenPath + "/analog-fore.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
            }

            if (bitmap != null) {
                Log.i("GlobalPrefs", bitmap.toString());
            } else {
                tempIsCustomTouchscreenSkin = false;
                tempTouchscreenPath = appData.touchscreenSkinsDir + "Outline";
            }

        }
        catch(IOException ex) {
            tempTouchscreenPath = appData.touchscreenSkinsDir + "Outline";
        }
        isCustomTouchscreenSkin = tempIsCustomTouchscreenSkin;
        touchscreenSkinPath = tempTouchscreenPath;

        // Video prefs
        displayResolution = getSafeInt( mPreferences, "displayResolution", 480 );
        videoSurfaceZoom = mPreferences.getInt( "displayZoomSeek", 100 );

        shaderPasses = new ArrayList<>();
        String shaderPassesString =  mPreferences.getString( KEY_SHADER_PASS, "" );

        if (!TextUtils.isEmpty(shaderPassesString)) {
            String[] passesSplitString = shaderPassesString.split(",");
            Collections.addAll(shaderPasses, passesSplitString);
        }

        // No need to scale if we are just using the default shader
        shaderScaleFactor = shaderPasses.size() == 0 ? 1 : mPreferences.getInt( "shaderScaleFactor", 2 );

        displayScaling = DisplayScaling.getScaling(mPreferences.getString( "displayScaling", "original" ));
        isImmersiveModeEnabled = mPreferences.getBoolean( "displayImmersiveMode_v2", true );
        displayOrientation = getSafeInt( mPreferences, "displayOrientation", 0 );
        final int transparencyPercent = mPreferences.getInt( "displayActionBarTransparency", 80 );
        displayActionBarTransparency = ( 255 * transparencyPercent ) / 100;

        String fpsPosition = mPreferences.getString( "displayFpsV2", "off" );


        isFpsEnabled = !fpsPosition.equals("off");

        int tempFpsXPosition = 0;
        int tempFpsYPosition = 0;

        switch(fpsPosition)
        {
            case "topLeft":
                tempFpsXPosition = 2;
                tempFpsYPosition = 2;
                break;
            case "topCenter":
                tempFpsXPosition = 50;
                tempFpsYPosition = 2;
                break;
            case "topRight":
                tempFpsXPosition = 98;
                tempFpsYPosition = 2;
                break;
            case "bottomLeft":
                tempFpsXPosition = 2;
                tempFpsYPosition = 98;
                break;
            case "bottomCenter":
                tempFpsXPosition = 50;
                tempFpsYPosition = 98;
                break;
            case "bottomRight":
                tempFpsXPosition = 98;
                tempFpsYPosition = 98;
                break;
        }
        fpsXPosition = tempFpsXPosition;
        fpsYPosition = tempFpsYPosition;

        final int selectedHardwareType = getSafeInt( mPreferences, "videoHardwareType", -1 );
        isPolygonOffsetHackEnabled = selectedHardwareType > -2;
        videoHardwareType = selectedHardwareType < 0 ? appData.hardwareInfo.hardwareType : selectedHardwareType;
        switch( videoHardwareType )
        {
            case HardwareInfo.HARDWARE_TYPE_OMAP:
                videoPolygonOffset = 0.2f;
                break;
            case HardwareInfo.HARDWARE_TYPE_OMAP_2:
                videoPolygonOffset = -1.5f;
                break;
            case HardwareInfo.HARDWARE_TYPE_QUALCOMM:
                videoPolygonOffset = -0.2f;
                break;
            case HardwareInfo.HARDWARE_TYPE_IMAP:
                videoPolygonOffset = -0.001f;
                break;
            case HardwareInfo.HARDWARE_TYPE_TEGRA:
                videoPolygonOffset = -2.0f;
                break;
            case HardwareInfo.HARDWARE_TYPE_UNKNOWN:
                videoPolygonOffset = -3.0f;
                break;
            default:
                videoPolygonOffset = SafeMethods.toFloat( mPreferences.getString( "videoPolygonOffset", "-3.0" ), -3.0f );
                break;
        }

        threadedGLideN64 = mPreferences.getBoolean( "threadedGLideN64", true );
        hybridTextureFilterGLideN64 = mPreferences.getBoolean( "hybridTextureFilter_v2", true );

        // Audio prefs
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioSwapChannels = mPreferences.getBoolean( "audioSwapChannels", false );
        enableAudioTimeSretching = mPreferences.getBoolean( "audioTimeStretch", true );
        audioVolume = getSafeInt( mPreferences, "audioVolume", 100 );
        audioBufferSizeMs = getSafeInt( mPreferences, "audioBufferSize", 64 );
        audioFloatingPoint = mPreferences.getBoolean( "audioFloatingPoint", false );
        audioSamplingType = getSafeInt( mPreferences, "audioSamplingType", 0 );

        // Say that any device that only has OpenGL ES 2.0 is a low end device by default, this works
        // 90% of the time and the user can still modify it if they want to
        String openGlVersion = AppData.getOpenGlEsVersion(context);
        lowPerformanceAudio = mPreferences.getBoolean( AUDIO_LOW_PERFORMANCE_MODE, openGlVersion.equals("2.0") );

        // Give this an initial value so that the correct value shows up under settings when audio
        // settings is first shown
        if (!mPreferences.contains(AUDIO_LOW_PERFORMANCE_MODE)) {
            mPreferences.edit().putBoolean(AUDIO_LOW_PERFORMANCE_MODE, lowPerformanceAudio).apply();
        }

        int tempAudioSamplingRate = 0;

        // Don't change the game sample rate for low end devices since this can cut performance by up to 25%!
        if (!lowPerformanceAudio) {
            // Automatically determine best sampling rate for devices that can handle it
            try {
                tempAudioSamplingRate = Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
            } catch (java.lang.NumberFormatException e) {
                Log.e("GlobalPrefs", "Invalid sampling rate number: " + audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
            }
        }

        audioSamplingRate = tempAudioSamplingRate;

        int tempAudioSecondaryBufferSize = 256;
        try
        {
            tempAudioSecondaryBufferSize = Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
        }
        catch(java.lang.NumberFormatException e)
        {
            Log.e("GlobalPrefs", "Invalid frames per buffer number: " + audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
        }

        audioHardwareBufferSize = tempAudioSecondaryBufferSize;

        if( audioVolume != 0 )
            isFramelimiterEnabled = !mPreferences.getBoolean( "audioSynchronize", true );
        else
            isFramelimiterEnabled = true;

        // User interface modes
        final String navMode = mPreferences.getString( "navigationMode", "auto" );
        switch(navMode) {
            case "bigscreen":
                isBigScreenMode = true;
                break;
            case "standard":
                isBigScreenMode = false;
                break;
            default:
                isBigScreenMode = appData.isAndroidTv;
        }

        final String inGameMenuMode = mPreferences.getString( "inGameMenu", "back-key" );

        maxAutoSaves = mPreferences.getInt( "gameAutoSaves", 5 );

        useFlatGameDataPath = mPreferences.getBoolean( "useFlatGameDataPath", false );

        externalFileStoragePath = mPreferences.getString(PATH_GAME_SAVES, "");
        useExternalStorge = mPreferences.getString(GAME_DATA_STORAGE_TYPE, "internal").equals("external") &&
                !TextUtils.isEmpty(externalFileStoragePath);

        japanIplPath = mPreferences.getString(PATH_JAPAN_IPL_ROM, "");

        // Determine the key codes that should not be mapped to controls
        volKeysMappable = mPreferences.getBoolean( "inputVolumeMappable", false );
        final boolean backKeyMappable = mPreferences.getBoolean( "inputBackMappable", false );
        final boolean menuKeyMappable = mPreferences.getBoolean( "inputMenuMappable", false );

        inGameMenuIsSwipGesture = inGameMenuMode.equals("swipe") || menuKeyMappable || backKeyMappable;

        final List<Integer> unmappables = new ArrayList<>();

        if(!menuKeyMappable)
        {
            unmappables.add( KeyEvent.KEYCODE_MENU );
        }
        if(!backKeyMappable)
        {
            // Back key is needed to show/hide the action bar in HC+
            unmappables.add( KeyEvent.KEYCODE_BACK );
        }
        if( !volKeysMappable )
        {
            unmappables.add( KeyEvent.KEYCODE_VOLUME_UP );
            unmappables.add( KeyEvent.KEYCODE_VOLUME_DOWN );
            unmappables.add( KeyEvent.KEYCODE_VOLUME_MUTE );
        }
        unmappableKeyCodes = Collections.unmodifiableList( unmappables );

        // Controller profiles
        controllerProfile1 = loadControllerProfile( mPreferences, CONTROLLER_PROFILE1,
                getControllerProfileDefault(1),
                GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile2 = loadControllerProfile( mPreferences, CONTROLLER_PROFILE2,
                getControllerProfileDefault(2),
                GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile3 = loadControllerProfile( mPreferences, CONTROLLER_PROFILE3,
                getControllerProfileDefault(3),
                GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile4 = loadControllerProfile( mPreferences, CONTROLLER_PROFILE4,
                getControllerProfileDefault(4),
                GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );

        autoPlayerMapping = mPreferences.getBoolean( "autoPlayerMapping", false );

        allEmulatedControllersPlugged = mPreferences.getBoolean( "allEmulatedControllersPlugged", false );

        // Peripheral share mode
        isControllerShared = mPreferences.getBoolean( "inputShareController", false );

        showBuiltInEmulationProfiles = mPreferences.getBoolean(ManageEmulationProfilesActivity.SHOW_BUILT_IN_PREF_KEY, true);
        showBuiltInTouchscreenProfiles = mPreferences.getBoolean(ManageTouchscreenProfilesActivity.SHOW_BUILT_IN_PREF_KEY, true);
        showBuiltInControllerProfiles = mPreferences.getBoolean(ManageControllerProfilesActivity.SHOW_BUILT_IN_PREF_KEY, true);

        useHighPriorityThread = mPreferences.getBoolean( "useHighPriorityThread_v2", true );
        useRaphnetDevicesIfAvailable = mPreferences.getBoolean( "useRaphnetAdapter", false );
        holdControllerBottons = mPreferences.getBoolean( "holdButtonForMenu", true );

        useUpnpToMapNetplayPorts = mPreferences.getBoolean( "useUpnpToMapPorts", true );
        int tempRoomTcpPort = getSafeInt( mPreferences, ROOM_TCP_PORT, 43821 );
        netplayRoomTcpPort = tempRoomTcpPort > 1024 ? tempRoomTcpPort : 43821;
        int tempServerUdpTcpPort = getSafeInt( mPreferences, SERVER_UDP_TCP_PORT, 43822 );
        netplayServerUdpTcpPort = tempServerUdpTcpPort > 1024 && tempServerUdpTcpPort != tempRoomTcpPort ? tempServerUdpTcpPort : 43822;

        supportedGlesVersion = AppData.getOpenGlEsVersion(context);
        gpuRenderer = AppData.getOpenGlEsRenderer();
    }

    public void changeLocale( final Activity activity )
    {
        // Get the index of the current locale
        final int currentIndex = ArrayUtils.indexOf( mLocaleCodes, mLocaleCode );

        // Populate and show the language menu
        final Builder builder = new Builder( activity );
        builder.setTitle( R.string.menuItem_localeOverride );
        builder.setSingleChoiceItems( mLocaleNames, currentIndex, (dialog, which) -> {
            dialog.dismiss();
            if( which >= 0 && which != currentIndex )
            {
                mPreferences.edit().putString( KEY_LOCALE_OVERRIDE, mLocaleCodes[which] ).apply();
                activity.finishAffinity();
                ActivityHelper.startSplashActivity(activity);
            }
        });
        builder.create().show();
    }

    private void fillAllowedCountryCodes()
    {
        boolean showUnknownCountryCode = mPreferences.getBoolean( "libraryCountryFilterUnknown", true );
        boolean showUsaCountryCode = mPreferences.getBoolean( "libraryCountryFilterUsa", true );
        boolean showJapanCountryCode = mPreferences.getBoolean( "libraryCountryFilterJapan", true );
        boolean showEuropeCountryCode = mPreferences.getBoolean( "libraryCountryFilterEurope", true );
        boolean showAustraliaCountryCode = mPreferences.getBoolean( "libraryCountryFilterAustralia", true );
        boolean showKoreaCountryCode = mPreferences.getBoolean( "libraryCountryFilterKorea", true );
        boolean showGermanyCountryCode = mPreferences.getBoolean( "libraryCountryFilterGermany", true );
        boolean showFranceCountryCode = mPreferences.getBoolean( "libraryCountryFilterFrance", true );
        boolean showItalyCountryCode = mPreferences.getBoolean( "libraryCountryFilterItaly", true );
        boolean showSpainCountryCode = mPreferences.getBoolean( "libraryCountryFilterSpain", true );

        if(showUnknownCountryCode)
        {
            allowedCountryCodes.add(CountryCode.UNKNOWN);
            allowedCountryCodes.add(CountryCode.DEMO);
            allowedCountryCodes.add(CountryCode.BETA);
        }

        if(showUsaCountryCode)
        {
            allowedCountryCodes.add(CountryCode.JAPAN_USA);
            allowedCountryCodes.add(CountryCode.USA);
        }

        if(showJapanCountryCode)
        {
            allowedCountryCodes.add(CountryCode.JAPAN);
            allowedCountryCodes.add(CountryCode.JAPAN_USA);
            allowedCountryCodes.add(CountryCode.JAPAN_KOREA);
        }

        if(showEuropeCountryCode)
        {
            allowedCountryCodes.add(CountryCode.EUROPE_1);
            allowedCountryCodes.add(CountryCode.EUROPE_2);
            allowedCountryCodes.add(CountryCode.EUROPE_3);
            allowedCountryCodes.add(CountryCode.EUROPE_4);
            allowedCountryCodes.add(CountryCode.EUROPE_5);
            allowedCountryCodes.add(CountryCode.EUROPE_6);
        }

        if(showAustraliaCountryCode)
        {
            allowedCountryCodes.add(CountryCode.AUSTRALIA);
            allowedCountryCodes.add(CountryCode.AUSTRALIA_ALT);
        }

        if(showKoreaCountryCode)
        {
            allowedCountryCodes.add(CountryCode.KOREA);
            allowedCountryCodes.add(CountryCode.JAPAN_KOREA);
        }

        if(showGermanyCountryCode)
        {
            allowedCountryCodes.add(CountryCode.GERMANY);
        }

        if(showFranceCountryCode)
        {
            allowedCountryCodes.add(CountryCode.FRANCE);
        }

        if(showItalyCountryCode)
        {
            allowedCountryCodes.add(CountryCode.ITALY);
        }

        if(showSpainCountryCode)
        {
            allowedCountryCodes.add(CountryCode.SPAIN);
        }
    }

    public LinkedHashSet<CountryCode> getAllowedCountryCodes()
    {
        return allowedCountryCodes;
    }

    public String getEmulationProfileDefaultDefault()
    {
        String defaultEmulationProfile = DEFAULT_EMULATION_PROFILE_DEFAULT;

        if(AppData.doesSupportFullGL())
        {
            defaultEmulationProfile = "GlideN64-Very-Accurate";
        }
        else if(supportedGlesVersion.equals("3.1") || supportedGlesVersion.equals("3.2"))
        {
            defaultEmulationProfile = "Glide64-Accurate";
        }

        return defaultEmulationProfile;
    }

    public String getEmulationProfileDefault()
    {
        return getString( KEY_EMULATION_PROFILE_DEFAULT, getEmulationProfileDefaultDefault() );
    }

    public String getTouchscreenProfileDefault()
    {
        return getString( KEY_TOUCHSCREEN_PROFILE_DEFAULT, DEFAULT_TOUCHSCREEN_PROFILE_DEFAULT );
    }

    public String getTouchscreenDpadProfileDefault()
    {
        return getString( KEY_TOUCHSCREEN_DPAD_PROFILE_DEFAULT, DEFAULT_TOUCHSCREEN_DPAD_PROFILE_DEFAULT );
    }

    String getControllerProfileDefault( int player )
    {
        switch( player )
        {
            case 2: return getString( CONTROLLER_PROFILE2, DEFAULT_CONTROLLER_PROFILE_DEFAULT );
            case 3: return getString( CONTROLLER_PROFILE3, DEFAULT_CONTROLLER_PROFILE_DEFAULT );
            case 4: return getString( CONTROLLER_PROFILE4, DEFAULT_CONTROLLER_PROFILE_DEFAULT );
            default: break;
        }

        return getString( CONTROLLER_PROFILE1, DEFAULT_CONTROLLER_PROFILE_DEFAULT );
    }

    public void putEmulationProfileDefault( String value )
    {
        putString( KEY_EMULATION_PROFILE_DEFAULT, value );
    }

    public void putTouchscreenProfileDefault( String value )
    {
        putString( KEY_TOUCHSCREEN_PROFILE_DEFAULT, value );
    }

    public void putTouchscreenDpadProfileDefault( String value )
    {
        putString( KEY_TOUCHSCREEN_DPAD_PROFILE_DEFAULT, value );
    }

    public boolean getBoolean( String key, boolean defaultValue )
    {
        return mPreferences.getBoolean( key, defaultValue );
    }

    public String getString( String key, String defaultValue )
    {
        return mPreferences.getString( key, defaultValue );
    }

    public void putString( String key, String value )
    {
        mPreferences.edit().putString( key, value ).apply();
    }

    private Locale createLocale( String code )
    {
        final String[] codes = code.split( "_" );
        switch( codes.length )
        {
            case 1: // Language code provided
                return new Locale( codes[0] );
            case 2: // Language and country code provided
                return new Locale( codes[0], codes[1] );
            case 3: // Language, country, and variant code provided
                return new Locale( codes[0], codes[1], codes[2] );
            default: // Invalid input
                return null;
        }
    }

    /**
     * Gets the selected value of a ListPreference, as an integer.
     *
     * @param preferences  The object containing the ListPreference.
     * @param key          The key of the ListPreference.
     * @param defaultValue The value to use if parsing fails.
     *
     * @return The value of the selected entry, as an integer.
     */
    private static int getSafeInt( SharedPreferences preferences, String key, int defaultValue )
    {
        try
        {
            return parseInt( preferences.getString( key, String.valueOf( defaultValue ) ) );
        }
        catch( final NumberFormatException ex )
        {
            return defaultValue;
        }
    }

    public ConfigFile GetEmulationProfilesConfig()
    {
        if(mEmulationProfilesConfig == null)
        {
            mEmulationProfilesConfig = new ConfigFile( emulationProfiles_cfg );
        }

        return mEmulationProfilesConfig;
    }

    public ConfigFile GetTouchscreenProfilesConfig()
    {
        if(mTouchscreenProfilesConfig == null)
        {
            mTouchscreenProfilesConfig = new ConfigFile( touchscreenProfiles_cfg );
        }

        return mTouchscreenProfilesConfig;
    }

    public ConfigFile GetControllerProfilesConfig()
    {
        if(mControllerProfilesConfig == null)
        {
            mControllerProfilesConfig = new ConfigFile( controllerProfiles_cfg );
        }

        return mControllerProfilesConfig;
    }

    private static ControllerProfile loadControllerProfile( SharedPreferences prefs, String key,
                                                            String defaultName, ConfigFile custom, ConfigFile builtin )
    {
        final String name = prefs.getString( key, defaultName );

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

    public ArrayList<ShaderLoader> getShaderPasses() {
        ArrayList<ShaderLoader> shaderPassesValues = new ArrayList<>();

        for (int index = 0; index < shaderPasses.size(); ++index) {
            ShaderLoader selectedShader;

            try {
                selectedShader = ShaderLoader.valueOf(shaderPasses.get(index));
            } catch (java.lang.IllegalArgumentException e) {
                selectedShader = null;
            }

            if (selectedShader != null) {
                shaderPassesValues.add(selectedShader);
            }
        }

        return shaderPassesValues;
    }

    public void putShaderPasses(ArrayList<ShaderLoader> shaderPasses) {

        StringBuilder sb = new StringBuilder();

        for (ShaderLoader shaderPass : shaderPasses) {
            sb.append(shaderPass.toString()).append(",");
        }

        putString(KEY_SHADER_PASS, sb.toString());
    }

    public void putShaderScaleFactor(int factor) {

        mPreferences.edit().putInt( "shaderScaleFactor", factor).apply();
    }
}
