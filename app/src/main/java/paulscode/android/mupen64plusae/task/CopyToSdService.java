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
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.util.FileUtil;

@SuppressWarnings("FieldCanBeLocal")
public class CopyToSdService extends Service
{
    private File mSourcePath = null;
    private Uri mDestinationPath = null;
    private boolean mCancelled = false;
    
    private int mStartId;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private final IBinder mBinder = new LocalBinder();
    private CopyFilesListener mListener = null;

    final static int ONGOING_NOTIFICATION_ID = 1;
    final static String NOTIFICATION_CHANNEL_ID = "CopyFilesServiceChannel";
    
    public interface CopyFilesListener
    {
        //This is called once the task is complete
        void onCopyToSdFinished();
        
        //This is called when the service is destroyed
        void onCopyToSdServiceDestroyed();
        
        //This is called to get a progress dialog object
        ProgressDialog GetProgressDialog();
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public CopyToSdService getService() {
            // Return this instance of this class so clients can call public methods
            return CopyToSdService.this;
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
                    mListener.onCopyToSdFinished();
                }

                stopSelf(msg.arg1);
                return;
            }

            DocumentFile destLocation = FileUtil.getDocumentFileTree(getApplicationContext(), mDestinationPath);

            if (destLocation != null) {
                destLocation = FileUtil.createFolderIfNotPresent(getApplicationContext(), destLocation, AppData.applicationPath);
                if (destLocation != null) {
                    copyFolder(getApplicationContext(), mSourcePath, destLocation);
                }
            }
            
            if (mListener != null)
            {
                mListener.onCopyToSdFinished();
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

    /**
     * Copies a {@code src} {@link File} to a desired destination represented by a {@code dest}
     * {@link Uri}.
     *
     * @param context     Context to use to read the URI
     * @param src         Source file
     * @param dest        Desired destination
     *
     * @return True if the copy succeeded, false otherwise.
     */
    public boolean copyFolder( Context context, File src, DocumentFile dest )
    {
        if( src == null)
        {
            Log.e( "copyFile", "src null" );
            return false;
        }

        if(dest == null )
        {
            Log.e( "copyFile", "dest null" );
            return false;
        }

        // Do this instead for directly accessible files
        if(dest.getUri().getScheme().equals("file")) {

            File destFile = new File(dest.getUri().getPath() + "/" + src.getName());
            return FileUtil.copyFile( src, destFile, false );
        }

        boolean success = true;

        if( src.isDirectory() )
        {
            DocumentFile childFile = FileUtil.createFolderIfNotPresent(context, dest, src.getName());

            File[] files = src.listFiles();

            if (files != null) {
                for( File file : files ) {
                    success = success && copyFolder( context, file, childFile );

                    if (mCancelled) {
                        break;
                    }
                }
            }

            return success;
        }
        else
        {
            DocumentFile targetFile = dest.findFile(src.getName());

            if(targetFile == null){
                targetFile = dest.createFile("", src.getName());
            }

            if (targetFile == null) {
                return false;
            }

            try (ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(targetFile.getUri(), "w");
                 FileChannel in = new FileInputStream(src).getChannel();
                 FileChannel out = new FileOutputStream(parcelFileDescriptor.getFileDescriptor()).getChannel()) {

                long bytesTransferred = 0;

                while (bytesTransferred < in.size()) {
                    bytesTransferred += in.transferTo(bytesTransferred, in.size(), out);
                }

            } catch (Exception|OutOfMemoryError e) {
                Log.e("copyFile", "Exception: " + e.getMessage());
            }
        }

        return true;
    }

    public void initChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    getString(R.string.importExportActivity_exportDialogTitle), NotificationManager.IMPORTANCE_LOW);
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
          .setContentTitle(getString(R.string.importExportActivity_exportDialogTitle))
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

                if (filePath != null) {
                    mSourcePath = new File(filePath);
                }
                if (uriString != null) {
                    mDestinationPath = Uri.parse(uriString);
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
            mListener.onCopyToSdServiceDestroyed();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    public void setCopyToSdListener(CopyFilesListener copyFilesListener)
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
