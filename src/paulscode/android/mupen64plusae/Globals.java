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


/**
 * Global settings and meta info. Generally just read-only convenience functions and debug options
 * for developers.
 */
public class Globals
{
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
 * TODO: API - Alternative to setAlpha for button-mapping
 * 
 * Bugs/features related to device parameters (e.g. screensize)
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
