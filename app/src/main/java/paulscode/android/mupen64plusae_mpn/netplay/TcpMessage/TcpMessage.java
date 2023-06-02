package paulscode.android.mupen64plusae_mpn.netplay.TcpMessage;

import java.io.IOException;
import java.io.InputStream;

public interface TcpMessage {

    void parse(InputStream stream) throws IOException;

    void process() throws IOException;
}
