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
package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Notifier;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.media.AudioTrack;
import android.os.Vibrator;
import android.util.Log;

/**
 * A class that consolidates all interactions with the emulator core.
 * <p/>
 * It uses a simple startup/shutdown semantic to ensure all objects are properly synchronized before
 * the core launches. This is much cleaner and safer than using public static fields (i.e. globals),
 * since client code need not know how and when to update each global object.
 * 
 * @see CoreInterfaceNative
 */
public class CoreInterface
{
    public interface OnStateCallbackListener
    {
        /**
         * Called when an emulator state/parameter has changed
         * 
         * @param paramChanged The parameter ID.
         * @param newValue The new value of the parameter.
         */
        public void onStateCallback( int paramChanged, int newValue );
    }
    
    public interface OnFpsChangedListener
    {
        /**
         * Called when the frame rate has changed.
         * 
         * @param newValue The new FPS value.
         */
        public void onFpsChanged( int newValue );
    }
    
    // Public constants
    // @formatter:off
    public static final int EMULATOR_STATE_UNKNOWN = 0;
    public static final int EMULATOR_STATE_STOPPED = 1;
    public static final int EMULATOR_STATE_RUNNING = 2;
    public static final int EMULATOR_STATE_PAUSED  = 3;
    
    public static final int M64CORE_EMU_STATE          = 1;
    public static final int M64CORE_VIDEO_MODE         = 2;
    public static final int M64CORE_SAVESTATE_SLOT     = 3;
    public static final int M64CORE_SPEED_FACTOR       = 4;
    public static final int M64CORE_SPEED_LIMITER      = 5;
    public static final int M64CORE_VIDEO_SIZE         = 6;
    public static final int M64CORE_AUDIO_VOLUME       = 7;
    public static final int M64CORE_AUDIO_MUTE         = 8;
    public static final int M64CORE_INPUT_GAMESHARK    = 9;
    public static final int M64CORE_STATE_LOADCOMPLETE = 10;
    public static final int M64CORE_STATE_SAVECOMPLETE = 11;
    
    public static final int PAK_TYPE_NONE   = 1;
    public static final int PAK_TYPE_MEMORY = 2;
    public static final int PAK_TYPE_RUMBLE = 5;
    // @formatter:on
    
    // Private constants
    protected static final long VIBRATE_TIMEOUT = 1000;
    protected static final int COMMAND_CHANGE_TITLE = 1;
    
    // External objects from Java side
    protected static Activity sActivity = null;
    protected static GameSurface sSurface = null;
    protected static final Vibrator[] sVibrators = new Vibrator[4];
    protected static AppData sAppData = null;
    protected static UserPrefs sUserPrefs = null;
    protected static OnStateCallbackListener sStateCallbackListener = null;
    
    // Internal flags/caches
    protected static boolean sIsRestarting = false;
    protected static String sCheatOptions = null;
    
    // Threading objects
    protected static Thread sCoreThread;
    protected static Thread sAudioThread = null;
    protected static final Object sStateCallbackLock = new Object();
    
    // Audio objects
    protected static AudioTrack sAudioTrack = null;
    protected static Object sAudioBuffer;
    
    // Frame rate listener
    protected static OnFpsChangedListener sFpsListener;
    protected static int sFpsRecalcPeriod = 0;
    protected static int sFrameCount = -1;
    protected static long sLastFpsTime = 0;
    
    public static void refresh( Activity activity, GameSurface surface )
    {
        sActivity = activity;
        sSurface = surface;
        sAppData = new AppData( sActivity );
        sUserPrefs = new UserPrefs( sActivity );
        syncConfigFiles( sUserPrefs, sAppData );
    }
    
    @TargetApi( 11 )
    public static void registerVibrator( int player, Vibrator vibrator )
    {
        boolean isUseable = AppData.IS_HONEYCOMB ? vibrator.hasVibrator() : true;

        if( isUseable && player > 0 && player < 5 )
        {
            sVibrators[player - 1] = vibrator;
        }
    }
    
