/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2015 Paul Lamb
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
 * Authors: fzurita
 */

package paulscode.android.mupen64plusae.jni;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.InputType;
import android.util.Log;
import android.view.Surface;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.util.ArrayList;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.jni.CoreService.CoreServiceListener;
import paulscode.android.mupen64plusae.jni.CoreService.LocalBinder;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Utility;
import paulscode.android.mupen64plusae.jni.NativeImports.OnFpsChangedListener;

import static paulscode.android.mupen64plusae.jni.NativeConstants.EMULATOR_STATE_UNKNOWN;

public class CoreFragment extends Fragment implements CoreServiceListener
{
    public interface CoreEventListener
    {
        /**
         * Will be called once the core service is valid
         */
        void onCoreServiceStarted();

        /**
         * Called when a game is requested to exited
         * @param shouldExit True if we want to exit
         */
        void onExitRequested(boolean shouldExit);

        /**
         * Called when a game is restarted
         * @param shouldRestart True if we want to restart
         */
        void onRestart(boolean shouldRestart);

        /**
         * Called when a prompt has finished
         */
        void onPromptFinished();

        /**
         * Called when a game is saved or a save is loaded
         */
        void onSaveLoad();
    }

    private static final String SAVE_STATE_FILE_CONFIRM_DIALOG_STATE = "SAVE_STATE_FILE_CONFIRM_DIALOG_STATE";
    private static final String RESTART_CONFIRM_DIALOG_STATE = "RESTART_CONFIRM_DIALOG_STATE";
    private static final String EXIT_CONFIRM_DIALOG_STATE = "RESTART_CONFIRM_DIALOG_STATE";

    public static final int SAVE_STATE_FILE_CONFIRM_DIALOG_ID = 3;
    public static final int RESET_CONFIRM_DIALOG_ID = 4;
    public static final int EXIT_CONFIRM_DIALOG_ID = 5;

    //Service connection for the progress dialog
    private ServiceConnection mServiceConnection;

    private boolean mCachedStartCore = false;
    private boolean mCachedStopCore = false;

    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private GamePrefs mGamePrefs = null;
    private String mRomGoodName = null;
    private String mRomPath = null;
    private String mRomMd5 = null;
    private String mRomCrc = null;
    private String mRomHeaderName = null;
    private byte mRomCountryCode = 0;
    private String mRomArtPath = null;
    private String mRomLegacySave = null;
    private String mCheatArgs = null;
    private boolean mIsRestarting = false;
    private String mSaveToLoad = null;

    private boolean mIsRunning = false;
    private CoreService mCoreService = null;
    private Surface mSurface = null;
    private OnFpsChangedListener mFpsChangeListener = null;
    private int mFpsRecalcPeriod = 1;
    private File mCurrentSaveStateFile = null;

    // Speed info - used internally
    private static final int BASELINE_SPEED = 100;
    private static final int DEFAULT_SPEED = 250;
    private static final int MAX_SPEED = 300;
    private static final int MIN_SPEED = 10;
    private static final int DELTA_SPEED = 10;
    private boolean mUseCustomSpeed = false;
    private int mCustomSpeed = DEFAULT_SPEED;

    CoreEventListener mCoreEventListener = null;

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.i("CoreFragment", "onCreate");

        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        Log.i("CoreFragment", "onActivityCreated");

        super.onActivityCreated(savedInstanceState);
        
        if(mCachedStartCore)
        {
            actuallyStartCore(getActivity());
            mCachedStartCore = false;
        }

