package paulscode.android.mupen64plusae.netplay.room;

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
         * @param serverName Server name
         * @param romMd5 ROM MD5
         */
        void onServerRoomData(String serverName, String romMd5);

        /**
         * Called when server responds with registration information
         * @param regId Registration id
         * @param player Player number
         * @param address Host address
         * @param port Host port
         */
        void onServerRegistration(int regId, int player, InetAddress address, int port);

        /**
         * Called when the server wants to start the game
         */
        void onServerStartGame();
    }

    static final String TAG = "ServerHandler";

    // Client device name
    String mDeviceName;

    // Callback for when server room data arrives
    OnServerRoomData mOnServerRoomData;

    // True if we registered to this room
    boolean mRegisteredToRoom = false;

    // True if we are running
    boolean mRunning = true;

    // Server address
    InetAddress mAddress;

    // Server port
    int mPort;

    // Thread used to list for messages
    Thread mClientThread;

    // Client socket
    Socket mClientSocket;

    // Output socket stream
    OutputStream mSocketOutputStream;

    // Input socket stream
    InputStream mSocketInputStream;

    // Send and receive buffers
    ByteBuffer mSendBuffer = ByteBuffer.allocate(100);
    ByteBuffer mReceiveBuffer = ByteBuffer.allocate(100);

    public NetplayRoomServerHandler(String deviceName, InetAddress address, int port, OnServerRoomData onServerRoomData)
    {
        mDeviceName = deviceName;
        mAddress = address;
        mPort = port;
        mOnServerRoomData = onServerRoomData;
    }

    public void connect()
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

            mClientThread = new Thread(this::runTcpClient);
            mClientThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {

        if (obj instanceof NetplayRoomServerHandler) {
            NetplayRoomServerHandler handler = (NetplayRoomServerHandler) obj;
            return handler.mAddress.equals(mAddress) && handler.mPort == mPort;
        }

        return false;
    }

    private void handleRoomData() throws IOException
    {
        int offset = 0;
        mReceiveBuffer.reset();
        while (offset < NetplayRoomClientHandler.SIZE_SEND_ROOM_DATA) {
            int bytesRead = mSocketInputStream.read(mReceiveBuffer.array(), offset,
                    NetplayRoomClientHandler.SIZE_SEND_ROOM_DATA - offset);
            offset += bytesRead != -1 ? bytesRead : 0;
        }

        byte[] deviceName = new byte[NetplayRoomClientHandler.DEVICE_NAME_MAX];
        byte[] romMd5 = new byte[NetplayRoomClientHandler.ROM_MD5_MAX];

        mReceiveBuffer.get(deviceName);
        int deviceNameEnd = 0;
        while (deviceNameEnd < deviceName.length && deviceName[deviceNameEnd] != 0) {
            ++deviceNameEnd;
        }

        mReceiveBuffer.get(romMd5);
        int romMd5End = 0;
        while (romMd5End < romMd5.length && romMd5[romMd5End] != 0) {
            ++romMd5End;
        }

        String serverDeviceName = new String(deviceName, 0, deviceNameEnd, StandardCharsets.ISO_8859_1);
        String serverRomMd5 = new String(romMd5, 0, romMd5End, StandardCharsets.ISO_8859_1);

        Log.i(TAG, "Device name=" + serverDeviceName + " md5=" + serverRomMd5);

        mOnServerRoomData.onServerRoomData(serverDeviceName, serverRomMd5);
    }

    private void handlerRegistrationData() throws IOException
    {
        Log.i(TAG, "Got registration data");

        int offset = 0;
        mReceiveBuffer.reset();
        while (offset < NetplayRoomClientHandler.SIZE_SEND_REGISTRATION_DATA) {
            int bytesRead = mSocketInputStream.read(mReceiveBuffer.array(), offset,
                    NetplayRoomClientHandler.SIZE_SEND_REGISTRATION_DATA - offset);
            offset += bytesRead != -1 ? bytesRead : 0;
        }

        int regId = mReceiveBuffer.getInt();
        int playerNumber = mReceiveBuffer.getInt();
        int serverPort = mReceiveBuffer.getInt();

        Log.i(TAG, "Registration id =" + regId + " player #=" + playerNumber + " port=" + serverPort);

        mOnServerRoomData.onServerRegistration(regId, playerNumber, mClientSocket.getInetAddress(), serverPort);
    }

    private void handleStart() throws IOException
    {
        if (mRegisteredToRoom) {
            mOnServerRoomData.onServerStartGame();
        }
    }

    synchronized public void getRoomData()
    {
        Log.i(TAG, "Requesting room data");

        if (mSocketOutputStream != null) {
            try {
                mSendBuffer.reset();
                mSendBuffer.putInt(NetplayRoomClientHandler.ID_GET_ROOM_DATA);
                mSocketOutputStream.write(mSendBuffer.array(), 0, mSendBuffer.position());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void registerToRoomAsync() {
        // Must run on a separate thread, running network operation on the main
        // thread leads to NetworkOnMainThreadException exceptions
        Thread registerThread = new Thread(this::registerToRoom);
        registerThread.setDaemon(true);
        registerThread.start();
    }

    synchronized private void registerToRoom()
    {
        Log.i(TAG, "Requesting room registration");

        try {
            mRegisteredToRoom = true;
            mSendBuffer.reset();
            mSendBuffer.putInt(NetplayRoomClientHandler.ID_REGISTER_TO_ROOM);

            // Device name, 30 bytes
            byte[] deviceNameBytes = mDeviceName.getBytes(StandardCharsets.ISO_8859_1);
            byte[] sendDeviceNameBytes = new byte[NetplayRoomClientHandler.DEVICE_NAME_MAX];
            Arrays.fill(sendDeviceNameBytes, (byte)0);
            System.arraycopy(deviceNameBytes, 0, sendDeviceNameBytes, 0, Math.min(deviceNameBytes.length, sendDeviceNameBytes.length));
            sendDeviceNameBytes[sendDeviceNameBytes.length-1] = 0;
            mSendBuffer.put(sendDeviceNameBytes, 0, sendDeviceNameBytes.length);

            mSocketOutputStream.write(mSendBuffer.array(), 0, mSendBuffer.position());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                while (offset < NetplayRoomClientHandler.ID_SIZE) {
                    int bytesRead = mSocketInputStream.read(mReceiveBuffer.array(), offset,
                            NetplayRoomClientHandler.ID_SIZE - offset);
                    offset += bytesRead != -1 ? bytesRead : 0;
                }

                int id = mReceiveBuffer.getInt();

                Log.i(TAG, "Got message with id=" + id);

                if (id == -1) {
                    mRunning = false;
                }

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
            } catch (IOException e) {
                e.printStackTrace();
                mRunning = false;
                break;
            }
        }
    }
}
