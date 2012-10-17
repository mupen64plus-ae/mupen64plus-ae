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

import paulscode.android.mupen64plusae.input.InputMap;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class Settings
{
    public static _User user;
    public static _Device device;
    public static _Path path;
    
    // Configuration files for native code
    public static Config mupen64plus_cfg;
    public static Config gles2n64_conf;
    public static int[][] ctrlr = new int[4][4];
    
    public static void refreshUser( SharedPreferences userSharedPreferences )
    {
        user = new _User( userSharedPreferences );
    }
    
    public static void refreshDevice( SharedPreferences deviceSharedPreferences )
    {
        device = new _Device( deviceSharedPreferences );
    }
    
    public static void refreshPath( Activity mainActivity )
    {
        path = new _Path( mainActivity );
    }
    
    public static class _User
    {
        public final String lastGame;
        
        public final boolean touchscreenEnabled;
        public final int touchscreenLayoutIndex;
        public final boolean touchscreenFrameRate;
        public final boolean touchscreenOctagonJoystick;
        public final boolean touchscreenRedrawAll;
        
        public final boolean gamepadEnabled;
        public final int gamepadPluginIndex;
        public final InputMap gamepadMap1;
        public final InputMap gamepadMap2;
        public final InputMap gamepadMap3;
        public final InputMap gamepadMap4;
        public final boolean volumeKeysEnabled;
        
        public final boolean videoEnabled;
        public final int videoPluginIndex;
        public final boolean videoStretch;
        public final boolean videoRGBA8888;
        
        public final int audioPluginIndex;
        public final int xperiaPluginIndex;
        public final int rspPluginIndex;
        public final int corePluginIndex;
        
        public final String gameRomDir;
        public final String gameSaveDir;
        public final boolean autoSaveEnabled;
        
        // Derived values
        public final boolean audioEnabled;
        public final boolean xperiaEnabled;
        public final boolean isLastGameZipped;
        public final boolean isLastGameNull;
        
        private final SharedPreferences preferences;
        
        public _User( SharedPreferences userSharedPreferences )
        {
            // TODO: Seriously? ADK can't auto-generate a class like this?
            
            preferences = userSharedPreferences;
            
            lastGame = preferences.getString( "lastGame", "" );
            
            gamepadEnabled = preferences.getBoolean( "gamepadEnabled", true );
            gamepadPluginIndex = getArrayIndex( preferences, "gamepadPlugin", 0 );
            gamepadMap1 = new InputMap( preferences.getString( "gamepadMap1", "" ) );
            gamepadMap2 = new InputMap( preferences.getString( "gamepadMap2", "" ) );
            gamepadMap3 = new InputMap( preferences.getString( "gamepadMap3", "" ) );
            gamepadMap4 = new InputMap( preferences.getString( "gamepadMap4", "" ) );
            volumeKeysEnabled = preferences.getBoolean( "volumeKeysEnabled", true );
            
            touchscreenEnabled = preferences.getBoolean( "touchscreenEnabled", true );
            touchscreenLayoutIndex = getArrayIndex( preferences, "touchscreenLayout", 0 );
            touchscreenFrameRate = preferences.getBoolean( "touchscreenFrameRate", false );
            touchscreenOctagonJoystick = preferences
                    .getBoolean( "touchscreenOctagonJoystick", true );
            touchscreenRedrawAll = preferences.getBoolean( "touchscreenRedrawAll", false );
            
            videoEnabled = preferences.getBoolean( "videoEnabled", true );
            videoPluginIndex = getArrayIndex( preferences, "videoPlugin", 0 );
            videoStretch = preferences.getBoolean( "videoStretch", false );
            videoRGBA8888 = preferences.getBoolean( "videoRGBA8888", false );
            
            audioPluginIndex = getArrayIndex( preferences, "audioPlugin", 0 );
            xperiaPluginIndex = getArrayIndex( preferences, "xperiaPlugin", -1 );
            rspPluginIndex = getArrayIndex( preferences, "rspPlugin", 0 );
            corePluginIndex = getArrayIndex( preferences, "corePlugin", 0 );
            
            // TODO: Bad code smell... path needs to be initialized first
            assert path != null;
            gameRomDir = preferences.getString( "gameRomDir", path.defaultRomDir );
            gameSaveDir = preferences.getString( "gameSaveDir", path.storageDir );
            autoSaveEnabled = preferences.getBoolean( "autoSaveEnabled", false );
            
            // Derived values
            audioEnabled = audioPluginIndex >= 0;
            xperiaEnabled = xperiaPluginIndex >= 0;
            isLastGameNull = lastGame == null && lastGame.length() > 0;
            isLastGameZipped = lastGame.substring( lastGame.length() - 3, lastGame.length() )
                    .equalsIgnoreCase( "zip" );
        }
        
        public void resetToDefaults()
        {
            // TODO: Implement reset preferences
        }
    }
    
    public static class _Device
    {
        private final SharedPreferences preferences;
        
        // paulscode, added for different configurations based on hardware
        // (part of the missing shadows and stars bug fix)
        // Must match the #define's in OpenGL.cpp!
        public static final int HARDWARE_TYPE_UNKNOWN = 0;
        public static final int HARDWARE_TYPE_OMAP = 1;
        public static final int HARDWARE_TYPE_QUALCOMM = 2;
        public static final int HARDWARE_TYPE_IMAP = 3;
        public static final int HARDWARE_TYPE_TEGRA2 = 4;
        
        public _Device( SharedPreferences deviceSharedPreferences )
        {
            preferences = deviceSharedPreferences;
        }
        
        public boolean getFirstRun()
        {
            return preferences.getBoolean( "firstRun", false );
        }
        
        public void setFirstRun( boolean value )
        {
            preferences.edit().putBoolean( "firstRun", value );
        }
        
        public boolean getUpgraded19()
        {
            return preferences.getBoolean( "updatedVer19", false );
        }
        
        public void setUpgraded19( boolean value )
        {
            preferences.edit().putBoolean( "updatedVer19", value );
        }
        
        public int getHardwareType()
        {
            return preferences.getInt( "hardwareType", HARDWARE_TYPE_UNKNOWN );
        }
        
        public void setHardwareType( String hardware, String features )
        {
            if (hardware != null)
            {
                if( hardware.contains( "mapphone" )
                        || hardware.contains( "tuna" )
                        || hardware.contains( "smdkv" )
                        || hardware.contains( "herring" )
                        || hardware.contains( "aries" ) )
                    setHardwareType( HARDWARE_TYPE_OMAP );
                
                else if( hardware.contains( "liberty" )
                        || hardware.contains( "gt-s5830" )
                        || hardware.contains( "zeus" ) )
                    setHardwareType( HARDWARE_TYPE_QUALCOMM );
                
                else if( hardware.contains( "imap" ) )
                    setHardwareType( HARDWARE_TYPE_IMAP );
                
                else if( hardware.contains( "tegra 2" )
                        || hardware.contains( "grouper" )
                        || hardware.contains( "meson-m1" )
                        || hardware.contains( "smdkc" )
                        || ( features != null && features.contains( "vfpv3d16" ) ) )
                    setHardwareType( HARDWARE_TYPE_TEGRA2 );
                
                else
                    setHardwareType( HARDWARE_TYPE_UNKNOWN );
            }
            else
                setHardwareType( HARDWARE_TYPE_UNKNOWN );
        }
        
        private void setHardwareType( int value )
        {
            preferences.edit().putInt( "hardwareType", value );
        }
        
        public void resetToDefaults()
        {
        }
    }
    
    public static class _Path
    {
        public final String packageName;
        
        // Directory names
        public final String storageDir;
        public final String dataDir;
        public final String libsDir;
        public final String restoreDir;
        public final String savesDir;
        public final String savesBackupDir;
        public final String defaultRomDir;
        
        // File names
        public final String mupen64plus_cfg;
        public final String gles2n64_conf;
        public final String error_log;
        public final String gamepad_ini;
        public final String touchpad_ini;
        
        // Preference names
        public final String devicePrefName;
        public final String userPrefName;
        
        public static String dataDownloadUrl = "Data size is 1.0 Mb|mupen64plus_data.zip";
        
        // Internal settings; TODO: Should we make this public?
        private static boolean downloadToSdCard = true;
        
        public _Path( Activity mainActivity )
        {
            packageName = mainActivity.getPackageName();
            
            // Directories
            libsDir = "/data/data/" + packageName;
            storageDir = downloadToSdCard
                    ? Environment.getExternalStorageDirectory().getAbsolutePath()
                    : mainActivity.getFilesDir().getAbsolutePath();
            dataDir = storageDir + ( downloadToSdCard
                    ? "/Android/data/" + packageName
                    : "" );
            savesDir = dataDir + "/data/save";
            restoreDir = storageDir + "/mp64p_tmp_asdf1234lkjh0987/data/save";
            savesBackupDir = restoreDir + "/data/save";
            
            String romFolder = storageDir + "/roms/n64";
            if( ( new File( romFolder ) ).isDirectory() )
                defaultRomDir = romFolder;
            else
                defaultRomDir = storageDir;
            
            // Files
            mupen64plus_cfg = dataDir + "/mupen64plus.cfg";
            gles2n64_conf = dataDir + "/data/gles2n64.conf";
            error_log = dataDir + "/error.log";
            gamepad_ini = dataDir + "/skins/gamepads/gamepad_list.ini";
            touchpad_ini = dataDir + "/skins/touchpads/touchpad_list.ini";
            
            // Preference names
            devicePrefName = mainActivity.getPackageName() + "_preferences_device";
            userPrefName = mainActivity.getPackageName() + "_preferences_user";
            
            Log.v( "DataDir Check", "PackageName set to '" + packageName + "'" );
            Log.v( "DataDir Check", "LibsDir set to '" + libsDir + "'" );
            Log.v( "DataDir Check", "StorageDir set to '" + storageDir + "'" );
            Log.v( "DataDir Check", "DataDir set to '" + dataDir + "'" );
        }
        
        public boolean isSdCardAccessible()
        {
            return ( new File( storageDir ) ).exists();
        }
    }
    
    private static int getArrayIndex( SharedPreferences preferences, String key, int defaultValue )
    {
        // This assumes the array values can be parsed from strings to ints
        try
        {
            return Integer.parseInt( preferences.getString( key, String.valueOf( defaultValue ) ),
                    defaultValue );
        }
        catch( NumberFormatException ex )
        {
            return defaultValue;
        }
    }
}
