/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 *
 * Copyright (C) 2020 Paul Lamb
 *
 * This file is part of Mupen64PlusAE.
 *
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * Authors: fzurita
 */
package paulscode.android.mupen64plusae.task;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDiskIOException;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.tvprovider.media.tv.PreviewProgram;
import androidx.tvprovider.media.tv.TvContractCompat;

import paulscode.android.mupen64plusae.R;

import java.io.File;
import java.util.List;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.GalleryActivity;
import paulscode.android.mupen64plusae.GalleryItem;
import paulscode.android.mupen64plusae.SplashActivity;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.FileUtil;

/**
 * Syncs programs for a channel. A channel id is required to be passed via the {@link
 * JobParameters}. This service is scheduled to listen to changes to a channel. Once the job
 * completes, it will reschedule itself to listen for the next change to the channel.
 */
public class SyncProgramsJobService extends JobService implements GalleryRefreshTask.GalleryRefreshFinishedListener {

    private static final String TAG = "SyncProgramsJobService";

    private static final long CHANNEL_JOB_ID_OFFSET = 1000;

    static public class StartupIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                    AppData appData = new AppData( context );
                    syncProgramsForChannel(context, appData.getChannelId());
                }
            }
        }
    }

    private static int getJobIdForChannelId(long channelId) {
        return (int) (CHANNEL_JOB_ID_OFFSET + channelId);
    }

    /**
     * Schedulers syncing programs for a channel. The scheduler will listen to a {@link Uri} for a
     * particular channel.
     *
     * @param context for accessing the JobScheduler.
     * @param channelId for the channel to listen for changes.
     */
    public static void scheduleSyncingProgramsForChannel(Context context, long channelId)
    {
        AppData appData = new AppData(context);

        if (AppData.IS_OREO && appData.isAndroidTv) {
            ComponentName componentName = new ComponentName(context, SyncProgramsJobService.class);

            JobInfo.Builder builder = new JobInfo.Builder(getJobIdForChannelId(channelId), componentName);

            JobInfo.TriggerContentUri triggerContentUri = new JobInfo.TriggerContentUri(
                TvContractCompat.buildChannelUri(channelId),
                 JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS);
            builder.addTriggerContentUri(triggerContentUri);
            builder.setTriggerContentMaxDelay(0L);
            builder.setTriggerContentUpdateDelay(0L);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putLong(TvContractCompat.EXTRA_CHANNEL_ID, channelId);
            builder.setExtras(bundle);

            try {
                JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                if (scheduler != null) {
                    scheduler.cancel(getJobIdForChannelId(channelId));
                    scheduler.schedule(builder.build());
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Schedulers syncing programs for a channel. The scheduler will listen to a {@link Uri} for a
     * particular channel.
     *
     * @param context for accessing the JobScheduler.
     * @param channelId for the channel to listen for changes.
     */
    public static void syncProgramsForChannel(Context context, long channelId)
    {
        AppData appData = new AppData(context);
        if (AppData.IS_OREO && appData.isAndroidTv) {
            ComponentName componentName = new ComponentName(context, SyncProgramsJobService.class);

            JobInfo.Builder builder = new JobInfo.Builder(getJobIdForChannelId(channelId), componentName);

            builder.setMinimumLatency(1);
            builder.setOverrideDeadline(1);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putLong(TvContractCompat.EXTRA_CHANNEL_ID, channelId);
            builder.setExtras(bundle);

            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (scheduler != null) {
                scheduler.cancel(getJobIdForChannelId(channelId));
                scheduler.schedule(builder.build());
            }
        }
    }

    private long mChannelId = -1;

    JobParameters mJobParameters;
    GalleryRefreshTask mGalleryRefreshTask = null;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.d(TAG, "onStartJob(): " + jobParameters);

        mJobParameters = jobParameters;
        PersistableBundle extras = jobParameters.getExtras();
        long channelId = extras.getLong(TvContractCompat.EXTRA_CHANNEL_ID, -1L);

        if (channelId == -1L) {
            return false;
        }
        Log.d(TAG, "onStartJob(): Scheduling syncing for programs for channel " + channelId);
        mChannelId = channelId;

        AppData appData = new AppData( getApplicationContext() );
        GlobalPrefs globalPrefs = new GlobalPrefs( getApplicationContext(), appData );
        ConfigFile config = new ConfigFile(globalPrefs.romInfoCacheCfg);

        mGalleryRefreshTask = new GalleryRefreshTask(SyncProgramsJobService.this,
                getApplicationContext(), globalPrefs, "", config);
        mGalleryRefreshTask.doInBackground();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

    /*
     * Syncs programs by querying the given channel id.
     */
    private void syncPrograms(long channelId, List<GalleryItem> galleryItems) {
        Log.d(TAG, "Sync programs for channel: " + channelId);
        // Clear out the programs first
        try {
            getApplicationContext().getContentResolver().delete(
                    TvContractCompat.buildPreviewProgramsUriForChannel(mChannelId),
                    null, null);
        } catch (IllegalArgumentException|SQLiteDiskIOException e) {
            e.printStackTrace();
        }


        createPrograms(mChannelId, galleryItems);

    }

    private void createPrograms(long channelId, List<GalleryItem> galleryItems) {
        for (GalleryItem galleryItem : galleryItems) {
            PreviewProgram previewProgram = buildProgram(channelId, galleryItem);

            try {
                getContentResolver().insert(
                        TvContractCompat.PreviewPrograms.CONTENT_URI,
                        previewProgram.toContentValues());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @NonNull
    private PreviewProgram buildProgram(long channelId, GalleryItem item) {
        // Add recently played games to channel
        Uri coverArtUri;
        int aspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_3_2;

        // Create cover art link
        if (!TextUtils.isEmpty(item.artPath) && new File(item.artPath).exists()) {
            coverArtUri = FileUtil.buildBanner(getApplicationContext(), item.artPath);

            // Determine aspect ratio
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(item.artPath, options);
            int width = options.outWidth;
            int height = options.outHeight;

            if (height > width)
            {
                aspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_2_3;
            }

            if (height == -1 || width == -1) {
                coverArtUri = FileUtil.resourceToUri(getApplicationContext(), R.drawable.default_coverart);
            }

        } else {
            coverArtUri = FileUtil.resourceToUri(getApplicationContext(), R.drawable.default_coverart);
        }

        Intent gameIntent = new Intent(getApplicationContext(), SplashActivity.class);

        gameIntent.putExtra(GalleryActivity.KEY_IS_LEANBACK, true);
        gameIntent.putExtra(ActivityHelper.Keys.ROM_PATH, item.romUri);
        gameIntent.putExtra(ActivityHelper.Keys.ZIP_PATH, item.zipUri);
        gameIntent.putExtra(ActivityHelper.Keys.ROM_MD5, item.md5);
        gameIntent.putExtra(ActivityHelper.Keys.ROM_CRC, item.crc);
        gameIntent.putExtra(ActivityHelper.Keys.ROM_HEADER_NAME, item.headerName);
        gameIntent.putExtra(ActivityHelper.Keys.ROM_COUNTRY_CODE, item.countryCode.getValue());
        gameIntent.putExtra(ActivityHelper.Keys.ROM_ART_PATH, item.artPath);
        gameIntent.putExtra(ActivityHelper.Keys.ROM_GOOD_NAME, item.goodName);
        gameIntent.putExtra(ActivityHelper.Keys.ROM_DISPLAY_NAME, item.displayName);
        gameIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        PreviewProgram.Builder builder = new PreviewProgram.Builder();
        builder.setChannelId(channelId)
                .setType(TvContractCompat.PreviewPrograms.TYPE_GAME)
                .setTitle(item.displayName)
                .setPosterArtUri(coverArtUri)
                .setPosterArtAspectRatio(aspectRatio)
                .setIntent(gameIntent);

        return builder.build();
    }

    @Override
    public void onGalleryRefreshFinished(List<GalleryItem> items, List<GalleryItem> allItems, List<GalleryItem> recentItems)
    {
        syncPrograms(mChannelId, recentItems);
        // Daisy chain listening for the next change to the channel.
        scheduleSyncingProgramsForChannel(SyncProgramsJobService.this, mChannelId);
        jobFinished(mJobParameters, false);
    }
}
