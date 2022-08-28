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
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.net.InetAddress;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.StartCoreServiceParams;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog;
import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.jni.CoreService.CoreServiceListener;
import paulscode.android.mupen64plusae.jni.CoreService.LocalBinder;
import paulscode.android.mupen64plusae.netplay.NetplayFragment;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.PixelBuffer;
import paulscode.android.mupen64plusae.util.Utility;

public class CoreFragment extends Fragment implements CoreServiceListener, CoreService.LoadingDataListener
{
    public interface CoreEventListener
    {
        /**
         * Will be called once the core service is valid
         */
        void onCoreServiceStarted();

        /**
         * Will be called once the game has started
         */
        void onGameStarted();

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
         * Called when a setting has changed the game's surface
         */
        void onRecreateSurface();

        /**
         * Called when we should exit the application
         */
        void onExitFinished();

        /**
         * Called when the service is bound
         */
        void onBindService();

        /**
         * Called when the frame rate has changed.
         *
         * @param newValue The new FPS value.
         */
        void onFpsChanged( int newValue );
    }
    
    private static final String TAG = "CoreFragment";

    private static final String SAVE_STATE_FILE_CONFIRM_DIALOG_STATE = "SAVE_STATE_FILE_CONFIRM_DIALOG_STATE";
    private static final String RESTART_CONFIRM_DIALOG_STATE = "RESTART_CONFIRM_DIALOG_STATE";
    private static final String EXIT_CONFIRM_DIALOG_STATE = "RESTART_CONFIRM_DIALOG_STATE";

    private static final int SAVE_STATE_FILE_CONFIRM_DIALOG_ID = 3;
    private static final int RESET_CONFIRM_DIALOG_ID = 4;
    private static final int EXIT_CONFIRM_DIALOG_ID = 5;

    //Progress dialog for extracting ROMs
    private ProgressDialog mProgress = null;

    // Speed info - used internally
    public static final int BASELINE_SPEED = 100;
    private static final int DEFAULT_SPEED = 250;
    private static final int MAX_SPEED = 300;
    private static final int MIN_SPEED = 10;
    private static final int DELTA_SPEED = 10;

    public static class DataViewModel extends ViewModel {

        public DataViewModel() { }

        //Service connection for the progress dialog
        ServiceConnection mServiceConnection;
        CoreService.LocalBinder mBinder;

        GamePrefs mGamePrefs = null;
        String mRomGoodName = null;
        String mRomDisplayName = null;
        String mRomPath = null;
        String mZipPath = null;
        String mRomMd5 = null;
        String mRomCrc = null;
        String mRomHeaderName = null;
        byte mRomCountryCode = 0;
        String mRomArtPath = null;
        boolean mIsRestarting = false;
        boolean mUseRaphnetIfAvailable = false;
        int mVideoRenderWidth = 0;
        int mVideoRenderHeight = 0;
        boolean mUsingNetplay = false;
        boolean mIsRunning = false;
        File mCurrentSaveStateFile = null;
        boolean mUseCustomSpeed = false;
        int mCustomSpeed = DEFAULT_SPEED;
        boolean mAskingForExit = false;
        boolean mLoadingInProgress = false;
        boolean mSettingsReset = false;
        boolean mResolutionReset = false;
        CoreFragment mCurrentFragment = null;
    }

    DataViewModel mViewModel;

    private CoreService mCoreService = null;

    private CoreEventListener mCoreEventListener = null;

    private boolean mRecreateSurface = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        Log.i(TAG, "onAttach");

        Activity activity = requireActivity();
        mViewModel = new ViewModelProvider(requireActivity()).get(CoreFragment.DataViewModel.class);
        mViewModel.mCurrentFragment = this;

        if (mViewModel.mLoadingInProgress) {
            CharSequence title = activity.getString( R.string.extractRomTask_title );
            CharSequence message = activity.getString( R.string.toast_pleaseWait );

            String displayName;

            if (mViewModel.mZipPath != null) {
                DocumentFile file = FileUtil.getDocumentFileSingle(activity, Uri.parse(mViewModel.mZipPath));
                displayName = file == null ? mViewModel.mRomDisplayName : file.getName();
            } else {
                displayName = mViewModel.mRomDisplayName;
            }

            mProgress = new ProgressDialog( mProgress, activity, title, displayName, message, false );
            mProgress.show();
        }

