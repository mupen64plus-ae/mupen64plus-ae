/**
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
package paulscode.android.mupen64plusae;

/**
 * Just a simple class to consolidate all strings used as keys throughout the app. Keys are
 * typically used to pass data between activities (in the extras bundle) and to persist data (in
 * shared preferences, config files, etc.).
 */
public class Keys
{
    /**
     * Keys used to pass data to activities via the intent extras bundle. It's good practice to
     * namespace the keys to avoid conflicts with other apps. By convention this is usually the
     * package name but it's not a strict requirement. We'll use the fully qualified name of this
     * class since it's easy to get.
     */
    public static class Extras
    {
        private static final String NAMESPACE = Extras.class.getCanonicalName() + ".";
        //@formatter:off
        public static final String ROM_PATH             = NAMESPACE + "ROM_PATH";
        public static final String ROM_MD5              = NAMESPACE + "ROM_MD5";
        public static final String CHEAT_ARGS           = NAMESPACE + "CHEAT_ARGS";
        public static final String DO_RESTART           = NAMESPACE + "DO_RESTART";
        public static final String PROFILE_NAME         = NAMESPACE + "PROFILE_NAME";
        public static final String MENU_DISPLAY_MODE    = NAMESPACE + "MENU_DISPLAY_MODE";
        public static final String ART_PATH             = NAMESPACE + "ART_PATH";
        public static final String ROM_NAME             = NAMESPACE + "ROM_NAME";
        //@formatter:on
    }
    
    public static class Prefs
    {
        
    }
    
    public static class Config
    {
        
    }
}
