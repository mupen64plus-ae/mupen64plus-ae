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

import paulscode.android.mupen64plusae.cheat.CheatUtils;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.task.ExtractAssetsTask;
import paulscode.android.mupen64plusae.task.ExtractAssetsTask.ExtractAssetsListener;
import paulscode.android.mupen64plusae.task.ExtractAssetsTask.Failure;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.PrefUtil;
import paulscode.android.mupen64plusae.util.RomDetail;
import tv.ouya.console.api.OuyaFacade;
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
public class SplashActivity extends Activity implements ExtractAssetsListener
{
    /**
     * Asset version number, used to determine stale assets. Increment this number every time the
     * assets are updated on disk.
     */
    private static final int ASSET_VERSION = 43;
    
    /** The total number of assets to be extracted (for computing progress %). */
    private static final int TOTAL_ASSETS = 115;
    
    /** The minimum duration that the splash screen is shown, in milliseconds. */
    private static final int SPLASH_DELAY = 1000;
    
    /** PaulsCode OUYA developer UUID */
    private static final String DEVELOPER_ID = "68d84579-c1e2-4418-8976-cda2692133f1";
    
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
    private static final String DISPLAY_ORIENTATION = "displayOrientation";
    private static final String DISPLAY_POSITION = "displayPosition";
    private static final String DISPLAY_RESOLUTION = "displayResolution";
    private static final String DISPLAY_SCALING = "displayScaling";
    private static final String DISPLAY_FPS_REFRESH = "displayFpsRefresh";
    private static final String VIDEO_HARDWARE_TYPE = "videoHardwareType";
    private static final String AUDIO_PLUGIN = "audioPlugin";
    private static final String AUDIO_BUFFER_SIZE = "audioBufferSize";
    private static final String TOUCHSCREEN_STYLE = "touchscreenStyle";
    private static final String TOUCHSCREEN_REFRESH = "touchscreenRefresh";
    private static final String TOUCHSCREEN_AUTO_HOLD = "touchscreenAutoHold";
    private static final String TOUCHPAD_LAYOUT = "touchpadLayout";
    private static final String NAVIGATION_MODE = "navigationMode";
    
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
        
        // Ensure that any missing preferences are populated with defaults (e.g. preference added to
        // new release)
        PreferenceManager.setDefaultValues( this, R.xml.preferences_global, false );
        
        // Ensure that selected plugin names and other list preferences are valid
        // @formatter:off
        Resources res = getResources();
        PrefUtil.validateListPreference( res, mPrefs, DISPLAY_ORIENTATION,      R.string.displayOrientation_default,    R.array.displayOrientation_values );
        PrefUtil.validateListPreference( res, mPrefs, DISPLAY_POSITION,         R.string.displayPosition_default,       R.array.displayPosition_values );
        PrefUtil.validateListPreference( res, mPrefs, DISPLAY_RESOLUTION,       R.string.displayResolution_default,     R.array.displayResolution_values );
        PrefUtil.validateListPreference( res, mPrefs, DISPLAY_SCALING,          R.string.displayScaling_default,        R.array.displayScaling_values );
        PrefUtil.validateListPreference( res, mPrefs, DISPLAY_FPS_REFRESH,      R.string.displayFpsRefresh_default,     R.array.displayFpsRefresh_values );
        PrefUtil.validateListPreference( res, mPrefs, VIDEO_HARDWARE_TYPE,      R.string.videoHardwareType_default,     R.array.videoHardwareType_values );
        PrefUtil.validateListPreference( res, mPrefs, AUDIO_PLUGIN,             R.string.audioPlugin_default,           R.array.audioPlugin_values );
        PrefUtil.validateListPreference( res, mPrefs, AUDIO_BUFFER_SIZE,        R.string.audioBufferSize_default,       R.array.audioBufferSize_values );
        PrefUtil.validateListPreference( res, mPrefs, TOUCHSCREEN_STYLE,        R.string.touchscreenStyle_default,      R.array.touchscreenStyle_values );
        PrefUtil.validateListPreference( res, mPrefs, TOUCHSCREEN_REFRESH,      R.string.touchscreenRefresh_default,    R.array.touchscreenRefresh_values );
        PrefUtil.validateListPreference( res, mPrefs, TOUCHSCREEN_AUTO_HOLD,    R.string.touchscreenAutoHold_default,   R.array.touchscreenAutoHold_values );
        PrefUtil.validateListPreference( res, mPrefs, TOUCHPAD_LAYOUT,          R.string.touchpadLayout_default,        R.array.touchpadLayout_values );
        PrefUtil.validateListPreference( res, mPrefs, NAVIGATION_MODE,          R.string.navigationMode_default,        R.array.navigationMode_values );
        // @formatter:on
        
