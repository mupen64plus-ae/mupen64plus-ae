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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;

import paulscode.android.mupen64plusae.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.ImportExportActivity;
import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.util.FileUtil;

@SuppressWarnings("FieldCanBeLocal")
public class CopyFromSdService extends Service
{
    private Uri mSourcePath = null;
    private File mDestinationPath = null;
    
    private int mStartId;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private boolean mCancelled = false;

    private final IBinder mBinder = new LocalBinder();
    private CopyFilesListener mListener = null;

    final static int ONGOING_NOTIFICATION_ID = 1;
    final static String NOTIFICATION_CHANNEL_ID = "CopyFilesServiceChannel";
    
    public interface CopyFilesListener
    {
        //This is called once the task is complete
        void onCopyFromSdFinished();
        
        //This is called when the service is destroyed
        void onCopyFromSdServiceDestroyed();
        
        //This is called to get a progress dialog object
        ProgressDialog GetProgressDialog();
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public CopyFromSdService getService() {
            // Return this instance of this class so clients can call public methods
            return CopyFromSdService.this;
        }
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(@NonNull Message msg) {

            mCancelled = false;

            //Check for error conditions
            if( mSourcePath == null || mDestinationPath == null)
            {
                if (mListener != null)
                {
                    mListener.onCopyFromSdFinished();
                }

                stopSelf(msg.arg1);
                return;
            }

            DocumentFile sourceLocation = FileUtil.getDocumentFileTree(getApplicationContext(), mSourcePath);
            if (sourceLocation != null && sourceLocation.getName() != null) {
                if (!sourceLocation.getName().equals(mDestinationPath.getName())) {
                    sourceLocation = sourceLocation.findFile(mDestinationPath.getName());
                }
                copyFolder(sourceLocation, mDestinationPath );
            }
            
            if (mListener != null)
            {
                mListener.onCopyFromSdFinished();
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

    /**
     * Copies a {@code src} {@link DocumentFile} to a desired destination represented by a {@code dest}
     * {@link File}.
     *
     * @param src         Source file
     * @param dest        Desired destination
     *
     */
    public void copyFolder(DocumentFile src, File dest )
    {
        if(src == null)
        {
            Log.e( "copyFile", "src null" );
            return;
        }

        if( dest == null )
        {
            Log.e( "copyFile", "dest null" );
            return;
        }

        if (src.isDirectory()) {
            FileUtil.makeDirs(dest.getPath());

            DocumentFile[] files = src.listFiles();
            for (DocumentFile file : files) {
                File newDest = new File(dest.getAbsolutePath() + "/" + file.getName());
                copyFolder(file, newDest );

                if (mCancelled) {
                    break;
                }
            }
        } else {
            try (ParcelFileDescriptor parcelFileDescriptor = getApplicationContext().getContentResolver().openFileDescriptor(src.getUri(), "r");
                 FileChannel in = new FileInputStream(parcelFileDescriptor.getFileDescriptor()).getChannel();
                 FileChannel out = new FileOutputStream(dest).getChannel()) {

                long bytesTransferred = 0;

                while (bytesTransferred < in.size()) {
                    bytesTransferred += in.transferTo(bytesTransferred, in.size(), out);
                }

            } catch (Exception|OutOfMemoryError e) {
                Log.e("copyFile", "Exception: " + e.getMessage());
            }
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
                    getString(R.string.importExportActivity_importDialogTitle), NotificationManager.IMPORTANCE_LOW);
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
      Intent notificationIntent = new Intent(this, ImportExportActivity.class);
      PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
      NotificationCompat.Builder builder =
          new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).setSmallIcon(R.drawable.icon)
          .setContentTitle(getString(R.string.importExportActivity_importDialogTitle))
          .setContentText(getString(R.string.toast_pleaseWait))
          .setContentIntent(pendingIntent);
      startForeground(ONGOING_NOTIFICATION_ID, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null)
        {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String filePath = extras.getString(ActivityHelper.Keys.FILE_PATH);
                String uriString = extras.getString(ActivityHelper.Keys.FILE_URI);

                if (uriString != null) {
                    mSourcePath = Uri.parse(uriString);
                }
                if (filePath != null) {
                    mDestinationPath = new File(filePath);
                }
            }
        }

        mStartId = startId;

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }
    
    @Override
    public void onDestroy()
    {        
        if (mListener != null)
        {
            mListener.onCopyFromSdServiceDestroyed();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    public void setCopyFromSdListener(CopyFilesListener copyFilesListener)
    {
        mCancelled = false;

        mListener = copyFilesListener;
        mListener.GetProgressDialog().setOnCancelListener(() -> mCancelled = true);
        
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = mStartId;
        mServiceHandler.sendMessage(msg);
    }
}
