package paulscode.android.mupen64plusae-mpn.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog.Builder;
import android.util.Log;

public class ConfirmationDialog extends DialogFragment
{
    private static final String STATE_ID = "STATE_ID";
    private static final String STATE_TITLE = "STATE_TITLE";
    private static final String STATE_MESSAGE = "STATE_MESSAGE";
    
    private int mId = 0;
    
    /**
     * The listener interface for handling confirmations.
     */
    public interface PromptConfirmListener
    {
        /**
         * Handle the user's confirmation.
         */
        void onPromptDialogClosed(int id, int which);
    }

    public static ConfirmationDialog newInstance(int id, String title, String message)
    {
        ConfirmationDialog frag = new ConfirmationDialog();
        Bundle args = new Bundle();
        args.putInt(STATE_ID, id);
        args.putString(STATE_TITLE, title);
        args.putString(STATE_MESSAGE, message);

        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final String title = getArguments() != null ? getArguments().getString(STATE_TITLE) : "";
        final String message = getArguments() != null ? getArguments().getString(STATE_MESSAGE) : "";
        mId = getArguments().getInt(STATE_ID);

        // When the user clicks Ok, notify the downstream listener
        OnClickListener internalListener = (dialog, which) -> {
            if (getActivity() instanceof PromptConfirmListener)
            {
                ((PromptConfirmListener) getActivity()).onPromptDialogClosed(mId, which);
            }
            else
            {
                Log.e("ConfirmationDialog", "Activity doesn't implement PromptConfirmListener");
            }
        };

        Builder builder = new Builder(requireActivity());
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setNegativeButton(requireActivity().getString(android.R.string.cancel), internalListener);
        builder.setPositiveButton(requireActivity().getString( android.R.string.ok ), internalListener);

        return builder.create();
    }
    
    @Override
    public void onCancel(@NonNull DialogInterface dialog)
    {
        if (getActivity() instanceof PromptConfirmListener)
        {
            ((PromptConfirmListener) getActivity()).onPromptDialogClosed(mId, DialogInterface.BUTTON_NEGATIVE);
        }
        else
        {
            Log.e("ConfirmationDialog", "Activity doesn't implement PromptConfirmListener");
        }
    }
}