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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.opengl.EGL14;
import android.opengl.GLES10;
import android.os.Build;

import androidx.core.content.pm.PackageInfoCompat;
import androidx.preference.PreferenceManager;

import android.os.Environment;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;

import java.util.Locale;

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
@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
public class AppData
{
    public enum VideoPlugin {
        DUMMY("dummy"),
        GLIDE64MK2("mupen64plus-video-glide64mk2"),
        GLIDE64MK2_EGL("mupen64plus-video-glide64mk2-egl"),
        GLIDEN64("mupen64plus-video-GLideN64"),
        RICE("mupen64plus-video-rice"),
        GLN64("mupen64plus-video-gln64"),
        ANGRYLION("mupen64plus-video-angrylion-plus"),
        PARALLEL("mupen64plus-video-parallel");

        String mPlugingLib;

        VideoPlugin(String plugingLib)
        {
            mPlugingLib = plugingLib;
        }

        public String getPluginLib()
        {
            return mPlugingLib;
        }

        static VideoPlugin getPlugin(String pluginText)
        {
            if (pluginText.toLowerCase().contains("gliden64"))
            {
                return GLIDEN64;
            }
            else if (pluginText.toLowerCase().contains("glide64mk2") && AppData.doesSupportFullGL())
            {
                return GLIDE64MK2_EGL;
            }
            else if (pluginText.toLowerCase().contains("glide64mk2"))
            {
                return GLIDE64MK2;
            }
            else if (pluginText.toLowerCase().contains("rice"))
            {
                return RICE;
            }
            else if (pluginText.toLowerCase().contains("gln64"))
            {
                return GLN64;
            }
            else if (pluginText.toLowerCase().contains("angrylion"))
            {
                return ANGRYLION;
            }
            else if (pluginText.toLowerCase().contains("parallel"))
            {
                return PARALLEL;
            }
            else if (TextUtils.isEmpty(pluginText) || pluginText.toLowerCase().contains("dummy"))
            {
                return DUMMY;
            }
            else
            {
                return GLIDE64MK2;
            }
        }
    }

    public enum AudioPlugin {
        DUMMY("dummy"),
        ANDROID("mupen64plus-audio-android"),
        ANDROID_FP("mupen64plus-audio-android-fp");

        String mPlugingLib;

        AudioPlugin(String plugingLib)
        {
            mPlugingLib = plugingLib;
        }

        public String getPluginLib()
        {
            return mPlugingLib;
        }

        static AudioPlugin getPlugin(GlobalPrefs prefs)
        {
            if (prefs.audioVolume == 0)
            {
                return DUMMY;
            }
            else if (prefs.audioFloatingPoint)
            {
                return ANDROID_FP;
            }
            else
            {
                return ANDROID;
            }
        }
    }

    public enum InputPlugin {
        DUMMY("dummy"),
        ANDROID("mupen64plus-input-android"),
        RAPHNET("mupen64plus-input-raphnet");

        String mPlugingLib;

        InputPlugin(String plugingLib)
        {
            mPlugingLib = plugingLib;
        }

        public String getPluginLib()
        {
            return mPlugingLib;
        }
    }

    public enum RspPlugin {
        DUMMY("dummy"),
        HLE("mupen64plus-rsp-hle"),
        PARALLEL("mupen64plus-rsp-parallel"),
        CXD4_HLE("mupen64plus-rsp-cxd4"),
        CXD4_LLE("mupen64plus-rsp-cxd4");

        String mPlugingLib;

        RspPlugin(String plugingLib)
        {
            mPlugingLib = plugingLib;
        }

        public String getPluginLib()
        {
            return mPlugingLib;
        }

        static RspPlugin getPlugin(String pluginText)
        {
            switch (pluginText) {
                case "rsp-hle":
                    return RspPlugin.HLE;
                case "rsp-parallel":
                    return RspPlugin.PARALLEL;
                case "rsp-cxd4-hle":
                    return RspPlugin.CXD4_HLE;
                default:
                    return RspPlugin.CXD4_LLE;
            }
        }

        public boolean isHle()
        {
            return name().toLowerCase().contains("hle");
        }
    }

    /** True if device is running marshmallow or later (24 - Android 7.0.x) */
    public static final boolean IS_NOUGAT = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;

    /** True if device is running marshmallow or later (26 - Android 8.0.x) */
    public static final boolean IS_OREO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;

    public static final String CORE_WORKING_DIR_NAME = "WorkingPath";
    
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
    
    /** The directory containing all touchscreen skin folders.  */
    public final String touchscreenSkinsDir;
    
    /** The path of the gln64 configuration file. Deleted on uninstall, sometimes overwritten on update. */
    public final String gln64_conf;

    /** The path of the glideN64 configuration file. Deleted on uninstall, sometimes overwritten on update. */
    public final String glideN64_conf;

    /** Test ROM built from sources: https://github.com/mupen64plus/mupen64plus-rom/ */
    public final String mupen64plus_test_rom_v64;
    
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
    public final boolean isAndroidTv;

    /** True if the legacy file browser should be used */
    public final boolean useLegacyFileBrowser;
    
    /** The object used to persist the settings. */
    private final SharedPreferences mPreferences;

    /** The parent directory containing all user-writable data files. */
    final String legacyUserDataDir;

    /** The parent directory containing all user-writable data files. */
    public final String gameDataDir;

    /** The parent directory containing all user-writable data files. */
    public final String legacyGameDataDir;

    /** Contex */
    private final Context mContext;

