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
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.media.AudioManager;
import androidx.preference.PreferenceManager;

import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.WindowManager;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.WordUtils;
import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.persistent.AppData.HardwareInfo;
import paulscode.android.mupen64plusae.profile.ControllerProfile;
import paulscode.android.mupen64plusae.profile.ManageControllerProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageEmulationProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageTouchscreenProfilesActivity;
import paulscode.android.mupen64plusae.util.CountryCode;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;
import paulscode.android.mupen64plusae.util.Plugin;
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
@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal", "ConstantConditions"})
public class GlobalPrefs
{
    public enum DisplayScaling
    {
        ORIGINAL("original"),
        STRETCH("stretch"),
        STRETCH_169("stretch169");

        private String text;

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

    /** The subdirectory containing unzipped 64DD files. */
    public final String unzippedRomsDir;

    /** The subdirectory containing screenshot files. */
    public final String screenshotsDir;

    /** The subdirectory containing crash logs. */
    public final String crashLogDir;

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

    /** Legacy core config folder */
    public final String legacyCoreConfigDir;

    /** Legacy auto save directory */
    public final String legacyAutoSaves;

    /** Legacy slot save directory */
    public final String legacySlotSaves;

    /** The path of the rom info cache for the gallery. */
    public final String romInfoCache_cfg;

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

    /** The selected audio plug-in. */
    public final Plugin audioPlugin;

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
    private final int displayResolution;

    /** The zoom value applied to the viewing surface, in percent. */
    public final int videoSurfaceZoom;

    /** Display scaling */
    final DisplayScaling displayScaling;

    /** The width of the viewing surface, in pixels with the correct aspect ratio. */
    private int videoSurfaceWidthOriginal;

    /** The height of the viewing surface, in pixels with the correct aspect ratio. */
    private int videoSurfaceHeightOriginal;

    /** The rendering width in pixels with the correct aspect ratio. */
    private int videoRenderWidthNative;

    /** The rendering heigh in pixels with the correct aspect ratio. */
    private int videoRenderHeightNative;

    /** The width of the viewing surface, in pixels with the stretched aspect ratio. */
    private int videoSurfaceWidthStretch;

    /** The height of the viewing surface, in pixels with the stretched aspect ratio. */
    private int videoSurfaceHeightStretch;

    /** Screen aspect ratio */
    private float aspect;

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

    /** Enable hack to fix up upside down screen in GLideN64 GLES 3.0 for some devices */
    public final boolean enableBlitScreenWorkaround;

    /** Enable threading in GLideN64*/
    public final boolean threadedGLideN64;

    /** True if the left and right audio channels are swapped. */
    public final boolean audioSwapChannels;

    /** Stretch audio to prevent crackling in SLES audio plugin */
    public final boolean enableSLESAudioTimeSretching;

    /** Size of secondary buffer in output samples. This is SLES's hardware buffer, which directly affects latency. */
    public final int audioSLESSecondaryBufferSize;

    /** Number of SLES secondary buffers. */
    public final int audioSLESSecondaryBufferNbr;

    /** Number of SLES sampling rate. */
    public final int audioSLESSamplingRate;

    /** SLES sampling type, 0=trivial, 1=soundtouch. */
    public final int audioSLESSamplingType;

    /** Use SLES floating point samples */
    public final boolean audioSLESFloatingPoint;

    /** True if big-screen navigation mode is enabled. */
    public final boolean isBigScreenMode;

    /** True if we are using the swipe gesture for the in-game menu, false if we are using the back key */
    public final boolean inGameMenuIsSwipGesture;

    /** Maximum number of auto saves */
    public final int maxAutoSaves;

    /** True if specific game data should be saved in a flat file structure */
    final boolean useFlatGameDataPath;

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

    // Shared preferences keys and key templates
    static final String KEY_EMULATION_PROFILE_DEFAULT = "emulationProfileDefault";
    static final String KEY_TOUCHSCREEN_PROFILE_DEFAULT = "touchscreenProfileDefault";
    static final String KEY_TOUCHSCREEN_DPAD_PROFILE_DEFAULT = "touchscreenProfileDpadDefault";
    public static final String KEY_LOCALE_OVERRIDE = "localeOverride";
    public static final String KEY_TOUCHSCREEN_SKIN_CUSTOM_PATH = "touchscreenCustomSkin";
    public static final String CONTROLLER_PROFILE1 = "controllerProfile1";
    public static final String CONTROLLER_PROFILE2 = "controllerProfile2";
    public static final String CONTROLLER_PROFILE3 = "controllerProfile3";
    public static final String CONTROLLER_PROFILE4 = "controllerProfile4";
    public static final String PLAYER_MAP = "playerMap";
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

