package paulscode.android.mupen64plusae.netplay;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

public class UdpServer {

    static class KeepAlive {
        KeepAlive(int keepAlive, int playerNumber) {
            mKeepAlive = keepAlive;
            mPlayerNumber = playerNumber;
        }

        public int mKeepAlive;
        public int mPlayerNumber;
    }

    static class Buttons {
        Buttons (int keys, int plugin) {
            mKeys = keys;
            mPlugin = plugin;
        }
        public int mKeys;
        public int mPlugin;
    }

    static final int NUM_PLAYERS = 4;

    static final int KEY_INFO_MSG = 0;
    static final int REQUEST_DATA_MSG = 2;
    static final int CP0_DATA_MSG = 4;

    DatagramSocket mUdpSocket;
    Thread mUdpServerThread;
    boolean mRunning = true;
    ByteBuffer mSendBuffer = ByteBuffer.allocate( 512 );
    ByteBuffer mReceiveBuffer = ByteBuffer.allocate( 1024*512 );

    DatagramPacket[] mSendPackets = new DatagramPacket[NUM_PLAYERS];
    DatagramPacket mRequestInfoSendPacket;
    DatagramPacket mReceivePacket;

    // Array of states per count
    ArrayList<HashMap<Integer, Buttons>> mInputs = new ArrayList<>(Arrays.asList(
            new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>()));

    // Hash used to determine if we are in sync by using cp0
    HashMap<Integer, Integer> mSyncHash = new HashMap<>();

    // Temporary byte array used for hashing
    byte[] mHashData = new byte[128];

    //reg_id, <keepalive, playernumber>
    HashMap<Integer, KeepAlive> mPlayerKeepAlive = new HashMap<>();

    // Buttons
    ArrayList<LinkedList<Buttons>> mButtons = new ArrayList<>(Arrays.asList( new LinkedList<>(),
            new LinkedList<>(), new LinkedList<>(), new LinkedList<>()));

    int[] mLeadCount = new int[NUM_PLAYERS];
    int[] mBufferSize = new int[NUM_PLAYERS];
    int[] mBufferHealth = new int[NUM_PLAYERS];
    int[] mInputDelay = new int[NUM_PLAYERS];

    int mPort;
    int mStatus;

    int mBufferTarget;

    public UdpServer(int _buffer_target)
    {
        for (int playerIndex = 0; playerIndex < NUM_PLAYERS; ++playerIndex)
        {
            mLeadCount[playerIndex] = 0;
            mBufferSize[playerIndex] = 3;
            mBufferHealth[playerIndex] = -1;
            mInputDelay[playerIndex] = -1;
            mSendPackets[playerIndex] = null;
        }
        mStatus = 0;
        mBufferTarget = _buffer_target;

        mSendBuffer.order(ByteOrder.BIG_ENDIAN);
        mReceiveBuffer.order(ByteOrder.BIG_ENDIAN);

        mReceiveBuffer.mark();
        mSendBuffer.mark();

        mRequestInfoSendPacket = new DatagramPacket(mSendBuffer.array(), mSendBuffer.array().length);
    }

    void handleKeyInfoMessage()
    {
        int playerNum = mReceiveBuffer.get();
        mSendPackets[playerNum].setAddress(mReceivePacket.getAddress());
        mSendPackets[playerNum].setPort(mReceivePacket.getPort());

        int count = mReceiveBuffer.getInt();
        int keys = mReceiveBuffer.getInt();
        int plugin = mReceiveBuffer.get();

        if (mInputDelay[playerNum] >= 0) {
            insertInput(playerNum, count + mInputDelay[playerNum], keys, plugin);
        } else if (mButtons.get(playerNum).size() == 0) {
            mButtons.get(playerNum).add(new Buttons(keys, plugin));
        }

        for (int playerIndex = 0; playerIndex < NUM_PLAYERS; ++playerIndex) {
            if (mSendPackets[playerIndex] != null && mSendPackets[playerIndex].getPort() != -1) {
                sendInput(count, playerNum, mSendPackets[playerIndex], 1);
            }
        }
    }

