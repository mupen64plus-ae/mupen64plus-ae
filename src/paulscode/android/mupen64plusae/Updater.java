package paulscode.android.mupen64plusae;

import java.io.File;

import paulscode.android.mupen64plusae.persistent.Config;
import paulscode.android.mupen64plusae.persistent.Settings;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

class Updater
{
    public static boolean checkv1_9( Activity instance )
    {
        // Version 1.9 requires app data to be restored.  Back up saves, then delete the old app data.
        if( Settings.device.getUpgraded19() )
        {
            Utility.copyFile( new File( Settings.path.savesDir ),
                              new File( Settings.path.savesBackupDir )  );
            Utility.deleteFolder( new File( Settings.path.dataDir ) );
            Intent intent = new Intent( instance, MainActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            MenuActivity.mInstance.startActivity( intent );
            instance.finish();
            return false;
        }
        return true;
    }

    public static boolean checkFirstRun( Activity instance )
    {
        // This gets run the first time the app runs only!
        if( Settings.device.getFirstRun() )
        {
            Settings.device.setFirstRun( false );
            selectGamepad( instance );

            String oldPackage = "paulscode.android.mupen64plus";
            File oldVer = new File( Settings.path.storageDir + "/Android/data/" + oldPackage );
            if( !oldVer.exists() )
                oldPackage = "paulscode.android.mupen64plus.xperiaplay";
            Utility.copyFile( new File( Settings.path.storageDir + "/Android/data/" + oldPackage + "/data/save" ),
                              new File( Settings.path.savesDir )  );
            
            if( !Settings.path.isSdCardAccessible() )
            {
               Log.e( "Updater", "SD Card not accessable in method checkFirstRun" );
               return false;
            }
            return true;
        }
        return true;
    }
    
    // Correct the config file if another app (*caugh* N64 4 Droid) corrupted it with an older version
    public static boolean checkConfigFiles( Activity instance )
    {
        boolean everythingOk = true;
        String val;

        val = Settings.mupen64plus_cfg.get( "Core", "Version" );
        if( val == null || !val.equals( "1.00" ) )
            everythingOk = false;

        Settings.mupen64plus_cfg.put( "Core", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "Video-General", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "Video-Rice", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "Input-SDL-Control2", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "Input-SDL-Control3", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "Input-SDL-Control4", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "Audio-SDL", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "UI-Console", "Version", "1.00" );

        val = Settings.mupen64plus_cfg.get( "UI-Console", "PluginDir" );
        if( val == null || !val.equals( "\"" + Settings.path.libsDir + "/lib/\"" ) )
        {
            Settings.mupen64plus_cfg.put( "UI-Console", "PluginDir", "\"" + Settings.path.libsDir + "/lib/\"" );
            Settings.mupen64plus_cfg.put( "UI-Console", "VideoPlugin", "\"" + Settings.path.libsDir + "/lib/libgles2n64.so\"" );
            Settings.mupen64plus_cfg.put( "UI-Console", "AudioPlugin", "\"" + Settings.path.libsDir + "/lib/libaudio-sdl.so\"" );
            Settings.mupen64plus_cfg.put( "UI-Console", "InputPlugin", "\"" + Settings.path.libsDir + "/lib/libinput-sdl.so\"" );
            Settings.mupen64plus_cfg.put( "UI-Console", "RspPlugin", "\"" + Settings.path.libsDir + "/lib/librsp-hle.so\"" );
            everythingOk = false;
        }

        // TODO: Test to see what else could be messed up in this case, and correct it
        return everythingOk;
    }

    public static boolean restoreDefaults( Activity instance )
    {
        Settings.mupen64plus_cfg.put( "Core", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "Core", "OnScreenDisplay", "True" );
        Settings.mupen64plus_cfg.put( "Core", "R4300Emulator", "2" );
        Settings.mupen64plus_cfg.put( "Core", "NoCompiledJump", "False" );
        Settings.mupen64plus_cfg.put( "Core", "DisableExtraMem", "False" );
        Settings.mupen64plus_cfg.put( "Core", "AutoStateSlotIncrement", "False" );
        Settings.mupen64plus_cfg.put( "Core", "EnableDebugger", "False" );
        Settings.mupen64plus_cfg.put( "Core", "CurrentStateSlot", "0" );
        Settings.mupen64plus_cfg.put( "Core", "ScreenshotPath", "\"\"" );
        Settings.mupen64plus_cfg.put( "Core", "SaveStatePath", "\"\"" );
        Settings.mupen64plus_cfg.put( "Core", "SharedDataPath", "\"\"" );

        Settings.mupen64plus_cfg.put( "CoreEvents", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Stop", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Fullscreen", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Save State", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Load State", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Increment Slot", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Reset", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Speed Down", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Speed Up", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Screenshot", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Pause", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Mute", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Increase Volume", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Decrease Volume", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Fast Forward", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Frame Advance", "0" );
        Settings.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Gameshark", "0" );

        Settings.mupen64plus_cfg.put( "Audio-SDL", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "UI-Console", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "UI-Console", "PluginDir", "\"" + Settings.path.libsDir + "/lib/\"" );
        Settings.mupen64plus_cfg.put( "UI-Console", "VideoPlugin", "\"" + Settings.path.libsDir + "/lib/libgles2n64.so\"" );
        Settings.mupen64plus_cfg.put( "UI-Console", "AudioPlugin", "\"" + Settings.path.libsDir + "/lib/libaudio-sdl.so\"" );
        Settings.mupen64plus_cfg.put( "UI-Console", "InputPlugin", "\"" + Settings.path.libsDir + "/lib/libinput-sdl.so\"" );
        Settings.mupen64plus_cfg.put( "UI-Console", "RspPlugin", "\"" + Settings.path.libsDir + "/lib/librsp-hle.so\"" );

        Settings.mupen64plus_cfg.put( "Video-General", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "Video-Rice", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "Video-Rice", "SkipFrame", "1" );
        Settings.mupen64plus_cfg.put( "Video-Rice", "FastTextureLoading", "1" );
        Settings.mupen64plus_cfg.put( "Video-Rice", "FastTextureCRC", "0" );
        Settings.mupen64plus_cfg.put( "Video-Rice", "LoadHiResTextures", "1" );

        Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "Version", "1.00" );
        Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "plugged", "True" );
        Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "plugin", "2" );
        Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "device", "-2" );
        Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "mouse", "False" );
        
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1 )
        {
            // Legacy defaults
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad R", "key(22)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad L", "key(21)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad D", "key(20)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad U", "key(19)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "Start", "key(108)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "Z Trig", "key(102)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "B Button", "key(99)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "A Button", "key(23)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button R", "key(109)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button L", "key(106)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button D", "key(107)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button U", "key(105)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "R Trig", "key(103)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "L Trig", "key(120)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "Mempak switch", "key(44)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "Rumblepak switch", "key(46)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "X Axis", "key(276,275)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "Y Axis", "key(273,274)" );
        }
        else // New defaults if we have Android 3.1+ native gamepad support
        {
            // These inputs are mapped to the Xbox controller's analog inputs, so disable their key mappings
            // These inputs will be collected from MotionEvent objects
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "Z Trig", "key(0)" );               // AXIS_Z:  Xbox left analog trigger
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button R", "key(0)" );           // AXIS RX: Xbox right stick X
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button L", "key(0)" );           // AXIS_RX: Xbox right stick X
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button D", "key(0)" );           // AXIS_RY: Xbox right stick Y
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button U", "key(0)" );           // AXIS_RY: Xbox right stick Y
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "X Axis", "key(0,0)" );             // AXIS_X:  Xbox left stick X
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "Y Axis", "key(0,0)" );             // AXIS_Y:  Xbox left stick Y
            
            // These inputs are mapped to the Xbox controller's digital buttons, so map accordingly
            // These inputs will be collected from KeyEvent objects
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "A Button", "key(96)" );            // KEYCODE_BUTTON_A:  Xbox A button
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "B Button", "key(99)" );            // KEYCODE_BUTTON_X:  Xbox X button (more natural position than Xbox B button)
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "L Trig", "key(102)" );             // KEYCODE_BUTTON_L1: Xbox left shoulder bumper
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "R Trig", "key(103)" );             // KEYCODE_BUTTON_R1: Xbox right shoulder bumper
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "Start", "key(108)" );              // KEYCODE_BUTTON_START:  Xbox start button
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "Mempak switch", "key(106)" );      // KEYCODE_BUTTON_THUMBL: Xbox left stick button
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "Rumblepak switch", "key(107)" );   // KEYCODE_BUTTON_THUMBR: Xbox right stick button
            
            // D-pad inputs will be collected from KeyEvent objects
            // They could also be collected from MotionEvent objects
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad R", "key(22)" );              // KEYCODE_DPAD_RIGHT or AXIS_HAT_X
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad L", "key(21)" );              // KEYCODE_DPAD_LEFT  or AXIS_HAT_X
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad D", "key(20)" );              // KEYCODE_DPAD_DOWN  or AXIS_HAT_Y
            Settings.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad U", "key(19)" );              // KEYCODE_DPAD_UP    or AXIS_HAT_Y
        }
        
        for( int x = 2; x < 5; x++ )
        {
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Version", "1.00" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "plugged", "False" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "plugin", "2" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "device", "-2" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "mouse", "False" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "DPad R", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "DPad L", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "DPad D", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "DPad U", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Start", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Z Trig", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "B Button", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "A Button", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "C Button R", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "C Button L", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "C Button D", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "C Button U", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "R Trig", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "L Trig", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Mempak switch", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Rumblepak switch", "key(0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "X Axis", "key(0,0)" );
            Settings.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Y Axis", "key(0,0)" );
        }
        
        Settings.gles2n64_conf = new Config( Settings.path.gles2n64_conf );
        Settings.gles2n64_conf.put( "[<sectionless!>]", "enable fog", "0" );
        Settings.gles2n64_conf.put( "[<sectionless!>]", "enable alpha test", "1" );
        Settings.gles2n64_conf.put( "[<sectionless!>]", "force screen clear", "0" );
        Settings.gles2n64_conf.put( "[<sectionless!>]", "hack z", "0" );

        if( !Settings.path.isSdCardAccessible() )
        {
           Log.e( "Updater", "SD Card not accessable in method restoreDefaults" );
           return false;
        }
        
        Settings.mupen64plus_cfg.save();
        Settings.gles2n64_conf.save();

        // TODO: Reset preferences to default values (firstRun,updatedV19,redrawAll,octagon,fps,gamepadenabled,rgba8888,volkeys).

        return checkFirstRun( instance );
    }

    private static boolean selectGamepad( Activity instance )
    {
        // TODO: Select appropriate touchscreen layout based on device size.
        return true;
    }
}