    private final String supportedGlesVersion;

    /**
     * Instantiates a new user preferences wrapper.
     *
     * @param context
     *            The application context.
     */
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
        mLocaleNames = entries;
        mLocaleCodes = values;

        // Files
        String galleryCacheDir = appData.userDataDir + "/GalleryCache";
        coverArtDir = galleryCacheDir + "/CoverArt";
        unzippedRomsDir = galleryCacheDir + "/UnzippedRoms";
        screenshotsDir = appData.userDataDir + "/Screenshots";
        String profilesDir = appData.userDataDir + "/Profiles";
        crashLogDir = appData.userDataDir + "/CrashLogs";
        legacyCoreConfigDir = appData.userDataDir + "/CoreConfig";

        final String coreConfigDir = context.getCacheDir().getAbsolutePath() + "/CoreConfig";

        coreUserDataDir = coreConfigDir + "/UserData";
        coreUserCacheDir = coreConfigDir + "/UserCache";
        hiResTextureDir = coreUserDataDir + "/mupen64plus/hires_texture/"; // MUST match what rice assumes natively
        textureCacheDir = coreUserCacheDir + "/mupen64plus/cache";
        shaderCacheDir = coreUserCacheDir + "/mupen64plus/shaders";
        romInfoCache_cfg = galleryCacheDir + "/romInfoCache.cfg";
        controllerProfiles_cfg = profilesDir + "/controller.cfg";
        touchscreenProfiles_cfg = profilesDir + "/touchscreen.cfg";
        emulationProfiles_cfg = profilesDir + "/emulation.cfg";
        customCheats_txt = profilesDir + "/customCheats.txt";
        touchscreenCustomSkinsDir = appData.userDataDir + "/CustomSkins";
        legacyAutoSaves = appData.userDataDir + "/AutoSaves";
        legacySlotSaves = appData.userDataDir + "/SlotSaves";

        //Generate .nomedia files to prevent android from adding these to gallery apps
        File file = new File(coreConfigDir + "/.nomedia");
        if (!file.exists()) {
            try {
                if ( file.createNewFile()) {
                    Log.e("GlobalPrefs", "Unable to create " + file.getPath());
                }
            } catch (IOException e) {
                Log.e("GlobalPrefs", "Unable to create " + file.getPath());
            }
        }

        // Plug-ins
        audioPlugin = new Plugin( mPreferences, "audioPlugin" );

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
        touchscreenSkin = mPreferences.getString( "touchscreenSkin", "JoshaGibs" );
        if( touchscreenSkin.equals( "Custom" ) )
            touchscreenSkinPath =  mPreferences.getString( "touchscreenCustomSkin", "" );
        else
            touchscreenSkinPath = appData.touchscreenSkinsDir + touchscreenSkin;

        // Video prefs
        displayResolution = getSafeInt( mPreferences, "displayResolution", 480 );
        videoSurfaceZoom = mPreferences.getInt( "displayZoomSeek", 100 );
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
                tempFpsXPosition = 0;
                tempFpsYPosition = 0;
                break;
            case "topCenter":
                tempFpsXPosition = 50;
                tempFpsYPosition = 0;
                break;
            case "topRight":
                tempFpsXPosition = 100;
                tempFpsYPosition = 0;
                break;
            case "bottomLeft":
                tempFpsXPosition = 0;
                tempFpsYPosition = 100;
                break;
            case "bottomCenter":
                tempFpsXPosition = 50;
                tempFpsYPosition = 100;
                break;
            case "bottomRight":
                tempFpsXPosition = 100;
                tempFpsYPosition = 100;
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

        enableBlitScreenWorkaround = mPreferences.getBoolean( "enableBlitScreenWorkaround", false );
        threadedGLideN64 = mPreferences.getBoolean( "threadedGLideN64", true );

        // Audio prefs
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioSwapChannels = mPreferences.getBoolean( "audioSwapChannels", false );
        enableSLESAudioTimeSretching = mPreferences.getBoolean( "audioSLESTimeStretch", true );
        audioSLESSecondaryBufferNbr = getSafeInt( mPreferences, "audioSLESBufferNbr2", 10 );
        audioSLESFloatingPoint = mPreferences.getBoolean( "audioSLESFloatingPoint", false );
        audioSLESSamplingType = getSafeInt( mPreferences, "audioSLESSamplingType", 0 );

        boolean audioSlesSamplingRateGame = mPreferences.getString( "audioSLESSamplingRate2", "game" ).equals("game");

        int tempAudioSLESSamplingRate = 0;

