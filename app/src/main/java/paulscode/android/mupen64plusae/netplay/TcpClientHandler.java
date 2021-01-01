package paulscode.android.mupen64plusae.netplay;

import android.util.Log;

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
        Log.e("TcpClientHandler", "NEW CLIENT!!!");

        mBufferTarget = _buffer_target;
        mSocket = socket;
        mServer = server;

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
            Log.e("TcpClientHandler", "CLIENT THREAD IS RUNNING");

            // First read the whole message
            try {
                int id = mSocketInputStream.read();
                if (id == -1) {
                    mRunning = false;
                }

                Log.e("TcpClientHandler", "GOT MESSAGE WITH ID=" + id);


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
