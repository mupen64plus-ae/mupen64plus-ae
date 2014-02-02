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
 * Authors: Paul Lamb, littleguy77
 */
package paulscode.android.mupen64plusae.persistent;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import org.acra.ACRA;
import org.acra.ErrorReporter;

import paulscode.android.mupen64plusae.util.DeviceUtil;
import paulscode.android.mupen64plusae.util.FileUtil;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

/**
 * A convenience class for retrieving and persisting data defined internally by the application.
 * <p>
 * <b>Developers:</b> Use this class to persist small bits of application data across sessions and
 * reboots. (For large data sets, consider using databases or files.) To add a new variable to
 * persistent storage, use the following pattern:
 * 
 * <pre>
 * {@code
 * // Define keys for each variable
 * private static final String KEY_FOO = "foo";
 * private static final String KEY_BAR = "bar";
 * 
 * // Define default values for each variable
 * private static final float   DEFAULT_FOO = 3.14f;
 * private static final boolean DEFAULT_BAR = false;
 * 
 * // Create getters
 * public float getFoo()
 * {
 *     return mPreferences.getFloat( KEY_FOO, DEFAULT_FOO );
 * }
 * 
 * public boolean getBar()
 * {
 *     return mPreferences.getBoolean( KEY_BAR, DEFAULT_BAR );
 * }
 * 
 * // Create setters
 * public void setFoo( float value )
 * {
 *     mPreferences.edit().putFloat( KEY_FOO, value ).commit();
 * }
 * 
 * public void setBar( boolean value )
 * {
 *     mPreferences.edit().putBoolean( KEY_BAR, value ).commit();
 * }
 * </pre>
 */
public class AppData
{
    /** True if device is running Gingerbread or later (9 - Android 2.3.x) */
    public static final boolean IS_GINGERBREAD = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    
    /** True if device is running Honeycomb or later (11 - Android 3.0.x) */
    public static final boolean IS_HONEYCOMB = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    
    /** True if device is running Honeycomb MR1 or later (12 - Android 3.1.x) */
    public static final boolean IS_HONEYCOMB_MR1 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    
    /** True if device is running Ice Cream Sandwich or later (14 - Android 4.0.x) */
    public static final boolean IS_ICE_CREAM_SANDWICH = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    
    /** True if device is running Jellybean or later (16 - Android 4.1.x) */
    public static final boolean IS_JELLY_BEAN = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    
    /** True if device is running KitKat or later (19 - Android 4.4.x) */
    public static final boolean IS_KITKAT = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    
    /** Debug option: download data to SD card (default true). */
    public static final boolean DOWNLOAD_TO_SDCARD = true;
    
    /** The hardware info, refreshed at the beginning of every session. */
    public final HardwareInfo hardwareInfo;
    
    /** The package name. */
    public final String packageName;
    
    /** The app version string. */
    public final String appVersion;
    
    /** The app version code. */
    public final int appVersionCode;
    
    /** The device's storage directory (typically the external storage directory). */
    private final String storageDir;
    
    /**
     * The subdirectory returned from the core's ConfigGetSharedDataPath() method. Location of extra
     * plugin-specific/cheats data. Contents deleted on uninstall.
     */
    public final String coreSharedDataDir;
    
    /** The directory for temporary files. Contents deleted on uninstall. */
    public final String tempDir;
    
    /** The directory containing the native Mupen64Plus libraries. Contents deleted on uninstall, not accessible without root. */
    public final String libsDir;
    
    /** The directory containing all touchscreen layout folders. Contents deleted on uninstall. */
    public final String touchscreenLayoutsDir;
    
    /** The directory containing all Xperia Play layout folders. Contents deleted on uninstall. */
    public final String touchpadLayoutsDir;
    
    /** The directory containing all fonts. Contents deleted on uninstall. */
    public final String fontsDir;
    
    /** The directory contaiing all built-in profiles. Contents deleted on uninstall. */
    public final String profilesDir;
    
    /** The path of the core library. Deleted on uninstall, not accessible without root. */
    public final String coreLib;
    
    /** The path of the RSP library. Deleted on uninstall, not accessible without root. */
    public final String rspLib;
    
