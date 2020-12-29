package paulscode.android.mupen64plusae.netplay.TcpMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import paulscode.android.mupen64plusae.netplay.TcpServer;

public class RequestSettingsMessage implements TcpMessage {

    TcpServer mTcpServer;
    ByteBuffer mOutboundByteBuffer = ByteBuffer.allocate(20);
    OutputStream mOutputStream;

    public RequestSettingsMessage(TcpServer tcpServer, OutputStream outputStream) {
        mTcpServer = tcpServer;
        mOutputStream = outputStream;
    }

    @Override
    public void parse(InputStream stream) throws IOException {
        // This is a header only message
    }

    @Override
    public void process() {
        TcpServer.CoreSettings settings = mTcpServer.getSettings();

        mOutboundByteBuffer.putInt(settings.mCountPerOp);
        mOutboundByteBuffer.putInt(settings.mDisableExtraMem);
        mOutboundByteBuffer.putInt(settings.mSiDmADuration);
        mOutboundByteBuffer.putInt(settings.mEmuMode);
        mOutboundByteBuffer.putInt(settings.mNoCompiledJump);

        try {
            mOutputStream.write(mOutboundByteBuffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
