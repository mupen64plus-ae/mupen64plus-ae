/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;

import paulscode.android.mupen64plusae.R;

import java.io.File;
import java.io.IOException;
import java.util.List;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.GalleryActivity;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.DriveServiceHelper;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.GoogleDriveFileHolder;

@SuppressWarnings("FieldCanBeLocal")
public class SyncToGoogleDriveService extends JobService
{
    private static String TAG = "SyncToGoogleDriveService";

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private JobParameters mParams;
    final static int JOB_ID = 97;

    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;

    final static int ONGOING_NOTIFICATION_ID = 1;
    final static String NOTIFICATION_CHANNEL_ID = "CopyFilesServiceChannel";

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public SyncToGoogleDriveService getService() {
            // Return this instance of this class so clients can call public methods
            return SyncToGoogleDriveService.this;
        }
    }

    private DocumentFile getExternalGameFolder(String gameFolderName)
    {
        File gameDataFolder = new File(mAppData.gameDataDir);

        DocumentFile sourceLocation = FileUtil.getDocumentFileTree(getApplicationContext(), Uri.parse(mGlobalPrefs.externalFileStoragePath));
        if (sourceLocation != null) {
            sourceLocation = sourceLocation.findFile(gameDataFolder.getName());

            if (sourceLocation != null) {
                sourceLocation = sourceLocation.findFile(gameFolderName);
            }
        }

        return sourceLocation;
    }

    private DocumentFile getExternalFlatGameFolder()
    {
        File gameDataFolder = new File(mAppData.gameDataDir);

        DocumentFile sourceLocation = FileUtil.getDocumentFileTree(getApplicationContext(), Uri.parse(mGlobalPrefs.externalFileStoragePath));
        if (sourceLocation != null) {
            sourceLocation = sourceLocation.findFile(gameDataFolder.getName());
        }

        return sourceLocation;
    }

    private DocumentFile getInternalGameFolder(String gameFolderName)
    {
        File gameDataFolder = new File(mAppData.gameDataDir + "/" + gameFolderName);
        return FileUtil.getDocumentFileTree(getApplicationContext(), Uri.fromFile(gameDataFolder));
    }

    private DocumentFile getInternalFlatGameFolder()
    {
        File gameDataFolder = new File(mAppData.gameDataDir);
        return FileUtil.getDocumentFileTree(getApplicationContext(), Uri.fromFile(gameDataFolder));
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        private void backupGameData(DriveServiceHelper driveServiceHelper, String gameFolderName, String gameGoodName, String gameHeaderName)
        {
            DocumentFile sourceData;
            DocumentFile sourceFlataData;
            // Copy game data from external storage
            if (mGlobalPrefs.useExternalStorge) {
                sourceData = getExternalGameFolder(gameFolderName);
                sourceFlataData = getExternalFlatGameFolder();
            } else {
                sourceData = getInternalGameFolder(gameFolderName);
                sourceFlataData = getInternalFlatGameFolder();
            }

            if (sourceData == null || sourceFlataData == null) {
                Log.w(TAG, "Unable to retrieve game data folder");
                return;
            }

            try {
                GoogleDriveFileHolder driveFile = driveServiceHelper.createFolderIfNotExist(getString(R.string.app_name_pro), null);

                if (driveFile == null) {
                    Log.w(TAG, "Unable to create google drive folder");
                    return;
                }
                // Delete the old folder before uploading the new one
                GoogleDriveFileHolder gameFolder = driveServiceHelper.getExistingFolder(sourceData.getName(), driveFile.getId());

                if (gameFolder != null) {
                    driveServiceHelper.deleteFolderFile(gameFolder.getId());
                }


                // Delete the old flat data file before uploading new one
                List<GoogleDriveFileHolder> files = driveServiceHelper.getExistingFilesStartWith(gameGoodName, driveFile.getId());

                for (GoogleDriveFileHolder file : files) {
                    driveServiceHelper.deleteFolderFile(file.getId());
                }

                files = driveServiceHelper.getExistingFilesStartWith(gameHeaderName, driveFile.getId());

                for (GoogleDriveFileHolder file : files) {
                    driveServiceHelper.deleteFolderFile(file.getId());
                }

                driveServiceHelper.uploadFolder(getApplicationContext(), sourceData, driveFile.getId());
                driveServiceHelper.uploadFilesThatStartWith(getApplicationContext(), sourceFlataData, driveFile.getId(), gameGoodName);
                driveServiceHelper.uploadFilesThatStartWith(getApplicationContext(), sourceFlataData, driveFile.getId(), gameHeaderName);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void handleMessage(@NonNull Message msg)
        {
            Scope driveFileScope = new Scope(Scopes.DRIVE_FILE);
            Scope emailScope = new Scope(Scopes.EMAIL);

            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

            if (GoogleSignIn.hasPermissions(account, driveFileScope, emailScope)) {

                DriveServiceHelper driveServiceHelper = new DriveServiceHelper(
                        DriveServiceHelper.getGoogleDriveService(getApplicationContext(), account, getString(R.string.app_name_pro)));

                Bundle data = msg.getData();
                String sourceGameDataDirName = data.getString(ActivityHelper.Keys.FILE_PATH);
                String gameGoodName = data.getString(ActivityHelper.Keys.ROM_GOOD_NAME);
                String gameHeaderName = data.getString(ActivityHelper.Keys.ROM_HEADER_NAME);

                if (sourceGameDataDirName != null && gameGoodName != null && gameHeaderName != null) {

                    //Show the notification
                    initChannels(getApplicationContext());
                    NotificationCompat.Builder builder =
                            new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID).setSmallIcon(R.drawable.icon)
                                    .setContentTitle(getString(R.string.exportGoogleDriveService_exportNotificationTitle))
                                    .setContentText(sourceGameDataDirName);
                    startForeground(ONGOING_NOTIFICATION_ID, builder.build());

                    // Backup the data
                    Log.d(TAG, "backing up: " + sourceGameDataDirName);
                    backupGameData(driveServiceHelper, sourceGameDataDirName, gameGoodName, gameHeaderName);
                    Log.d(TAG, "done backing up: " + sourceGameDataDirName);
                }
            }

            jobFinished(mParams,false);

            //Stop the service
            stopForeground(true);
            stopSelf();
        }
    }

    public void initChannels(Context context) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    getString(R.string.exportGoogleDriveService_exportNotificationTitle), NotificationManager.IMPORTANCE_LOW);
            channel.enableVibration(false);
            channel.setSound(null,null);

            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onCreate() {
      // Start up the thread running the service.  Note that we create a
      // separate thread because the service normally runs in the process's
      // main thread, which we don't want to block.  We also make it
      // background priority so CPU-intensive work will not disrupt our UI.
      HandlerThread thread = new HandlerThread("ServiceStartArguments",
              Process.THREAD_PRIORITY_BACKGROUND);
      thread.start();

      // Get the HandlerThread's Looper and use it for our Handler
      mServiceLooper = thread.getLooper();
      mServiceHandler = new ServiceHandler(mServiceLooper);

      mAppData = new AppData(this);
      mGlobalPrefs = new GlobalPrefs( this, mAppData );
    }

    /**
     * Schedulers syncing programs for a channel. The scheduler will listen to a {@link Uri} for a
     * particular channel.
     *
     * @param context for accessing the JobScheduler.
     * @param sourceGameDataDirName Foldet name to upload
     */
    public static void syncToGoogleDrive(Context context, String sourceGameDataDirName, String gameGoodName, String gameHeader, boolean nowRequired )
    {
        if (context == null || sourceGameDataDirName == null || gameGoodName == null || gameHeader == null) {
            return;
        }

        AppData appData = new AppData(context);
        GlobalPrefs globalPrefs = new GlobalPrefs( context, appData );

        ComponentName componentName = new ComponentName(context, SyncToGoogleDriveService.class);

        int hash = sourceGameDataDirName.hashCode();
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID + hash, componentName);

        builder.setMinimumLatency(1);
        builder.setPersisted(true);

        if (globalPrefs.backupOverCellData || nowRequired) {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        } else {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);
        }

        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(ActivityHelper.Keys.FILE_PATH, sourceGameDataDirName);
        bundle.putString(ActivityHelper.Keys.ROM_GOOD_NAME, gameGoodName);
        bundle.putString(ActivityHelper.Keys.ROM_HEADER_NAME, gameHeader);
        builder.setExtras(bundle);

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) {
            scheduler.cancel(JOB_ID + hash);
            scheduler.schedule(builder.build());
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {

        if(params != null)
        {
            PersistableBundle extras = params.getExtras();
            String sourceGameDataDirName = extras.getString(ActivityHelper.Keys.FILE_PATH);
            String gameGoodName = extras.getString(ActivityHelper.Keys.ROM_GOOD_NAME);
            String gameHeaderName = extras.getString(ActivityHelper.Keys.ROM_HEADER_NAME);

            if (sourceGameDataDirName != null && gameGoodName != null && gameHeaderName != null) {
                Log.d(TAG, "New path added: " + sourceGameDataDirName + " good name=" + gameGoodName +
                        " game header=" + gameHeaderName);
                mParams = params;

                // For each start request, send a message to start a job and deliver the
                // start ID so we know which request we're stopping when we finish the job
                Message msgHandler = mServiceHandler.obtainMessage();
                Bundle data = new Bundle();
                data.putString(ActivityHelper.Keys.FILE_PATH, sourceGameDataDirName);
                data.putString(ActivityHelper.Keys.ROM_GOOD_NAME, gameGoodName);
                data.putString(ActivityHelper.Keys.ROM_HEADER_NAME, gameHeaderName);
                msgHandler.setData(data);
                mServiceHandler.sendMessage(msgHandler);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
