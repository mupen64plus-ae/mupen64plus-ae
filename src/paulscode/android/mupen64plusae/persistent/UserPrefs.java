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

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.jni.NativeConstants;
import paulscode.android.mupen64plusae.util.OUYAInterface;
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
public class UserPrefs
{
    /** The parent directory containing all save files. */
    public final String gameSaveDir;
    
    /** The subdirectory containing slot save files. */
    public final String slotSaveDir;
    
    /** The subdirectory containing SRAM/EEPROM data (in-game saves). */
    public final String sramSaveDir;
    
    /** The subdirectory containing auto save files. */
    public final String autoSaveDir;
    
    /** The subdirectory containing custom profiles. */
    public final String profilesDir;
    
    /** The subdirectory returned from the core's ConfigGetUserConfigPath() method. Location of core config file. */
    public final String coreUserConfigDir;
    
    /** The subdirectory returned from the core's ConfigGetUserDataPath() method. */
    public final String coreUserDataDir;
    
    /** The subdirectory returned from the core's ConfigGetUserCachePath() method. */
    public final String coreUserCacheDir;
    
    /** The subdirectory where hi-res textures must be unzipped. */
    public final String hiResTextureDir;
    
    /** The path of the Mupen64Plus base configuration file. */
    public final String mupen64plus_cfg;
    
    /** The path of the custom controller profiles file. */
    public final String controllerProfiles_cfg;
    
    /** The path of the custom touchscreen profiles file. */
    public final String touchscreenProfiles_cfg;
    
    /** The path of the custom emulation profiles file. */
    public final String emulationProfiles_cfg;
    
    /** The path of the user's custom cheat files. */
    public final String usrcheat_txt;
    
    /** The selected audio plug-in. */
    public final Plugin audioPlugin;
    
    /** True if Xperia Play touchpad is enabled. */
    public final boolean isTouchpadEnabled;
    
    /** True if Xperia Play touchpad feedback is enabled. */
    public final boolean isTouchpadFeedbackEnabled;
    
    /** The filename of the selected Xperia Play layout. */
    public final String touchpadLayout;
    
    /** True if a single peripheral device can control multiple players concurrently. */
    public final boolean isControllerShared;
    
    /** The set of key codes that are not allowed to be mapped. **/
    public final List<Integer> unmappableKeyCodes;
    
    /** The screen orientation for the game activity. */
    public final int displayOrientation;
    
    /** The vertical screen position. */
    public final int displayPosition;
    
    /** The width of the OpenGL rendering context, in pixels. */
    public final int videoRenderWidth;
    
    /** The height of the OpenGL rendering context, in pixels. */
    public final int videoRenderHeight;
    
    /** The width of the viewing surface, in pixels. */
    public final int videoSurfaceWidth;
    
    /** The height of the viewing surface, in pixels. */
    public final int videoSurfaceHeight;
    
    /** The action bar transparency value. */
    public final int displayActionBarTransparency;
    
    /** The number of frames over which FPS is calculated (0 = disabled). */
    public final int displayFpsRefresh;
    
    /** True if the FPS indicator is displayed. */
    public final boolean isFpsEnabled;
    
    /** True if immersive mode should be used (KitKat only). */
    public final boolean isImmersiveModeEnabled;
    
    /** True if framelimiter is used. */
    public final boolean isFramelimiterEnabled;
    
    /** The manually-overridden hardware type, used for flicker reduction. */
    public final int videoHardwareType;
    
    /** The polygon offset to use if hardware type is 'custom'. */
    public final float videoPolygonOffset;
    
    /** True if the left and right audio channels are swapped. */
    public final boolean audioSwapChannels;
    
    /** Size of secondary buffer in output samples. This is SDL's hardware buffer, which directly affects latency. */
    public final int audioSecondaryBufferSize;
    
    /** True if big-screen navigation mode is enabled. */
    public final boolean isBigScreenMode;
    
    /** True if the action bar is available. */
    public final boolean isActionBarAvailable;
    
