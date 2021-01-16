package paulscode.android.mupen64plusae.netplay;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.DialogFragment;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.util.DisplayWrapper;

public class NetplaySetupDialog extends DialogFragment
{
    public interface OnDialogActionListener {
        /**
         * Called when asked to connet
         * @param player Player number, 1-4
         */
        void connect(int player);

        /**
         * Called when memulation is started
         */
        void start();
    }
    Button mConnectButtonPlayer1;
    Button mConnectButtonPlayer2;
    Button mStartEmulationButton;

    /**
     *
     * @return A cheat dialog
     */
    public static NetplaySetupDialog newInstance()
    {
        NetplaySetupDialog frag = new NetplaySetupDialog();
        Bundle args = new Bundle();
        frag.setArguments(args);
        return frag;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        setRetainInstance(true);

        String title = "BLAH";


        View dialogView = View.inflate(getActivity(), R.layout.netplay_setup_dialog, null);

        mConnectButtonPlayer1 = dialogView.findViewById(R.id.connectButtonPlayer1);
        mConnectButtonPlayer2 = dialogView.findViewById(R.id.connectButtonPlayer2);
        mStartEmulationButton = dialogView.findViewById(R.id.startEmulation);

        //Time to create the dialog
        Builder builder = new Builder(requireActivity());
        builder.setTitle(title);

        builder.setNegativeButton(null, null);
        builder.setPositiveButton(null, null);

        mConnectButtonPlayer1.setOnClickListener(v -> {
            if (getActivity() instanceof OnDialogActionListener)
            {
                ((OnDialogActionListener) getActivity()).connect(1);
            }
            else
            {
                Log.e("NetplaySetupDialog", "Activity doesn't implement onDialogActionListener");
            }
        });

        mConnectButtonPlayer2.setOnClickListener(v -> {
            if (getActivity() instanceof OnDialogActionListener)
            {
                ((OnDialogActionListener) getActivity()).connect(2);
            }
            else
            {
                Log.e("NetplaySetupDialog", "Activity doesn't implement onDialogActionListener");
            }
        });

        mStartEmulationButton.setOnClickListener(v -> {
            if (getActivity() instanceof OnDialogActionListener)
            {
                ((OnDialogActionListener) getActivity()).start();
            }
            else
            {
                Log.e("NetplaySetupDialog", "Activity doesn't implement onDialogActionListener");
            }
        });

        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        DisplayWrapper.setDialogToResizeWithKeyboard(dialog, dialogView);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

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
}