        if (mViewModel.mBinder != null) {
            Log.i(TAG, "Assigning service");
            mCoreService = mViewModel.mBinder.getService();
            mCoreService.setCoreServiceListener(mViewModel.mCurrentFragment);
            mCoreService.setLoadingDataListener(mViewModel.mCurrentFragment);
        }
    }

    @Override
    public void onDetach()
    {
        Log.i(TAG, "onDetach");

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

    public void resetCoreServiceAppData(){
        if(mCoreService != null)
            mCoreService.resetAppData();
    }

    public void resetCoreServiceControllers(){
        if(mCoreService != null)
            mCoreService.resetControllers();
    }

    public void resetCoreServiceControllersNetplay(NetplayFragment fragment){
        if(mCoreService != null)
            mCoreService.resetControllersNetplay(fragment);
    }

    @Override
    public void onFinish()
    {
        Log.i(TAG, "onFinish");

        try {
            requireActivity().runOnUiThread(() -> {
                if (mCoreEventListener != null) {
                    mCoreEventListener.onExitFinished();
                }
            });
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }

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

        mViewModel.mIsRunning = false;
    }

    @Override
    public void onFpsChanged(int newValue) {

        try {
            requireActivity().runOnUiThread(() -> {
                if (mCoreEventListener != null) {
                    mCoreEventListener.onFpsChanged(newValue);
                }
            });
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
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

                if (mViewModel.mZipPath != null) {
                    DocumentFile file = FileUtil.getDocumentFileSingle(activity, Uri.parse(mViewModel.mZipPath));
                    displayName = file == null ? mViewModel.mRomDisplayName : file.getName();
                } else {
                    displayName = mViewModel.mRomDisplayName;
                }
                mProgress = new ProgressDialog( mProgress, activity, title, displayName, message, false );
                mProgress.show();

                mViewModel.mLoadingInProgress = true;
            });
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadingFinished()
    {
        mViewModel.mLoadingInProgress = false;

        try {
            Activity activity = requireActivity();
            activity.runOnUiThread(() -> {
                if (mProgress != null) {
                    mProgress.dismiss();
                    if (mCoreEventListener != null) {
                        mCoreEventListener.onGameStarted();
                    }
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
                          boolean isRestarting, boolean settingsReset, boolean resolutionReset, int videoRenderWidth, int videoRenderHeight,
                          boolean usingNetplay)
    {
        Log.i(TAG, "startCore");

        mViewModel.mGamePrefs = gamePrefs;
        mViewModel.mRomGoodName = romGoodName;
        mViewModel.mRomDisplayName = romDisplayName;
        mViewModel.mRomPath = romPath;
        mViewModel.mZipPath = zipPath;
        mViewModel.mIsRestarting = isRestarting;
        mViewModel.mSettingsReset = settingsReset;
        mViewModel.mResolutionReset = resolutionReset;
        mViewModel.mRomMd5 = romMd5;
        mViewModel.mRomCrc = romCrc;
        mViewModel.mRomHeaderName = romHeaderName;
        mViewModel.mRomCountryCode = romCountryCode;
        mViewModel.mRomArtPath = romArtPath;
        mViewModel.mUseRaphnetIfAvailable = globalPrefs.useRaphnetDevicesIfAvailable && RaphnetControllerHandler.raphnetDevicesPresent(getContext());
        mViewModel.mVideoRenderWidth = videoRenderWidth;
        mViewModel.mVideoRenderHeight = videoRenderHeight;
        mViewModel.mUsingNetplay = usingNetplay;

        if(!mViewModel.mIsRunning)
        {
            try {
                actuallyStartCore(requireActivity());
            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
            }

            mViewModel.mIsRunning = true;
        }
    }

    private void actuallyStartCore(Activity activity)
    {
        Log.i(TAG, "actuallyStartCore");

        // Defines callbacks for service binding, passed to bindService()
        mViewModel.mServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.i(TAG, "onServiceConnected");
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                mViewModel.mBinder = (LocalBinder) service;
                mViewModel.mCurrentFragment.mCoreService = mViewModel.mBinder.getService();

                mViewModel.mCurrentFragment.mCoreService.setCoreServiceListener(mViewModel.mCurrentFragment);
                mViewModel.mCurrentFragment.mCoreService.setLoadingDataListener(mViewModel.mCurrentFragment);

                if (mViewModel.mCurrentFragment.mCoreEventListener != null) {
                    mViewModel.mCurrentFragment.mCoreEventListener.onBindService();
                }

                if(mRecreateSurface) {
                    mCoreEventListener.onRecreateSurface();//mViewModel?
                    mRecreateSurface = false;
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
            }
        };

        // Start the core
        StartCoreServiceParams params = new StartCoreServiceParams();
        params.setRomGoodName(mViewModel.mRomGoodName);
        params.setRomDisplayName(mViewModel.mRomDisplayName);
        params.setRomPath(mViewModel.mRomPath);
        params.setZipPath(mViewModel.mZipPath);
        params.setRomMd5(mViewModel.mRomMd5);
        params.setRomCrc(mViewModel.mRomCrc);
        params.setRomHeaderName(mViewModel.mRomHeaderName);
        params.setRomCountryCode(mViewModel.mRomCountryCode);
        params.setRomArtPath(mViewModel.mRomArtPath);
        params.setRestarting(mViewModel.mIsRestarting);
        params.setSettingsReset(mViewModel.mSettingsReset);
        params.setResolutionReset(mViewModel.mResolutionReset);
        params.setUseRaphnetDevicesIfAvailable(mViewModel.mUseRaphnetIfAvailable);
        params.setVideoRenderWidth(mViewModel.mVideoRenderWidth);
        params.setVideoRenderHeight(mViewModel.mVideoRenderHeight);
        params.setUsingNetplay(mViewModel.mUsingNetplay);

        ActivityHelper.startCoreService(activity.getApplicationContext(), mViewModel.mServiceConnection, params);
    }

    public void resumeEmulator()
    {
        Log.i(TAG, "resumeEmulator");

        if(mCoreService != null && !mViewModel.mAskingForExit)
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

    public void setControllerState( int controllerNum, boolean[] buttons, double axisX, double axisY, boolean isDigital )
    {
        if(mCoreService != null)
        {
            mCoreService.setControllerState( controllerNum, buttons, axisX, axisY, isDigital );
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
        else
        {
            Log.i(TAG, "core service is NULL");
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

        if (mCoreService == null) {
            Log.i(TAG, "Core service is null");
        }

        return mCoreService != null && mCoreService.isShuttingDown();
    }

    public void exit()
    {
        Log.i(TAG, "exit");

        if (mCoreService != null) {
            mCoreService.pauseEmulator();
        }

        try {
            mViewModel.mAskingForExit = true;
            FragmentActivity activity = requireActivity();
            String title = activity.getString(R.string.confirm_title);
            String message = activity.getString(R.string.confirmExitGame_message);

            ConfirmationDialog confirmationDialog =
                    ConfirmationDialog.newInstance(EXIT_CONFIRM_DIALOG_ID, title, message);

            FragmentManager fm = activity.getSupportFragmentManager();
            confirmationDialog.show(fm, EXIT_CONFIRM_DIALOG_STATE);
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
            mViewModel.mAskingForExit = false;
        }
    }

    public void toggleSpeed()
    {
        Log.i(TAG, "toggleSpeed");

        if (mCoreService != null)
        {
            mViewModel.mUseCustomSpeed = !mViewModel.mUseCustomSpeed;
            int speed = mViewModel.mUseCustomSpeed ? mViewModel.mCustomSpeed : BASELINE_SPEED;
            mCoreService.setCustomSpeed(speed);
        }
    }

    public void fastForward( boolean pressed )
    {
        Log.i(TAG, "fastForward");
        if (mCoreService != null)
        {
            int speed = pressed ? mViewModel.mCustomSpeed : BASELINE_SPEED;
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

    public int getAudioInit()
    {
        if(mCoreService == null)
            return 0;
        return mCoreService.getAudioInit();
    }

    public int getEmuModeInit()
    {
        if(mCoreService == null)
            return 0;
        return mCoreService.getEmuModeInit();
    }

    public void incrementSlot()
    {
        Log.i(TAG, "incrementSlot");

        if (mCoreService != null)
        {
            int slot = mCoreService.getSlot();
            mCoreService.setSlot( slot + 1 );

            try {
                Notifier.showToast( requireActivity(), R.string.toast_movingSlot, mCoreService.getSlot() );
            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    public void decrementSlot()
    {
        Log.i(TAG, "decrementSlot");

        if (mCoreService != null)
        {
            int slot = mCoreService.getSlot();

            // If we are currently at slot 0 then we need to go back to the last slot.
            if(slot == 0)
                slot = mCoreService.getSlotQuantity();

            mCoreService.setSlot( slot - 1 );

            try {
                Notifier.showToast( requireActivity(), R.string.toast_movingSlot, mCoreService.getSlot() );
            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
            }
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

        if (!mViewModel.mIsRunning) {
            return;
        }

        try {
            FragmentActivity activity = requireActivity();

            mViewModel.mCurrentSaveStateFile = new File(mViewModel.mGamePrefs.getUserSaveDir() + "/" +
                    filename + "." + mViewModel.mRomGoodName + ".sav");

            if (mViewModel.mCurrentSaveStateFile.exists()) {

                String title = activity.getString(R.string.confirm_title);
                String message = activity.getString(R.string.confirmOverwriteFile_message, filename);

                ConfirmationDialog confirmationDialog =
                        ConfirmationDialog.newInstance(SAVE_STATE_FILE_CONFIRM_DIALOG_ID, title, message);

                FragmentManager fm = activity.getSupportFragmentManager();
                confirmationDialog.show(fm, SAVE_STATE_FILE_CONFIRM_DIALOG_STATE);
            } else {
                if (mCoreService != null) {
                    mCoreService.saveState(mViewModel.mCurrentSaveStateFile.getName());
                    Notifier.showToast(activity, R.string.toast_savingFile, mViewModel.mCurrentSaveStateFile.getName());
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

        if (!mViewModel.mIsRunning) {
            return;
        }

        try {
            Activity activity = requireActivity();
            CharSequence title = activity.getText(R.string.menuItem_fileLoad);
            File startPath = new File(mViewModel.mGamePrefs.getUserSaveDir());
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

        if (!mViewModel.mIsRunning) {
            return;
        }

        try {
            Activity activity = requireActivity();
            CharSequence title = activity.getText(R.string.menuItem_fileLoadAutoSave);
            File startPath = new File(mViewModel.mGamePrefs.getAutoSaveDir());
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

    public boolean checkOnStateCallbackListeners(){
        if(mCoreService == null)
            return false;
        return mCoreService.checkOnStateCallbackListeners();
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

    public void connectForNetplay(int regId, int player, String videoPlugin, String rspPlugin, InetAddress address, int port)
    {
        Log.i("CoreFragment", "connectForNetplay");

        if (mCoreService != null)
        {
            mCoreService.connectForNetplay(regId, player, videoPlugin, rspPlugin, address, port);
        }
    }

    public void startNetplay()
    {
        Log.i("CoreFragment", "startNetplay");

        if (mCoreService != null)
        {
            mCoreService.startNetplay();
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

        setCustomSpeed( mViewModel.mCustomSpeed + DELTA_SPEED );
    }

    public void decrementCustomSpeed()
    {
        Log.i(TAG, "decrementCustomSpeed");

        setCustomSpeed( mViewModel.mCustomSpeed - DELTA_SPEED );
    }

    private void setCustomSpeed(int value)
    {
        Log.i(TAG, "setCustomSpeed");

        mViewModel.mCustomSpeed = Utility.clamp( value, MIN_SPEED, MAX_SPEED );
        mViewModel.mUseCustomSpeed = true;

        if(mCoreService != null)
        {
            mCoreService.setCustomSpeed(mViewModel.mCustomSpeed);
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
        return mViewModel != null && mViewModel.mUseCustomSpeed ?
                mViewModel.mCustomSpeed : BASELINE_SPEED;
    }

    public void setCustomSpeedFromPrompt()
    {
        Log.i(TAG, "setCustomSpeedFromPrompt");

        try {
            Activity activity = requireActivity();
            final CharSequence title = activity.getText(R.string.menuItem_setSpeed);
            Prompt.promptInteger(activity, title, "%1$d %%", mViewModel.mCustomSpeed, MIN_SPEED, MAX_SPEED,
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
                mCoreService.saveState(mViewModel.mCurrentSaveStateFile.getName());
            }
            try {
                Notifier.showToast(requireActivity(), R.string.toast_overwritingFile, mViewModel.mCurrentSaveStateFile.getName());
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
            mViewModel.mAskingForExit = false;
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
        return mViewModel.mIsRunning;
    }

    public boolean hasServiceStarted()
    {
        return mCoreService != null;
    }

    public PixelBuffer.SurfaceTextureWithSize getSurfaceTexture() {
        return mCoreService != null ? mCoreService.getSurfaceTexture() : null;
    }

    public void setRecreateSurface(boolean recreateSurface){ mRecreateSurface = recreateSurface; }

    public int getVolume(){
        if(mCoreService == null)
            return 0;
        else
            return mCoreService.getVolume();
    }

    public void setVolume(int volume){
        if(mCoreService != null)
            mCoreService.setVolume(volume);
    }

    public void setResolutionReset(boolean resolutionReset){
        mViewModel.mResolutionReset = resolutionReset;
        if(mCoreService != null)
            mCoreService.setResolutionReset(resolutionReset);
    }

    public void setResolution(int resolution){
        int width = (resolution >> 16) & 0xffff;
        int height = resolution & 0xffff;
        if(width > 0 && height > 0) {
            mViewModel.mVideoRenderWidth = width;
            mViewModel.mVideoRenderHeight = height;
        }
        if(mCoreService != null)
            mCoreService.setResolution(resolution);
    }
}