    /** The path of the input library. Deleted on uninstall, not accessible without root. */
    public final String inputLib;
    
    /** The path of the gln64 configuration file. Deleted on uninstall, sometimes overwritten on update. */
    public final String gln64_conf;
    
    /** The path of the glide configuration file. Deleted on uninstall, sometimes overwritten on update. */
    public final String glide64mk2_ini;
    
    /** The path of the Mupen64Plus cheats file. Deleted on uninstall, sometimes overwritten on update. */
    public final String mupencheat_default;
    
    /** The path of the Mupen64Plus cheats file. Deleted on uninstall, sometimes overwritten on update. */
    public final String mupencheat_txt;
    
    /** The path of the Mupen64Plus ini file. Deleted on uninstall, sometimes overwritten on update. */
    public final String mupen64plus_ini;
    
    /** The path of the built-in controller profiles file. Deleted on uninstall, sometimes overwritten on update. */
    public final String controllerProfiles_cfg;
    
    /** The path of the built-in touchscreen profiles file. Deleted on uninstall, sometimes overwritten on update. */
    public final String touchscreenProfiles_cfg;
    
    /** The path of the built-in emulation profiles file. Deleted on uninstall, sometimes overwritten on update. */
    public final String emulationProfiles_cfg;
    
    /** Whether the installation is valid. */
    public final boolean isValidInstallation;
    
    /** The object used to persist the settings. */
    private final SharedPreferences mPreferences;
    
    // Shared preferences keys
    private static final String KEY_ASSET_VERSION = "assetVersion";
    private static final String KEY_LAST_APP_VERSION_CODE = "lastAppVersion";
    // ... add more as needed
    
    // Shared preferences default values
    private static final int DEFAULT_ASSET_VERSION = 0;
    private static final int DEFAULT_LAST_APP_VERSION_CODE = 0;
    
    // ... add more as needed
    
    /**
     * Instantiates a new object to retrieve and persist app data.
     * 
     * @param context The application context.
     */
    public AppData( Context context )
    {
        hardwareInfo = new HardwareInfo();
        packageName = context.getPackageName();
        
        PackageInfo info;
        String version = "";
        int versionCode = -1;
        try
        {
            info = context.getPackageManager().getPackageInfo( packageName, 0 );
            version = info.versionName;
            versionCode = info.versionCode;
        }
        catch( NameNotFoundException e )
        {
            Log.e( "AppData", e.getMessage() );
        }
        appVersion = version;
        appVersionCode = versionCode;
        
        // Directories
        if( DOWNLOAD_TO_SDCARD )
        {
            storageDir = Environment.getExternalStorageDirectory().getAbsolutePath();
            coreSharedDataDir = storageDir + "/Android/data/" + packageName;
        }
        else
        {
            storageDir = context.getFilesDir().getAbsolutePath();
            coreSharedDataDir = storageDir;
        }
        tempDir = coreSharedDataDir + "/tmp";
        libsDir = context.getFilesDir().getParentFile().getAbsolutePath() + "/lib/";
        touchscreenLayoutsDir = coreSharedDataDir + "/skins/touchscreens/";
        touchpadLayoutsDir = coreSharedDataDir + "/skins/touchpads/";
        fontsDir = coreSharedDataDir + "/skins/fonts/";
        profilesDir = coreSharedDataDir + "/profiles";
        
        // Files
        coreLib = libsDir + "/libmupen64plus-core.so";
        rspLib = libsDir + "/libmupen64plus-rsp-hle.so";
        inputLib = libsDir + "/libmupen64plus-input-android.so";
        gln64_conf = coreSharedDataDir + "/gln64.conf";
        glide64mk2_ini = coreSharedDataDir + "/Glide64mk2.ini";
        mupencheat_default = coreSharedDataDir + "/mupencheat.default";
        mupencheat_txt = coreSharedDataDir + "/mupencheat.txt";
        mupen64plus_ini = coreSharedDataDir + "/mupen64plus.ini";
        controllerProfiles_cfg = profilesDir + "/controller.cfg";
        touchscreenProfiles_cfg = profilesDir + "/touchscreen.cfg";
        emulationProfiles_cfg = profilesDir + "/emulation.cfg";
        
        // Installation validity
        // @formatter:off
        isValidInstallation =
                libraryExists( "ae-exports" )                   &&
                libraryExists( "ae-imports" )                   &&
                libraryExists( "mupen64plus-audio-sdl" )        &&
                libraryExists( "mupen64plus-core" )             &&
                libraryExists( "mupen64plus-input-android" )    &&
                libraryExists( "mupen64plus-rsp-hle" )          &&
                libraryExists( "mupen64plus-ui-console" )       &&
                libraryExists( "mupen64plus-video-glide64mk2" ) &&
                libraryExists( "mupen64plus-video-gln64" )      &&
                libraryExists( "mupen64plus-video-rice" )       &&
                libraryExists( "SDL2" )                         &&
                libraryExists( "xperia-touchpad" );
        // @formatter:on
        
        // Preference object for persisting app data
        String appDataFilename = packageName + "_appdata";
        mPreferences = context.getSharedPreferences( appDataFilename, Context.MODE_PRIVATE );
        
        // Get the contents of the libraries directory
        ArrayList<CharSequence> names = new ArrayList<CharSequence>();
        ArrayList<String> paths = new ArrayList<String>();
        FileUtil.populate( new File( libsDir ), false, false, true, names, paths );
        String libnames = TextUtils.join( "\n", names );

        // Record some info in the crash reporter
        ErrorReporter reporter = ACRA.getErrorReporter();
        reportMultilineText( reporter, "Libraries", libnames );
        reporter.putCustomData( "CPU Features", hardwareInfo.features );
        reporter.putCustomData( "CPU Hardware", hardwareInfo.hardware );
        reporter.putCustomData( "CPU Processor", hardwareInfo.processor );
        reportMultilineText( reporter, "Axis Report", DeviceUtil.getAxisInfo() );
        reportMultilineText( reporter, "CPU Report", DeviceUtil.getCpuInfo() );
        reportMultilineText( reporter, "HID Report", DeviceUtil.getPeripheralInfo() );
    }
    
