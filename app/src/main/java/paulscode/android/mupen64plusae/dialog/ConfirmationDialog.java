package paulscode.android.mupen64plusae.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
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
     * 
     * @see Prompt#promptConfirm
     */
    public interface PromptConfirmListener
    {
        /**
         * Handle the user's confirmation.
         */
        public void onPromptDialogClosed( int id, int which );
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

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        setRetainInstance(true);

        final String title = getArguments().getString(STATE_TITLE);
        final String message = getArguments().getString(STATE_MESSAGE);
        mId = getArguments().getInt(STATE_ID);

        // When the user clicks Ok, notify the downstream listener
        OnClickListener internalListener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if (getActivity() instanceof PromptConfirmListener)
                {
                    ((PromptConfirmListener) getActivity()).onPromptDialogClosed(mId, which);
                }
                else
                {
                    Log.e("ConfirmationDialog", "Activity doesn't implement PromptConfirmListener");
                }
            }
        };

        Builder builder = new Builder(getActivity());
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setNegativeButton(getActivity().getString(android.R.string.cancel), internalListener);
        builder.setPositiveButton(getActivity().getString( android.R.string.ok ), internalListener);
        
        final AlertDialog promptInputCodeDialog = builder.create();
        
        return promptInputCodeDialog;
    }
    
    @Override
    public void onCancel(DialogInterface dialog)
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