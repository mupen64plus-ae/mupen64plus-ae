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
package paulscode.android.mupen64plusae.netplay;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.mupen64plusae.v3.alpha.R;

@SuppressWarnings("FieldCanBeLocal")
public class NetplayService extends Service
{
    public interface NetplayServiceListener
    {
        /**
         * Will be called once a tcp server port is obtained
         * @param port The port that was obtained
         */
        void onPortObtained(int port);

        /**
         * Will be called once the service finishes
         */
        void onFinish();
    }

    private static final String TAG = "NetplayService";

    public static final String SERVICE_QUIT = "M64P_NETPLAY_SERVICE_QUIT";

    private int mStartId;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private boolean mRunning = false;
    private UdpServer mUdpServer;
    private TcpServer mTcpServer;
    NetplayServiceListener mNetplayServiceListener;

    private final IBinder mBinder = new LocalBinder();

    final static int ONGOING_NOTIFICATION_ID = 5;
    final static String NOTIFICATION_CHANNEL_ID = "NetplayServiceChannel";

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public NetplayService getService() {
            // Return this instance of this class so clients can call public methods
            return NetplayService.this;
        }
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(@NonNull Message msg) {

            final int bufferTarget = 2;
            mUdpServer = new UdpServer(bufferTarget);
            mTcpServer = new TcpServer(bufferTarget, mUdpServer);

            Log.i(TAG, "Netplay service started");

            mTcpServer.setPort(0);
            mNetplayServiceListener.onPortObtained(mTcpServer.getPort());
            mUdpServer.setPort(mTcpServer.getPort());

            mUdpServer.waitForServerToEnd();
            mTcpServer.waitForServerToEnd();

            Log.i(TAG, "Netplay service finished");
            mRunning = false;

            mNetplayServiceListener.onFinish();
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
                    getString(R.string.netplay_running_title), NotificationManager.IMPORTANCE_LOW);
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

        Intent stopIntent = new Intent(this, NetplayService.class);
        stopIntent.setAction(SERVICE_QUIT);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        //Show the notification
        initChannels(getApplicationContext());
        NotificationCompat.Builder builder =
          new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).setSmallIcon(R.drawable.icon)
          .setContentTitle(getString(R.string.netplay_running_title))
                  .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(R.drawable.ic_box, getString(R.string.inputMapActivity_stop), stopPendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, builder.build());
    }

        @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i("NetplayService", "onStartCommand");

        if(intent != null)
        {
            String action = intent.getAction();
            boolean quitMessage = action != null && action.equals(SERVICE_QUIT);

            if (quitMessage) {
                //Stop the service immediately
                stopServers();
            }
        }

        mStartId = startId;

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void startListening(NetplayServiceListener netplayServiceListener)
    {
        mNetplayServiceListener = netplayServiceListener;

        if (!mRunning) {
            // For each start request, send a message to start a job and deliver the
            // start ID so we know which request we're stopping when we finish the job
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = mStartId;
            mServiceHandler.sendMessage(msg);

            mRunning = true;
        }
    }

    public void stopServers() {
        Log.i("NetplayService", "Stopping netplay service");

        if (mUdpServer != null) {
            mUdpServer.stopServer();
        }

        if (mTcpServer != null) {
            mTcpServer.stopServer();
        }
    }


    @Override
    public void onDestroy()
    {
        // Stop the service using the startId, so that we don't stop
        // the service in the middle of handling another job
        stopForeground(true);
        stopSelf();
    }
}
