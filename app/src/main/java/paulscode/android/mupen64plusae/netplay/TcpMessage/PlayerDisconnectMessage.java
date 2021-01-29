package paulscode.android.mupen64plusae.netplay.TcpMessage;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import paulscode.android.mupen64plusae.netplay.TcpServer;

public class PlayerDisconnectMessage implements TcpMessage {

    private static final int MESSAGE_SIZE = 5 - 1;
    TcpServer mTcpServer;
    ByteBuffer mReceiveBuffer = ByteBuffer.allocate(MESSAGE_SIZE);

    int mPlayerRegistrationId = 0;

    public PlayerDisconnectMessage(TcpServer tcpServer) {
        mTcpServer = tcpServer;
        mReceiveBuffer.order(ByteOrder.BIG_ENDIAN);
        mReceiveBuffer.mark();
    }

    @Override
    public void parse(InputStream stream) throws IOException {

        int offset = 0;
        mReceiveBuffer.reset();
        while (offset < MESSAGE_SIZE) {
            int bytesRead = stream.read(mReceiveBuffer.array(), offset, MESSAGE_SIZE - offset);
            offset += bytesRead != -1 ? bytesRead : 0;
        }

        mPlayerRegistrationId = mReceiveBuffer.getInt();
    }

    @Override
    public void process() {
        Log.i("TcpServer", "Player disconnected: " + mPlayerRegistrationId);

        mTcpServer.removePlayer(mPlayerRegistrationId);
    }
}
