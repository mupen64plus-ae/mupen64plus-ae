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
package paulscode.android.mupen64plusae.profile;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.input.TouchController;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.PrefUtil;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;

public class TouchscreenProfileActivity extends ProfileActivity
{
    // These constants must match the keys found in preferences_emulation.xml
    private static final String TOUCHSCREEN_REFRESH = "touchscreenRefresh";
    private static final String TOUCHSCREEN_AUTO_HOLD = "touchscreenAutoHold";
    private static final String TOUCHSCREEN_AUTO_HOLDABLES = "touchscreenAutoHoldables";
    private static final String TOUCHSCREEN_STYLE = "touchscreenStyle";
    private static final String TOUCHSCREEN_HEIGHT = "touchscreenHeight";
    private static final String TOUCHSCREEN_LAYOUT = "touchscreenLayout";
    private static final String PATH_CUSTOM_TOUCHSCREEN = "pathCustomTouchscreen";
    
    @Override
    protected int getPrefsResId()
    {
        return R.xml.profile_touchscreen;
    }
    
    @Override
    protected String getConfigFilePath()
    {
        return new UserPrefs( this ).touchscreenProfiles_cfg;
    }
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Ensure that the selected values in the list preferences are valid
        Resources res = getResources();
        PrefUtil.validateListPreference( res, mPrefs, TOUCHSCREEN_REFRESH,
                R.string.touchscreenRefresh_default, R.array.touchscreenRefresh_values );
        PrefUtil.validateListPreference( res, mPrefs, TOUCHSCREEN_AUTO_HOLD,
                R.string.touchscreenAutoHold_default, R.array.touchscreenAutoHold_values );
        PrefUtil.validateListPreference( res, mPrefs, TOUCHSCREEN_STYLE,
                R.string.touchscreenStyle_default, R.array.touchscreenStyle_values );
        PrefUtil.validateListPreference( res, mPrefs, TOUCHSCREEN_HEIGHT,
                R.string.touchscreenHeight_default, R.array.touchscreenHeight_values );
        PrefUtil.validateListPreference( res, mPrefs, TOUCHSCREEN_LAYOUT,
                R.string.touchscreenLayout_default, R.array.touchscreenLayout_values );
    }
    
    @Override
    protected void refreshViews()
    {
        // Get the current values
        int touchscreenAutoHold = getSafeInt( mPrefs, TOUCHSCREEN_AUTO_HOLD, 0 );
        boolean isTouchscreenCustom = mPrefs.getString( TOUCHSCREEN_LAYOUT, "" ).equals( "Custom" );
        
        // Enable the auto-holdables pref if auto-hold is not disabled
        PrefUtil.enablePreference( this, TOUCHSCREEN_AUTO_HOLDABLES,
                touchscreenAutoHold != TouchController.AUTOHOLD_METHOD_DISABLED );
        
        // Enable the custom touchscreen prefs under certain conditions
        PrefUtil.enablePreference( this, TOUCHSCREEN_STYLE, !isTouchscreenCustom );
        PrefUtil.enablePreference( this, TOUCHSCREEN_HEIGHT, !isTouchscreenCustom );
        PrefUtil.enablePreference( this, PATH_CUSTOM_TOUCHSCREEN, isTouchscreenCustom );
    }
    
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
