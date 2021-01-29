package paulscode.android.mupen64plusae.netplay.room;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayList;

public class NetplayRoomClient {

    static final String TAG = "NetplayRoomClient";

    public interface OnServerFound
    {
        /**
         * Called when a valid server is found
         * @param serverId Server ID
         * @param serverName Server name
         */
        void onValidServerFound(int serverId, String serverName);

        /**
         * Called when server responds with registration information
         * @param regId Registration id
         * @param player Player number
         * @param address Host address
         * @param port Host port
         */
        void onServerRegistration(int regId, int player, InetAddress address, int port);

        /**
         * Called when the server starts the game
         */
        void onServerStart();
    }

    // Broadcast service through NSD
    NsdManager mNsdManager;

    // NSD discovery listener
    NsdManager.DiscoveryListener mDiscoveryListener;

    WifiManager.MulticastLock mMulticastLock;

    // Device name
    String mDeviceName;

    // ROM MD5
    String mRomMd5;

    // Context for creating NSD service
    Context mContext;

    // List of found servers
    ArrayList<NetplayRoomServerHandler> mClients = new ArrayList<>();

    OnServerFound mOnServerData;


    class ResolveListener implements NsdManager.ResolveListener {

        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e(TAG, "Resolve failed: " + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "Resolve Succeeded. host=" + serviceInfo.getHost().getHostAddress() +
                    " port=" + serviceInfo.getPort());

            NetplayRoomServerHandler roomClient = new NetplayRoomServerHandler(mDeviceName, serviceInfo.getHost(), serviceInfo.getPort(),
                    new NetplayRoomServerHandler.OnServerRoomData() {
                        @Override
                        public void onServerRoomData(String serverName, String romMd5) {
                            if (romMd5.equals(mRomMd5)) {
                                mOnServerData.onValidServerFound(mClients.size() - 1, serverName);
                            }
                        }

                        @Override
                        public void onServerRegistration(int regId, int player, InetAddress address, int port) {
                            mOnServerData.onServerRegistration(regId, player, address, port);
                        }

                        @Override
                        public void onServerStartGame() {
                            mOnServerData.onServerStart();
                            stopListening();
                        }
                    });

            if (!mClients.contains(roomClient)) {
                roomClient.connect();
                roomClient.getRoomData();
                mClients.add(roomClient);
            }
        }
    };

    /**
     * Constructor
     */
    public NetplayRoomClient(Context context, String deviceName, String romMd5, OnServerFound onServerData)
    {
        mContext = context;
        mDeviceName = deviceName;
        mRomMd5 = romMd5;
        mOnServerData = onServerData;

        WifiManager wifi = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mMulticastLock = wifi.createMulticastLock("multicastLock");
        mMulticastLock.setReferenceCounted(true);
        mMulticastLock.acquire();

        initializeDiscoveryListener();

        mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
        mNsdManager.discoverServices(NetplayRoomServer.DEFAULT_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    private void initializeDiscoveryListener()
    {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            // Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.i(TAG, "NSD Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.i(TAG, "NSD Service discovery success: " + service);
                if (!service.getServiceType().equals(NetplayRoomServer.DEFAULT_SERVICE_TYPE)) {
                    Log.i(TAG, "NSD Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().contains(NetplayRoomServer.DEFAULT_SERVICE_NAME)){
                    Log.i(TAG, "NSD Found service: " + service.getServiceName());
                    mNsdManager.resolveService(service, new ResolveListener());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "NSD service lost: " + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "NSD Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "NSD Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "NSD Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void registerServer(int serverId)
    {
        mClients.get(serverId).registerToRoomAsync();
    }

    public void stopListening()
    {
        mMulticastLock.release();
    }

    public void leaveServer() {
        for (NetplayRoomServerHandler serverHandler : mClients) {
            serverHandler.leaveRoomAsync();
        }

        mMulticastLock.release();
    }
}
