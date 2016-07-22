package paulscode.android.mupen64plusae.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.mupen64plusae.v3.fzurita.R;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ProfileNameEditDialog extends DialogFragment
{
    private static final String STATE_DIALOG_ID = "STATE_DIALOG_ID";
    private static final String STATE_TITLE = "STATE_TITLE";
    private static final String STATE_NAME = "STATE_NAME";
    private static final String STATE_COMMENT = "STATE_COMMENT";
    private static final String STATE_NUM_ITEMS = "STATE_NUM_ITEMS";
    private static final String STATE_ITEMS = "STATE_ITEMS";
    private static final String STATE_ALLOW_SAME_NAME = "STATE_ALLOW_SAME_NAME";

    private String mName = null;
    private String mComment = null;
    private boolean mAllowSameName = false;
    private List<String> mItems = null;

    private View mDialogView = null;

    public interface OnProfileNameDialogButtonListener
    {
        /**
         * Called when a dialog menu item is selected
         * @param dialogId The dialog ID.
         * @param selectedButton The selected button.
         * @param name chosen name
         * @param comment chosen comment
         */
        public void onProfileNameDialogButton(int dialogId, int selectedButton, String name, String comment);
    }

    public static ProfileNameEditDialog newInstance(int dialogId, String title, String name, String comment,
        List<String> profileNames, boolean allowSameName)
    {
        ProfileNameEditDialog frag = new ProfileNameEditDialog();
        Bundle args = new Bundle();
        args.putInt(STATE_DIALOG_ID, dialogId);
        args.putString(STATE_TITLE, title);
        args.putString(STATE_NAME, name);
        args.putString(STATE_COMMENT, comment);
        args.putInt(STATE_NUM_ITEMS, profileNames.size());
        args.putBoolean(STATE_ALLOW_SAME_NAME, allowSameName);

        for (int index = 0; index < profileNames.size(); ++index)
        {
            String seq = profileNames.get(index);
            args.putString(STATE_ITEMS + index, seq);
        }

        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        setRetainInstance(true);

        final int dialogId = getArguments().getInt(STATE_DIALOG_ID);
        final String title = getArguments().getString(STATE_TITLE);
        mName = getArguments().getString(STATE_NAME);
        mComment = getArguments().getString(STATE_COMMENT);
        final int numItems = getArguments().getInt(STATE_NUM_ITEMS);
        mAllowSameName = getArguments().getBoolean(STATE_ALLOW_SAME_NAME);

        mItems = new ArrayList<String>();

        for (int index = 0; index < numItems; ++index)
        {
            String seq = getArguments().getString(STATE_ITEMS + index);
            mItems.add(seq);
        }

        mDialogView = View.inflate(getActivity(), R.layout.profile_edit_dialog, null);
        final TextView textWarning = (TextView) mDialogView.findViewById(R.id.textProfileWarning);
        final EditText editName = (EditText) mDialogView.findViewById(R.id.textProfileName);
        final EditText editComment = (EditText) mDialogView.findViewById(R.id.textProfileComment);

        editName.setText(mName);
        editComment.setText(mComment);

        Builder builder = new Builder(getActivity());
        builder.setTitle(title);

        // Create listener for OK/cancel button clicks
        OnClickListener clickListener = new OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                if (getActivity() instanceof OnProfileNameDialogButtonListener)
                {
                    ((OnProfileNameDialogButtonListener) getActivity()).onProfileNameDialogButton(dialogId, which,
                        editName.getText().toString().trim(), editComment.getText().toString());
                }
                else
                {
                    Log.e("ProfileNameEditDialog", "Activity doesn't implement OnProfileNameDialogButtonListener");
                }
            }
        };

        // Create the alert dialog
        builder.setTitle(title);
        builder.setView(mDialogView);
        builder.setPositiveButton(android.R.string.ok, clickListener);
        builder.setNegativeButton(android.R.string.cancel, clickListener);
        
        editName.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                Button okButton = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
                String warning = isValidName(mItems, mName, s.toString(), mAllowSameName);
                textWarning.setText(warning);
                okButton.setEnabled(TextUtils.isEmpty(warning));
            }
        });

        return builder.create();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        
        final TextView textWarning = (TextView) mDialogView.findViewById(R.id.textProfileWarning);
        final EditText editName = (EditText) mDialogView.findViewById(R.id.textProfileName);

        // Dynamically disable the OK button if the name is not unique
        final Button okButton = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
        String warning = isValidName(mItems, mName, editName.getText().toString(), mAllowSameName);
        textWarning.setText(warning);
        okButton.setEnabled(TextUtils.isEmpty(warning));
    }

    /**
     * Checks whether a candidate name is unique, non-empty, and contains only
     * safe characters. Unsafe characters are: '[', ']'.
     * 
     * @param profileNames
     *            list of profile names to compare against
     * @param oldName
     *            the old name
     * @param newName
     *            the new name
     * @param allowSameName
     *            set true to permit old and new names to be the same
     * @return empty string if the profile name is safe to use, otherwise a
     *         warning message
     */
    private String isValidName(List<String> profileNames, String oldName, String newName, boolean allowSameName)
    {
        newName = newName.trim();
        boolean isNotEmpty = !TextUtils.isEmpty(newName);
        boolean isLegal = !Pattern.matches(".*[\\[\\]].*", newName);
        boolean isSameName = oldName.equals(newName);
        boolean isUnique = !profileNames.contains(newName) || (isSameName && allowSameName);

        if (!isNotEmpty)
            return getString(R.string.profile_name_cannot_be_empty);
        else if (!isLegal)
            return getString(R.string.profile_name_cannot_contain_brackets);
        else if (!isUnique)
            return getString(R.string.profile_name_must_be_unique);
        else
            return "";
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