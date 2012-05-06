/*
Simple DirectMedia Layer
Java source code (C) 2009-2011 Sergii Pylypenko
  
This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.

Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
  
1. The origin of this software must not be misrepresented; you must not
   claim that you wrote the original software. If you use this software
   in a product, an acknowledgment in the product documentation would be
   appreciated but is not required. 
2. Altered source versions must be plainly marked as such, and must not be
   misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/

// Portions of this code were taken from from Pelya's Android SDL port.
// THIS IS NOT THE ORIGINAL SOURCE, IT HAS BEEN ALTERED TO FIT THIS APP
// (05SEP2011, http://www.paulscode.com)

package paulscode.android.mupen64plus;

import java.util.Locale;

import android.app.Activity;
import android.content.res.Configuration;

class Globals
{
    public static String PackageName = "paulscode.android.mupen64plus";
    public static String StorageDir = "/mnt/sdcard";
    public static String DataDir = StorageDir + "/Android/data/" + PackageName;
    public static String LibsDir = "/data/data/" + PackageName;
    public static boolean DataDirChecked = false;   // sdcard could be at "/sdcard"

    public static String DataDownloadUrl = "Data size is 1.0 Mb|mupen64plus_data.zip";
    public static boolean DownloadToSdcard = true;

    public static String errorMessage = null;

    public static boolean InhibitSuspend = true;

    public static boolean volumeKeysDisabled = false;
    public static boolean screen_stretch = false;
    public static boolean auto_frameskip = true;
    public static int max_frameskip = 2;
    public static boolean auto_save = true;

    public static Locale locale = null;
    public static Locale locale_default = null;

    // True if app is running on XperiaPlay:
    public static boolean isXperiaPlay = false;

    // Global analog input settings:
    public static boolean analog_100_64 = true; // IMEs where keycode * 100 + (0 --> 64)
    public static int[][] ctrlr = new int[4][4];

    public static String chosenROM = null;
    public static String extraArgs = null;

    // paulscode, added for the status bar icon:
    public static final int NOTIFICATION_ID = 10001;
    // paulscode, added for the credits dialog:
    public static final int ABOUT_ID = 20002;
    // paulscode, added for the "Are you sure?" dialog:
    public static final int SURE_ID = 30003;
    // paulscode, added for the cheat notice dialog:
    public static final int CHEAT_N_ID = 40004;

    //// pauscode, added for different configurations based on hardware
    // (part of the missing shadows and stars bug fix)
    // Must match the #define's in OpenGL.cpp!
    public static final int HARDWARE_TYPE_UNKNOWN     = 0;
    public static final int HARDWARE_TYPE_OMAP        = 1;
    public static final int HARDWARE_TYPE_QUALCOMM    = 2;
    public static final int HARDWARE_TYPE_IMAP        = 3;
    public static final int HARDWARE_TYPE_TEGRA2      = 4;
    public static int hardwareType = HARDWARE_TYPE_UNKNOWN;
    ////

    public static void checkLocale( Activity a )
    {
        if( locale == null )
            return;

        Configuration config = a.getBaseContext().getResources().getConfiguration();
        if( !config.locale.equals( locale ) )
        {  // Change to the new locale.  Everything will need to be closed or reloaded.
            config.locale = locale;
            a.getBaseContext().getResources().updateConfiguration( config, null );
        }
    }
}
