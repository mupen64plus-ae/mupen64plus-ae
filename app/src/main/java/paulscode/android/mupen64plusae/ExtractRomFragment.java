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
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.task.ExtractRomService;
import paulscode.android.mupen64plusae.task.ExtractRomService.LocalBinder;
import paulscode.android.mupen64plusae.util.Notifier;

public class ExtractRomFragment extends Fragment implements ExtractRomService.ExtractRomsListener
{    
    //Progress dialog for extracting ROMs
    private ProgressDialog mProgress = null;
    
    //Service connection for the progress dialog
    private ServiceConnection mServiceConnection;
    
    private boolean mCachedExtractRom = false;

    private boolean mCachedResult = false;
    
    private String mRomZipPath = null;
    private String mRomExtractPath = null;
    private String mRomPath = null;
    private String mMd5 = null;
    private String mRomCrc = null;
    private String mRomHeaderName = null;
    private byte mRomCountryCode = 0;
    private String mRomArtPath = null;
    private String mRomGoodName = null;
    private String mRomLegacySaveFileName = null;
    private boolean mIsRestarting = false;
    
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
            CharSequence title = getString( R.string.extractRomTask_title );
            CharSequence message = getString( R.string.toast_pleaseWait );
            mProgress = new ProgressDialog( mProgress, getActivity(), title, mRomZipPath, message, true );
            mProgress.show();
        }
        
        if(mCachedExtractRom)
        {
            actuallyExtractRom(getActivity());
            mCachedExtractRom = false;
        }

        if (mCachedResult && mInProgress)
        {
            mProgress.dismiss();
            launchGame();
            mCachedResult = false;
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
            ActivityHelper.stopExtractRomService(getActivity().getApplicationContext(), mServiceConnection);
        }
        
        super.onDestroy();
    }

    @Override
    public void onExtractRomFinished()
    {

    }

    private void launchGame()
    {
        // Notify user that the game activity is starting
        Notifier.showToast(getActivity(), R.string.toast_launchingEmulator);

        // Launch the game activity
        ActivityHelper.startGameActivity(getActivity(), mRomPath, mMd5, mRomCrc, mRomHeaderName, mRomCountryCode,
                mRomArtPath, mRomGoodName, mRomLegacySaveFileName, mIsRestarting);
    }
    
    @Override
    public void onExtractRomServiceDestroyed()
    {
        mInProgress = false;
        
        if(getActivity() != null)
        {
            mProgress.dismiss();
            launchGame();
        }
        else
        {
            mCachedResult = true;
        }
    }

    @Override
    public ProgressDialog GetProgressDialog()
    {
        return mProgress;
    }

    public void ExtractRom( String romZipPath, String romExtractPath, String romPath, String md5,
       String romCrc, String romHeaderName, byte romCountryCode, String romArtPath, String romGoodName,
       String romLegacySaveFileName, boolean isRestarting)
    {
        mRomZipPath = romZipPath;
        mRomExtractPath = romExtractPath;
        mRomPath = romPath;
        mMd5 = md5;
        mRomCrc = romCrc;
        mRomHeaderName = romHeaderName;
        mRomCountryCode = romCountryCode;
        mRomArtPath = romArtPath;
        mRomGoodName = romGoodName;
        mRomLegacySaveFileName = romLegacySaveFileName;
        mIsRestarting = isRestarting;
        
        if(getActivity() != null)
        {
            actuallyExtractRom(getActivity());
        }
        else
        {
            mCachedExtractRom = true;
        }
    }
    
    private void actuallyExtractRom(Activity activity)
    {
        mInProgress = true;
        
        CharSequence title = getString( R.string.extractRomTask_title );
        CharSequence message = getString( R.string.toast_pleaseWait );
        mProgress = new ProgressDialog( mProgress, getActivity(), title, mRomZipPath, message, true );
        mProgress.show();
        
        /* Defines callbacks for service binding, passed to bindService() */
        mServiceConnection = new ServiceConnection() {
            
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {

                // We've bound to LocalService, cast the IBinder and get LocalService instance
                LocalBinder binder = (LocalBinder) service;
                ExtractRomService ExtractRomService = binder.getService();
                ExtractRomService.setExtractRomListener(ExtractRomFragment.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
            }
        };

        // Asynchronously extract ROM image
        ActivityHelper.startExtractRomService(activity.getApplicationContext(), mServiceConnection,
                mRomZipPath, mRomExtractPath, mRomPath, mMd5);
    }
    
    public boolean IsInProgress()
    {
        return mInProgress;
    }
}