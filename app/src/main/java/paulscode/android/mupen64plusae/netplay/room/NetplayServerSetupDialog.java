package paulscode.android.mupen64plusae.netplay.room;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.DialogFragment;

import org.mupen64plusae.v3.alpha.R;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import paulscode.android.mupen64plusae.util.DeviceUtil;
import paulscode.android.mupen64plusae.util.DisplayWrapper;

public class NetplayServerSetupDialog extends DialogFragment
{
    static final String TAG = "NpServerSetupDialog";

    public interface OnClientDialogActionListener {
        /**
         * Called when asked to connet
         * @param regId Registration id
         * @param player Player number, 1-4
         * @param videoPlugin Video plugin
         * @param rspPlugin RSP plugin
         * @param address Server address
         * @param port Server port
         */
        void connect(int regId, int player, String videoPlugin, String rspPlugin, InetAddress address, int port);

        /**
         * Called when emulation is started
         */
        void start();

        /**
         * Called when netplay is cancelled
         */
        void cancel();
    }

    private static final String ROM_MD5 = "ROM_MD5";
    private static final String VIDEO_PLUGIN = "VIDEO_PLUGIN";
    private static final String RSP_PLUGIN = "RSP_PLUGIN";
    private static final String SERVER_PORT = "SERVER_PORT";

    private ClientListAdapter mServerListAdapter = null;

    private final ArrayList<NetplayClient> mClients = new ArrayList<>();

    private NetplayRoomServer mNetplayRoomService;

    /**
     *
     * @return A cheat dialog
     */
    public static NetplayServerSetupDialog newInstance(String romMd5, String videoPlugin, String rspPlugin, int serverPort)
    {
        NetplayServerSetupDialog frag = new NetplayServerSetupDialog();
        Bundle args = new Bundle();
        args.putString(ROM_MD5, romMd5);
        args.putString(VIDEO_PLUGIN, videoPlugin);
        args.putString(RSP_PLUGIN, rspPlugin);
        args.putInt(SERVER_PORT, serverPort);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_Mupen64plusaeTheme);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        setRetainInstance(true);

        Bundle args = getArguments();

        if (args == null) {
            args = new Bundle();
        }

        final String romMd5 = args.getString(ROM_MD5);
        final String videoPlugin = args.getString(VIDEO_PLUGIN);
        final String rspPlugin = args.getString(RSP_PLUGIN);

        final int serverPort = getArguments().getInt(SERVER_PORT);

        View dialogView = View.inflate(requireActivity(), R.layout.netplay_server_setup_dialog, null);

        ListView serverListView = dialogView.findViewById(R.id.clientList);
        Button startButton = dialogView.findViewById(R.id.buttonStart);

