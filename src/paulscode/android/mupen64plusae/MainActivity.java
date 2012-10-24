package paulscode.android.mupen64plusae;

import java.io.File;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.Path;
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

// TODO: Comment thoroughly
public class MainActivity extends Activity
{
    private TextView mTextView = null;
    
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get persisted system settings
        Globals.path = new Path( this );
        Globals.appData = new AppData( this, Globals.path.appSettingsFilename );
        Globals.mupen64plus_cfg = new ConfigFile( Globals.path.mupen64plus_cfg );
        
        // Initialize the error logger
        ErrorLogger.initialize( Globals.path.error_log );
        
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
        setContentView( R.layout.main );
        mTextView = (TextView) findViewById( R.id.mainText );
        
        final Handler handler = new Handler();
        handler.postDelayed( new Runnable()
        {
            public void run()
            {
                // Run the downloader on a separate thread
                // It will launch MenuActivity when it's finished
                runOnUiThread( new DownloaderThread( MainActivity.this ) );
            }
        }, Globals.SPLASH_DELAY );
    }
    
    private class DownloaderThread implements Runnable
    {
        MainActivity mParent;
        
        public DownloaderThread( MainActivity parent )
        {
            mParent = parent;
        }
        
        public void run()
        {
            Log.i( "MainActivity", "libSDL: Starting downloader" );
            Globals.downloader = new DataDownloader( mParent, mParent.mTextView );
        }
    }
    
    public void onDownloaderFinished()
    {
        Globals.downloader = null;
        
        // Record that the update completed
        Globals.appData.setUpgradedVer19( true );
        
        // Restore saves if they were backed up:
        File savesBak = new File( Globals.path.savesBackupDir );
        if( savesBak.exists() )
        {
            Utility.copyFile( savesBak, new File( Globals.path.defaultSavesDir ) );
            Utility.deleteFolder( new File( Globals.path.dataBackupDir ) );
        }
        
        // Launch the menu in a new activity
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
        if( Globals.downloader != null )
        {
            synchronized( Globals.downloader )
            {
                Globals.downloader.setStatusField( mTextView );
                if( Globals.downloader.DownloadComplete )
                    onDownloaderFinished();
                else if( Globals.downloader.DownloadFailed )
                {
                    Globals.downloader.DownloadFailed = false;
                    runOnUiThread( new DownloaderThread( this ) );
                }
            }
        }
    }
    
    @Override
    protected void onPause()
    {
        if( Globals.downloader != null )
        {
            synchronized( Globals.downloader )
            {
                Globals.downloader.setStatusField( null );
            }
        }
        super.onPause();
    }
    
    @Override
    protected void onDestroy()
    {
        if( Globals.downloader != null )
        {
            synchronized( Globals.downloader )
            {
                Globals.downloader.setStatusField( null );
            }
        }
        super.onDestroy();
    }
}
