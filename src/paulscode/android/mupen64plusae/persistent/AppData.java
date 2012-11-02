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

import paulscode.android.mupen64plusae.util.Utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * A convenience class for retrieving and persisting data defined internally by the application.
 * <p>
 * Hardware types are used to apply device-specific fixes for missing shadows and decals, and must
 * match the #defines in OpenGL.cpp.
 */
public class AppData
{
    /** Unknown hardware configuration. */
    public static final int HARDWARE_TYPE_UNKNOWN = 0;
    
    /** OMAP-based hardware. */
    public static final int HARDWARE_TYPE_OMAP = 1;
    
    /** QualComm-based hardware. */
    public static final int HARDWARE_TYPE_QUALCOMM = 2;
    
    /** IMAP-based hardware. */
    public static final int HARDWARE_TYPE_IMAP = 3;
    
    /** Tegra-based hardware. */
    public static final int HARDWARE_TYPE_TEGRA = 4;
    
    /** Unidentified hardware configuration (only used internally). */
    private static final int HARDWARE_TYPE_UNIDENTIFIED = -1;
    
    /** Default value for getHardwareType(). */
    public static final int DEFAULT_HARDWARE_TYPE = HARDWARE_TYPE_UNKNOWN;
    
    /** Default value for isFirstRun(). */
    public static final boolean DEFAULT_FIRST_RUN = false;
    
    /** Default value for isUpgradedVer19(). */
    public static final boolean DEFAULT_IS_UPGRADED_VER19 = false;
    
    /** The object used to persist the settings. */
    private final SharedPreferences mPreferences;
    
    /** The hardware type, refreshed at the beginning of every session. */
    private int mHardwareType = HARDWARE_TYPE_UNIDENTIFIED;
    
    /**
     * Instantiates a new AppData object to retrieve and persist app data.
     * 
     * @param context the context of the app data
     * @param filename the filename where the app data is persisted
     */
    public AppData( Context context, String filename )
    {
        mPreferences = context.getSharedPreferences( filename, Context.MODE_PRIVATE );
    }
    
    /**
     * Reset all app settings to their defaults.
     */
    public void resetToDefaults()
    {
        mPreferences.edit().clear().commit();
    }
    
    /**
     * Checks if this is the first time the app has been run.
     * 
     * @return true, if the app has never been run
     */
    public boolean isFirstRun()
    {
        return mPreferences.getBoolean( "firstRun", DEFAULT_FIRST_RUN );
    }
    
    /**
     * Sets the flag indicating whether the app has run at least once.
     * 
     * @param value true, to indicate the app has never been run
     */
    public void setFirstRun( boolean value )
    {
        mPreferences.edit().putBoolean( "firstRun", value ).commit();
    }
    
    /**
     * Checks if the app has been upgraded to Version 1.9.
     * 
     * @return true, if the app has been upgraded
     */
    public boolean isUpgradedVer19()
    {
        return mPreferences.getBoolean( "upgradedVer19", DEFAULT_IS_UPGRADED_VER19 );
    }
    
    /**
     * Sets the flag indicating whether the app has been upgraded to Version 1.9
     * 
     * @param value true, to indicate the app has been upgraded
     */
    public void setUpgradedVer19( boolean value )
    {
        mPreferences.edit().putBoolean( "upgradedVer19", value ).commit();
    }
    
    /**
     * Gets the hardware type.
     * 
     * @return the hardware type
     */
    public int getHardwareType()
    {
        if( mHardwareType == HARDWARE_TYPE_UNIDENTIFIED )
            mHardwareType = identifyHardwareType();
        return mHardwareType;
    }
    
    /**
     * Identifies the hardware type using information provided by /proc/cpuinfo.
     */
    private static int identifyHardwareType()
    {
        // Parse a long string of information from the operating system
        Log.v( "AppData", "CPU info available from file /proc/cpuinfo:" );
        String hardware = null;
        String features = null;
        String processor = null;
        
        String hwString = Utility.getCpuInfo().toLowerCase();
        Log.v( "AppData", hwString );

        String[] lines = hwString.split( "\\r\\n|\\n|\\r" );
        if( lines != null )
        {
            for( int i = 0; i < lines.length; i++ )
            {
                String[] splitLine = lines[i].split( ":" );
                if( splitLine != null && splitLine.length == 2 )
                {
                    if( processor == null && splitLine[0].trim().equals( "processor" ) )
                        processor = splitLine[1].trim();
                    else if( features == null && splitLine[0].trim().equals( "features" ) )
                        features = splitLine[1].trim();
                    else if( hardware == null && splitLine[0].trim().equals( "hardware" ) )
                        hardware = splitLine[1].trim();
                }
            }
        }
        
        // Identify the hardware type from the substrings
        int type = DEFAULT_HARDWARE_TYPE;
        if( hardware != null )
        {
            if( hardware.contains( "mapphone" ) || hardware.contains( "tuna" )
                    || hardware.contains( "smdkv" ) || hardware.contains( "herring" )
                    || hardware.contains( "aries" ) )
                type = HARDWARE_TYPE_OMAP;
            
            else if( hardware.contains( "liberty" ) || hardware.contains( "gt-s5830" )
                    || hardware.contains( "zeus" ) )
                type = HARDWARE_TYPE_QUALCOMM;
            
            else if( hardware.contains( "imap" ) )
                type = HARDWARE_TYPE_IMAP;
            
            else if( hardware.contains( "tegra 2" ) || hardware.contains( "grouper" )
                    || hardware.contains( "meson-m1" ) || hardware.contains( "smdkc" )
                    || ( features != null && features.contains( "vfpv3d16" ) ) )
                type = HARDWARE_TYPE_TEGRA;
        }
        
        return type;
    }
}