    public static void setOnStateCallbackListener( OnStateCallbackListener listener )
    {
        synchronized( sStateCallbackLock )
        {
            sStateCallbackListener = listener;
        }
    }
    
    public static void setOnFpsChangedListener( OnFpsChangedListener fpsListener, int fpsRecalcPeriod )
    {
        sFpsListener = fpsListener;
        sFpsRecalcPeriod = fpsRecalcPeriod;
    }
    
    public static void setStartupMode( String cheatArgs, boolean isRestarting )
    {
        if( cheatArgs != null && isRestarting )
            sCheatOptions = "--cheats " + cheatArgs; // Restart game with selected cheats
        else
            sCheatOptions = null;
        sIsRestarting = isRestarting;
    }
    
    public static void startupEmulator()
    {
        if( sCoreThread == null )
        {
            // Start the core thread if not already running
            sCoreThread = new Thread( new Runnable()
            {
                @Override
                public void run()
                {
                    // Initialize input-android plugin if and only if it is selected
                    // TODO: Find a more elegant solution, and be careful about lib name change
                    if( sUserPrefs.inputPlugin.name.equals( "libinput-android.so" ) )
                    {
                        CoreInterfaceNative.jniInitInput();
                        CoreInterfaceNative.setControllerConfig( 0, sUserPrefs.isPlugged1, sUserPrefs.getPakType( 1 ) );
                        CoreInterfaceNative.setControllerConfig( 1, sUserPrefs.isPlugged2, sUserPrefs.getPakType( 2 ) );
                        CoreInterfaceNative.setControllerConfig( 2, sUserPrefs.isPlugged3, sUserPrefs.getPakType( 3 ) );
                        CoreInterfaceNative.setControllerConfig( 3, sUserPrefs.isPlugged4, sUserPrefs.getPakType( 4 ) );
                    }
                    
                    CoreInterfaceNative.sdlInit();
                }
            }, "CoreThread" );
            sCoreThread.start();
            
            // Wait for the emulator to start running
            waitForEmuState( CoreInterface.EMULATOR_STATE_RUNNING );
            
            // Auto-load state and resume
            if( !sIsRestarting )
            {
                // Clear the flag so that subsequent calls don't reset
                sIsRestarting = false;
                
                Notifier.showToast( sActivity, R.string.toast_loadingSession );
                CoreInterfaceNative.emuLoadFile( sUserPrefs.selectedGameAutoSavefile );
            }
            
            resumeEmulator();
        }
    }
    
    public static void shutdownEmulator()
    {
        if( sCoreThread != null )
        {
            // Pause and auto-save state
            pauseEmulator( true );
            
            // Tell the core to quit
            CoreInterfaceNative.sdlQuit();
            
            // Now wait for the core thread to quit
            try
            {
                sCoreThread.join();
            }
            catch( InterruptedException e )
            {
                Log.i( "CoreInterface", "Problem stopping core thread: " + e );
            }
            sCoreThread = null;
        }
        
        // Clean up other resources
        CoreInterfaceNative.audioQuit();
    }
    
    public static void resumeEmulator()
    {
        if( sCoreThread != null )
        {
            CoreInterfaceNative.emuResume();
        }
    }
    
    public static void pauseEmulator( boolean autoSave )
    {
        if( sCoreThread != null )
        {
            CoreInterfaceNative.emuPause();
            
            // Auto-save in case device doesn't resume properly (e.g. OS kills process, battery dies, etc.)
            if( autoSave )
            {
                Notifier.showToast( sActivity, R.string.toast_savingSession );
                CoreInterfaceNative.emuSaveFile( sUserPrefs.selectedGameAutoSavefile );
            }
        }
    }
    
