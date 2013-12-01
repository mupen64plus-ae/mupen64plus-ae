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
import java.util.List;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.AssetExtractor;
import paulscode.android.mupen64plusae.util.AssetExtractor.ExtractionFailure;
import paulscode.android.mupen64plusae.util.AssetExtractor.OnExtractionProgressListener;
import paulscode.android.mupen64plusae.util.ErrorLogger;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The main activity that presents the splash screen, extracts the assets if necessary, and launches
 * the main menu activity.
 */
public class SplashActivity extends Activity implements OnExtractionProgressListener
{
    /**
     * Asset version number, used to determine stale assets. Increment this number every time the
     * assets are updated on disk.
     */
    private static final int ASSET_VERSION = 21;
    
    /** The total number of assets to be extracted (for computing progress %). */
    private static final int TOTAL_ASSETS = 124;
    
    /** The minimum duration that the splash screen is shown, in milliseconds. */
    private static final int SPLASH_DELAY = 1000;
    
    /**
     * The subdirectory within the assets directory to extract. A subdirectory is necessary to avoid
     * extracting all the default system assets in addition to ours.
     */
    private static final String SOURCE_DIR = "mupen64plus_data";
    
    /** Persistent application data. */
    private AppData mAppData;
    
    /** The text view that displays extraction progress info. */
    private TextView mTextView;
    
    /** The running count of assets extracted. */
    private int mAssetsExtracted;
    
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Save the path of the ROM, if it was passed to the activity
        Uri dataUri = this.getIntent().getData();
        if( dataUri != null )
        {
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putString( "pathSelectedGame", dataUri.getPath() ).commit();
        }
        
        // Enforce any locale overrides
        new UserPrefs( this ).enforceLocale( this );
        
        // Get app data
        mAppData = new AppData( this );
        
        // Initialize the error logger
        ErrorLogger.initialize( mAppData.error_log );
        
        // Initialize the toast/status bar notifier
        Notifier.initialize( this );
        
        // Don't let the activity sleep in the middle of extraction
        getWindow().setFlags( LayoutParams.FLAG_KEEP_SCREEN_ON, LayoutParams.FLAG_KEEP_SCREEN_ON );
        
        // Lay out the content
        setContentView( R.layout.main_activity );
        mTextView = (TextView) findViewById( R.id.mainText );
        
        if( new UserPrefs( this ).isBigScreenMode )
        {
            ImageView splash = (ImageView) findViewById( R.id.mainImage );
            splash.setImageResource( R.drawable.publisherlogo_ouya );
        }
        
        // Extract the assets in a separate thread and launch the menu activity
        // Handler.postDelayed ensures this runs only after activity has resumed
        final Handler handler = new Handler();
        handler.postDelayed( nonUiThreadLauncher, SPLASH_DELAY );
    }
    
    /** Runnable that launches the non-UI thread from the UI thread after the activity has resumed. */
    private final Runnable nonUiThreadLauncher = new Runnable()
    {
        @Override
        public void run()
        {
            Thread nonUiThread = new Thread( nonUiThreadWorker, "AssetExtractorThread" );
            nonUiThread.start();
        }
    };
    
    /** Runnable that performs the asset extraction from the non-UI thread. */
    private final Runnable nonUiThreadWorker = new Runnable()
    {
        @Override
        public void run()
        {
            // This runs on non-UI thread and ensures that the app is responsive during the lengthy
            // extraction process
            List<ExtractionFailure> failures = null;
            
            // Extract the assets if they are out of date
            if( mAppData.getAssetVersion() != ASSET_VERSION )
            {
                FileUtil.deleteFolder( new File( mAppData.dataDir ) );
                mAssetsExtracted = 0;
                
                failures = AssetExtractor.extractAssets( getAssets(), SOURCE_DIR, mAppData.dataDir,
                        SplashActivity.this );
            }
            
            // Launch menu activity if successful; post failure notice otherwise
            if( failures == null || failures.size() == 0 )
            {
                mAppData.putAssetVersion( ASSET_VERSION );
                updateText( R.string.assetExtractor_finished );
                
                // Launch the MenuActivity
                startActivity( new Intent( SplashActivity.this, MenuActivity.class ) );
                
                // We never want to come back to this activity, so finish it
                finish();
            }
            else
            {
                // There was an error, update the on-screen text and don't start next activity
                String weblink = getResources().getString( R.string.assetExtractor_uriHelp );
                String message = getString( R.string.assetExtractor_failed, weblink );
                String textHtml =  message.replace( "\n", "<br/>" )+ "<p><small>";
                for( ExtractionFailure failure : failures )
                {
                    textHtml += failure.toString() + "<br/>";
                }
                textHtml += "</small>";
                updateText( Html.fromHtml( textHtml ) );
            }
        }
    };
    
    /*
     * (non-Javadoc)
     * 
     * @see paulscode.android.mupen64plusae.util.AssetExtractor.OnExtractionProgressListener#
     * onExtractionProgress(java.lang.String)
     */
    @Override
    public void onExtractionProgress( final String nextFileExtracted )
    {
        float percent = ( 100f * mAssetsExtracted ) / (float) TOTAL_ASSETS;
        mAssetsExtracted++;
        updateText( R.string.assetExtractor_progress, percent, nextFileExtracted );
    }
    
    /**
     * Update the status text from the UI thread.
     * 
     * @param resId      The resource ID for the format string.
     * @param formatArgs The format arguments that will be used for substitution.
     */
    private void updateText( int resId, Object... formatArgs )
    {
        final String text = getString( resId, formatArgs );
        updateText( text );
    }

    /**
     * Update the status text from the UI thread.
     * 
     * @param text The text to be displayed
     */
    private void updateText( final CharSequence text )
    {
        // Ensures that text view is updated from the UI thread
        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                mTextView.setText( text );
            }
        } );
    }
}
