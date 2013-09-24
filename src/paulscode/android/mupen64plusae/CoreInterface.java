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

import paulscode.android.mupen64plusae.jni.NativeConfigFiles;
import paulscode.android.mupen64plusae.jni.NativeConstants;
import paulscode.android.mupen64plusae.jni.NativeExports;
import paulscode.android.mupen64plusae.jni.NativeImports;
import paulscode.android.mupen64plusae.jni.NativeInput;
import paulscode.android.mupen64plusae.jni.NativeSDL;
import paulscode.android.mupen64plusae.persistent.AppData;
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
    
    // Haptic objects - used by NativeInput
    protected static final Vibrator[] sVibrators = new Vibrator[4];
    
    // Core state callbacks - used by NativeImports
    protected static final ArrayList<OnStateCallbackListener> sStateCallbackListeners = new ArrayList<OnStateCallbackListener>();
    protected static final Object sStateCallbackLock = new Object();
    
    // User/app data - used by NativeImports, NativeSDL
    protected static AppData sAppData = null;
    protected static UserPrefs sUserPrefs = null;
    
    // Audio/video objects - used by NativeSDL
    protected static AudioTrack sAudioTrack = null;
    protected static GameSurface sSurface = null;
    
    // Frame rate info - used by NativeSDL
    protected static OnFpsChangedListener sFpsListener;
    protected static int sFpsRecalcPeriod = 0;
    protected static int sFrameCount = -1;
    protected static long sLastFpsTime = 0;
    
    // Activity and threading objects - used internally
    private static Activity sActivity = null;
    private static Thread sCoreThread;
    
    // Startup info - used internally
    protected static boolean sIsRestarting = false;
    protected static String sCheatOptions = null;
    
    // Speed info - used internally
    private static final int BASELINE_SPEED = 100;
    private static final int DEFAULT_SPEED = 250;
    private static final int MAX_SPEED = 300;
    private static final int MIN_SPEED = 10;
    private static final int DELTA_SPEED = 10;
    private static boolean sUseCustomSpeed = false;
    private static int sCustomSpeed = DEFAULT_SPEED;
    
    // Slot info - used internally
    private static final int NUM_SLOTS = 10;
    
    public static void initialize( Activity activity, GameSurface surface, String cheatArgs, boolean isRestarting )
    {
        sCheatOptions = cheatArgs;
        sIsRestarting = isRestarting;
        
        sActivity = activity;
        sSurface = surface;
        sAppData = new AppData( sActivity );
        sUserPrefs = new UserPrefs( sActivity );
        NativeConfigFiles.syncConfigFiles( sUserPrefs, sAppData );
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
