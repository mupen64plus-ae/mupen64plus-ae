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

/**
 * The base class for configuration profiles. Extend this class to encapsulate groups of settings.
 */
public class Profile implements Comparable<Profile>
{
    /** The name of the profile, displayed in the UI and used as a unique identifier. */
    public final String name;
    
    /** An optional brief description of the profile. Shown in some locations in the UI. */
    public final String comment;
    
    /**
     * Whether this profile is "built-in" to the app (vs. user defined). Built-in profiles are
     * read-only and can only be copied. Non-built-in profiles can be copied, renamed, edited,
     * created, and deleted. Built-in profiles are defined in the assets directory, and for all
     * intents and purposes are guaranteed to exist. Defaults should always reference built-in
     * profiles.
     */
    public final boolean isBuiltin;
    
    /**
     * Instantiates a new profile.
     * 
     * @param name the unique name of the profile
     * @param comment an optional brief description of the profile, shown in some of the UI
     * @param isBuiltin true if the profile is built-in; false if the profile is user-defined
     */
    public Profile( String name, String comment, boolean isBuiltin )
    {
        this.name = name;
        this.comment = comment;
        this.isBuiltin = isBuiltin;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( Profile another )
    {
        // User-defined profiles are always ahead of (higher precedence) built-ins. Otherwise
        // profiles are ordered alphabetically by name.
        if( this.isBuiltin == another.isBuiltin )
        {
            return this.name.compareToIgnoreCase( another.name );
        }
        else
        {
            return this.isBuiltin ? 1 : -1;
        }
    }
}