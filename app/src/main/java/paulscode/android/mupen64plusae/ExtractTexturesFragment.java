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
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.task.ExtractTexturesService;
import paulscode.android.mupen64plusae.task.ExtractTexturesService.ExtractTexturesListener;
import paulscode.android.mupen64plusae.task.ExtractTexturesService.LocalBinder;
import paulscode.android.mupen64plusae.util.FileUtil;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ExtractTexturesFragment extends Fragment implements ExtractTexturesListener
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
        private ServiceConnection mServiceConnection;
        LocalBinder mBinder = null;

        private Uri mTexturesFile = null;
        private boolean mInProgress = false;
        ExtractTexturesFragment mCurrentFragment = null;
    }

    DataViewModel mViewModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mViewModel = new ViewModelProvider(requireActivity()).get(DataViewModel.class);
        mViewModel.mCurrentFragment = this;

        if(mViewModel.mInProgress)
        {
            Activity activity = requireActivity();
            CharSequence title = getString(R.string.pathHiResTexturesTask_title);
            CharSequence message = getString(R.string.toast_pleaseWait);
            DocumentFile file = FileUtil.getDocumentFileSingle(activity, mViewModel.mTexturesFile);
            mProgress = new ProgressDialog(mProgress, activity, title, file == null ? "" : file.getName(), message, true);
            mProgress.show();

            if (mViewModel.mBinder != null) {
                mViewModel.mBinder.getService().setExtractTexturesListener(mViewModel.mCurrentFragment);
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
    public void onExtractTexturesFinished()
    {
        try {
            Activity activity = requireActivity();
            if (activity instanceof OnFinishListener) {
                ((OnFinishListener)activity).onFinish();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onExtractTexturesServiceDestroyed()
    {
        mViewModel.mInProgress = false;
        mProgress.dismiss();
    }

    @Override
    public ProgressDialog GetProgressDialog()
    {
        return mProgress;
    }

    public void extractTextures( Uri textureFile )
    {
        if (mViewModel != null) {
            mViewModel.mTexturesFile = textureFile;

            try {
                actuallyExtractTextures(requireActivity());
            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void actuallyExtractTextures(Activity activity)
    {
        mViewModel.mInProgress = true;
        
        CharSequence title = getString( R.string.pathHiResTexturesTask_title );
        CharSequence message = getString( R.string.toast_pleaseWait );
        DocumentFile file = FileUtil.getDocumentFileSingle(activity, mViewModel.mTexturesFile);
        mProgress = new ProgressDialog( mProgress, activity, title, file == null ? "" : file.getName(), message, true );
        mProgress.show();
        
        /* Defines callbacks for service binding, passed to bindService() */
        mViewModel.mServiceConnection = new ServiceConnection() {
            
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {

                // We've bound to LocalService, cast the IBinder and get LocalService instance
                mViewModel.mBinder = (LocalBinder) service;
                ExtractTexturesService extractTexturesService = mViewModel.mBinder.getService();
                extractTexturesService.setExtractTexturesListener(mViewModel.mCurrentFragment);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
            }
        };

        // Asynchronously extract textures
        ActivityHelper.startExtractTexturesService(activity.getApplicationContext(), mViewModel.mServiceConnection,
                mViewModel.mTexturesFile);
    }
    
    public boolean IsInProgress()
    {
        return mViewModel != null && mViewModel.mInProgress;
    }
}