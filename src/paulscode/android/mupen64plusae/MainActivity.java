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

/**
 * Bugs and feature requests not listed elsewhere, in order of priority.
 * 
 * Major bugs or missing features
 * TODO: Major - Add cheats menu
 * TODO: Major - Implement multi-player peripheral controls
 * TODO: Major - Implement special func mapping
 * 
 * Minor bugs or missing features
 * TODO: Minor - Figure out crash on NativeMethods.quit
 * TODO: Minor - Keep surface rendering onPause (don't blackout)
 * 
 * Bugs/features related to older APIs (e.g. Gingerbread)
 * TODO: API - Alternative to setAlpha for button-mapping
 * 
 * Features, nice-to-haves, anti-features
 * TODO: Feature - Do we need status notification? 
 * TODO: Feature - Hide action bar on menu click
 * TODO: Feature - Add menu item for quick access to IME (like Language menu)
 * TODO: Feature - Look into BlueZ and Zeemote protocols
 * TODO: Feature - Implement SensorController if desired
 * 
 * Code polishing (doesn't directly affect end user)
 * TODO: Polish - Cleanup Utility.java
 * TODO: Polish - Cleanup DataDownloader.java
 */

package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.util.DataDownloader;
import paulscode.android.mupen64plusae.util.ErrorLogger;
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
    
    private TextView mTextView;
    private DataDownloader mDownloader;
    private AppData mAppData;
    
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
                if( mDownloader.mDownloadFailed )
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
}
