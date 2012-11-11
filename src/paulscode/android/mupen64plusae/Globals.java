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
package paulscode.android.mupen64plusae;

import android.os.Build;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.Paths;
import paulscode.android.mupen64plusae.persistent.UserPrefs;

// TODO: Auto-generated Javadoc
/**
 * The Class Globals.
 */
public class Globals
{
    /** Read-only accessor for user preferences. */
    public static UserPrefs userPrefs;
    
    /** Persistent application state and data. */
    public static AppData appData;
    
    /** Read-only accessor for file system paths. */
    public static Paths paths;
    
    /** True if device is running Eclair or later (Android 2.0.x) */
    public static final boolean IS_ECLAIR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR;
    
    /** True if device is running Gingerbread or later (Android 2.3.x) */    
    public static final boolean IS_GINGERBREAD = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    
    /** True if device is running Honeycomb or later (Android 3.0.x)*/
    public static final boolean IS_HONEYCOMB = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    
    /** True if device is running Honeycomb MR1 or later (Android 3.1.x)*/
    public static final boolean IS_HONEYCOMB_MR1 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    
    /** True if device is running Jellybean or later (Android 4.1.x)*/
    public static final boolean IS_JELLYBEAN = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    
    /** Debug option: inhibit suspend (sleep) when game is running (default true). */
    public static final boolean INHIBIT_SUSPEND = true;
    
    /** Debug option: download data to SD card (default true). */
    public static final boolean DOWNLOAD_TO_SDCARD = true;
}

/**
 * Bugs and feature requests not listed elsewhere, in order of priority.
 * 
 * TODO: *Fix bug when loading zipped ROMS
 * TODO: *Implement multi-player peripheral controls
 * TODO: *Implement special func mapping
 * TODO: *Figure out force-close when loading last session autosavefile
 * TODO: *Figure out failure to return to main menu
 * TODO: Add menu item for quick access to IME (like Language menu)
 * TODO: Look into BlueZ and Zeemote protocols
 * TODO: Cleanup Utility.java
 * TODO: Cleanup DataDownloader.java
 * TODO: Any way to eliminate DataDownloader? (e.g. use AssetManager class)
 * TODO: Implement SensorController if desired
 * TODO: Do we need an updater class again?
 */
