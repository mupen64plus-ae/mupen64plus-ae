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

import java.io.File;
import java.util.Locale;

import paulscode.android.mupen64plusae.util.Utility;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

/**
 * A convenience class for retrieving and persisting data defined internally by the application.
 * <p>
 * Hardware types are used to apply device-specific fixes for missing shadows and decals, and must
 * match the #defines in OpenGL.cpp.
 */
public class AppData
{
    /** True if device is running Eclair or later (4 - Android 2.0.x) */
    public static final boolean IS_ECLAIR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR;
    
    /** True if device is running Gingerbread or later (9 - Android 2.3.x) */
    public static final boolean IS_GINGERBREAD = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    
    /** True if device is running Honeycomb or later (11 - Android 3.0.x) */
    public static final boolean IS_HONEYCOMB = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    
    /** True if device is running Honeycomb MR1 or later (12 - Android 3.1.x) */
    public static final boolean IS_HONEYCOMB_MR1 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    
    /** True if device is running Jellybean or later (16 - Android 4.1.x) */
    public static final boolean IS_JELLYBEAN = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    
    /** Debug option: download data to SD card (default true). */
    public static final boolean DOWNLOAD_TO_SDCARD = true;

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
    
    /** Default value for getHardwareType(). */
    public static final int DEFAULT_HARDWARE_TYPE = HARDWARE_TYPE_UNKNOWN;
    
    /** Default value for isFirstRun(). */
    public static final boolean DEFAULT_FIRST_RUN = false;
    
    /** Default value for isUpgradedVer19(). */
    public static final boolean DEFAULT_IS_UPGRADED_VER19 = false;
    
    /** The data download URL. */
    public static final String DATA_DOWNLOAD_URL = "Data size is 1.0 Mb|mupen64plus_data.zip";
    
   /** The package name. */
    public final String packageName;
    
    /** The user storage directory (typically the external storage directory). */
    public final String storageDir;
    
    /** The directory for storing internal app data. */
    public final String dataDir;
    
    /** The directory containing the native Mupen64Plus libraries. */
    public final String libsDir;
    
    /** The directory containing all touchscreen layout folders. */
    public final String touchscreenLayoutsDir;
    
    /** The directory containing all Xperia Play layout folders. */
    public final String xperiaPlayLayoutsDir;
    
    /** The directory containing all fonts. */
    public final String fontsDir;
    
    /** The directory for backing up user data during (un)installation. */
    public final String dataBackupDir;
    
    /** The directory for backing up game save files. */
    public final String savesBackupDir;
    
    /** The default directory containing game ROM files. */
    public final String defaultRomDir;
    
    /** The default directory containing game save files. */
    public final String defaultSavesDir;
    
    /** The name of the mupen64 core configuration file. */
    public final String mupen64plus_cfg;
    
    /** The name of the gles2n64 configuration file. */
    public final String gles2n64_conf;
    
    /** The name of the error log file. */
    public final String error_log;
    
    /** The hardware info, refreshed at the beginning of every session. */
    public final HardwareInfo hardwareInfo;
    
    /** The object used to persist the settings. */
    private final SharedPreferences mPreferences;
    
