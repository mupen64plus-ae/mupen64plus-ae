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

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.cheat.CheatUtils;
import paulscode.android.mupen64plusae.dialog.Popups;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.preference.PathPreference;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.task.ExtractAssetsTask;
import paulscode.android.mupen64plusae.task.ExtractAssetsTask.ExtractAssetsListener;
import paulscode.android.mupen64plusae.task.ExtractAssetsTask.Failure;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.RomDatabase;
import tv.ouya.console.api.OuyaFacade;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.text.Html;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The main activity that presents the splash screen, extracts the assets if necessary, and launches
 * the main menu activity.
 */
public class SplashActivity extends AppCompatActivity implements ExtractAssetsListener, OnRequestPermissionsResultCallback
{
    //Permission request ID
    static final int PERMISSION_REQUEST = 177;

    //Total number of permissions requested
    static final int NUM_PERMISSIONS = 2;

    /**
     * Asset version number, used to determine stale assets. Increment this number every time the
     * assets are updated on disk.
     */
    private static final int ASSET_VERSION = 80;

    /** The total number of assets to be extracted (for computing progress %). */
    private static final int TOTAL_ASSETS = 120;

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
    private GlobalPrefs mGlobalPrefs = null;
    private SharedPreferences mPrefs = null;

    // These constants must match the keys used in res/xml/preferences*.xml
    private static final String DISPLAY_ORIENTATION = "displayOrientation";
    private static final String DISPLAY_POSITION = "displayPosition";
    private static final String DISPLAY_SCALING = "displayScaling";
    private static final String VIDEO_HARDWARE_TYPE = "videoHardwareType";
    private static final String AUDIO_PLUGIN = "audioPlugin";
    private static final String AUDIO_SDL_BUFFER_SIZE = "audioSDLBufferSize";
    private static final String AUDIO_SLES_BUFFER_SIZE = "audioSLESBufferSize2";
    private static final String TOUCHSCREEN_AUTO_HOLD = "touchscreenAutoHold";
    private static final String NAVIGATION_MODE = "navigationMode";
    private static final String DATA_PATH = "pathGameSaves";

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
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        mGlobalPrefs.enforceLocale( this );
        mPrefs = PreferenceManager.getDefaultSharedPreferences( this );

        // Ensure that any missing preferences are populated with defaults (e.g. preference added to
        // new release)
        PreferenceManager.setDefaultValues( this, R.xml.preferences_audio, false );
        PreferenceManager.setDefaultValues( this, R.xml.preferences_data, false );
        PreferenceManager.setDefaultValues( this, R.xml.preferences_display, false );
        PreferenceManager.setDefaultValues( this, R.xml.preferences_input, false );
        PreferenceManager.setDefaultValues( this, R.xml.preferences_library, false );
        PreferenceManager.setDefaultValues( this, R.xml.preferences_touchscreen, false );

        // Ensure that selected plugin names and other list preferences are valid
        // @formatter:off
        final Resources res = getResources();
        PrefUtil.validateListPreference( res, mPrefs, DISPLAY_ORIENTATION,      R.string.displayOrientation_default,    R.array.displayOrientation_values );
        PrefUtil.validateListPreference( res, mPrefs, DISPLAY_POSITION,         R.string.displayPosition_default,       R.array.displayPosition_values );
        PrefUtil.validateListPreference( res, mPrefs, DISPLAY_SCALING,          R.string.displayScaling_default,        R.array.displayScaling_values );
        PrefUtil.validateListPreference( res, mPrefs, VIDEO_HARDWARE_TYPE,      R.string.videoHardwareType_default,     R.array.videoHardwareType_values );
        PrefUtil.validateListPreference( res, mPrefs, AUDIO_PLUGIN,             R.string.audioPlugin_default,           R.array.audioPlugin_values );
        PrefUtil.validateListPreference( res, mPrefs, AUDIO_SDL_BUFFER_SIZE,    R.string.audioSDLBufferSize_default,    R.array.audioSDLBufferSize_values );
        PrefUtil.validateListPreference( res, mPrefs, AUDIO_SLES_BUFFER_SIZE,   R.string.audioSLESBufferSize_default,   R.array.audioSLESBufferSize_values );
        PrefUtil.validateListPreference( res, mPrefs, TOUCHSCREEN_AUTO_HOLD,    R.string.touchscreenAutoHold_default,   R.array.touchscreenAutoHold_values );
        PrefUtil.validateListPreference( res, mPrefs, NAVIGATION_MODE,          R.string.navigationMode_default,        R.array.navigationMode_values );
        
        //Check for invalid data path
        String dataPathString = mPrefs.getString( DATA_PATH, null );
        
        if(dataPathString == null || dataPathString.isEmpty() ||
            dataPathString.contains(res.getString(R.string.pathGameSaves_default)))
        {
            String defValue = res.getString( R.string.pathGameSaves_default );
            String newDefValue = PathPreference.validate( defValue);
            
            mPrefs.edit().putString( DATA_PATH, newDefValue ).commit();
        }
        
        // @formatter:on

        // Refresh the preference data wrapper
        mGlobalPrefs = new GlobalPrefs( this, mAppData );

        // Make sure custom skin directory exist
        new File( mGlobalPrefs.touchscreenCustomSkinsDir ).mkdirs();

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

        if( mGlobalPrefs.isBigScreenMode )
        {
            final ImageView splash = (ImageView) findViewById( R.id.mainImage );
            splash.setImageResource( R.drawable.publisherlogo_ouya );
        }
        
