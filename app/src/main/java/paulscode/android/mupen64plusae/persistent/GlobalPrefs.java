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
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.media.AudioManager;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.WindowManager;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.WordUtils;
import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.input.map.PlayerMap;
import paulscode.android.mupen64plusae.jni.NativeConstants;
import paulscode.android.mupen64plusae.persistent.AppData.HardwareInfo;
import paulscode.android.mupen64plusae.profile.ControllerProfile;
import paulscode.android.mupen64plusae.profile.ManageControllerProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageEmulationProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageTouchscreenProfilesActivity;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;
import paulscode.android.mupen64plusae.util.Plugin;
import paulscode.android.mupen64plusae.util.SafeMethods;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
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
public class GlobalPrefs
{
    /** The subdirectory containing gallery data cache. */
    public final String galleryCacheDir;

    /** The subdirectory containing cover art files. */
    public final String coverArtDir;

    /** The subdirectory containing unzipped ROM files. */
    public final String unzippedRomsDir;

    /** The subdirectory containing custom profiles. */
    public final String profilesDir;

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
    public final int touchscreenAutoHold;

    /** Enable touchscreen auto-hide */
    public final boolean touchscreenAutoHideEnabled;

    /** How long before auto hiding touchscreen buttons */
    public final int touchscreenAutoHideSeconds;

    /** True if the touchscreen joystick is animated. */
    public final boolean isTouchscreenAnimated;

    /** The set of key codes that are not allowed to be mapped. **/
    public final List<Integer> unmappableKeyCodes;

    /** True if the recently played section of the gallery should be shown. */
    public final boolean isRecentShown;

    /** True if we should cache recently played games for faster load times */
    public final boolean cacheRecentlyPlayed;

    /** True if the full ROM rip info should be shown. */
    public final boolean isFullNameShown;

    /** Factor applied to the cover art scale */
    public final float coverArtScale;

    /** Default resolution */
    public final int displayResolution;

    /** Default resolution */
    public final boolean stretchScreen;

    /** The width of the viewing surface, in pixels with the correct aspect ratio. */
    public int videoSurfaceWidthOriginal;

    /** The height of the viewing surface, in pixels with the correct aspect ratio. */
    public int videoSurfaceHeightOriginal;

    /** The width of the viewing surface, in pixels with the stretched aspect ratio. */
    public int videoSurfaceWidthStretch;

    /** The height of the viewing surface, in pixels with the stretched aspect ratio. */
    public int videoSurfaceHeightStretch;

    /** The screen orientation for the game activity. */
    public final int displayOrientation;

    /** Current screen orientation */
    public final int currentDisplayOrientation;

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
    public final int videoHardwareType;

    /** The polygon offset to use. */
    public final float videoPolygonOffset;

    /** Enable hack to fix up upside down screen in GLideN64 GLES 3.0 for some devices */
    public final boolean enableBlitScreenWorkaround;

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

    /** Use SLES floating point samples */
    public final boolean audioSLESFloatingPoint;

    /** True if big-screen navigation mode is enabled. */
    public final boolean isBigScreenMode;

    /** True if we are using the swipe gesture for the in-game menu, false if we are using the back key */
    public final boolean inGameMenuIsSwipGesture;

    /** Maximum number of auto saves */
    public final int maxAutoSaves;

    /** True if specific game data should be saved in a flat file structure */
    public final boolean useFlatGameDataPath;

    /** True of volume keys are mappable*/
    public final boolean volKeysMappable;

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

    /** True if we want to show built in emulation profiles */
    public final boolean showBuiltInEmulationProfiles;

    /** True if we want to show built in touchscreen profiles */
    public final boolean showBuiltInTouchscreenProfiles;

    /** True if we want to show built in touchscreen profiles */
    public final boolean showBuiltInControllerProfiles;

    /** True to use a high priority thread for the core */
    public final boolean useHighPriorityThread;

    // Shared preferences keys and key templates
    public static final String KEY_EMULATION_PROFILE_DEFAULT = "emulationProfileDefault";
    public static final String KEY_TOUCHSCREEN_PROFILE_DEFAULT = "touchscreenProfileDefault";
    public static final String KEY_TOUCHSCREEN_DPAD_PROFILE_DEFAULT = "touchscreenProfileDpadDefault";
    public static final String KEYTEMPLATE_PAK_TYPE = "inputPakType%1$d";
    public static final String KEY_PLAYER_MAP_REMINDER = "playerMapReminder2";
    public static final String KEY_LOCALE_OVERRIDE = "localeOverride";
    // ... add more as needed

