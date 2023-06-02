package paulscode.android.mupen64plusae_mpn.netplay.TcpMessage;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import paulscode.android.mupen64plusae_mpn.netplay.TcpServer;

public class RequestPlayerRegistrationMessage implements TcpMessage {

    static final int NUM_PLAYERS = 4;

    TcpServer mTcpServer;
    ByteBuffer mSendBuffer = ByteBuffer.allocate(24);
    OutputStream mOutputStream;

    public RequestPlayerRegistrationMessage(TcpServer tcpServer, OutputStream outputStream) {
        mTcpServer = tcpServer;
        mOutputStream = outputStream;
        mSendBuffer.order(ByteOrder.BIG_ENDIAN);
        mSendBuffer.mark();
    }

    @Override
    public void parse(InputStream stream) throws IOException {
        // Nothing to parse, it's a header only message
    }

    @Override
    public void process() {

        mSendBuffer.reset();

        for (int playerIndex = 0; playerIndex < NUM_PLAYERS; ++playerIndex)
        {
            TcpServer.PlayerData playerData = mTcpServer.getPlayerData(playerIndex);

            if (playerData != null)
            {
                mSendBuffer.putInt(playerData.mRegId);
                mSendBuffer.put((byte)playerData.mPlugin);
                mSendBuffer.put((byte)(playerData.mRaw ? 1 : 0));
            }
            else
            {
                mSendBuffer.putInt(0);
                mSendBuffer.put((byte)0);
                mSendBuffer.put((byte)0);
            }
        }
        try {
            mOutputStream.write(mSendBuffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