    @SuppressWarnings( "deprecation" )
    public static void onResize( int format, int width, int height )
    {
        int sdlFormat = 0x85151002; // SDL_PIXELFORMAT_RGB565 by default
        switch( format )
        {
            case PixelFormat.A_8:
                break;
            case PixelFormat.LA_88:
                break;
            case PixelFormat.L_8:
                break;
            case PixelFormat.RGBA_4444:
                sdlFormat = 0x85421002; // SDL_PIXELFORMAT_RGBA4444
                break;
            case PixelFormat.RGBA_5551:
                sdlFormat = 0x85441002; // SDL_PIXELFORMAT_RGBA5551
                break;
            case PixelFormat.RGBA_8888:
                sdlFormat = 0x86462004; // SDL_PIXELFORMAT_RGBA8888
                break;
            case PixelFormat.RGBX_8888:
                sdlFormat = 0x86262004; // SDL_PIXELFORMAT_RGBX8888
                break;
            case PixelFormat.RGB_332:
                sdlFormat = 0x84110801; // SDL_PIXELFORMAT_RGB332
                break;
            case PixelFormat.RGB_565:
                sdlFormat = 0x85151002; // SDL_PIXELFORMAT_RGB565
                break;
            case PixelFormat.RGB_888:
                // Not sure this is right, maybe SDL_PIXELFORMAT_RGB24 instead?
                sdlFormat = 0x86161804; // SDL_PIXELFORMAT_RGB888
                break;
            case PixelFormat.OPAQUE:
                /*
                 * TODO: Not sure this is right, Android API says,
                 * "System chooses an opaque format", but how do we know which one??
                 */
                break;
            default:
                Log.w( "CoreInterface", "Pixel format unknown: " + format );
                break;
        }
        CoreInterfaceNative.sdlOnResize( width, height, sdlFormat );
    }
    
    public static void waitForEmuState( final int state )
    {
        final Object lock = new Object();
        setOnStateCallbackListener( new OnStateCallbackListener()
        {
            @Override
            public void onStateCallback( int paramChanged, int newValue )
            {
                if( paramChanged == M64CORE_EMU_STATE && newValue == state )
                {
                    setOnStateCallbackListener( null );
                    synchronized( lock )
                    {
                        lock.notify();
                    }
                }
            }
        } );
        
        synchronized( lock )
        {
            try
            {
                lock.wait();
            }
            catch( InterruptedException ignored )
            {
            }
        }
    }
    
