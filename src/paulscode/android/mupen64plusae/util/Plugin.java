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
package paulscode.android.mupen64plusae.util;

import paulscode.android.mupen64plusae.profile.Profile;
import android.content.SharedPreferences;

/**
 * A tiny class containing inter-dependent plug-in information.
 */
public class Plugin
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
     * @param prefs The shared preferences containing plug-in information.
     * @param libsDir The directory containing the plug-in file.
     * @param key The shared preference key for the plug-in.
     */
    public Plugin( SharedPreferences prefs, String libsDir, String key )
    {
        name = prefs.getString( key, "" );
        enabled = name.endsWith( ".so" );
        path = enabled ? libsDir + name : "dummy";
    }
    
    /**
     * Instantiates a new plug-in meta-info object.
     * 
     * @param profile The shared preferences containing plug-in information.
     * @param libsDir The directory containing the plug-in file.
     * @param key The shared preference key for the plug-in.
     */
    public Plugin( Profile profile, String libsDir, String key )
    {
        name = profile.get( key, "" );
        enabled = name.endsWith( ".so" );
        path = enabled ? libsDir + name : "dummy";
    }
}