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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.WordUtils;

import paulscode.android.mupen64plusae.CoreInterface;
import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.input.map.PlayerMap;
import paulscode.android.mupen64plusae.util.OUYAInterface;
import paulscode.android.mupen64plusae.util.Utility;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;

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
    /** Display names of locales that are both available on the device and translated for Mupen. */
    public final String[] localeNames;
    
    /** Codes of locales that are both available on the device and translated for Mupen. */
    public final String[] localeCodes;
    
    /** The filename of the ROM selected by the user. */
    public final String selectedGame;
    
    /** The filename of the auto-saved session of the ROM selected by the user. */
    public final String selectedGameAutoSavefile;
    
    /** The parent directory containing all save files. */
    public final String gameSaveDir;
    
    /** The subdirectory containing manual save files. */
    public final String manualSaveDir;
    
    /** The subdirectory containing slot save files. */
    public final String slotSaveDir;
    
    /** The subdirectory containing auto save files. */
    public final String autoSaveDir;
    
    /** The subdirectory containing input map profiles. */
    public final String profileDir;
    
    /** The selected video plug-in. */
    public final Plugin videoPlugin;
    
    /** The selected audio plug-in. */
    public final Plugin audioPlugin;
    
    /** The selected input plug-in. */
    public final Plugin inputPlugin;
    
    /** The selected Reality Signal Processor. */
    public final Plugin rspPlugin;
    
    /** The selected emulator core. */
    public final Plugin corePlugin;
    
    /** The selected R4300 emulator. */
    public final String r4300Emulator;
    
    /** True if the cheats category should be shown in the menu. */
    public final boolean isCheatOptionsShown;
    
    /** True if the touchscreen is enabled. */
    public final boolean isTouchscreenEnabled;
    
    /** True if the touchscreen feedback is enabled. */
    public final boolean isTouchscreenFeedbackEnabled;
    
    /** The number of frames over which touchscreen is redrawn (0 = disabled). */
    public final int touchscreenRefresh;
    
    /** The method used for auto holding buttons. */
    public final int touchscreenAutoHold;
    
    /** The set of auto-holdable button commands. */
    public final Set<Integer> touchscreenAutoHoldables;
    
    /** The touchscreen transparency value. */
    public final int touchscreenTransparency;
    
    /** True if the touchscreen overlay is hidden. */
    public final boolean isTouchscreenHidden;
    
    /** Factor applied to the final calculated visible touchmap scale. */
    public final float touchscreenScale;
    
    /** The folder name of the selected touchscreen style. */
    public final String touchscreenStyle;
    
    /** The folder name of the selected touchscreen layout. */
    public final String touchscreenLayout;
    
    /** True if a custom touchscreen is provided. */
    public final boolean isTouchscreenCustom;
    
    /** True if Xperia Play touchpad is enabled. */
    public final boolean isTouchpadEnabled;
    
    /** True if Xperia Play touchpad feedback is enabled. */
    public final boolean isTouchpadFeedbackEnabled;
    
    /** The filename of the selected Xperia Play layout. */
    public final String touchpadLayout;
    
    /** True if Player 1's controller is enabled. */
    public final boolean isInputEnabled1;
    
    /** True if Player 2's controller is enabled. */
    public final boolean isInputEnabled2;
    
    /** True if Player 3's controller is enabled. */
    public final boolean isInputEnabled3;
    
    /** True if Player 4's controller is enabled. */
    public final boolean isInputEnabled4;
    
    /** The button map for Player 1. */
    public final InputMap inputMap1;
    
    /** The button map for Player 2. */
    public final InputMap inputMap2;
    
    /** The button map for Player 3. */
    public final InputMap inputMap3;
    
    /** The button map for Player 4. */
    public final InputMap inputMap4;
    
    /** The deadzone for Player 1, in percent. */
    public final int inputDeadzone1;
    
    /** The deadzone for Player 2, in percent. */
    public final int inputDeadzone2;
    
    /** The deadzone for Player 3, in percent. */
    public final int inputDeadzone3;
    
    /** The deadzone for Player 4, in percent. */
    public final int inputDeadzone4;
    
    /** The player map for multi-player gaming. */
    public final PlayerMap playerMap;
    
    /** True if any type of AbstractController is enabled for Player 1. */
    public final boolean isPlugged1;
    
    /** True if any type of AbstractController is enabled for Player 2. */
    public final boolean isPlugged2;
    
    /** True if any type of AbstractController is enabled for Player 3. */
    public final boolean isPlugged3;
    
    /** True if any type of AbstractController is enabled for Player 4. */
    public final boolean isPlugged4;
    
    /** The set of key codes that are not allowed to be mapped. **/
    public final List<Integer> unmappableKeyCodes;
    
    /** True if the analog is constrained to an octagon. */
    public final boolean isOctagonalJoystick;
    
    /** The screen orientation for the game activity. */
    public final int videoOrientation;
    
    /** The vertical screen position. */
    public final int videoPosition;
    
    /** The width of the renderable portion of the screen. */
    public final int videoRenderWidth;
    
    /** The height of the renderable portion of the screen. */
    public final int videoRenderHeight;
    
    /** The action bar transparency value. */
    public final int videoActionBarTransparency;
    
    /** The number of frames over which FPS is calculated (0 = disabled). */
    public final int videoFpsRefresh;
    
    /** True if the FPS indicator is displayed. */
    public final boolean isFpsEnabled;
    
    /** True if the video should be stretched. */
    public final boolean isStretched;
    
    /** True if framelimiter is used. */
    public final boolean isFramelimiterEnabled;
    
    /** True if RGBA8888 mode should be used for video. */
    public final boolean isRgba8888;
    
    /** The manually-overridden hardware type, used for flicker reduction. */
    public final int videoHardwareType;
    
    /** True if gles2n64 video plug-in is enabled. */
    public final boolean isGles2N64Enabled;
    
    /** The maximum frameskip in the gles2n64 library. */
    public final int gles2N64MaxFrameskip;
    
    /** True if auto-frameskip is enabled in the gles2n64 library. */
    public final boolean isGles2N64AutoFrameskipEnabled;
    
    /** True if fog is enabled in the gles2n64 library. */
    public final boolean isGles2N64FogEnabled;
    
    /** True if SaI texture filtering is enabled in the gles2n64 library. */
    public final boolean isGles2N64SaiEnabled;
    
    /** True if force screen clear is enabled in the gles2n64 library. */
    public final boolean isGles2N64ScreenClearEnabled;
    
    /** True if alpha test is enabled in the gles2n64 library. */
    public final boolean isGles2N64AlphaTestEnabled;
    
    /** True if depth test is enabled in the gles2n64 library. */
    public final boolean isGles2N64DepthTestEnabled;
    
    /** True if gles2rice video plug-in is enabled. */
    public final boolean isGles2RiceEnabled;
    
    /** True if auto-frameskip is enabled in the gles2rice library. */
    public final boolean isGles2RiceAutoFrameskipEnabled;
    
    /** True if fast texture CRC is enabled in the gles2rice library. */
    public final boolean isGles2RiceFastTextureCrcEnabled;
    
    /** True if fast texture loading is enabled in the gles2rice library. */
    public final boolean isGles2RiceFastTextureLoadingEnabled;
    
    /** True if force texture filter is enabled in the gles2rice library. */
    public final boolean isGles2RiceForceTextureFilterEnabled;
    
    /** The mipmapping algorithm to use in gles2rice */
    public final String gles2RiceMipmappingAlg;

    /** The screen update setting to use in gles2rice */
    public final String gles2RiceScreenUpdateType;
    
    /** The texture enhancement algorithm to be used in the gles2rice library */
    public final String gles2RiceTextureEnhancement;
    
    /** True if hi-resolution textures are enabled in the gles2rice library. */
    public final boolean isGles2RiceHiResTexturesEnabled;
    
    /** True if gles2glide64 video plug-in is enabled. */
    public final boolean isGles2Glide64Enabled;
        
    /** True if the left and right audio channels are swapped. */
    public final boolean audioSwapChannels;
    
    /** The audio resampling algorithm to use. */
    public final String audioResampleAlg;
    
    /** True if OUYA navigation mode is enabled. */
    public final boolean isOuyaMode;
    
    // Shared preferences keys and key templates
    private static final String KEYTEMPLATE_PAK_TYPE = "inputPakType%1$d";
    private static final String KEYTEMPLATE_INPUT_MAP_STRING = "inputMapString%1$d";
    private static final String KEYTEMPLATE_INPUT_DEADZONE = "inputDeadzone%1$d";
    private static final String KEYTEMPLATE_SPECIAL_VISIBILITY = "inputSpecialVisibility%1$d";
    private static final String KEY_PLAYER_MAP_REMINDER = "playerMapReminder";
    // ... add more as needed
    
    // Shared preferences default values
    public static final int DEFAULT_PAK_TYPE = CoreInterface.PAK_TYPE_MEMORY;
    public static final String DEFAULT_INPUT_MAP_STRING = OUYAInterface.IS_OUYA_HARDWARE ?
            InputMap.DEFAULT_INPUT_MAP_STRING_OUYA : InputMap.DEFAULT_INPUT_MAP_STRING_GENERIC;
    public static final int DEFAULT_INPUT_DEADZONE = 0;
    public static final boolean DEFAULT_SPECIAL_VISIBILITY = false;
    public static final boolean DEFAULT_PLAYER_MAP_REMINDER = true;
    // ... add more as needed
    
    private final SharedPreferences mPreferences;
    private final Locale mLocale;
    
    /**
     * Instantiates a new user preferences wrapper.
     * 
     * @param context The application context.
     */
    @SuppressLint( "InlinedApi" )
    public UserPrefs( Context context )
    {
        AppData appData = new AppData( context );
        mPreferences = PreferenceManager.getDefaultSharedPreferences( context );
        
        // Locale
        String language = mPreferences.getString( "localeOverride", null );
        mLocale = TextUtils.isEmpty( language ) ? Locale.getDefault() : createLocale( language );
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
        localeNames = entries;
        localeCodes = values;
        
        // Files
        selectedGame = mPreferences.getString( "pathSelectedGame", "" );
        gameSaveDir = mPreferences.getString( "pathGameSaves", "" );
        slotSaveDir = gameSaveDir + "/SlotSaves";
        autoSaveDir = gameSaveDir + "/AutoSaves";
        profileDir = gameSaveDir + "/InputProfiles";
        File game = new File( selectedGame );
        manualSaveDir = gameSaveDir + "/" + game.getName();
        selectedGameAutoSavefile = autoSaveDir + "/" + game.getName() + ".sav";
        
        // Plug-ins
        videoPlugin = new Plugin( mPreferences, appData.libsDir, "pluginVideo" );
        audioPlugin = new Plugin( mPreferences, appData.libsDir, "pluginAudio" );
        inputPlugin = new Plugin( mPreferences, appData.libsDir, "pluginInput" );
        rspPlugin = new Plugin( mPreferences, appData.libsDir, "pluginRsp" );
        corePlugin = new Plugin( mPreferences, appData.libsDir, "pluginCore" );
        
        // R4300 emulator
        r4300Emulator = mPreferences.getString( "r4300Emulator", "2" );
        
        // Play menu
        isCheatOptionsShown = mPreferences.getBoolean( "playShowCheats", false );
        
        // Touchscreen prefs
        isTouchscreenEnabled = mPreferences.getBoolean( "touchscreenEnabled", true );
        isTouchscreenFeedbackEnabled = mPreferences.getBoolean( "touchscreenFeedback", false );
        touchscreenRefresh = getSafeInt( mPreferences, "touchscreenRefresh", 0 );
        touchscreenAutoHold = getSafeInt( mPreferences, "touchscreenAutoHold", 0 );
        touchscreenAutoHoldables = getSafeIntSet( mPreferences, "touchscreenAutoHoldables" );
        int transparencyPercent = mPreferences.getInt( "touchscreenTransparency", 100 );
        touchscreenTransparency = ( 255 * transparencyPercent ) / 100;
        isTouchscreenHidden = transparencyPercent == 0;
        
        // Xperia PLAY touchpad prefs
        isTouchpadEnabled = mPreferences.getBoolean( "touchpadEnabled", false );
        isTouchpadFeedbackEnabled = mPreferences.getBoolean( "touchpadFeedback", false );
        touchpadLayout = appData.touchpadLayoutsDir + mPreferences.getString( "touchpadLayout", "" );
        
        // Input prefs
        isOctagonalJoystick = mPreferences.getBoolean( "inputOctagonConstraints", true );
        isInputEnabled1 = mPreferences.getBoolean( "inputEnabled1", false );
        isInputEnabled2 = mPreferences.getBoolean( "inputEnabled2", false );
        isInputEnabled3 = mPreferences.getBoolean( "inputEnabled3", false );
        isInputEnabled4 = mPreferences.getBoolean( "inputEnabled4", false );
        
        // Controller prefs
        inputMap1 = new InputMap( getInputMapString( 1 ) );
        inputMap2 = new InputMap( getInputMapString( 2 ) );
        inputMap3 = new InputMap( getInputMapString( 3 ) );
        inputMap4 = new InputMap( getInputMapString( 4 ) );
        playerMap = new PlayerMap( mPreferences.getString( "playerMap", "" ) );
        inputDeadzone1 = getInputDeadzone( 1 );
        inputDeadzone2 = getInputDeadzone( 2 );
        inputDeadzone3 = getInputDeadzone( 3 );
        inputDeadzone4 = getInputDeadzone( 4 );
        
        // Video prefs
        videoOrientation = getSafeInt( mPreferences, "videoOrientation", 0 );
        videoPosition = getSafeInt( mPreferences, "videoPosition", Gravity.CENTER_VERTICAL );
        transparencyPercent = mPreferences.getInt( "videoActionBarTransparency", 50 );
        videoActionBarTransparency = ( 255 * transparencyPercent ) / 100;
        videoFpsRefresh = getSafeInt( mPreferences, "videoFpsRefresh", 0 );
        isFpsEnabled = videoFpsRefresh > 0;
        videoHardwareType = getSafeInt( mPreferences, "videoHardwareType", -1 );
        isStretched = mPreferences.getBoolean( "videoStretch", false );
        isRgba8888 = mPreferences.getBoolean( "videoRgba8888", false );
        isFramelimiterEnabled = mPreferences.getBoolean( "videoUseFramelimiter", false );
        
        // Video prefs - gles2n64
        isGles2N64Enabled = videoPlugin.name.equals( "libgles2n64.so" );
        int maxFrameskip = getSafeInt( mPreferences, "gles2N64Frameskip", 0 );
        isGles2N64AutoFrameskipEnabled = maxFrameskip < 0;
        gles2N64MaxFrameskip = Math.abs( maxFrameskip );
        isGles2N64FogEnabled = mPreferences.getBoolean( "gles2N64Fog", false );
        isGles2N64SaiEnabled = mPreferences.getBoolean( "gles2N64Sai", false );
        isGles2N64ScreenClearEnabled = mPreferences.getBoolean( "gles2N64ScreenClear", true );
        isGles2N64AlphaTestEnabled = mPreferences.getBoolean( "gles2N64AlphaTest", true );
        isGles2N64DepthTestEnabled = mPreferences.getBoolean( "gles2N64DepthTest", true );
        
        // Video prefs - gles2rice
        isGles2RiceEnabled = videoPlugin.name.equals( "libgles2rice.so" );
        isGles2RiceAutoFrameskipEnabled = mPreferences.getBoolean( "gles2RiceAutoFrameskip", false );
        isGles2RiceFastTextureCrcEnabled = mPreferences.getBoolean( "gles2RiceFastTextureCrc", true );
        isGles2RiceFastTextureLoadingEnabled = mPreferences.getBoolean( "gles2RiceFastTexture", false );
        isGles2RiceForceTextureFilterEnabled = mPreferences.getBoolean( "gles2RiceForceTextureFilter", false );
        gles2RiceMipmappingAlg = mPreferences.getString( "gles2RiceMipmapping", "0" );
        gles2RiceScreenUpdateType = mPreferences.getString( "gles2RiceScreenUpdate", "4" );
        gles2RiceTextureEnhancement = mPreferences.getString( "gles2RiceTextureEnhancement", "0" );
        isGles2RiceHiResTexturesEnabled = mPreferences.getBoolean( "gles2RiceHiResTextures", true );
        
        // Video prefs - gles2glide64
        isGles2Glide64Enabled = videoPlugin.name.equals( "libgles2glide64.so" );
        
        // Audio prefs
        audioSwapChannels = mPreferences.getBoolean( "audioSwapChannels", false );
        audioResampleAlg = mPreferences.getString( "audioResampleAlg", "trivial" );
        
        // Navigation mode
        String navMode = mPreferences.getString( "navigationMode", "auto" );
        if( navMode.equals( "ouya" ) )
            isOuyaMode = true;
        else if( navMode.equals( "standard" ) )
            isOuyaMode = false;
        else
            isOuyaMode = OUYAInterface.IS_OUYA_HARDWARE;
        
        // Determine the touchscreen layout
        boolean isCustom = false;
        String folder = "";
        if( inputPlugin.enabled && isTouchscreenEnabled )
        {
            String layout = mPreferences.getString( "touchscreenLayout", "" );
            if( layout.equals( "Custom" ) )
            {
                isCustom = true;
                folder = mPreferences.getString( "pathCustomTouchscreen", "" );
            }
            else
            {
                // Use the "No-stick" skin if analog input is shown but stick ("hat") is not animated
                if( layout.equals( "Mupen64Plus-AE-Analog" ) || layout.equals( "Mupen64Plus-AE-All" ) )
                {
                    if( touchscreenRefresh == 0 )
                        layout += "-Nostick";
                    else
                        layout += "-Stick";
                }
                
                String height = mPreferences.getString( "touchscreenHeight", "" );
                if( TextUtils.isEmpty( height ) )
                {
                    // Use the "Tablet" skin if the device is a tablet or is in portrait orientation
                    if( context instanceof Activity )
                    {
                        DisplayMetrics metrics = new DisplayMetrics();
                        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics( metrics );
                        float screenWidthInches = (float) metrics.widthPixels / (float) metrics.xdpi;
                        float screenHeightInches = (float) metrics.heightPixels / (float) metrics.ydpi;
                        float screenSizeInches = (float) Math.sqrt( ( screenWidthInches * screenWidthInches ) + ( screenHeightInches * screenHeightInches ) );
                        if( screenSizeInches >= Utility.MINIMUM_TABLET_SIZE ||
                            videoOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                            videoOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT )
                        {
                            layout += "-Half-Height";
                        }
                        else
                        {
                            layout += "-Full-Height";
                        }
                    }
                }
                else
                {
                    layout += height;
                }
                
                folder = appData.touchscreenLayoutsDir + layout;
            }
        }
        else if( isFpsEnabled )
        {
            folder = appData.touchscreenLayoutsDir
                    + context.getString( R.string.touchscreenLayout_fpsOnly );
        }
        isTouchscreenCustom = isCustom;
        touchscreenLayout = folder;
        
        // Determine the touchscreen style
        folder = "";
        if( inputPlugin.enabled && isTouchscreenEnabled && !isCustom ) {
            folder = mPreferences.getString( "touchscreenStyle", "Mupen64Plus-AE-Outline" );
        }
        touchscreenStyle = folder;
        
        touchscreenScale = ( (float) mPreferences.getInt( "touchscreenScale", 100 ) ) / 100.0f; 
        
        // Determine which players are "plugged in"
        isPlugged1 = isInputEnabled1 || isTouchscreenEnabled || isTouchpadEnabled;
        isPlugged2 = isInputEnabled2;
        isPlugged3 = isInputEnabled3;
        isPlugged4 = isInputEnabled4;
        
        // Determine whether controller deconfliction is needed
        int numControllers = 0;
        numControllers += isInputEnabled1 ? 1 : 0;
        numControllers += isInputEnabled2 ? 1 : 0;
        numControllers += isInputEnabled3 ? 1 : 0;
        numControllers += isInputEnabled4 ? 1 : 0;
        boolean isControllerShared = mPreferences.getBoolean( "inputShareController", false );
        playerMap.setEnabled( numControllers > 1 && !isControllerShared );
        
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
        
        // Determine the pixel dimensions of the rendering context
        boolean isLandscape = videoOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            || videoOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        int width = isLandscape ? appData.maxScreenSize.x : appData.maxScreenSize.y;
        int height = isLandscape ? appData.maxScreenSize.y : appData.maxScreenSize.x;
        if( !isStretched )
        {
            // Maintain aspect ratio (may pillarbox/letterbox)
            float aspect = 0.75f; // TODO: Handle PAL
            float maxWidth = width;
            float maxHeight = height;
            
            if( maxHeight / maxWidth > aspect )
            {
                // Typically, letterbox when in portrait
                width = (int) maxWidth;
                height = (int) ( maxWidth * aspect );
            }
            else
            {
                // Typically, pillarbox when in landscape
                height = (int) maxHeight;
                width = (int) ( maxHeight / aspect );
            }
        }
        videoRenderWidth = width;
        videoRenderHeight = height;
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
    
    public int getPakType( int player )
    {
        return getInt( KEYTEMPLATE_PAK_TYPE, player, DEFAULT_PAK_TYPE );
    }
    
    public String getInputMapString( int player )
    {
        return getString( KEYTEMPLATE_INPUT_MAP_STRING, player, DEFAULT_INPUT_MAP_STRING );
    }
    
    public int getInputDeadzone( int player )
    {
        return getInt( KEYTEMPLATE_INPUT_DEADZONE, player, DEFAULT_INPUT_DEADZONE );
    }

    public boolean getSpecialVisibility( int player )
    {
        return getBoolean( KEYTEMPLATE_SPECIAL_VISIBILITY, player, DEFAULT_SPECIAL_VISIBILITY );
    }
    
    public boolean getPlayerMapReminder()
    {
        return getBoolean( KEY_PLAYER_MAP_REMINDER, DEFAULT_PLAYER_MAP_REMINDER );
    }
    
    public void putPakType( int player, int value )
    {
        putInt( KEYTEMPLATE_PAK_TYPE, player, value );
    }
    
    public void putInputMapString( int player, String value )
    {
        putString( KEYTEMPLATE_INPUT_MAP_STRING, player, value );
    }
    
    public void putInputDeadzone( int player, int value )
    {
        putInt( KEYTEMPLATE_INPUT_DEADZONE, player, value );
    }

    public void putSpecialVisibility( int player, boolean value )
    {
        putBoolean( KEYTEMPLATE_SPECIAL_VISIBILITY, player, value );
    }
    
    public void putPlayerMapReminder( boolean value )
    {
        putBoolean( KEY_PLAYER_MAP_REMINDER, value );
    }
    
    private boolean getBoolean( String key, boolean defaultValue )
    {
        return mPreferences.getBoolean( key, defaultValue );
    }
    
    private boolean getBoolean( String keyTemplate, int index, boolean defaultValue )
    {
        String key = String.format( Locale.US, keyTemplate, index );
        return getBoolean( key, defaultValue );
    }
    
    private int getInt( String keyTemplate, int index, int defaultValue )
    {
        String key = String.format( Locale.US, keyTemplate, index );
        return mPreferences.getInt( key, defaultValue );
    }
    
    private String getString( String keyTemplate, int index, String defaultValue )
    {
        String key = String.format( Locale.US, keyTemplate, index );
        return mPreferences.getString( key, defaultValue );
    }
    
    private void putBoolean( String key, boolean value )
    {
        mPreferences.edit().putBoolean( key, value ).commit();
    }
    
    private void putBoolean( String keyTemplate, int index, boolean value )
    {
        String key = String.format( Locale.US, keyTemplate, index );
        putBoolean( key, value );
    }
    
    private void putInt( String keyTemplate, int index, int value )
    {
        String key = String.format( Locale.US, keyTemplate, index );
        mPreferences.edit().putInt( key, value ).commit();
    }
    
    private void putString( String keyTemplate, int index, String value )
    {
        String key = String.format( Locale.US, keyTemplate, index );
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
    
    /**
     * Gets the selected values of a MultiSelectListPreference, as an integer set.
     *
     * @param preferences The object containing the MultiSelectListPreference.
     * @param key         The key of the MultiSelectListPreference.
     * 
     * @return The values, as an integer set.
     */
    private static Set<Integer> getSafeIntSet( SharedPreferences preferences, String key )
    {
        Set<Integer> mutableSet = new HashSet<Integer>();
        {
            String holdables = preferences.getString( key, "" );
            for( String s : MultiSelectListPreference.deserialize( holdables ) )
            {
                try
                {
                    mutableSet.add( Integer.valueOf( s ) );
                }
                catch( NumberFormatException ignored )
                {
                }
            }
        }
        return Collections.unmodifiableSet( mutableSet );
    }

    /**
     * A tiny class containing inter-dependent plug-in information.
     */
    public static class Plugin
    {
        /** The name of the plug-in, with extension, without parent directory. */
        public final String name;
        
        /** The full absolute path name of the plug-in. */
        public final String path;
        
        /** True if the plug-in is enabled. */
        public final boolean enabled;
        
        /**
         * Instantiates a new plug-in meta-info object.
         * 
         * @param prefs   The shared preferences containing plug-in information.
         * @param libsDir The directory containing the plug-in file.
         * @param key     The shared preference key for the plug-in.
         */
        public Plugin( SharedPreferences prefs, String libsDir, String key )
        {
            name = prefs.getString( key, "" );
            enabled = !TextUtils.isEmpty( name );
            path = enabled ? libsDir + name : "dummy";
        }
    }
}
