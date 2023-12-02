/*
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.tvprovider.media.tv.Channel;
import androidx.tvprovider.media.tv.TvContractCompat;
import androidx.tvprovider.media.tv.ChannelLogoUtils;

import java.util.List;

import paulscode.android.mupen64plusae.cheat.CheatUtils;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.task.ExtractAssetsOrCleanupTask;
import paulscode.android.mupen64plusae.task.ExtractAssetsOrCleanupTask.ExtractAssetsListener;
import paulscode.android.mupen64plusae.task.ExtractAssetsOrCleanupTask.Failure;
import paulscode.android.mupen64plusae.task.SyncProgramsJobService;
import paulscode.android.mupen64plusae.util.DeviceUtil;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.RomDatabase;

/**
 * The main activity that presents the splash screen, extracts the assets if necessary, and launches
 * the main menu activity.
 */
public class SplashActivity extends AppCompatActivity implements ExtractAssetsListener, OnRequestPermissionsResultCallback
{
    //Permission request ID
    static final int PERMISSION_REQUEST = 177;

    //Total number of permissions requested
    static final int NUM_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU? 1 : 2;

    /** The minimum duration that the splash screen is shown, in milliseconds. */
    private static final int SPLASH_DELAY = 1000;

    /**
     * The subdirectory within the assets directory to extract. A subdirectory is necessary to avoid
     * extracting all the default system assets in addition to ours.
     */
    public static final String SOURCE_DIR = "mupen64plus_data";

    /** The text view that displays extraction progress info. */
    private TextView mTextView;

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;

    private AlertDialog mPermissionsNeeded = null;
    private boolean mRequestingPermissions = false;

    // These constants must match the keys used in res/xml/preferences*.xml
    private static final String STATE_REQUESTING_PERMISSIONS = "STATE_REQUESTING_PERMISSIONS";

