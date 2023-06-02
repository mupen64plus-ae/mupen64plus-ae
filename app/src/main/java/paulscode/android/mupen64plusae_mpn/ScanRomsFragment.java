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

package paulscode.android.mupen64plusae_mpn;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.mupen64plusae_mpn.v3.alpha.R;

import paulscode.android.mupen64plusae_mpn.dialog.ProgressDialog;
import paulscode.android.mupen64plusae_mpn.persistent.AppData;
import paulscode.android.mupen64plusae_mpn.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae_mpn.task.CacheRomInfoService;
import paulscode.android.mupen64plusae_mpn.task.CacheRomInfoService.CacheRomInfoListener;
import paulscode.android.mupen64plusae_mpn.task.CacheRomInfoService.LocalBinder;
import paulscode.android.mupen64plusae_mpn.util.FileUtil;

@SuppressWarnings({"WeakerAccess", "unused", "RedundantSuppression"})
public class ScanRomsFragment extends Fragment implements CacheRomInfoListener
{    
    //Progress dialog for ROM scan
    private ProgressDialog mProgress = null;

    public static class DataViewModel extends ViewModel {

        public DataViewModel() { }

        //Service connection for the progress dialog
        LocalBinder mBinder = null;

        AppData mAppData = null;

        GlobalPrefs mGlobalPrefs = null;

        String mSearchUri = null;
        boolean mSearchZips = false;
        boolean mDownloadArt = false;
        boolean mClearGallery = false;
        boolean mSearchSubdirectories = false;
        boolean mSearchSingleFile = false;

        boolean mInProgress = false;

        boolean mScanRomsOnActivityCreated = false;
        ScanRomsFragment mCurrentFragment = null;
    }
    DataViewModel mViewModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mViewModel = new ViewModelProvider(requireActivity()).get(ScanRomsFragment.DataViewModel.class);
        mViewModel.mCurrentFragment = this;

        if (mViewModel.mScanRomsOnActivityCreated) {
            mViewModel.mScanRomsOnActivityCreated = false;
            ActuallyRefreshRoms(requireActivity());
        } else if(mViewModel.mInProgress) {
            Activity activity = requireActivity();
            CharSequence title = getString(R.string.scanning_title);
            CharSequence message = getString(R.string.toast_pleaseWait);

            DocumentFile rootDocumentFile = mViewModel.mSearchSingleFile ? FileUtil.getDocumentFileSingle(activity, Uri.parse(mViewModel.mSearchUri)) :
                    FileUtil.getDocumentFileTree(activity, Uri.parse(mViewModel.mSearchUri));
            String text = rootDocumentFile != null ? rootDocumentFile.getName() : "";

            mProgress = new ProgressDialog(mProgress, activity, title, text, message, true);
            mProgress.show();

            if (mViewModel.mBinder != null) {
                CacheRomInfoService cacheRomInfoService = mViewModel.mBinder.getService();
                cacheRomInfoService.SetCacheRomInfoListener(mViewModel.mCurrentFragment);
            }
        }
    }
    
    @Override
    public void onDetach()
    {
        //This can be null if this fragment is never utilized and this will be called on shutdown
        if(mProgress != null) {
            mProgress.dismiss();
        }
        super.onDetach();
    }

    @Override
    public void onCacheRomInfoFinished()
    {
    }
    
    @Override
    public void onCacheRomInfoServiceDestroyed()
    {
        mViewModel.mInProgress = false;

        try {
            Activity activity = requireActivity();
            activity.runOnUiThread(((GalleryActivity) activity)::reloadCacheAndRefreshGrid);
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
        boolean searchSubdirectories, boolean searchSingleFile, AppData appData, GlobalPrefs globalPrefs )
    {
        mViewModel.mSearchUri = searchUri;
        mViewModel.mSearchZips = searchZips;
        mViewModel.mDownloadArt = downloadArt;
        mViewModel.mClearGallery = clearGallery;
        mViewModel.mSearchSubdirectories = searchSubdirectories;
        mViewModel.mSearchSingleFile = searchSingleFile;
        mViewModel.mAppData = appData;
        mViewModel.mGlobalPrefs = globalPrefs;

        try {
            ActuallyRefreshRoms(requireActivity());
        } catch (java.lang.IllegalStateException e) {
            Log.w("ScanRomsFragment", "Activity not created yet, scan later");
            mViewModel.mScanRomsOnActivityCreated = true;
        }
    }
    
    private void ActuallyRefreshRoms(Activity activity)
    {
        mViewModel.mInProgress = true;
        
        CharSequence title = getString( R.string.scanning_title );
        CharSequence message = getString( R.string.toast_pleaseWait );

        DocumentFile rootDocumentFile = mViewModel.mSearchSingleFile ? FileUtil.getDocumentFileSingle(activity, Uri.parse(mViewModel.mSearchUri)) :
                FileUtil.getDocumentFileTree(activity, Uri.parse(mViewModel.mSearchUri));

        String text = rootDocumentFile != null ? rootDocumentFile.getName() : "";
        mProgress = new ProgressDialog( mProgress, activity, title, text, message, true );
        mProgress.show();
        
        /* Defines callbacks for service binding, passed to bindService() */
        ServiceConnection serviceConnection = new ServiceConnection() {
            
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                mViewModel.mBinder = (LocalBinder) service;
                CacheRomInfoService cacheRomInfoService = mViewModel.mBinder.getService();
                cacheRomInfoService.SetCacheRomInfoListener(mViewModel.mCurrentFragment);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
            }
        };

        // Asynchronously search for ROMs
        ActivityHelper.startCacheRomInfoService(activity.getApplicationContext(), serviceConnection,
                mViewModel.mSearchUri, mViewModel.mAppData.mupen64plus_ini, mViewModel.mGlobalPrefs.romInfoCacheCfg,
                mViewModel.mGlobalPrefs.coverArtDir, mViewModel.mSearchZips,
                mViewModel.mDownloadArt, mViewModel.mClearGallery, mViewModel.mSearchSubdirectories, mViewModel.mSearchSingleFile);
    }
    
    public boolean IsInProgress()
    {
        return mViewModel.mInProgress;
    }
}