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
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.StartCoreServiceParams;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog;
import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.jni.CoreService.CoreServiceListener;
import paulscode.android.mupen64plusae.jni.CoreService.LocalBinder;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.PixelBuffer;
import paulscode.android.mupen64plusae.util.Utility;
import paulscode.android.mupen64plusae.jni.CoreInterface.OnFpsChangedListener;

public class CoreFragment extends Fragment implements CoreServiceListener, CoreService.LoadingDataListener
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

        /**
         * Provides the current state of netplay initialization
         * @param success True of initialization was complete
         */
        void onNetplayInit(boolean success);
    }
    
    private static final String TAG = "CoreFragment";

    private static final String SAVE_STATE_FILE_CONFIRM_DIALOG_STATE = "SAVE_STATE_FILE_CONFIRM_DIALOG_STATE";
    private static final String RESTART_CONFIRM_DIALOG_STATE = "RESTART_CONFIRM_DIALOG_STATE";
    private static final String EXIT_CONFIRM_DIALOG_STATE = "RESTART_CONFIRM_DIALOG_STATE";

    private static final int SAVE_STATE_FILE_CONFIRM_DIALOG_ID = 3;
    private static final int RESET_CONFIRM_DIALOG_ID = 4;
    private static final int EXIT_CONFIRM_DIALOG_ID = 5;

    //Service connection for the progress dialog
    private ServiceConnection mServiceConnection;

    //Progress dialog for extracting ROMs
    private ProgressDialog mProgress = null;

    private GamePrefs mGamePrefs = null;
    private String mRomGoodName = null;
    private String mRomDisplayName = null;
    private String mRomPath = null;
    private String mZipPath = null;
    private String mRomMd5 = null;
    private String mRomCrc = null;
    private String mRomHeaderName = null;
    private byte mRomCountryCode = 0;
    private String mRomArtPath = null;
    private boolean mIsRestarting = false;
    private boolean mUseRaphnetIfAvailable = false;
    private int mVideoRenderWidth = 0;
    private int mVideoRenderHeight = 0;
    private String mNetplayHost = "";
    private int mNetplayPort = 0;

    private boolean mIsRunning = false;
    private CoreService mCoreService = null;
    private OnFpsChangedListener mFpsChangeListener = null;
    private int mFpsRecalcPeriod = 30;
    private File mCurrentSaveStateFile = null;

    // Speed info - used internally
    public static final int BASELINE_SPEED = 100;
    private static final int DEFAULT_SPEED = 250;
    private static final int MAX_SPEED = 300;
    private static final int MIN_SPEED = 10;
    private static final int DELTA_SPEED = 10;
    private boolean mUseCustomSpeed = false;
    private int mCustomSpeed = DEFAULT_SPEED;

    private CoreEventListener mCoreEventListener = null;

    private boolean mAskingForExit = false;

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        Log.i(TAG, "onActivityCreated");

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDetach()
    {
        //This can be null if this fragment is never utilized and this will be called on shutdown
        if(mProgress != null)
        {
            mProgress.dismiss();
        }

        super.onDetach();
    }

    @Override
    public void onFailure(final int errorCode)
    {
        if( errorCode != 0)
        {
            String message = null;
            try {
                FragmentActivity activity = requireActivity();
                message = activity.getString( R.string.toast_nativeMainFailure07 );
                String finalMessage = message;
                activity.runOnUiThread(() -> Notifier.showToast( activity, finalMessage));
            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
            }

            Log.e( TAG, "Launch failure: " + message );
        }
    }

    @Override
    public void onFinish()
    {
        Log.i(TAG, "onFinish");

        mCoreService = null;
    }

    @Override
    public void onCoreServiceStarted() {
        Log.i(TAG, "onCoreServiceStarted");

        try {
            requireActivity().runOnUiThread(() -> {
                if (mCoreEventListener != null) {
                    mCoreEventListener.onCoreServiceStarted();
                }
            });
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCoreServiceDestroyed()
    {
        Log.i(TAG, "onCoreServiceDestroyed");

        mIsRunning = false;
    }

    @Override
    public void onNetplayInitComplete(boolean success) {
        if(mCoreEventListener != null)
        {
            mCoreEventListener.onNetplayInit(success);
        }
    }

    @Override
    public void loadingStarted()
    {
        try {
            FragmentActivity activity = requireActivity();
            activity.runOnUiThread(() -> {
                CharSequence title = activity.getString( R.string.extractRomTask_title );
                CharSequence message = activity.getString( R.string.toast_pleaseWait );

                String displayName;

                if (mZipPath != null) {
                    DocumentFile file = FileUtil.getDocumentFileSingle(activity, Uri.parse(mZipPath));
                    displayName = file == null ? mRomDisplayName : file.getName();
                } else {
                    displayName = mRomDisplayName;
                }
                mProgress = new ProgressDialog( mProgress, activity, title, displayName, message, false );
                mProgress.show();
            });
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadingFinished()
    {
        try {
            Activity activity = requireActivity();
            activity.runOnUiThread(() -> {
                if (mProgress != null) {
                    activity.runOnUiThread(() -> mProgress.dismiss());
                }
            });

        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void setCoreEventListener(CoreEventListener coreEventListener)
    {
        Log.i(TAG, "setCoreEventListener");

        mCoreEventListener = coreEventListener;
    }

    public void startCore(GlobalPrefs globalPrefs, GamePrefs gamePrefs, String romGoodName, String romDisplayName,
                          String romPath, String zipPath, String romMd5, String romCrc, String romHeaderName, byte romCountryCode, String romArtPath,
                          boolean isRestarting, int videoRenderWidth, int videoRenderHeight, String netplayHost, int netplayPort)
    {
        Log.i(TAG, "startCore");

        mGamePrefs = gamePrefs;
        mRomGoodName = romGoodName;
        mRomDisplayName = romDisplayName;
        mRomPath = romPath;
        mZipPath = zipPath;
        mIsRestarting = isRestarting;
        mRomMd5 = romMd5;
        mRomCrc = romCrc;
        mRomHeaderName = romHeaderName;
        mRomCountryCode = romCountryCode;
        mRomArtPath = romArtPath;
        mUseRaphnetIfAvailable = globalPrefs.useRaphnetDevicesIfAvailable && RaphnetControllerHandler.raphnetDevicesPresent(getContext());
        mVideoRenderWidth = videoRenderWidth;
        mVideoRenderHeight = videoRenderHeight;
        mNetplayHost = netplayHost;
        mNetplayPort = netplayPort;

        if(!mIsRunning)
        {
            try {
                actuallyStartCore(requireActivity());
            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
            }

            mIsRunning = true;
        }
    }

    private void actuallyStartCore(Activity activity)
    {
        Log.i(TAG, "actuallyStartCore");

        // Defines callbacks for service binding, passed to bindService()
        mServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {

                // We've bound to LocalService, cast the IBinder and get LocalService instance
                Log.e(TAG, service.getClass().getCanonicalName());
                LocalBinder binder = (LocalBinder) service;
                mCoreService = binder.getService();

                mCoreService.addOnFpsChangedListener(mFpsChangeListener, mFpsRecalcPeriod);
                mCoreService.setCoreServiceListener(CoreFragment.this);
                mCoreService.setLoadingDataListener(CoreFragment.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
            }
        };

        // Start the core
        StartCoreServiceParams params = new StartCoreServiceParams();
        params.setRomGoodName(mRomGoodName);
        params.setRomDisplayName(mRomDisplayName);
        params.setRomPath(mRomPath);
        params.setZipPath(mZipPath);
        params.setRomMd5(mRomMd5);
        params.setRomCrc(mRomCrc);
        params.setRomHeaderName(mRomHeaderName);
        params.setRomCountryCode(mRomCountryCode);
        params.setRomArtPath(mRomArtPath);
        params.setRestarting(mIsRestarting);
        params.setUseRaphnetDevicesIfAvailable(mUseRaphnetIfAvailable);
        params.setVideoRenderWidth(mVideoRenderWidth);
        params.setVideoRenderHeight(mVideoRenderHeight);
        params.setNetplayHost(mNetplayHost);
        params.setNetplayPort(mNetplayPort);

        ActivityHelper.startCoreService(activity.getApplicationContext(), mServiceConnection, params);
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    private void actuallyStopCore()
    {
        Log.i(TAG, "actuallyStopCore");

        if(mIsRunning)
        {
            mIsRunning = false;

            try {
                Activity activity = requireActivity();

                activity.runOnUiThread(() -> {
                    mCoreService = null;
                    ActivityHelper.stopCoreService(activity.getApplicationContext(), mServiceConnection);
                });
            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    public void resumeEmulator()
    {
        Log.i(TAG, "resumeEmulator");

        if(mCoreService != null && !mAskingForExit)
        {
            mCoreService.resumeEmulator();
        }
    }

    public void advanceFrame()
    {
        Log.i(TAG, "advanceFrame");

        if(mCoreService != null)
        {
            mCoreService.advanceFrame();
        }
    }

    public void emuGameShark(boolean pressed)
    {
        Log.i(TAG, "emuGameShark");

        if(mCoreService != null)
        {
            mCoreService.emuGameShark(pressed);
        }
    }

    public void clearOnFpsChangedListener()
    {
        Log.i(TAG, "clearOnFpsChangedListener");

        if(mCoreService != null && mFpsChangeListener != null)
        {
            mCoreService.removeOnFpsChangedListener(mFpsChangeListener);
        }
    }

    public void setOnFpsChangedListener(OnFpsChangedListener fpsListener, int fpsRecalcPeriod )
    {
        Log.i(TAG, "addOnFpsChangedListener");

        mFpsChangeListener = fpsListener;
        mFpsRecalcPeriod = fpsRecalcPeriod;
        if(mCoreService != null)
        {
            mCoreService.addOnFpsChangedListener(fpsListener, fpsRecalcPeriod);
        }
    }

    public void setControllerState( int controllerNum, boolean[] buttons, int axisX, int axisY, boolean isKeyboard )
    {
        if(mCoreService != null)
        {
            mCoreService.setControllerState( controllerNum, buttons, axisX, axisY, isKeyboard );
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
        Log.i(TAG, "pauseEmulator");

        if(mCoreService != null)
        {
            mCoreService.pauseEmulator();
        }
    }

    public void togglePause()
    {
        Log.i(TAG, "togglePause");

        if(mCoreService != null)
        {
            mCoreService.togglePause();
        }
    }

    public boolean isRunning()
    {
        Log.i(TAG, "isRunning");

        return mCoreService != null && mCoreService.isRunning();
    }

    public  boolean isShuttingDown()
    {
        Log.i(TAG, "isShuttingDown");

        return mCoreService != null && mCoreService.isShuttingDown();
    }

    public void exit()
    {
        Log.i(TAG, "exit");

        if (mCoreService != null) {
            mCoreService.pauseEmulator();
        }

        try {
            mAskingForExit = true;
            FragmentActivity activity = requireActivity();
            String title = activity.getString(R.string.confirm_title);
            String message = activity.getString(R.string.confirmExitGame_message);

            ConfirmationDialog confirmationDialog =
                    ConfirmationDialog.newInstance(EXIT_CONFIRM_DIALOG_ID, title, message);

            FragmentManager fm = activity.getSupportFragmentManager();
            confirmationDialog.show(fm, EXIT_CONFIRM_DIALOG_STATE);
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
            mAskingForExit = false;
        }
    }

    public void toggleSpeed()
    {
        Log.i(TAG, "toggleSpeed");

        if (mCoreService != null)
        {
            mUseCustomSpeed = !mUseCustomSpeed;
            int speed = mUseCustomSpeed ? mCustomSpeed : BASELINE_SPEED;
            mCoreService.setCustomSpeed(speed);
        }
    }

    public void fastForward( boolean pressed )
    {
        Log.i(TAG, "fastForward");
        if (mCoreService != null)
        {
            int speed = pressed ? mCustomSpeed : BASELINE_SPEED;
            mCoreService.setCustomSpeed( speed );
        }
    }


    public void saveSlot()
    {
        Log.i(TAG, "saveSlot");

        if (mCoreService != null)
        {
            int slot = mCoreService.getSlot();

            try {
                Notifier.showToast(requireActivity(), R.string.toast_savingSlot, slot);
            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
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
        Log.i(TAG, "loadSlot");

        if (mCoreService != null)
        {
            int slot = mCoreService.getSlot();
            try {
                Notifier.showToast( requireActivity(), R.string.toast_loadingSlot, slot );
            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
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
        Log.i(TAG, "getSlot");

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
        Log.i(TAG, "incrementSlot");

        if (mCoreService != null)
        {
            int slot = mCoreService.getSlot();
            mCoreService.setSlot( slot + 1 );
        }
    }

    public void setSlotFromPrompt()
    {
        Log.i(TAG, "setSlotFromPrompt");

        if (mCoreService == null) {
            return;
        }

        try {
            Activity activity = requireActivity();
            final CharSequence title = activity.getString(R.string.menuItem_selectSlot);

            Prompt.promptRadioInteger(activity, title, mCoreService.getSlot(), 0, 2, 5,
                    (value, which) -> {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            if (mCoreService != null) {
                                mCoreService.setSlot(value);
                            }

                            if (mCoreEventListener != null) {
                                mCoreEventListener.onPromptFinished();
                            }
                        }
                    });
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void saveFileFromPrompt()
    {
        Log.i(TAG, "saveFileFromPrompt");

        try {
            Activity activity = requireActivity();
            CharSequence title = activity.getText(R.string.menuItem_fileSave);
            CharSequence hint = activity.getText(R.string.hintFileSave);
            int inputType = InputType.TYPE_CLASS_TEXT;
            Prompt.promptText(activity, title, null, null, hint, inputType, (text, which) -> {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    saveState(text.toString());
                }
            });
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void saveState( final String filename )
    {
        Log.i(TAG, "saveState");

        if (!mIsRunning) {
            return;
        }

        try {
            FragmentActivity activity = requireActivity();

            mCurrentSaveStateFile = new File(mGamePrefs.getUserSaveDir() + "/" +
                    filename + "." + mRomGoodName + ".sav");

            if (mCurrentSaveStateFile.exists()) {

                String title = activity.getString(R.string.confirm_title);
                String message = activity.getString(R.string.confirmOverwriteFile_message, filename);

                ConfirmationDialog confirmationDialog =
                        ConfirmationDialog.newInstance(SAVE_STATE_FILE_CONFIRM_DIALOG_ID, title, message);

                FragmentManager fm = activity.getSupportFragmentManager();
                confirmationDialog.show(fm, SAVE_STATE_FILE_CONFIRM_DIALOG_STATE);
            } else {
                if (mCoreService != null) {
                    mCoreService.saveState(mCurrentSaveStateFile.getName());
                    Notifier.showToast(activity, R.string.toast_savingFile, mCurrentSaveStateFile.getName());
                }

                if (mCoreEventListener != null) {
                    mCoreEventListener.onSaveLoad();
                }
            }
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void loadFileFromPrompt()
    {
        Log.i(TAG, "loadFileFromPrompt");

        if (!mIsRunning) {
            return;
        }

        try {
            Activity activity = requireActivity();
            CharSequence title = activity.getText(R.string.menuItem_fileLoad);
            File startPath = new File(mGamePrefs.getUserSaveDir());
            Prompt.promptFile(activity, title, null, startPath, "", (file, which) -> {
                if (which >= 0) {
                    loadState(file);

                    if (mCoreEventListener != null) {
                        mCoreEventListener.onSaveLoad();
                    }
                }
            });
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void loadAutoSaveFromPrompt()
    {
        Log.i(TAG, "loadAutoSaveFromPrompt");

        if (!mIsRunning) {
            return;
        }

        try {
            Activity activity = requireActivity();
            CharSequence title = activity.getText(R.string.menuItem_fileLoadAutoSave);
            File startPath = new File(mGamePrefs.getAutoSaveDir());
            Prompt.promptFile(activity, title, null, startPath, "sav", (file, which) -> {
                if (which >= 0) {
                    loadState(file);

                    if (mCoreEventListener != null) {
                        mCoreEventListener.onSaveLoad();
                    }
                }
            });
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void loadState( File file )
    {
        Log.i(TAG, "loadState");

        try {
            Notifier.showToast(requireActivity(), R.string.toast_loadingFile, file.getName());
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }

        if (mCoreService != null) {
            mCoreService.loadState(file);
        }
    }

    public void autoSaveState(boolean shutdownOnFinish)
    {
        Log.i(TAG, "autoSaveState");

        if (mCoreService != null)
        {
            mCoreService.autoSaveState(shutdownOnFinish);
        }
    }

    public void screenshot()
    {
        Log.i(TAG, "screenshot");

        if (mCoreService != null)
        {
            try {
                Notifier.showToast(requireActivity(), R.string.toast_savingScreenshot);
            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
            }
            mCoreService.screenshot();
        }
    }

    public void toggleFramelimiter()
    {
        Log.i(TAG, "toggleFramelimiter");

        if (mCoreService != null)
        {
            mCoreService.toggleFramelimiter();
        }
    }

    public boolean getFramelimiter()
    {
        Log.i(TAG, "getFramelimiter");

        return mCoreService != null &&  mCoreService.getFramelimiter();
    }

    public void restart()
    {
        Log.i(TAG, "restart");

        if (mCoreService != null)
        {
            mCoreService.pauseEmulator();

            try {
                FragmentActivity activity = requireActivity();
                String title = activity.getString(R.string.confirm_title);
                String message = activity.getString(R.string.confirmResetGame_message);

                ConfirmationDialog confirmationDialog =
                        ConfirmationDialog.newInstance(RESET_CONFIRM_DIALOG_ID, title, message);

                FragmentManager fm = activity.getSupportFragmentManager();
                confirmationDialog.show(fm, RESTART_CONFIRM_DIALOG_STATE);
            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    public void restartEmulator()
    {
        Log.i(TAG, "restartEmulator");

        if (mCoreService != null)
        {
            mCoreService.restart();
        }
    }

    public void connectForNetplay(int player)
    {
        Log.i("CoreFragment", "connectForNetplay");

        if (mCoreService != null)
        {
            mCoreService.connectForNetplay(player);
        }
    }


    public void shutdownEmulator()
    {
        Log.i( TAG, "shutdownEmulator" );

        if (mCoreService != null)
        {
            mCoreService.shutdownEmulator();
        }
    }

    public CoreTypes.m64p_emu_state getState()
    {
        Log.i(TAG, "getState");

        if (mCoreService != null)
        {
            return mCoreService.getState();
        }
        else
        {
            return CoreTypes.m64p_emu_state.M64EMU_UNKNOWN;
        }
    }

    public void incrementCustomSpeed()
    {
        Log.i(TAG, "incrementCustomSpeed");

        setCustomSpeed( mCustomSpeed + DELTA_SPEED );
    }

    public void decrementCustomSpeed()
    {
        Log.i(TAG, "decrementCustomSpeed");

        setCustomSpeed( mCustomSpeed - DELTA_SPEED );
    }

    private void setCustomSpeed(int value)
    {
        Log.i(TAG, "setCustomSpeed");

        mCustomSpeed = Utility.clamp( value, MIN_SPEED, MAX_SPEED );
        mUseCustomSpeed = true;

        if(mCoreService != null)
        {
            mCoreService.setCustomSpeed(mCustomSpeed);
        }
    }

    public void updateControllerConfig(int player, boolean plugged, CoreTypes.PakType pakType)
    {
        Log.i(TAG, "updateControllerConfig");

        if(mCoreService != null)
        {
            mCoreService.updateControllerConfig(player, plugged, pakType);
        }
    }

    public int getCurrentSpeed()
    {
        return mUseCustomSpeed ? mCustomSpeed : BASELINE_SPEED;
    }

    public void setCustomSpeedFromPrompt()
    {
        Log.i(TAG, "setCustomSpeedFromPrompt");

        try {
            Activity activity = requireActivity();
            final CharSequence title = activity.getText(R.string.menuItem_setSpeed);
            Prompt.promptInteger(activity, title, "%1$d %%", mCustomSpeed, MIN_SPEED, MAX_SPEED,
                    (value, which) -> {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            setCustomSpeed(value);

                            if (mCoreEventListener != null) {
                                mCoreEventListener.onPromptFinished();
                            }
                        }
                    });
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void onPromptDialogClosed(int id, int which)
    {
        Log.i(TAG, "onPromptDialogClosed");

        if (id == SAVE_STATE_FILE_CONFIRM_DIALOG_ID)
        {
            if (mCoreService != null) {
                mCoreService.saveState(mCurrentSaveStateFile.getName());
            }
            try {
                Notifier.showToast(requireActivity(), R.string.toast_overwritingFile, mCurrentSaveStateFile.getName());
            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
            }
            if(mCoreEventListener != null)
            {
                mCoreEventListener.onSaveLoad();
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
            mAskingForExit = false;
            if(mCoreEventListener != null)
                mCoreEventListener.onExitRequested( which == DialogInterface.BUTTON_POSITIVE );
        }
    }

    public void forceExit()
    {
        Log.i(TAG, "forceExit");

        if(mCoreService != null)
        {
            mCoreService.forceExit();
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

    public PixelBuffer.SurfaceTextureWithSize getSurfaceTexture() {
        return mCoreService.getSurfaceTexture();
    }
}