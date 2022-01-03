package paulscode.android.mupen64plusae.netplay.TcpMessage;

import android.util.Log;

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
        TcpServer.CoreSettings settings = null;
        final int maxTries = 100;
        int currentTry = 0;

        while (settings == null && currentTry < maxTries) {
            settings = mTcpServer.getSettings();

            if (settings != null) {
                mOutboundByteBuffer.putInt(settings.mCountPerOp);
                mOutboundByteBuffer.putInt(settings.mCountPerOpDenomPot);
                mOutboundByteBuffer.putInt(settings.mDisableExtraMem);
                mOutboundByteBuffer.putInt(settings.mSiDmADuration);
                mOutboundByteBuffer.putInt(settings.mEmuMode);
                mOutboundByteBuffer.putInt(settings.mNoCompiledJump);

                Log.e("Netplay", "count_per_op=" + settings.mCountPerOp +
                        " count_per_op_denom_pot=" + settings.mCountPerOpDenomPot +
                        " disable_extra_mem=" + settings.mDisableExtraMem +
                        " si_dma_duration=" + settings.mSiDmADuration +
                                " emu_mode=" + settings.mEmuMode +
                                " no_compiled_jump=" + settings.mNoCompiledJump

                        );

                try {
                    mOutputStream.write(mOutboundByteBuffer.array());
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
            Log.e("Netplay", "Unable to send settings");
        }
    }
}
