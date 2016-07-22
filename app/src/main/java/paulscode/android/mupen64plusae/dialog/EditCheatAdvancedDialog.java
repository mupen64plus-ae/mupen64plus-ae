package paulscode.android.mupen64plusae.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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


public class EditCheatAdvancedDialog extends DialogFragment
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
  
    private EditText mEditName = null;
    private EditText mEditComment = null;
    private EditText mEditCheat = null;
    private EditText mEditOption = null;

    private List<CheatAddressData> mAddresses = null;
    private List<CheatOptionData> mOptionItems = null;
    private List<String> mCheatNameItems = null;

    private View mDialogView = null;

    public interface OnAdvancedEditCompleteListener
    {
        /**
         * Called after cheat editing is complete
         * @param selectedButton The selected button.
         * @param name chosen cheat title
         * @param comment chosen cheat comment
         * @param address cheat address
         * @param options cheat options
         */
        public void onAdvancedEditComplete(int selectedButton, String name, String comment,
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
    public static EditCheatAdvancedDialog newInstance(String title, String name, String comment,
        List<CheatAddressData> address, List<CheatOptionData> options, List<String> cheatNames)
    {
        EditCheatAdvancedDialog frag = new EditCheatAdvancedDialog();
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

        mDialogView = View.inflate(getActivity(), R.layout.cheat_edit_advanced_dialog, null);
        
        mEditName = (EditText) mDialogView.findViewById(R.id.textCheatTitle);
        mEditComment = (EditText) mDialogView.findViewById(R.id.textCheatNotes);
        mEditCheat = (EditText) mDialogView.findViewById(R.id.textCheat);
        mEditOption = (EditText) mDialogView.findViewById(R.id.textOptions);
        
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
                    okButton.setTextColor(ContextCompat.getColor(getActivity(), isValid ? R.color.accent_material_dark :
                        R.color.dim_foreground_disabled_material_dark));
					
                }
            }
        };
        
        mEditName.addTextChangedListener(fieldValidator);
        mEditCheat.addTextChangedListener(fieldValidator);
        mEditOption.addTextChangedListener(fieldValidator);
                
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
                if (getActivity() instanceof OnAdvancedEditCompleteListener)
                {
                    populateCheatsFromText();
                    ((OnAdvancedEditCompleteListener) getActivity()).onAdvancedEditComplete( which,
                        mEditName.getText().toString(), mEditComment.getText().toString(),
                        mAddresses, mOptionItems);
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
    
    private void populateCheatsFromText()
    {
        //Clear everything first
        mAddresses.clear();
        mOptionItems.clear();
        
        //Convert address string to a list of addresses
        if( !TextUtils.isEmpty( mEditCheat.getText().toString() ) )
        {
            String[] addressStrings = null;
            addressStrings = mEditCheat.getText().toString().split("\n");
            
            for(String address : addressStrings)
            {
                if(!TextUtils.isEmpty(address))
                {
                    CheatAddressData addressData = new CheatAddressData();

                    String addressString = address.substring(0, 8);
                    String valueString = address.substring(address.length()-4, address.length());

                    addressData.address = Long.valueOf(addressString, 16);
                    if(!valueString.contains("?"))
                    {
                        addressData.value = Integer.valueOf(valueString, 16);
                        mAddresses.add(addressData);
                    }
                    else
                    {
                        //The cheat with the option goes at the front
                        addressData.value = -1;
                        mAddresses.add(0, addressData);
                    }
                }
            }
        }
        
        //Convert options into a list of options
        if( !TextUtils.isEmpty( mEditOption.getText().toString() ) )
        {
            String[] optionStrings = null;
            optionStrings = mEditOption.getText().toString().split( "\n" );
            
            for(String option : optionStrings)
            {
                if(!TextUtils.isEmpty(option))
                {
                    CheatOptionData cheatData = new CheatOptionData();
                    String valueString = option.substring(option.length()-4, option.length());
                    cheatData.value = Integer.valueOf(valueString, 16);
                    cheatData.description = option.substring(0, option.length() - 5);
                    mOptionItems.add(cheatData);
                }
            }
        }
    }
    
    private boolean isHexNumber( String num )
    {
        try
        {
            Long.parseLong( num, 16 );
            return true;
        }
        catch( NumberFormatException ex )
        {
            return false;
        }
    }
    
    private boolean validateFields()
    {
        boolean isTitleValid = isValidName(mCheatNameItems, mName, mEditName.getText().toString());
        boolean cheatValid = true;
        boolean optionValid = true;
        boolean hasOption = false;
        
        //Verify cheats
        String verify = mEditCheat.getText().toString();
        String[] split = verify.split("\n");
        for (int o = 0; o < split.length; o++)
        {
            if (split[o].length() != 13)
            {
                cheatValid = false;
                break;
            }
            if (split[o].indexOf(' ') != -1)
            {
                hasOption = split[o].substring(split[o].indexOf(' ') + 1).equals("????");
                
                if (!isHexNumber(split[o].substring(0, split[o].indexOf(' '))))
                {
                    cheatValid = false;
                    break;
                }
                if (!isHexNumber(split[o].substring(split[o].indexOf(' ') + 1)) && !hasOption)
                {
                    cheatValid = false;
                    break;
                }
            }
            else
            {
                cheatValid = false;
                break;
            }
        }
        
        //Verify options
        verify = mEditOption.getText().toString();
        
        if(!verify.isEmpty() || hasOption)
        {
            split = verify.split( "\n" );
            verify = "";
            for( int o = 0; o < split.length; o++ )
            {
                if( split[o].length() <= 5 )
                {
                    optionValid = false;
                    break;
                }
                if( !isHexNumber( split[o].substring( split[o].length() - 4 ) ) )
                {
                    optionValid = false;
                    break;
                }
                if( split[o].lastIndexOf( ' ' ) != split[o].length() - 5 )
                {
                    optionValid = false;
                    break;
                }
                split[o] = split[o].substring( 0, split[o].length() - 5 ) + " " + split[o].substring( split[o].length() - 4 ).toUpperCase( Locale.US );
                String y = "";
                if( o != split.length - 1 )
                {
                    y = "\n";
                }
                verify += split[o] + y;
            }
        }
        
        return isTitleValid && cheatValid && optionValid;
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
     * Sets current values
     */
    private void setValues(final TextWatcher fieldValidator)
    {
        mEditName.setText(mName);
        mEditComment.setText(mComment);

        if (!mAddresses.isEmpty())
        {
            StringBuilder builder = new StringBuilder();
            
            //Fill in addresses
            String optionAddressString = new String();
            for(CheatAddressData data : mAddresses)
            {
                if(data.value != -1)
                {
                    builder.append(String.format("%08X %04X\n", data.address, data.value));
                }
                else
                {
                    //Always place the address with the option last
                    optionAddressString = String.format("%08X ????\n", data.address);
                }
            }
            
            builder.append(optionAddressString);
            
            mEditCheat.setText(builder.toString());
            
            builder = new StringBuilder();
            
            //Fill in the options
            for (CheatOptionData data : mOptionItems)
            {
                builder.append(String.format("%s %04X\n", data.description, data.value));
            }
            
            mEditOption.setText(builder.toString());
        }
    }
    
    /**
     * Checks whether a candidate name is unique, non-empty, and contains only
     * safe characters. Unsafe characters are: '[', ']'.
     * 
     * @param cheatNames
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
