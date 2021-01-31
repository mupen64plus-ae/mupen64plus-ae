package paulscode.android.mupen64plusae.netplay.room;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.util.DeviceUtil;
import paulscode.android.mupen64plusae.util.DisplayWrapper;
import paulscode.android.mupen64plusae.util.Notifier;

public class NetplayClientSetupDialog extends DialogFragment implements AdapterView.OnItemClickListener
{
    static final String TAG = "NpClientSetupDialog";

    public interface OnServerDialogActionListener {
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

    private NetplayRoomClient mRoomClient = null;

    private LinearLayout mLinearLayoutWaiting;

    private LinearLayout mLinearLayoutManualEntry;

    private boolean mWaiting = false;
    private boolean mManualEntry = false;

    private ListView mServerListView;

    private String mRomMd5 = "";

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
        
        Bundle args = getArguments();
        
        if (args == null) {
            args = new Bundle();
        }

        mRomMd5 = args.getString(ROM_MD5);

        View dialogView = View.inflate(requireActivity(), R.layout.netplay_client_setup_dialog, null);

        mServerListView = dialogView.findViewById(R.id.serverList);
        mLinearLayoutWaiting = dialogView.findViewById(R.id.linearLayoutWaiting);
        mLinearLayoutManualEntry = dialogView.findViewById(R.id.linearLayoutManualEntry);

        if (mWaiting) {
            mServerListView.setVisibility(View.GONE);
            mLinearLayoutManualEntry.setVisibility(View.GONE);
        } else {
            mLinearLayoutWaiting.setVisibility(View.GONE);

            if (mManualEntry) {
                mServerListView.setVisibility(View.GONE);
            } else {
                mLinearLayoutManualEntry.setVisibility(View.GONE);
            }
        }

        if (mServerListAdapter == null) {
            mServerListAdapter = new ServerListAdapter(requireActivity(), mServers);
            mServerListView.setAdapter(mServerListAdapter);
            mServerListView.setOnItemClickListener(this);
        }

        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);
        Button enterIp = dialogView.findViewById(R.id.buttonEnterIp);

        EditText manualIp = dialogView.findViewById(R.id.ipAddressEditText);
        EditText manualPort = dialogView.findViewById(R.id.portEditText);

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

        String deviceName = DeviceUtil.getDeviceName(requireActivity().getContentResolver());

        if (mRoomClient == null) {
            mRoomClient = new NetplayRoomClient(requireActivity(), deviceName, new NetplayRoomClient.OnServerFound() {

                @Override
                public void onValidServerFound(int serverId, String serverName, String romMd5) {
                    requireActivity().runOnUiThread(() -> {
                        mServers.add(new NetplayServer(serverId, serverName, romMd5));
                        mServerListAdapter.notifyDataSetChanged();
                    });
                }

                @Override
                public void onServerRegistration(int regId, int player, String videoPlugin, String rspPlugin,
                                                 InetAddress address, int port) {
                    if (requireActivity() instanceof OnServerDialogActionListener)
                    {
                        requireActivity().runOnUiThread(() -> {
                            ((OnServerDialogActionListener) requireActivity()).connect(regId, player, videoPlugin, rspPlugin, address, port);
                            mServerListView.setVisibility(View.GONE);
                            mLinearLayoutWaiting.setVisibility(View.VISIBLE);
                            enterIp.setVisibility(View.GONE);
                            mWaiting = true;
                        });
                    }
                    else
                    {
                        Log.e(TAG, "Activity doesn't implement OnServerDialogActionListener");
                    }
                }

                @Override
                public void onServerStart() {
                    if (requireActivity() instanceof OnServerDialogActionListener)
                    {
                        requireActivity().runOnUiThread(() -> ((OnServerDialogActionListener) requireActivity()).start());
                    }
                    else
                    {
                        Log.e(TAG, "Activity doesn't implement OnServerDialogActionListener");
                    }
                }
            });
        }

        cancelButton.setOnClickListener(v -> {
            mRoomClient.leaveServer();

            if (requireActivity() instanceof OnServerDialogActionListener) {
                ((OnServerDialogActionListener) requireActivity()).cancel();
            }
        });

        enterIp.setOnClickListener(v -> {

            // Turn this button into a connect button when it's pressed
            if (!mManualEntry) {
                mManualEntry = true;
                mServerListView.setVisibility(View.GONE);
                mLinearLayoutManualEntry.setVisibility(View.VISIBLE);
                enterIp.setText(R.string.netplay_connect);
            } else {
                mManualEntry = false;
                mServerListView.setVisibility(View.VISIBLE);
                mLinearLayoutManualEntry.setVisibility(View.GONE);
                enterIp.setText(R.string.netplay_enterIp);

                if (mRoomClient != null) {

                    try {
                        String hostnameString = manualIp.getText().toString();
                        String portString = manualPort.getText().toString();

                        if (!TextUtils.isEmpty(hostnameString) && !TextUtils.isEmpty(portString)) {
                            InetAddress address = InetAddress.getByName(hostnameString);
                            int port = Integer.parseInt(portString);
                            mRoomClient.connectToServer(address, port);
                        }
                    } catch (UnknownHostException|NumberFormatException e) {
                        e.printStackTrace();
                    }
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
        if (mServers.get(position).mRomMd5.equals(mRomMd5)) {
            mRoomClient.registerServer(mServers.get(position).mServerId);
        } else {
            Notifier.showToast(requireActivity(), R.string.netplay_romMd5Mismatch);
        }
    }

    static class NetplayServer implements Comparable<NetplayServer>
    {
        private final int mServerId;
        private final String mServerName;
        private final String mRomMd5;

        NetplayServer(int serverId, String serverName, String romMd5)
        {
            mServerId = serverId;
            mServerName = serverName;
            mRomMd5 = romMd5;
        }

        @Override
        public int compareTo(NetplayServer other) {
            return toString().compareTo(other.toString());
        }

        @Override
        @NonNull
        public String toString() {
            return "" + mServerId + ": " + mServerName + " md5=" + mRomMd5;
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
