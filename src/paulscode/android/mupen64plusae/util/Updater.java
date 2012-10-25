package paulscode.android.mupen64plusae.util;

import java.io.File;

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.MainActivity;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class Updater
{
    public static boolean checkFirstRun( Activity instance )
    {
        // This is called the first time the app runs only!
        if( Globals.appData.isFirstRun() )
        {
            Globals.appData.setFirstRun( false );
            setTouchscreenLayout( instance );

            String oldPackage = "paulscode.android.mupen64plus";
            File oldVer = new File( Globals.paths.storageDir + "/Android/data/" + oldPackage );
            if( !oldVer.exists() )
                oldPackage = "paulscode.android.mupen64plus.xperiaplay";
            Utility.copyFile( new File( Globals.paths.storageDir + "/Android/data/" + oldPackage + "/data/save" ),
                              new File( Globals.paths.defaultSavesDir )  );
            
            if( !Globals.paths.isSdCardAccessible() )
            {
               Log.e( "Updater", "SD Card not accessable in method checkFirstRun" );
               return false;
            }
            return true;
        }
        return true;
    }
    
    public static boolean checkLatestVersion( Activity instance )
    {
        // Version 1.9 requires app data to be restored. Back up saves, then delete the old app data.
        if( !Globals.appData.isUpgradedVer19() )
        {
            Globals.appData.setUpgradedVer19( true );
            Utility.copyFile( new File( Globals.paths.defaultSavesDir ), new File(
                    Globals.paths.savesBackupDir ) );
            Utility.deleteFolder( new File( Globals.paths.dataDir ) );
            Intent intent = new Intent( instance, MainActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            instance.startActivity( intent );
            instance.finish();
            return false;
        }
        return true;
    }
    
    public static boolean checkConfigFiles( Activity instance )
    {
        // Correct the config file if another app (*caugh* N64 4 Droid) corrupted it with an older version
        boolean everythingOk = true;
        String val = Globals.mupen64plus_cfg.get( "Core", "Version" );
        if( val == null || !val.equals( "1.00" ) )
            everythingOk = false;

        Globals.mupen64plus_cfg.put( "Core", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "Video-General", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "Video-Rice", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "Input-SDL-Control2", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "Input-SDL-Control3", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "Input-SDL-Control4", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "Audio-SDL", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "UI-Console", "Version", "1.00" );

        val = Globals.mupen64plus_cfg.get( "UI-Console", "PluginDir" );
        if( val == null || !val.equals( "\"" + Globals.paths.libsDir + "\"" ) )
        {
            everythingOk = false;
            Globals.mupen64plus_cfg.put( "UI-Console", "PluginDir", "\"" + Globals.paths.libsDir + "\"" );
            Globals.mupen64plus_cfg.put( "UI-Console", "VideoPlugin", "\"" + Globals.paths.libsDir + "libgles2n64.so\"" );
            Globals.mupen64plus_cfg.put( "UI-Console", "AudioPlugin", "\"" + Globals.paths.libsDir + "libaudio-sdl.so\"" );
            Globals.mupen64plus_cfg.put( "UI-Console", "InputPlugin", "\"" + Globals.paths.libsDir + "libinput-sdl.so\"" );
            Globals.mupen64plus_cfg.put( "UI-Console", "RspPlugin", "\"" + Globals.paths.libsDir + "librsp-hle.so\"" );
        }

        // TODO: Test to see what else could be messed up in this case, and correct it
        return everythingOk;
    }

    public static boolean restoreDefaults( Activity instance )
    {
        Globals.mupen64plus_cfg.put( "Core", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "Core", "OnScreenDisplay", "True" );
        Globals.mupen64plus_cfg.put( "Core", "R4300Emulator", "2" );
        Globals.mupen64plus_cfg.put( "Core", "NoCompiledJump", "False" );
        Globals.mupen64plus_cfg.put( "Core", "DisableExtraMem", "False" );
        Globals.mupen64plus_cfg.put( "Core", "AutoStateSlotIncrement", "False" );
        Globals.mupen64plus_cfg.put( "Core", "EnableDebugger", "False" );
        Globals.mupen64plus_cfg.put( "Core", "CurrentStateSlot", "0" );
        Globals.mupen64plus_cfg.put( "Core", "ScreenshotPath", "\"\"" );
        Globals.mupen64plus_cfg.put( "Core", "SaveStatePath", "\"\"" );
        Globals.mupen64plus_cfg.put( "Core", "SharedDataPath", "\"\"" );

        Globals.mupen64plus_cfg.put( "CoreEvents", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Stop", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Fullscreen", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Save State", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Load State", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Increment Slot", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Reset", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Speed Down", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Speed Up", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Screenshot", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Pause", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Mute", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Increase Volume", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Decrease Volume", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Fast Forward", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Frame Advance", "0" );
        Globals.mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Gameshark", "0" );

        Globals.mupen64plus_cfg.put( "Audio-SDL", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "UI-Console", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "UI-Console", "PluginDir", "\"" + Globals.paths.libsDir + "\"" );
        Globals.mupen64plus_cfg.put( "UI-Console", "VideoPlugin", "\"" + Globals.paths.libsDir + "libgles2n64.so\"" );
        Globals.mupen64plus_cfg.put( "UI-Console", "AudioPlugin", "\"" + Globals.paths.libsDir + "libaudio-sdl.so\"" );
        Globals.mupen64plus_cfg.put( "UI-Console", "InputPlugin", "\"" + Globals.paths.libsDir + "libinput-sdl.so\"" );
        Globals.mupen64plus_cfg.put( "UI-Console", "RspPlugin", "\"" + Globals.paths.libsDir + "librsp-hle.so\"" );

        Globals.mupen64plus_cfg.put( "Video-General", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "Video-Rice", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "Video-Rice", "SkipFrame", "1" );
        Globals.mupen64plus_cfg.put( "Video-Rice", "FastTextureLoading", "1" );
        Globals.mupen64plus_cfg.put( "Video-Rice", "FastTextureCRC", "0" );
        Globals.mupen64plus_cfg.put( "Video-Rice", "LoadHiResTextures", "1" );

        Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "Version", "1.00" );
        Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "plugged", "True" );
        Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "plugin", "2" );
        Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "device", "-2" );
        Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "mouse", "False" );
        
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1 )
        {
            // Legacy defaults
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad R", "key(22)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad L", "key(21)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad D", "key(20)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad U", "key(19)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "Start", "key(108)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "Z Trig", "key(102)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "B Button", "key(99)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "A Button", "key(23)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button R", "key(109)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button L", "key(106)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button D", "key(107)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button U", "key(105)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "R Trig", "key(103)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "L Trig", "key(120)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "Mempak switch", "key(44)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "Rumblepak switch", "key(46)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "X Axis", "key(276,275)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "Y Axis", "key(273,274)" );
        }
        else // New defaults if we have Android 3.1+ native gamepad support
        {
            // These inputs are mapped to the Xbox controller's analog inputs, so disable their key mappings
            // These inputs will be collected from MotionEvent objects
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "Z Trig", "key(0)" );               // AXIS_Z:  Xbox left analog trigger
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button R", "key(0)" );           // AXIS RX: Xbox right stick X
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button L", "key(0)" );           // AXIS_RX: Xbox right stick X
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button D", "key(0)" );           // AXIS_RY: Xbox right stick Y
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "C Button U", "key(0)" );           // AXIS_RY: Xbox right stick Y
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "X Axis", "key(0,0)" );             // AXIS_X:  Xbox left stick X
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "Y Axis", "key(0,0)" );             // AXIS_Y:  Xbox left stick Y
            
            // These inputs are mapped to the Xbox controller's digital buttons, so map accordingly
            // These inputs will be collected from KeyEvent objects
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "A Button", "key(96)" );            // KEYCODE_BUTTON_A:  Xbox A button
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "B Button", "key(99)" );            // KEYCODE_BUTTON_X:  Xbox X button (more natural position than Xbox B button)
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "L Trig", "key(102)" );             // KEYCODE_BUTTON_L1: Xbox left shoulder bumper
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "R Trig", "key(103)" );             // KEYCODE_BUTTON_R1: Xbox right shoulder bumper
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "Start", "key(108)" );              // KEYCODE_BUTTON_START:  Xbox start button
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "Mempak switch", "key(106)" );      // KEYCODE_BUTTON_THUMBL: Xbox left stick button
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "Rumblepak switch", "key(107)" );   // KEYCODE_BUTTON_THUMBR: Xbox right stick button
            
            // D-pad inputs will be collected from KeyEvent objects
            // They could also be collected from MotionEvent objects
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad R", "key(22)" );              // KEYCODE_DPAD_RIGHT or AXIS_HAT_X
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad L", "key(21)" );              // KEYCODE_DPAD_LEFT  or AXIS_HAT_X
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad D", "key(20)" );              // KEYCODE_DPAD_DOWN  or AXIS_HAT_Y
            Globals.mupen64plus_cfg.put( "Input-SDL-Control1", "DPad U", "key(19)" );              // KEYCODE_DPAD_UP    or AXIS_HAT_Y
        }
        
        for( int x = 2; x < 5; x++ )
        {
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Version", "1.00" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "plugged", "False" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "plugin", "2" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "device", "-2" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "mouse", "False" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "DPad R", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "DPad L", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "DPad D", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "DPad U", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Start", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Z Trig", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "B Button", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "A Button", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "C Button R", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "C Button L", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "C Button D", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "C Button U", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "R Trig", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "L Trig", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Mempak switch", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Rumblepak switch", "key(0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "X Axis", "key(0,0)" );
            Globals.mupen64plus_cfg.put( "Input-SDL-Control" + x, "Y Axis", "key(0,0)" );
        }
        
        Globals.gles2n64_conf = new ConfigFile( Globals.paths.gles2n64_conf );
        Globals.gles2n64_conf.put( "[<sectionless!>]", "enable fog", "0" );
        Globals.gles2n64_conf.put( "[<sectionless!>]", "enable alpha test", "1" );
        Globals.gles2n64_conf.put( "[<sectionless!>]", "force screen clear", "0" );
        Globals.gles2n64_conf.put( "[<sectionless!>]", "hack z", "0" );

        if( !Globals.paths.isSdCardAccessible() )
        {
           Log.e( "Updater", "SD Card not accessable in method restoreDefaults" );
           return false;
        }
        
        Globals.mupen64plus_cfg.save();
        Globals.gles2n64_conf.save();

        // TODO: Reset preferences to default values (firstRun,updatedV19,redrawAll,octagon,fps,gamepadenabled,rgba8888,volkeys).

        return checkFirstRun( instance );
    }

    private static boolean setTouchscreenLayout( Activity instance )
    {
        // TODO: Select appropriate touchscreen layout based on device size.
        return true;
    }
}
