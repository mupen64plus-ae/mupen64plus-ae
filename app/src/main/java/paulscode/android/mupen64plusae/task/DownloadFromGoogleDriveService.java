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
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;

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

import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.DataPrefsActivity;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.DriveServiceHelper;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.GoogleDriveFileHolder;

@SuppressWarnings("FieldCanBeLocal")
public class DownloadFromGoogleDriveService extends Service
{
    private int mStartId;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private final IBinder mBinder = new LocalBinder();
    private DownloadFilesListener mListener = null;
    private boolean mbStopped = false;

    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;

    final static int ONGOING_NOTIFICATION_ID = 1;
    final static String NOTIFICATION_CHANNEL_ID = "CopyFilesServiceChannel";
    
    public interface DownloadFilesListener
    {
        //This is called once the task is complete
        void onDownloadFinished();
        
        //This is called when the service is destroyed
        void onServiceDestroyed();
        
        //This is called to get a progress dialog object
        ProgressDialog GetProgressDialog();
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public DownloadFromGoogleDriveService getService() {
            // Return this instance of this class so clients can call public methods
            return DownloadFromGoogleDriveService.this;
        }
    }

    private DocumentFile getExternalGameFolder()
    {
        if (TextUtils.isEmpty(mGlobalPrefs.externalFileStoragePath)) {
            return null;
        }
        return FileUtil.getDocumentFileTree(getApplicationContext(), Uri.parse(mGlobalPrefs.externalFileStoragePath));
    }

    private DocumentFile getInternalGameFolder()
    {
        File gameDataFolder = new File(mAppData.gameDataDir).getParentFile();
        return FileUtil.getDocumentFileTree(getApplicationContext(), Uri.fromFile(gameDataFolder));
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
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

                // Copy game data to external storage
                DocumentFile destData;
                if (mGlobalPrefs.useExternalStorge) {
                    destData = getExternalGameFolder();
                } else {
                    destData = getInternalGameFolder();
                }

                if (destData != null) {
                    try {
                        File gameDataFolder = new File(mAppData.gameDataDir);
                        DocumentFile gameDataDir = FileUtil.createFolderIfNotPresent(getApplicationContext(), destData, gameDataFolder.getName());

                        // Delete the old folder before uploading the new one
                        GoogleDriveFileHolder gameFolder = driveServiceHelper.getExistingFolder(getString(R.string.app_name_pro), null);

                        if (gameFolder != null && gameDataDir != null && !TextUtils.isEmpty(gameDataDir.getName())) {
                            List<GoogleDriveFileHolder> files = driveServiceHelper.queryFiles(gameFolder.getId());

                            for (GoogleDriveFileHolder file : files) {
                                int maxStringLength = 30;
                                int endIndex = Math.min(file.getName().length(), maxStringLength);
                                mListener.GetProgressDialog().setText(file.getName().substring(0, endIndex));
                                driveServiceHelper.downloadFolder(getApplicationContext(), gameDataDir, file);

                                if (mbStopped) break;
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            if (mListener != null)
            {
                mListener.onDownloadFinished();
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
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
                    getString(R.string.importGoogleDriveService_importNotificationTitle), NotificationManager.IMPORTANCE_LOW);
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

        //Show the notification
        initChannels(getApplicationContext());
        Intent notificationIntent = new Intent(this, DataPrefsActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).setSmallIcon(R.drawable.icon)
                        .setContentTitle(getString(R.string.importGoogleDriveService_importNotificationTitle))
                        .setContentText(getString(R.string.toast_pleaseWait))
                        .setContentIntent(pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, builder.build());

        mAppData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStartId = startId;

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }
    
    @Override
    public void onDestroy()
    {        
        if (mListener != null)
        {
            mListener.onServiceDestroyed();
        }
    }

    void stop() {
        mbStopped = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    public void setDownloadFilesListener(DownloadFilesListener downloadFilesListener)
    {
        mListener = downloadFilesListener;
        mListener.GetProgressDialog().setOnCancelListener(this::stop);
        
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = mStartId;
        mbStopped = false;

        mServiceHandler.sendMessage(msg);
    }
}
