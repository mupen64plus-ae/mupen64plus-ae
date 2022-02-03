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
package emulator.android.mupen64plusae.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import emulator.android.mupen64plusae.persistent.ConfigFile;
import emulator.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import emulator.android.mupen64plusae.util.SafeMethods;
import android.text.TextUtils;

/**
 * The base class for configuration profiles. Extend this class to encapsulate groups of settings.
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "RedundantSuppression"})
public class Profile implements Comparable<Profile>
{
    /** The name of the profile, displayed in the UI and used as a unique identifier. */
    public String name;
    
    /** An optional brief description of the profile. Shown in some locations in the UI. */
    public String comment;
    
    /**
     * Whether this profile is "built-in" to the app (vs. user defined). Built-in profiles are
     * read-only and can only be copied. Non-built-in profiles can be copied, renamed, edited,
     * created, and deleted. Built-in profiles are defined in the assets directory, and for all
     * intents and purposes are guaranteed to exist. Defaults should always reference built-in
     * profiles.
     */
    final boolean isBuiltin;
    
    private static final String KEY_COMMENT = "comment";
    
    private final HashMap<String, String> data = new HashMap<>();
    
    public static List<Profile> getProfiles( ConfigFile config, boolean isBuiltin )
    {
        List<Profile> profiles = new ArrayList<>();
        for( String name : config.keySet() )
            if( !ConfigFile.SECTIONLESS_NAME.equals( name ) )
                profiles.add( new Profile( isBuiltin, config.get( name ) ) );
        return profiles;
    }
    
    /**
     * Instantiates an empty profile.
     * 
     * @param isBuiltin true if the profile is built-in; false if the profile is user-defined
     * @param name the unique name of the profile
     * @param comment an optional brief description of the profile, shown in some of the UI
     */
    public Profile( boolean isBuiltin, String name, String comment )
    {
        this.isBuiltin = isBuiltin;
        this.name = name;
        this.comment = comment;
        data.put( KEY_COMMENT, comment );
    }
    
    /**
     * Instantiates a profile from a {@link ConfigSection}.
     * 
     * @param isBuiltin true if the profile is built-in; false if the profile is user-defined
     * @param section a back-end datastore for the profile
     */
    public Profile( boolean isBuiltin, ConfigSection section )
    {
        this.isBuiltin = isBuiltin;
        this.name = section.name;
        this.comment = section.get( KEY_COMMENT );
        for( String key : section.keySet() )
            this.data.put( key, section.get( key ) );
    }
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public String getComment()
    {
        return comment;
    }
    
    public void setComment(String comment)
    {
        this.comment = comment;
    }
    
    /**
     * Gets the value mapped to the specified key.
     * 
     * @param key the data key
     * @return the value mapped to the key, or null if no mapping for the key exists
     */
    public String get( String key )
    {
        return data.get( key );
    }
    
    /**
     * Gets the value mapped to the specified key.
     * 
     * @param key the data key
     * @param defaultValue the value to use if the key is not mapped
     * @return the value mapped to the key, or <code>defaultValue</code> if no mapping exists
     */
    public String get( String key, String defaultValue )
    {
        String value = data.get( key );
        return value == null ? defaultValue : value;
    }
    
    /**
     * @see #get(String, String)
     */
    public boolean getBoolean( String key, boolean defaultValue )
    {
        String value = data.get( key );
        return SafeMethods.toBoolean( value, defaultValue );
    }
    
    /**
     * @see #get(String, String)
     */
    public int getInt( String key, int defaultValue )
    {
        String value = data.get( key );
        return SafeMethods.toInt( value, defaultValue );
    }
    
    /**
     * @see #get(String, String)
     */
    public float getFloat( String key, int defaultValue )
    {
        String value = data.get( key );
        return SafeMethods.toFloat( value, defaultValue );
    }
    
    /**
     * Maps the specified value to the specified key.
     * 
     * @param key the data key
     * @param value the value to be mapped to the key
     */
    public void put( String key, String value )
    {
        data.put( key, value );
    }
    
    /**
     * @see #put(String, String)
     */
    public void putBoolean( String key, boolean value )
    {
        put( key, String.valueOf( value ) );
    }
    
    /**
     * @see #put(String, String)
     */
    public void putInt( String key, int value )
    {
        put( key, String.valueOf( value ) );
    }
    
    /**
     * @see #put(String, String)
     */
    public void putFloat( String key, float value )
    {
        put( key, String.valueOf( value ) );
    }
    
    /**
     * Reads key-value pairs from a given config file into the profile. Key-value pairs that already
     * exist in the profile are overwritten.
     * 
     * @param config the {@link ConfigFile} to read from
     * @return true if the config file is non-null and the profile name is non-empty
     */
    public boolean readFrom( ConfigFile config )
    {
        if( config == null || TextUtils.isEmpty( name ) )
            return false;
        
        ConfigSection source = config.get( name );
        if( source == null )
            return false;
        
        for( String key : source.keySet() )
            data.put( key, source.get( key ) );
        return true;
    }
    
    /**
     * Writes key-value pairs from the profile into a given config file. Key-value pairs that
     * already exist in the config file are overwritten.
     * 
     * @param config the {@link ConfigFile} to write to
     * @return true if the config file is non-null and the profile name is non-empty
     */
    boolean writeTo( ConfigFile config )
    {
        if( config == null || TextUtils.isEmpty( name ) )
            return false;
        
        for( String key : data.keySet() )
            config.put( name, key, data.get( key ) );
        return true;
    }
    
    /**
     * Copies a profile, changing only its name and comment.
     * 
     * @param name the name of the copy
     * @param comment the comment of the copy
     * @return the copied profile
     */
    public Profile copy( String name, String comment )
    {
        if( TextUtils.isEmpty( name ) )
            return null;
        
        Profile newProfile = new Profile( false, name, comment );
        for( String key : data.keySet() )
            if( !KEY_COMMENT.equals( key ) )
                newProfile.data.put( key, data.get( key ) );
        return newProfile;
    }
    
    @Override
    public int compareTo( Profile another )
    {
        return this.name.compareToIgnoreCase( another.name );
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Profile)) {
            return false;
        }

        return this.name.compareToIgnoreCase( ((Profile)obj).name ) == 0;
    }
}