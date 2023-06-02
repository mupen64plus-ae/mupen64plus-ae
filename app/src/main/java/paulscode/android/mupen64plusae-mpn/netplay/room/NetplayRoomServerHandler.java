package paulscode.android.mupen64plusae-mpn.netplay.room;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NetplayRoomServerHandler {

    interface OnServerRoomData {
        /**
         * Called when room data is provided by the server
         * @param netplayVersion Netplay server version
         * @param serverName Server name
         * @param romMd5 ROM MD5
         */
        void onServerRoomData(int netplayVersion, String serverName, String romMd5);

        /**
         * Called when server responds with registration information
         * @param regId Registration id
         * @param player Player number
         * @param videoPlugin Video plugin
         * @param rspPlugin RSP plugin
         * @param address Host address
         * @param port Host port
         */
        void onServerRegistration(int regId, int player, String videoPlugin, String rspPlugin, InetAddress address, int port);

        /**
         * Called when the server wants to start the game
         */
        void onServerStartGame();
    }

    static final String TAG = "ServerHandler";

    // Client device name
    private final String mDeviceName;

    // Callback for when server room data arrives
    private final OnServerRoomData mOnServerRoomData;

    // True if we registered to this room
    private boolean mRegisteredToRoom = false;

    // True if we are running
    private boolean mRunning = true;

    // Server address
    private final InetAddress mAddress;

    // Server port
    private final int mPort;

    // Client socket
    private Socket mClientSocket;

    // Output socket stream
    private OutputStream mSocketOutputStream;

    // Output socket synchronization object
    private final Object mSocketOutputSync = new Object();

    // Input socket stream
    private InputStream mSocketInputStream;

    // Send and receive buffers
    private final ByteBuffer mSendBuffer = ByteBuffer.allocate(100);
    private final ByteBuffer mReceiveBuffer = ByteBuffer.allocate(300);

    public NetplayRoomServerHandler(String deviceName, InetAddress address, int port, OnServerRoomData onServerRoomData)
    {
        mDeviceName = deviceName;
        mAddress = address;
        mPort = port;
        mOnServerRoomData = onServerRoomData;
    }

    public void connectAsync()
    {
        // Must run on a separate thread, running network operation on the main
        // thread leads to NetworkOnMainThreadException exceptions
        Thread connectThread = new Thread(this::connect);
        connectThread.setDaemon(true);
        connectThread.start();
    }

    private void connect()
    {
        mSendBuffer.order(ByteOrder.BIG_ENDIAN);
        mSendBuffer.mark();

        mReceiveBuffer.order(ByteOrder.BIG_ENDIAN);
        mReceiveBuffer.mark();

        try {
            mClientSocket = new Socket(mAddress, mPort);
            mSocketOutputStream = mClientSocket.getOutputStream();
            mSocketInputStream = mClientSocket.getInputStream();
            mRunning = true;

            // Thread used to list for messages
            Thread clientThread = new Thread(this::runTcpClient);
            clientThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

        getRoomData();
    }

    @Override
    public boolean equals(@Nullable Object obj) {

        if (obj instanceof NetplayRoomServerHandler) {
            NetplayRoomServerHandler handler = (NetplayRoomServerHandler) obj;
            return handler.mAddress.equals(mAddress) && handler.mPort == mPort;
        }

        return false;
    }

    private String getStringFromBuffer(int maxSize)
    {
        byte[] stringBytes = new byte[maxSize];

        mReceiveBuffer.get(stringBytes);
        int stringEndIndex = 0;
        while (stringEndIndex < stringBytes.length && stringBytes[stringEndIndex] != 0) {
            ++stringEndIndex;
        }

        return new String(stringBytes, 0, stringEndIndex, StandardCharsets.ISO_8859_1);
    }

    private void handleRoomData() throws IOException
    {
        int offset = 0;
        mReceiveBuffer.reset();
        int bytesRead = 0;
        while (offset < NetplayRoomClientHandler.SIZE_SEND_ROOM_DATA && bytesRead != -1) {
            bytesRead = mSocketInputStream.read(mReceiveBuffer.array(), offset,
                    NetplayRoomClientHandler.SIZE_SEND_ROOM_DATA - offset);
            offset += bytesRead != -1 ? bytesRead : 0;
        }

        int netplayVersion = mReceiveBuffer.getInt();
        String serverDeviceName = getStringFromBuffer(NetplayRoomClientHandler.DEVICE_NAME_MAX);
        String serverRomMd5 = getStringFromBuffer(NetplayRoomClientHandler.ROM_MD5_MAX);

        Log.i(TAG, "Netplay version=" + netplayVersion + " Device name=" + serverDeviceName + " md5=" + serverRomMd5);

        mOnServerRoomData.onServerRoomData(netplayVersion, serverDeviceName, serverRomMd5);
    }

    private void handlerRegistrationData() throws IOException
    {
        Log.i(TAG, "Got registration data");

        int offset = 0;
        mReceiveBuffer.reset();
        int bytesRead = 0;
        while (offset < NetplayRoomClientHandler.SIZE_SEND_REGISTRATION_DATA && bytesRead != -1) {
            bytesRead = mSocketInputStream.read(mReceiveBuffer.array(), offset,
                    NetplayRoomClientHandler.SIZE_SEND_REGISTRATION_DATA - offset);
            offset += bytesRead != -1 ? bytesRead : 0;
        }

        int regId = mReceiveBuffer.getInt();
        int playerNumber = mReceiveBuffer.getInt();
        int serverPort = mReceiveBuffer.getInt();
        String videoPlugin = getStringFromBuffer(NetplayRoomClientHandler.VIDEO_PLUGIN_MAX);
        String rspPlugin = getStringFromBuffer(NetplayRoomClientHandler.RSP_PLUGIN_MAX);

        Log.i(TAG, "Registration id =" + regId + " player =" + playerNumber +
                " port=" + serverPort + " video plugin = " + videoPlugin + " RSP plugin =" + rspPlugin);

        if (serverPort != 0) {
            mOnServerRoomData.onServerRegistration(regId, playerNumber, videoPlugin, rspPlugin,
                    mClientSocket.getInetAddress(), serverPort);
        }
    }

    private void handleStart()
    {
        if (mRegisteredToRoom) {
            mOnServerRoomData.onServerStartGame();
        }
    }

    synchronized private void getRoomData()
    {
        if (mSocketOutputStream == null) {
            return;
        }

        Log.i(TAG, "Requesting room data");

        synchronized (mSocketOutputSync) {
            try {
                mSendBuffer.reset();
                mSendBuffer.putInt(NetplayRoomClientHandler.ID_GET_ROOM_DATA);
                mSocketOutputStream.write(mSendBuffer.array(), 0, mSendBuffer.position());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void registerToRoomAsync()
    {
        // Must run on a separate thread, running network operation on the main
        // thread leads to NetworkOnMainThreadException exceptions
        Thread registerThread = new Thread(this::registerToRoom);
        registerThread.setDaemon(true);
        registerThread.start();
    }

    private void registerToRoom()
    {
        if (mSocketOutputStream == null) {
            return;
        }

        Log.i(TAG, "Requesting room registration");

        synchronized (mSocketOutputSync) {

            try {
                mRegisteredToRoom = true;
                mSendBuffer.reset();
                mSendBuffer.putInt(NetplayRoomClientHandler.ID_REGISTER_TO_ROOM);

                // Device name, 30 bytes
                byte[] deviceNameBytes = mDeviceName.getBytes(StandardCharsets.ISO_8859_1);
                byte[] sendDeviceNameBytes = new byte[NetplayRoomClientHandler.DEVICE_NAME_MAX];
                Arrays.fill(sendDeviceNameBytes, (byte) 0);
                System.arraycopy(deviceNameBytes, 0, sendDeviceNameBytes, 0, Math.min(deviceNameBytes.length, sendDeviceNameBytes.length));
                sendDeviceNameBytes[sendDeviceNameBytes.length - 1] = 0;
                mSendBuffer.put(sendDeviceNameBytes, 0, sendDeviceNameBytes.length);

                mSocketOutputStream.write(mSendBuffer.array(), 0, mSendBuffer.position());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void leaveRoomAsync()
    {
        // Must run on a separate thread, running network operation on the main
        // thread leads to NetworkOnMainThreadException exceptions
        Thread registerThread = new Thread(this::leaveRoom);
        registerThread.setDaemon(true);
        registerThread.start();
    }

    private void leaveRoom()
    {
        if (mSocketOutputStream == null) {
            return;
        }

        Log.i(TAG, "Leaving room");

        synchronized (mSocketOutputSync) {

            try {
                mRegisteredToRoom = true;
                mSendBuffer.reset();
                mSendBuffer.putInt(NetplayRoomClientHandler.ID_LEAVE_ROOM);
                mSocketOutputStream.write(mSendBuffer.array(), 0, mSendBuffer.position());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        disconnect();
    }

    private void runTcpClient()
    {
        Log.i(TAG, "Started Room TCP client");

        while (mRunning)
        {
            // First read the whole message
            try {
                int offset = 0;
                mReceiveBuffer.reset();
                int bytesRead = 0;
                while (offset < NetplayRoomClientHandler.ID_SIZE && bytesRead != -1) {
                    bytesRead = mSocketInputStream.read(mReceiveBuffer.array(), offset,
                            NetplayRoomClientHandler.ID_SIZE - offset);
                    offset += bytesRead != -1 ? bytesRead : 0;
                }

                int id = mReceiveBuffer.getInt();

                Log.i(TAG, "Got message with id=" + id);

                if (bytesRead == -1) {
                    mRunning = false;
                } else {
                    if (id == NetplayRoomClientHandler.ID_SEND_ROOM_DATA) {
                        handleRoomData();
                    }
                    else if (id == NetplayRoomClientHandler.ID_SEND_REGISTRATION_DATA) {
                        handlerRegistrationData();
                    }
                    else if (id == NetplayRoomClientHandler.ID_SEND_START_PLAY) {
                        handleStart();
                    }
                    else {
                        mRunning = false;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                mRunning = false;
                break;
            }
        }
    }

    public void disconnect()
    {
        mRunning = false;

        if (mClientSocket != null) {
            try {
                mClientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