    void handleRequestDataMessage()
    {
        int playerNum = mReceiveBuffer.get();
        int regi_id = mReceiveBuffer.getInt();
        mRequestInfoSendPacket.setAddress(mReceivePacket.getAddress());
        mRequestInfoSendPacket.setPort(mReceivePacket.getPort());

        if (mPlayerKeepAlive.containsKey(regi_id)) {
            KeepAlive playerKeepAlive = mPlayerKeepAlive.get(regi_id);

            if (playerKeepAlive != null) {
                playerKeepAlive.mKeepAlive = 0;
            }
        }

        int count = mReceiveBuffer.getInt();
        int spectator = mReceiveBuffer.get();

        if (count >= mLeadCount[playerNum] && spectator == 0) {
            mBufferHealth[playerNum] = mReceiveBuffer.get();
            mLeadCount[playerNum] = count;
        }

        sendInput(count, playerNum, mRequestInfoSendPacket, spectator);
    }

    void handleCp0Message()
    {
        if ((mStatus & 1) == 0) {

            int vi_count = mReceiveBuffer.getInt();
            mReceiveBuffer.get(mHashData, 0, mHashData.length);

            int hash = Arrays.hashCode(mHashData);
            Integer currentHash = mSyncHash.get(vi_count);

            if (currentHash == null) {
                if (mSyncHash.size() > 500)
                    mSyncHash.clear();

                mSyncHash.put(vi_count, hash);
            } else if (currentHash != hash) {
                mStatus |= 1;
                Log.w("UdpServer", "We have desynced!!!");
            }
        }
    }

