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

import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.util.SafeMethods;
import android.text.TextUtils;

public class ControllerProfile extends Profile
{
    public static final int DEFAULT_DEADZONE = 0;
    public static final int DEFAULT_SENSITIVITY = 100;
    
    public final InputMap map;
    public final int deadzone;
    public final int sensitivity;
    
    private static final String KEY_COMMENT = "comment";
    private static final String KEY_MAP = "map";
    private static final String KEY_DEADZONE = "deadzone";
    private static final String KEY_SENSITIVITY = "sensitivity";
    
    public ControllerProfile( String name, String comment, boolean isBuiltin, InputMap map,
            int deadzone, int sensitivity )
    {
        super( name, comment, isBuiltin );
        this.map = map;
        this.deadzone = deadzone;
        this.sensitivity = sensitivity;
    }
    
    public ControllerProfile( String name, String comment, boolean isBuiltin )
    {
        this( name, comment, isBuiltin, new InputMap(), DEFAULT_DEADZONE, DEFAULT_SENSITIVITY );
    }
    
    public static ControllerProfile read( ConfigFile config, String name, boolean isBuiltin )
    {
        if( config == null || TextUtils.isEmpty( name ) )
            return null;
        
        String comment = config.get( name, KEY_COMMENT );
        InputMap map = new InputMap( config.get( name, KEY_MAP ) );
        int deadzone = SafeMethods.toInt( config.get( name, KEY_DEADZONE ), DEFAULT_DEADZONE );
        int sensitivity = SafeMethods.toInt( config.get( name, KEY_SENSITIVITY ), DEFAULT_SENSITIVITY );
        
        return new ControllerProfile( name, comment, isBuiltin, map, deadzone, sensitivity );
    }
    
    public static boolean write( ConfigFile config, ControllerProfile profile )
    {
        if( config == null || profile == null )
            return false;
        
        config.put( profile.name, KEY_COMMENT, profile.comment );
        config.put( profile.name, KEY_MAP, profile.map.serialize() );
        config.put( profile.name, KEY_DEADZONE, String.valueOf( profile.deadzone ) );
        config.put( profile.name, KEY_SENSITIVITY, String.valueOf( profile.sensitivity ) );
        return true;
    }

    public static List<ControllerProfile> getProfiles( ConfigFile config, boolean isBuiltin )
    {
        ArrayList<ControllerProfile> profiles = new ArrayList<ControllerProfile>();
        for( String key : config.keySet() )
            if( !ConfigFile.SECTIONLESS_NAME.equals( key ) )
                profiles.add( read( config, key, isBuiltin ) );
        return profiles;
    }
}