    /**
     * Instantiates a new object to retrieve and persist app data.
     * 
     * @param context The application context.
     */
    public AppData( Context context )
    {
        packageName = context.getPackageName();
        
        // Directories
        if( DOWNLOAD_TO_SDCARD )
        {
            storageDir = Environment.getExternalStorageDirectory().getAbsolutePath();
            dataDir = storageDir + "/Android/data/" + packageName;
        }
        else
        {
            storageDir = context.getFilesDir().getAbsolutePath();
            dataDir = storageDir;
        }
        libsDir = "/data/data/" + packageName + "/lib/";
        touchscreenLayoutsDir = dataDir + "/skins/gamepads/";
        xperiaPlayLayoutsDir = dataDir + "/skins/touchpads/";
        fontsDir = dataDir + "/skins/fonts/";
        dataBackupDir = storageDir + "/mp64p_tmp_asdf1234lkjh0987/data/save";
        savesBackupDir = dataBackupDir + "/data/save";
        
        String romFolder = storageDir + "/roms/n64";
        if( ( new File( romFolder ) ).isDirectory() )
            defaultRomDir = romFolder;
        else
            defaultRomDir = storageDir;
        defaultSavesDir = storageDir + "/GameSaves";
        
        // Files
        mupen64plus_cfg = dataDir + "/mupen64plus.cfg";
        gles2n64_conf = dataDir + "/data/gles2n64.conf";
        error_log = dataDir + "/error.log";
        
        // Preference object for persisting app data
        String appDataFilename = packageName + "_preferences_device";
        mPreferences = context.getSharedPreferences( appDataFilename, Context.MODE_PRIVATE );
        hardwareInfo = new HardwareInfo();
        
        Log.v( "Paths - DataDir Check", "PackageName set to '" + packageName + "'" );
        Log.v( "Paths - DataDir Check", "LibsDir set to '" + libsDir + "'" );
        Log.v( "Paths - DataDir Check", "StorageDir set to '" + storageDir + "'" );
        Log.v( "Paths - DataDir Check", "DataDir set to '" + dataDir + "'" );
    }
    
    /**
     * Checks if the storage directory is accessible.
     * 
     * @return True, if the storage directory is accessible.
     */
    public boolean isSdCardAccessible()
    {
        return ( new File( storageDir ) ).exists();
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
     * Small class containing hardware info provided by /proc/cpuinfo.
     */
    public class HardwareInfo
    {
        public final String hardware;
        public final String processor;
        public final String features;
        public final int hardwareType;
        public final boolean isXperiaPlay;
        
        public HardwareInfo()
        {
            // Identify the hardware, features, and processor strings
            {
                // Temporaries since we can't assign the final fields this way
                String _hardware = "";
                String _features = "";
                String _processor = "";   
                
                // Parse a long string of information from the operating system
                String hwString = Utility.getCpuInfo().toLowerCase( Locale.ENGLISH );
                String[] lines = hwString.split( "\\r\\n|\\n|\\r" );
                for( int i = 0; i < lines.length; i++ )
                {
                    String[] splitLine = lines[i].split( ":" );
                    if( splitLine.length == 2 )
                    {
                        String heading = splitLine[0].trim();
                        if( _processor == "" && heading.equals( "processor" ) )
                            _processor = splitLine[1].trim();
                        else if( _features == "" && heading.equals( "features" ) )
                            _features = splitLine[1].trim();
                        else if( _hardware == "" && heading.equals( "hardware" ) )
                            _hardware = splitLine[1].trim();
                    }
                }
                
                // Assign the final fields
                hardware = _hardware;
                processor = _processor;
                features = _features;
            }            
            
            // Identify the hardware type from the substrings
            //@formatter:off
            if(        hardware.contains( "mapphone" )
                    || hardware.contains( "tuna" )
                    || hardware.contains( "smdkv" )
                    || hardware.contains( "herring" )
                    || hardware.contains( "aries" ) )
                hardwareType = HARDWARE_TYPE_OMAP;
            
            else if(   hardware.contains( "liberty" )
                    || hardware.contains( "gt-s5830" )
                    || hardware.contains( "zeus" ) )
                hardwareType = HARDWARE_TYPE_QUALCOMM;
            
            else if(   hardware.contains( "imap" ) )
                hardwareType = HARDWARE_TYPE_IMAP;
            
            else if(   hardware.contains( "tegra 2" )
                    || hardware.contains( "grouper" )
                    || hardware.contains( "meson-m1" )
                    || hardware.contains( "smdkc" )
                    || hardware.contains( "smdk4x12" )
                    || ( features != null && features.contains( "vfpv3d16" ) ) )
                hardwareType = HARDWARE_TYPE_TEGRA;
            else
                hardwareType = DEFAULT_HARDWARE_TYPE;
            //@formatter:on
            
            // Identify whether this is an Xperia PLAY
            isXperiaPlay = hardware.contains( "zeus" );           
        }
    }
}