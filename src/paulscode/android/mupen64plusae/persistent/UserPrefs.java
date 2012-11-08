/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.persistent;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.input.map.InputMap;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * A convenience class for quickly, safely, and consistently retrieving typed user preferences.
 * <p>
 * An instance of this class should be re-constructed every time a preference value changes, from a
 * android.content.SharedPreferences.OnSharedPreferenceChangeListener. A good place to implement the
 * listener is in your PreferenceActivity subclass.
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
 * If you didn't use this class, you would need to search through the entire codebase for every call
 * to getString( "myOldKey", ... ) and update each one. This class also ensures that the same
 * fallback value will be used everywhere. A third advantage is that you can easily provide
 * frequently-used "derived" preferences, as in
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
 * <p>
 * TODO: Seriously? ADK can't auto-generate a class like this?
 */
public class UserPrefs
{
    /** True if the touchscreen is enabled. */
    public final boolean isTouchscreenEnabled;
    
    /** True if a custom touchscreen is provided. */
    public final boolean isTouchscreenCustom;
    
    /** The filename of the selected touchscreen layout. */
    public final String touchscreenLayoutFolder;
    
    /** True if the touchscreen joystick is represented as an octagon. */
    public final boolean isOctagonalJoystick;
    
    /** True if external gamepads/joysticks are enabled. */
    public final boolean isInputEnabled;
    
    /** The filename of the selected input plug-in. */
    public final String inputPlugin;
    
    /** The button map for player 1. */
    public final InputMap inputMap1;
    
    /** The button map for player 2. */
    public final InputMap inputMap2;
    
    /** The button map for player 3. */
    public final InputMap inputMap3;
    
    /** The button map for player 4. */
    public final InputMap inputMap4;
    
    /** True if volume keys can be used as controls. */
    public final boolean isVolKeysEnabled;
    
    /** True if video is enabled. */
    public final boolean isVideoEnabled;
    
    /** The filename of the selected video plug-in. */
    public final String videoPlugin;
    
    /** True if the video should be stretched. */
    public final boolean isStretched;
    
    /** True if RGBA8888 mode should be used for video. */
    public final boolean isRgba8888;
    
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
    
    /** True if auto-frameskip is enabled in the gles2rice library. */
    public final boolean isGles2RiceAutoFrameskipEnabled;
    
    /** True if fast texture CRC is enabled in the gles2rice library. */
    public final boolean isGles2RiceFastTextureCrcEnabled;
    
    /** True if fast texture loading is enabled in the gles2rice library. */
    public final boolean isGles2RiceFastTextureLoadingEnabled;
    
    /** True if hi-resolution textures are enabled in the gles2rice library. */
    public final boolean isGles2RiceHiResTexturesEnabled;
    
    /** True if Xperia Play-specific features are enabled. */
    public final boolean isXperiaEnabled;
    
    /** The filename of the selected Xperia Play layout. */
    public final String xperiaLayout;
    
    /** True if audio is enabled. */
    public final boolean isAudioEnabled;
    
    /** The filename of the selected audio plug-in. */
    public final String audioPlugin;
    
    /** True if the Reality Signal Processor is enabled. */
    public final boolean isRspEnabled;
    
    /** The filename of the selected Reality Signal Processor. */
    public final String rspPlugin;
    
    /** The filename of the selected emulator core. */
    public final String corePlugin;
    
    /** The directory containing game save files. */
    public final String gameSaveDir;
    
    /** True if game state should be saved before the game exits. */
    public final boolean isAutoSaveEnabled;
    
    /** True if the frame rate is displayed. */
    public final boolean isFrameRateEnabled;
    
    /** The filename of the ROM selected by the user. */
    public final String selectedGame;
    
    /** The filename of the auto-saved session of the ROM selected by the user. */
    public final String selectedGameAutoSavefile;
    
    /** The object used to retrieve the settings. */
    private final SharedPreferences mPreferences;
    
