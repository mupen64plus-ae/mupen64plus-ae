package paulscode.android.mupen64plusae.netplay.TcpMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import paulscode.android.mupen64plusae.netplay.TcpServer;

public class SaveFileDataMessage implements TcpMessage {

    TcpServer mTcpServer;
    ByteBuffer mReceiveBuffer = ByteBuffer.allocate(1024*5);

    String mFileName = "";
    int mSizeOfFile;
    byte[] mFileData = new byte[1024*1024];

    public SaveFileDataMessage(TcpServer tcpServer) {
        mTcpServer = tcpServer;
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

        mFileName = new String(mReceiveBuffer.array());

        // Read how many bytes of data are present
        int fileSizeBytes = 4;
        mReceiveBuffer.reset();

        while (offset < fileSizeBytes) {
            int bytesRead = stream.read(mReceiveBuffer.array(), offset, fileSizeBytes - offset);
            offset += bytesRead != -1 ? bytesRead : 0;
        }

        // Read remaining size
        mSizeOfFile = mReceiveBuffer.getInt();
        mReceiveBuffer.reset();

        offset = 0;
        while (offset < mSizeOfFile) {
            int bytesRead = stream.read(mReceiveBuffer.array(), offset, fileSizeBytes - offset);
            offset += bytesRead != -1 ? bytesRead : 0;
        }

        mReceiveBuffer.get(mFileData, 0, mSizeOfFile);
    }

    @Override
    public void process() {
        mTcpServer.addFile(mFileName, mFileData);
    }
}
