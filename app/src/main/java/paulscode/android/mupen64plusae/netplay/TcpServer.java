package paulscode.android.mupen64plusae.netplay;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("SameParameterValue")
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
    boolean mCoreSettingsSet = false;
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

    synchronized public void updateSettings(CoreSettings settings)
    {
        mSettings = settings;
        mCoreSettingsSet = true;
    }

    public void updateGliden64Settings(String settings)
    {
        mGliden64Settings = settings;
    }

    synchronized public CoreSettings getSettings()
    {
        if (!mCoreSettingsSet)
            return null;
        return mSettings;
    }

    public String getGliden64Settings()
    {
        return mGliden64Settings;
    }

    synchronized public byte[] getFile(String filename)
    {
        if (mFiles.containsKey(filename))
        {
            return mFiles.get(filename);
        }
        return null;
    }

    synchronized public void addFile(String filename, byte[] contents)
    {
        mFiles.put(filename, contents.clone());
    }

    synchronized public PlayerData getPlayerData(int player)
    {
        if (mReg.containsKey(player)) {
            return mReg.get(player);
        }

        return null;
    }

    synchronized public void addPlayerData(int player, PlayerData playerData)
    {
        mReg.put(player, playerData);

        mUdpServer.registerPlayer(playerData.mRegId, player, playerData.mPlugin);
    }

    void runTcpServer() {

        while (mRunning) {
            try {
                mClients.add(new TcpClientHandler(this, mBufferTarget, mServerSocket.accept()));
            } catch (IOException e) {
                e.printStackTrace();
                mRunning = false;
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

    int getPort()
    {
        return mServerSocket.getLocalPort();
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
