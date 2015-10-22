/**
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.WordUtils;
import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.jni.NativeConstants;
import paulscode.android.mupen64plusae.persistent.AppData.HardwareInfo;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.profile.Profile;
import paulscode.android.mupen64plusae.util.Plugin;
import paulscode.android.mupen64plusae.util.SafeMethods;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;

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
    /** The parent directory containing all user-writable data files. */
    public final String userDataDir;
    
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
    
    /** The directory containing all custom touchscreen skin folders. */
    public final String touchscreenCustomSkinsDir;
    
    /** The path of the rom info cache for the gallery. */
    public final String romInfoCache_cfg;
    
    /** The path of the custom controller profiles file. */
    public final String controllerProfiles_cfg;
    
    /** The path of the custom touchscreen profiles file. */
    public final String touchscreenProfiles_cfg;
    
    /** The path of the custom emulation profiles file. */
    public final String emulationProfiles_cfg;
    
    /** The path of the custom touchpad profile */
    public final String touchpadProfiles_cfg;
    
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
    
    /** True if Xperia Play touchpad is enabled. */
    public final boolean isTouchpadEnabled;
    
    /** True if Xperia Play touchpad feedback is enabled. */
    public final boolean isTouchpadFeedbackEnabled;
    
    /** The directory of the selected Xperia Play skin. */
    public final String touchpadSkin;
    
    /** The touchpad profile. */
    public Profile touchpadProfile;
    
    /** True if a single peripheral device can control multiple players concurrently. */
    public final boolean isControllerShared;
    
    /** The set of key codes that are not allowed to be mapped. **/
    public final List<Integer> unmappableKeyCodes;
    
    /** True if the recently played section of the gallery should be shown. */
    public final boolean isRecentShown;
    
    /** True if the full ROM rip info should be shown. */
    public final boolean isFullNameShown;
    
    /** The screen orientation for the game activity. */
    public final int displayOrientation;
    
    /** The vertical screen position. */
    public final int displayPosition;
    
    /** The width of the viewing surface, in pixels. */
    public final int videoSurfaceWidth;
    
    /** The height of the viewing surface, in pixels. */
    public final int videoSurfaceHeight;
    
    /** The action bar transparency value. */
    public final int displayActionBarTransparency;
    
    /** True if the FPS indicator is displayed. */
    public final boolean isFpsEnabled;
    
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
    
    /** True if the left and right audio channels are swapped. */
    public final boolean audioSwapChannels;
    
    /** Size of secondary buffer in output samples. This is SDL's hardware buffer, which directly affects latency. */
    public final int audioSDLSecondaryBufferSize;
    
    /** Size of secondary buffer in output samples. This is SLES's hardware buffer, which directly affects latency. */
    public final int audioSLESSecondaryBufferSize;
    
    /** Number of SLES secondary buffers. */
    public final int audioSLESSecondaryBufferNbr;
    
    /** True if big-screen navigation mode is enabled. */
    public final boolean isBigScreenMode;
    
    /** Maximum number of auto saves */
    public final int maxAutoSaves;
    
    // Shared preferences keys and key templates
    private static final String KEY_EMULATION_PROFILE_DEFAULT = "emulationProfileDefault";
    private static final String KEY_TOUCHSCREEN_PROFILE_DEFAULT = "touchscreenProfileDefault";
    private static final String KEY_CONTROLLER_PROFILE_DEFAULT = "controllerProfileDefault";
    private static final String KEYTEMPLATE_PAK_TYPE = "inputPakType%1$d";
    private static final String KEY_PLAYER_MAP_REMINDER = "playerMapReminder";
    private static final String KEY_LOCALE_OVERRIDE = "localeOverride";
    // ... add more as needed
    
    // Shared preferences default values
    public static final String DEFAULT_EMULATION_PROFILE_DEFAULT = "Glide64-Fast";
    public static final String DEFAULT_TOUCHSCREEN_PROFILE_DEFAULT = AppData.IS_OUYA_HARDWARE ? "" : "Analog";
    public static final String DEFAULT_CONTROLLER_PROFILE_DEFAULT = AppData.IS_OUYA_HARDWARE ? "OUYA" : "";
    public static final int DEFAULT_PAK_TYPE = NativeConstants.PAK_TYPE_MEMORY;
    public static final boolean DEFAULT_PLAYER_MAP_REMINDER = true;
    public static final String DEFAULT_LOCALE_OVERRIDE = "";
    public static final boolean DEFAULT_SEARCH_ZIPS = true;
    public static final boolean DEFAULT_DOWNLOAD_ART = true;
    public static final boolean DEFAULT_CLEAR_GALLERY = true;
    // ... add more as needed
    
    private final SharedPreferences mPreferences;
    private final Locale mLocale;
    private final String mLocaleCode;
    private final String[] mLocaleNames;
    private final String[] mLocaleCodes;
    
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
    @SuppressWarnings( "deprecation" )
    @SuppressLint( "InlinedApi" )
    @TargetApi( 17 )
    public GlobalPrefs( Context context, AppData appData )
    {
        mPreferences = PreferenceManager.getDefaultSharedPreferences( context );
        
        // Locale
        mLocaleCode = mPreferences.getString( KEY_LOCALE_OVERRIDE, DEFAULT_LOCALE_OVERRIDE );
        mLocale = TextUtils.isEmpty( mLocaleCode ) ? Locale.getDefault() : createLocale( mLocaleCode );
        Locale[] availableLocales = Locale.getAvailableLocales();
        String[] values = context.getResources().getStringArray( R.array.localeOverride_values );
        String[] entries = new String[values.length];
        for( int i = values.length - 1; i > 0; i-- )
        {
            Locale locale = createLocale( values[i] );
            
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
        userDataDir = mPreferences.getString( "pathGameSaves", "" );
        galleryCacheDir = userDataDir + "/GalleryCache";
        coverArtDir = galleryCacheDir + "/CoverArt";
        unzippedRomsDir = galleryCacheDir + "/UnzippedRoms";
        profilesDir = userDataDir + "/Profiles";
        crashLogDir = userDataDir + "/CrashLogs";
        coreUserDataDir = userDataDir + "/CoreConfig/UserData";
        coreUserCacheDir = userDataDir + "/CoreConfig/UserCache";
        hiResTextureDir = coreUserDataDir + "/mupen64plus/hires_texture/"; // MUST match what rice assumes natively
        romInfoCache_cfg = galleryCacheDir + "/romInfoCache.cfg";
        controllerProfiles_cfg = profilesDir + "/controller.cfg";
        touchscreenProfiles_cfg = profilesDir + "/touchscreen.cfg";
        emulationProfiles_cfg = profilesDir + "/emulation.cfg";
        customCheats_txt = profilesDir + "/customCheats.txt";
        touchscreenCustomSkinsDir = userDataDir + "/CustomSkins";
        
        // Plug-ins
        audioPlugin = new Plugin( mPreferences, appData.libsDir, "audioPlugin" );
        
        // Library prefs
        isRecentShown = mPreferences.getBoolean( "showRecentlyPlayed", true );
        isFullNameShown = mPreferences.getBoolean( "showFullNames", true );
        
        // Touchscreen prefs
        isTouchscreenFeedbackEnabled = mPreferences.getBoolean( "touchscreenFeedback", false );
        touchscreenScale = ( (float) mPreferences.getInt( "touchscreenScale", 100 ) ) / 100.0f;
        touchscreenTransparency = ( 255 * mPreferences.getInt( "touchscreenTransparency", 100 ) ) / 100;
        touchscreenAutoHold = getSafeInt( mPreferences, "touchscreenAutoHold", 0 );
        
        // Xperia PLAY touchpad prefs
        isTouchpadEnabled = appData.hardwareInfo.isXperiaPlay && mPreferences.getBoolean( "touchpadEnabled", true );
        isTouchpadFeedbackEnabled = mPreferences.getBoolean( "touchpadFeedback", false );
        touchpadSkin = appData.touchpadSkinsDir + "/Xperia-Play";

        touchpadProfiles_cfg = appData.touchpadProfiles_cfg;
        
        // Video prefs
        displayOrientation = getSafeInt( mPreferences, "displayOrientation", 0 );
        displayPosition = getSafeInt( mPreferences, "displayPosition", Gravity.CENTER_VERTICAL );
        int transparencyPercent = mPreferences.getInt( "displayActionBarTransparency", 50 );
        displayActionBarTransparency = ( 255 * transparencyPercent ) / 100;
        isFpsEnabled = mPreferences.getBoolean( "displayFps", false );
        int selectedHardwareType = getSafeInt( mPreferences, "videoHardwareType", -1 );
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
                videoPolygonOffset = -1.5f;
                break;
            default:
                videoPolygonOffset = SafeMethods.toFloat( mPreferences.getString( "videoPolygonOffset", "-1.5" ), -1.5f );
                break;
        }
        isImmersiveModeEnabled = mPreferences.getBoolean( "displayImmersiveMode", false );
        
        // Audio prefs
        audioSwapChannels = mPreferences.getBoolean( "audioSwapChannels", false );
        audioSDLSecondaryBufferSize = getSafeInt( mPreferences, "audioSDLBufferSize", 2048 );
        audioSLESSecondaryBufferSize = getSafeInt( mPreferences, "audioSLESBufferSize", 1024 );
        audioSLESSecondaryBufferNbr = getSafeInt( mPreferences, "audioSLESBufferNbr", 2 );
        
        if( audioPlugin.enabled )
            isFramelimiterEnabled = mPreferences.getBoolean( "audioSynchronize", true );
        else
            isFramelimiterEnabled = !mPreferences.getString( "audioPlugin", "" ).equals( "nospeedlimit" );
        
        // User interface modes
        String navMode = mPreferences.getString( "navigationMode", "auto" );
        if( navMode.equals( "bigscreen" ) )
            isBigScreenMode = true;
        else if( navMode.equals( "standard" ) )
            isBigScreenMode = false;
        else
            isBigScreenMode = AppData.IS_OUYA_HARDWARE || appData.isAndroidTv; // TODO: Add other systems as they enter market
        
        // Peripheral share mode
        isControllerShared = mPreferences.getBoolean( "inputShareController", false );
        
        maxAutoSaves = mPreferences.getInt( "gameAutoSaves", 5 );
        
        // Determine the key codes that should not be mapped to controls
        boolean volKeysMappable = mPreferences.getBoolean( "inputVolumeMappable", false );
        List<Integer> unmappables = new ArrayList<Integer>();
        unmappables.add( KeyEvent.KEYCODE_MENU );
        if( AppData.IS_HONEYCOMB )
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
        
        // Determine the pixel dimensions of the rendering context and view surface
        {
            // Screen size
            final WindowManager windowManager = (WindowManager) context.getSystemService(android.content.Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            int stretchWidth;
            int stretchHeight;
            if( display == null )
            {
                stretchWidth = stretchHeight = 0;
            }
            else if( AppData.IS_KITKAT && isImmersiveModeEnabled )
            {
                DisplayMetrics metrics = new DisplayMetrics();
                display.getRealMetrics( metrics );
                stretchWidth = metrics.widthPixels;
                stretchHeight = metrics.heightPixels;
            }
            else
            {
                stretchWidth = display.getWidth();
                stretchHeight = display.getHeight();
            }
            
            float aspect = 0.75f; // TODO: Handle PAL
            boolean isLetterboxed = ( (float) stretchHeight / (float) stretchWidth ) > aspect;
            int originalWidth = isLetterboxed ? stretchWidth : Math.round( (float) stretchHeight / aspect );
            int originalHeight = isLetterboxed ? Math.round( (float) stretchWidth * aspect ) : stretchHeight;
            
            String scaling = mPreferences.getString( "displayScaling", "original" );

            // Native resolution
            if( scaling.equals( "stretch" ) )
            {
                videoSurfaceWidth = stretchWidth;
                videoSurfaceHeight = stretchHeight;
            }
            else // scaling.equals( "original")
            {
                videoSurfaceWidth = originalWidth;
                videoSurfaceHeight = originalHeight;
            }
        }
    }
    
    public void enforceLocale( Activity activity )
    {
        Configuration config = activity.getBaseContext().getResources().getConfiguration();
        if( !mLocale.equals( config.locale ) )
        {
            config.locale = mLocale;
            activity.getBaseContext().getResources().updateConfiguration( config, null );
        }
    }
    
    public void changeLocale( final Activity activity )
    {
        // Get the index of the current locale
        final int currentIndex = ArrayUtils.indexOf( mLocaleCodes, mLocaleCode );
        
        // Populate and show the language menu
        Builder builder = new Builder( activity );
        builder.setTitle( R.string.menuItem_localeOverride );
        builder.setSingleChoiceItems( mLocaleNames, currentIndex, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                dialog.dismiss();
                if( which >= 0 && which != currentIndex )
                {
                    mPreferences.edit().putString( KEY_LOCALE_OVERRIDE, mLocaleCodes[which] ).commit();
                    ActivityHelper.restartActivity( activity );
                }
            }
        } );
        builder.create().show();
    }

    public String getEmulationProfileDefault()
    {
        return getString( KEY_EMULATION_PROFILE_DEFAULT, DEFAULT_EMULATION_PROFILE_DEFAULT );
    }
    
    public String getTouchscreenProfileDefault()
    {
        return getString( KEY_TOUCHSCREEN_PROFILE_DEFAULT, DEFAULT_TOUCHSCREEN_PROFILE_DEFAULT );
    }
    
    public String getControllerProfileDefault()
    {
        return getString( KEY_CONTROLLER_PROFILE_DEFAULT, DEFAULT_CONTROLLER_PROFILE_DEFAULT );
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
    
    public void putControllerProfileDefault( String value )
    {
        putString( KEY_CONTROLLER_PROFILE_DEFAULT, value );
    }
    
    public void putPakType( int player, PakType pakType )
    {
        putInt( KEYTEMPLATE_PAK_TYPE, player, pakType.getNativeValue() );
    }
    
    public void putPlayerMapReminder( boolean value )
    {
        putBoolean( KEY_PLAYER_MAP_REMINDER, value );
    }
    
    private boolean getBoolean( String key, boolean defaultValue )
    {
        return mPreferences.getBoolean( key, defaultValue );
    }
    
    private int getInt( String keyTemplate, int index, int defaultValue )
    {
        String key = String.format( Locale.US, keyTemplate, index );
        return mPreferences.getInt( key, defaultValue );
    }
    
    private String getString( String key, String defaultValue )
    {
        return mPreferences.getString( key, defaultValue );
    }
    
    private void putBoolean( String key, boolean value )
    {
        mPreferences.edit().putBoolean( key, value ).commit();
    }
    
    private void putInt( String keyTemplate, int index, int value )
    {
        String key = String.format( Locale.US, keyTemplate, index );
        mPreferences.edit().putInt( key, value ).commit();
    }
    
    private void putString( String key, String value )
    {
        mPreferences.edit().putString( key, value ).commit();
    }
    
    private Locale createLocale( String code )
    {
        String[] codes = code.split( "_" );
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
            return Integer.parseInt( preferences.getString( key, String.valueOf( defaultValue ) ) );
        }
        catch( NumberFormatException ex )
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
    
    public Profile GetTouchpadProfile()
    {
        if(touchpadProfile == null)
        {
            ConfigFile touchpad_cfg = new ConfigFile(touchpadProfiles_cfg);
            ConfigSection section = touchpad_cfg.get( mPreferences.getString( "touchpadLayout", "" ) );
            if( section != null )
                touchpadProfile = new Profile( true, section );
            else
                touchpadProfile = null;
        }
        
        return touchpadProfile;
    }
    
}