        if(mCachedStopCore)
        {
            actuallyStopCore();
            mCachedStopCore = false;
        }
    }
    
    @Override
    public void onDetach()
    {
        Log.i("CoreFragment", "onDetach");

        super.onDetach();
    }

    @Override
    public void onFailure(final int errorCode)
    {
        final Activity activity = getActivity();
        if(activity != null){
            // Messages match return codes from mupen64plus-ui-console/main.c
            String message = null;

            if( errorCode != 0)
            {
                switch( errorCode )
                {
                    case 1:
                        message = activity.getString( R.string.toast_nativeMainFailure01 );
                        break;
                    case 2:
                        message = activity.getString( R.string.toast_nativeMainFailure02 );
                        break;
                    case 3:
                        message = activity.getString( R.string.toast_nativeMainFailure03 );
                        break;
                    case 4:
                        message = activity.getString( R.string.toast_nativeMainFailure04 );
                        break;
                    case 5:
                        message = activity.getString( R.string.toast_nativeMainFailure05 );
                        break;
                    case 6:
                        message = activity.getString( R.string.toast_nativeMainFailure06 );
                        break;
                    case 7:
                        message = activity.getString( R.string.toast_nativeMainFailure07 );
                        break;
                    case 8:
                        message = activity.getString( R.string.toast_nativeMainFailure08 );
                        break;
                    case 9:
                        message = activity.getString( R.string.toast_nativeMainFailure09 );
                        break;
                    case 10:
                        message = activity.getString( R.string.toast_nativeMainFailure10 );
                        break;
                    case 11:
                        message = activity.getString( R.string.toast_nativeMainFailure11 );
                        break;
                    case 12:
                        message = activity.getString( R.string.toast_nativeMainFailure12 );
                        break;
                    case 13:
                        message = activity.getString( R.string.toast_nativeMainFailure13 );
                        break;
                    default:
                        message = activity.getString( R.string.toast_nativeMainFailureUnknown );
                        break;
                }
                Log.e( "CoreFragment", "Launch failure: " + message );
            }

            final String finalMessage = message;

            activity.runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    Notifier.showToast( activity, finalMessage );
                }
            } );
        }
    }

    @Override
    public void onFinish()
    {
        Log.i("CoreFragment", "onFinish");

        mCoreService = null;
    }
    
    @Override
    public void onCoreServiceDestroyed()
    {
        Log.i("CoreFragment", "onCoreServiceDestroyed");

        mIsRunning = false;
    }

    public void setCoreEventListener(CoreEventListener coreEventListener)
    {
        Log.i("CoreFragment", "setCoreEventListener");

        mCoreEventListener = coreEventListener;
    }

    public void startCore( AppData appData, GlobalPrefs globalPrefs, GamePrefs gamePrefs, String romGoodName,
        String romPath, String romMd5, String romCrc, String romHeaderName, byte romCountryCode, String romArtPath,
        String romLegacySave, String cheatArgs, boolean isRestarting, String saveToLoad)
    {
        Log.i("CoreFragment", "startCore");

        mAppData = appData;
        mGlobalPrefs = globalPrefs;
        mGamePrefs = gamePrefs;
        mRomGoodName = romGoodName;
        mRomPath = romPath;
        mCheatArgs = cheatArgs;
        mIsRestarting = isRestarting;
        mSaveToLoad = saveToLoad;
        mRomMd5 = romMd5;
        mRomCrc = romCrc;
        mRomHeaderName = romHeaderName;
        mRomCountryCode = romCountryCode;
        mRomArtPath = romArtPath;
        mRomLegacySave = romLegacySave;

        if(!mIsRunning)
        {
            if(!NativeConfigFiles.syncConfigFiles( mGamePrefs, mGlobalPrefs, mAppData ))
            {
                if(getActivity() != null)
                {
                    Notifier.showToast(getActivity(), R.string.coreFragment_sdcard_write_error);
                    getActivity().finish();
                }
            }

            if(getActivity() != null)
            {
                actuallyStartCore(getActivity());
            }
            else
            {
                mCachedStartCore = true;
            }

            mIsRunning = true;
        }
    }
    
    private void actuallyStartCore(Activity activity)
    {
        Log.i("CoreFragment", "actuallyStartCore");

        // Defines callbacks for service binding, passed to bindService()
        mServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {

                // We've bound to LocalService, cast the IBinder and get LocalService instance
                LocalBinder binder = (LocalBinder) service;
                mCoreService = binder.getService();

                mCoreService.setSurface(mSurface);
                mCoreService.addOnFpsChangedListener(mFpsChangeListener, mFpsRecalcPeriod);
                mCoreService.setCoreServiceListener(CoreFragment.this);

                if(mCoreEventListener != null && getActivity() != null)
                {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCoreEventListener.onCoreServiceStarted();
                        }
                    });
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
            }
        };

        ArrayList<Integer> pakTypes = new ArrayList<>();
        pakTypes.add(mGamePrefs.getPakType(1).getNativeValue());
        pakTypes.add(mGamePrefs.getPakType(2).getNativeValue());
        pakTypes.add(mGamePrefs.getPakType(3).getNativeValue());
        pakTypes.add(mGamePrefs.getPakType(4).getNativeValue());


        // Start the core
        ActivityHelper.startCoreService(activity.getApplicationContext(), mServiceConnection, mRomGoodName, mRomPath,
                mRomMd5, mRomCrc, mRomHeaderName, mRomCountryCode, mRomArtPath, mRomLegacySave,
                mCheatArgs, mIsRestarting, mSaveToLoad, mAppData.coreLib, mGlobalPrefs.useHighPriorityThread, pakTypes,
                mGamePrefs.isPlugged, mGlobalPrefs.isFramelimiterEnabled, mGlobalPrefs.coreUserDataDir,
                mGlobalPrefs.coreUserCacheDir, mGamePrefs.getCoreUserConfigDir(), mGamePrefs.getUserSaveDir(), mAppData.libsDir);
    }

    private void actuallyStopCore()
    {
        Log.i("CoreFragment", "actuallyStopCore");

        if(mIsRunning)
        {
            mIsRunning = false;

            if(getActivity() != null)
            {
                getActivity().runOnUiThread( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mCoreService = null;
                        ActivityHelper.stopCoreService(getActivity().getApplicationContext(), mServiceConnection);

                    }
                } );
            }
        }
    }

    public void resumeEmulator()
    {
        Log.i("CoreFragment", "resumeEmulator");

        if(mCoreService != null)
        {
            mCoreService.resumeEmulator();
        }
    }

    public void advanceFrame()
    {
        Log.i("CoreFragment", "advanceFrame");

        if(mCoreService != null)
        {
            mCoreService.advanceFrame();
        }
    }

    public void emuGameShark(boolean pressed)
    {
        Log.i("CoreFragment", "emuGameShark");

        if(mCoreService != null)
        {
            mCoreService.emuGameShark(pressed);
        }
    }

    public void clearOnFpsChangedListener()
    {
        Log.i("CoreFragment", "clearOnFpsChangedListener");

        if(mCoreService != null && mFpsChangeListener != null)
        {
            mCoreService.removeOnFpsChangedListener(mFpsChangeListener);
        }
    }

    public void setOnFpsChangedListener(OnFpsChangedListener fpsListener, int fpsRecalcPeriod )
    {
        Log.i("CoreFragment", "addOnFpsChangedListener");

        mFpsChangeListener = fpsListener;
        mFpsRecalcPeriod = fpsRecalcPeriod;
        if(mCoreService != null)
        {
            mCoreService.addOnFpsChangedListener(fpsListener, fpsRecalcPeriod);
        }
    }

    public void setControllerState( int controllerNum, boolean[] buttons, int axisX, int axisY )
    {
        if(mCoreService != null)
        {
            mCoreService.setControllerState( controllerNum, buttons, axisX, axisY );
        }
    }

    public void registerVibrator( int player, Vibrator vibrator )
    {
        if(mCoreService != null)
        {
            mCoreService.registerVibrator(player, vibrator);
        }
    }

    public void pauseEmulator()
    {
        Log.i("CoreFragment", "pauseEmulator");

        if(mCoreService != null)
        {
            mCoreService.pauseEmulator();
        }
    }

    public void togglePause()
    {
        Log.i("CoreFragment", "togglePause");

        if(mCoreService != null)
        {
            mCoreService.togglePause();
        }
    }

    public boolean isRunning()
    {
        Log.i("CoreFragment", "isRunning");

        return mCoreService != null && mCoreService.isRunning();
    }

    public  boolean isShuttingDown()
    {
        Log.i("CoreFragment", "isShuttingDown");

        return mCoreService != null && mCoreService.isShuttingDown();
    }

    public void exit()
    {
        Log.i("CoreFragment", "exit");

        if (mCoreService != null) {
            mCoreService.pauseEmulator();
        }

        if(getActivity() != null)
        {
            String title = getActivity().getString( R.string.confirm_title );
            String message = getActivity().getString( R.string.confirmExitGame_message );

            ConfirmationDialog confirmationDialog =
                    ConfirmationDialog.newInstance(EXIT_CONFIRM_DIALOG_ID, title, message);

            FragmentManager fm = getActivity().getSupportFragmentManager();
            confirmationDialog.show(fm, EXIT_CONFIRM_DIALOG_STATE);
        }
    }

    public void toggleSpeed()
    {
        Log.i("CoreFragment", "toggleSpeed");

        if (mCoreService != null)
        {
            mUseCustomSpeed = !mUseCustomSpeed;
            int speed = mUseCustomSpeed ? mCustomSpeed : BASELINE_SPEED;
            mCoreService.setCustomSpeed(speed);
        }
    }

    public void fastForward( boolean pressed )
    {
        Log.i("CoreFragment", "fastForward");

        int speed = pressed ? mCustomSpeed : BASELINE_SPEED;
        NativeExports.emuSetSpeed( speed );
    }


    public void saveSlot()
    {
        Log.i("CoreFragment", "saveSlot");

        if (mCoreService != null)
        {
            int slot = mCoreService.getSlot();

            if(getActivity() != null)
            {
                Notifier.showToast( getActivity(), R.string.toast_savingSlot, slot );
            }

            mCoreService.saveSlot();

            if(mCoreEventListener != null)
            {
                mCoreEventListener.onSaveLoad();
            }
        }
    }

    public void loadSlot()
    {
        Log.i("CoreFragment", "loadSlot");

        if (mCoreService != null)
        {
            int slot = mCoreService.getSlot();

            if(getActivity() != null)
            {
                Notifier.showToast( getActivity(), R.string.toast_loadingSlot, slot );
            }

            mCoreService.loadSlot();

            if(mCoreEventListener != null)
            {
                mCoreEventListener.onSaveLoad();
            }
        }
    }

    public int getSlot()
    {
        Log.i("CoreFragment", "getSlot");

        if (mCoreService != null)
        {
            return mCoreService.getSlot();
        }
        else
        {
            return 0;
        }
    }

    public void incrementSlot()
    {
        Log.i("CoreFragment", "incrementSlot");

        if (mCoreService != null)
        {
            int slot = mCoreService.getSlot();
            mCoreService.setSlot( slot + 1 );
        }
    }

    public void setSlotFromPrompt()
    {
        Log.i("CoreFragment", "setSlotFromPrompt");

        if(getActivity() != null)
        {
            final CharSequence title = getActivity().getString(R.string.menuItem_selectSlot);

            Prompt.promptRadioInteger( getActivity(), title, NativeExports.emuGetSlot(), 0, 2, 5,
                    new Prompt.PromptIntegerListener()
                    {
                        @Override
                        public void onDialogClosed( Integer value, int which )
                        {
                            if( which == DialogInterface.BUTTON_POSITIVE )
                            {
                                mCoreService.setSlot(value);

                                if(mCoreEventListener != null)
                                {
                                    mCoreEventListener.onPromptFinished();
                                }
                            }
                        }
                    } );
        }
    }

    public void saveFileFromPrompt()
    {
        Log.i("CoreFragment", "saveFileFromPrompt");

        if(getActivity() != null)
        {
            CharSequence title = getActivity().getText( R.string.menuItem_fileSave );
            CharSequence hint = getActivity().getText( R.string.hintFileSave );
            int inputType = InputType.TYPE_CLASS_TEXT;
            Prompt.promptText( getActivity(), title, null, null, hint, inputType, new Prompt.PromptTextListener()
            {
                @Override
                public void onDialogClosed( CharSequence text, int which )
                {
                    if( which == DialogInterface.BUTTON_POSITIVE )
                    {
                        saveState(text.toString());
                    }
                }
            } );
        }
    }

    public void saveState( final String filename )
    {
        Log.i("CoreFragment", "saveState");

        if(getActivity() != null)
        {
            mCurrentSaveStateFile = new File( mGamePrefs.getUserSaveDir() + "/" +
                    filename + "." + mRomGoodName + ".sav");

            if( mCurrentSaveStateFile.exists() )
            {

                String title = getActivity().getString( R.string.confirm_title );
                String message = getActivity().getString( R.string.confirmOverwriteFile_message, filename );

                ConfirmationDialog confirmationDialog =
                        ConfirmationDialog.newInstance(SAVE_STATE_FILE_CONFIRM_DIALOG_ID, title, message);

                FragmentManager fm = getActivity().getSupportFragmentManager();
                confirmationDialog.show(fm, SAVE_STATE_FILE_CONFIRM_DIALOG_STATE);
            }
            else
            {
                NativeExports.emuSaveFile( mCurrentSaveStateFile.getAbsolutePath() );

                Notifier.showToast( getActivity(), R.string.toast_savingFile, mCurrentSaveStateFile.getName() );

                if(mCoreEventListener != null)
                {
                    mCoreEventListener.onSaveLoad();
                }
            }
        }
    }

    public void loadFileFromPrompt()
    {
        Log.i("CoreFragment", "loadFileFromPrompt");

        if(getActivity() != null)
        {
            CharSequence title = getActivity().getText( R.string.menuItem_fileLoad );
            File startPath = new File( mGamePrefs.getUserSaveDir() );
            Prompt.promptFile( getActivity(), title, null, startPath, "", new Prompt.PromptFileListener()
            {
                @Override
                public void onDialogClosed( File file, int which )
                {
                    if( which >= 0 )
                    {
                        loadState(file);

                        if(mCoreEventListener != null)
                        {
                            mCoreEventListener.onSaveLoad();
                        }
                    }
                }
            } );
        }
    }

    public void loadAutoSaveFromPrompt()
    {
        Log.i("CoreFragment", "loadAutoSaveFromPrompt");

        if(getActivity() != null)
        {
            CharSequence title = getActivity().getText( R.string.menuItem_fileLoadAutoSave );
            File startPath = new File( mGamePrefs.getAutoSaveDir() );
            Prompt.promptFile( getActivity(), title, null, startPath, "sav", new Prompt.PromptFileListener()
            {
                @Override
                public void onDialogClosed( File file, int which )
                {
                    if( which >= 0 )
                    {
                        loadState(file);

                        if(mCoreEventListener != null)
                        {
                            mCoreEventListener.onSaveLoad();
                        }
                    }
                }
            } );
        }
    }

    public void loadState( File file )
    {
        Log.i("CoreFragment", "loadState");

        if(getActivity() != null)
        {
            Notifier.showToast( getActivity(), R.string.toast_loadingFile, file.getName() );
        }

        mCoreService.loadState(file);
    }

    public void autoSaveState( final String latestSave, boolean shutdownOnFinish )
    {
        Log.i("CoreFragment", "autoSaveState");

        if (mCoreService != null)
        {
            mCoreService.autoSaveState(latestSave, shutdownOnFinish);
        }
    }

    public void screenshot()
    {
        Log.i("CoreFragment", "screenshot");

        if (mCoreService != null)
        {
            if(getActivity() != null)
            {
                Notifier.showToast( getActivity(), R.string.toast_savingScreenshot );
            }

            mCoreService.screenshot();
        }
    }

    public void toggleFramelimiter()
    {
        Log.i("CoreFragment", "toggleFramelimiter");

        if (mCoreService != null)
        {
            mCoreService.toggleFramelimiter();
        }
    }

    public boolean getFramelimiter()
    {
        Log.i("CoreFragment", "getFramelimiter");

        return mCoreService != null &&  mCoreService.getFramelimiter();
    }

    public void restart()
    {
        Log.i("CoreFragment", "restart");

        if (mCoreService != null)
        {
            if(getActivity() != null)
            {
                mCoreService.pauseEmulator();
                String title = getActivity().getString( R.string.confirm_title );
                String message = getActivity().getString( R.string.confirmResetGame_message );

                ConfirmationDialog confirmationDialog =
                        ConfirmationDialog.newInstance(RESET_CONFIRM_DIALOG_ID, title, message);

                FragmentManager fm = getActivity().getSupportFragmentManager();
                confirmationDialog.show(fm, RESTART_CONFIRM_DIALOG_STATE);
            }
        }
    }

    public void restartEmulator()
    {
        Log.i("CoreFragment", "restartEmulator");

        if (mCoreService != null)
        {
            mCoreService.restart();
        }
    }

    public void shutdownEmulator()
    {
        Log.i( "CoreFragment", "shutdownEmulator" );

        if (mCoreService != null)
        {
            mCoreService.shutdownEmulator();
        }
    }

    public int getState()
    {
        Log.i("CoreFragment", "getState");

        if (mCoreService != null)
        {
            return mCoreService.getState();
        }
        else
        {
            return EMULATOR_STATE_UNKNOWN;
        }
    }

    public void incrementCustomSpeed()
    {
        Log.i("CoreFragment", "incrementCustomSpeed");

        setCustomSpeed( mCustomSpeed + DELTA_SPEED );
    }

    public void decrementCustomSpeed()
    {
        Log.i("CoreFragment", "decrementCustomSpeed");

        setCustomSpeed( mCustomSpeed - DELTA_SPEED );
    }

    public void setCustomSpeed(int value)
    {
        Log.i("CoreFragment", "setCustomSpeed");

        mCustomSpeed = Utility.clamp( value, MIN_SPEED, MAX_SPEED );
        mUseCustomSpeed = true;

        if(mCoreService != null)
        {
            mCoreService.setCustomSpeed(mCustomSpeed);
        }
    }

    public void updateControllerConfig(int player, boolean plugged, int value)
    {
        Log.i("CoreFragment", "updateControllerConfig");

        if(mCoreService != null)
        {
            mCoreService.updateControllerConfig(player, plugged, value);
        }
    }

    public int getCurrentSpeed()
    {
        Log.i("CoreFragment", "getCurrentSpeed");

        return mUseCustomSpeed ? mCustomSpeed : BASELINE_SPEED;
    }

    public void setCustomSpeedFromPrompt()
    {
        Log.i("CoreFragment", "setCustomSpeedFromPrompt");

        if(getActivity() != null)
        {
            final CharSequence title = getActivity().getText( R.string.menuItem_setSpeed );
            Prompt.promptInteger( getActivity(), title, "%1$d %%", mCustomSpeed, MIN_SPEED, MAX_SPEED,
                    new Prompt.PromptIntegerListener()
                    {
                        @Override
                        public void onDialogClosed( Integer value, int which )
                        {
                            if( which == DialogInterface.BUTTON_POSITIVE )
                            {
                                setCustomSpeed( value );

                                if(mCoreEventListener != null)
                                {
                                    mCoreEventListener.onPromptFinished();
                                }
                            }
                        }
                    } );
        }
    }

    public void onPromptDialogClosed(int id, int which)
    {
        Log.i("CoreFragment", "onPromptDialogClosed");

        if (id == SAVE_STATE_FILE_CONFIRM_DIALOG_ID)
        {
            mCoreService.saveState(mCurrentSaveStateFile.getAbsolutePath());

            if(getActivity() != null)
            {
                Notifier.showToast(getActivity(), R.string.toast_overwritingFile, mCurrentSaveStateFile.getName());
                if(mCoreEventListener != null)
                {
                    mCoreEventListener.onSaveLoad();
                }
            }
        }
        else if (id == RESET_CONFIRM_DIALOG_ID)
        {
            if(mCoreEventListener != null)
            {
                mCoreEventListener.onRestart( which == DialogInterface.BUTTON_POSITIVE );
            }
        }
        else if (id == EXIT_CONFIRM_DIALOG_ID)
        {
            if(mCoreEventListener != null)
                mCoreEventListener.onExitRequested( which == DialogInterface.BUTTON_POSITIVE );
        }
    }
    public void setSurface(Surface surface)
    {
        Log.i( "CoreFragment", "setSurface" );
        mSurface = surface;
        if(mCoreService != null)
        {
            mCoreService.setSurface(mSurface);
        }
    }

    public void unsetSurface()
    {
        Log.i( "CoreFragment", "unsetSurface" );
        if(mCoreService != null)
        {
            mCoreService.unsetSurface();
        }
    }

    public void destroySurface()
    {
        Log.i("CoreFragment", "destroySurface");

        if(mCoreService != null)
        {
            mCoreService.destroySurface();
        }
    }
    
    public boolean IsInProgress()
    {
        return mIsRunning;
    }

    public boolean hasServiceStarted()
    {
        return mCoreService != null;
    }
}