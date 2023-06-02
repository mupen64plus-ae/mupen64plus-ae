package paulscode.android.mupen64plusae_mpn.netplay;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import paulscode.android.mupen64plusae_mpn.netplay.TcpMessage.MessageFactory;
import paulscode.android.mupen64plusae_mpn.netplay.TcpMessage.TcpMessage;

public class TcpClientHandler {

    Thread mClientThread;
    boolean mRunning = true;
    OutputStream mSocketOutputStream;
    InputStream mSocketInputStream;
    MessageFactory mMessageFactory;

    int mBufferTarget;

    TcpClientHandler(TcpServer server, int _buffer_target, Socket socket)
    {
        Log.i("TcpClientHandler", "New client connected");

        mBufferTarget = _buffer_target;

        try {
            mSocketOutputStream = socket.getOutputStream();
            mSocketInputStream = socket.getInputStream();
            mMessageFactory = new MessageFactory(server, mSocketOutputStream);
            mRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        mClientThread = new Thread(this::runClient);
        mClientThread.start();
    }
    
    void runClient()
    {
        while (mRunning)
        {
            // First read the whole message
            try {
                int id = mSocketInputStream.read();

                Log.i("TcpClientHandler", "Received message id=" + id);

                if (id == -1) {
                    mRunning = false;
                }

                if (id >= 0) {
                    TcpMessage message = mMessageFactory.getMessage(id);
                    if (message != null) {
                        message.parse(mSocketInputStream);
                        message.process();
                    } else {
                        mRunning = false;
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                mRunning = false;
                break;
            }
        }
    }
}
