package paulscode.android.mupen64plusae.netplay.TcpMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import paulscode.android.mupen64plusae.netplay.TcpServer;

public class SaveFileDataMessage implements TcpMessage {

    TcpServer mTcpServer;
    ByteBuffer mReceiveBuffer = ByteBuffer.allocate(1024*512);

    String mFileName = "";
    int mSizeOfFile;
    byte[] mFileData = null;

    public SaveFileDataMessage(TcpServer tcpServer) {
        mTcpServer = tcpServer;
        mReceiveBuffer.order(ByteOrder.BIG_ENDIAN);
        mReceiveBuffer.mark();
    }

    @Override
    public void parse(InputStream stream) throws IOException {

        // Variable size message, ready until 0 is encountered.
        int data = -1;
        int offset = 0;
        mReceiveBuffer.reset();

        while (data != 0) {
            data = stream.read();

            if (data != -1) {
                mReceiveBuffer.put((byte)data);
            }
        }

        mFileName = new String(mReceiveBuffer.array(), 0, mReceiveBuffer.position() - 1);

        // Read how many bytes of data are present
        int fileSizeBytes = 4;
        mReceiveBuffer.reset();

        int bytesRead = 0;
        while (offset < fileSizeBytes && bytesRead != -1) {
            bytesRead = stream.read(mReceiveBuffer.array(), offset, fileSizeBytes - offset);
            offset += bytesRead != -1 ? bytesRead : 0;
        }

        // Read remaining size
        mSizeOfFile = mReceiveBuffer.getInt();

        mReceiveBuffer.reset();

        offset = 0;
        while (offset < mSizeOfFile && bytesRead != -1) {
            bytesRead = stream.read(mReceiveBuffer.array(), offset, mSizeOfFile - offset);
            offset += bytesRead != -1 ? bytesRead : 0;

        }

        mFileData = new byte[mSizeOfFile];
        mReceiveBuffer.get(mFileData, 0, mSizeOfFile);
    }

    @Override
    public void process() {
        mTcpServer.addFile(mFileName, mFileData);
    }
}
