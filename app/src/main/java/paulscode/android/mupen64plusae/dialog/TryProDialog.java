package paulscode.android.mupen64plusae.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog.Builder;
import android.util.Log;

import paulscode.android.mupen64plusae.R;

public class TryProDialog extends DialogFragment
{
    /**
     * The listener interface for handling confirmations.
     */
    public interface PromptTryPoListener
    {
        /**
         * Handle the user's confirmation.
         */
        void onTryProDialogClosed(int which);
    }

    public static TryProDialog newInstance()
    {
        TryProDialog frag = new TryProDialog();
        Bundle args = new Bundle();

        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        setRetainInstance(true);

        final String title = getString(R.string.confirmTryPro);
        final String message = getString(R.string.confirmPleaseTryPro);

        // When the user clicks Ok, notify the downstream listener
        OnClickListener internalListener = (dialog, which) -> {
            if (requireActivity() instanceof PromptTryPoListener)
            {
                ((PromptTryPoListener) requireActivity()).onTryProDialogClosed(which);
            }
            else
            {
                Log.e("PleaseRateDialog", "Activity doesn't implement PromptConfirmListener");
            }
        };

        Builder builder = new Builder(requireActivity());
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton(requireActivity().getString(R.string.confirmTryPro), internalListener);
        builder.setNeutralButton(requireActivity().getString(R.string.confirmRemindMeLater), internalListener);
        builder.setNegativeButton(requireActivity().getString(R.string.confirmNoThanks), internalListener);
        
        return builder.create();
    }
    
    @Override
    public void onCancel(@NonNull DialogInterface dialog)
    {
        if (requireActivity() instanceof PromptTryPoListener)
        {
            ((PromptTryPoListener) requireActivity()).onTryProDialogClosed(DialogInterface.BUTTON_NEUTRAL);
        }
        else
        {
            Log.e("PleaseRateDialog", "Activity doesn't implement PromptConfirmListener");
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