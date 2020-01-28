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
import android.os.Vibrator;

import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.text.InputType;
import android.util.Log;
import android.view.Surface;

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
import paulscode.android.mupen64plusae.util.Utility;
import paulscode.android.mupen64plusae.jni.CoreInterface.OnFpsChangedListener;

public class CoreFragment extends Fragment implements CoreServiceListener, CoreService.RomExtractionListener
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

    private CoreEventListener mCoreEventListener = null;

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
            final String message = requireActivity().getString( R.string.toast_nativeMainFailure07 );

            requireActivity().runOnUiThread(() -> Notifier.showToast( requireActivity(), message ));

            Log.e( "CoreFragment", "Launch failure: " + message );
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

    @Override
    public void romExtractionStarted()
    {
        requireActivity().runOnUiThread(() -> {
            CharSequence title = getString( R.string.extractRomTask_title );
            CharSequence message = getString( R.string.toast_pleaseWait );

            DocumentFile file = FileUtil.getDocumentFileSingle(requireActivity(), Uri.parse(mZipPath));
            String zipName = file.getName();
            mProgress = new ProgressDialog( mProgress, requireActivity(), title, zipName, message, false );
            mProgress.show();
        });
    }

    @Override
    public void romExtractionFinished()
    {
        try {
            requireActivity().runOnUiThread(() -> mProgress.dismiss());
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void setCoreEventListener(CoreEventListener coreEventListener)
    {
        Log.i("CoreFragment", "setCoreEventListener");

        mCoreEventListener = coreEventListener;
    }

    public void startCore(GlobalPrefs globalPrefs, GamePrefs gamePrefs, String romGoodName, String romDisplayName,
                          String romPath, String zipPath, String romMd5, String romCrc, String romHeaderName, byte romCountryCode, String romArtPath,
                          boolean isRestarting)
    {
        Log.i("CoreFragment", "startCore");

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

        if(!mIsRunning)
        {
            actuallyStartCore(requireActivity());

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
                mCoreService.setRomExtractionListener(CoreFragment.this);

                if(mCoreEventListener != null)
                {
                    requireActivity().runOnUiThread(() -> mCoreEventListener.onCoreServiceStarted());
                }
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

        ActivityHelper.startCoreService(activity.getApplicationContext(), mServiceConnection, params);
    }

    @SuppressWarnings("unused")
    private void actuallyStopCore()
    {
        Log.i("CoreFragment", "actuallyStopCore");

        if(mIsRunning)
        {
            mIsRunning = false;
            requireActivity().runOnUiThread(() -> {
                mCoreService = null;
                ActivityHelper.stopCoreService(requireActivity().getApplicationContext(), mServiceConnection);
            });
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

        String title = requireActivity().getString( R.string.confirm_title );
        String message = requireActivity().getString( R.string.confirmExitGame_message );

        ConfirmationDialog confirmationDialog =
                ConfirmationDialog.newInstance(EXIT_CONFIRM_DIALOG_ID, title, message);

        FragmentManager fm = requireActivity().getSupportFragmentManager();
        confirmationDialog.show(fm, EXIT_CONFIRM_DIALOG_STATE);
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
        mCoreService.setCustomSpeed( speed );
    }


    public void saveSlot()
    {
        Log.i("CoreFragment", "saveSlot");

        if (mCoreService != null)
        {
            int slot = mCoreService.getSlot();

            Notifier.showToast( requireActivity(), R.string.toast_savingSlot, slot );

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
            
            Notifier.showToast( requireActivity(), R.string.toast_loadingSlot, slot );

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

        final CharSequence title = requireActivity().getString(R.string.menuItem_selectSlot);

        Prompt.promptRadioInteger( requireActivity(), title, mCoreService.getSlot(), 0, 2, 5,
                (value, which) -> {
                    if( which == DialogInterface.BUTTON_POSITIVE )
                    {
                        if (mCoreService != null) {
                            mCoreService.setSlot(value);
                        }

                        if(mCoreEventListener != null)
                        {
                            mCoreEventListener.onPromptFinished();
                        }
                    }
                });
    }

    public void saveFileFromPrompt()
    {
        Log.i("CoreFragment", "saveFileFromPrompt");

        CharSequence title = requireActivity().getText( R.string.menuItem_fileSave );
        CharSequence hint = requireActivity().getText( R.string.hintFileSave );
        int inputType = InputType.TYPE_CLASS_TEXT;
        Prompt.promptText( requireActivity(), title, null, null, hint, inputType, (text, which) -> {
            if( which == DialogInterface.BUTTON_POSITIVE )
            {
                saveState(text.toString());
            }
        });
    }

    private void saveState( final String filename )
    {
        Log.i("CoreFragment", "saveState");

        mCurrentSaveStateFile = new File( mGamePrefs.getUserSaveDir() + "/" +
                filename + "." + mRomGoodName + ".sav");

        if( mCurrentSaveStateFile.exists() )
        {

            String title = requireActivity().getString( R.string.confirm_title );
            String message = requireActivity().getString( R.string.confirmOverwriteFile_message, filename );

            ConfirmationDialog confirmationDialog =
                    ConfirmationDialog.newInstance(SAVE_STATE_FILE_CONFIRM_DIALOG_ID, title, message);

            FragmentManager fm = requireActivity().getSupportFragmentManager();
            confirmationDialog.show(fm, SAVE_STATE_FILE_CONFIRM_DIALOG_STATE);
        }
        else
        {
            if (mCoreService != null) {
                mCoreService.saveState(mCurrentSaveStateFile.getName());
                Notifier.showToast( requireActivity(), R.string.toast_savingFile, mCurrentSaveStateFile.getName() );
            }

            if(mCoreEventListener != null)
            {
                mCoreEventListener.onSaveLoad();
            }
        }
    }

    public void loadFileFromPrompt()
    {
        Log.i("CoreFragment", "loadFileFromPrompt");

        CharSequence title = requireActivity().getText( R.string.menuItem_fileLoad );
        File startPath = new File( mGamePrefs.getUserSaveDir() );
        Prompt.promptFile( requireActivity(), title, null, startPath, "", (file, which) -> {
            if( which >= 0 )
            {
                loadState(file);

                if(mCoreEventListener != null)
                {
                    mCoreEventListener.onSaveLoad();
                }
            }
        });
    }

    public void loadAutoSaveFromPrompt()
    {
        Log.i("CoreFragment", "loadAutoSaveFromPrompt");

        CharSequence title = requireActivity().getText( R.string.menuItem_fileLoadAutoSave );
        File startPath = new File( mGamePrefs.getAutoSaveDir() );
        Prompt.promptFile( requireActivity(), title, null, startPath, "sav", (file, which) -> {
            if( which >= 0 )
            {
                loadState(file);

                if(mCoreEventListener != null)
                {
                    mCoreEventListener.onSaveLoad();
                }
            }
        });
    }

    private void loadState( File file )
    {
        Log.i("CoreFragment", "loadState");

        Notifier.showToast( requireActivity(), R.string.toast_loadingFile, file.getName() );

        if (mCoreService != null) {
            mCoreService.loadState(file);
        }
    }

    public void autoSaveState(boolean shutdownOnFinish)
    {
        Log.i("CoreFragment", "autoSaveState");

        if (mCoreService != null)
        {
            mCoreService.autoSaveState(shutdownOnFinish);
        }
    }

    public void screenshot()
    {
        Log.i("CoreFragment", "screenshot");

        if (mCoreService != null)
        {
            Notifier.showToast( requireActivity(), R.string.toast_savingScreenshot );
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
            mCoreService.pauseEmulator();
            String title = requireActivity().getString( R.string.confirm_title );
            String message = requireActivity().getString( R.string.confirmResetGame_message );

            ConfirmationDialog confirmationDialog =
                    ConfirmationDialog.newInstance(RESET_CONFIRM_DIALOG_ID, title, message);

            FragmentManager fm = requireActivity().getSupportFragmentManager();
            confirmationDialog.show(fm, RESTART_CONFIRM_DIALOG_STATE);
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

    public CoreTypes.m64p_emu_state getState()
    {
        Log.i("CoreFragment", "getState");

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
        Log.i("CoreFragment", "incrementCustomSpeed");

        setCustomSpeed( mCustomSpeed + DELTA_SPEED );
    }

    public void decrementCustomSpeed()
    {
        Log.i("CoreFragment", "decrementCustomSpeed");

        setCustomSpeed( mCustomSpeed - DELTA_SPEED );
    }

    private void setCustomSpeed(int value)
    {
        Log.i("CoreFragment", "setCustomSpeed");

        mCustomSpeed = Utility.clamp( value, MIN_SPEED, MAX_SPEED );
        mUseCustomSpeed = true;

        if(mCoreService != null)
        {
            mCoreService.setCustomSpeed(mCustomSpeed);
        }
    }

    public void updateControllerConfig(int player, boolean plugged, CoreTypes.PakType pakType)
    {
        Log.i("CoreFragment", "updateControllerConfig");

        if(mCoreService != null)
        {
            mCoreService.updateControllerConfig(player, plugged, pakType);
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

        final CharSequence title = requireActivity().getText( R.string.menuItem_setSpeed );
        Prompt.promptInteger( requireActivity(), title, "%1$d %%", mCustomSpeed, MIN_SPEED, MAX_SPEED,
                (value, which) -> {
                    if( which == DialogInterface.BUTTON_POSITIVE )
                    {
                        setCustomSpeed( value );

                        if(mCoreEventListener != null)
                        {
                            mCoreEventListener.onPromptFinished();
                        }
                    }
                });
    }

    public void onPromptDialogClosed(int id, int which)
    {
        Log.i("CoreFragment", "onPromptDialogClosed");

        if (id == SAVE_STATE_FILE_CONFIRM_DIALOG_ID)
        {
            if (mCoreService != null) {
                mCoreService.saveState(mCurrentSaveStateFile.getName());
            }

            Notifier.showToast(requireActivity(), R.string.toast_overwritingFile, mCurrentSaveStateFile.getName());
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

    public void forceExit()
    {
        Log.i("CoreFragment", "forceExit");

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
}