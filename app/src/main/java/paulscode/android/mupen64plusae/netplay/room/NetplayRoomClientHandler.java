package paulscode.android.mupen64plusae.netplay.room;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class NetplayRoomClientHandler
{
    public interface OnClientRegistered {

        /**
         * Called when a player registers
         * @param playerNumber Player number
         * @param deviceName Device name
         */
        void onClientRegistration(int playerNumber, String deviceName );

        /**
         * Called when a client leaves
         * @param playerNumber Player number
         */
        void onClientLeave(int playerNumber);
    }

    static final String TAG = "ClientHandler";

    static final int ID_GET_ROOM_DATA = 0;
    static final int ID_REGISTER_TO_ROOM = 1;
    static final int ID_LEAVE_ROOM = 2;
    static final int ID_SEND_ROOM_DATA = 3;
    static final int ID_SEND_REGISTRATION_DATA = 4;
    static final int ID_SEND_START_PLAY = 5;

    static final int SIZE_SEND_ROOM_DATA = 67;
    static final int SIZE_SEND_REGISTRATION_DATA = 132;
    static final int ID_SIZE = 4;

    static final int DEVICE_NAME_MAX = 30;
    static final int VIDEO_PLUGIN_MAX = 60;
    static final int RSP_PLUGIN_MAX = 60;
    static final int ROM_MD5_MAX = 33;

    static final int MAX_PLAYERS = 4;

    static final int NETPLAY_VERSION = 1;

    private final String mDeviceName;
    private final String mRomMd5;
    private final String mVideoPlugin;
    private final String mRspPlugin;
    private final int mRegId;
    private static int mPlayerNumber = 0;
    private final int mServerPort;

    private final OnClientRegistered mOnClientRegistered;

    private boolean mRunning = true;
    private final Object mSocketOutputSync = new Object();
    private OutputStream mSocketOutputStream;
    private InputStream mSocketInputStream;

    private final ByteBuffer mSendBuffer = ByteBuffer.allocate(300);
    private final ByteBuffer mReceiveBuffer = ByteBuffer.allocate(100);

    private int mCurrentPlayerNumber = -1;
    private boolean mClientRegistered = false;

    NetplayRoomClientHandler(String deviceName, String romMd5, String videoPlugin, String rspPlugin,
                             int regId, int serverPort, Socket socket, OnClientRegistered onClientRegistered)
    {
        mDeviceName = deviceName;
        mRomMd5 = romMd5;
        mVideoPlugin = videoPlugin;
        mRspPlugin = rspPlugin;
        mRegId = regId;
        mServerPort = serverPort;
        mOnClientRegistered = onClientRegistered;

        mSendBuffer.order(ByteOrder.BIG_ENDIAN);
        mSendBuffer.mark();
        mReceiveBuffer.order(ByteOrder.BIG_ENDIAN);
        mReceiveBuffer.mark();

        try {
            mSocketOutputStream = socket.getOutputStream();
            mSocketInputStream = socket.getInputStream();
            mRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread clientThread = new Thread(this::runClient);
        clientThread.start();
    }

    private void putString(String theString, int maxSize)
    {
        byte[] stringBytes = theString.getBytes(StandardCharsets.ISO_8859_1);
        byte[] sendStringBytes = new byte[maxSize];
        Arrays.fill(sendStringBytes, (byte)0);
        System.arraycopy(stringBytes, 0, sendStringBytes, 0, Math.min(stringBytes.length,
                sendStringBytes.length));
        sendStringBytes[sendStringBytes.length-1] = 0;
        mSendBuffer.put(sendStringBytes, 0, sendStringBytes.length);
    }

    private void handleGetRoomData() throws IOException
    {
        synchronized (mSocketOutputSync) {
            mSendBuffer.reset();
            // Message id
            mSendBuffer.putInt(ID_SEND_ROOM_DATA);

            // Netplay version
            mSendBuffer.putInt(NETPLAY_VERSION);

            // Device name, 30 bytes
            putString(mDeviceName, DEVICE_NAME_MAX);

            // Rom MD5, 33 bytes
            putString(mRomMd5, ROM_MD5_MAX);

            mSocketOutputStream.write(mSendBuffer.array(), 0, mSendBuffer.position());
        }
    }

    public synchronized static void resetPlayers()
    {
        // First player is reserved for server
        mPlayerNumber = 1;
    }

    private synchronized static int getNextPlayer()
    {
        ++mPlayerNumber;
        return mPlayerNumber;
    }

    private void handleRegisterToRoom() throws IOException
    {
        Log.i(TAG, "Requesting room registration");

        byte[] receiveDeviceNameBytes = new byte[DEVICE_NAME_MAX];

        int offset = 0;
        while (offset < DEVICE_NAME_MAX) {
            int bytesRead = mSocketInputStream.read(receiveDeviceNameBytes, offset, DEVICE_NAME_MAX - offset);
            offset += bytesRead != -1 ? bytesRead : 0;
        }

        int deviceNameEnd = 0;
        while (deviceNameEnd < receiveDeviceNameBytes.length && receiveDeviceNameBytes[deviceNameEnd] != 0) {
            ++deviceNameEnd;
        }

        String clientDeviceName = new String(receiveDeviceNameBytes, 0, deviceNameEnd, StandardCharsets.ISO_8859_1);

        synchronized (mSocketOutputSync) {

            // Player number
            mCurrentPlayerNumber = getNextPlayer();

            if (mCurrentPlayerNumber <= MAX_PLAYERS) {
                mClientRegistered = true;

                mSendBuffer.reset();
                // Message id
                mSendBuffer.putInt(ID_SEND_REGISTRATION_DATA);
                // Registration id
                mSendBuffer.putInt(mRegId);
                mSendBuffer.putInt(mCurrentPlayerNumber);
                // Server port
                mSendBuffer.putInt(mServerPort);

                // Video plugin, 60 bytes
                putString(mVideoPlugin, VIDEO_PLUGIN_MAX);

                // RSP plugin 60 bytes
                putString(mRspPlugin, RSP_PLUGIN_MAX);

                mOnClientRegistered.onClientRegistration(mCurrentPlayerNumber, clientDeviceName);
            } else {
                // Stop accepting registrations
                mSendBuffer.reset();
                // Message id
                mSendBuffer.putInt(ID_SEND_REGISTRATION_DATA);
                // Registration id
                mSendBuffer.putInt(0);
                mSendBuffer.putInt(0);
                // Server port
                mSendBuffer.putInt(0);
                // Video plugin, 60 bytes
                putString("dummy", VIDEO_PLUGIN_MAX);

                // RSP plugin 60 bytes
                putString("dummy", RSP_PLUGIN_MAX);
            }
            mSocketOutputStream.write(mSendBuffer.array(), 0, mSendBuffer.position());
        }
    }

    private synchronized static void removePlayer()
    {
        --mPlayerNumber;
    }

    private void handleLeaveRoom()
    {
        if (mClientRegistered) {
            mOnClientRegistered.onClientLeave(mCurrentPlayerNumber);
            mClientRegistered = false;
            removePlayer();
        }
    }

    public void sendStartAsync()
    {
        // Must run on a separate thread, running network operation on the main
        // thread leads to NetworkOnMainThreadException exceptions
        Thread registerThread = new Thread(this::sendStart);
        registerThread.setDaemon(true);
        registerThread.start();
    }

    private void sendStart()
    {
        synchronized (mSocketOutputSync) {
            mSendBuffer.reset();
            mSendBuffer.putInt(ID_SEND_START_PLAY);

            try {
                mSocketOutputStream.write(mSendBuffer.array(), 0, mSendBuffer.position());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void runClient()
    {
        while (mRunning)
        {
            // First read the whole message
            try {

                int offset = 0;
                mReceiveBuffer.reset();
                while (offset < ID_SIZE) {
                    int bytesRead = mSocketInputStream.read(mReceiveBuffer.array(), offset, ID_SIZE - offset);
                    offset += bytesRead != -1 ? bytesRead : 0;
                }

                int id = mReceiveBuffer.getInt();

                Log.i(TAG, "Got message with id=" + id);

                if (id == -1) {
                    mRunning = false;
                }

                if (id == ID_GET_ROOM_DATA) {
                    handleGetRoomData();
                }
                else if (id == ID_REGISTER_TO_ROOM) {
                    handleRegisterToRoom();
                }
                else if (id == ID_LEAVE_ROOM) {
                    handleLeaveRoom();
                } else {
                    mRunning = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                mRunning = false;
                break;
            }
        }


        Log.i(TAG, "Socket has been closed");
    }
}
