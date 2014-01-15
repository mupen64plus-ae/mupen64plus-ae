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
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.OUYAInterface;
import paulscode.android.mupen64plusae.util.PrefUtil;
import paulscode.android.mupen64plusae.util.RomDetail;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
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
    private static final int ASSET_VERSION = 26;
    
    /** The total number of assets to be extracted (for computing progress %). */
    private static final int TOTAL_ASSETS = 123;
    
    /** The minimum duration that the splash screen is shown, in milliseconds. */
    private static final int SPLASH_DELAY = 1000;
    
    /**
     * The subdirectory within the assets directory to extract. A subdirectory is necessary to avoid
     * extracting all the default system assets in addition to ours.
     */
    private static final String SOURCE_DIR = "mupen64plus_data";
    
    /** The text view that displays extraction progress info. */
    private TextView mTextView;
    
    /** The running count of assets extracted. */
    private int mAssetsExtracted;
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    private SharedPreferences mPrefs = null;
    
    // These constants must match the keys used in res/xml/preferences*.xml
    private static final String TOUCHSCREEN_STYLE = "touchscreenStyle";
    private static final String TOUCHSCREEN_HEIGHT = "touchscreenHeight";
    private static final String TOUCHSCREEN_LAYOUT = "touchscreenLayout";
    private static final String TOUCHPAD_LAYOUT = "touchpadLayout";
    private static final String DISPLAY_POSITION = "displayPosition";
    private static final String DISPLAY_RESOLUTION = "displayResolution";
    private static final String DISPLAY_SCALING = "displayScaling";
    private static final String NAVIGATION_MODE = "navigationMode";
    private static final String R4300_EMULATOR = "r4300Emulator";
    private static final String AUDIO_PLUGIN = "audioPlugin";
    private static final String AUDIO_BUFFER_SIZE = "audioBufferSize";
    
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get app data and user preferences
        mAppData = new AppData( this );
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
        mPrefs = PreferenceManager.getDefaultSharedPreferences( this );
        
        // Ensure that any missing preferences are populated with defaults (e.g. preference added to new release)
        PreferenceManager.setDefaultValues( this, R.xml.preferences_global, false );
        PreferenceManager.setDefaultValues( this, R.xml.preferences_play, false );
        PreferenceManager.setDefaultValues( this, R.xml.preferences_video, false );
        
        // Ensure that selected plugin names and other list preferences are valid
        // @formatter:off
        Resources res = getResources();
        PrefUtil.validateListPreference( res, mPrefs, TOUCHSCREEN_STYLE,  R.string.touchscreenStyle_default,  R.array.touchscreenStyle_values );
        PrefUtil.validateListPreference( res, mPrefs, TOUCHSCREEN_HEIGHT, R.string.touchscreenHeight_default, R.array.touchscreenHeight_values );
        PrefUtil.validateListPreference( res, mPrefs, TOUCHSCREEN_LAYOUT, R.string.touchscreenLayout_default, R.array.touchscreenLayout_values );
        PrefUtil.validateListPreference( res, mPrefs, TOUCHPAD_LAYOUT,    R.string.touchpadLayout_default,    R.array.touchpadLayout_values );
        PrefUtil.validateListPreference( res, mPrefs, DISPLAY_POSITION,   R.string.displayPosition_default,   R.array.displayPosition_values );
        PrefUtil.validateListPreference( res, mPrefs, DISPLAY_RESOLUTION, R.string.displayResolution_default, R.array.displayResolution_values );
        PrefUtil.validateListPreference( res, mPrefs, DISPLAY_SCALING,    R.string.displayScaling_default,    R.array.displayScaling_values );
        PrefUtil.validateListPreference( res, mPrefs, AUDIO_PLUGIN,       R.string.audioPlugin_default,       R.array.audioPlugin_values );
        PrefUtil.validateListPreference( res, mPrefs, AUDIO_BUFFER_SIZE,  R.string.audioBufferSize_default,   R.array.audioBufferSize_values );
        PrefUtil.validateListPreference( res, mPrefs, R4300_EMULATOR,     R.string.r4300Emulator_default,     R.array.r4300Emulator_values );
        PrefUtil.validateListPreference( res, mPrefs, NAVIGATION_MODE,    R.string.navigationMode_default,    R.array.navigationMode_values );
        // @formatter:on
        
        // Refresh the preference data wrapper
        mUserPrefs = new UserPrefs( this );
        
        // Initialize ROM database
        RomDetail.initializeDatabase( mAppData.mupen64plus_ini );
        
        // Initialize the OUYA interface if running on OUYA
        if( OUYAInterface.IS_OUYA_HARDWARE )
            OUYAInterface.init( this );
        
        // Initialize the toast/status bar notifier
        Notifier.initialize( this );
        
        // Don't let the activity sleep in the middle of extraction
        getWindow().setFlags( LayoutParams.FLAG_KEEP_SCREEN_ON, LayoutParams.FLAG_KEEP_SCREEN_ON );
        
        // Lay out the content
        setContentView( R.layout.splash_activity );
        mTextView = (TextView) findViewById( R.id.mainText );
        
        if( mUserPrefs.isBigScreenMode )
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
                FileUtil.deleteFolder( new File( mAppData.coreSharedDataDir ) );
                mAssetsExtracted = 0;
                
                failures = AssetExtractor.extractAssets( getAssets(), SOURCE_DIR, mAppData.coreSharedDataDir,
                        SplashActivity.this );
            }
            
            // Launch menu activity if successful; post failure notice otherwise
            if( failures == null || failures.size() == 0 )
            {
                mAppData.putAssetVersion( ASSET_VERSION );
                updateText( R.string.assetExtractor_finished );
                
                // Launch the GalleryActivity, passing ROM path if it was provided externally
                Intent intent = new Intent( SplashActivity.this, GalleryActivity.class );
                Uri dataUri = SplashActivity.this.getIntent().getData();
                if( dataUri != null )
                    intent.putExtra( Keys.Extras.ROM_PATH, dataUri.getPath() );
                startActivity( intent );
                
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
