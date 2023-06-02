package paulscode.android.mupen64plusae_mpn.netplay.TcpMessage;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import paulscode.android.mupen64plusae_mpn.netplay.TcpServer;

public class RequestSaveFileDataMessage implements TcpMessage {

    TcpServer mTcpServer;
    OutputStream mOutputStream;
    ByteBuffer mReceiveBuffer = ByteBuffer.allocate(1000);
    String mFileName = "";

    public RequestSaveFileDataMessage(TcpServer tcpServer, OutputStream outputStream) {
        mTcpServer = tcpServer;
        mOutputStream = outputStream;
        mReceiveBuffer.order(ByteOrder.BIG_ENDIAN);
        mReceiveBuffer.mark();
    }

    @Override
    public void parse(InputStream stream) throws IOException {

        mReceiveBuffer.reset();
        int data = -1;
        while (data != 0) {
            data = stream.read();

            if (data != -1) {
                mReceiveBuffer.put((byte)data);
            }
        }

        mFileName = new String(mReceiveBuffer.array(), 0, mReceiveBuffer.position() - 1);
    }

    @Override
    public void process() {
        byte[] fileContents = null;

        final int maxTries = 100;
        int currentTry = 0;

        while (fileContents == null && currentTry < maxTries) {

            fileContents = mTcpServer.getFile(mFileName);

            if (fileContents != null)
            {
                try {
                    Log.i("Netplay", "Sending " + fileContents.length + " bytes");
                    mOutputStream.write(fileContents);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            ++currentTry;
        }

        if (currentTry == maxTries) {
            Log.e("Netplay", "Unable to send file contents for " + mFileName);
        }
    }
}
