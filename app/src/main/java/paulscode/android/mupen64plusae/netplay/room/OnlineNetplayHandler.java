package paulscode.android.mupen64plusae.netplay.room;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class OnlineNetplayHandler {


    static final int INIT_SESSION = 0;
    static final int REGISTER_NP_SERVER = 1;
    static final int NP_SERVER_GAME_STARTED = 2;
    static final int NP_CLIENT_REQUEST_REGISTRATION = 3;
    static final int INIT_SESSION_RESPONSE = 100;
    static final int NP_CLIENT_REQUEST_REGISTRATION_RESPONSE = 103;

    static final int INIT_SESSION_RESPONSE_SIZE = 4;
    static final int NP_CLIENT_REQUEST_REGISTRATION_RESPONSE_SIZE = 50;

    static final int ONLINE_NETPLAY_VERSION = 2;

    interface OnOnlineNetplayData {
        /**
         * Called when init response is provided by the server
         * @param success True if initialization was successful
         */
        void onInitSessionResponse(boolean success);

        /**
         * Called when the server responds to a registration data request
         * @param address Address of server
         * @param port Port of server or -1 if server was not found
         */
        void onRoomData(InetAddress address, int port);
    }

    static final String TAG = "OnlineNetplayHandler";

    // Callback for when online netplay data arrives
    private final OnOnlineNetplayData mOnOnlineNetplayData;

    // Online netplay server address
    InetAddress mOnlineNetplayServerAddress;

    // Online netplay server port
    int mOnlineNetplayServerPort;

    // Local room server port used for netplay
    int mLocalServerRoomPort;

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

    // True if we are running
    boolean mRunning = false;

    // Room ID assigned by the netplay server
    long mRoomId;

    /**
     * Constructor
     * @param address Online netplay server handler address
     * @param port Online netplay server handler port
     * @param localServerRoomPort Local port if we are a server
     * @param roomId Room ID if we are a client
     * @param onOnlineNetplayData Handler of responses
     */
    public OnlineNetplayHandler(InetAddress address, int port, int localServerRoomPort, long roomId,
                                OnOnlineNetplayData onOnlineNetplayData)
    {
        mOnOnlineNetplayData = onOnlineNetplayData;
        mOnlineNetplayServerAddress = address;
        mOnlineNetplayServerPort = port;
        mLocalServerRoomPort = localServerRoomPort;
        mRoomId = roomId;
    }

    public void connectAndGetDataFromCode()
    {
        connect();
        initSession();
        requestRegistrationData();
    }

    public void connectAndRequestCode()
    {
        connect();
        initSession();
        registerNetplayServer();
    }

    private void connect()
    {
        mSendBuffer.order(ByteOrder.BIG_ENDIAN);
        mSendBuffer.mark();

        mReceiveBuffer.order(ByteOrder.BIG_ENDIAN);
        mReceiveBuffer.mark();

        try {
            mClientSocket = new Socket(mOnlineNetplayServerAddress, mOnlineNetplayServerPort);
            mSocketOutputStream = mClientSocket.getOutputStream();
            mSocketInputStream = mClientSocket.getInputStream();
            mRunning = true;

            // Thread used to list for messages
            Thread clientThread = new Thread(this::runTcpClient);
            clientThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initSession()
    {
        if (mSocketOutputStream == null) {
            return;
        }

        synchronized (mSocketOutputSync) {

            try {
                mSendBuffer.reset();
                mSendBuffer.putInt(INIT_SESSION);
                mSendBuffer.putInt(ONLINE_NETPLAY_VERSION);
                mSocketOutputStream.write(mSendBuffer.array(), 0, mSendBuffer.position());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void registerNetplayServer()
    {
        Log.i(TAG, "Registering netplay server");

        synchronized (mSocketOutputSync) {
            if (mSocketOutputStream != null) {
                try {
                    mSendBuffer.reset();
                    mSendBuffer.putInt(REGISTER_NP_SERVER);
                    mSendBuffer.putInt(mLocalServerRoomPort);
                    mSocketOutputStream.write(mSendBuffer.array(), 0, mSendBuffer.position());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void requestRegistrationData()
    {
        Log.i(TAG, "Requesting registration data");

        synchronized (mSocketOutputSync) {

            try {
                mSendBuffer.reset();
                mSendBuffer.putInt(NP_CLIENT_REQUEST_REGISTRATION);
                mSendBuffer.putInt(Long.valueOf(mRoomId).intValue());

                if (mSocketOutputStream != null) {
                    mSocketOutputStream.write(mSendBuffer.array(), 0, mSendBuffer.position());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void notifyGameStartedAsync()
    {
        // Must run on a separate thread, running network operation on the main
        // thread leads to NetworkOnMainThreadException exceptions
        Thread registerThread = new Thread(this::notifyGameStarted);
        registerThread.setDaemon(true);
        registerThread.start();
    }

    private void notifyGameStarted()
    {
        Log.i(TAG, "Requesting registration data");

        synchronized (mSocketOutputSync) {

            try {
                mSendBuffer.reset();
                mSendBuffer.putInt(NP_SERVER_GAME_STARTED);

                if (mSocketOutputStream != null) {
                    mSocketOutputStream.write(mSendBuffer.array(), 0, mSendBuffer.position());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        disconnect();
    }

    @SuppressWarnings("SameParameterValue")
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

    private void handleInitSessionResponse() throws IOException
    {
        int offset = 0;
        mReceiveBuffer.reset();
        int bytesRead = 0;
        while (offset < INIT_SESSION_RESPONSE_SIZE && bytesRead != -1) {
            bytesRead = mSocketInputStream.read(mReceiveBuffer.array(), offset,
                    NetplayRoomClientHandler.SIZE_SEND_ROOM_DATA - offset);
            offset += bytesRead != -1 ? bytesRead : 0;
        }

        boolean success = mReceiveBuffer.getInt() != 0;

        Log.e(TAG, "Init session response, success=" + success);

        mOnOnlineNetplayData.onInitSessionResponse(success);

        if (!success) {
            disconnect();
        }
    }

    private void handleRequestRegistrationDataResponse() throws IOException
    {
        Log.i(TAG, "handle registration data response");

        int offset = 0;
        mReceiveBuffer.reset();
        int bytesRead = 0;
        while (offset < NP_CLIENT_REQUEST_REGISTRATION_RESPONSE_SIZE && bytesRead != -1) {
            bytesRead = mSocketInputStream.read(mReceiveBuffer.array(), offset,
                    NetplayRoomClientHandler.SIZE_SEND_REGISTRATION_DATA - offset);
            offset += bytesRead != -1 ? bytesRead : 0;
        }

        final int numIpAddressChars = 46;
        String hostname = getStringFromBuffer(numIpAddressChars);
        int port = mReceiveBuffer.getInt();
        InetAddress address = InetAddress.getByName(hostname);
        mOnOnlineNetplayData.onRoomData(address, port);
    }

    private void runTcpClient()
    {
        Log.i(TAG, "Started online netplay TCP client");

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
                    if (id == INIT_SESSION_RESPONSE) {
                        handleInitSessionResponse();
                    }
                    else if (id == NP_CLIENT_REQUEST_REGISTRATION_RESPONSE) {
                        handleRequestRegistrationDataResponse();
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
