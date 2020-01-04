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
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import androidx.fragment.app.Fragment;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;

import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.task.CopyFromSdService;
import paulscode.android.mupen64plusae.task.CopyFromSdService.CopyFilesListener;
import paulscode.android.mupen64plusae.task.CopyFromSdService.LocalBinder;

@SuppressWarnings({"unused", "WeakerAccess"})
public class CopyFromSdFragment extends Fragment implements CopyFilesListener
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
    
    //Service connection for the progress dialog
    private ServiceConnection mServiceConnection;
    
    private boolean mCachedCopyFiles = false;

    private Uri mSource = null;
    private File mDestination = null;
    
    private boolean mInProgress = false;

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        
        if(mInProgress)
        {
            CharSequence title = getString( R.string.importExportActivity_importDialogTitle );
            CharSequence message = getString( R.string.toast_pleaseWait );
            mProgress = new ProgressDialog( mProgress, getActivity(), title, "", message, true );
            mProgress.show();
        }
        
        if(mCachedCopyFiles && getActivity() != null)
        {
            actuallyCopyFiles(getActivity());
            mCachedCopyFiles = false;
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
    public void onDestroy()
    {        
        if(mServiceConnection != null && mInProgress && getActivity() != null)
        {
            ActivityHelper.stopCopyFromSdService(getActivity().getApplicationContext(), mServiceConnection);
        }
        
        super.onDestroy();
    }

    @Override
    public void onCopyFromSdFinished()
    {
        if (getActivity() != null && getActivity() instanceof OnFinishListener) {
            ((OnFinishListener)getActivity()).onFinish();
        }
    }
    
    @Override
    public void onCopyFromSdServiceDestroyed()
    {
        mInProgress = false;
        
        if(getActivity() != null)
        {
            mProgress.dismiss();
        }
    }

    @Override
    public ProgressDialog GetProgressDialog()
    {
        return mProgress;
    }

    public void copyFromSd( Uri source, File destination )
    {
        this.mSource = source;
        this.mDestination = destination;
        
        if(getActivity() != null)
        {
            actuallyCopyFiles(getActivity());
        }
        else
        {
            mCachedCopyFiles = true;
        }
    }
    
    private void actuallyCopyFiles(Activity activity)
    {
        mInProgress = true;
        
        CharSequence title = getString( R.string.importExportActivity_importDialogTitle );
        CharSequence message = getString( R.string.toast_pleaseWait );
        mProgress = new ProgressDialog( mProgress, getActivity(), title, "", message, true );
        mProgress.show();
        
        /* Defines callbacks for service binding, passed to bindService() */
        mServiceConnection = new ServiceConnection() {
            
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {

                // We've bound to LocalService, cast the IBinder and get LocalService instance
                LocalBinder binder = (LocalBinder) service;
                CopyFromSdService copyFromSdService = binder.getService();
                copyFromSdService.setCopyFromSdListener(CopyFromSdFragment.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
            }
        };

        // Asynchronously copy data to SD
        ActivityHelper.startCopyFromSdService(activity.getApplicationContext(), mServiceConnection,
                mSource, mDestination);
    }
    
    public boolean IsInProgress()
    {
        return mInProgress;
    }
}