    public static void reportMultilineText( ErrorReporter reporter, String key, String multilineText )
    {
        final String[] lines = multilineText.split( "\n" );
        
        int numLines = lines.length;
        int padding = 1;
        while( numLines > 9 )
        {
            numLines /= 10;
            padding++;
        }
        final String template = "%s.%0" + padding + "d"; 
        
        for( int i = 0; i < lines.length; i++ )
        {
            reporter.putCustomData( String.format( template, key, i ), lines[i].trim() );
        }
    }
    
    /**
     * Checks if the storage directory is accessible.
     * 
     * @return True, if the storage directory is accessible.
     */
    public boolean isSdCardAccessible()
    {
        return ( new File( storageDir ) ).exists();
    }
    
    /**
     * Gets the asset version.
     * 
     * @return The asset version.
     */
    public int getAssetVersion()
    {
        return getInt( KEY_ASSET_VERSION, DEFAULT_ASSET_VERSION );
    }
    
    /**
     * Gets the version code when the user last ran the app
     * 
     * @return The app version.
     */
    public int getLastAppVersionCode()
    {
        return getInt( KEY_LAST_APP_VERSION_CODE, DEFAULT_LAST_APP_VERSION_CODE );
    }
    
    /**
     * Persists the asset version.
     * 
     * @param value The asset version.
     */
    public void putAssetVersion( int value )
    {
        putInt( KEY_ASSET_VERSION, value );
    }
    
    /**
     * Persists the version code when the user last ran the app.
     * 
     * @param value The app version code.
     */
    public void putLastAppVersionCode( int value )
    {
        putInt( KEY_LAST_APP_VERSION_CODE, value );
    }
    
    private int getInt( String key, int defaultValue )
    {
        return mPreferences.getInt( key, defaultValue );
    }
    
    private void putInt( String key, int value )
    {
        mPreferences.edit().putInt( key, value ).commit();
    }
    
    private boolean libraryExists( String undecoratedName )
    {
        File library = new File( libsDir + "lib" + undecoratedName + ".so" );
        return library.exists();
    }
    
