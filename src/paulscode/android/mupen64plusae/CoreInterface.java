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

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import paulscode.android.mupen64plusae.jni.NativeConstants;
import paulscode.android.mupen64plusae.jni.NativeExports;
import paulscode.android.mupen64plusae.jni.NativeImports;
import paulscode.android.mupen64plusae.jni.NativeInput;
import paulscode.android.mupen64plusae.jni.NativeSDL;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.ErrorLogger;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.util.Prompt.PromptFileListener;
import paulscode.android.mupen64plusae.util.Prompt.PromptIntegerListener;
import paulscode.android.mupen64plusae.util.Prompt.PromptTextListener;
import paulscode.android.mupen64plusae.util.Utility;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.media.AudioTrack;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;

/**
 * A class that consolidates all interactions with the emulator core.
 * <p/>
 * It uses a simple startup/shutdown semantic to ensure all objects are properly synchronized before
 * the core launches. This is much cleaner and safer than using public static fields (i.e. globals),
 * since client code need not know how and when to update each global object.
 * 
 * @see NativeConstants
 * @see NativeExports
 * @see NativeImports
 * @see NativeInput
 * @see NativeSDL
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
    
    // External objects from Java side
    protected static Activity sActivity = null;
    protected static GameSurface sSurface = null;
    protected static final Vibrator[] sVibrators = new Vibrator[4];
    protected static AppData sAppData = null;
    protected static UserPrefs sUserPrefs = null;
    protected static final ArrayList<OnStateCallbackListener> sStateCallbackListeners = new ArrayList<OnStateCallbackListener>();
    
    // Internal flags/caches
    protected static boolean sIsRestarting = false;
    protected static String sCheatOptions = null;
    
    // Threading objects
    protected static Thread sCoreThread;
    protected static final Object sStateCallbackLock = new Object();
    
    // Audio objects
    protected static AudioTrack sAudioTrack = null;
    
    // Frame rate listener
    protected static OnFpsChangedListener sFpsListener;
    protected static int sFpsRecalcPeriod = 0;
    protected static int sFrameCount = -1;
    protected static long sLastFpsTime = 0;
    
    // Speed info
    private static final int BASELINE_SPEED = 100;
    private static final int DEFAULT_SPEED = 250;
    private static final int MAX_SPEED = 300;
    private static final int MIN_SPEED = 10;
    private static final int DELTA_SPEED = 10;
    private static boolean sUseCustomSpeed = false;
    private static int sCustomSpeed = DEFAULT_SPEED;
    
    // Slot info
    private static final int NUM_SLOTS = 10;
    
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
        boolean hasVibrator = AppData.IS_HONEYCOMB ? vibrator.hasVibrator() : true;
        
        if( sAppData.hasVibratePermission && hasVibrator && player > 0 && player < 5 )
        {
            sVibrators[player - 1] = vibrator;
        }
    }
    
    public static void addOnStateCallbackListener( OnStateCallbackListener listener )
    {
        synchronized( sStateCallbackLock )
        {
            // Do not allow multiple instances, in case listeners want to remove themselves
            if( !sStateCallbackListeners.contains( listener ) )
                sStateCallbackListeners.add( listener );
        }
    }
    
    public static void removeOnStateCallbackListener( OnStateCallbackListener listener )
    {
        synchronized( sStateCallbackLock )
        {
            sStateCallbackListeners.remove( listener );
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
            sCheatOptions = cheatArgs; // Restart game with selected cheats
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
                    // Initialize input-android plugin (even if we aren't going to use it)
                    NativeInput.init();
                    NativeInput.setConfig( 0, sUserPrefs.isPlugged1, sUserPrefs.getPakType( 1 ) );
                    NativeInput.setConfig( 1, sUserPrefs.isPlugged2, sUserPrefs.getPakType( 2 ) );
                    NativeInput.setConfig( 2, sUserPrefs.isPlugged3, sUserPrefs.getPakType( 3 ) );
                    NativeInput.setConfig( 3, sUserPrefs.isPlugged4, sUserPrefs.getPakType( 4 ) );
                    
                    ArrayList<String> arglist = new ArrayList<String>();
                    arglist.add( "mupen64plus" );
                    arglist.add( "--corelib" );
                    arglist.add( sAppData.coreLib );
                    arglist.add( "--configdir" );
                    arglist.add( sAppData.dataDir );
                    if( !sUserPrefs.isFramelimiterEnabled )
                    {
                        arglist.add( "--nospeedlimit" );
                    }
                    if( sCheatOptions != null )
                    {
                        arglist.add( "--cheats" );
                        arglist.add( sCheatOptions );
                    }
                    arglist.add( getROMPath() );
                    NativeExports.emuStart( sAppData.libsDir, sAppData.dataDir, arglist.toArray() );
                }
            }, "CoreThread" );
            sCoreThread.start();
            
            // Auto-load state if desired
            if( !sIsRestarting )
            {
                Notifier.showToast( sActivity, R.string.toast_loadingSession );
                addOnStateCallbackListener( new OnStateCallbackListener()
                {
                    @Override
                    public void onStateCallback( int paramChanged, int newValue )
                    {
                        if( paramChanged == NativeConstants.M64CORE_EMU_STATE
                                && newValue == NativeConstants.EMULATOR_STATE_RUNNING )
                        {
                            removeOnStateCallbackListener( this );
                            NativeExports.emuLoadFile( sUserPrefs.selectedGameAutoSavefile );
                        }
                    }
                } );
            }
            
            // Ensure the auto-save is loaded if the operating system stops & restarts the activity
            sIsRestarting = false;
        }
    }
    
    public static void shutdownEmulator()
    {
        if( sCoreThread != null )
        {
            // Tell the core to quit
            NativeExports.emuStop();
            
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
        NativeSDL.audioQuit();
    }
    
    public static void resumeEmulator()
    {
        if( sCoreThread != null )
        {
            NativeExports.emuResume();
        }
    }
    
    public static void pauseEmulator( boolean autoSave )
    {
        if( sCoreThread != null )
        {
            NativeExports.emuPause();
            
            // Auto-save in case device doesn't resume properly (e.g. OS kills process, battery dies, etc.)
            if( autoSave )
            {
                Notifier.showToast( sActivity, R.string.toast_savingSession );
                NativeExports.emuSaveFile( sUserPrefs.selectedGameAutoSavefile );
            }
        }
    }
    
    public static void togglePause()
    {
        int state = NativeExports.emuGetState();
        if( state == NativeConstants.EMULATOR_STATE_PAUSED )
            NativeExports.emuResume();
        else if( state == NativeConstants.EMULATOR_STATE_RUNNING )
            NativeExports.emuPause();
    }
    
    public static void setSlot( int value )
    {
        int slot = value % NUM_SLOTS;
        NativeExports.emuSetSlot( slot );
        Notifier.showToast( sActivity, R.string.toast_usingSlot, slot );
    }
    
    public static void incrementSlot()
    {
        int slot = NativeExports.emuGetSlot();
        setSlot( slot + 1 );
    }
    
    public static void saveSlot()
    {
        int slot = NativeExports.emuGetSlot();
        Notifier.showToast( sActivity, R.string.toast_savingSlot, slot );
        NativeExports.emuSaveSlot();
    }
    
    public static void loadSlot()
    {
        int slot = NativeExports.emuGetSlot();
        Notifier.showToast( sActivity, R.string.toast_loadingSlot, slot );
        NativeExports.emuLoadSlot();
    }
    
    public static void saveFileFromPrompt()
    {
        CoreInterface.pauseEmulator( false );
        CharSequence title = sActivity.getText( R.string.menuItem_fileSave );
        CharSequence hint = sActivity.getText( R.string.hintFileSave );
        int inputType = InputType.TYPE_CLASS_TEXT;
        Prompt.promptText( sActivity, title, null, hint, inputType, new PromptTextListener()
        {
            @Override
            public void onDialogClosed( CharSequence text, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                    saveState( text.toString() );
                CoreInterface.resumeEmulator();
            }
        } );
    }
    
    public static void loadFileFromPrompt()
    {
        CoreInterface.pauseEmulator( false );
        CharSequence title = sActivity.getText( R.string.menuItem_fileLoad );
        File startPath = new File( sUserPrefs.manualSaveDir );
        Prompt.promptFile( sActivity, title, null, startPath, new PromptFileListener()
        {
            @Override
            public void onDialogClosed( File file, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                    loadState( file );
                CoreInterface.resumeEmulator();
            }
        } );
    }
    
    public static void saveState( final String filename )
    {
        final File file = new File( sUserPrefs.manualSaveDir + "/" + filename );
        if( file.exists() )
        {
            String title = sActivity.getString( R.string.confirm_title );
            String message = sActivity.getString( R.string.confirmOverwriteFile_message, filename );
            Prompt.promptConfirm( sActivity, title, message, new PromptConfirmListener()
            {
                @Override
                public void onConfirm()
                {
                    Notifier.showToast( sActivity, R.string.toast_overwritingFile, file.getName() );
                    NativeExports.emuSaveFile( file.getAbsolutePath() );
                }
            } );
        }
        else
        {
            Notifier.showToast( sActivity, R.string.toast_savingFile, file.getName() );
            NativeExports.emuSaveFile( file.getAbsolutePath() );
        }
    }
    
    public static void loadState( File file )
    {
        Notifier.showToast( sActivity, R.string.toast_loadingFile, file.getName() );
        NativeExports.emuLoadFile( file.getAbsolutePath() );
    }
    
    public static void setCustomSpeedFromPrompt()
    {
        NativeExports.emuPause();
        final CharSequence title = sActivity.getText( R.string.menuItem_setSpeed );
        Prompt.promptInteger( sActivity, title, "%1$d %%", sCustomSpeed, MIN_SPEED, MAX_SPEED,
                new PromptIntegerListener()
                {
                    @Override
                    public void onDialogClosed( Integer value, int which )
                    {
                        if( which == DialogInterface.BUTTON_POSITIVE )
                        {
                            setCustomSpeed( value );
                        }
                        NativeExports.emuResume();
                    }
                } );
    }
    
    public static void incrementCustomSpeed()
    {
        setCustomSpeed( sCustomSpeed + DELTA_SPEED );
    }
    
    public static void decrementCustomSpeed()
    {
        setCustomSpeed( sCustomSpeed - DELTA_SPEED );
    }
    
    public static void setCustomSpeed( int value )
    {
        sCustomSpeed = Utility.clamp( value, MIN_SPEED, MAX_SPEED );
        sUseCustomSpeed = true;
        NativeExports.emuSetSpeed( sCustomSpeed );
    }
    
    public static void toggleSpeed()
    {
        sUseCustomSpeed = !sUseCustomSpeed;
        int speed = sUseCustomSpeed ? sCustomSpeed : BASELINE_SPEED;
        NativeExports.emuSetSpeed( speed );
    }
    
    public static void fastForward( boolean pressed )
    {
        int speed = pressed ? sCustomSpeed : BASELINE_SPEED;
        NativeExports.emuSetSpeed( speed );
    }
    
    public static void advanceFrame()
    {
        NativeExports.emuPause();
        NativeExports.emuAdvanceFrame();
    }
    
    /**
     * Populates the core configuration files with the user preferences.
     */
    private static void syncConfigFiles( UserPrefs user, AppData appData )
    {
        //@formatter:off
        final String EMPTY = "\"\"";
        
        // GLES2N64 config file
        ConfigFile gles2n64_conf = new ConfigFile( appData.gles2n64_conf );
        gles2n64_conf.put( "[<sectionless!>]", "window width", String.valueOf( user.videoRenderWidth ) );
        gles2n64_conf.put( "[<sectionless!>]", "window height", String.valueOf( user.videoRenderHeight ) );
        gles2n64_conf.put( "[<sectionless!>]", "auto frameskip", boolToNum( user.isGles2N64AutoFrameskipEnabled ) );
        gles2n64_conf.put( "[<sectionless!>]", "max frameskip", String.valueOf( user.gles2N64MaxFrameskip ) );
        gles2n64_conf.put( "[<sectionless!>]", "enable fog", boolToNum( user.isGles2N64FogEnabled ) );
        gles2n64_conf.put( "[<sectionless!>]", "enable alpha test", boolToNum( user.isGles2N64AlphaTestEnabled ) );
        gles2n64_conf.put( "[<sectionless!>]", "force screen clear", boolToNum( user.isGles2N64ScreenClearEnabled ) );
        gles2n64_conf.put( "[<sectionless!>]", "hack z", boolToNum( !user.isGles2N64DepthTestEnabled ) );                   // Hack z enabled means that depth test is disabled

        // GLES2GLIDE64 config file
        ConfigFile gles2glide64_conf = new ConfigFile( appData.gles2glide64_conf );
        gles2glide64_conf.put( "DEFAULT", "aspect", "2" );                                                                  // Stretch to GameSurface, Java will manage aspect ratio
        gles2glide64_conf.put( "DEFAULT", "autoframeskip", boolToNum( user.isGles2Glide64AutoFrameskipEnabled ) );
        gles2glide64_conf.put( "DEFAULT", "maxframeskip", String.valueOf( user.gles2Glide64MaxFrameskip ) );
        
        // Core and GLES2RICE config file
        ConfigFile mupen64plus_cfg = new ConfigFile( appData.mupen64plus_cfg );
        
        mupen64plus_cfg.put( "Audio-SDL", "Version", "1.000000" );                                                          // Mupen64Plus SDL Audio Plugin config parameter version number
        mupen64plus_cfg.put( "Audio-SDL", "DEFAULT_FREQUENCY", "33600" );                                                   // Frequency which is used if rom doesn't want to change it
        mupen64plus_cfg.put( "Audio-SDL", "SWAP_CHANNELS", boolToTF( user.audioSwapChannels ) );                            // Swaps left and right channels
        mupen64plus_cfg.put( "Audio-SDL", "PRIMARY_BUFFER_SIZE", "16384" );                                                 // Size of primary buffer in output samples. This is where audio is loaded after it's extracted from n64's memory.
        mupen64plus_cfg.put( "Audio-SDL", "PRIMARY_BUFFER_TARGET", "10240" );                                               // Fullness level target for Primary audio buffer, in equivalent output samples
        mupen64plus_cfg.put( "Audio-SDL", "SECONDARY_BUFFER_SIZE", "2048" );                                                // Size of secondary buffer in output samples. This is SDL's hardware buffer.
        mupen64plus_cfg.put( "Audio-SDL", "RESAMPLE", '"' + user.audioResampleAlg + '"' );                                  // Audio resampling algorithm. src-sinc-best-quality, src-sinc-medium-quality, src-sinc-fastest, src-zero-order-hold, src-linear, speex-fixed-{10-0}, trivial
        mupen64plus_cfg.put( "Audio-SDL", "VOLUME_CONTROL_TYPE", "1" );                                                     // Volume control type: 1 = SDL (only affects Mupen64Plus output)  2 = OSS mixer (adjusts master PC volume)
        mupen64plus_cfg.put( "Audio-SDL", "VOLUME_ADJUST", "5" );                                                           // Percentage change each time the volume is increased or decreased
        mupen64plus_cfg.put( "Audio-SDL", "VOLUME_DEFAULT", "80" );                                                         // Default volume when a game is started.  Only used if VOLUME_CONTROL_TYPE is 1
        
        mupen64plus_cfg.put( "Core", "Version", "1.010000" );                                                               // Mupen64Plus Core config parameter set version number.  Please don't change this version number.
        mupen64plus_cfg.put( "Core", "OnScreenDisplay", "False" );                                                          // Draw on-screen display if True, otherwise don't draw OSD
        mupen64plus_cfg.put( "Core", "R4300Emulator", user.r4300Emulator );                                                 // Use Pure Interpreter if 0, Cached Interpreter if 1, or Dynamic Recompiler if 2 or more
        mupen64plus_cfg.put( "Core", "NoCompiledJump", "False" );                                                           // Disable compiled jump commands in dynamic recompiler (should be set to False) 
        mupen64plus_cfg.put( "Core", "DisableExtraMem", "False" );                                                          // Disable 4MB expansion RAM pack. May be necessary for some games
        mupen64plus_cfg.put( "Core", "AutoStateSlotIncrement", "False" );                                                   // Increment the save state slot after each save operation
        mupen64plus_cfg.put( "Core", "EnableDebugger", "False" );                                                           // Activate the R4300 debugger when ROM execution begins, if core was built with Debugger support
        mupen64plus_cfg.put( "Core", "ScreenshotPath", EMPTY );                                                             // Path to directory where screenshots are saved. If this is blank, the default value of ${UserConfigPath}/screenshot will be used
        mupen64plus_cfg.put( "Core", "SaveStatePath", '"' + user.slotSaveDir + '"' );                                       // Path to directory where emulator save states (snapshots) are saved. If this is blank, the default value of ${UserConfigPath}/save will be used
        mupen64plus_cfg.put( "Core", "SaveSRAMPath", '"' + user.sramSaveDir + '"' );                                        // Path to directory where SRAM/EEPROM data (in-game saves) are stored. If this is blank, the default value of ${UserConfigPath}/save will be used
        mupen64plus_cfg.put( "Core", "SharedDataPath", '"' + appData.sharedDataDir + '"' );                                 // Path to a directory to search when looking for shared data files
        
        mupen64plus_cfg.put( "CoreEvents", "Version", "1.000000" );                                                         // Mupen64Plus CoreEvents config parameter set version number.  Please don't change this version number.
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Stop", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Fullscreen", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Save State", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Load State", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Increment Slot", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Reset", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Speed Down", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Speed Up", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Screenshot", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Pause", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Mute", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Increase Volume", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Decrease Volume", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Fast Forward", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Frame Advance", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Gameshark", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Stop", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Fullscreen", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Save State", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Load State", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Increment Slot", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Screenshot", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Pause", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Mute", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Increase Volume", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Decrease Volume", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Fast Forward", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Gameshark", EMPTY );
        
        mupen64plus_cfg.put( "UI-Console", "Version", "1.000000" );                                                         // Mupen64Plus UI-Console config parameter set version number.  Please don't change this version number.
        mupen64plus_cfg.put( "UI-Console", "PluginDir", '"' + appData.libsDir + '"' );                                      // Directory in which to search for plugins
        mupen64plus_cfg.put( "UI-Console", "VideoPlugin", '"' + user.videoPlugin.path + '"' );                              // Filename of video plugin
        mupen64plus_cfg.put( "UI-Console", "AudioPlugin", '"' + user.audioPlugin.path + '"' );                              // Filename of audio plugin
        mupen64plus_cfg.put( "UI-Console", "InputPlugin", '"' + user.inputPlugin.path + '"' );                              // Filename of input plugin
        mupen64plus_cfg.put( "UI-Console", "RspPlugin", '"' + user.rspPlugin.path + '"' );                                  // Filename of RSP plugin
        
        mupen64plus_cfg.put( "Video-General", "Fullscreen", "False" );                                                      // Use fullscreen mode if True, or windowed mode if False
        mupen64plus_cfg.put( "Video-General", "ScreenWidth", String.valueOf( user.videoRenderWidth ) );                     // Width of output window or fullscreen width
        mupen64plus_cfg.put( "Video-General", "ScreenHeight", String.valueOf( user.videoRenderHeight ) );                   // Height of output window or fullscreen height
        mupen64plus_cfg.put( "Video-General", "VerticalSync", "False" );                                                    // If true, activate the SDL_GL_SWAP_CONTROL attribute
        
        mupen64plus_cfg.put( "Video-Rice", "ScreenUpdateSetting", user.gles2RiceScreenUpdateType );                         // Control when the screen will be updated (0=ROM default, 1=VI origin update, 2=VI origin change, 3=CI change, 4=first CI change, 5=first primitive draw, 6=before screen clear, 7=after screen drawn)
        mupen64plus_cfg.put( "Video-Rice", "FastTextureLoading", boolToTF( user.isGles2RiceFastTextureLoadingEnabled ) );   // Use a faster algorithm to speed up texture loading and CRC computation
        mupen64plus_cfg.put( "Video-Rice", "SkipFrame", boolToTF( user.isGles2RiceAutoFrameskipEnabled ) );                 // If this option is enabled, the plugin will skip every other frame
        mupen64plus_cfg.put( "Video-Rice", "LoadHiResTextures", boolToTF( user.isGles2RiceHiResTexturesEnabled ) );         // Enable hi-resolution texture file loading
        if( user.isGles2RiceForceTextureFilterEnabled )                                                                     // Force to use texture filtering or not (0=auto: n64 choose, 1=force no filtering, 2=force filtering)
            mupen64plus_cfg.put( "Video-Rice", "ForceTextureFilter", "2");
        else
            mupen64plus_cfg.put( "Video-Rice", "ForceTextureFilter", "0");
        mupen64plus_cfg.put( "Video-Rice", "TextureEnhancement", user.gles2RiceTextureEnhancement );                        // Primary texture enhancement filter (0=None, 1=2X, 2=2XSAI, 3=HQ2X, 4=LQ2X, 5=HQ4X, 6=Sharpen, 7=Sharpen More, 8=External, 9=Mirrored)
        mupen64plus_cfg.put( "Video-Rice", "TextureEnhancementControl", "1" );                                              // Secondary texture enhancement filter (0 = none, 1-4 = filtered)
        mupen64plus_cfg.put( "Video-Rice", "FogMethod", boolToNum( user.isGles2RiceFogEnabled ) );                          // Enable, Disable or Force fog generation (0=Disable, 1=Enable n64 choose, 2=Force Fog)
        
        gles2n64_conf.save();
        gles2glide64_conf.save();
        mupen64plus_cfg.save();
        //@formatter:on
    }
    
    private static String boolToTF( boolean b )
    {
        return b ? "True" : "False";
    }
    
    private static String boolToNum( boolean b )
    {
        return b ? "1" : "0";
    }
    
    private static String getROMPath()
    {
        String selectedGame = sUserPrefs.selectedGame;
        boolean isSelectedGameNull = selectedGame == null || !( new File( selectedGame ) ).exists();
        boolean isSelectedGameZipped = !isSelectedGameNull && selectedGame.length() >= 5
                && selectedGame.toLowerCase( Locale.US ).endsWith( ".zip" );
        
        if( sActivity == null )
        {
            return null;
        }
        else if( isSelectedGameNull )
        {
            Log.e( "CoreInterface", "ROM does not exist: '" + selectedGame + "'" );
            if( ErrorLogger.hasError() )
                ErrorLogger.putLastError( "OPEN_ROM", "fail_crash" );
            sActivity.finish();
            return null;
        }
        else if( isSelectedGameZipped )
        {
            // Create the temp folder if it doesn't exist:
            String tmpFolderName = sAppData.tempDir;
            File tmpFolder = new File( tmpFolderName );
            tmpFolder.mkdir();
            
            // Clear the folder if anything is in there:
            String[] children = tmpFolder.list();
            if( children != null )
            {
                for( String child : children )
                {
                    FileUtil.deleteFolder( new File( tmpFolder, child ) );
                }
            }
            
            // Unzip the ROM
            String selectedGameUnzipped = Utility.unzipFirstROM( new File( selectedGame ), tmpFolderName );
            if( selectedGameUnzipped == null )
            {
                Log.e( "CoreInterface", "ROM cannot be unzipped: '" + selectedGame + "'" );
                if( ErrorLogger.hasError() )
                    ErrorLogger.putLastError( "OPEN_ROM", "fail_crash" );
                sActivity.finish();
                return null;
            }
            else
            {
                return selectedGameUnzipped;
            }
        }
        else
        {
            return selectedGame;
        }
    }
}
