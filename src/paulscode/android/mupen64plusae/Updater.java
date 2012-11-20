package paulscode.android.mupen64plusae;

import java.io.File; 

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

class Updater
{
    public static boolean checkv1_9( Activity mInstance )
    {
        String upgraded = MenuActivity.gui_cfg.get( "GENERAL", "upgraded_1.9" );

        // Version 1.9 requires app data to be restored.  Back up saves, then delete the old app data.
        if( upgraded == null || !upgraded.equals( "1" ) )
        {
            File appData = new File( Globals.DataDir );
            Utility.copyFile( new File( Globals.DataDir + "/data/save" ),
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

        // This gets run the first time the app runs only!
        if( first_run != null && first_run.equals( "1" ) )
        {
            MenuActivity.gui_cfg.put( "GENERAL", "first_run", "0" );
            MenuActivity.gui_cfg.put( "TOUCH_PAD", "which_pad", "Mupen64Plus-AE-Xperia-Play" );
            selectGamepad( mInstance );

            String oldPackage = "paulscode.android.mupen64plus";
            File oldVer = new File( Globals.StorageDir + "/Android/data/" + oldPackage );
            if( !oldVer.exists() )
                oldPackage = "paulscode.android.mupen64plus.xperiaplay";
            Utility.copyFile( new File( Globals.StorageDir + "/Android/data/" + oldPackage + "/data/save" ),
                              new File( Globals.DataDir + "/data/save" )  );

            String romFolder;
            if( (new File( Globals.StorageDir + "/roms/n64" )).isDirectory() )
                romFolder = Globals.StorageDir + "/roms/n64";
            else
                romFolder = Globals.StorageDir;
            
            MenuActivity.gui_cfg.put( "LAST_SESSION", "rom_folder", romFolder );
            MenuActivity.gui_cfg.put( "GENERAL", "auto_save", "0" );
            
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
    
    // Correct the config file if another app (*caugh* N64 4 Droid) corrupted it with an older version
    public static boolean checkCfgVer( Activity mInstance )
    {
        boolean everythingOk = true;
        String val;

        val = MenuActivity.mupen64plus_cfg.get( "Core", "Version" );
        if( val == null || !val.equals( "1.00" ) )
            everythingOk = false;

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

        val = MenuActivity.mupen64plus_cfg.get( "UI-Console", "PluginDir" );
        if( val == null || !val.equals( "\"" + Globals.LibsDir + "/lib/\"" ) )
        {
            MenuActivity.mupen64plus_cfg.put( "UI-Console", "PluginDir", "\"" + Globals.LibsDir + "/lib/\"" );
            MenuActivity.mupen64plus_cfg.put( "UI-Console", "VideoPlugin", "\"" + Globals.LibsDir + "/lib/libgles2n64.so\"" );
            MenuActivity.mupen64plus_cfg.put( "UI-Console", "AudioPlugin", "\"" + Globals.LibsDir + "/lib/libaudio-sdl.so\"" );
            MenuActivity.mupen64plus_cfg.put( "UI-Console", "InputPlugin", "\"" + Globals.LibsDir + "/lib/libinput-sdl.so\"" );
            MenuActivity.mupen64plus_cfg.put( "UI-Console", "RspPlugin", "\"" + Globals.LibsDir + "/lib/librsp-hle.so\"" );
            everythingOk = false;
        }

        // TODO: Test to see what else could be messed up in this case, and correct it
        return everythingOk;
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
        MenuActivity.mupen64plus_cfg.put( "UI-Console", "PluginDir", "\"" + Globals.LibsDir + "/lib/\"" );
        MenuActivity.mupen64plus_cfg.put( "UI-Console", "VideoPlugin", "\"" + Globals.LibsDir + "/lib/libgles2n64.so\"" );
        MenuActivity.mupen64plus_cfg.put( "UI-Console", "AudioPlugin", "\"" + Globals.LibsDir + "/lib/libaudio-sdl.so\"" );
        MenuActivity.mupen64plus_cfg.put( "UI-Console", "InputPlugin", "\"" + Globals.LibsDir + "/lib/libinput-sdl.so\"" );
        MenuActivity.mupen64plus_cfg.put( "UI-Console", "RspPlugin", "\"" + Globals.LibsDir + "/lib/librsp-hle.so\"" );

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
        
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1 )
        {
            // Legacy defaults
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
        }
        else // New defaults if we have Android 3.1+ native gamepad support
        {
            // These inputs are mapped to the Xbox controller's analog inputs, so disable their key mappings
            // These inputs will be collected from MotionEvent objects
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "Z Trig", "key(0)" );               // AXIS_Z:  Xbox left analog trigger
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button R", "key(0)" );           // AXIS RX: Xbox right stick X
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button L", "key(0)" );           // AXIS_RX: Xbox right stick X
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button D", "key(0)" );           // AXIS_RY: Xbox right stick Y
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button U", "key(0)" );           // AXIS_RY: Xbox right stick Y
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "X Axis", "key(0,0)" );             // AXIS_X:  Xbox left stick X
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "Y Axis", "key(0,0)" );             // AXIS_Y:  Xbox left stick Y
            
            // These inputs are mapped to the Xbox controller's digital buttons, so map accordingly
            // These inputs will be collected from KeyEvent objects
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "A Button", "key(96)" );            // KEYCODE_BUTTON_A:  Xbox A button
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "B Button", "key(99)" );            // KEYCODE_BUTTON_X:  Xbox X button (more natural position than Xbox B button)
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "L Trig", "key(102)" );             // KEYCODE_BUTTON_L1: Xbox left shoulder bumper
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "R Trig", "key(103)" );             // KEYCODE_BUTTON_R1: Xbox right shoulder bumper
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "Start", "key(108)" );              // KEYCODE_BUTTON_START:  Xbox start button
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "Mempak switch", "key(106)" );      // KEYCODE_BUTTON_THUMBL: Xbox left stick button
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "Rumblepak switch", "key(107)" );   // KEYCODE_BUTTON_THUMBR: Xbox right stick button
            
            // D-pad inputs will be collected from KeyEvent objects
            // They could also be collected from MotionEvent objects
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad R", "key(22)" );              // KEYCODE_DPAD_RIGHT or AXIS_HAT_X
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad L", "key(21)" );              // KEYCODE_DPAD_LEFT  or AXIS_HAT_X
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad D", "key(20)" );              // KEYCODE_DPAD_DOWN  or AXIS_HAT_Y
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad U", "key(19)" );              // KEYCODE_DPAD_UP    or AXIS_HAT_Y
        }
        
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
        MenuActivity.gui_cfg.put( "GENERAL", "upgraded_1.9", "1" );
        MenuActivity.gui_cfg.put( "GAME_PAD", "redraw_all", "1" );
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

    private static boolean selectGamepad( Activity mInstance )
    {
        int width, height;
        float inches;

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
        
        // Pick a virtual gamepad layout based on screen size/ resolution:
        if( inches > 5.5f )
            MenuActivity.gui_cfg.put( "GAME_PAD", "which_pad", "Mupen64Plus-AE-Analog-Tablet" );
        else if( width <= 320 )
            MenuActivity.gui_cfg.put( "GAME_PAD", "which_pad", "Mupen64Plus-AE-Analog-Tiny" );
        else if( width < 800 )
            MenuActivity.gui_cfg.put( "GAME_PAD", "which_pad", "Mupen64Plus-AE-Analog-Small" );
        else
            MenuActivity.gui_cfg.put( "GAME_PAD", "which_pad", "Mupen64Plus-AE-Analog" );

        return true;
    }

}
