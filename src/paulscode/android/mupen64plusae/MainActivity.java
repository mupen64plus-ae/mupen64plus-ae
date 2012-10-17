package paulscode.android.mupen64plusae;

import java.io.File;

import paulscode.android.mupen64plusae.persistent.Config;
import paulscode.android.mupen64plusae.persistent.Settings;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

// TODO: Comment thoroughly
public class MainActivity extends Activity
{
    // TODO: Which fields need to be public? static?
    public ImageView _img = null;
    public TextView _tv = null;
    public LinearLayout _layout = null;
    public LinearLayout _layout2 = null;
    public FrameLayout _videoLayout = null;
    public static DataDownloader downloader = null;
    
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        Settings.refreshPath( this );
        ErrorLogger.initialize( Settings.path.error_log );
        Settings.refreshDevice( this.getSharedPreferences( Settings.path.devicePrefName, Context.MODE_PRIVATE ) );
        Settings.mupen64plus_cfg = new Config( Settings.path.mupen64plus_cfg );
        
        // fullscreen mode
        requestWindowFeature( Window.FEATURE_NO_TITLE );
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN );
        if( GameActivity.GameState.inhibitSuspend )
            getWindow().setFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
        
        _layout = new LinearLayout( this );
        _layout.setOrientation( LinearLayout.VERTICAL );
        _layout.setLayoutParams( new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT ) );
        
        _layout2 = new LinearLayout( this );
        _layout2.setLayoutParams( new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
        _layout.addView( _layout2 );
        _img = new ImageView( this );
        _img.setScaleType( ImageView.ScaleType.FIT_CENTER ); // FIT_XY
        try
        {
            _img.setImageDrawable( Drawable.createFromStream( getAssets().open( "logo.png" ),
                    "logo.png" ) );
        }
        catch( Exception e )
        {
            _img.setImageResource( R.drawable.publisherlogo );
        }
        _img.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT ) );
        _layout.addView( _img );
        _videoLayout = new FrameLayout( this );
        _videoLayout.addView( _layout );
        setContentView( _videoLayout );
        
        class Callback implements Runnable
        {
            MainActivity p;
            
            Callback( MainActivity _p )
            {
                p = _p;
            }
            
            public void run()
            {
                p.startDownloader();
            }
        }
        ;
        
        Thread downloaderThread = null;
        downloaderThread = new Thread( new Callback( this ) );
        downloaderThread.start();
    }
    
    @Override
    protected void onPause()
    {
        if( MainActivity.downloader != null )
        {
            synchronized( MainActivity.downloader )
            {
                MainActivity.downloader.setStatusField( null );
            }
        }
        super.onPause();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        if( MainActivity.downloader != null )
        {
            synchronized( MainActivity.downloader )
            {
                MainActivity.downloader.setStatusField( _tv );
                if( MainActivity.downloader.DownloadComplete )
                    downloaderFinished();
                else if( MainActivity.downloader.DownloadFailed )
                {
                    MainActivity.downloader.DownloadFailed = false;
                    class Callback implements Runnable
                    {
                        MainActivity p;
                        
                        Callback( MainActivity _p )
                        {
                            p = _p;
                        }
                        
                        public void run()
                        {
                            p.startDownloader();
                        }
                    }
                    ;
                    
                    Thread downloaderThread = null;
                    downloaderThread = new Thread( new Callback( this ) );
                    downloaderThread.start();
                }
            }
        }
    }
    
    @Override
    protected void onDestroy()
    {
        if( MainActivity.downloader != null )
        {
            synchronized( MainActivity.downloader )
            {
                MainActivity.downloader.setStatusField( null );
            }
        }
        super.onDestroy();
    }
    
    public void setUpStatusLabel()
    {
        MainActivity Parent = this;
        if( Parent._tv == null )
        {
            Parent._tv = new TextView( Parent );
            Parent._tv.setMaxLines( 1 );
            Parent._tv.setText( R.string.initializing );
            Parent._layout2.addView( Parent._tv );
        }
    }
    
    public void startDownloader()
    {
        Log.i( "MainActivity", "libSDL: Starting data downloader" );
        class Callback implements Runnable
        {
            public MainActivity Parent;
            
            public void run()
            {
                setUpStatusLabel();
                Log.i( "MainActivity", "libSDL: Starting downloader" );
                MainActivity.downloader = new DataDownloader( Parent, Parent._tv );
            }
        }
        Callback cb = new Callback();
        cb.Parent = this;
        this.runOnUiThread( cb );
    }
    
    public void downloaderFinished()
    {
        MainActivity.downloader = null;

        // Record that the update completed
        Settings.device.setUpgraded19( true );
        
        // Restore saves if they were backed up:
        File savesBak = new File( Settings.path.savesBackupDir );
        if( savesBak.exists() )
        {
            Utility.copyFile( savesBak, new File( Settings.path.savesDir ) );
            Utility.deleteFolder( new File( Settings.path.restoreDir ) );
        }
        
        Intent intent = new Intent( this, MenuActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
        startActivity( intent );
        finish();
    }
}
