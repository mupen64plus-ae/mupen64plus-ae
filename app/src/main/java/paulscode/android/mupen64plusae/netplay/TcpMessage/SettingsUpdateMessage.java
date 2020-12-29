package paulscode.android.mupen64plusae.netplay.TcpMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import paulscode.android.mupen64plusae.netplay.TcpServer;

public class SettingsUpdateMessage implements TcpMessage {

    private static final int MESSAGE_SIZE = 21;
    TcpServer mTcpServer;
    ByteBuffer mReceiveBuffer = ByteBuffer.allocate(MESSAGE_SIZE);

    TcpServer.CoreSettings mSettings = new TcpServer.CoreSettings();

    public SettingsUpdateMessage(TcpServer tcpServer) {
        mTcpServer = tcpServer;
    }

    @Override
    public void parse(InputStream stream) throws IOException {

        int offset = 0;
        mReceiveBuffer.reset();
        while (offset < MESSAGE_SIZE) {
            int bytesRead = stream.read(mReceiveBuffer.array(), offset, MESSAGE_SIZE - offset);
            offset += bytesRead != -1 ? bytesRead : 0;
        }
        mSettings.mCountPerOp = mReceiveBuffer.getInt();
        mSettings.mDisableExtraMem = mReceiveBuffer.getInt();
        mSettings.mSiDmADuration = mReceiveBuffer.getInt();
        mSettings.mEmuMode = mReceiveBuffer.getInt();
        mSettings.mNoCompiledJump = mReceiveBuffer.getInt();
    }

    @Override
    public void process() {
        mTcpServer.updateSettings(mSettings);
    }
}
