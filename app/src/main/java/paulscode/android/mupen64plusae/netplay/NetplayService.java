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
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.RouteInfo;
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

import com.sun.jna.Native;

import paulscode.android.mupen64plusae.R;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;

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
         * Called when a desync is detected
         * @param vi VI in which de-sync occurred
         */
        void onDesync(int vi);

        /**
         * Will be called once the service finishes
         */
        void onFinish();

        /**
         * Callback when a UDP port has been mapped
         * @param tcpPort1 Port for room server
         * @param tcpPort2 Port for TCP netplay server
         * @param udpPort2 Port for UDP netplay server
         */
        void onUpnpPortsObtained(int tcpPort1, int tcpPort2, int udpPort2);
    }

    private static final String TAG = "NetplayService";

    public static final String SERVICE_QUIT = "M64P_NETPLAY_SERVICE_QUIT";

    private GlobalPrefs mGlobalPrefs = null;

    private int mStartId;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private boolean mRunning = false;
    private UdpServer mUdpServer;
    private TcpServer mTcpServer;
    private NetplayServiceListener mNetplayServiceListener;
    private boolean mPortMappingEnabled = false;
    private int mRoomPort = -1;
    private final MiniUpnpLibrary mMiniUpnpLibrary = Native.load("miniupnp-bridge", MiniUpnpLibrary.class);
    private final Object mUpnpSyncObject = new Object();
    private boolean mShuttingDown = false;

    // Set to true if we are currently using UPnP for port forwarding false if we are using NAT-PMP
    private boolean mUsingUpnp = true;

    private final IBinder mBinder = new LocalBinder();

    private final static int ONGOING_NOTIFICATION_ID = 5;
    private final static String NOTIFICATION_CHANNEL_ID = "NetplayServiceChannel";

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder
    {
        public NetplayService getService()
        {
            // Return this instance of this class so clients can call public methods
            return NetplayService.this;
        }
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler
    {
        ServiceHandler(Looper looper)
        {
            super(looper);
        }
        
        @Override
        public void handleMessage(@NonNull Message msg)
        {
            // Run at higher priority to try to prevent skips
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            final int bufferTarget = 2;
            mUdpServer = new UdpServer(bufferTarget, vi -> mNetplayServiceListener.onDesync(vi));
            mTcpServer = new TcpServer(bufferTarget, mUdpServer);

            Log.i(TAG, "Netplay service started");

            AppData appData = new AppData(getApplicationContext());
            mGlobalPrefs = new GlobalPrefs(getApplicationContext(), appData);
            int port = mGlobalPrefs.useUpnpToMapNetplayPorts ? 0 : mGlobalPrefs.netplayServerUdpTcpPort;

            mTcpServer.setPort(port);
            mNetplayServiceListener.onPortObtained(mTcpServer.getPort());
            mUdpServer.setPort(mTcpServer.getPort());

            mUdpServer.waitForServerToEnd();
            mTcpServer.waitForServerToEnd();

            Log.i(TAG, "Netplay service finished");
            mRunning = false;

            mNetplayServiceListener.onFinish();

            synchronized (mUpnpSyncObject) {
                mShuttingDown = true;
                Log.i(TAG, "Shutting down ports");

                if (mUsingUpnp) {
                    mMiniUpnpLibrary.UPnPShutdown();
                } else {
                    mMiniUpnpLibrary.NATPMP_Shutdown();
                }
            }
        }
    }

    public void initChannels(Context context)
    {
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
    public void onCreate()
    {
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
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

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
    public int onStartCommand(Intent intent, int flags, int startId)
    {

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

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public void stopServers()
    {
        Log.i("NetplayService", "Stopping netplay service");

        if (mUdpServer != null) {
            mUdpServer.stopServer();
        }

        if (mTcpServer != null) {
            mTcpServer.stopServer();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }

    public void mapPorts(int roomPort)
    {
        if (!mPortMappingEnabled && mGlobalPrefs.useUpnpToMapNetplayPorts) {
            mPortMappingEnabled = true;
            mRoomPort = roomPort;

            Thread mappingThread = new Thread(this::mapPorts);
            mappingThread.setDaemon(true);
            mappingThread.start();
        }

        if (!mGlobalPrefs.useUpnpToMapNetplayPorts) {
            mNetplayServiceListener.onUpnpPortsObtained(mGlobalPrefs.netplayRoomTcpPort,
                    mGlobalPrefs.netplayServerUdpTcpPort, mGlobalPrefs.netplayServerUdpTcpPort);
        }
    }

    private boolean mapPortsUpnp()
    {
        mUsingUpnp = true;

        mMiniUpnpLibrary.UPnPInit(2000);
        boolean port1Success = mMiniUpnpLibrary.UPnP_Add("TCP","M64Plus Room", mRoomPort, mRoomPort);
        boolean port2Success = mMiniUpnpLibrary.UPnP_Add("TCP", "M64Plus Core TCP", mTcpServer.getPort(), mTcpServer.getPort());
        boolean port3Success = mMiniUpnpLibrary.UPnP_Add("UDP", "M64Plus Core UDP", mTcpServer.getPort(), mTcpServer.getPort());

        return port1Success && port2Success && port3Success;
    }

    private boolean mapPortsNatPmp(int gatewayAddress)
    {
        mUsingUpnp = false;

        mMiniUpnpLibrary.NATPMP_Init(gatewayAddress);
        boolean port1Success = mMiniUpnpLibrary.NATPMP_Add("TCP", mRoomPort, mRoomPort);
        boolean port2Success = mMiniUpnpLibrary.NATPMP_Add("TCP", mTcpServer.getPort(), mTcpServer.getPort());
        boolean port3Success = mMiniUpnpLibrary.NATPMP_Add("UDP", mTcpServer.getPort(), mTcpServer.getPort());

        return port1Success && port2Success && port3Success;
    }

    private void actuallyMapPorts(int gatewayAddress)
    {
        boolean success = mapPortsNatPmp(gatewayAddress);

        if (!success) {
            Log.w(TAG, "NAT-PMP port forwading failed, trying UPnP");
            success = mapPortsUpnp();
        }

        if (success) {
            mNetplayServiceListener.onUpnpPortsObtained(mRoomPort, mTcpServer.getPort(), mTcpServer.getPort());
        } else {
            Log.w(TAG, "UPnP port forwading failed");
            mNetplayServiceListener.onUpnpPortsObtained(-1, -1, -1);
        }
    }

    private void mapPorts()
    {
        synchronized (mUpnpSyncObject) {
            if (mShuttingDown) {
                return;
            }

            final ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET);
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

            final NetworkRequest networkRequest = builder.build();

            ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback()
            {
                public void onAvailable(Network network)
                {
                    super.onAvailable(network);

                    LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                    List<RouteInfo> routes = linkProperties.getRoutes();
                    for (RouteInfo route : routes) {
                        if (route.getGateway() != null)
                        {
                            InetAddress gatewayAddress = route.getGateway();

                            byte[] addressBytes = gatewayAddress.getAddress();
                            ByteBuffer addressBuffer = ByteBuffer.allocate(addressBytes.length);
                            addressBuffer.order(ByteOrder.LITTLE_ENDIAN);
                            addressBuffer.put(addressBytes);
                            addressBuffer.position(0);
                            int addressInt = addressBuffer.getInt();

                            if (addressInt != 0 && gatewayAddress instanceof Inet4Address) {
                                Log.i(TAG, "Received gateway address=" + gatewayAddress);

                                actuallyMapPorts(addressInt);

                                break;
                            }
                        }
                    }

                    connectivityManager.unregisterNetworkCallback(this);
                }
            };
            
            connectivityManager.registerNetworkCallback(networkRequest, callback);
        }
    }

    @Override
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public void onDestroy()
    {
        // Stop the service using the startId, so that we don't stop
        // the service in the middle of handling another job
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }
}
