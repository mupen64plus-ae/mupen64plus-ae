/**
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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.task;

import java.io.File;
import org.mupen64plusae.v3.fzurita.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.GalleryActivity;
import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.dialog.ProgressDialog.OnCancelListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.TextureInfo;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.widget.Toast;

public class ExtractTexturesService extends Service
{
    private String mZipPath;
    
    private int mStartId;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private final IBinder mBinder = new LocalBinder();
    private ExtractTexturesListener mListener = null;

    final static int ONGOING_NOTIFICATION_ID = 1;
    
    public interface ExtractTexturesListener
    {
        //This is called once the ROM scan is finished
        void onExtractTexturesFinished();
        
        //This is called when the service is destroyed
        void onExtractTexturesServiceDestroyed();
        
        //This is called to get a progress dialog object
        ProgressDialog GetProgressDialog();
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public ExtractTexturesService getService() {
            // Return this instance of ExtractTexturesService so clients can call public methods
            return ExtractTexturesService.this;
        }
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg) {

            File searchPathFile = new File(mZipPath);
            
            if( mZipPath == null )
                throw new IllegalArgumentException( "Root path cannot be null" );
            if( !searchPathFile.exists() )
                throw new IllegalArgumentException( "Root path does not exist: " + searchPathFile.getAbsolutePath() );
            
            AppData appData = new AppData( ExtractTexturesService.this );
            GlobalPrefs globalPrefs = new GlobalPrefs( ExtractTexturesService.this, appData );

            if(mZipPath.toLowerCase().endsWith("zip"))
            {
                String headerName = TextureInfo.getTexturePackName( mZipPath );
                if( !TextUtils.isEmpty( headerName ) )
                {
                    String outputFolder = globalPrefs.hiResTextureDir + headerName;
                    FileUtil.deleteFolder( new File( outputFolder ) );
                    FileUtil.unzipAll( new File( mZipPath ), outputFolder );
                }
                else
                {
                    final String text = getString(R.string.pathHiResTexturesTask_errorMessage);

                    Handler handler = new Handler(Looper.getMainLooper());

                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(ExtractTexturesService.this.getApplicationContext(),text,Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            else if(mZipPath.toLowerCase().endsWith("htc"))
            {
                if(mZipPath.toLowerCase().endsWith("_hirestextures.htc"))
                {
                    FileUtil.copyFile(new File(mZipPath), new File(globalPrefs.textureCacheDir + "/" + new File(mZipPath).getName()));
                }
                else
                {
                    final String text = getString(R.string.pathHiResTexturesTask_errorMessageInvalidHTC);

                    Handler handler = new Handler(Looper.getMainLooper());

                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(ExtractTexturesService.this.getApplicationContext(),text,Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            
            if (mListener != null)
            {
                mListener.onExtractTexturesFinished();
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
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
      Intent notificationIntent = new Intent(this, GalleryActivity.class);
      PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
      NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.icon)
          .setContentTitle(getString(R.string.pathHiResTexturesTask_title))
          .setContentText(getString(R.string.toast_pleaseWait))
          .setContentIntent(pendingIntent);
      startForeground(ONGOING_NOTIFICATION_ID, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null)
        {
            Bundle extras = intent.getExtras();
            mZipPath = extras.getString( ActivityHelper.Keys.SEARCH_PATH );
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
            mListener.onExtractTexturesServiceDestroyed();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    public void setExtractTexturesListener(ExtractTexturesListener extractTexturesListener)
    {
        mListener = extractTexturesListener;
        mListener.GetProgressDialog().setOnCancelListener(new OnCancelListener()
        {
            @Override
            public void OnCancel()
            {

            }
        });
        
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = mStartId;
        mServiceHandler.sendMessage(msg);
    }
}
