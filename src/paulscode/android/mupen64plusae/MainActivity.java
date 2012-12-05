/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: paulscode, lioncash, littleguy77
 */
package paulscode.android.mupen64plusae;

import java.io.File;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.util.DataDownloader;
import paulscode.android.mupen64plusae.util.ErrorLogger;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

public class MainActivity extends Activity implements DataDownloader.Listener
{
    /** The minimum duration that the splash screen is shown, in milliseconds. */
    public static final int SPLASH_DELAY = 1000;
    
    private TextView mTextView = null;
    private DataDownloader mDownloader = null;
    private AppData mAppData = null;
    
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get app data
        mAppData = new AppData( this );
        
        // Initialize the error logger
        ErrorLogger.initialize( mAppData.error_log );
        
        // Initialize the toast/status bar notifier
        Notifier.initialize( this );
        
        // TODO: Is this necessary?
        getWindow().setFlags( LayoutParams.FLAG_KEEP_SCREEN_ON, LayoutParams.FLAG_KEEP_SCREEN_ON );
        
        // Lay out the content
        setContentView( R.layout.main_activity );
        mTextView = (TextView) findViewById( R.id.mainText );
        
        final Handler handler = new Handler();
        handler.postDelayed( new Runnable()
        {
            @Override
            public void run()
            {
                // Run the downloader on a separate thread
                // It will launch MenuActivity when it's done
                runOnUiThread( new DownloaderThread() );
            }
        }, MainActivity.SPLASH_DELAY );
    }
    
    private class DownloaderThread implements Runnable
    {
        @Override
        public void run()
        {
            Log.i( "MainActivity", "libSDL: Starting downloader" );
            mDownloader = new DataDownloader( MainActivity.this, MainActivity.this, mTextView,
                    mAppData.dataDir );
        }
    }
    
    @Override
    public void onDownloadComplete()
    {
        mDownloader = null;
        
        // Record that the update completed
        mAppData.setUpgradedVer19( true );
        
        // Restore saves if they were backed up:
        File savesBak = new File( mAppData.savesBackupDir );
        if( savesBak.exists() )
        {
            FileUtil.copyFile( savesBak, new File( mAppData.defaultSavesDir ) );
            FileUtil.deleteFolder( new File( mAppData.dataBackupDir ) );
        }
        
        // Launch the MenuActivity
        startActivity( new Intent( this, MenuActivity.class ) );
        
        // We never want to come back to this screen, so finish it
        finish();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        if( mDownloader != null )
        {
            synchronized( mDownloader )
            {
                mDownloader.setStatusField( mTextView );
                if( mDownloader.mDownloadComplete )
                {
                    onDownloadComplete(); // TODO: is this necessary?
                }
                else if( mDownloader.mDownloadFailed )
                {
                    // Try again
                    mDownloader.mDownloadFailed = false;
                    runOnUiThread( new DownloaderThread() );
                }
            }
        }
    }
    
    @Override
    protected void onPause()
    {
        if( mDownloader != null )
        {
            synchronized( mDownloader )
            {
                mDownloader.setStatusField( null );
            }
        }
        super.onPause();
    }
    
    @Override
    protected void onDestroy()
    {
        if( mDownloader != null )
        {
            synchronized( mDownloader )
            {
                mDownloader.setStatusField( null );
            }
        }
        super.onDestroy();
    }
}