    // Shared preferences keys and key templates
    private static final String KEY_EMULATION_PROFILE_DEFAULT = "emulationProfileDefault";
    private static final String KEY_TOUCHSCREEN_PROFILE_DEFAULT = "touchscreenProfileDefault";
    private static final String KEY_CONTROLLER_PROFILE_DEFAULT = "controllerProfileDefault";
    private static final String KEYTEMPLATE_PAK_TYPE = "inputPakType%1$d";
    private static final String KEY_PLAYER_MAP_REMINDER = "playerMapReminder";
    private static final String KEY_LOCALE_OVERRIDE = "localeOverride";
    // ... add more as needed
    
    // Shared preferences default values
    public static final String DEFAULT_EMULATION_PROFILE_DEFAULT = "Speed-gln64";
    public static final String DEFAULT_TOUCHSCREEN_PROFILE_DEFAULT = "";
    public static final String DEFAULT_CONTROLLER_PROFILE_DEFAULT = "";
    public static final int DEFAULT_PAK_TYPE = NativeConstants.PAK_TYPE_MEMORY;
    public static final boolean DEFAULT_PLAYER_MAP_REMINDER = true;
    public static final String DEFAULT_LOCALE_OVERRIDE = "";
    public static final String DEFAULT_PATH_SELECTED_GAME = "~roms/n64";
    // ... add more as needed
    
    private final SharedPreferences mPreferences;
    private final Locale mLocale;
    private final String mLocaleCode;
    private final String[] mLocaleNames;
    private final String[] mLocaleCodes;
    
