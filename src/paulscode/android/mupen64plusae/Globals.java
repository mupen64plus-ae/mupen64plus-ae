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

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.Paths;
import paulscode.android.mupen64plusae.persistent.UserPrefs;

public class Globals
{
    // Global preferences/settings
    public static UserPrefs userPrefs;
    public static AppData appData;
    public static Paths paths;
    
    // Internal development options, might change later or expose to user
    public static final boolean INHIBIT_SUSPEND = true;
    public static final boolean DOWNLOAD_TO_SDCARD = true;
    public static final int SPLASH_DELAY = 1000;
}

/**
 * Bugs and feature requests not listed elsewhere
 * 
 * TODO: Implement multi-player peripheral controls
 * TODO: Cleanup Utility.java
 * TODO: Cleanup DataDownloader.java
 * TODO: Any way to eliminate DataDownloader? (e.g. use AssetManager class)
 * TODO: Implement save/load state dialogs
 * TODO: Make sure state saves/loads to the right place according to preference
 * TODO: Implement SensorController if desired
 * TODO: Do we need an updater class again?
 * TODO: Figure out force-close when resumeLastSession is true (might just relate to broken statesaves)
 * TODO: Figure out failure to return to main menu
 */
