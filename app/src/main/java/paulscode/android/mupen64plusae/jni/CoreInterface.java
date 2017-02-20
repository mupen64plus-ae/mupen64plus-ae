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

import android.content.DialogInterface;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Vibrator;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import paulscode.android.mupen64plusae.dialog.ConfirmationDialog;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptFileListener;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptIntegerListener;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptTextListener;
import paulscode.android.mupen64plusae.game.GameAutoSaveManager;
import paulscode.android.mupen64plusae.game.GameSurface;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Utility;

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
         * Called when a game is requested to exited
         * @param shouldExit True if we want to exit
         */
        public void onExitRequested(boolean shouldExit);

        /**
         * Called when a game is done exiting
         */
        public void onExitFinished();
    }

    public interface OnRestartListener
    {
        /**
         * Called when a game is restarted
         * @param shouldRestart True if we want to restart
         */
        public void onRestart(boolean shouldRestart);
    }

    public static final String COMPLETE_EXTENSION = "complete";

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
    private static Thread sCoreThread = null;

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

    private static boolean sIsPaused = false;

    private static boolean sIsCoreRunning = false;

    private static boolean sUnexpectedVideoLoss = false;

    private static Object sActivitySync = new Object();
    protected static Object sSurfaceSync = new Object();

    public static void initialize(AppCompatActivity activity,
                                  GameSurface surface, GamePrefs gamePrefs, String romPath,
                                  String cheatArgs, boolean isRestarting, String openGlEsVersion)
    {
        sRomPath = romPath;
        sCheatOptions = cheatArgs;
        sIsRestarting = isRestarting;

        sActivity = activity;
        sSurface = surface;
        sAppData = new AppData( sActivity );
        sGlobalPrefs = new GlobalPrefs( sActivity, sAppData );
        sGamePrefs = gamePrefs;
        NativeConfigFiles.syncConfigFiles( sGamePrefs, sGlobalPrefs, sAppData, openGlEsVersion );

        makeDirs();
        moveFromLegacy();
    }

    public static void detachActivity()
    {
        synchronized (sActivitySync)
        {
            sActivity = null;
        }

        synchronized (sSurfaceSync)
        {
            sSurface = null;
        }
    }

    public static boolean isCoreRunning()
    {
        return sIsCoreRunning;
    }

    public static void setUnexpectedVideoLoss(boolean unexpectedVideoLoss)
    {
        sUnexpectedVideoLoss = unexpectedVideoLoss;
    }

    public static boolean isUnexpectedVideoLoss()
    {
        return sUnexpectedVideoLoss;
    }

    private static void makeDirs()
    {
        // Make sure various directories exist so that we can write to them
        FileUtil.makeDirs(sGamePrefs.sramDataDir);
        FileUtil.makeDirs(sGamePrefs.sramDataDir);
        FileUtil.makeDirs(sGamePrefs.autoSaveDir);
        FileUtil.makeDirs(sGamePrefs.slotSaveDir);
        FileUtil.makeDirs(sGamePrefs.userSaveDir);
        FileUtil.makeDirs(sGamePrefs.screenshotDir);
        FileUtil.makeDirs(sGamePrefs.coreUserConfigDir);
        FileUtil.makeDirs(sGlobalPrefs.coreUserDataDir);
        FileUtil.makeDirs(sGlobalPrefs.coreUserCacheDir);
    }

    /**
     * Move any legacy files to new folder structure
     */
    private static void moveFromLegacy()
    {
        final File legacySlotPath = new File(sGlobalPrefs.legacySlotSaves);
        final File legacyAutoSavePath = new File(sGlobalPrefs.legacyAutoSaves);

        if (legacySlotPath.listFiles() != null)
        {
            // Move sra, mpk, fla, and eep files
            final FileFilter fileSramFilter = new FileFilter()
            {

                @Override
                public boolean accept(File pathname)
                {
                    final String fileName = pathname.getName();

                    return fileName.contains(sGamePrefs.gameGoodName + ".sra")
                        || fileName.contains(sGamePrefs.gameGoodName + ".eep")
                        || fileName.contains(sGamePrefs.gameGoodName + ".mpk")
                        || fileName.contains(sGamePrefs.gameGoodName + ".fla");
                }
            };

            // Move all files found
            for (final File file : legacySlotPath.listFiles(fileSramFilter))
            {
                String targetPath = sGamePrefs.sramDataDir + "/" + file.getName();
                File targetFile = new File(targetPath);

                if (!targetFile.exists())
                {
                    Log.i("CoreInterface", "Found legacy SRAM file: " + file + " Moving to " + targetFile.getPath());

                    file.renameTo(targetFile);
                }
                else
                {
                    Log.i("CoreInterface", "Found legacy SRAM file: " + file + " but can't move");
                }
            }

            // Move all st files
            final FileFilter fileSlotFilter = new FileFilter()
            {

                @Override
                public boolean accept(File pathname)
                {
                    final String fileName = pathname.getName();
                    return fileName.contains(sGamePrefs.gameGoodName)
                        && fileName.substring(fileName.length() - 3).contains("st");
                }
            };

            for (final File file : legacySlotPath.listFiles(fileSlotFilter))
            {
                String targetPath = sGamePrefs.slotSaveDir + "/" + file.getName();
                File targetFile = new File(targetPath);

                if (!targetFile.exists())
                {
                    Log.i("CoreInterface", "Found legacy ST file: " + file + " Moving to " + targetFile.getPath());

                    file.renameTo(targetFile);
                }
                else
                {
                    Log.i("CoreInterface", "Found legacy ST file: " + file + " but can't move");
                }
            }
        }

        if(legacyAutoSavePath.listFiles() != null)
        {
            //Move auto saves
            final FileFilter fileAutoSaveFilter = new FileFilter(){

                @Override
                public boolean accept(File pathname)
                {
                    final String fileName = pathname.getName();
                    return fileName.equals(sGamePrefs.legacySaveFileName + ".sav");
                }
            };

            //Move all files found
            for( final File file : legacyAutoSavePath.listFiles(fileAutoSaveFilter) )
            {
                final DateFormat dateFormat = new SimpleDateFormat(GameAutoSaveManager.sFormatString, java.util.Locale.getDefault());
                final String dateAndTime = dateFormat.format(new Date()).toString();
                final String fileName = dateAndTime + ".sav";

                String targetPath = sGamePrefs.autoSaveDir + "/" + fileName;
                File targetFile= new File(targetPath);

                if(!targetFile.exists())
                {
                    Log.i("CoreInterface", "Found legacy SAV file: " + file +
                        " Moving to " + targetFile.getPath());

                    file.renameTo(targetFile);
                }
                else
                {
                    Log.i("CoreInterface", "Found legacy SAV file: " + file +
                        " but can't move");
                }
            }
        }
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
        if( sCoreThread == null )
        {
            Log.i("CoreInterface", "Startup emulator");

            // Start the core thread if not already running
            sCoreThread = new Thread( new Runnable()
            {
                @Override
                public void run()
                {

                    // Load the native libraries
                    NativeExports.loadLibraries( sAppData.libsDir, Build.VERSION.SDK_INT );

                    // Only increase priority if we have more than one processor. The call to check the number of
                    // processors is only available in API level 17
                    if(AppData.IS_JELLY_BEAN_MR1 && Runtime.getRuntime().availableProcessors() > 1 && sGlobalPrefs.useHighPriorityThread) {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                        Log.i("CoreInterface", "Using high priority mode");
                    }

                    sIsCoreRunning = true;

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

                    if(!sIsRestarting)
                    {
                        arglist.add( "--savestate" );
                        arglist.add( saveToLoad );
                    }

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

                    sIsRestarting = false;
                    final int result = NativeExports.emuStart( sGlobalPrefs.coreUserDataDir, sGlobalPrefs.coreUserCacheDir, arglist.toArray() );
                    sIsCoreRunning = false;

                    Log.e( "CoreInterface", "Core thread exit!");

                    synchronized (sActivitySync)
                    {
                        if(sActivity != null)
                        {
                            // Messages match return codes from mupen64plus-ui-console/main.c
                            String message = null;

                            if( result != 0)
                            {
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
                            }

                            final String finalMessage = message;

                            sCoreThread = null;

                            // Unload the native libraries
                            NativeExports.unloadLibraries();
                            sActivity.runOnUiThread( new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if(sActivity != null && result != 0 && finalMessage != null)
                                    {
                                        Notifier.showToast( sActivity, finalMessage );
                                        sActivity.finish();
                                    }

                                    if(sActivity != null && sActivity instanceof OnExitListener && !sIsRestarting)
                                        ((OnExitListener)sActivity).onExitFinished();
                                }
                            } );
                        }
                    }

                    if(sIsRestarting)
                    {
                        CoreInterface.startupEmulator(null);
                    }

                }
            }, "CoreThread" );

            sUseCustomSpeed = false;
            NativeExports.emuSetSpeed( BASELINE_SPEED );

            // Start the core on its own thread

            sIsPaused = false;
            sCoreThread.start();
        }
    }

    public static synchronized void shutdownEmulator()
    {
        if( sCoreThread != null )
        {
            // Tell the core to quit
            NativeExports.emuStop();
        }
    }

    public static synchronized void killEmulator()
    {
        if( sCoreThread != null )
        {

            // Tell the core to quit
            NativeExports.emuStop();
            NativeExports.emuShutdown();
            // Unload the native libraries
            NativeExports.unloadLibraries();

            sCoreThread = null;
        }
    }

    public static synchronized void resumeEmulator()
    {
        if( sCoreThread != null )
        {
            sIsPaused = false;
            NativeExports.emuResume();
        }
    }

    public static synchronized void autoSaveState( final String latestSave )
    {
        // Auto-save in case device doesn't resume properly (e.g. OS kills process, battery dies, etc.)

        //Resume to allow save to take place
        resumeEmulator();
        Log.i("CoreInterface", "Saving file: " + latestSave);

        addOnStateCallbackListener( new OnStateCallbackListener()
        {
            @Override
            public void onStateCallback( int paramChanged, int newValue )
            {
                if( paramChanged == NativeConstants.M64CORE_STATE_SAVECOMPLETE )
                {
                    removeOnStateCallbackListener( this );

                    //newValue == 1, then it was successful
                    if(newValue == 1)
                    {
                        try {
                            new File(latestSave + "." + COMPLETE_EXTENSION).createNewFile();
                        } catch (IOException e) {
                            Log.e("CoreInterface", "Unable to save file due to file write failure: " + latestSave);
                        }
                    }
                    else
                    {
                        Log.e("CoreInterface", "Unable to save file due to bad return: " + latestSave);
                    }
                }
                else
                {
                    Log.e("CoreInterface", "Param changed = " + paramChanged + " value = " + newValue);
                }
            }
        } );

        NativeExports.emuSaveFile( latestSave );
    }

    public static void saveState( final String filename )
    {
        if(sActivity != null)
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
                NativeExports.emuSaveFile( sCurrentSaveStateFile.getAbsolutePath() );

                Notifier.showToast( sActivity, R.string.toast_savingFile, sCurrentSaveStateFile.getName() );

                if(sActivity instanceof OnSaveLoadListener)
                {
                    ((OnSaveLoadListener)sActivity).onSaveLoad();
                }
            }
        }
    }

    public static synchronized void pauseEmulator(  )
    {
        if( sCoreThread != null )
        {
            sIsPaused = true;
            NativeExports.emuPause();
        }
    }

    public static void togglePause()
    {
        int state = NativeExports.emuGetState();
        if( state == NativeConstants.EMULATOR_STATE_PAUSED ) {
            sIsPaused = false;
            NativeExports.emuResume();
        }
        else if( state == NativeConstants.EMULATOR_STATE_RUNNING ){
            sIsPaused = true;
            NativeExports.emuPause();
        }
    }

    public static boolean isPaused()
    {
        return sIsPaused;
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

        if(sActivity != null)
        {
            Notifier.showToast( sActivity, R.string.toast_savingSlot, slot );
        }

        NativeExports.emuSaveSlot();

        if(onSaveLoadListener != null)
        {
            onSaveLoadListener.onSaveLoad();
        }
    }

    public static void loadSlot(final OnSaveLoadListener onSaveLoadListener)
    {
        int slot = NativeExports.emuGetSlot();

        if(sActivity != null)
        {
            Notifier.showToast( sActivity, R.string.toast_loadingSlot, slot );
        }

        NativeExports.emuLoadSlot();

        if(onSaveLoadListener != null)
        {
            onSaveLoadListener.onSaveLoad();
        }
    }

    public static void saveFileFromPrompt()
    {
        if(sActivity != null)
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
    }

    public static void loadFileFromPrompt(final OnSaveLoadListener onSaveLoadListener)
    {
        if(sActivity != null)
        {
            CharSequence title = sActivity.getText( R.string.menuItem_fileLoad );
            File startPath = new File( sGamePrefs.userSaveDir );
            Prompt.promptFile( sActivity, title, null, startPath, "", new PromptFileListener()
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
    }

    public static void loadAutoSaveFromPrompt(final OnSaveLoadListener onSaveLoadListener)
    {
        if(sActivity != null)
        {
            CharSequence title = sActivity.getText( R.string.menuItem_fileLoadAutoSave );
            File startPath = new File( sGamePrefs.autoSaveDir );
            Prompt.promptFile( sActivity, title, null, startPath, "sav", new PromptFileListener()
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
    }

    public static void loadState( File file )
    {
        if(sActivity != null)
        {
            Notifier.showToast( sActivity, R.string.toast_loadingFile, file.getName() );
        }

        NativeExports.emuLoadFile( file.getAbsolutePath() );
    }

    public static void screenshot()
    {
        if(sActivity != null)
        {
            Notifier.showToast( sActivity, R.string.toast_savingScreenshot );
        }

        NativeExports.emuScreenshot();
    }

    public static void setCustomSpeedFromPrompt(final OnPromptFinishedListener promptFinishedListener)
    {
        if(sActivity != null)
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
    }

    public static void setSlotFromPrompt(final OnPromptFinishedListener promptFinishedListener)
    {
        if(sActivity != null)
        {
            final CharSequence title = sActivity.getString(R.string.menuItem_selectSlot);

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
        sIsRestarting = true;
        CoreInterface.shutdownEmulator();
    }

    public static synchronized void restart()
    {
        if(sActivity != null)
        {
            sIsPaused = true;
            NativeExports.emuPause();
            String title = sActivity.getString( R.string.confirm_title );
            String message = sActivity.getString( R.string.confirmResetGame_message );

            ConfirmationDialog confirmationDialog =
                    ConfirmationDialog.newInstance(RESTART_CONFIRM_DIALOG_ID, title, message);

            FragmentManager fm = sActivity.getSupportFragmentManager();
            confirmationDialog.show(fm, RESTART_CONFIRM_DIALOG_STATE);
        }
    }

    public static void exit()
    {
        sIsPaused = true;
        NativeExports.emuPause();

        if(sActivity != null)
        {
            String title = sActivity.getString( R.string.confirm_title );
            String message = sActivity.getString( R.string.confirmExitGame_message );

            ConfirmationDialog confirmationDialog =
                    ConfirmationDialog.newInstance(EXIT_CONFIRM_DIALOG_ID, title, message);

            FragmentManager fm = sActivity.getSupportFragmentManager();
            confirmationDialog.show(fm, EXIT_CONFIRM_DIALOG_STATE);
        }
    }

    public static void onPromptDialogClosed(int id, int which)
    {
        if (id == SAVE_STATE_FILE_CONFIRM_DIALOG_ID)
        {
            if (which == DialogInterface.BUTTON_POSITIVE)
            {
                NativeExports.emuSaveFile(sCurrentSaveStateFile.getAbsolutePath());

                if(sActivity != null)
                {
                    Notifier.showToast(sActivity, R.string.toast_overwritingFile, sCurrentSaveStateFile.getName());
                    if(sActivity instanceof OnSaveLoadListener)
                    {
                        ((OnSaveLoadListener)sActivity).onSaveLoad();
                    }
                }
            }

        }
        else if (id == RESTART_CONFIRM_DIALOG_ID)
        {
            if(sActivity != null && sActivity instanceof OnRestartListener)
            {
                ((OnRestartListener)sActivity).onRestart( which == DialogInterface.BUTTON_POSITIVE );
            }
        }
        else if (id == EXIT_CONFIRM_DIALOG_ID)
        {
            if(sActivity != null && sActivity instanceof OnExitListener)
                ((OnExitListener)sActivity).onExitRequested( which == DialogInterface.BUTTON_POSITIVE );
        }

    }
}
