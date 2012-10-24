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

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * A convenience class for retrieving and persisting data defined internally by the application.
 * 
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
     * @param value
     *            true, to indicate the app has never been run
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
     * @param value
     *            true, to indicate the app has been upgraded
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
        if( mPreferences.getInt( "hardwareType", HARDWARE_TYPE_UNIDENTIFIED ) == HARDWARE_TYPE_UNIDENTIFIED )
            identifyHardwareType();
        return mPreferences.getInt( "hardwareType", DEFAULT_HARDWARE_TYPE );
    }
    
    /**
     * Identifies the hardware type using information provided by /proc/cpuinfo.
     */
    private void identifyHardwareType()
    {
        // Parse a long string of information from the operating system
        Log.v( "Application", "CPU info available from file /proc/cpuinfo:" );
        ProcessBuilder cmd;
        String hardware = null;
        String features = null;
        try
        {
            String[] args = { "/system/bin/cat", "/proc/cpuinfo" };
            cmd = new ProcessBuilder( args );
            java.lang.Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[1024];
            String line;
            String[] lines;
            String[] splitLine;
            String processor = null;
            
            if( in.read( re ) != -1 )
            {
                line = new String( re );
                Log.v( "Application", line );
                lines = line.split( "\\r\\n|\\n|\\r" );
                if( lines != null )
                {
                    for( int x = 0; x < lines.length; x++ )
                    {
                        splitLine = lines[x].split( ":" );
                        if( splitLine != null && splitLine.length == 2 )
                        {
                            if( processor == null
                                    && splitLine[0].trim().toLowerCase().equals( "processor" ) )
                                processor = splitLine[1].trim().toLowerCase();
                            else if( features == null
                                    && splitLine[0].trim().toLowerCase().equals( "features" ) )
                                features = splitLine[1].trim().toLowerCase();
                            else if( hardware == null
                                    && splitLine[0].trim().toLowerCase().equals( "hardware" ) )
                                hardware = splitLine[1].trim().toLowerCase();
                        }
                    }
                }
            }
            in.close();
        }
        catch( IOException ioe )
        {
            ioe.printStackTrace();
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
        
        // Persist the value to storage so that we don't have to do this every time
        mPreferences.edit().putInt( "hardwareType", type ).commit();
    }
}