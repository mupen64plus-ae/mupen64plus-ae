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
package emulator.android.mupen64plusae.profile;

import emulator.android.mupen64plusae.input.map.InputMap;
import emulator.android.mupen64plusae.persistent.ConfigFile.ConfigSection;

public class ControllerProfile extends Profile
{
    private static final int DEFAULT_DEADZONE = 15;
    private static final boolean DEFAULT_AUTO_DEADZONE = true;
    private static final int DEFAULT_SENSITIVITY = 100;
    
    private static final String KEY_MAP = "map";
    private static final String KEY_AUTO_DEADZONE = "auto_deadzone";
    private static final String KEY_DEADZONE = "deadzone";
    private static final String KEY_SENSITIVITY_X = "sensitivity_x";
    private static final String KEY_SENSITIVITY_Y = "sensitivity_y";
    
    public InputMap getMap()
    {
        return new InputMap( get( KEY_MAP ) );
    }
    
    public int getDeadzone()
    {
        return getInt( KEY_DEADZONE, DEFAULT_DEADZONE );
    }

    public boolean getAutoDeadzone()
    {
        return getBoolean( KEY_AUTO_DEADZONE, DEFAULT_AUTO_DEADZONE );
    }

    public int getSensitivityX()
    {
        return getInt( KEY_SENSITIVITY_X, DEFAULT_SENSITIVITY );
    }

    public int getSensitivityY()
    {
        return getInt( KEY_SENSITIVITY_Y, DEFAULT_SENSITIVITY );
    }
    
    void putMap( InputMap map )
    {
        put( KEY_MAP, map.serialize() );
    }
    
    void putDeadzone( int deadzone )
    {
        putInt( KEY_DEADZONE, deadzone );
    }

    void putAutoDeadzone( boolean autoDeadzone )
    {
        putBoolean( KEY_AUTO_DEADZONE, autoDeadzone );
    }
    
    void putSensitivityX( int sensitivity )
    {
        putInt( KEY_SENSITIVITY_X, sensitivity );
    }

    void putSensitivityY( int sensitivity )
    {
        putInt( KEY_SENSITIVITY_Y, sensitivity );
    }
    
    public ControllerProfile( boolean isBuiltin, String name, String comment )
    {
        super( isBuiltin, name, comment );
    }
    
    public ControllerProfile( boolean isBuiltin, ConfigSection section )
    {
        super( isBuiltin, section );
    }
}