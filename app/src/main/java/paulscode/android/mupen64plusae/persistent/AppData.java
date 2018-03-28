/*
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

import android.app.ActivityManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.opengl.EGL14;
import android.os.Build;
import android.support.v7.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import paulscode.android.mupen64plusae.preference.PathPreference;
import paulscode.android.mupen64plusae.util.DeviceUtil;
import paulscode.android.mupen64plusae.util.PixelBuffer;

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
@SuppressWarnings("SameParameterValue")
public class AppData
{
    /** True if device is running Lollipop or later (21 - Android 5.0.x) */
    public static final boolean IS_LOLLIPOP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    
    /** True if device is running marshmallow or later (23 - Android 6.0.x) */
    private static final boolean IS_MARSHMELLOW = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

    /** True if device is running marshmallow or later (24 - Android 7.0.x) */
    private static final boolean IS_NOUGAT = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    
    /** The hardware info, refreshed at the beginning of every session. */
    final HardwareInfo hardwareInfo;
    
    /** The app version string. */
    public final String appVersion;
    
    /** The app version code. */
    public final int appVersionCode;
    
    /**
     * The subdirectory returned from the core's ConfigGetSharedDataPath() method. Location of extra
     * plugin-specific/cheats data. Contents deleted on uninstall.
     */
    public final String coreSharedDataDir;
    
    /** The directory containing the native Mupen64Plus libraries. Contents deleted on uninstall, not accessible without root. */
    public final String libsDir;
    
    /** The directory containing all touchscreen skin folders. Contents deleted on uninstall. */
    public final String touchscreenSkinsDir;
    
    /** The path of the core library. Deleted on uninstall, not accessible without root. */
    public final String coreLib;
    
    /** The path of the input library. Deleted on uninstall, not accessible without root. */
    public final String inputLib;
    
    /** The path of the gln64 configuration file. Deleted on uninstall, sometimes overwritten on update. */
    public final String gln64_conf;

    /** The path of the glideN64 configuration file. Deleted on uninstall, sometimes overwritten on update. */
    public final String glideN64_conf;
    
    /** The path of the glide configuration file. Deleted on uninstall, sometimes overwritten on update. */
    public final String glide64mk2_ini;
    
    /** The path of the Mupen64Plus cheats file. Deleted on uninstall, sometimes overwritten on update. */
    public final String mupencheat_default;
    
    /** The path of the Mupen64Plus cheats file. Deleted on uninstall, sometimes overwritten on update. */
    public final String mupencheat_txt;
    
    /** The path of the Mupen64Plus ini file. Deleted on uninstall, sometimes overwritten on update. */
    public final String mupen64plus_ini;
    
    /** The path of the built-in controller profiles file. Deleted on uninstall, sometimes overwritten on update. */
    private final String controllerProfiles_cfg;
    
    /** The path of the built-in touchscreen profiles file. Deleted on uninstall, sometimes overwritten on update. */
    private final String touchscreenProfiles_cfg;
    
    /** The path of the built-in emulation profiles file. Deleted on uninstall, sometimes overwritten on update. */
    private final String emulationProfiles_cfg;
    
    /** The controller profiles config */
    private ConfigFile mControllerProfilesConfig = null;
    
    /** The touchscreen profiles config */
    private ConfigFile mTouchscreenProfilesConfig = null;
    
    /** The emulation profiles config */
    private ConfigFile mEmulationProfilesConfig = null;
    
    /** True if this is android TV hardware */
    final boolean isAndroidTv;
    
    /** The object used to persist the settings. */
    private final SharedPreferences mPreferences;

    /** The parent directory containing all user-writable data files. */
    final String userDataDir;

    /** The parent directory containing all user-writable data files. */
    final String gameDataDir;

    private static String openGlVersion = null;
    
    // Shared preferences keys
    private static final String KEY_FORCE_ASSET_CHECK = "assetCheck";
    private static final String KEY_APP_VERSION = "appVersion";
    // ... add more as needed
    
    /**
     * Instantiates a new object to retrieve and persist app data.
     * 
     * @param context The application context.
     */
    public AppData( Context context )
    {
        hardwareInfo = new HardwareInfo();
        final String packageName = context.getPackageName();

        // Preference object for persisting app data
        mPreferences = PreferenceManager.getDefaultSharedPreferences( context );
        
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

        String defaultRelPath = context.getString(R.string.pathGameSaves_default);

        //App data
        String tempUserDataDir = mPreferences.getString( "pathAppData", "" );
        if(TextUtils.isEmpty(tempUserDataDir) || tempUserDataDir.contains(defaultRelPath))
        {
            tempUserDataDir = PathPreference.validate(defaultRelPath);
        }
        userDataDir = tempUserDataDir;

        //Game data
        String tempGameDataDir = mPreferences.getString( "pathGameSaves", "" );
        if(TextUtils.isEmpty(tempGameDataDir) || tempGameDataDir.contains(defaultRelPath))
        {
            tempGameDataDir = userDataDir;
        }
        gameDataDir = tempGameDataDir + "/GameData";

        coreSharedDataDir = context.getFilesDir().getAbsolutePath();
        String _libsDir = context.getFilesDir().getParentFile().getAbsolutePath() + "/lib/";
        if( !( new File( _libsDir ) ).exists() )
            _libsDir = context.getApplicationInfo().nativeLibraryDir;
        libsDir = _libsDir;
        touchscreenSkinsDir = coreSharedDataDir + "/skins/touchscreen/";
        String profilesDir = coreSharedDataDir + "/profiles";

        //Generate .nomedia files to prevent android from adding these to gallery apps
        File file = new File(touchscreenSkinsDir + "Outline/.nomedia");
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                   Log.w("AppData", "Unable to create file:" + file.getPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        file = new File(touchscreenSkinsDir + "Shaded/.nomedia");
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    Log.w("AppData", "Unable to create file:" + file.getPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        file = new File(touchscreenSkinsDir + "JoshaGibs/.nomedia");
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    Log.w("AppData", "Unable to create file:" + file.getPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        coreLib = libsDir + "/libmupen64plus-core.so";
        inputLib = libsDir + "/libmupen64plus-input-android.so";
        gln64_conf = coreSharedDataDir + "/gln64.conf";
        glide64mk2_ini = coreSharedDataDir + "/Glide64mk2.ini";
        glideN64_conf = coreSharedDataDir + "/GLideN64.custom.ini";
        mupencheat_default = coreSharedDataDir + "/mupencheat.default";
        mupencheat_txt = coreSharedDataDir + "/mupencheat.txt";
        mupen64plus_ini = coreSharedDataDir + "/mupen64plus.ini";
        controllerProfiles_cfg = profilesDir + "/controller.cfg";
        touchscreenProfiles_cfg = profilesDir + "/touchscreen.cfg";
        emulationProfiles_cfg = profilesDir + "/emulation.cfg";
        
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);

        isAndroidTv = uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    /**
     * Gets the asset version.
     *
     * @return The asset version.
     */
    public boolean getAssetCheckNeeded()
    {
        return getBoolean(KEY_FORCE_ASSET_CHECK, true );
    }

    /**
     * Persists the asset version.
     *
     * @param value The asset version.
     */
    public void putAssetCheckNeeded( boolean value )
    {
        putBoolean(KEY_FORCE_ASSET_CHECK, value );
    }

    /**
     * Gets the asset version.
     *
     * @return The asset version.
     */
    public int getAppVersion()
    {
        return getInt( KEY_APP_VERSION, 0 );
    }

    /**
     * Persists the asset version.
     *
     * @param value The asset version.
     */
    public void putAppVersion( int value )
    {
        putInt( KEY_APP_VERSION, value );
    }

    private int getInt( String key, int defaultValue )
    {
        return mPreferences.getInt( key, defaultValue );
    }
    
    private void putInt( String key, int value )
    {
        mPreferences.edit().putInt( key, value ).apply();
    }

    private boolean getBoolean( String key, boolean defaultValue )
    {
        return mPreferences.getBoolean( key, defaultValue );
    }

    private void putBoolean( String key, boolean value )
    {
        mPreferences.edit().putBoolean( key, value ).apply();
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
        static final int HARDWARE_TYPE_UNKNOWN = 0;
        
        /** OMAP-based hardware. */
        static final int HARDWARE_TYPE_OMAP = 1;
        
        /** OMAP-based hardware, type #2. */
        static final int HARDWARE_TYPE_OMAP_2 = 2;
        
        /** QualComm-based hardware. */
        static final int HARDWARE_TYPE_QUALCOMM = 3;
        
        /** IMAP-based hardware. */
        static final int HARDWARE_TYPE_IMAP = 4;
        
        /** Tegra-based hardware. */
        static final int HARDWARE_TYPE_TEGRA = 5;
        
        /** Default hardware type */
        private static final int DEFAULT_HARDWARE_TYPE = HARDWARE_TYPE_UNKNOWN;
        
        public final String hardware;
        final String processor;
        final String features;
        final int hardwareType;
        
        HardwareInfo()
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
                    || hardware.contains( "expresso10" ))
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
                    || hardware.contains( "mt799" )
                    || ( !TextUtils.isEmpty(features) && features.contains( "vfpv3d16" ) ) )
                hardwareType = HARDWARE_TYPE_TEGRA;
            
            else
                hardwareType = DEFAULT_HARDWARE_TYPE;
            //@formatter:on
        }
    }
    
    public ConfigFile GetEmulationProfilesConfig()
    {
        if(mEmulationProfilesConfig == null)
        {
            mEmulationProfilesConfig = new ConfigFile( emulationProfiles_cfg );
        }

        return mEmulationProfilesConfig;
    }
    
    public ConfigFile GetTouchscreenProfilesConfig()
    {
        if(mTouchscreenProfilesConfig == null)
        {
            mTouchscreenProfilesConfig = new ConfigFile( touchscreenProfiles_cfg );
        }

        return mTouchscreenProfilesConfig;
    }
    
    public ConfigFile GetControllerProfilesConfig()
    {
        if(mControllerProfilesConfig == null)
        {
            mControllerProfilesConfig = new ConfigFile( controllerProfiles_cfg );
        }

        return mControllerProfilesConfig;
    }
    
    private static int getMajorVersion(int glEsVersion) {
        return ((glEsVersion & 0xffff0000) >> 16);
    }

    private static int getMinorVersion(int glEsVersion) {
        return glEsVersion & 0xffff;
    }

    public static String getOpenGlEsVersion(Context activity)
    {
        if(openGlVersion == null)
        {
            PixelBuffer buffer = new PixelBuffer(320,240);
            String versionString = buffer.getGLVersion();
            buffer.destroyGlContext();

            int firstDot = -1;
            if(versionString != null)
            {
                Log.i("AppData", "GL Version = " + versionString);
                versionString = versionString.toLowerCase();
                firstDot = versionString.indexOf('.');
            }

            //Version string is not valid, fallback to secondary method
            if(firstDot == -1 || firstDot == 0 || firstDot == versionString.length() - 1)
            {
                final ActivityManager activityManager =
                        (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
                final ConfigurationInfo configurationInfo = activityManager != null ?
                        activityManager.getDeviceConfigurationInfo() : null;

                if (configurationInfo != null) {
                    openGlVersion =  "" + getMajorVersion(configurationInfo.reqGlEsVersion) +
                            "." + getMinorVersion(configurationInfo.reqGlEsVersion);
                } else {
                    openGlVersion = "2.0";
                }
            }
            else
            {
                String parsedString = versionString.substring(firstDot-1, firstDot + 2);
                Log.i("AppData", "GL Version = " + parsedString);
                openGlVersion = parsedString;
            }
        }

        return openGlVersion;
    }

    public static boolean doesSupportFullGL()
    {

        // Files
        String arch = System.getProperty("os.arch");

        // Check for x86, older versions of AndroidX86 report GL support, but it doesn't work
        boolean itsX86 = arch.equals( "i686" );

        boolean supportsFullGl = (itsX86 && IS_MARSHMELLOW && EGL14.eglBindAPI(EGL14.EGL_OPENGL_API)) ||
                (!itsX86 && EGL14.eglBindAPI(EGL14.EGL_OPENGL_API));

        //Return back to the original after we determine that full GL is supported
        EGL14.eglBindAPI(EGL14.EGL_OPENGL_ES_API);
        return supportsFullGl;
    }

    private boolean libraryExists( String undecoratedName )
    {
        File library = new File( libsDir + undecoratedName + ".so" );

        boolean libraryExists = library.exists();
        if (!libraryExists) {
            Log.e("AppData", "Missing library: " + library.getPath());
        }

        return libraryExists;
    }

    public boolean isValidInstallation() {
        // Installation validity
        return libraryExists("libae-exports") &&
                libraryExists("libae-imports") &&
                libraryExists("libae-vidext") &&
                libraryExists("libc++_shared") &&
                libraryExists("libfreetype") &&
                libraryExists("libmupen64plus-audio-sles-fp") &&
                libraryExists("libmupen64plus-audio-sles") &&
                libraryExists("libmupen64plus-core") &&
                libraryExists("libmupen64plus-input-android") &&
                libraryExists("libmupen64plus-rsp-cxd4") &&
                libraryExists("libmupen64plus-rsp-hle") &&
                libraryExists("libmupen64plus-ui-console") &&
                libraryExists("libmupen64plus-video-angrylion-rdp-plus") &&
                libraryExists("libmupen64plus-video-glide64mk2-egl") &&
                libraryExists("libmupen64plus-video-glide64mk2") &&
                libraryExists("libmupen64plus-video-gliden64") &&
                libraryExists("libmupen64plus-video-gln64") &&
                libraryExists("libmupen64plus-video-rice") &&
                libraryExists("libosal") &&
                libraryExists("libSDL2") &&
                libraryExists("libsoundtouch_fp") &&
                libraryExists("libsoundtouch");
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String source)
    {
        if (IS_NOUGAT) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }
}
