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

import paulscode.android.mupen64plusae.Globals;
import android.app.Activity;
import android.os.Environment;
import android.util.Log;

/**
 * A convenience class for retrieving path locations and performing related checks.
 */
public class Paths
{
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
    
    /** The name of the touchscreen layouts file. */
    public final String touchscreenLayouts_ini;
    
    /** The name of the Xperia Play touchpad layouts file. */
    public final String xperiaPlayLayouts_ini;
    
    /** The name of the application settings file. */
    public final String appSettingsFilename;
    
    public static String tmpFile;
    
    // TODO: Should we move this to strings.xml?
    /** The data download URL. */
    public static String dataDownloadUrl = "Data size is 1.0 Mb|mupen64plus_data.zip";
    
    /**
     * Instantiates a new Path object to retrieve path locations and perform related checks.
     * 
     * @param activity the main activity
     */
    public Paths( Activity activity )
    {
        packageName = activity.getPackageName();
        
        // Directories
        storageDir = Globals.DOWNLOAD_TO_SDCARD
                ? Environment.getExternalStorageDirectory().getAbsolutePath()
                : activity.getFilesDir().getAbsolutePath();
        dataDir = storageDir + ( Globals.DOWNLOAD_TO_SDCARD
                ? "/Android/data/" + packageName
                : "" );
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
        defaultSavesDir = dataDir + "/data/save";
        
        // Files
        mupen64plus_cfg = dataDir + "/mupen64plus.cfg";
        gles2n64_conf = dataDir + "/data/gles2n64.conf";
        error_log = dataDir + "/error.log";
        touchscreenLayouts_ini = touchscreenLayoutsDir + "gamepad_list.ini";
        xperiaPlayLayouts_ini = xperiaPlayLayoutsDir + "touchpad_list.ini";
        
        // Preference file names
        appSettingsFilename = packageName + "_preferences_device";
        
        Log.v( "DataDir Check", "PackageName set to '" + packageName + "'" );
        Log.v( "DataDir Check", "LibsDir set to '" + libsDir + "'" );
        Log.v( "DataDir Check", "StorageDir set to '" + storageDir + "'" );
        Log.v( "DataDir Check", "DataDir set to '" + dataDir + "'" );
    }
    
    /**
     * Checks if the storage directory is accessible.
     * 
     * @return true, if the storage directory is accessible
     */
    public boolean isSdCardAccessible()
    {
        return ( new File( storageDir ) ).exists();
    }
}