package paulscode.android.mupen64plusae.netplay;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.netplay.room.NetplayRoomClient;
import paulscode.android.mupen64plusae.netplay.room.NetplayRoomServer;
import paulscode.android.mupen64plusae.util.DeviceUtil;
import paulscode.android.mupen64plusae.util.DisplayWrapper;

public class NetplayServerSetupDialog extends DialogFragment
{
    static final String TAG = "NpServerSetupDialog";

    public interface OnClientDialogActionListener {
        /**
         * Called when asked to connet
         * @param player Player number, 1-4
         */
        void connect(int regId, int player, InetAddress address, int port);

        /**
         * Called when emulation is started
         */
        void start();
    }

    private static final String ROM_MD5 = "ROM_MD5";
    private static final String SERVER_PORT = "SERVER_PORT";

    private ClientListAdapter mServerListAdapter = null;

    private final ArrayList<NetplayClient> mClients = new ArrayList<>();

    private NetplayRoomServer mNetplayRoomService;

    /**
     *
     * @return A cheat dialog
     */
    public static NetplayServerSetupDialog newInstance(String romMd5, int serverPort)
    {
        NetplayServerSetupDialog frag = new NetplayServerSetupDialog();
        Bundle args = new Bundle();
        args.putString(ROM_MD5, romMd5);
        args.putInt(SERVER_PORT, serverPort);
        frag.setArguments(args);
        return frag;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        setRetainInstance(true);

        final String romMd5 = getArguments().getString(ROM_MD5);
        final int serverPort = getArguments().getInt(SERVER_PORT);

        View dialogView = View.inflate(getActivity(), R.layout.netplay_server_setup_dialog, null);

        ListView serverListView = dialogView.findViewById(R.id.clientList);
        Button startButton = dialogView.findViewById(R.id.buttonStart);

        mServerListAdapter = new ClientListAdapter(getActivity(), mClients);
        serverListView.setAdapter(mServerListAdapter);

        //Time to create the dialog
        Builder builder = new Builder(requireActivity());
        builder.setTitle(getString(R.string.netplayClients_title));

        builder.setNegativeButton(null, null);
        builder.setPositiveButton(null, null);

        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        DisplayWrapper.setDialogToResizeWithKeyboard(dialog, dialogView);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        String deviceName = DeviceUtil.getDeviceName(getActivity().getContentResolver());

        // Add ourselves
        mClients.add(new NetplayClient(1, deviceName));
        mServerListAdapter.notifyDataSetChanged();

        mNetplayRoomService = new NetplayRoomServer(getActivity().getApplicationContext(), deviceName, romMd5, serverPort, (playerNumber, deviceName1) -> getActivity().runOnUiThread(() -> {
            mClients.add(new NetplayClient(playerNumber, deviceName1));
            mServerListAdapter.notifyDataSetChanged();
        }));

        int registrationId = mNetplayRoomService.registerPlayerOne();

        if (getActivity() instanceof OnClientDialogActionListener) {
            OnClientDialogActionListener listener = (OnClientDialogActionListener)getActivity();
            listener.connect(registrationId, 1, DeviceUtil.wifiIpAddress(getActivity()), serverPort);
        } else {
            Log.e(TAG, "Invalid activity, expected OnClientDialogActionListener");
        }

        startButton.setOnClickListener(v -> {
            if (getActivity() instanceof OnClientDialogActionListener) {
                OnClientDialogActionListener listener = (OnClientDialogActionListener)getActivity();
                mNetplayRoomService.start();
                listener.start();
            } else {
                Log.e(TAG, "Invalid activity, expected OnClientDialogActionListener");
            }
        });

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