    public static final String applicationPath = "mupen64plus";

    /** Default legacy data path, needed for moving legacy data to internal storage */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public final String legacyDefaultDataPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + applicationPath;

    private static String openGlVersion = null;
    private static String openGlRenderer = null;
    private static Boolean isAngleRenderer = null;

    public final String manufacturer;
    
    // Shared preferences keys
    private static final String KEY_FORCE_ASSET_CHECK = "assetCheck";
    private static final String KEY_APP_VERSION = "appVersion";
    private static final String CHANNEL_ID = "CHANNEL_ID";
    // ... add more as needed
    
    /**
     * Instantiates a new object to retrieve and persist app data.
     * 
     * @param context The application context.
     */
    @SuppressWarnings({"unused","deprecation", "RedundantSuppression"})
    public AppData( Context context )
    {
        mContext = context;
        hardwareInfo = new HardwareInfo();
        final String packageName = context.getPackageName();

        // Preference object for persisting app data
        mPreferences = PreferenceManager.getDefaultSharedPreferences( context );
        
        PackageInfo info;
        String version = "";
        long versionCode = -1;
        try
        {
            info = context.getPackageManager().getPackageInfo( packageName, 0 );
            version = info.versionName;
            try {
                versionCode = PackageInfoCompat.getLongVersionCode(info);
            } catch (java.lang.NoSuchMethodError e) {
                versionCode = info.versionCode;
            }
        }
        catch( NameNotFoundException e )
        {
            Log.e( "AppData", e.toString() );
        }

        appVersion = version;
        appVersionCode = (int)(versionCode & 0xffff);
        
        // Directories

        //App data
        legacyUserDataDir = mPreferences.getString( "pathAppData", legacyDefaultDataPath );

        //Game data
        String tempGameDataDir = mPreferences.getString( "pathGameSaves", legacyDefaultDataPath );
        legacyGameDataDir = tempGameDataDir + "/GameData";
        gameDataDir = context.getFilesDir().getAbsolutePath() + "/GameData";

        coreSharedDataDir = context.getFilesDir().getAbsolutePath();
        touchscreenSkinsDir = "mupen64plus_data/skins/touchscreen/";

        gln64_conf = coreSharedDataDir + "/gln64.conf";
        glide64mk2_ini = coreSharedDataDir + "/Glide64mk2.ini";
        glideN64_conf = "mupen64plus_data/GLideN64.custom.ini";
        mupen64plus_test_rom_v64 = "mupen64plus_data/m64p_test_rom.v64";
        mupencheat_default = coreSharedDataDir + "/mupencheat.default";
        mupencheat_txt = coreSharedDataDir + "/mupencheat.txt";
        mupen64plus_ini = coreSharedDataDir + "/mupen64plus.ini";
        controllerProfiles_cfg = "mupen64plus_data/profiles/controller.cfg";
        touchscreenProfiles_cfg = "mupen64plus_data/profiles/touchscreen.cfg";
        emulationProfiles_cfg = "mupen64plus_data/profiles/emulation.cfg";
        
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);

        isAndroidTv = uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        useLegacyFileBrowser = ((isAndroidTv && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) || Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1 ||
                intent.resolveActivity(context.getPackageManager()) == null);

        manufacturer = android.os.Build.MANUFACTURER;
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


    /**
     * Persists the channel ID.
     *
     * @param value The asset version.
     */
    public void putChannelId( long value )
    {
        putLong( CHANNEL_ID, value );
    }
    /**
     * Gets the asset version.
     *
     * @return The channel Id.
     */
    public long getChannelId()
    {
        return getLong( CHANNEL_ID, -1 );
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

    private long getLong( String key, long defaultValue )
    {
        return mPreferences.getLong( key, defaultValue );
    }

    private void putLong( String key, long value )
    {
        mPreferences.edit().putLong( key, value ).apply();
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
            mEmulationProfilesConfig = new ConfigFile(mContext, emulationProfiles_cfg);
        }

        return mEmulationProfilesConfig;
    }
    
    public ConfigFile GetTouchscreenProfilesConfig()
    {
        if(mTouchscreenProfilesConfig == null)
        {
            mTouchscreenProfilesConfig = new ConfigFile(mContext, touchscreenProfiles_cfg);
        }

        return mTouchscreenProfilesConfig;
    }
    
    public ConfigFile GetControllerProfilesConfig()
    {
        if(mControllerProfilesConfig == null)
        {
            mControllerProfilesConfig = new ConfigFile(mContext, controllerProfiles_cfg);
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

    public static String getOpenGlEsRenderer()
    {
        if(openGlRenderer == null)
        {
            PixelBuffer buffer = new PixelBuffer(320,240);
            openGlRenderer = buffer.getGLRenderer();
            buffer.destroyGlContext();
            Log.i("AppData", "GL Renderer = " + openGlRenderer);
        }

        return openGlRenderer;
    }

    public static boolean doesSupportFullGL()
    {
        // Files
        boolean supportsFullGl = EGL14.eglBindAPI(EGL14.EGL_OPENGL_API);

        //Return back to the original after we determine that full GL is supported
        EGL14.eglBindAPI(EGL14.EGL_OPENGL_ES_API);
        return supportsFullGl;
    }

    public static boolean isAngleRenderer()
    {
        if (isAngleRenderer == null) {
            PixelBuffer buffer = new PixelBuffer(320,240);
            String versionString = buffer.getGLVersion();
            isAngleRenderer = versionString.contains("ANGLE");
        }

        return isAngleRenderer;
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
