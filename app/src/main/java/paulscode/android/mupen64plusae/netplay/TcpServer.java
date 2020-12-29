package paulscode.android.mupen64plusae.netplay;

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

    HashMap<String, byte[]> mFiles;
    CoreSettings mSettings = new CoreSettings();
    String mGliden64Settings = "";
    HashMap<Integer, PlayerData> mReg = new HashMap<>();

    int mClientNumber = 0;
    int mBufferTarget;

    ServerSocket mServerSocket;
    Thread mServerThread;
    boolean mRunning = true;
    ArrayList<TcpClientHandler> mClients = new ArrayList<>();

    TcpServer(int _buffer_target)
    {
        mBufferTarget = _buffer_target;
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
    }

    public boolean newRegistration() {
        return mReg.size() == mClientNumber;
    }

    void runTcpServer() {
        while (mRunning) {
            try {
                mClients.add(new TcpClientHandler(this, mBufferTarget, mServerSocket.accept()));
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

    void reg_player(int reg_id, int playerNum, int plugin)
    {

    }
    void playerDisconnect(int reg_id)
    {

    }

    void onNewConnection()
    {

    }

    void setClientNumber(int size)
    {
        mClientNumber = size;
    }

    void register_player(int reg_id, int playerNum, int plugin)
    {

    }

    void disconnect_player(int reg_id)
    {

    }

    void stopServer() {
        try {
            mRunning = false;
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
