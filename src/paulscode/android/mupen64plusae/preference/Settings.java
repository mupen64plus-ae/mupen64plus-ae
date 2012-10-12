package paulscode.android.mupen64plusae.preference;

import paulscode.android.mupen64plusae.DataDownloader;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class Settings
{
    public static void refreshPaths( Activity mainActivity )
    {
        paths = new _Paths( mainActivity );
    }
    
    public static void refreshUser( SharedPreferences preferences )
    {
        user = new _User( preferences );
    }
    
    public static _User user;
    public static _Paths paths;    

    // Legacy config files
    public static Config mupen64plus_cfg;
    public static Config gui_cfg;
    public static Config error_log;
    
    public static String dataDownloadUrl = "Data size is 1.0 Mb|mupen64plus_data.zip";
    public static String errorMessage = null;
    public static String chosenROM = null;
    public static String extraArgs = null;
    
    public static boolean inhibitSuspend = true;
    public static boolean analog_100_64 = true; // IMEs where keycode * 100 + (0 --> 64)
    
    public static boolean volumeKeysDisabled = false;
    public static boolean auto_frameskip = true;
    public static int max_frameskip = 2;
    
    public static int[][] ctrlr = new int[4][4];
    public static final int NOTIFICATION_ID = 10001;
    public static final int SURE_ID = 30003;
    public static final int CHEAT_N_ID = 40004;
    
    // paulscode, added for different configurations based on hardware
    // (part of the missing shadows and stars bug fix)
    // Must match the #define's in OpenGL.cpp!
    public static final int HARDWARE_TYPE_UNKNOWN = 0;
    public static final int HARDWARE_TYPE_OMAP = 1;
    public static final int HARDWARE_TYPE_QUALCOMM = 2;
    public static final int HARDWARE_TYPE_IMAP = 3;
    public static final int HARDWARE_TYPE_TEGRA2 = 4;
    public static int hardwareType = HARDWARE_TYPE_UNKNOWN;
    
    public static class _Paths
    {
        public final String packageName;
        
        // Directories
        public final String storageDir;
        public final String dataDir;
        public final String libsDir;
        public final String restoreDir;
        
        // Files
        public final String mupen64plus_cfg;
        public final String gui_cfg;
        public final String error_log;
        
        // Internal settings
        private static boolean downloadToSdCard = true;

        public _Paths( Activity mainActivity )
        {
            packageName = mainActivity.getPackageName();
            storageDir = downloadToSdCard ? Environment.getExternalStorageDirectory()
                    .getAbsolutePath() : mainActivity.getFilesDir().getAbsolutePath();
            dataDir = storageDir + ( downloadToSdCard ? "/Android/data/" + packageName : "" );
            libsDir = "/data/data/" + packageName;
            restoreDir = storageDir + "/mp64p_tmp_asdf1234lkjh0987/data/save";

            mupen64plus_cfg = dataDir + "/mupen64plus.cfg";
            gui_cfg = dataDir + "/data/gui.cfg";
            error_log = dataDir + "/error.log";
            
            Log.v( "DataDir Check", "PackageName set to '" + packageName + "'" );
            Log.v( "DataDir Check", "LibsDir set to '" + libsDir + "'" );
            Log.v( "DataDir Check", "StorageDir set to '" + storageDir + "'" );
            Log.v( "DataDir Check", "DataDir set to '" + dataDir + "'" );
        }
    }
    
    public static class _User
    {
        public final boolean gamepadEnabled;
        public final String gamepadPlugin;
        // public final InputMap gamepad1;
        // public final InputMap gamepad2;
        // public final InputMap gamepad3;
        // public final InputMap gamepad4;
        
        public final boolean touchscreenEnabled;
        public final String touchscreenLayout;
        public final boolean touchscreenFrameRate;
        public final boolean touchscreenOctagonJoystick;
        public final boolean touchscreenRedrawAll;
        
        public final boolean xperiaEnabled;
        public final String xperiaLayout;
        
        public final boolean videoEnabled;
        public final String videoPlugin;
        public final boolean videoStretch;
        public final boolean videoRGBA8888;
        
        public final String audioPlugin;
        public final String rspPlugin;
        public final String corePlugin;
        
        public final String language;
        public final boolean autoSaveEnabled;
        
        private final SharedPreferences sharedPreferences;
        
        public _User( SharedPreferences preferences )
        {
            sharedPreferences = preferences;
            
            gamepadEnabled = sharedPreferences.getBoolean( "gamepadEnabled", true );
            gamepadPlugin = sharedPreferences.getString( "gamepadPlugin", "" );
            // gamepad1 =
            // gamepad2 =
            // gamepad3 =
            // gamepad4 =
            
            touchscreenEnabled = sharedPreferences.getBoolean( "touchscreenEnabled", true );
            touchscreenLayout = sharedPreferences.getString( "touchscreenLayout", "" );
            touchscreenFrameRate = sharedPreferences.getBoolean( "touchscreenFrameRate", false );
            touchscreenOctagonJoystick = sharedPreferences.getBoolean(
                    "touchscreenOctagonJoystick", true );
            touchscreenRedrawAll = sharedPreferences.getBoolean( "touchscreenRedrawAll", false );
            
            xperiaEnabled = sharedPreferences.getBoolean( "xperiaEnabled", false );
            xperiaLayout = sharedPreferences.getString( "xperiaLayout", "" );
            
            videoEnabled = sharedPreferences.getBoolean( "videoEnabled", true );
            videoPlugin = sharedPreferences.getString( "videoPlugin", "" );
            videoStretch = sharedPreferences.getBoolean( "videoStretch", false );
            videoRGBA8888 = sharedPreferences.getBoolean( "videoRGBA8888", false );
            
            audioPlugin = sharedPreferences.getString( "audioPlugin", "" );
            rspPlugin = sharedPreferences.getString( "rspPlugin", "" );
            corePlugin = sharedPreferences.getString( "corePlugin", "" );
            
            language = sharedPreferences.getString( "language", "0" );
            autoSaveEnabled = sharedPreferences.getBoolean( "autoSaveEnabled", true );
        }
    }

    public static DataDownloader downloader = null;
}