    @Override
    protected void attachBaseContext(Context newBase) {
        if(TextUtils.isEmpty(LocaleContextWrapper.getLocalCode()))
        {
            super.attachBaseContext(newBase);
        }
        else
        {
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase,LocaleContextWrapper.getLocalCode()));
        }
    }

    @Override
    protected void onNewIntent( Intent intent )
    {
        Log.i("SplashActivity", "onNewIntent");

        // If the activity is already running and is launched again (e.g. from a file manager app),
        // the existing instance will be reused rather than a new one created. This behavior is
        // specified in the manifest (launchMode = singleTask). In that situation, any activities
        // above this on the stack (e.g. GameActivity, GamePrefsActivity) will be destroyed
        // gracefully and onNewIntent() will be called on this instance. onCreate() will NOT be
        // called again on this instance.
        super.onNewIntent( intent );

        // Only remember the last intent used
        setIntent( intent );

        // Assets already extracted, just launch gallery activity, passing ROM path if it was provided externally
        ActivityHelper.startGalleryActivity( SplashActivity.this, getIntent() );

        // We never want to come back to this activity, so finish it
        finish();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        Log.i("SplashActivity", "onCreate");

        super.onCreate( savedInstanceState );

        // Clear logcat on app first start
        DeviceUtil.clearLogCat();

        // Get app data and user preferences
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );

        // Ensure that any missing preferences are populated with defaults (e.g. preference added to
        // new release)
        PreferenceManager.setDefaultValues( this, R.xml.preferences_audio, false );
        PreferenceManager.setDefaultValues( this, R.xml.preferences_data, false );
        PreferenceManager.setDefaultValues( this, R.xml.preferences_display, false );
        PreferenceManager.setDefaultValues( this, R.xml.preferences_input, false );
        PreferenceManager.setDefaultValues( this, R.xml.preferences_library, false );
        PreferenceManager.setDefaultValues( this, R.xml.preferences_touchscreen, false );

        // @formatter:on

        // Refresh the preference data wrapper
        mGlobalPrefs = new GlobalPrefs( this, mAppData );

        // Make sure custom skin directory exist
        FileUtil.makeDirs(mGlobalPrefs.touchscreenCustomSkinsDir);

        // Initialize the toast/status bar notifier
        Notifier.initialize( this );

        // Don't let the activity sleep in the middle of extraction
        getWindow().setFlags( LayoutParams.FLAG_KEEP_SCREEN_ON, LayoutParams.FLAG_KEEP_SCREEN_ON );

        try {
            // Lay out the content
            setContentView( R.layout.splash_activity );

        // Sanity check to make sure resources are present, this can happen if app is not installed
        // correctly
        } catch (android.view.InflateException e) {
            Log.e("SplashActivity", "Resource NOT found");
            Notifier.showToast(this, R.string.invalidInstall_message);
            return;
        }

        mTextView = findViewById( R.id.mainText );

        // Sanity check to make sure resources are present, this can happen if app is not installed
        // correctly
        try {
            Drawable randomDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_arrow_u, null);

            if (randomDrawable != null) {
                Log.i("SplashActivity", "Resource found: " + randomDrawable);
            }
        } catch (android.content.res.Resources.NotFoundException e) {
            Log.e("SplashActivity", "Resource NOT found");
            Notifier.showToast(this, R.string.invalidInstall_message);
            return;
        }

        if ( mGlobalPrefs.isBigScreenMode )
        {
            final ImageView splash = findViewById( R.id.mainImage );
            splash.setImageResource( R.drawable.publisherlogo);
        }

        if (mAppData.isAndroidTv && AppData.IS_OREO && mAppData.getChannelId() == -1)
        {
            createChannel();
        }

        SyncProgramsJobService.scheduleSyncingProgramsForChannel(this, mAppData.getChannelId());

        if ( savedInstanceState != null )
        {
            mRequestingPermissions = savedInstanceState.getBoolean(STATE_REQUESTING_PERMISSIONS);
        }

        if (!mRequestingPermissions) {
            requestPermissions();
        }
    }

    @Override
    public void onSaveInstanceState( Bundle savedInstanceState )
    {
        Log.i("SplashActivity", "onSaveInstanceState");

        savedInstanceState.putBoolean(STATE_REQUESTING_PERMISSIONS, mRequestingPermissions);

        super.onSaveInstanceState( savedInstanceState );
    }

    @Override
    public void onDestroy()
    {
        Log.i("SplashActivity", "onDestroy");

        super.onDestroy();

        if (mPermissionsNeeded != null) {
            mPermissionsNeeded.dismiss();
        }
    }

    public void requestPermissions()
    {
        // Notification permissions for Android 13 and up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            // Android 13 needs notification permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS))
                {
                    //Show dialog asking for permissions
                    //Show dialog stating that the app can't continue without proper permissions
                    mPermissionsNeeded = new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.assetExtractor_permissions_title))
                            .setMessage(getString(R.string.assetExtractor_permissions_rationale_notifications))
                            .setPositiveButton(getString(android.R.string.ok), (dialog, which) -> actuallyRequestPermissions()).setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> mPermissionsNeeded = new AlertDialog.Builder(SplashActivity.this).setTitle(getString(R.string.assetExtractor_error))
                                    .setMessage(getString(R.string.assetExtractor_failed_permissions))
                                    .setPositiveButton(getString( android.R.string.ok ), (dialog1, which1) -> SplashActivity.this.finish()).setCancelable(false).show()).setCancelable(false).show();
                }
                else
                {
                    // No explanation needed, we can request the permission.
                    actuallyRequestPermissions();
                }
            }
            else
            {
                checkExtractAssetsOrCleanup();
            }

            return;
        }

        // Request storage permissions for older android version only without scoped storage
        // This doesn't work reliably with older Android versions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
           ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
                //Show dialog asking for permissions
                //Show dialog stating that the app can't continue without proper permissions
                mPermissionsNeeded = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.assetExtractor_permissions_title))
                    .setMessage(getString(R.string.assetExtractor_permissions_rationale))
                    .setPositiveButton(getString(android.R.string.ok), (dialog, which) -> actuallyRequestPermissions()).setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> mPermissionsNeeded = new AlertDialog.Builder(SplashActivity.this).setTitle(getString(R.string.assetExtractor_error))
                        .setMessage(getString(R.string.assetExtractor_failed_permissions))
                        .setPositiveButton(getString( android.R.string.ok ), (dialog1, which1) -> SplashActivity.this.finish()).setCancelable(false).show()).setCancelable(false).show();
            }
            else
            {
                // No explanation needed, we can request the permission.
                actuallyRequestPermissions();
            }
        }
        else
        {
            checkExtractAssetsOrCleanup();
        }
    }

    @SuppressLint("InlinedApi")
    public void actuallyRequestPermissions()
    {
        mRequestingPermissions = true;

        // Notification permissions for Android 13 and up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST);
            return;
        }

        ActivityCompat.requestPermissions(this, new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE }, PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST)
        {
            // If request is cancelled, the result arrays are empty.
            boolean good = permissions.length == NUM_PERMISSIONS && grantResults.length == NUM_PERMISSIONS;

            for (int i = 0; i < grantResults.length && good; i++)
            {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    good = false;
                    break;
                }
            }

            if (!good)
            {
                // permission denied, boo! Disable the app.
                mPermissionsNeeded = new AlertDialog.Builder(SplashActivity.this).setTitle(getString(R.string.assetExtractor_error))
                    .setMessage(getString(R.string.assetExtractor_failed_permissions))
                    .setPositiveButton(getString( android.R.string.ok ), (dialog, which) -> SplashActivity.this.finish()).setCancelable(false).show();
            }
            else
            {
                //Permissions already granted, continue
                checkExtractAssetsOrCleanup();
            }
        }
    }

    private void checkExtractAssetsOrCleanup()
    {
        if( mAppData.getAssetCheckNeeded() || mAppData.getAppVersion() != mAppData.appVersionCode ||
                !ExtractAssetsOrCleanupTask.areAllAssetsValid(PreferenceManager.getDefaultSharedPreferences(this),
                        SOURCE_DIR, mAppData.coreSharedDataDir))
        {
            mAppData.putAppVersion(mAppData.appVersionCode);

            // Extract the assets in a separate thread and launch the menu activity
            // Handler.postDelayed ensures this runs only after activity has resumed
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed( extractAssetsTaskLauncher, SPLASH_DELAY );
        }
        else
        {
            // Assets already extracted, just launch gallery activity, passing ROM path if it was provided externally
            ActivityHelper.startGalleryActivity( SplashActivity.this, getIntent() );

            // We never want to come back to this activity, so finish it
            finish();
        }
    }

    /** Runnable that launches the non-UI thread from the UI thread after the activity has resumed. */
    private final Runnable extractAssetsTaskLauncher = this::extractAssets;

    /**
     * Extract assets
     */
    private void extractAssets()
    {
        // Extract and merge the assets if they are out of date
        new ExtractAssetsOrCleanupTask( this, getAssets(), mAppData, mGlobalPrefs, SOURCE_DIR, mAppData.coreSharedDataDir, SplashActivity.this ).doInBackground();
    }

    @Override
    public void onExtractAssetsProgress( String nextFileToExtract, int currentAsset, int totalAssets )
    {
        runOnUiThread(() -> {
            final float percent = ( 100f * currentAsset ) / totalAssets;
            final String text = getString( R.string.assetExtractor_progress, percent, nextFileToExtract );
            mTextView.setText( text );
        });
    }

    @Override
    public void onExtractAssetsFinished( List<Failure> failures )
    {
        runOnUiThread(() -> {
            if (failures.size() != 0)
            {
                // Extraction failed, update the on-screen text and don't start next activity
                final String message = getString( R.string.assetExtractor_failed );

                StringBuilder builder = new StringBuilder();
                builder.append(message.replace( "\n", "<br/>" )).append("<p><small>");
                for( final Failure failure : failures )
                {
                    builder.append(failure.toString());
                    builder.append("<br/>");
                }
                builder.append("</small>");

                mTextView.setText( AppData.fromHtml( builder.toString() ) );
                Log.e("SplashActivity", "Setting text: " +  AppData.fromHtml( builder.toString() ));

                mAppData.putAssetCheckNeeded( true );

            } else {
                mAppData.putAssetCheckNeeded( false );
                mTextView.setText( R.string.assetExtractor_finished );
            }

            CheatUtils.mergeCheatFiles( mAppData.mupencheat_default, mGlobalPrefs.customCheats_txt, mAppData.mupencheat_txt );

            if(!RomDatabase.getInstance().hasDatabaseFile())
            {
                RomDatabase.getInstance().setDatabaseFile(mAppData.mupen64plus_ini);
            }

            // We never want to come back to this activity, so finish it
            final Handler handler = new Handler(Looper.getMainLooper());
            long delay = failures.size() != 0 ? 5000 : 0;
            handler.postDelayed(() -> {
                // Launch gallery activity, passing ROM path if it was provided externally
                ActivityHelper.startGalleryActivity( SplashActivity.this, getIntent() );
                SplashActivity.this.finish();
            }, delay);
        });
    }

    private void createChannel()
    {
        Channel.Builder builder = new Channel.Builder();

        Intent appIntent = new Intent(getApplicationContext(), SplashActivity.class);
        appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        builder.setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(getString(R.string.showRecentlyPlayed_title))
                .setAppLinkIntent(appIntent);

        Context context = getApplicationContext();

        try {
            Uri channelUri = context.getContentResolver().insert(
                    TvContractCompat.Channels.CONTENT_URI, builder.build().toContentValues());

            if (channelUri != null) {
                long channelId = ContentUris.parseId(channelUri);
                mAppData.putChannelId(channelId);
                Bitmap bitmapIcon = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
                ChannelLogoUtils.storeChannelLogo(context, channelId, bitmapIcon);
                TvContractCompat.requestChannelBrowsable(context, channelId);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}
