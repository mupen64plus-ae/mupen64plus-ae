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

package paulscode.android.mupen64plusae;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import paulscode.android.mupen64plusae.R;

import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.task.DownloadFromGoogleDriveService;
import paulscode.android.mupen64plusae.task.DownloadFromGoogleDriveService.DownloadFilesListener;
import paulscode.android.mupen64plusae.task.DownloadFromGoogleDriveService.LocalBinder;
import paulscode.android.mupen64plusae.util.CountryCode;

@SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
public class DownloadFromGoogleDriveFragment extends Fragment implements DownloadFilesListener
{
    public interface OnFinishListener
    {
        /**
         * Will be called once extraction finishes
         */
        void onFinish();
    }

    //Progress dialog for extracting textures
    private ProgressDialog mProgress = null;

    public static class DataViewModel extends ViewModel {

        public DataViewModel() {}

        //Service connection for the progress dialog
        LocalBinder mBinder = null;

        private boolean mInProgress = false;

        String mRomMd5;
        String mRomCrc;
        String mRomHeaderName;
        String mRomGoodName;
        CountryCode mRomCountryCode;
        DownloadFromGoogleDriveFragment mCurrentFragment = null;
    }

    DataViewModel mViewModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mViewModel = new ViewModelProvider(requireActivity()).get(DataViewModel.class);
        mViewModel.mCurrentFragment = this;

        if(mViewModel.mInProgress)
        {
            CharSequence title = getString(R.string.importGoogleDriveService_importNotificationTitle);
            CharSequence message = getString(R.string.toast_pleaseWait);
            mProgress = new ProgressDialog(mProgress, requireActivity(), title, "", message, true);
            mProgress.show();

            if (mViewModel.mBinder != null) {
                DownloadFromGoogleDriveService downloadFromGoogleDriveService = mViewModel.mBinder.getService();
                downloadFromGoogleDriveService.setDownloadFilesListener(mViewModel.mCurrentFragment);
            }
        }
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
    public void onDownloadFinished()
    {
        try {
            Activity activity = requireActivity();
            if (activity instanceof OnFinishListener) {
                ((OnFinishListener) activity).onFinish();
            }
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onServiceDestroyed()
    {
        mViewModel.mInProgress = false;
        mProgress.dismiss();
    }

    @Override
    public ProgressDialog GetProgressDialog()
    {
        return mProgress;
    }

    public void downloadFromGoogleDrive(String romMd5, String romCrc, String romHeaderName, String romGoodName,  CountryCode romCountryCode)
    {
        mViewModel.mRomMd5 = romMd5;
        mViewModel.mRomCrc = romCrc;
        mViewModel.mRomHeaderName = romHeaderName;
        mViewModel.mRomGoodName = romGoodName;
        mViewModel.mRomCountryCode = romCountryCode;

        try {
            actuallyDownloadFiles(requireActivity());
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }
    }
    
    private void actuallyDownloadFiles(Activity activity)
    {
        mViewModel.mInProgress = true;

        try {
            CharSequence title = getString(R.string.importGoogleDriveService_importNotificationTitle);
            CharSequence message = getString(R.string.toast_pleaseWait);
            mProgress = new ProgressDialog(mProgress, requireActivity(), title, "", message, true);
            mProgress.show();
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }
        
        /* Defines callbacks for service binding, passed to bindService() */
        ServiceConnection serviceConnection = new ServiceConnection() {
            
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {

                // We've bound to LocalService, cast the IBinder and get LocalService instance
                mViewModel.mBinder = (LocalBinder) service;
                DownloadFromGoogleDriveService downloadFromGoogleDriveService = mViewModel.mBinder.getService();
                downloadFromGoogleDriveService.setDownloadFilesListener(mViewModel.mCurrentFragment);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
            }
        };

        // Asynchronously copy data to SD
        ActivityHelper.startDownloadFromGoogleDriveService(activity.getApplicationContext(),
                serviceConnection, mViewModel.mRomMd5, mViewModel.mRomCrc, mViewModel.mRomHeaderName,
                mViewModel.mRomGoodName, mViewModel.mRomCountryCode);
    }
    
    public boolean IsInProgress()
    {
        return mViewModel != null && mViewModel.mInProgress;
    }
}