        mServerListAdapter = new ClientListAdapter(requireActivity(), mClients);
        serverListView.setAdapter(mServerListAdapter);

        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);
        TextView serverAddress = dialogView.findViewById(R.id.textHostAddress);
        TextView port1 = dialogView.findViewById(R.id.textPort1);
        TextView port2 = dialogView.findViewById(R.id.textPort2);

        //Time to create the dialog
        Builder builder = new Builder(requireActivity());
        builder.setNegativeButton(null, null);
        builder.setPositiveButton(null, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        DisplayWrapper.setDialogToResizeWithKeyboard(dialog, dialogView);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        setCancelable(false);

        cancelButton.setOnClickListener(v -> {
            if (requireActivity() instanceof OnClientDialogActionListener) {
                ((OnClientDialogActionListener) requireActivity()).cancel();
            }
        });

        InetAddress address = DeviceUtil.wifiIpAddress(requireActivity());

        if (address != null) {
            String serverInfoText = "" + address.getHostAddress();

            SpannableString spanString = new SpannableString(serverInfoText);
            spanString.setSpan(new UnderlineSpan(), 0, spanString.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), 0, spanString.length(), 0);

            serverAddress.setText(spanString);
        }

        String deviceName = DeviceUtil.getDeviceName(requireActivity().getContentResolver());

        // Add ourselves
        if (mClients.size() == 0) {
            mClients.add(new NetplayClient(1, deviceName));
            mServerListAdapter.notifyDataSetChanged();

            mNetplayRoomService = new NetplayRoomServer(requireActivity().getApplicationContext(),
                    deviceName, romMd5, videoPlugin, rspPlugin, serverPort,
                    new NetplayRoomServer.OnClientFound() {
                        @Override
                        public void onClientRegistration(int playerNumber, String deviceName) {
                            try {
                                mClients.add(new NetplayClient(playerNumber, deviceName));

                                requireActivity().runOnUiThread(() -> mServerListAdapter.notifyDataSetChanged());
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onClienLeave(int playerNumber) {
                            try {
                                mClients.removeIf(client -> client.mPlayerNumer == playerNumber);

                                requireActivity().runOnUiThread(() -> mServerListAdapter.notifyDataSetChanged());
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                        }
                    });

            int registrationId = mNetplayRoomService.registerPlayerOne();

            if (requireActivity() instanceof OnClientDialogActionListener) {

                if (address == null) {
                    byte[] ipAddr = new byte[]{127, 0, 0, 1};
                    try {
                        address = InetAddress.getByAddress(ipAddr);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }

                if (address != null) {
                    OnClientDialogActionListener listener = (OnClientDialogActionListener)requireActivity();
                    listener.connect(registrationId, 1, videoPlugin, rspPlugin,
                            address, serverPort);
                }
            } else {
                Log.e(TAG, "Invalid activity, expected OnClientDialogActionListener");
            }
        }

        if (mNetplayRoomService != null) {
            SpannableString port1SpanString = new SpannableString(String.format(Locale.getDefault(), "%d", mNetplayRoomService.getServerPort()));
            port1SpanString.setSpan(new UnderlineSpan(), 0, port1SpanString.length(), 0);
            port1SpanString.setSpan(new StyleSpan(Typeface.ITALIC), 0, port1SpanString.length(), 0);

            port1.setText(port1SpanString);
        }

        SpannableString port2SpanString = new SpannableString(String.format(Locale.getDefault(), "%d", serverPort));
        port2SpanString.setSpan(new UnderlineSpan(), 0, port2SpanString.length(), 0);
        port2SpanString.setSpan(new StyleSpan(Typeface.ITALIC), 0, port2SpanString.length(), 0);
        port2.setText(port2SpanString);

        startButton.setOnClickListener(v -> {
            if (requireActivity() instanceof OnClientDialogActionListener) {
                OnClientDialogActionListener listener = (OnClientDialogActionListener)requireActivity();
                mNetplayRoomService.start();
                listener.start();
            } else {
                Log.e(TAG, "Invalid activity, expected OnClientDialogActionListener");
            }
        });


        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    @Override
    public void onDestroyView()
    {
        // This is needed because of this:
        // https://code.google.com/p/android/issues/detail?id=17423

        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    static class NetplayClient implements Comparable<NetplayClient>
    {
        private final int mPlayerNumer;
        private final String mPlayerName;

        NetplayClient(int playerNumber, String playerName)
        {
            mPlayerNumer = playerNumber;
            mPlayerName = playerName;
        }

        @Override
        public int compareTo(NetplayClient other) {
            return toString().compareTo(other.toString());
        }

        @Override
        @NonNull
        public String toString() {
            return "" + mPlayerNumer + ": " + mPlayerName;
        }
    }

    private static class ClientListAdapter extends ArrayAdapter<NetplayClient>
    {
        private static final int RESID = R.layout.list_single_text;

        ClientListAdapter(Context context, List<NetplayClient> clients)
        {
            super(context, RESID, clients);
        }

        @Override
        public @NonNull View getView(int position, @Nullable View convertView,
                                     @NonNull ViewGroup parent)
        {
            Context context = getContext();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            View view = convertView;
            if( view == null )
                view = inflater.inflate( RESID, null );

            NetplayClient item = getItem( position );
            if( item != null )
            {
                TextView text1 = view.findViewById( R.id.text1 );
                String player = item.mPlayerNumer + ": " + item.mPlayerName;
                text1.setText(player);
            }
            return view;
        }
    }
}