        //If sampling rate is not set to game, then automatically determine best sampling rate
        if(!audioSlesSamplingRateGame) {
            try {
                tempAudioSLESSamplingRate = Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
            } catch (java.lang.NumberFormatException e) {
                Log.e("GlobalPrefs", "Invalid sampling rate number: " + audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
            }
        }

        audioSLESSamplingRate = tempAudioSLESSamplingRate;

        int tempAudioSLESSecondaryBufferSize = 256;
        try
        {
            tempAudioSLESSecondaryBufferSize = Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
        }
        catch(java.lang.NumberFormatException e)
        {
            Log.e("GlobalPrefs", "Invalid frames per buffer number: " + audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
        }

        audioSLESSecondaryBufferSize = tempAudioSLESSecondaryBufferSize;

        if( audioPlugin.enabled )
            isFramelimiterEnabled = !mPreferences.getBoolean( "audioSynchronize", true );
        else
            isFramelimiterEnabled = !mPreferences.getString( "audioPlugin", "" ).equals( "nospeedlimit" );

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

        useHighPriorityThread = mPreferences.getBoolean( "useHighPriorityThread", false );
        useRaphnetDevicesIfAvailable = mPreferences.getBoolean( "useRaphnetAdapter", false );

        supportedGlesVersion = AppData.getOpenGlEsVersion(context);
    }

    public void changeLocale( final Activity activity )
    {
        // Get the index of the current locale
        final int currentIndex = ArrayUtils.indexOf( mLocaleCodes, mLocaleCode );

        // Populate and show the language menu
        final Builder builder = new Builder( activity );
        builder.setTitle( R.string.menuItem_localeOverride );
        builder.setSingleChoiceItems( mLocaleNames, currentIndex, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                dialog.dismiss();
                if( which >= 0 && which != currentIndex )
                {
                    mPreferences.edit().putString( KEY_LOCALE_OVERRIDE, mLocaleCodes[which] ).apply();
                    activity.finishAffinity();
                    ActivityHelper.startSplashActivity(activity);
                }
            }
        } );
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

    private void putString( String key, String value )
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

    void determineResolutionData(Context context, DisplayScaling scaling)
    {
        // Determine the pixel dimensions of the rendering context and view surface
        // Screen size
        final WindowManager windowManager = (WindowManager) context.getSystemService(android.content.Context.WINDOW_SERVICE);
        final Display display = windowManager != null ? windowManager.getDefaultDisplay() : null;

        final Point dimensions = new Point(0,0);

        if( display != null )
        {
            if(isImmersiveModeEnabled )
            {
                display.getRealSize(dimensions);
            }
            else
            {
                display.getSize(dimensions);
            }
        }

        videoSurfaceWidthStretch = dimensions.x;
        videoSurfaceHeightStretch = dimensions.y;

        switch (scaling) {
            case ORIGINAL:
                aspect = 3f/4f;
                break;
            case STRETCH:
                aspect = (float)Math.min(dimensions.x, dimensions.y)/Math.max(dimensions.x, dimensions.y);
                break;
            case STRETCH_169:
                aspect = 9f/16f;
                break;
        }

        int minDimension = Math.min(dimensions.x, dimensions.y);
        videoRenderWidthNative = Math.round( minDimension/aspect );
        videoRenderHeightNative = minDimension;

        // Assume we are are in portrait mode if height is greater than the width
        boolean portrait = dimensions.y > dimensions.x;
        if(portrait)
        {
            videoSurfaceWidthOriginal = minDimension;
            videoSurfaceHeightOriginal = Math.round( minDimension*aspect);
        }
        else
        {
            videoSurfaceWidthOriginal = Math.round( minDimension/aspect );
            videoSurfaceHeightOriginal = minDimension;
        }

        Log.i("GlobalPrefs", "render_width=" + videoRenderWidthNative + " render_height=" + videoRenderHeightNative);
    }

    int getResolutionWidth(boolean stretch, int hResolution)
    {
        if( hResolution == -1)
        {
            hResolution = displayResolution;
        }

        if (hResolution == 0)
        {
            hResolution = videoRenderHeightNative;
        }

        float aspect = stretch ? (float)videoSurfaceWidthStretch/videoSurfaceHeightStretch : 4f/3f;

        return Math.round((float)hResolution*aspect);
    }

    int getResolutionHeight(int hResolution)
    {
        if (hResolution == -1) {
            hResolution = displayResolution;
        }

        return hResolution == 0 ? videoRenderHeightNative : hResolution;
    }

    int getSurfaceResolutionHeight()
    {
        return videoSurfaceHeightOriginal;
    }

    int getSurfaceResolutionWidth()
    {
        return videoSurfaceWidthOriginal;
    }
}
