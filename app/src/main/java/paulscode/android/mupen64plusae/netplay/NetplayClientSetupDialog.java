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
import android.widget.LinearLayout;
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
import paulscode.android.mupen64plusae.util.DeviceUtil;
import paulscode.android.mupen64plusae.util.DisplayWrapper;

public class NetplayClientSetupDialog extends DialogFragment implements AdapterView.OnItemClickListener
{
    static final String TAG = "NpClientSetupDialog";

    public interface OnServerDialogActionListener {
        /**
         * Called when asked to connet
         * @param player Player number, 1-4
         */
        void connect(int regId, int player, InetAddress address, int port);

        /**
         * Called when memulation is started
         */
        void start();

        /**
         * Called when netplay is cancelled
         */
        void cancel();
    }

    private static final String ROM_MD5 = "ROM_MD5";

    private ServerListAdapter mServerListAdapter = null;

    private final ArrayList<NetplayServer> mServers = new ArrayList<>();

    private NetplayRoomClient mRoomClient;

    private LinearLayout mLinearLayoutWaiting;

    ListView mServerListView;

    /**
     *
     * @return A cheat dialog
     */
    public static NetplayClientSetupDialog newInstance(String romMd5)
    {
        NetplayClientSetupDialog frag = new NetplayClientSetupDialog();
        Bundle args = new Bundle();
        args.putString(ROM_MD5, romMd5);
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

        final String romMd5 = getArguments().getString(ROM_MD5);

        View dialogView = View.inflate(getActivity(), R.layout.netplay_client_setup_dialog, null);

        mServerListView = dialogView.findViewById(R.id.serverList);
        mLinearLayoutWaiting = dialogView.findViewById(R.id.linearLayoutWaiting);
        mLinearLayoutWaiting.setVisibility(View.GONE);

        mServerListAdapter = new ServerListAdapter(getActivity(), mServers);
        mServerListView.setAdapter(mServerListAdapter);
        mServerListView.setOnItemClickListener(this);

        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        //Time to create the dialog
        Builder builder = new Builder(requireActivity());
        builder.setTitle(getString(R.string.netplayServers_title));

        builder.setNegativeButton(null, null);
        builder.setPositiveButton(null, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        DisplayWrapper.setDialogToResizeWithKeyboard(dialog, dialogView);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        setCancelable(false);

        cancelButton.setOnClickListener(v -> {
            if (getActivity() instanceof OnServerDialogActionListener) {
                ((OnServerDialogActionListener) getActivity()).cancel();
            }
        });

        String deviceName = DeviceUtil.getDeviceName(getActivity().getContentResolver());
        mRoomClient = new NetplayRoomClient(getActivity(), deviceName, romMd5, new NetplayRoomClient.OnServerFound() {
            @Override
            public void onValidServerFound(int serverId, String serverName) {
                getActivity().runOnUiThread(() -> {
                    mServers.add(new NetplayServer(serverId, serverName));
                    mServerListAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onServerRegistration(int regId, int player, InetAddress address, int port) {
                if (getActivity() instanceof OnServerDialogActionListener)
                {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((OnServerDialogActionListener) getActivity()).connect(regId, player, address, port);
                            mServerListView.setVisibility(View.GONE);
                            mLinearLayoutWaiting.setVisibility(View.VISIBLE);
                        }
                    });
                }
                else
                {
                    Log.e(TAG, "Activity doesn't implement OnServerDialogActionListener");
                }
            }

            @Override
            public void onServerStart() {
                if (getActivity() instanceof OnServerDialogActionListener)
                {
                    getActivity().runOnUiThread(() -> ((OnServerDialogActionListener) getActivity()).start());
                }
                else
                {
                    Log.e(TAG, "Activity doesn't implement OnServerDialogActionListener");
                }
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mRoomClient.registerServer(mServers.get(position).mServerId);
    }

    static class NetplayServer implements Comparable<NetplayServer>
    {
        private final int mServerId;
        private final String mServerName;

        NetplayServer(int serverId, String serverName)
        {
            mServerId = serverId;
            mServerName = serverName;
        }

        @Override
        public int compareTo(NetplayServer other) {
            return toString().compareTo(other.toString());
        }

        @Override
        @NonNull
        public String toString() {
            return "" + mServerId + ": " + mServerName;
        }
    }

    private static class ServerListAdapter extends ArrayAdapter<NetplayServer>
    {
        private static final int RESID = R.layout.list_single_text;

        ServerListAdapter(Context context, List<NetplayServer> servers )
        {
            super(context, RESID, servers);
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

            NetplayServer item = getItem( position );
            if( item != null )
            {
                TextView text1 = view.findViewById( R.id.text1 );
                text1.setText( item.mServerName );
            }
            return view;
        }
    }
}