    /**
     * Instantiates a new UserPrefs object to retrieve user preferences.
     * 
     * @param context the context of the app
     * @param paths the app's path definitions
     */
    public UserPrefs( Context context, Paths paths )
    {
        mPreferences = PreferenceManager.getDefaultSharedPreferences( context );
        
        // Touchscreen prefs
        isTouchscreenEnabled = mPreferences.getBoolean( "touchscreenEnabled", true );
        isOctagonalJoystick = mPreferences.getBoolean( "touchscreenOctagonJoystick", true );
        
        // Peripherals prefs
        isInputEnabled = mPreferences.getBoolean( "inputEnabled", true );
        inputMap1 = new InputMap( mPreferences.getString( "inputMap1", "" ) );
        inputMap2 = new InputMap( mPreferences.getString( "inputMap2", "" ) );
        inputMap3 = new InputMap( mPreferences.getString( "inputMap3", "" ) );
        inputMap4 = new InputMap( mPreferences.getString( "inputMap4", "" ) );
        isVolKeysEnabled = mPreferences.getBoolean( "volumeKeysEnabled", false );
        
        // Video prefs
        isVideoEnabled = mPreferences.getBoolean( "videoEnabled", true );
        isStretched = mPreferences.getBoolean( "videoStretch", false );
        isRgba8888 = mPreferences.getBoolean( "videoRGBA8888", false );
        
        // Video prefs - gles2n64
        gles2N64MaxFrameskip = getSafeInt( mPreferences, "gles2N64Frameskip", -1 );
        isGles2N64FogEnabled = mPreferences.getBoolean( "gles2N64Fog", false );
        isGles2N64SaiEnabled = mPreferences.getBoolean( "gles2N64Sai", false );
        isGles2N64ScreenClearEnabled = mPreferences.getBoolean( "gles2N64ScreenClear", true );
        isGles2N64AlphaTestEnabled = mPreferences.getBoolean( "gles2N64AlphaTest", true );
        isGles2N64DepthTestEnabled = mPreferences.getBoolean( "gles2N64DepthTest", true );
        
        // Video prefs - gles2rice
        isGles2RiceAutoFrameskipEnabled = mPreferences.getBoolean( "gles2RiceAutoFrameskip", false );
        isGles2RiceFastTextureCrcEnabled = mPreferences
                .getBoolean( "gles2RiceFastTextureCRC", true );
        isGles2RiceFastTextureLoadingEnabled = mPreferences.getBoolean( "gles2RiceFastTexture",
                false );
        isGles2RiceHiResTexturesEnabled = mPreferences.getBoolean( "gles2RiceHiResTextures", true );
        
        // Other prefs
        selectedGame = mPreferences.getString( "selectedGame", "" );
        gameSaveDir = mPreferences.getString( "gameSaveDir", paths.defaultSavesDir );
        isAutoSaveEnabled = mPreferences.getBoolean( "autoSaveEnabled", false );
        isFrameRateEnabled = mPreferences.getBoolean( "frameRateEnabled", false );
        
        // Plug-ins and layouts
        inputPlugin = paths.libsDir + ( isInputEnabled
                ? mPreferences.getString( "inputPlugin", "" )
                : "" );
        videoPlugin = paths.libsDir + ( isVideoEnabled
                ? mPreferences.getString( "videoPlugin", "" )
                : "" );
        audioPlugin = paths.libsDir + mPreferences.getString( "audioPlugin", "" );
        rspPlugin = paths.libsDir + mPreferences.getString( "rspPlugin", "" );
        corePlugin = paths.libsDir + mPreferences.getString( "corePlugin", "" );
        xperiaLayout = mPreferences.getString( "xperiaPlugin", "" );
        
        boolean isCustom = false;
        String folder = "";
        if( isTouchscreenEnabled )
        {
            String layout = mPreferences.getString( "touchscreenLayout", "" );
            if( layout.equals( "Custom" ) )
            {
                isCustom = true;
                folder = mPreferences.getString( "touchscreenCustom", "" );
            }
            else
            {
                folder = paths.touchscreenLayoutsDir + layout
                        + mPreferences.getString( "touchscreenSize", "" );
            }
        }
        else if( isFrameRateEnabled )
        {
            folder = paths.touchscreenLayoutsDir
                    + context.getString( R.string.touchscreenLayout_fpsOnly );
        }
        isTouchscreenCustom = isCustom;
        touchscreenLayoutFolder = folder;
        
        // Derived values
        isGles2N64AutoFrameskipEnabled = gles2N64MaxFrameskip < 0;
        isAudioEnabled = audioPlugin != null && !audioPlugin.equals( "" );
        isXperiaEnabled = xperiaLayout != null && !xperiaLayout.equals( "" );
        isRspEnabled = rspPlugin != null && rspPlugin.equals( "" );
        selectedGameAutoSavefile = paths.dataDir + "/autosave_" + Math.abs( selectedGame.hashCode() ) + ".sav";
    }
    
    /**
     * Gets the selected value of a ListPreference, as an integer.
     * 
     * @param preferences the object containing the ListPreference
     * @param key the key of the ListPreference
     * @param defaultValue the value to use if parsing fails
     * @return the value of the selected entry, as an integer
     */
    private static int getSafeInt( SharedPreferences preferences, String key, int defaultValue )
    {
        try
        {
            return Integer.parseInt( preferences.getString( key, String.valueOf( defaultValue ) ),
                    defaultValue );
        }
        catch( NumberFormatException ex )
        {
            return defaultValue;
        }
    }
}