    /**
     * Small class that summarizes the info provided by /proc/cpuinfo.
     * <p>
     * <b>Developers:</b> Hardware types are used to apply device-specific fixes for missing shadows
     * and decals, and must match the #defines in OpenGL.cpp.
     */
    public static class HardwareInfo
    {
        /** Unknown hardware configuration. */
        public static final int HARDWARE_TYPE_UNKNOWN = 0;
        
        /** OMAP-based hardware. */
        public static final int HARDWARE_TYPE_OMAP = 1;
        
        /** OMAP-based hardware, type #2. */
        public static final int HARDWARE_TYPE_OMAP_2 = 2;
        
        /** QualComm-based hardware. */
        public static final int HARDWARE_TYPE_QUALCOMM = 3;
        
        /** IMAP-based hardware. */
        public static final int HARDWARE_TYPE_IMAP = 4;
        
        /** Tegra-based hardware. */
        public static final int HARDWARE_TYPE_TEGRA = 5;
        
        /** Default hardware type */
        private static final int DEFAULT_HARDWARE_TYPE = HARDWARE_TYPE_UNKNOWN;
        
        public final String hardware;
        public final String processor;
        public final String features;
        public final int hardwareType;
        public final boolean isXperiaPlay;
        
        public HardwareInfo()
        {
            // Identify the hardware, features, and processor strings
            {
                // Temporaries since we can't assign the final fields this way
                String _hardware = "";
                String _features = "";
                String _processor = "";
                
                // Parse a long string of information from the operating system
                String hwString = DeviceUtil.getCpuInfo().toLowerCase( Locale.US );
                String[] lines = hwString.split( "\\r\\n|\\n|\\r" );
                for( String line : lines )
                {
                    String[] splitLine = line.split( ":" );
                    if( splitLine.length == 2 )
                    {
                        String arg = splitLine[0].trim();
                        String val = splitLine[1].trim();
                        if( arg.equals( "processor" ) && val.length() > 1 )
                            _processor = val;
                        else if( arg.equals( "features" ) )
                            _features = val;
                        else if( arg.equals( "hardware" ) )
                            _hardware = val;
                    }
                }
                
                // Assign the final fields
                hardware = _hardware;
                processor = _processor;
                features = _features;
            }
            
            // Identify the hardware type from the substrings
            //@formatter:off
            if(        ( hardware.contains( "mapphone" )
                         && !processor.contains( "rev 3" ) )
                    || hardware.contains( "smdkv" )
                    || hardware.contains( "herring" )
                    || hardware.contains( "aries" )
                    || hardware.contains( "expresso10" )
                    || ( hardware.contains( "tuna" )
                         && !IS_JELLY_BEAN ) )
                hardwareType = HARDWARE_TYPE_OMAP;
            
            else if(   hardware.contains( "tuna" )
                    || hardware.contains( "mapphone" )
                    || hardware.contains( "amlogic meson3" )
                    || hardware.contains( "rk30board" )
                    || hardware.contains( "smdk4210" )
                    || hardware.contains( "riogrande" )
                    || hardware.contains( "manta" )
                    || hardware.contains( "cardhu" )
                    || hardware.contains( "mt6517" ) )
                hardwareType = HARDWARE_TYPE_OMAP_2;
            
            else if(   hardware.contains( "liberty" )
                    || hardware.contains( "gt-s5830" )
                    || hardware.contains( "zeus" ) )
                hardwareType = HARDWARE_TYPE_QUALCOMM;
            
            else if(   hardware.contains( "imap" ) )
                hardwareType = HARDWARE_TYPE_IMAP;
            
            else if(   hardware.contains( "tegra 2" )
                    || hardware.contains( "grouper" )
                    || hardware.contains( "meson-m1" )
                    || hardware.contains( "smdkc" )
                    || hardware.contains( "smdk4x12" )
                    || hardware.contains( "sun6i" )
                    || ( features != null && features.contains( "vfpv3d16" ) ) )
                hardwareType = HARDWARE_TYPE_TEGRA;
            
            else
                hardwareType = DEFAULT_HARDWARE_TYPE;
            //@formatter:on
            
            // Identify whether this is an Xperia PLAY
            isXperiaPlay = hardware.contains( "zeus" );
        }
    }
}
