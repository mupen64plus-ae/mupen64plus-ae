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

public class Globals
{
    // Global preferences/settings
    public static UserPrefs userPrefs;
    public static AppData appData;
    public static Paths paths;
    
    //-- Device specific booleans --//
    
    /** True if device is runnning Eclair or later (Android 2.0.x) */
    public static boolean isEclair = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR;
    /** True if device is running Gingerbread or later (Android 2.3.x) */
    public static boolean isGingerbread = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    /** True if device is running Honeycomb or later (Android 3.0.x)*/
    public static boolean isHoneycomb = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    /** True if device is running Honeycomb MR1 or later (Android 3.1.x)*/
    public static boolean isHoneycombMR1 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    /** True if device is running Jellybean or later (Android 4.2.x)*/
    public static boolean isJellyBean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    
    // Internal development options, might change later or expose to user
    public static final boolean INHIBIT_SUSPEND = true;
    public static final boolean DOWNLOAD_TO_SDCARD = true;
    public static final int SPLASH_DELAY = 1000;
}

/**
 * Bugs and feature requests not listed elsewhere
 * 
 * TODO: *Figure out force-close when loading last session autosavefile
 * TODO: *Figure out failure to return to main menu
 * TODO: *Implement multi-player peripheral controls
 * TODO: *Implement special func mapping
 * TODO: Add menu item for quick access to IME (like Language menu)
 * TODO: Look into BlueZ and Zeemote protocols
 * TODO: Cleanup Utility.java
 * TODO: Cleanup DataDownloader.java
 * TODO: Any way to eliminate DataDownloader? (e.g. use AssetManager class)
 * TODO: Implement SensorController if desired
 * TODO: Do we need an updater class again?
 */
