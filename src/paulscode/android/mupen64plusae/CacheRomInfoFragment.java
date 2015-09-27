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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.task.CacheRomInfoService;
import paulscode.android.mupen64plusae.task.CacheRomInfoService.CacheRomInfoListener;
import paulscode.android.mupen64plusae.task.CacheRomInfoService.LocalBinder;
import android.app.Activity;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

public class CacheRomInfoFragment extends Fragment implements CacheRomInfoListener
{
    //Progress dialog for ROM scan
    private ProgressDialog mProgress = null;
    
    //Service connection for the progress dialog
    private ServiceConnection mServiceConnection;
    
    private AppData mAppData = null;
    
    private GlobalPrefs mGlobalPrefs = null;
    
    private boolean mCachedResult = false;
    
    private boolean mCachedRefreshRoms = false;
    
    private boolean mCachedScanFinish = false;
    
    private File mStartDir = null;
    private boolean mSearchZips = false;
    private boolean mDownloadArt = false;
    private boolean mClearGallery = false;
    
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
            mProgress = new ProgressDialog( mProgress, getActivity(), title, mStartDir.getAbsolutePath(), message, true );
            mProgress.show();
        }
        
        if (mCachedResult && mInProgress)
        {
            ((GalleryActivity)getActivity()).refreshGrid();
            mCachedResult = false;
        }
        
        if(mCachedRefreshRoms)
        {
            ActuallyRefreshRoms(getActivity());
            mCachedRefreshRoms = false;
        }
        
        if(mCachedScanFinish)
        {
            ActivityHelper.stopCacheRomInfoService(getActivity().getApplicationContext(), mServiceConnection);
            mCachedScanFinish = false;
        }
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
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
    public void onCacheRomInfoFinished()
    {
        if(getActivity() != null)
        {
            ActivityHelper.stopCacheRomInfoService(getActivity().getApplicationContext(), mServiceConnection);
        }
        else
        {
            mCachedScanFinish = true;
        }

    }
    
    @Override
    public void onCacheRomInfoServiceDestroyed()
    {
        mInProgress = false;
        
        if(getActivity() != null)
        {
            ((GalleryActivity)getActivity()).refreshGrid();
            mProgress.dismiss();
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

    public void refreshRoms( File startDir, boolean searchZips, boolean downloadArt, boolean clearGallery,
        AppData appData, GlobalPrefs globalPrefs )
    {
        this.mStartDir = startDir;
        this.mSearchZips = searchZips;
        this.mDownloadArt = downloadArt;
        this.mClearGallery = clearGallery;
        this.mAppData = appData;
        this.mGlobalPrefs = globalPrefs;
        
        if(getActivity() != null)
        {
            ActuallyRefreshRoms(getActivity());
        }
        else
        {
            mCachedRefreshRoms = true;
        }
    }
    
    private void ActuallyRefreshRoms(Activity activity)
    {
        mInProgress = true;
        
        CharSequence title = getString( R.string.scanning_title );
        CharSequence message = getString( R.string.toast_pleaseWait );
        mProgress = new ProgressDialog( mProgress, getActivity(), title, mStartDir.getAbsolutePath(), message, true );
        mProgress.show();
        
        /** Defines callbacks for service binding, passed to bindService() */
        mServiceConnection = new ServiceConnection() {
            
            @Override
            public void onServiceConnected(ComponentName className,
                    IBinder service) {

                // We've bound to LocalService, cast the IBinder and get LocalService instance
                LocalBinder binder = (LocalBinder) service;
                CacheRomInfoService cacheRomInfoService = binder.getService();
                cacheRomInfoService.SetCacheRomInfoListener(CacheRomInfoFragment.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
            }
        };

        // Asynchronously search for ROMs
        ActivityHelper.startCacheRomInfoService(activity.getApplicationContext(), mServiceConnection,
            mStartDir.getAbsolutePath(), mAppData.mupen64plus_ini, mGlobalPrefs.romInfoCache_cfg,
            mGlobalPrefs.coverArtDir, mGlobalPrefs.unzippedRomsDir, mSearchZips,
            mDownloadArt, mClearGallery);
    }
    
    public boolean IsInProgress()
    {
        return mInProgress;
    }
    

    public static File extractRomFile( File destDir, ZipEntry zipEntry, InputStream inStream )
    {        
        // Read the first 4 bytes of the entry
        byte[] buffer = new byte[1024];
        try
        {
            if( inStream.read( buffer, 0, 4 ) != 4 )
                return null;
        }
        catch( IOException e )
        {
            Log.w( "GalleryActivity", e );
            return null;
        }
        
        // This entry appears to be a valid ROM, extract it
        Log.i( "CacheRomInfoFragment", "Found zip entry " + zipEntry.getName() );

        String entryName = new File( zipEntry.getName() ).getName();
        File extractedFile = new File( destDir, entryName );
        try
        {
            // Open the output stream (throws exceptions)
            OutputStream outStream = new FileOutputStream( extractedFile );
            try
            {
                // Buffer the stream
                outStream = new BufferedOutputStream( outStream );
                
                // Write the first four bytes we already peeked at (throws exceptions)
                outStream.write( buffer, 0, 4 );
                
                // Read/write the remainder of the zip entry (throws exceptions)
                int n;
                while( ( n = inStream.read( buffer ) ) >= 0 )
                {
                    outStream.write( buffer, 0, n );
                }
                return extractedFile;
            }
            catch( IOException e )
            {
                Log.w( "GalleryActivity", e );
                return null;
            }
            finally
            {
                // Flush output stream and guarantee no memory leaks
                outStream.close();
            }
        }
        catch( IOException e )
        {
            Log.w( "GalleryActivity", e );
            return null;
        }
    }
}