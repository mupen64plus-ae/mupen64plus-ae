package paulscode.android.mupen64plusae.netplay;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;

public class TcpServer {

    public static class PlayerData {
        public int mRegId;
        public int mPlugin;
        public boolean mRaw;
    }

    public static class CoreSettings {
        public int mCountPerOp;
        public int mDisableExtraMem;
        public int mSiDmADuration;
        public int mEmuMode;
        public int mNoCompiledJump;
    }

    HashMap<String, byte[]> mFiles = new HashMap<>();
    CoreSettings mSettings = new CoreSettings();
    String mGliden64Settings = "";
    HashMap<Integer, PlayerData> mReg = new HashMap<>();

    int mBufferTarget;

    ServerSocket mServerSocket;
    Thread mServerThread;
    boolean mRunning = true;

    UdpServer mUdpServer;

    ArrayList<TcpClientHandler> mClients = new ArrayList<>();

    TcpServer(int _buffer_target, UdpServer udpServer)
    {
        mBufferTarget = _buffer_target;
        mUdpServer = udpServer;
    }

    public void updateSettings(CoreSettings settings)
    {
        mSettings = settings;
    }

    public void updateGliden64Settings(String settings)
    {
        mGliden64Settings = settings;
    }

    public CoreSettings getSettings()
    {
        return mSettings;
    }

    public String getGliden64Settings()
    {
        return mGliden64Settings;
    }

    public byte[] getFile(String filename)
    {
        if (mFiles.containsKey(filename))
        {
            return mFiles.get(filename);
        }
        return null;
    }

    public void addFile(String filename, byte[] contents)
    {
        mFiles.put(filename, contents.clone());
    }

    public PlayerData getPlayerData(int player)
    {
        if (mReg.containsKey(player)) {
            return mReg.get(player);
        }

        return null;
    }

    public void addPlayerData(int player, PlayerData playerData)
    {
        mReg.put(player, playerData);

        mUdpServer.registerPlayer(playerData.mRegId, player, playerData.mPlugin);
    }

    void runTcpServer() {

        Log.e("TcpServer", "STARTED TCP SERVER!!!");

        while (mRunning) {
            try {
                Log.e("TcpServer", "WAITING FOR CLIENT");
                mClients.add(new TcpClientHandler(this, mBufferTarget, mServerSocket.accept()));
                Log.e("TcpServer", "GOT CLIENT");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void setPort(int port)
    {
        try {
            mServerSocket = new ServerSocket(port);
            mServerThread = new Thread(this::runTcpServer);
            mServerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getBufferTarget()
    {
        return mBufferTarget;
    }

    void stopServer() {
        try {
            mRunning = false;
            mServerSocket.close();
            mServerThread.join();
        } catch (IOException|InterruptedException e) {
            e.printStackTrace();
        }
    }

    void waitForServerToEnd() {
        try {
            mServerThread.join();
            Log.i("TcpServer", "Server thread finished");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
