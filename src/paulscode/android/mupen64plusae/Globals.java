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

/**
 * Global settings and meta info. Generally just read-only convenience functions and debug options
 * for developers.
 */
public class Globals
{
    /** Read-only accessor for user preferences. */
    public static UserPrefs userPrefs;
    
    /** Persistent application state and data. */
    public static AppData appData;
    
    /** Read-only accessor for file system path strings. */
    public static Paths paths;
    
    /** True if device is running Eclair or later (Android 2.0.x) */
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
}

/**
 * Bugs and feature requests not listed elsewhere, in order of priority.
 * 
 * Major bugs or missing features
 * TODO: Major - Add cheats menu
 * TODO: Major - Add reset to ingame menu
 * TODO: Major - Implement multi-player peripheral controls
 * TODO: Major - Implement special func mapping
 * 
 * Minor bugs or missing features
 * TODO: Minor - Figure out crash on NativeMethods.quit (or implement workaround)
 * TODO: Minor - Keep surface rendering onPause (don't blackout)
 * 
 * Bugs/features related to older APIs (e.g. Gingerbread)
 * TODO: API - Use SuppressLint instead of TargetAPI?
 * TODO: API - Blurry letters in dialog boxes
 * TODO: API - Mapping crash
 * TODO: API - Alternative to setAlpha for button-mapping
 * 
 * Bugs related to device parameters (e.g. screensize)
 * TODO: Device - Button mapping layout
 * 
 * Features, nice-to-haves, anti-features
 * TODO: Feature - Hide action bar on menu click
 * TODO: Feature - Add menu item for quick access to IME (like Language menu)
 * TODO: Feature - Look into BlueZ and Zeemote protocols
 * TODO: Feature - Implement SensorController if desired
 * TODO: Feature - Do we need an updater class?
 * TODO: Feature - Do we need status notification? 
 * 
 * Code polishing (doesn't directly affect end user)
 * TODO: Polish - Cleanup Utility.java
 * TODO: Polish - Cleanup DataDownloader.java
 * TODO: Polish - Look into AssetManager class vs DataDownloader
 */
