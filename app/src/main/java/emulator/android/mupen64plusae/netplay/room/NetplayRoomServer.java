package emulator.android.mupen64plusae.netplay.room;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import emulator.android.mupen64plusae.util.DeviceUtil;

import static android.content.Context.WIFI_SERVICE;

public class NetplayRoomServer {

    public interface OnClientFound {

        /**
         * Called when a player registers
         * @param playerNumber Player number
         * @param deviceName Device name
         */
        void onClientRegistration(int playerNumber, String deviceName );

        /**
         * Called when a player leaves
         * @param playerNumber Player number
         */
        void onClienLeave(int playerNumber);

        /**
         * Called when we a get a room code
         * @param roomCode Room code
         */
        void onRoomCode(long roomCode);
    }

    static final String TAG = "NetplayRoomServer";

    static final String DEFAULT_SERVICE_NAME = "M64PlusAE";
    static final String DEFAULT_SERVICE_TYPE = "_m64plusae._tcp.";

    // Port where the netplay server is listening
    private int mServerPort;

    // TCP server used to communicate game data
    private ServerSocket mServerSocket;

    // Broadcast service through NSD
    private NsdManager mNsdManager;

    // NSD registration listener
    private NsdManager.RegistrationListener mRegistrationListener;

    private final WifiManager.MulticastLock mMulticastLock;

    // Service name used for NSD
    private String mNsdServiceName;

    // True if we are running
    private boolean mRunning = true;

    // Device name
    private final String mDeviceName;

    // ROM MD5
    private final String mRomMd5;

    // Video plugin
    private final String mVideoPlugin;

    // RSP plugin
    private final String mRspPlugin;

    // Context for creating NSD service
    private final Context mContext;

    // Called when a client is found
    private final OnClientFound mOnClientFound;

    // List of clients
    private final ArrayList<NetplayRoomClientHandler> mClients = new ArrayList<>();

    // List of registration ids
    private final Set<Integer> mRegistrationIds = new HashSet<>();

    // Handler for registering for the NSD service repeatedly
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final Object mClientSyncObject = new Object();

    /**
     * Constructor
     * @param serverPort Port in which the netplay server is listening on
     */
    public NetplayRoomServer(Context context, String deviceName, String romMd5, String videoPlugin, String rspPlugin,
                             int serverPort, OnClientFound onClientFound)
    {
        mContext = context;
        mDeviceName = deviceName;
        mRomMd5 = romMd5;
        mVideoPlugin = videoPlugin;
        mRspPlugin = rspPlugin;
        mServerPort = serverPort;
        mOnClientFound = onClientFound;
        mNsdServiceName = DEFAULT_SERVICE_NAME;

        NetplayRoomClientHandler.resetPlayers();

        WifiManager wifi = (WifiManager) mContext.getApplicationContext().getSystemService(WIFI_SERVICE);
        mMulticastLock = wifi.createMulticastLock("multicastLock");
        mMulticastLock.setReferenceCounted(true);
        mMulticastLock.acquire();

        try {
            mServerSocket = new ServerSocket(0);

            // Thread used to listen for new connections
            Thread serverThread = new Thread(this::runTcpServer);
            serverThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Register/Unregister in a loop to workaround issues with NSD in Android
        mHandler.postDelayed(this::registerService, 500);
    }

    public int getServerPort()
    {
        return mServerSocket.getLocalPort();
    }

    public void registerService() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                Log.i(TAG, "NSD Service has been registered");
                // Save the service name. Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                mNsdServiceName = nsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "NSD registration failure, code=" + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                Log.i(TAG, "NSD Service has been unregisterd");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "NSD unregistration failure, code=" + errorCode);
            }
        };

        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(mNsdServiceName);
        serviceInfo.setServiceType(DEFAULT_SERVICE_TYPE);
        serviceInfo.setPort(mServerSocket.getLocalPort());
        serviceInfo.setHost(DeviceUtil.getIPAddress());
        serviceInfo.setAttribute("dummy", "dummy");

        Log.i(TAG, "NS registering: " + serviceInfo.toString());

        mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        mHandler.postDelayed(this::unregisterService, 2000);
    }

    private void unregisterService()
    {
        mNsdManager.unregisterService(mRegistrationListener);

        if (mRunning) {
            mHandler.postDelayed(this::registerService, 2000);
        }
    }

    private void runTcpServer() {

        Log.i(TAG, "Started Room TCP server");
        Random rand = new Random();

        while (mRunning) {
            try {
                // Get a valid registration ID that doesn't exist yet
                int regId = rand.nextInt();
                while (mRegistrationIds.contains(regId) && mRunning) {
                    regId = rand.nextInt();
                }

                mRegistrationIds.add(regId);

                Socket socketAccepted = mServerSocket.accept();

                synchronized (mClientSyncObject) {
                    mClients.add(new NetplayRoomClientHandler(mDeviceName, mRomMd5, mVideoPlugin, mRspPlugin,
                            regId, mServerPort, socketAccepted,
                            new NetplayRoomClientHandler.OnClientRegistered() {
                                @Override
                                public void onClientRegistration(int playerNumber, String deviceName) {
                                    mOnClientFound.onClientRegistration(playerNumber, deviceName);
                                }

                                @Override
                                public void onClientLeave(int playerNumber) {
                                    mOnClientFound.onClienLeave(playerNumber);
                                }

                                @Override
                                public void onRoomCode(long roomCode) {
                                    mOnClientFound.onRoomCode(roomCode);
                                }
                            }));
                }
            } catch (IOException e) {
                e.printStackTrace();
                mRunning = false;
            }
        }
    }

    public int registerPlayerOne()
    {
        Random rand = new Random();

        int regId = rand.nextInt();
        mRegistrationIds.add(regId);

        return regId;
    }

    public void start()
    {
        for (NetplayRoomClientHandler client : mClients) {
            client.sendStartAsync();
        }

        stopServer();
    }

    public void stopServer()
    {
        mRunning = false;
        try {
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMulticastLock.release();
    }

    public void updateServerPort(int serverPort)
    {
        mServerPort = serverPort;

        synchronized (mClientSyncObject) {
            for (NetplayRoomClientHandler client : mClients) {
                client.updateServerPort(serverPort);
            }
        }
    }
}
