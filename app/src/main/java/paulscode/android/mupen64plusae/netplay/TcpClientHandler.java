package paulscode.android.mupen64plusae.netplay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import paulscode.android.mupen64plusae.netplay.TcpMessage.MessageFactory;
import paulscode.android.mupen64plusae.netplay.TcpMessage.TcpMessage;

public class TcpClientHandler {

    Socket mSocket;
    Thread mClientThread;
    boolean mRunning = true;
    OutputStream mSocketOutputStream;
    InputStream mSocketInputStream;
    MessageFactory mMessageFactory;

    TcpServer mServer;
    int mBufferTarget;

    TcpClientHandler(TcpServer server, int _buffer_target, Socket socket)
    {
        mBufferTarget = _buffer_target;
        mSocket = socket;
        mServer = server;

        try {
            mSocketOutputStream = socket.getOutputStream();
            mSocketInputStream = socket.getInputStream();
            mMessageFactory = new MessageFactory(server, mSocketOutputStream);
            mRunning = false;
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
