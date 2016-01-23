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
package paulscode.android.mupen64plusae.jni;

import java.io.File;
import java.util.ArrayList;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.dialog.ConfirmationDialog;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptFileListener;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptIntegerListener;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptTextListener;
import paulscode.android.mupen64plusae.game.GameSurface;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Utility;
import android.content.DialogInterface;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Vibrator;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
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
    
    public interface OnPromptFinishedListener
    {
        /**
         * Called when a prompt is complete
         */
        public void onPromptFinished();
    }
    
    public interface OnSaveLoadListener
    {
        /**
         * Called when a game is saved or a save is loaded
         */
        public void onSaveLoad();
    }
    
    public interface OnExitListener
    {
        /**
         * Called when a game is exited
         * @param True if we want to exit
         */
        public void onExit(boolean shouldExit);
    }
    
    public interface OnRestartListener
    {
        /**
         * Called when a game is restarted
         * @param True if we want to restart
         */
        public void onRestart(boolean shouldRestart);
    }
    
    private static final int SAVE_STATE_FILE_CONFIRM_DIALOG_ID = 0;
    private static final String SAVE_STATE_FILE_CONFIRM_DIALOG_STATE = "SAVE_STATE_FILE_CONFIRM_DIALOG_STATE";
    
    private static final int RESTART_CONFIRM_DIALOG_ID = 1;
    private static final String RESTART_CONFIRM_DIALOG_STATE = "RESTART_CONFIRM_DIALOG_STATE";
    
    private static final int EXIT_CONFIRM_DIALOG_ID = 2;
    private static final String EXIT_CONFIRM_DIALOG_STATE = "RESTART_CONFIRM_DIALOG_STATE";
    
    // Haptic objects - used by NativeInput
    protected static final Vibrator[] sVibrators = new Vibrator[4];
    
    // Core state callbacks - used by NativeImports
    protected static final ArrayList<OnStateCallbackListener> sStateCallbackListeners = new ArrayList<OnStateCallbackListener>();
    protected static final Object sStateCallbackLock = new Object();
    
    // User/app data - used by NativeImports, NativeSDL
    protected static AppData sAppData = null;
    protected static GlobalPrefs sGlobalPrefs = null;
    protected static GamePrefs sGamePrefs = null;
    
    // Audio/video objects - used by NativeSDL
    protected static AudioTrack sAudioTrack = null;
    protected static GameSurface sSurface = null;
    
    // Frame rate info - used by NativeSDL
    protected static OnFpsChangedListener sFpsListener;
    protected static int sFpsRecalcPeriod = 0;
    protected static int sFrameCount = -1;
    protected static long sLastFpsTime = 0;
    
    // Activity and threading objects - used internally
    private static AppCompatActivity sActivity = null;
    private static Thread sCoreThread;
    
    // Startup info - used internally
    protected static String sRomPath = null;
    protected static String sCheatOptions = null;
    protected static boolean sIsRestarting = false;
    
    // Speed info - used internally
    private static final int BASELINE_SPEED = 100;
    private static final int DEFAULT_SPEED = 250;
    private static final int MAX_SPEED = 300;
    private static final int MIN_SPEED = 10;
    private static final int DELTA_SPEED = 10;
    private static boolean sUseCustomSpeed = false;
    private static int sCustomSpeed = DEFAULT_SPEED;
    
    private static File sCurrentSaveStateFile = null;
    
    // Slot info - used internally
    private static final int NUM_SLOTS = 10;
    
    public static void initialize( AppCompatActivity activity,
        GameSurface surface, GamePrefs gamePrefs, String romPath, String romMd5,
        String cheatArgs, boolean isRestarting )
    {
        sRomPath = romPath;
        sCheatOptions = cheatArgs;
        sIsRestarting = isRestarting;
        
        sActivity = activity;
        sSurface = surface;
        sAppData = new AppData( sActivity );
        sGlobalPrefs = new GlobalPrefs( sActivity, sAppData );
        sGamePrefs = gamePrefs;
        NativeConfigFiles.syncConfigFiles( sGamePrefs, sGlobalPrefs, sAppData );
        
        // Make sure various directories exist so that we can write to them
        new File( sGamePrefs.sramDataDir ).mkdirs();
        new File( sGamePrefs.autoSaveDir ).mkdirs();
        new File( sGamePrefs.slotSaveDir ).mkdirs();
        new File( sGamePrefs.userSaveDir ).mkdirs();
        new File( sGamePrefs.screenshotDir ).mkdirs();
        new File( sGamePrefs.coreUserConfigDir ).mkdirs();
        new File( sGlobalPrefs.coreUserDataDir ).mkdirs();
        new File( sGlobalPrefs.coreUserCacheDir ).mkdirs();
    }
    
    public static void registerVibrator( int player, Vibrator vibrator )
    {
        boolean hasVibrator = vibrator.hasVibrator();
        
        if( hasVibrator && player > 0 && player < 5 )
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
    
    public static synchronized void startupEmulator(final String saveToLoad)
    {
        Log.i("CoreInterface", "Startup emulator");
        if( sCoreThread == null )
        {
            // Load the native libraries
            NativeExports.loadLibraries( sAppData.libsDir, Build.VERSION.SDK_INT );
            
            // Start the core thread if not already running
            sCoreThread = new Thread( new Runnable()
            {
                @Override
                public void run()
                {
                    // Initialize input-android plugin (even if we aren't going to use it)
                    NativeInput.init();
                    NativeInput.setConfig( 0, sGamePrefs.isPlugged1, sGlobalPrefs.getPakType( 1 ).getNativeValue() );
                    NativeInput.setConfig( 1, sGamePrefs.isPlugged2, sGlobalPrefs.getPakType( 2 ).getNativeValue() );
                    NativeInput.setConfig( 2, sGamePrefs.isPlugged3, sGlobalPrefs.getPakType( 3 ).getNativeValue() );
                    NativeInput.setConfig( 3, sGamePrefs.isPlugged4, sGlobalPrefs.getPakType( 4 ).getNativeValue() );
                    
                    ArrayList<String> arglist = new ArrayList<String>();
                    arglist.add( "mupen64plus" );
                    arglist.add( "--corelib" );
                    arglist.add( sAppData.coreLib );
                    arglist.add( "--configdir" );
                    arglist.add( sGamePrefs.coreUserConfigDir );
                    if( !sGlobalPrefs.isFramelimiterEnabled )
                    {
                        arglist.add( "--nospeedlimit" );
                    }
                    if( sCheatOptions != null )
                    {
                        arglist.add( "--cheats" );
                        arglist.add( sCheatOptions );
                    }
                    arglist.add( sRomPath );
                    int result = NativeExports.emuStart( sGlobalPrefs.coreUserDataDir, sGlobalPrefs.coreUserCacheDir, arglist.toArray() );
                    if( result != 0 )
                    {
                        // Messages match return codes from mupen64plus-ui-console/main.c
                        final String message;
                        switch( result )
                        {
                            case 1:
                                message = sActivity.getString( R.string.toast_nativeMainFailure01 );
                                break;
                            case 2:
                                message = sActivity.getString( R.string.toast_nativeMainFailure02 );
                                break;
                            case 3:
                                message = sActivity.getString( R.string.toast_nativeMainFailure03 );
                                break;
                            case 4:
                                message = sActivity.getString( R.string.toast_nativeMainFailure04 );
                                break;
                            case 5:
                                message = sActivity.getString( R.string.toast_nativeMainFailure05 );
                                break;
                            case 6:
                                message = sActivity.getString( R.string.toast_nativeMainFailure06 );
                                break;
                            case 7:
                                message = sActivity.getString( R.string.toast_nativeMainFailure07 );
                                break;
                            case 8:
                                message = sActivity.getString( R.string.toast_nativeMainFailure08 );
                                break;
                            case 9:
                                message = sActivity.getString( R.string.toast_nativeMainFailure09 );
                                break;
                            case 10:
                                message = sActivity.getString( R.string.toast_nativeMainFailure10 );
                                break;
                            case 11:
                                message = sActivity.getString( R.string.toast_nativeMainFailure11 );
                                break;
                            case 12:
                                message = sActivity.getString( R.string.toast_nativeMainFailure12 );
                                break;
                            case 13:
                                message = sActivity.getString( R.string.toast_nativeMainFailure13 );
                                break;
                            default:
                                message = sActivity.getString( R.string.toast_nativeMainFailureUnknown );
                                break;
                        }
                        Log.e( "CoreInterface", "Launch failure: " + message );
                        sActivity.runOnUiThread( new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Notifier.showToast( sActivity, message );
                            }
                        } );
                    }
                }
            }, "CoreThread" );
            
            // Auto-load state if desired
            if( !sIsRestarting)
            {
                addOnStateCallbackListener( new OnStateCallbackListener()
                {
                    @Override
                    public void onStateCallback( int paramChanged, int newValue )
                    {
                        if( paramChanged == NativeConstants.M64CORE_EMU_STATE
                                && newValue == NativeConstants.EMULATOR_STATE_RUNNING
                                && saveToLoad != null)
                        {
                            removeOnStateCallbackListener( this );
                            NativeExports.emuLoadFile( saveToLoad );

                            sActivity.runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Notifier.showToast(sActivity, R.string.toast_loadingSession);

                                }
                            });
                        }
                    }
                } );
            }
            
            // Ensure the auto-save is loaded if the operating system stops & restarts the activity
            sIsRestarting = false;
            
            // Start the core on its own thread
            sCoreThread.start();
        }
    }
    
    public static synchronized void shutdownEmulator()
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
            
            // Unload the native libraries
            NativeExports.unloadLibraries();
        }
    }
    
    public static synchronized void resumeEmulator()
    {
        if( sCoreThread != null )
        {
            NativeExports.emuResume();
        }
    }
    
    public static synchronized void pauseEmulator( boolean autoSave, String latestSave )
    {
        if( sCoreThread != null )
        {
            NativeExports.emuPause();
            
            // Auto-save in case device doesn't resume properly (e.g. OS kills process, battery dies, etc.)
            if( autoSave && latestSave != null)
            {                
                
                Notifier.showToast( sActivity, R.string.toast_savingSession );
                
                Log.i("CoreInterface", "Saving file: " + latestSave);
                NativeExports.emuSaveFile( latestSave );
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
    
    public static void toggleFramelimiter()
    {
        boolean state = NativeExports.emuGetFramelimiter();
        NativeExports.emuSetFramelimiter( !state );
    }
    
    public static void setSlot( int value )
    {
        int slot = value % NUM_SLOTS;
        NativeExports.emuSetSlot( slot );
    }
    
    public static void incrementSlot()
    {
        int slot = NativeExports.emuGetSlot();
        setSlot( slot + 1 );
    }
    
    public static void saveSlot(final OnSaveLoadListener onSaveLoadListener)
    {
        int slot = NativeExports.emuGetSlot();
        Notifier.showToast( sActivity, R.string.toast_savingSlot, slot );
        NativeExports.emuSaveSlot();
        
        if(onSaveLoadListener != null)
        {
            onSaveLoadListener.onSaveLoad();
        }
    }
    
    public static void loadSlot(final OnSaveLoadListener onSaveLoadListener)
    {
        int slot = NativeExports.emuGetSlot();
        Notifier.showToast( sActivity, R.string.toast_loadingSlot, slot );
        NativeExports.emuLoadSlot();
        
        if(onSaveLoadListener != null)
        {
            onSaveLoadListener.onSaveLoad();
        }
    }
    
    public static void saveFileFromPrompt()
    {
        CharSequence title = sActivity.getText( R.string.menuItem_fileSave );
        CharSequence hint = sActivity.getText( R.string.hintFileSave );
        int inputType = InputType.TYPE_CLASS_TEXT;
        Prompt.promptText( sActivity, title, null, null, hint, inputType, new PromptTextListener()
        {
            @Override
            public void onDialogClosed( CharSequence text, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    saveState( text.toString() );
                }
            }
        } );
    }
    
    public static void loadFileFromPrompt(final OnSaveLoadListener onSaveLoadListener)
    {
        CharSequence title = sActivity.getText( R.string.menuItem_fileLoad );
        File startPath = new File( sGamePrefs.userSaveDir );
        Prompt.promptFile( sActivity, title, null, startPath, new PromptFileListener()
        {
            @Override
            public void onDialogClosed( File file, int which )
            {
                if( which >= 0 )
                {
                    loadState( file );
                    
                    if(onSaveLoadListener != null)
                    {
                        onSaveLoadListener.onSaveLoad();
                    }
                }

            }
        } );
    }
    
    public static void loadAutoSaveFromPrompt(final OnSaveLoadListener onSaveLoadListener)
    {
        CharSequence title = sActivity.getText( R.string.menuItem_fileLoadAutoSave );
        File startPath = new File( sGamePrefs.autoSaveDir );
        Prompt.promptFile( sActivity, title, null, startPath, new PromptFileListener()
        {
            @Override
            public void onDialogClosed( File file, int which )
            {
                if( which >= 0 )
                {
                    loadState( file );
                    
                    if(onSaveLoadListener != null)
                    {
                        onSaveLoadListener.onSaveLoad();
                    }
                }

            }
        } );
    }
    
    public static void saveState( final String filename )
    {
        sCurrentSaveStateFile = new File( sGamePrefs.userSaveDir + "/" + filename );
        
        if( sCurrentSaveStateFile.exists() )
        {
            String title = sActivity.getString( R.string.confirm_title );
            String message = sActivity.getString( R.string.confirmOverwriteFile_message, filename );
            
            ConfirmationDialog confirmationDialog =
                ConfirmationDialog.newInstance(SAVE_STATE_FILE_CONFIRM_DIALOG_ID, title, message);
            
            FragmentManager fm = sActivity.getSupportFragmentManager();
            confirmationDialog.show(fm, SAVE_STATE_FILE_CONFIRM_DIALOG_STATE);
        }
        else
        {
            Notifier.showToast( sActivity, R.string.toast_savingFile, sCurrentSaveStateFile.getName() );
            NativeExports.emuSaveFile( sCurrentSaveStateFile.getAbsolutePath() );
            
            if(sActivity instanceof OnSaveLoadListener)
            {
                ((OnSaveLoadListener)sActivity).onSaveLoad();
            }
        }
    }
    
    public static void loadState( File file )
    {
        Notifier.showToast( sActivity, R.string.toast_loadingFile, file.getName() );
        NativeExports.emuLoadFile( file.getAbsolutePath() );
    }
    
    public static void screenshot()
    {
        Notifier.showToast( sActivity, R.string.toast_savingScreenshot );
        NativeExports.emuScreenshot();
    }
    
    public static void setCustomSpeedFromPrompt(final OnPromptFinishedListener promptFinishedListener)
    {
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
                            
                            if(promptFinishedListener != null)
                            {
                                promptFinishedListener.onPromptFinished();
                            }
                        }
                    }
                } );
    }
    
    public static void setSlotFromPrompt(final OnPromptFinishedListener promptFinishedListener)
    {
        final CharSequence title = sActivity.getString(R.string.menuItem_selectSlot, NativeExports.emuGetSlot());
            
        Prompt.promptRadioInteger( sActivity, title, NativeExports.emuGetSlot(), 0, 2, 5,
                new PromptIntegerListener()
                {
                    @Override
                    public void onDialogClosed( Integer value, int which )
                    {
                        if( which == DialogInterface.BUTTON_POSITIVE )
                        {
                            setSlot( value );
                            
                            if(promptFinishedListener != null)
                            {
                                promptFinishedListener.onPromptFinished();
                            }
                        }
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
    
    public static synchronized void restartEmulator()
    {
        CoreInterface.shutdownEmulator();
        CoreInterface.startupEmulator(null);
    }
    
    public static synchronized void restart()
    {        
        NativeExports.emuPause();
        String title = sActivity.getString( R.string.confirm_title );
        String message = sActivity.getString( R.string.confirmResetGame_message );
        
        ConfirmationDialog confirmationDialog =
            ConfirmationDialog.newInstance(RESTART_CONFIRM_DIALOG_ID, title, message);
        
        FragmentManager fm = sActivity.getSupportFragmentManager();
        confirmationDialog.show(fm, RESTART_CONFIRM_DIALOG_STATE);
    }
    
    public static void exit()
    {
        NativeExports.emuPause();
        String title = sActivity.getString( R.string.confirm_title );
        String message = sActivity.getString( R.string.confirmExitGame_message );
        
        ConfirmationDialog confirmationDialog =
            ConfirmationDialog.newInstance(EXIT_CONFIRM_DIALOG_ID, title, message);
        
        FragmentManager fm = sActivity.getSupportFragmentManager();
        confirmationDialog.show(fm, EXIT_CONFIRM_DIALOG_STATE);
    }

    public static void onPromptDialogClosed(int id, int which)
    {
        if (id == SAVE_STATE_FILE_CONFIRM_DIALOG_ID)
        {
            if (which == DialogInterface.BUTTON_POSITIVE)
            {
                Notifier.showToast(sActivity, R.string.toast_overwritingFile, sCurrentSaveStateFile.getName());
                NativeExports.emuSaveFile(sCurrentSaveStateFile.getAbsolutePath());

                if(sActivity instanceof OnSaveLoadListener)
                {
                    ((OnSaveLoadListener)sActivity).onSaveLoad();
                }
            }

        }
        else if (id == RESTART_CONFIRM_DIALOG_ID)
        {            
            if(sActivity instanceof OnRestartListener)
            {
                ((OnRestartListener)sActivity).onRestart( which == DialogInterface.BUTTON_POSITIVE );
            }
        }
        else if (id == EXIT_CONFIRM_DIALOG_ID)
        {
            if(sActivity instanceof OnExitListener)
                ((OnExitListener)sActivity).onExit( which == DialogInterface.BUTTON_POSITIVE );
        }

    }
}
