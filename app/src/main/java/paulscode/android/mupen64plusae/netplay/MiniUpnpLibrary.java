package paulscode.android.mupen64plusae.netplay;

import com.sun.jna.Callback;
import com.sun.jna.JNIEnv;
import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * Library used to interface with AE Vid Ext implementation
 */

@SuppressWarnings("unused")
public interface MiniUpnpLibrary extends Library {

    // Initialize library

    /**
     * Initialize library
     * @param timeout Timeout for initialization
     */
    void UPnPInit(final int timeout);

    /**
     * Shutdown library
     */
    void UPnPShutdown();

    /**
     * Add port forward
     * @param protocol Protocol, either "TCP" or "UDP"
     * @param description Port description
     * @param port External port
     * @param intport Internal port
     */
    void UPnP_Add(String protocol, String description, int port, int intport);

    /**
     * Remove port forward
     * @param protocol Protocol, either "TCP" or "UDP"
     * @param port External port
     */
    void UPnP_Remove(String protocol, int port);
}