        requestPermissions();

        // Popup a warning if the installation appears to be corrupt
        if( !mAppData.isValidInstallation() )
        {
            Popups.showInvalidInstall( this );
        }
    }

    public void requestPermissions()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
           ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
                //Show dialog asking for permissions
                new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.assetExtractor_permissions_title))
                    .setMessage(getString(R.string.assetExtractor_permissions_rationale))
                    .setPositiveButton(getString(android.R.string.ok), new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            actuallyRequestPermissions();
                        }

                    }).setNegativeButton(getString(android.R.string.cancel), new OnClickListener()
                    {
                        //Show dialog stating that the app can't continue without proper permissions
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            new AlertDialog.Builder(SplashActivity.this).setTitle(getString(R.string.assetExtractor_error))
                                .setMessage(getString(R.string.assetExtractor_failed_permissions))
                                .setPositiveButton(getString( android.R.string.ok ), new OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        SplashActivity.this.finish();
                                    }

                                }).setCancelable(false).show();
                        }
                    }).setCancelable(false).show();
            }
            else
            {
                // No explanation needed, we can request the permission.
                actuallyRequestPermissions();
            }
        }
        else
        {
            // Extract the assets in a separate thread and launch the menu activity
            // Handler.postDelayed ensures this runs only after activity has resumed
            final Handler handler = new Handler();
            handler.postDelayed( extractAssetsTaskLauncher, SPLASH_DELAY );
        }
    }

    @SuppressLint("InlinedApi")
    public void actuallyRequestPermissions()
    {
        ActivityCompat.requestPermissions(this, new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE }, PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
        case PERMISSION_REQUEST:
        {
            // If request is cancelled, the result arrays are empty.
            boolean good = true;
            if (permissions.length != NUM_PERMISSIONS || grantResults.length != NUM_PERMISSIONS)
            {
                good = false;
            }

            for (int i = 0; i < grantResults.length && good; i++)
            {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                {
                    good = false;
                }
            }

            if (!good)
            {
                // permission denied, boo! Disable the app.
                new AlertDialog.Builder(SplashActivity.this).setTitle(getString(R.string.assetExtractor_error))
                    .setMessage(getString(R.string.assetExtractor_failed_permissions))
                    .setPositiveButton(getString( android.R.string.ok ), new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            SplashActivity.this.finish();
                        }

                    }).setCancelable(false).show();
            }
            else
            {
                //Permissions already granted, continue
                // Extract the assets in a separate thread and launch the menu activity
                // Handler.postDelayed ensures this runs only after activity has resumed
                final Handler handler = new Handler();
                handler.postDelayed( extractAssetsTaskLauncher, SPLASH_DELAY );
            }
            return;
        }

        // other 'case' lines to check for other
        // permissions this app might request
        }
    }

    /** Runnable that launches the non-UI thread from the UI thread after the activity has resumed. */
    private final Runnable extractAssetsTaskLauncher = new Runnable()
    {
        @Override
        public void run()
        {
            if( mAppData.getAssetVersion() != ASSET_VERSION )
            {
                extractAssets();
            }
            else
            {
                // Assets already extracted, just launch gallery activity, passing ROM path if it was provided externally
                ActivityHelper.startGalleryActivity( SplashActivity.this, getIntent().getData() );

                // We never want to come back to this activity, so finish it
                finish();
            }
        }
    };

    /**
     * Extract assets
     */
    private void extractAssets()
    {
        // Extract and merge the assets if they are out of date
        FileUtil.deleteFolder( new File( mAppData.coreSharedDataDir ) );
        mAssetsExtracted = 0;
        new ExtractAssetsTask( getAssets(), SOURCE_DIR, mAppData.coreSharedDataDir, SplashActivity.this ).execute();
    }

    @Override
    public void onExtractAssetsProgress( String nextFileToExtract )
    {
        final float percent = ( 100f * mAssetsExtracted ) / TOTAL_ASSETS;
        final String text = getString( R.string.assetExtractor_progress, percent, nextFileToExtract );
        mTextView.setText( text );
        mAssetsExtracted++;
    }

    @Override
    public void onExtractAssetsFinished( List<Failure> failures )
    {
        if( failures.size() == 0 )
        {
            // Extraction succeeded, record new asset version and merge cheats
            mTextView.setText( R.string.assetExtractor_finished );
            mAppData.putAssetVersion( ASSET_VERSION );
            CheatUtils.mergeCheatFiles( mAppData.mupencheat_default, mGlobalPrefs.customCheats_txt, mAppData.mupencheat_txt );

            if(!RomDatabase.getInstance().hasDatabaseFile())
            {
                RomDatabase.getInstance().setDatabaseFile(mAppData.mupen64plus_ini);
            }

            // Launch gallery activity, passing ROM path if it was provided externally
            ActivityHelper.startGalleryActivity( this, getIntent().getData() );

            // We never want to come back to this activity, so finish it
            finish();
        }
        else
        {
            // Extraction failed, update the on-screen text and don't start next activity
            final String weblink = getResources().getString( R.string.assetExtractor_uriHelp );
            final String message = getString( R.string.assetExtractor_failed, weblink );
            String textHtml = message.replace( "\n", "<br/>" ) + "<p><small>";
            for( final Failure failure : failures )
            {
                textHtml += failure.toString() + "<br/>";
            }
            textHtml += "</small>";
            mTextView.setText( Html.fromHtml( textHtml ) );
        }
    }
}
