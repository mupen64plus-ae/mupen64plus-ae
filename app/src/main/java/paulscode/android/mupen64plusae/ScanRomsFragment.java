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

import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.task.CacheRomInfoService;
import paulscode.android.mupen64plusae.task.CacheRomInfoService.CacheRomInfoListener;
import paulscode.android.mupen64plusae.task.CacheRomInfoService.LocalBinder;
import paulscode.android.mupen64plusae.util.FileUtil;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ScanRomsFragment extends Fragment implements CacheRomInfoListener
{    
    //Progress dialog for ROM scan
    private ProgressDialog mProgress = null;
    
    //Service connection for the progress dialog
    private ServiceConnection mServiceConnection;
    
    private AppData mAppData = null;
    
    private GlobalPrefs mGlobalPrefs = null;

    private String mSearchUri = null;
    private boolean mSearchZips = false;
    private boolean mDownloadArt = false;
    private boolean mClearGallery = false;
    private boolean mSearchSubdirectories = false;
    
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
            CharSequence title = getString( R.string.scanning_title );
            CharSequence message = getString( R.string.toast_pleaseWait );

            DocumentFile rootDocumentFile = FileUtil.getDocumentFileTree(requireActivity(), Uri.parse(mSearchUri));
            String text = rootDocumentFile != null ? rootDocumentFile.getName() : "";

            mProgress = new ProgressDialog( mProgress, requireActivity(), title, text, message, true );
            mProgress.show();
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
            ActivityHelper.stopCacheRomInfoService(requireActivity().getApplicationContext(), mServiceConnection);
        }
        
        super.onDestroy();
    }

    @Override
    public void onCacheRomInfoFinished()
    {

    }
    
    @Override
    public void onCacheRomInfoServiceDestroyed()
    {
        mInProgress = false;

        try {
            requireActivity().runOnUiThread(() -> ((GalleryActivity)requireActivity()).reloadCacheAndRefreshGrid());
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
        }

        mProgress.dismiss();
    }

    @Override
    public ProgressDialog GetProgressDialog()
    {
        return mProgress;
    }

    public void refreshRoms( String searchUri, boolean searchZips, boolean downloadArt, boolean clearGallery,
        boolean searchSubdirectories, AppData appData, GlobalPrefs globalPrefs )
    {
        this.mSearchUri = searchUri;
        this.mSearchZips = searchZips;
        this.mDownloadArt = downloadArt;
        this.mClearGallery = clearGallery;
        this.mSearchSubdirectories = searchSubdirectories;
        this.mAppData = appData;
        this.mGlobalPrefs = globalPrefs;

        ActuallyRefreshRoms(requireActivity());
    }
    
    private void ActuallyRefreshRoms(Activity activity)
    {
        mInProgress = true;
        
        CharSequence title = getString( R.string.scanning_title );
        CharSequence message = getString( R.string.toast_pleaseWait );

        DocumentFile rootDocumentFile = FileUtil.getDocumentFileTree(activity, Uri.parse(mSearchUri));

        String text = rootDocumentFile != null ? rootDocumentFile.getName() : "";
        mProgress = new ProgressDialog( mProgress, activity, title, text, message, true );
        mProgress.show();
        
        /* Defines callbacks for service binding, passed to bindService() */
        mServiceConnection = new ServiceConnection() {
            
            @Override
            public void onServiceConnected(ComponentName className,
                    IBinder service) {

                // We've bound to LocalService, cast the IBinder and get LocalService instance
                LocalBinder binder = (LocalBinder) service;
                CacheRomInfoService cacheRomInfoService = binder.getService();
                cacheRomInfoService.SetCacheRomInfoListener(ScanRomsFragment.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
            }
        };

        // Asynchronously search for ROMs
        ActivityHelper.startCacheRomInfoService(activity.getApplicationContext(), mServiceConnection,
            mSearchUri, mAppData.mupen64plus_ini, mGlobalPrefs.romInfoCacheCfg,
            mGlobalPrefs.coverArtDir, mGlobalPrefs.unzippedRomsDir, mSearchZips,
            mDownloadArt, mClearGallery, mSearchSubdirectories);
    }
    
    public boolean IsInProgress()
    {
        return mInProgress;
    }
}