    /**
     * Populates the core configuration files with the user preferences.
     */
    private static void syncConfigFiles( UserPrefs user, AppData appData )
    {
        //@formatter:off
        
        // Core and GLES2RICE config file
        ConfigFile mupen64plus_cfg = new ConfigFile( appData.mupen64plus_cfg );
        mupen64plus_cfg.put( "Core", "Version", "1.00" );
        mupen64plus_cfg.put( "Core", "OnScreenDisplay", "False" );
        mupen64plus_cfg.put( "Core", "R4300Emulator", user.r4300Emulator );
        mupen64plus_cfg.put( "Core", "NoCompiledJump", "False" );
        mupen64plus_cfg.put( "Core", "DisableExtraMem", "False" );
        mupen64plus_cfg.put( "Core", "AutoStateSlotIncrement", "False" );
        mupen64plus_cfg.put( "Core", "EnableDebugger", "False" );
        mupen64plus_cfg.put( "Core", "CurrentStateSlot", "0" );
        mupen64plus_cfg.put( "Core", "ScreenshotPath", "\"\"" );
        mupen64plus_cfg.put( "Core", "SaveStatePath", '"' + user.slotSaveDir + '"' );
        mupen64plus_cfg.put( "Core", "SharedDataPath", "\"\"" );
    
        mupen64plus_cfg.put( "CoreEvents", "Version", "1.00" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Stop", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Fullscreen", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Save State", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Load State", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Increment Slot", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Reset", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Speed Down", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Speed Up", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Screenshot", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Pause", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Mute", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Increase Volume", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Decrease Volume", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Fast Forward", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Frame Advance", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Gameshark", "0" );
    
        mupen64plus_cfg.put( "Audio-SDL", "Version", "1.00" );
        mupen64plus_cfg.put( "Audio-SDL", "SWAP_CHANNELS", booleanToString( user.audioSwapChannels ) );
        mupen64plus_cfg.put( "Audio-SDL", "RESAMPLE", user.audioResampleAlg);
        
        mupen64plus_cfg.put( "UI-Console", "Version", "1.00" );
        mupen64plus_cfg.put( "UI-Console", "PluginDir", '"' + appData.libsDir + '"' );
        mupen64plus_cfg.put( "UI-Console", "VideoPlugin", '"' + user.videoPlugin.path + '"' );
        mupen64plus_cfg.put( "UI-Console", "AudioPlugin", '"' + user.audioPlugin.path + '"' );
        mupen64plus_cfg.put( "UI-Console", "InputPlugin", '"' + user.inputPlugin.path + '"' );
        mupen64plus_cfg.put( "UI-Console", "RspPlugin", '"' + user.rspPlugin.path + '"' );
    
        mupen64plus_cfg.put( "Video-General", "Version", "1.00" );
        if( user.videoOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                || user.videoOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT )
        {
            mupen64plus_cfg.put( "Video-General", "ScreenWidth", Integer.toString( appData.screenSize.y ) );
            mupen64plus_cfg.put( "Video-General", "ScreenHeight", Integer.toString( appData.screenSize.x ) );
        }
        else
        {
            mupen64plus_cfg.put( "Video-General", "ScreenWidth", Integer.toString( appData.screenSize.x ) );
            mupen64plus_cfg.put( "Video-General", "ScreenHeight", Integer.toString( appData.screenSize.y ) );
        }
        
        mupen64plus_cfg.put( "Video-Rice", "Version", "1.00" );
        mupen64plus_cfg.put( "Video-Rice", "SkipFrame", booleanToString( user.isGles2RiceAutoFrameskipEnabled ) );
        mupen64plus_cfg.put( "Video-Rice", "FastTextureLoading", booleanToString( user.isGles2RiceFastTextureLoadingEnabled ) );
        mupen64plus_cfg.put( "Video-Rice", "FastTextureCRC", booleanToString( user.isGles2RiceFastTextureCrcEnabled ) );
        mupen64plus_cfg.put( "Video-Rice", "LoadHiResTextures", booleanToString( user.isGles2RiceHiResTexturesEnabled ) );
        mupen64plus_cfg.put( "Video-Rice", "Mipmapping", user.gles2RiceMipmappingAlg );
        mupen64plus_cfg.put( "Video-Rice", "ScreenUpdateSetting", user.gles2RiceScreenUpdateType );
        mupen64plus_cfg.put( "Video-Rice", "TextureEnhancement", user.gles2RiceTextureEnhancement );
        mupen64plus_cfg.put( "Video-Rice", "TextureEnhancementControl", "1" );
    
        if(user.isGles2RiceForceTextureFilterEnabled)
            mupen64plus_cfg.put( "Video-Rice", "ForceTextureFilter", "2");
        else
            mupen64plus_cfg.put( "Video-Rice", "ForceTextureFilter", "0");
        
        mupen64plus_cfg.save();
        
        // GLES2N64 config file
        ConfigFile gles2n64_conf = new ConfigFile( appData.gles2n64_conf );
        gles2n64_conf.put( "[<sectionless!>]", "enable fog", booleanToString( user.isGles2N64FogEnabled ) );
        gles2n64_conf.put( "[<sectionless!>]", "enable alpha test", booleanToString( user.isGles2N64AlphaTestEnabled ) );
        gles2n64_conf.put( "[<sectionless!>]", "force screen clear", booleanToString( user.isGles2N64ScreenClearEnabled ) );
        gles2n64_conf.put( "[<sectionless!>]", "hack z", booleanToString( !user.isGles2N64DepthTestEnabled ) ); // hack z enabled means that depth test is disabled
        gles2n64_conf.save();        
        //@formatter:on
    }
    
    private static String booleanToString( boolean b )
    {
        return b ? "1" : "0";
    }
}