        // Refresh the preference data wrapper
        mUserPrefs = new UserPrefs( this );
        
        // Initialize the OUYA interface if running on OUYA
        if( AppData.IS_OUYA_HARDWARE )
            OuyaFacade.getInstance().init( this, DEVELOPER_ID );
        
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
        handler.postDelayed( extractAssetsTaskLauncher, SPLASH_DELAY );
    }
    
    /** Runnable that launches the non-UI thread from the UI thread after the activity has resumed. */
    private final Runnable extractAssetsTaskLauncher = new Runnable()
    {
        @Override
        public void run()
        {
            // Extract and merge the assets if they are out of date
            if( mAppData.getAssetVersion() != ASSET_VERSION )
            {
                FileUtil.deleteFolder( new File( mAppData.coreSharedDataDir ) );
                mAssetsExtracted = 0;
                new ExtractAssetsTask( getAssets(), SOURCE_DIR, mAppData.coreSharedDataDir, SplashActivity.this ).execute();
            }
            else
            {
                launchGalleryActivity();
            }
        }
    };
    
    @Override
    public void onExtractAssetsProgress( String nextFileToExtract )
    {
        final float percent = ( 100f * mAssetsExtracted ) / (float) TOTAL_ASSETS;
        final String text = getString( R.string.assetExtractor_progress, percent, nextFileToExtract );
        mTextView.setText( text );
        mAssetsExtracted++;
    }
    
    @Override
    public void onExtractAssetsFinished( List<Failure> failures )
    {
        if( failures.size() == 0 )
        {
            // Extraction succeeded, record new asset version, merge cheats, and launch next activity
            CheatUtils.mergeCheatFiles( mAppData.mupencheat_default, mUserPrefs.usrcheat_txt, mAppData.mupencheat_txt );
            launchGalleryActivity();
        }
        else
        {
            // Extraction failed, update the on-screen text and don't start next activity
            String weblink = getResources().getString( R.string.assetExtractor_uriHelp );
            String message = getString( R.string.assetExtractor_failed, weblink );
            String textHtml = message.replace( "\n", "<br/>" ) + "<p><small>";
            for( Failure failure : failures )
            {
                textHtml += failure.toString() + "<br/>";
            }
            textHtml += "</small>";
            mTextView.setText( Html.fromHtml( textHtml ) );
        }
    }
    
    private void launchGalleryActivity( )
    {
        // Initialize ROM database after the assets have been extracted
        RomDetail.initializeDatabase( mAppData.mupen64plus_ini );
        
        // Remember what asset version is installed
        mAppData.putAssetVersion( ASSET_VERSION );
        mTextView.setText( R.string.assetExtractor_finished );
        
        // Launch the activity, passing ROM path if it was provided externally
        Intent intent = new Intent( this, GalleryActivity.class );
        Uri dataUri = getIntent().getData();
        if( dataUri != null )
            intent.putExtra( Keys.Extras.ROM_PATH, dataUri.getPath() );
        startActivity( intent );
        
        // We never want to come back to this activity, so finish it
        finish();
    }
}
