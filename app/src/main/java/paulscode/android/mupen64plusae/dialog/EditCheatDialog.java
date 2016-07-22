package paulscode.android.mupen64plusae.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.mupen64plusae.v3.fzurita.R;

import paulscode.android.mupen64plusae.cheat.CheatEditorActivity.CheatAddressData;
import paulscode.android.mupen64plusae.cheat.CheatEditorActivity.CheatOptionData;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class EditCheatDialog extends DialogFragment
{
    private static final String STATE_TITLE = "STATE_TITLE";
    private static final String STATE_NAME = "STATE_NAME";
    private static final String STATE_COMMENT = "STATE_COMMENT";
    private static final String STATE_NUM_ADDRESS = "STATE_NUM_ADDRESS";
    private static final String STATE_ADDRESS_ITEM = "STATE_ADDRESS_ITEM";
    private static final String STATE_ADDRESS_VALUE = "STATE_ADDRESS_VALUE";
    private static final String STATE_NUM_OPTION_ITEMS = "STATE_NUM_ITEMS";
    private static final String STATE_OPTION_ITEMS_DESC = "STATE_OPTION_ITEMS_DESC";
    private static final String STATE_OPTION_ITEMS_VALUE = "STATE_OPTION_ITEMS_VALUE";
    private static final String STATE_NUM_CHEAT_ITEMS = "STATE_NUM_CHEAT_ITEMS";
    private static final String STATE_ITEMS_CHEAT = "STATE_ITEMS_CHEAT";

    private String mName = null;
    private String mComment = null;
    private List<CheatOptionData> mOptionItems = null;
    private List<CheatAddressData> mAddresses = null;
    private List<String> mCheatNameItems = null;
    
    private EditText mEditName = null;
    private EditText mEditComment = null;
    private EditText mEditAddress = null;
    private EditText mEditValue = null;
    private Button mAddOptionButton = null;
    private Button mAddAddressButton = null;
    private LinearLayout mOptionsLayoutHolder = null;

    private View mDialogView = null;
    private ArrayList<EditText> mOptionValueFields = null;
    private ArrayList<EditText> mCheatAddressFields = null;
    private ArrayList<EditText> mCheatValueFields = null;

    public interface OnEditCompleteListener
    {
        /**
         * Called after cheat editing is complete
         * @param selectedButton The selected button.
         * @param name chosen cheat title
         * @param comment chosen cheat comment
         * @param address cheat address
         * @param options cheat options
         */
        public void onEditComplete(int selectedButton, String name, String comment,
            List<CheatAddressData> address, List<CheatOptionData> options);
    }
    
    /**
     * 
     * @param title Dialog title
     * @param name Cheat title
     * @param comment Cheat description
     * @param address Cheat memory address
     * @param options Cheat options
     * @param cheatNames All cheat titles to prevent duplicate names
     * @return
     */
    public static EditCheatDialog newInstance(String title, String name, String comment,
        List<CheatAddressData> address, List<CheatOptionData> options, List<String> cheatNames)
    {
        EditCheatDialog frag = new EditCheatDialog();
        Bundle args = new Bundle();
        args.putString(STATE_TITLE, title);
        args.putString(STATE_NAME, name);
        args.putString(STATE_COMMENT, comment);
        
        if(address != null)
        {
            args.putInt(STATE_NUM_ADDRESS, address.size());
            for (int index = 0; index < address.size(); ++index)
            {
                CheatAddressData seq = address.get(index);
                args.putLong(STATE_ADDRESS_ITEM + index, seq.address);
                args.putInt(STATE_ADDRESS_VALUE + index, seq.value);
            }
        }
        
        if(options != null)
        {
            args.putInt(STATE_NUM_OPTION_ITEMS, options.size());
            for (int index = 0; index < options.size(); ++index)
            {
                CheatOptionData seq = options.get(index);
                args.putString(STATE_OPTION_ITEMS_DESC + index, seq.description);
                args.putInt(STATE_OPTION_ITEMS_VALUE + index, seq.value);
            }
        }
        
        args.putInt(STATE_NUM_CHEAT_ITEMS, cheatNames.size());

        for (int index = 0; index < cheatNames.size(); ++index)
        {
            String seq = cheatNames.get(index);
            args.putString(STATE_ITEMS_CHEAT + index, seq);
        }

        frag.setArguments(args);
        return frag;
    }
    
    private void unpackFields()
    {
        mName = getArguments().getString(STATE_NAME);
        mComment = getArguments().getString(STATE_COMMENT);
        
        final int numAddressItems = getArguments().getInt(STATE_NUM_ADDRESS);
        mAddresses = new ArrayList<CheatAddressData>();

        //Fill the values
        for (int index = 0; index < numAddressItems; ++index)
        {
            long address = getArguments().getLong(STATE_ADDRESS_ITEM + index);
            int value = getArguments().getInt(STATE_ADDRESS_VALUE + index);
            
            CheatAddressData data = new CheatAddressData();
            data.address = address;
            data.value = value;
            mAddresses.add(data);
        }
        
        final int numOptionItems = getArguments().getInt(STATE_NUM_OPTION_ITEMS);
        mOptionItems = new ArrayList<CheatOptionData>();

        //Fill the values
        for (int index = 0; index < numOptionItems; ++index)
        {
            String desc = getArguments().getString(STATE_OPTION_ITEMS_DESC + index);
            int option = getArguments().getInt(STATE_OPTION_ITEMS_VALUE + index);
            
            CheatOptionData data = new CheatOptionData();
            data.description = desc;
            data.value = option;
            mOptionItems.add(data);
        }
        
        final int numCheatItems = getArguments().getInt(STATE_NUM_CHEAT_ITEMS);
        mCheatNameItems = new ArrayList<String>();

        //Fill the values
        for (int index = 0; index < numCheatItems; ++index)
        {
            String cheatName = getArguments().getString(STATE_ITEMS_CHEAT + index);
            
            mCheatNameItems.add(cheatName);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        setRetainInstance(true);
		
        final String title = getArguments().getString(STATE_TITLE);
        unpackFields();

        mDialogView = View.inflate(getActivity(), R.layout.cheat_edit_dialog, null);
        
        mEditName = (EditText) mDialogView.findViewById(R.id.textCheatTitle);
        mEditComment = (EditText) mDialogView.findViewById(R.id.textCheatNotes);
        mEditAddress = (EditText) mDialogView.findViewById(R.id.textCheatAddress);
        mEditValue = (EditText) mDialogView.findViewById(R.id.textCheatMainValue);
        mAddOptionButton = (Button) mDialogView.findViewById(R.id.addMoreCheatOptionsButton);
        mAddAddressButton = (Button) mDialogView.findViewById(R.id.addMoreCheatsButton);
        mOptionsLayoutHolder = (LinearLayout) mDialogView.findViewById(R.id.linearLayoutCheatOptionsHolder);
        
        mOptionValueFields = new ArrayList<EditText>();
        mCheatAddressFields = new ArrayList<EditText>();
        mCheatValueFields = new ArrayList<EditText>();
        
        TextWatcher fieldValidator = new TextWatcher()
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
                AlertDialog dialog = (AlertDialog) getDialog();
                
                if(dialog != null)
                {
                    Button okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    boolean isValid = validateFields();
                    okButton.setEnabled(isValid);
                    okButton.setTextColor(ContextCompat.getColor(getActivity(), isValid ? R.color.accent_material_dark : R.color.dim_foreground_disabled_material_dark));
                }

            }
        };
        
        mEditName.addTextChangedListener(fieldValidator);
        mEditAddress.addTextChangedListener(fieldValidator);
        mEditValue.addTextChangedListener(fieldValidator);
        
        setDefaultBehavior(fieldValidator);
        
        if(mName != null)
        {
            setValues(fieldValidator);
        }

        //Time to create the dialog
        Builder builder = new Builder(getActivity());
        builder.setTitle(title);

        // Create listener for OK/cancel button clicks
        OnClickListener clickListener = new OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                //Populate the options and submit to activity
                if (getActivity() instanceof OnEditCompleteListener)
                {
                    List<CheatAddressData> address = getAddressesFromView();
                    List<CheatOptionData> options = getOptionsFromView();

                    ((OnEditCompleteListener) getActivity()).onEditComplete( which,
                        mEditName.getText().toString(), mEditComment.getText().toString(),
                        address, options);
                }
                else
                {
                    Log.e("EditCheatDialog", "Activity doesn't implement OnEditCompleteListener");
                }
            }
        };

        builder.setView(mDialogView);
        builder.setPositiveButton(android.R.string.ok, clickListener);
        builder.setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = builder.create();

        /* Make the dialog resize to the keyboard */ 
        dialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return dialog;
    }
    
    private boolean validateFields()
    {
        final String invalidValue = getString(R.string.cheatEditor_invalid_value);
        
        boolean isTitleValid = isValidName(mCheatNameItems, mName, mEditName.getText().toString());
        boolean isAddressValid = mEditAddress.getText().toString().length() == 8;
        boolean isValueNA = mEditValue.getText().toString().equals(invalidValue);
        boolean isValueValid = isValueNA || mEditValue.getText().toString().length() == 4;
        int numValidOptions = 0;
        
        //For all options that are present, make sure all values have 4 characters.
        for(EditText optionValue : mOptionValueFields)
        {
            boolean isCurrentEditTextValid = optionValue.getText().toString().length() == 4;
            
            if(isCurrentEditTextValid)
            {
                ++numValidOptions;
            }
        }
        
        //Make sure all addresses are valid
        int numValidAddresses = 0;
        for(EditText address : mCheatAddressFields)
        {
            boolean isCurrentEditTextValid = address.getText().toString().length() == 8;
            
            if(isCurrentEditTextValid)
            {
                ++numValidAddresses;
            }
        }

        //Make sure all values are valid
        int numValidValues = 0;
        for(EditText value : mCheatValueFields)
        {
            boolean isCurrentEditTextValid = value.getText().toString().length() == 4;
            
            if(isCurrentEditTextValid)
            {
                ++numValidValues;
            }
        }
        
        return isTitleValid && isAddressValid && isValueValid &&
            numValidOptions == mOptionValueFields.size() &&
            numValidAddresses == mCheatAddressFields.size() &&
            numValidValues == mCheatValueFields.size();
    }
    
    @Override
    public void onStart()
    {
        super.onStart();

        // Dynamically disable the OK after field validation
        AlertDialog dialog = (AlertDialog) getDialog();
        
        if(dialog != null)
        {
            Button okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);                
            okButton.setEnabled(validateFields());
        }
    }
    
    /**
     * Default behavior
     */
    private void setDefaultBehavior(final TextWatcher fieldValidator)
    {
        final String invalidValue = getString(R.string.cheatEditor_invalid_value);
        
        //Add an option
        mAddOptionButton.setOnClickListener(new View.OnClickListener ()
        {
            @Override
            public void onClick(View v)
            {
                final View optionLayout = View.inflate( getActivity(), R.layout.cheat_edit_dialog_options, null );
                mOptionsLayoutHolder.addView(optionLayout);
                
                //Disable this field
                mEditValue.setEnabled(false);
                mEditValue.setText(invalidValue);
                
                ImageButton cheatOptionDelete = (ImageButton) optionLayout.findViewById(R.id.removeCheatOptionButton);
                cheatOptionDelete.setOnClickListener(getDeleteOptionOnClickListener(optionLayout));
                
                EditText cheatValueText = (EditText) optionLayout.findViewById(R.id.textCheatValue);
                cheatValueText.addTextChangedListener(fieldValidator);
                
                mOptionValueFields.add(cheatValueText);
                
                validateFields();
            }
        });
        
        mAddAddressButton.setOnClickListener(new View.OnClickListener ()
        {
            @Override
            public void onClick(View v)
            {
                final View moreCheatsLayout = View.inflate( getActivity(), R.layout.cheat_edit_dialog_cheats, null );
                mOptionsLayoutHolder.addView(moreCheatsLayout);
                
                ImageButton cheatDelete = (ImageButton) moreCheatsLayout.findViewById(R.id.removeCheatOptionButton);
                cheatDelete.setOnClickListener(getDeleteCheatOnClickListener(moreCheatsLayout));
                
                EditText cheatAddressText = (EditText) moreCheatsLayout.findViewById(R.id.textCheatExtraAddress);
                cheatAddressText.addTextChangedListener(fieldValidator);
                EditText cheatValueText = (EditText) moreCheatsLayout.findViewById(R.id.textCheatExtraValue);
                cheatValueText.addTextChangedListener(fieldValidator);

                mCheatAddressFields.add(cheatAddressText);
                mCheatValueFields.add(cheatValueText);
                
                validateFields();
            }
        });
    }
    
    /**
     * Sets current values
     */
    private void setValues(final TextWatcher fieldValidator)
    {
        final String invalidValue = getString(R.string.cheatEditor_invalid_value);

        mEditName.setText(mName);
        mEditComment.setText(mComment);

        if (!mAddresses.isEmpty())
        {
            //Fill in addresses
            String adressString = String.format("%08X", mAddresses.get(0).address);
            mEditAddress.setText(adressString);
            
            String valueString = String.format("%04X", mAddresses.get(0).value);
            mEditValue.setText(valueString);

            for (int index = 1; index < mAddresses.size(); ++index)
            {
                final View optionLayout = View.inflate(getActivity(), R.layout.cheat_edit_dialog_cheats, null);
                EditText cheatAddressText = (EditText) optionLayout.findViewById(R.id.textCheatExtraAddress);
                EditText cheatValueText = (EditText) optionLayout.findViewById(R.id.textCheatExtraValue);
                ImageButton cheatOptionDelete = (ImageButton) optionLayout.findViewById(R.id.removeCheatOptionButton);

                String cheatAddressString = String.format("%08X", mAddresses.get(index).address);
                String cheatValueString = String.format("%04X", mAddresses.get(index).value);

                cheatAddressText.setText(cheatAddressString);
                cheatValueText.setText(cheatValueString);
                cheatOptionDelete.setOnClickListener(getDeleteCheatOnClickListener(optionLayout));

                cheatValueText.addTextChangedListener(fieldValidator);

                mOptionsLayoutHolder.addView(optionLayout);

                mCheatAddressFields.add(cheatAddressText);
                mCheatValueFields.add(cheatValueText);
            }
            
            //Disable the first value field if there are any options
            if(!mOptionItems.isEmpty())
            {
                // Disable this field
                mEditValue.setEnabled(false);
                mEditValue.setText(invalidValue);
            }

            //Fill in the options
            for (CheatOptionData data : mOptionItems)
            {
                final View optionLayout = View.inflate(getActivity(), R.layout.cheat_edit_dialog_options, null);
                EditText cheatValueText = (EditText) optionLayout.findViewById(R.id.textCheatValue);
                EditText cheatValueDescription = (EditText) optionLayout.findViewById(R.id.textCheatValueDescription);
                ImageButton cheatOptionDelete = (ImageButton) optionLayout.findViewById(R.id.removeCheatOptionButton);

                String optionValueString = String.format("%04X", data.value);

                cheatValueText.setText(optionValueString);
                cheatValueDescription.setText(data.description);
                cheatOptionDelete.setOnClickListener(getDeleteOptionOnClickListener(optionLayout));

                cheatValueText.addTextChangedListener(fieldValidator);

                mOptionsLayoutHolder.addView(optionLayout);
                mOptionValueFields.add(cheatValueText);
            }
        }
    }
    
    /**
     * Default behavior when deleting an option
     * @param viewToRemove view to remove when an option is being deleted
     * @return OnClickListener that takes the correct action
     */
    View.OnClickListener getDeleteOptionOnClickListener(final View viewToRemove)
    {
        View.OnClickListener listener = new View.OnClickListener ()
        {
            //Remove an option
            @Override
            public void onClick(View v)
            {
                mOptionsLayoutHolder.removeView(viewToRemove);
                
                EditText cheatValueText = (EditText) viewToRemove.findViewById(R.id.textCheatValue);
                mOptionValueFields.remove(cheatValueText);
                
                if(mOptionValueFields.size() == 0)
                {
                    mEditValue.setEnabled(true);
                    mEditValue.setText("");
                }
                
                validateFields();
            }
        };
        
        return listener;
    }
    
    /**
     * Default behavior when deleting a cheat
     * @param viewToRemove view to remove when an option is being deleted
     * @return OnClickListener that takes the correct action
     */
    View.OnClickListener getDeleteCheatOnClickListener(final View viewToRemove)
    {
        View.OnClickListener listener = new View.OnClickListener ()
        {
            //Remove an option
            @Override
            public void onClick(View v)
            {
                mOptionsLayoutHolder.removeView(viewToRemove);
                
                EditText cheatValueText = (EditText) viewToRemove.findViewById(R.id.textCheatExtraValue);
                EditText cheatAddressText = (EditText) viewToRemove.findViewById(R.id.textCheatExtraAddress);

                mCheatAddressFields.remove(cheatAddressText);
                mCheatValueFields.remove(cheatValueText);
                
                validateFields();
            }
        };
        
        return listener;
    }
    
    List<CheatOptionData> getOptionsFromView()
    {
        List<CheatOptionData> options = new ArrayList<CheatOptionData>();

        for (int index = 0; index < mOptionsLayoutHolder.getChildCount(); ++index)
        {
            View child = mOptionsLayoutHolder.getChildAt(index);
            EditText cheatValueText = (EditText) child.findViewById(R.id.textCheatValue);
            EditText cheatValueDescription = (EditText) child.findViewById(R.id.textCheatValueDescription);

            //If these are nulls then that means that this layout must be an option instead of an address
            if(cheatValueText != null && cheatValueDescription != null)
            {
                CheatOptionData data = new CheatOptionData();
                String valueString = cheatValueText.getText().toString();
                if (!valueString.isEmpty())
                {
                    data.value = Integer.valueOf(valueString, 16);
                    data.description = cheatValueDescription.getText().toString();
                    options.add(data);
                }
            }
        }

        return options;
    }
    
    List<CheatAddressData> getAddressesFromView()
    {
        final String invalidValue = getString(R.string.cheatEditor_invalid_value);
        List<CheatAddressData> allAddressData = new ArrayList<CheatAddressData>();
        
        String addressString = mEditAddress.getText().toString();
        String valueString = mEditValue.getText().toString();
        
        if(!addressString.isEmpty() && !valueString.isEmpty())
        {
            CheatAddressData data = new CheatAddressData();
            data.address = Long.valueOf(addressString, 16);
            
            if(!valueString.equals(invalidValue))
            {
                data.value = Integer.valueOf(valueString, 16);
            }
            else
            {
                data.value = -1;
            }
            allAddressData.add(data);
        }
        
        for(int index = 0; index < mOptionsLayoutHolder.getChildCount(); ++index)
        {
            View child = mOptionsLayoutHolder.getChildAt(index);
            EditText cheatAddressText = (EditText) child.findViewById(R.id.textCheatExtraAddress);
            EditText cheatValueText = (EditText) child.findViewById(R.id.textCheatExtraValue);

            //If these are null, then this must be an address layout
            if(cheatAddressText != null && cheatValueText != null)
            {
                addressString = cheatAddressText.getText().toString();
                valueString = cheatValueText.getText().toString();
                
                if(!addressString.isEmpty() && !valueString.isEmpty())
                {
                    CheatAddressData data = new CheatAddressData();
                    data.address = Long.valueOf(addressString, 16);
                    data.value = Integer.valueOf(valueString, 16);
                    allAddressData.add(data);
                }
            }
        }

        return allAddressData;
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
     * @return true if cheat name is safe to use
     */
    private boolean isValidName(List<String> cheatNames, String oldName, String newName)
    {
        boolean isNotEmpty = !TextUtils.isEmpty(newName);
        boolean isLegal = !Pattern.matches(".*[\\[\\]].*", newName);
        boolean isSameName = false;
        
        if (!TextUtils.isEmpty(oldName))
        {
            isSameName = oldName.equals(newName);
        }
        boolean isUnique = !cheatNames.contains(newName) || isSameName;

        return isNotEmpty && isLegal && isUnique;
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
