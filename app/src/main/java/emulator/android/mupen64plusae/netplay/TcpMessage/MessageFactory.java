package emulator.android.mupen64plusae.netplay.TcpMessage;

import java.io.OutputStream;

import emulator.android.mupen64plusae.netplay.TcpServer;

public class MessageFactory {

    enum MessageId {
        SAVE_FILE_DATA(1),
        REQUEST_SAVE_FILE_DATA(2),
        SETTINGS_UPDATE(3),
        REQUEST_SETTINGS(4),
        PLAYER_REGISTRATION(5),
        REQUEST_PLAYER_REGISTRATION(6),
        PLAYER_DISCONNECT(7),
        UNKNOWN(-1);

        int mId;

        MessageId(int id) {
            mId = id;
        }

        static MessageId getMessage(int idNumber) {
            for(MessageId id : MessageId.values()) {
                if (id.mId == idNumber) return id;
            }

            return UNKNOWN;
        }
    }

    PlayerDisconnectMessage mPlayerDisconnectMessage;
    PlayerRegistrationMessage mPlayerRegistrationMessage;
    RequestPlayerRegistrationMessage mRequestPlayerRegistrationMessage;
    RequestSaveFileDataMessage mRequestSaveFileDataMessage;
    RequestSettingsMessage mRequestSettingsMessage;
    SaveFileDataMessage mSaveFileDataMessage;
    SettingsUpdateMessage mSettingsUpdateMessage;

    public MessageFactory(TcpServer server, OutputStream outputStream) {
        mPlayerDisconnectMessage = new PlayerDisconnectMessage(server);
        mPlayerRegistrationMessage = new PlayerRegistrationMessage(server, outputStream);
        mRequestPlayerRegistrationMessage = new RequestPlayerRegistrationMessage(server, outputStream);
        mRequestSaveFileDataMessage = new RequestSaveFileDataMessage(server, outputStream);
        mRequestSettingsMessage = new RequestSettingsMessage(server, outputStream);
        mSaveFileDataMessage = new SaveFileDataMessage(server);
        mSettingsUpdateMessage = new SettingsUpdateMessage(server);
    }

    public TcpMessage getMessage(int messageId) {
        MessageId id = MessageId.getMessage(messageId);

        switch (id) {
            case SAVE_FILE_DATA:
                return mSaveFileDataMessage;
            case REQUEST_SAVE_FILE_DATA:
                return mRequestSaveFileDataMessage;
            case SETTINGS_UPDATE:
                return mSettingsUpdateMessage;
            case REQUEST_SETTINGS:
                return mRequestSettingsMessage;
            case PLAYER_REGISTRATION:
                return mPlayerRegistrationMessage;
            case REQUEST_PLAYER_REGISTRATION:
                return mRequestPlayerRegistrationMessage;
            case PLAYER_DISCONNECT:
                return mPlayerDisconnectMessage;
            case UNKNOWN:
                return null;
        }

        return null;
    }
}
