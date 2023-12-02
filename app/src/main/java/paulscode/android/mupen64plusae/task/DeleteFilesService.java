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
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

import paulscode.android.mupen64plusae.R;

import java.io.File;
import java.util.ArrayList;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.GalleryActivity;
import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.dialog.ProgressDialog.OnCancelListener;
import paulscode.android.mupen64plusae.util.FileUtil;

public class DeleteFilesService extends Service {
    private ArrayList<String> mDeletePath;
    private ArrayList<String> mDeleteFilter;

    private int mStartId;
    private ServiceHandler mServiceHandler;

    private final IBinder mBinder = new LocalBinder();
    private DeleteFilesListener mListener = null;

    final static int ONGOING_NOTIFICATION_ID = 1;

    final static String NOTIFICATION_CHANNEL_ID = "DeleteFilesServiceChannel";
    final static String NOTIFICATION_CHANNEL_ID_V2 = "DeleteFilesServiceChannelV2";

    public interface DeleteFilesListener {
        //This is called once deleting files is finished
        void onDeleteFilesFinished();

        //This is called when the service is destroyed
        void onDeleteFilesServiceDestroyed();

        //This is called to get a progress dialog object
        ProgressDialog GetProgressDialog();
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public DeleteFilesService getService() {
            // Return this instance of DeleteFilesService so clients can call public methods
            return DeleteFilesService.this;
        }
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            //Check for error conditions
            if (mDeletePath == null) {
                if (mListener != null) {
                    mListener.onDeleteFilesFinished();
                }

                stopSelf(msg.arg1);
                return;
            }

            for (int index = 0 ; index < mDeletePath.size(); ++index) {
                if (mDeletePath.get(index) != null) {
                    if (TextUtils.isEmpty(mDeleteFilter.get(index))) {
                        FileUtil.deleteFolder(new File(mDeletePath.get(index)));
                    } else {
                        FileUtil.deleteFileFilter(new File(mDeletePath.get(index)), mDeleteFilter.get(index));
                    }
                }
            }

            if (mListener != null) {
                mListener.onDeleteFilesFinished();
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
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_V2,
                    getString(R.string.pathDeletingFilesTask_title), NotificationManager.IMPORTANCE_LOW);
            channel.enableVibration(false);
            channel.setSound(null,null);
            notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID);
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
        Looper serviceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(serviceLooper);

        //Show the notification
        initChannels(getApplicationContext());
        Intent notificationIntent = new Intent(this, GalleryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_V2).setSmallIcon(R.drawable.icon)
                        .setContentTitle(getString(R.string.pathDeletingFilesTask_title))
                        .setContentText(getString(R.string.toast_pleaseWait))
                        .setContentIntent(pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Bundle extras = intent.getExtras();

            if (extras != null) {

                mDeletePath = extras.getStringArrayList(ActivityHelper.Keys.DELETE_PATH);
                mDeleteFilter = extras.getStringArrayList(ActivityHelper.Keys.DELETE_FILTER);
            }
        }

        mStartId = startId;

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mListener != null) {
            mListener.onDeleteFilesServiceDestroyed();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setDeleteFilesListener(DeleteFilesListener deleteFilesListener) {
        mListener = deleteFilesListener;
        mListener.GetProgressDialog().setOnCancelListener(() -> {

        });

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = mStartId;
        mServiceHandler.sendMessage(msg);
    }
}