    /**
     * Instantiates a new user preferences wrapper.
     * 
     * @param context The application context.
     */
    @SuppressWarnings( "deprecation" )
    @SuppressLint( "InlinedApi" )
    @TargetApi( 17 )
    public UserPrefs( Context context )
    {
        AppData appData = new AppData( context );
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
        gameSaveDir = mPreferences.getString( "pathGameSaves", "" );
        slotSaveDir = gameSaveDir + "/SlotSaves";
        sramSaveDir = slotSaveDir; // Version3: consider gameSaveDir + "/InGameSaves";
        autoSaveDir = gameSaveDir + "/AutoSaves";
        profilesDir = gameSaveDir + "/Profiles";
        coreUserConfigDir = gameSaveDir + "/CoreConfig/UserConfig";
        coreUserDataDir = gameSaveDir + "/CoreConfig/UserData";
        coreUserCacheDir = gameSaveDir + "/CoreConfig/UserCache";
        hiResTextureDir = coreUserDataDir + "/mupen64plus/hires_texture/"; // MUST match what rice assumes natively
        mupen64plus_cfg = coreUserConfigDir + "/mupen64plus.cfg";
        controllerProfiles_cfg = profilesDir + "/controller.cfg";
        touchscreenProfiles_cfg = profilesDir + "/touchscreen.cfg";
        emulationProfiles_cfg = profilesDir + "/emulation.cfg";
        usrcheat_txt = profilesDir + "/usrcheat.txt";
        
        // Plug-ins
        audioPlugin = new Plugin( mPreferences, appData.libsDir, "audioPlugin" );
        
        // Xperia PLAY touchpad prefs
        isTouchpadEnabled = appData.hardwareInfo.isXperiaPlay && mPreferences.getBoolean( "touchpadEnabled", true );
        isTouchpadFeedbackEnabled = mPreferences.getBoolean( "touchpadFeedback", false );
        touchpadLayout = appData.touchpadLayoutsDir + mPreferences.getString( "touchpadLayout", "" );
        
        // Video prefs
        displayOrientation = getSafeInt( mPreferences, "displayOrientation", 0 );
        displayPosition = getSafeInt( mPreferences, "displayPosition", Gravity.CENTER_VERTICAL );
        int transparencyPercent = mPreferences.getInt( "displayActionBarTransparency", 50 );
        displayActionBarTransparency = ( 255 * transparencyPercent ) / 100;
        displayFpsRefresh = getSafeInt( mPreferences, "displayFpsRefresh", 0 );
        isFpsEnabled = displayFpsRefresh > 0;
        videoHardwareType = getSafeInt( mPreferences, "videoHardwareType", -1 );
        videoPolygonOffset = SafeMethods.toFloat( mPreferences.getString( "videoPolygonOffset", "-0.2" ), -0.2f );
        isImmersiveModeEnabled = mPreferences.getBoolean( "displayImmersiveMode", false );
        
        // Audio prefs
        audioSwapChannels = mPreferences.getBoolean( "audioSwapChannels", false );
        audioSecondaryBufferSize = getSafeInt( mPreferences, "audioBufferSize", 2048 );
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
            isBigScreenMode = OUYAInterface.IS_OUYA_HARDWARE; // TODO: Add other systems as they enter market
        isActionBarAvailable = AppData.IS_HONEYCOMB && !isBigScreenMode;
        
        // Peripheral share mode
        isControllerShared = mPreferences.getBoolean( "inputShareController", false );
        
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
            int zoomWidth = isLetterboxed ? stretchWidth : Math.round( (float) stretchHeight / aspect );
            int zoomHeight = isLetterboxed ? Math.round( (float) stretchWidth * aspect ) : stretchHeight;
            int cropWidth = isLetterboxed ? Math.round( (float) stretchHeight / aspect ) : stretchWidth;
            int cropHeight = isLetterboxed ? stretchHeight : Math.round( (float) stretchWidth * aspect );
            
            int hResolution = getSafeInt( mPreferences, "displayResolution", 0 );
            String scaling = mPreferences.getString( "displayScaling", "zoom" );
            if( hResolution == 0 )
            {
                // Native resolution
                if( scaling.equals( "stretch" ) )
                {
                    videoRenderWidth = videoSurfaceWidth = stretchWidth;
                    videoRenderHeight = videoSurfaceHeight = stretchHeight;
                }
                else if( scaling.equals( "crop" ) )
                {
                    videoRenderWidth = videoSurfaceWidth = cropWidth;
                    videoRenderHeight = videoSurfaceHeight = cropHeight;
                }
                else // scaling.equals( "zoom") || scaling.equals( "none" )
                {
                    videoRenderWidth = videoSurfaceWidth = zoomWidth;
                    videoRenderHeight = videoSurfaceHeight = zoomHeight;
                }
            }
            else
            {
                // Non-native resolution
                switch( hResolution )
                {
                    case 720:
                        videoRenderWidth = 960;
                        videoRenderHeight = 720;
                        break;
                    case 600:
                        videoRenderWidth = 800;
                        videoRenderHeight = 600;
                        break;
                    case 480:
                        videoRenderWidth = 640;
                        videoRenderHeight = 480;
                        break;
                    case 360:
                        videoRenderWidth = 480;
                        videoRenderHeight = 360;
                        break;
                    case 240:
                        videoRenderWidth = 320;
                        videoRenderHeight = 240;
                        break;
                    case 120:
                        videoRenderWidth = 160;
                        videoRenderHeight = 120;
                        break;
                    default:
                        videoRenderWidth = Math.round( (float) hResolution / aspect );
                        videoRenderHeight = hResolution;
                        break;
                }
                if( scaling.equals( "zoom" ) )
                {
                    videoSurfaceWidth = zoomWidth;
                    videoSurfaceHeight = zoomHeight;
                }
                else if( scaling.equals( "crop" ) )
                {
                    videoSurfaceWidth = cropWidth;
                    videoSurfaceHeight = cropHeight;
                }
                else if( scaling.equals( "stretch" ) )
                {
                    videoSurfaceWidth = stretchWidth;
                    videoSurfaceHeight = stretchHeight;
                }
                else // scaling.equals( "none" )
                {
                    videoSurfaceWidth = videoRenderWidth;
                    videoSurfaceHeight = videoRenderHeight;
                }
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
                    activity.finish();
                    activity.startActivity( activity.getIntent() );
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
    
    public int getPakType( int player )
    {
        return getInt( KEYTEMPLATE_PAK_TYPE, player, DEFAULT_PAK_TYPE );
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
    
    public void putPakType( int player, int value )
    {
        putInt( KEYTEMPLATE_PAK_TYPE, player, value );
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
}
