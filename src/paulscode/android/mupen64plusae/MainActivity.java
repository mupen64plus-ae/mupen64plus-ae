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
 * Authors: paulscode, littleguy77
 */
package paulscode.android.mupen64plusae;

import java.io.File;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.Paths;
import paulscode.android.mupen64plusae.util.DataDownloader;
import paulscode.android.mupen64plusae.util.ErrorLogger;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Updater;
import paulscode.android.mupen64plusae.util.Utility;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

public class MainActivity extends Activity implements DataDownloader.Listener
{
    private TextView mTextView = null;
    private DataDownloader mDownloader = null;
    
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get persisted system settings
        Globals.paths = new Paths( this );
        Globals.appData = new AppData( this, Globals.paths.appSettingsFilename );
        Globals.mupen64plus_cfg = new ConfigFile( Globals.paths.mupen64plus_cfg );
        
        // Initialize the error logger
        ErrorLogger.initialize( Globals.paths.error_log );
        
        // Initialize the toast/status bar notifier
        Notifier.initialize( this );
        
        // Make sure the app is up to date
        // Globals.app.resetToDefaults(); // TODO: Comment out before release
        Updater.checkFirstRun( this );
        Updater.checkConfigFiles( this );
        Updater.checkLatestVersion( this );
        
        // Configure full-screen mode
        requestWindowFeature( Window.FEATURE_NO_TITLE );
        getWindow().setFlags( LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN );
        
        // Keep screen on under certain conditions
        if( Globals.INHIBIT_SUSPEND )
            getWindow().setFlags( LayoutParams.FLAG_KEEP_SCREEN_ON,
                    LayoutParams.FLAG_KEEP_SCREEN_ON );
        
        // Lay out the content
        setContentView( R.layout.main_activity );
        mTextView = (TextView) findViewById( R.id.mainText );
        
        final Handler handler = new Handler();
        handler.postDelayed( new Runnable()
        {
            public void run()
            {
                // Run the downloader on a separate thread
                // It will launch MenuActivity when it's done
                runOnUiThread( new DownloaderThread() );
            }
        }, Globals.SPLASH_DELAY );
    }
    
    private class DownloaderThread implements Runnable
    {
        public void run()
        {
            Log.i( "MainActivity", "libSDL: Starting downloader" );
            mDownloader = new DataDownloader( MainActivity.this, MainActivity.this, mTextView );
        }
    }

    public void onDownloadComplete()
    {
        mDownloader = null;
        
        // Record that the update completed
        Globals.appData.setUpgradedVer19( true );
        
        // Restore saves if they were backed up:
        File savesBak = new File( Globals.paths.savesBackupDir );
        if( savesBak.exists() )
        {
            Utility.copyFile( savesBak, new File( Globals.paths.defaultSavesDir ) );
            Utility.deleteFolder( new File( Globals.paths.dataBackupDir ) );
        }
        
        // Launch the MenuActivity
        Intent intent = new Intent( this, MenuActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
        startActivity( intent );
        
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
                    onDownloadComplete(); // TODO: is this necessary?
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
