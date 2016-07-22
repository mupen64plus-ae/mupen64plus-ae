/**
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

import java.io.File;

import org.mupen64plusae.v3.fzurita.R;

import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.task.ExtractTexturesService;
import paulscode.android.mupen64plusae.task.ExtractTexturesService.ExtractTexturesListener;
import paulscode.android.mupen64plusae.task.ExtractTexturesService.LocalBinder;
import android.app.Activity;
import android.support.v4.app.Fragment;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

public class ExtractTexturesFragment extends Fragment implements ExtractTexturesListener
{    
    //Progress dialog for extracting textures
    private ProgressDialog mProgress = null;
    
    //Service connection for the progress dialog
    private ServiceConnection mServiceConnection;
    
    private boolean mCachedExtractTextures = false;
    
    private boolean mCachedExtractFinish = false;
    
    private File mTexturesZipPath = null;
    
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
            CharSequence title = getString( R.string.pathHiResTexturesTask_title );
            CharSequence message = getString( R.string.toast_pleaseWait );
            mProgress = new ProgressDialog( mProgress, getActivity(), title, mTexturesZipPath.getAbsolutePath(), message, true );
            mProgress.show();
        }
        
        if(mCachedExtractTextures)
        {
            actuallyExtractTextures(getActivity());
            mCachedExtractTextures = false;
        }
        
        if(mCachedExtractFinish)
        {
            ActivityHelper.stopExtractTexturesService(getActivity().getApplicationContext(), mServiceConnection);
            mCachedExtractFinish = false;
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
        if(mServiceConnection != null && mInProgress)
        {
            ActivityHelper.stopExtractTexturesService(getActivity().getApplicationContext(), mServiceConnection);
        }
        
        super.onDestroy();
    }

    @Override
    public void onExtractTexturesFinished()
    {
        if(getActivity() != null)
        {
            ActivityHelper.stopExtractTexturesService(getActivity().getApplicationContext(), mServiceConnection);
        }
        else
        {
            mCachedExtractFinish = true;
        }

    }
    
    @Override
    public void onExtractTexturesServiceDestroyed()
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

    public void extractTextures( File texturesZipPath )
    {
        this.mTexturesZipPath = texturesZipPath;
        
        if(getActivity() != null)
        {
            actuallyExtractTextures(getActivity());
        }
        else
        {
            mCachedExtractTextures = true;
        }
    }
    
    private void actuallyExtractTextures(Activity activity)
    {
        mInProgress = true;
        
        CharSequence title = getString( R.string.pathHiResTexturesTask_title );
        CharSequence message = getString( R.string.toast_pleaseWait );
        mProgress = new ProgressDialog( mProgress, getActivity(), title, mTexturesZipPath.getAbsolutePath(), message, true );
        mProgress.show();
        
        /** Defines callbacks for service binding, passed to bindService() */
        mServiceConnection = new ServiceConnection() {
            
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {

                // We've bound to LocalService, cast the IBinder and get LocalService instance
                LocalBinder binder = (LocalBinder) service;
                ExtractTexturesService extractTexturesService = binder.getService();
                extractTexturesService.setExtractTexturesListener(ExtractTexturesFragment.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
            }
        };

        // Asynchronously extract textures
        ActivityHelper.startExtractTexturesService(activity.getApplicationContext(), mServiceConnection,
            mTexturesZipPath.getAbsolutePath());
    }
    
    public boolean IsInProgress()
    {
        return mInProgress;
    }
}