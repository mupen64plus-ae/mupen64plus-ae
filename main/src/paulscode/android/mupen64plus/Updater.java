package paulscode.android.mupen64plus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;

class Updater
{
    public static boolean checkv1_9( Activity mInstance )
    {
        String upgraded = MenuActivity.gui_cfg.get( "GENERAL", "upgraded_1.9" );

        if( upgraded == null || !upgraded.equals( "1" ) )
        { // Version 1.9 requires app data to be restored.  Back up saves, then delete the old app data.
            File appData = new File( Globals.DataDir );
            Updater.copyFile( new File( Globals.DataDir + "/data/save" ),
                              new File( Globals.StorageDir + "/mp64p_tmp_asdf1234lkjh0987/data/save" )  );
            Utility.deleteFolder( appData );
            Intent intent = new Intent( mInstance, MainActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            MenuActivity.mInstance.startActivity( intent );
            mInstance.finish();
            return false;
        }
        return true;
    }
    public static boolean checkFirstRun( Activity mInstance )
    {
        String first_run = MenuActivity.gui_cfg.get( "GENERAL", "first_run" );
        int width, height;
        float inches;

        if( first_run != null && first_run.equals( "1" ) )
        { // This gets run the first time the app runs only!
            MenuActivity.gui_cfg.put( "GENERAL", "first_run", "0" );
            DisplayMetrics metrics = new DisplayMetrics();
            mInstance.getWindowManager().getDefaultDisplay().getMetrics( metrics );
            if( metrics.widthPixels > metrics.heightPixels )
            {
                width = metrics.widthPixels;
                inches = (float) width / metrics.xdpi;
                height = metrics.heightPixels;
            }
            else
            {
                width = metrics.heightPixels;
                inches = (float) width / metrics.ydpi;
                height = metrics.widthPixels;
            }
 
            // Pick a virtual gamepad layout based on screen resolution:
            if( inches > 5.5f )
                MenuActivity.gui_cfg.put( "GAME_PAD", "which_pad", "Mupen64Plus-AE-Touch-Tablet" );
            else if( width <= 320 )
                MenuActivity.gui_cfg.put( "GAME_PAD", "which_pad", "Mupen64Plus-AE-Touch-Tiny" );
            else if( width < 800 )
                MenuActivity.gui_cfg.put( "GAME_PAD", "which_pad", "Mupen64Plus-AE-Touch-Small" );
            else
                MenuActivity.gui_cfg.put( "GAME_PAD", "which_pad", "Mupen64Plus-AE-Touch" );
            MenuActivity.gui_cfg.put( "TOUCH_PAD", "which_pad", "Mupen64Plus-AE-Xperia-Play" );

            String romFolder;
            if( (new File( Globals.StorageDir + "/roms/n64" )).isDirectory() )
                romFolder = Globals.StorageDir + "/roms/n64";
            else
                romFolder = Globals.StorageDir;
            MenuActivity.gui_cfg.put( "LAST_SESSION", "rom_folder", romFolder );
            MenuActivity.gui_cfg.put( "GENERAL", "auto_save", "1" );
            File f = new File( Globals.StorageDir );
            if( !f.exists() )
            {
               Log.e( "Updater", "SD Card not accessable in method restoreDefaults" );
               return false;
            }
            return true;
        }
        return true;
    }
    public static boolean checkCfgVer( Activity mInstance )
    {  // Correct the config file if another app (*caugh* N64 4 Droid) corrupted it with an older version
        MenuActivity.mupen64plus_cfg.put( "Core", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "Video-General", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "Video-Rice", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control2", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control3", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control4", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "Audio-SDL", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "UI-Console", "Version", "1.00" );

        Log.v( "CfgVer Check", "PluginDir = '" + MenuActivity.mupen64plus_cfg.get( "UI_Console", "PluginDir" ) + "'" );
        Log.v( "CfgVer Check", "VideoPlugin = '" + MenuActivity.mupen64plus_cfg.get( "UI_Console", "VideoPlugin" ) + "'" );
        Log.v( "CfgVer Check", "AudioPlugin = '" + MenuActivity.mupen64plus_cfg.get( "UI_Console", "AudioPlugin" ) + "'" );
        Log.v( "CfgVer Check", "InputPlugin = '" + MenuActivity.mupen64plus_cfg.get( "UI_Console", "InputPlugin" ) + "'" );
        Log.v( "CfgVer Check", "RspPlugin = '" + MenuActivity.mupen64plus_cfg.get( "UI_Console", "RspPlugin" ) + "'" );

        // TODO: Test to see what else could be messed up in this case, correct it, & return meaningful value
        return true;
    }

    public static boolean restoreDefaults( Activity mInstance )
    {
        MenuActivity.mupen64plus_cfg.put( "Core", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "Core", "OnScreenDisplay", "True" );
        MenuActivity.mupen64plus_cfg.put( "Core", "R4300Emulator", "2" );
        MenuActivity.mupen64plus_cfg.put( "Core", "NoCompiledJump", "False" );
        MenuActivity.mupen64plus_cfg.put( "Core", "DisableExtraMem", "False" );
        MenuActivity.mupen64plus_cfg.put( "Core", "AutoStateSlotIncrement", "False" );
        MenuActivity.mupen64plus_cfg.put( "Core", "EnableDebugger", "False" );
        MenuActivity.mupen64plus_cfg.put( "Core", "CurrentStateSlot", "0" );
        MenuActivity.mupen64plus_cfg.put( "Core", "ScreenshotPath", "\"\"" );
        MenuActivity.mupen64plus_cfg.put( "Core", "SaveStatePath", "\"\"" );
        MenuActivity.mupen64plus_cfg.put( "Core", "SharedDataPath", "\"\"" );

        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Stop", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Fullscreen", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Save State", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Load State", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Increment Slot", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Reset", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Speed Down", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Speed Up", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Screenshot", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Pause", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Mute", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Increase Volume", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Decrease Volume", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Fast Forward", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Frame Advance", "0" );
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Gameshark", "0" );

        MenuActivity.mupen64plus_cfg.put( "Audio-SDL", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "UI-Console", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "UI-Console", "PluginDir", Globals.LibsDir + "/lib/" );
        MenuActivity.mupen64plus_cfg.put( "UI-Console", "VideoPlugin", Globals.LibsDir + "/lib/libgles2n64.so" );
        MenuActivity.mupen64plus_cfg.put( "UI-Console", "AudioPlugin", Globals.LibsDir + "/lib/libaudio-sdl.so" );
        MenuActivity.mupen64plus_cfg.put( "UI-Console", "InputPlugin", Globals.LibsDir + "/lib/libinput-sdl.so" );
        MenuActivity.mupen64plus_cfg.put( "UI-Console", "RspPlugin", Globals.LibsDir + "/lib/librsp-hle.so" );

        MenuActivity.mupen64plus_cfg.put( "Video-General", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "Video-Rice", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "Video-Rice", "SkipFrame", "1" );
        MenuActivity.mupen64plus_cfg.put( "Video-Rice", "FastTextureLoading", "1" );
        MenuActivity.mupen64plus_cfg.put( "Video-Rice", "FastTextureCRC", "0" );
        MenuActivity.mupen64plus_cfg.put( "Video-Rice", "LoadHiResTextures", "1" );

        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "Version", "1.00" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "plugged", "True" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "plugin", "2" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "device", "-2" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "mouse", "False" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad R", "key(22)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad L", "key(21)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad D", "key(20)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad U", "key(19)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "Start", "key(108)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "Z Trig", "key(102)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "B Button", "key(99)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "A Button", "key(23)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button R", "key(109)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button L", "key(106)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button D", "key(107)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button U", "key(105)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "R Trig", "key(103)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "L Trig", "key(120)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "Mempak switch", "key(44)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "Rumblepak switch", "key(46)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "X Axis", "key(276,275)" );
        MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "Y Axis", "key(273,274)" );

        for( int x = 2; x < 5; x++ )
        {
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Version", "1.00" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "plugged", "False" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "plugin", "2" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "device", "-2" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "mouse", "False" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "DPad R", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "DPad L", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "DPad D", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "DPad U", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Start", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Z Trig", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "B Button", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "A Button", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "C Button R", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "C Button L", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "C Button D", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "C Button U", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "R Trig", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "L Trig", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Mempak switch", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Rumblepak switch", "key(0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "X Axis", "key(0,0)" );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Y Axis", "key(0,0)" );
        }

        MenuActivity.gui_cfg.clear();
        MenuActivity.gui_cfg.put( "GENERAL", "first_run", "1" );
        MenuActivity.gui_cfg.put( "GAME_PAD", "redraw_all", "0" );
        MenuActivity.gui_cfg.put( "GAME_PAD", "analog_octagon", "1" );
        MenuActivity.gui_cfg.put( "GAME_PAD", "show_fps", "0" );
        MenuActivity.gui_cfg.put( "GAME_PAD", "enabled", "1" );
        MenuActivity.gui_cfg.put( "VIDEO_PLUGIN", "enabled", "1" );
        MenuActivity.gui_cfg.put( "VIDEO_PLUGIN", "rgba8888", "0" );
        MenuActivity.gui_cfg.put( "KEYS", "disable_volume_keys", "0" );
        Globals.volumeKeysDisabled = false;

        Config gles2n64_conf = new Config( Globals.DataDir + "/data/gles2n64.conf" );
        gles2n64_conf.put( "[<sectionless!>]", "enable fog", "0" );
        gles2n64_conf.put( "[<sectionless!>]", "enable alpha test", "1" );
//        gles2n64_conf.put( "[<sectionless!>]", "tribuffer opt", "1" );
        gles2n64_conf.put( "[<sectionless!>]", "force screen clear", "0" );
        gles2n64_conf.put( "[<sectionless!>]", "hack z", "0" );

        File f = new File( Globals.StorageDir );
        if( !f.exists() )
        {
           Log.e( "Updater", "SD Card not accessable in method restoreDefaults" );
           return false;
        }
        MenuActivity.mupen64plus_cfg.save();
        MenuActivity.gui_cfg.save();
        gles2n64_conf.save();
        return checkFirstRun( mInstance );
    }
    public static boolean copyFile( File src, File dest )
    {
        if( src == null )
            return true;

        if( dest == null )
        {
            Log.e( "Updater", "dest null in method 'copyFile'" );
            return false;
        }

        if( src.isDirectory() )
        {
            boolean success = true;
            if( !dest.exists() )
                dest.mkdirs();
            String files[] = src.list();
            for( String file : files )
            {
                success = success && copyFile( new File( src, file ), new File( dest, file ) );
            }
            return success;
        }
        else
        {
            File f = dest.getParentFile();
            if( f == null )
            {
                Log.e( "Updater", "dest parent folder null in method 'copyFile'" );
                return false;
            }
            if( !f.exists() )
                f.mkdirs();

            InputStream in = null;
            OutputStream out = null;
            try
            {
                in = new FileInputStream( src );
                out = new FileOutputStream( dest );

                byte[] buf = new byte[1024];
                int len;
                while( ( len = in.read( buf ) ) > 0 )
                {
                    out.write( buf, 0, len );
                }
            }
            catch( IOException ioe )
            {
                Log.e( "Updater", "IOException in method 'copyFile': " + ioe.getMessage() );
                return false;
            }
            try
            {
                in.close();
                out.close();
            }
            catch( IOException ioe )
            {}
            catch( NullPointerException npe )
            {}
            return true;
        }
    }
}
