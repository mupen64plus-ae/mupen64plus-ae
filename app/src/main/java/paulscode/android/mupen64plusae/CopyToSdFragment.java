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
import android.net.Uri;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;

import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.task.CopyToSdService;
import paulscode.android.mupen64plusae.task.CopyToSdService.CopyFilesListener;
import paulscode.android.mupen64plusae.task.CopyToSdService.LocalBinder;

@SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
public class CopyToSdFragment extends Fragment implements CopyFilesListener
{
    public interface OnFinishListener
    {
        /**
         * Will be called once extraction finishes
         */
        void onFinish();
    }

    //Progress dialog to show progress
    private ProgressDialog mProgress = null;

    public static class DataViewModel extends ViewModel {

        public DataViewModel() {}

        //Service connection for the progress dialog
        private ServiceConnection mServiceConnection;

        private File mSource = null;
        private Uri mDestination = null;
        private boolean mInProgress = false;
    }

    DataViewModel mViewModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            mViewModel = new ViewModelProvider(requireActivity()).get(DataViewModel.class);

            if(mViewModel.mInProgress)
            {
                Activity activity = requireActivity();
                CharSequence title = getString(R.string.importExportActivity_exportDialogTitle);
                CharSequence message = getString(R.string.toast_pleaseWait);
                mProgress = new ProgressDialog(mProgress, activity, title, "", message, true);
                mProgress.show();
            }

        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
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
    public void onCopyToSdFinished()
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
    public void onCopyToSdServiceDestroyed()
    {
        mViewModel.mInProgress = false;
        mProgress.dismiss();
    }

    @Override
    public ProgressDialog GetProgressDialog()
    {
        return mProgress;
    }

    public void copyToSd( File source, Uri destination )
    {
        if (mViewModel != null) {
            mViewModel.mSource = source;
            mViewModel.mDestination = destination;
            try {
                actuallyCopyFiles(requireActivity());
            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void actuallyCopyFiles(Activity activity)
    {
        mViewModel.mInProgress = true;
        
        CharSequence title = getString( R.string.importExportActivity_exportDialogTitle );
        CharSequence message = getString( R.string.toast_pleaseWait );
        mProgress = new ProgressDialog( mProgress, activity, title, "", message, true );
        mProgress.show();
        
        /* Defines callbacks for service binding, passed to bindService() */
        mViewModel.mServiceConnection = new ServiceConnection() {
            
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {

                // We've bound to LocalService, cast the IBinder and get LocalService instance
                LocalBinder binder = (LocalBinder) service;
                CopyToSdService copyToSdService = binder.getService();
                copyToSdService.setCopyToSdListener(CopyToSdFragment.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
            }
        };

        // Asynchronously copy data to SD
        ActivityHelper.startCopyToSdService(activity.getApplicationContext(), mViewModel.mServiceConnection,
                mViewModel.mSource, mViewModel.mDestination);
    }
    
    public boolean IsInProgress()
    {
        return mViewModel != null && mViewModel.mInProgress;
    }
}