    // Shared preferences default values
    public static final String DEFAULT_EMULATION_PROFILE_DEFAULT = "Glide64-Fast";
    public static final String DEFAULT_TOUCHSCREEN_PROFILE_DEFAULT = "Analog";
    public static final String DEFAULT_TOUCHSCREEN_DPAD_PROFILE_DEFAULT = "Everything";
    public static final String DEFAULT_CONTROLLER_PROFILE_DEFAULT = "Android Gamepad";
    public static final int DEFAULT_PAK_TYPE = NativeConstants.PAK_TYPE_MEMORY;
    public static final boolean DEFAULT_PLAYER_MAP_REMINDER = false;
    public static final String DEFAULT_LOCALE_OVERRIDE = "";
    // ... add more as needed

    private final SharedPreferences mPreferences;
    private final String mLocaleCode;
    private final String[] mLocaleNames;
    private final String[] mLocaleCodes;

    private final String supportedGlesVersion;

    //Pak Type
    public enum PakType {
        NONE(NativeConstants.PAK_TYPE_NONE, R.string.menuItem_pak_empty),
        MEMORY(NativeConstants.PAK_TYPE_MEMORY, R.string.menuItem_pak_mem),
        RAMBLE(NativeConstants.PAK_TYPE_RUMBLE, R.string.menuItem_pak_rumble);

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
                default:
                    return NONE;

            }
        }

        public int getResourceString()
        {
            return mResourceStringName;
        }
    }

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
            if( ArrayUtils.contains( availableLocales, locale ) )
            {
                // Get the name of the language, as written natively
                entries[i] = WordUtils.capitalize( locale.getDisplayName( locale ) );
            }
            else
            {
                // Remove the item from the list
                entries = (String[]) ArrayUtils.remove( entries, i );
                values = (String[]) ArrayUtils.remove( values, i );
            }
        }
        entries[0] = context.getString( R.string.localeOverride_entrySystemDefault );
        mLocaleNames = entries;
        mLocaleCodes = values;

        // Files
        galleryCacheDir = appData.userDataDir + "/GalleryCache";
        coverArtDir = galleryCacheDir + "/CoverArt";
        unzippedRomsDir = galleryCacheDir + "/UnzippedRoms";
        profilesDir = appData.userDataDir + "/Profiles";
        crashLogDir = appData.userDataDir + "/CrashLogs";
        final String coreConfigDir = appData.userDataDir + "/CoreConfig";
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
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Plug-ins
        audioPlugin = new Plugin( mPreferences, appData.libsDir, "audioPlugin" );

        // Library prefs
        isRecentShown = mPreferences.getBoolean( "showRecentlyPlayed", true );
        cacheRecentlyPlayed = mPreferences.getBoolean( "cacheRecentlyPlayed", true );
        isFullNameShown = mPreferences.getBoolean( "showFullNames", true );
        coverArtScale = ( mPreferences.getInt( "libraryArtScale", 100 ) ) / 100.0f;

        // Touchscreen prefs
        isTouchscreenFeedbackEnabled = mPreferences.getBoolean( "touchscreenFeedback", false );
        touchscreenScale = ( mPreferences.getInt( "touchscreenScaleV2", 100 ) ) / 100.0f;
        touchscreenTransparency = ( 255 * mPreferences.getInt( "touchscreenTransparencyV2", 60 ) ) / 100;
        touchscreenAutoHold = getSafeInt( mPreferences, "touchscreenAutoHoldV2", 0 );
        touchscreenAutoHideEnabled = mPreferences.getBoolean( "touchscreenAutoHideEnabled", true );
        touchscreenAutoHideSeconds = mPreferences.getInt( "touchscreenAutoHideSeconds", 5 );
        isTouchscreenAnimated = mPreferences.getBoolean( "touchscreenAnimated_v2", true );

        // Video prefs
        displayResolution = getSafeInt( mPreferences, GamePrefs.DISPLAY_RESOLUTION, 480 );
        stretchScreen = mPreferences.getString( "displayScaling", "original" ).equals("stretch");
        isImmersiveModeEnabled = mPreferences.getBoolean( "displayImmersiveMode_v2", true );
        DetermineResolutionData(context);
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

        // Audio prefs
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioSwapChannels = mPreferences.getBoolean( "audioSwapChannels", false );
        enableSLESAudioTimeSretching = mPreferences.getBoolean( "audioSLESTimeStretch", true );
        audioSLESSecondaryBufferNbr = getSafeInt( mPreferences, "audioSLESBufferNbr2", 10 );
        audioSLESFloatingPoint = mPreferences.getBoolean( "audioSLESFloatingPoint", false );

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
        if( navMode.equals( "bigscreen" ) )
            isBigScreenMode = true;
        else if( navMode.equals( "standard" ) )
            isBigScreenMode = false;
        else
            isBigScreenMode = appData.isAndroidTv;

        final String inGameMenuMode = mPreferences.getString( "inGameMenu", "back-key" );

        maxAutoSaves = mPreferences.getInt( "gameAutoSaves", 5 );

        useFlatGameDataPath = mPreferences.getBoolean( "useFlatGameDataPath", false );

        // Determine the key codes that should not be mapped to controls
        volKeysMappable = mPreferences.getBoolean( "inputVolumeMappable", false );
        final boolean backKeyMappable = mPreferences.getBoolean( "inputBackMappable", false );
        final boolean menuKeyMappable = mPreferences.getBoolean( "inputMenuMappable", false );

        inGameMenuIsSwipGesture = inGameMenuMode.equals("swipe") || menuKeyMappable || backKeyMappable;

        final List<Integer> unmappables = new ArrayList<Integer>();

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
        controllerProfile1 = loadControllerProfile( mPreferences, GamePrefs.CONTROLLER_PROFILE1,
                getControllerProfileDefault(1),
                GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile2 = loadControllerProfile( mPreferences, GamePrefs.CONTROLLER_PROFILE2,
                getControllerProfileDefault(2),
                GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile3 = loadControllerProfile( mPreferences, GamePrefs.CONTROLLER_PROFILE3,
                getControllerProfileDefault(3),
                GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );
        controllerProfile4 = loadControllerProfile( mPreferences, GamePrefs.CONTROLLER_PROFILE4,
                getControllerProfileDefault(4),
                GetControllerProfilesConfig(), appData.GetControllerProfilesConfig() );

        // Player map
        playerMap = new PlayerMap( mPreferences.getString( GamePrefs.PLAYER_MAP, "" ) );

        // Determine whether controller deconfliction is needed
        int numControllers = 0;
        numControllers += controllerProfile1 != null ? 1 : 0;
        numControllers += controllerProfile2 != null ? 1 : 0;
        numControllers += controllerProfile3 != null ? 1 : 0;
        numControllers += controllerProfile4 != null ? 1 : 0;

        playerMap.setEnabled( numControllers > 1 );

        showBuiltInEmulationProfiles = mPreferences.getBoolean(ManageEmulationProfilesActivity.SHOW_BUILT_IN_PREF_KEY, true);
        showBuiltInTouchscreenProfiles = mPreferences.getBoolean(ManageTouchscreenProfilesActivity.SHOW_BUILT_IN_PREF_KEY, true);
        showBuiltInControllerProfiles = mPreferences.getBoolean(ManageControllerProfilesActivity.SHOW_BUILT_IN_PREF_KEY, true);

        useHighPriorityThread = mPreferences.getBoolean( "useHighPriorityThread", false );

        supportedGlesVersion = AppData.getOpenGlEsVersion(context);

        currentDisplayOrientation = context.getResources().getConfiguration().orientation;
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

    public String getEmulationProfileDefaultDefault()
    {
        String defaultEmulationProfile = DEFAULT_EMULATION_PROFILE_DEFAULT;

        if(AppData.doesSupportFullGL())
        {
            defaultEmulationProfile = "GlideN64-Very-Accurate";
        }
        else if(supportedGlesVersion.equals("3.1"))
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
            case 2: return getString( GamePrefs.CONTROLLER_PROFILE2, "" );
            case 3: return getString( GamePrefs.CONTROLLER_PROFILE3, "" );
            case 4: return getString( GamePrefs.CONTROLLER_PROFILE4, "" );
            default: break;
        }

        return getString( GamePrefs.CONTROLLER_PROFILE1, DEFAULT_CONTROLLER_PROFILE_DEFAULT );
    }

    public PakType getPakType( int player )
    {
        return PakType.getPakTypeFromNativeValue(getInt( KEYTEMPLATE_PAK_TYPE, player, DEFAULT_PAK_TYPE ));
    }

    public boolean getPlayerMapReminder()
    {
        return getBoolean( KEY_PLAYER_MAP_REMINDER, DEFAULT_PLAYER_MAP_REMINDER );
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

    public void putPakType( int player, PakType pakType )
    {
        putInt( KEYTEMPLATE_PAK_TYPE, player, pakType.getNativeValue() );
    }

    public void putPlayerMapReminder( boolean value )
    {
        putBoolean( KEY_PLAYER_MAP_REMINDER, value );
    }

    public boolean getBoolean( String key, boolean defaultValue )
    {
        return mPreferences.getBoolean( key, defaultValue );
    }

    public int getInt( String keyTemplate, int index, int defaultValue )
    {
        final String key = String.format( Locale.US, keyTemplate, index );
        return mPreferences.getInt( key, defaultValue );
    }

    public String getString( String key, String defaultValue )
    {
        return mPreferences.getString( key, defaultValue );
    }

    private void putBoolean( String key, boolean value )
    {
        mPreferences.edit().putBoolean( key, value ).apply();
    }

    private void putInt( String keyTemplate, int index, int value )
    {
        final String key = String.format( Locale.US, keyTemplate, index );
        mPreferences.edit().putInt( key, value ).apply();
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

    private void DetermineResolutionData(Context context)
    {
        // Determine the pixel dimensions of the rendering context and view surface
        // Screen size
        final WindowManager windowManager = (WindowManager) context.getSystemService(android.content.Context.WINDOW_SERVICE);
        final Display display = windowManager.getDefaultDisplay();

        final Point dimensions = new Point();

        if( display == null )
        {
            videoSurfaceWidthStretch = videoSurfaceHeightStretch = 0;
        }
        else if(isImmersiveModeEnabled )
        {

            display.getRealSize(dimensions);
            videoSurfaceWidthStretch = dimensions.x;
            videoSurfaceHeightStretch = dimensions.y;
        }
        else
        {
            display.getSize(dimensions);
            videoSurfaceWidthStretch = dimensions.x;
            videoSurfaceHeightStretch = dimensions.y;
        }

        final float aspect = 0.75f; // TODO: Handle PAL

        boolean portrait = videoSurfaceHeightStretch > videoSurfaceWidthStretch;
        if(portrait)
        {
            videoSurfaceWidthOriginal = videoSurfaceWidthStretch;
            videoSurfaceHeightOriginal = Math.round( videoSurfaceWidthOriginal*aspect);
        }
        else
        {
            videoSurfaceWidthOriginal = Math.round( videoSurfaceHeightStretch / aspect );
            videoSurfaceHeightOriginal = videoSurfaceHeightStretch;
        }
    }

    public int getResolutionWidth(boolean stretch, boolean fixAspect, int hResolution)
    {
        if( hResolution == -1)
        {
            hResolution = displayResolution;
        }

        // Display prefs, default value is the global default
        int tempVideoRenderWidth = 0;

        switch( hResolution )
        {
            case 720:
                tempVideoRenderWidth = 960;
                break;
            case 600:
                tempVideoRenderWidth = 800;
                break;
            case 480:
                tempVideoRenderWidth = 640;
                break;
            case 360:
                tempVideoRenderWidth = 480;
                break;
            case 240:
                tempVideoRenderWidth = 320;
                break;
            case 120:
                tempVideoRenderWidth = 160;
                break;
            case 0:
                hResolution = videoSurfaceHeightOriginal;
                tempVideoRenderWidth = videoSurfaceWidthOriginal;
                break;
            default:
                break;
        }

        boolean isPortrait = (displayOrientation != -1 &&
                displayOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                displayOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) ||
                (displayOrientation == -1 && currentDisplayOrientation == ORIENTATION_PORTRAIT);

        if(stretch || fixAspect)
        {
            //If we are in stretch mode we have to increase the approppriate dimension by the corresponding
            //ratio to make it full screen
            if(!isPortrait)
            {
                float widthRatio = (float)videoSurfaceWidthStretch/(float)videoSurfaceWidthOriginal;
                final float newWidth = tempVideoRenderWidth * widthRatio;
                tempVideoRenderWidth = Math.round(newWidth);
            }
            else if(fixAspect)
            {
                float screenAspect = videoSurfaceHeightStretch*1.0f/videoSurfaceWidthStretch;
                tempVideoRenderWidth = Math.round(hResolution*screenAspect);
            }
        }

        return tempVideoRenderWidth;
    }

    public int getResolutionHeight(boolean stretch, boolean fixAspect, int hResolution)
    {
        if( hResolution == -1)
        {
            hResolution = displayResolution;
        }

        // Display prefs, default value is the global default
        int tempVideoRenderHeight = 0;

        switch( hResolution )
        {
            case 0:
                tempVideoRenderHeight = videoSurfaceHeightOriginal;
                break;
            default:
                tempVideoRenderHeight = hResolution;
                break;
        }

        boolean isPortrait = (displayOrientation != -1 &&
                displayOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                displayOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) ||
                (displayOrientation == -1 && currentDisplayOrientation == ORIENTATION_PORTRAIT);

        if(stretch)
        {
            float heightRatio = (float)videoSurfaceHeightStretch/(float)videoSurfaceHeightOriginal;

            //If we are in stretch mode we have to increase the approppriate dimension by the corresponding
            //ratio to make it full screen
            if(isPortrait)
            {
                final float newWidth = tempVideoRenderHeight * heightRatio;
                tempVideoRenderHeight = Math.round(newWidth);
            }
        }

        if(fixAspect && isPortrait)
        {
            int width = getResolutionWidth(false, fixAspect, hResolution);
            float aspect = videoSurfaceHeightStretch*1.0f/videoSurfaceWidthStretch;
            tempVideoRenderHeight = Math.round(width/aspect);
        }

        return tempVideoRenderHeight;
    }
}