    public void runUdpServer()
    {
        while (mRunning) {

            try {
                mReceiveBuffer.reset();
                mUdpSocket.receive(mReceivePacket);

                int messageId = mReceiveBuffer.get();

                //Log.e("UdpServer", "GOT UDP MESSAGE: " + messageId);

                if (messageId == KEY_INFO_MSG) {
                    handleKeyInfoMessage();
                } else if (messageId == REQUEST_DATA_MSG) {
                    handleRequestDataMessage();
                } else if (messageId == CP0_DATA_MSG) {
                    handleCp0Message();
                } else {
                    Log.w("UdpServer", "Received unknown message with id=" + messageId);
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int getPort()
    {
        return mPort;
    }

    public void setPort(int _port)
    {
        mPort = _port;

        try {
            mUdpSocket = new DatagramSocket(mPort);
            mReceivePacket = new DatagramPacket(mReceiveBuffer.array(), mReceiveBuffer.array().length);

            mUdpServerThread = new Thread(this::runUdpServer);
            mUdpServerThread.start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void setInputDelay(int playerNum, int inputDelay)
    {
        mInputDelay[playerNum] = inputDelay;
    }

    void checkConnctions()
    {
        for (int playerIndex = 0; playerIndex < NUM_PLAYERS; ++playerIndex)
        {
            if (mBufferHealth[playerIndex] != -1)
            {
                if (mBufferHealth[playerIndex] > mBufferTarget && mBufferSize[playerIndex] > 0)
                    --mBufferSize[playerIndex];
                else if (mBufferHealth[playerIndex] < mBufferTarget)
                    ++mBufferSize[playerIndex];
            }
        }

        int should_delete = 0;

        for (HashMap.Entry<Integer, KeepAlive> entry : mPlayerKeepAlive.entrySet()) {
            KeepAlive keepAlive = entry.getValue();
            ++keepAlive.mKeepAlive;

            if (keepAlive.mKeepAlive > 40) {
                should_delete = entry.getKey();
            }
        }

        if (should_delete != 0) {
            Log.i("UdpServer", "Disconnect player " + should_delete);
        }
    }

    public void registerPlayer(int reg_id, int playerNum, int plugin)
    {
        mPlayerKeepAlive.put(reg_id, new KeepAlive(0, playerNum));
        mInputs.get(playerNum).put(0, new Buttons(0, plugin));

        mSendPackets[playerNum] = new DatagramPacket(mSendBuffer.array(), mSendBuffer.array().length);
    }

    public void disconnectPlayer(int reg_id)
    {
        if (mPlayerKeepAlive.containsKey(reg_id)) {
            KeepAlive keepAliveData = mPlayerKeepAlive.get(reg_id);
            int playerNum = keepAliveData != null ? keepAliveData.mPlayerNumber : -1;

            Log.i("UdpServer", "Player " + (playerNum + 1) + " disconnected, port=" + mPort);

            mStatus |= (0x1 << (playerNum + 1));
            mPlayerKeepAlive.remove(reg_id);

            if (mPlayerKeepAlive.isEmpty()) {
                Log.i("UdpServer", "No players left!");
            }
        }
    }

    private void sendInput(int count, int playerNum, DatagramPacket destPacket, int spectator)
    {
        int count_lag = mLeadCount[playerNum] - count;

        mSendBuffer.reset();
        mSendBuffer.put((byte)1);// Key info from server
        mSendBuffer.put((byte)playerNum);
        mSendBuffer.put((byte)mStatus);
        mSendBuffer.put((byte)count_lag);
        mSendBuffer.put((byte)0);
        int start = count;
        int end = start + mBufferSize[playerNum];

        /*if (playerNum == 1)
             Log.e("UdpServer", "GOT NEW DATA, count=" + count + " spectator=" + spectator + " lag=" + count_lag
                + " end=" + end + " playerNum=" + playerNum + " containskey=" + mInputs.get(playerNum).containsKey(count)
                + " size=" + mInputs.get(playerNum).size());

         */

        while ( (mSendBuffer.position() < 500) && ( (spectator == 0 && count_lag == 0 && count < end) || mInputs.get(playerNum).containsKey(count) ) )
        {
            mSendBuffer.putInt(count);
            if (!checkIfExists(playerNum, count))
            {
                // we don't have an input for this frame yet
                end = count - 1;
                continue;
            }

            Buttons buttons = mInputs.get(playerNum).get(count);

            if (buttons != null) {
                mSendBuffer.putInt(buttons.mKeys);
                mSendBuffer.put((byte)buttons.mPlugin);
            } else {
                mSendBuffer.putInt(0);
                mSendBuffer.put((byte)0);
            }
            ++count;
        }

        int counts = count - start;

        //number of counts in packet
        mSendBuffer.put(4, (byte)counts);

        if (counts > 0) {
            try {
                destPacket.setData(mSendBuffer.array(), 0, mSendBuffer.position());
                mUdpSocket.send(destPacket);
               // if (playerNum == 1)
                //Log.e("UdpServer", "Sent data, player=" + playerNum + " counts=" + counts);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkIfExists(int playerNumber, int count)
    {
        boolean inputExists = mInputs.get(playerNumber).containsKey(count);

        mInputs.get(playerNumber).remove(count - 5000);

        if (mInputDelay[playerNumber] < 0 && !inputExists)
        {
            if (!mButtons.get(playerNumber).isEmpty()) {
                mInputs.get(playerNumber).put(count, mButtons.get(playerNumber).removeFirst());
            } else if (mInputs.get(playerNumber).containsKey(count-1)) {
                mInputs.get(playerNumber).put(count, mInputs.get(playerNumber).get(count - 1));
            } else {
                mInputs.get(playerNumber).put(count, new Buttons(0, 0/*Controller not present*/));
            }

            return true;
        } else {
            // When using input delay, we must wait for inputs
            return inputExists;
        }
    }

    private void insertInput(int playerNum, int count, int button, int plugin)
    {
        int previousCount = count - 1;

        mInputs.get(playerNum).put(count, new Buttons(button, plugin));

        /* The recursion here covers two situations:
         *
         * 1. The count < inputDelay, so we need to populate the first frames
         * 2. We lost a udp packet, or received them out of order.
         */
        if (previousCount == 0 || (previousCount > 0 && !mInputs.get(playerNum).containsKey(previousCount)))
            insertInput(playerNum, previousCount, button, plugin);
    }

    void stopServer() {
        try {
            mRunning = false;
            mUdpSocket.close();
            mUdpServerThread.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void waitForServerToEnd() {
        try {
            mUdpServerThread.join();
            Log.i("UdpServer", "Server thread finished");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
