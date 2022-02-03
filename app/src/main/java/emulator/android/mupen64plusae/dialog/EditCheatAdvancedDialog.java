package emulator.android.mupen64plusae.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.mupen64plusae.v3.alpha.R;

import emulator.android.mupen64plusae.cheat.CheatEditorActivity.CheatAddressData;
import emulator.android.mupen64plusae.cheat.CheatEditorActivity.CheatOptionData;
import emulator.android.mupen64plusae.util.DisplayWrapper;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
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
        void onAdvancedEditComplete(int selectedButton, String name, String comment,
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
     * @return Dialog for editing advanced cheats
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
        if (getArguments() == null) {
            return;
        }

        mName = getArguments().getString(STATE_NAME);
        mComment = getArguments().getString(STATE_COMMENT);
        
        final int numAddressItems = getArguments().getInt(STATE_NUM_ADDRESS);
        mAddresses = new ArrayList<>();

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
        mOptionItems = new ArrayList<>();

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
        mCheatNameItems = new ArrayList<>();

        //Fill the values
        for (int index = 0; index < numCheatItems; ++index)
        {
            String cheatName = getArguments().getString(STATE_ITEMS_CHEAT + index);
            
            mCheatNameItems.add(cheatName);
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        setRetainInstance(true);

        String title;

        if (getArguments() == null)  {
            title = "";
        } else {
            title = getArguments().getString(STATE_TITLE);
        }

        unpackFields();

        View dialogView = View.inflate(getActivity(), R.layout.cheat_edit_advanced_dialog, null);
        
        mEditName = dialogView.findViewById(R.id.textCheatTitle);
        mEditComment = dialogView.findViewById(R.id.textCheatNotes);
        mEditCheat = dialogView.findViewById(R.id.textCheat);
        mEditOption = dialogView.findViewById(R.id.textOptions);
        
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
                    okButton.setTextColor(ContextCompat.getColor(requireActivity(), isValid ? R.color.accent_material_dark :
                        R.color.dim_foreground_disabled_material_dark));
					
                }
            }
        };
        
        mEditName.addTextChangedListener(fieldValidator);
        mEditCheat.addTextChangedListener(fieldValidator);
        mEditOption.addTextChangedListener(fieldValidator);
                
        if(mName != null)
        {
            setValues();
        }

        //Time to create the dialog
        Builder builder = new Builder(requireActivity());
        builder.setTitle(title);

        // Create listener for OK/cancel button clicks
        OnClickListener clickListener = (dialog, which) -> {
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
        };

        builder.setView(dialogView);
        builder.setPositiveButton(android.R.string.ok, clickListener);
        builder.setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = builder.create();
        DisplayWrapper.setDialogToResizeWithKeyboard(dialog, dialogView);

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
            String[] addressStrings;
            addressStrings = mEditCheat.getText().toString().split("\n");
            
            for(String address : addressStrings)
            {
                if(!TextUtils.isEmpty(address))
                {
                    try {
                        CheatAddressData addressData = new CheatAddressData();

                        String addressString = address.substring(0, 8);
                        String valueString = address.substring(address.length() - 4);

                        addressData.address = Long.valueOf(addressString, 16);
                        if (!valueString.contains("?")) {
                            addressData.value = Integer.valueOf(valueString, 16);
                            mAddresses.add(addressData);
                        } else {
                            //The cheat with the option goes at the front
                            addressData.value = -1;
                            mAddresses.add(0, addressData);
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        //Convert options into a list of options
        if( !TextUtils.isEmpty( mEditOption.getText().toString() ) )
        {
            String[] optionStrings;
            optionStrings = mEditOption.getText().toString().split( "\n" );
            
            for(String option : optionStrings)
            {
                if(!TextUtils.isEmpty(option))
                {
                    CheatOptionData cheatData = new CheatOptionData();
                    String valueString = option.substring(option.length()-4);
                    cheatData.value = Integer.valueOf(valueString, 16);
                    cheatData.description = option.substring(0, option.length() - 5);
                    mOptionItems.add(cheatData);
                }
            }
        }
    }
    
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isHexNumber(String num )
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
        StringBuilder verify = new StringBuilder(mEditCheat.getText().toString());
        String[] split = verify.toString().split("\n");
        for (String s : split) {
            if (s.length() != 13) {
                cheatValid = false;
                break;
            }
            if (s.indexOf(' ') != -1) {
                hasOption = s.substring(s.indexOf(' ') + 1).equals("????");

                if (!isHexNumber(s.substring(0, s.indexOf(' ')))) {
                    cheatValid = false;
                    break;
                }
                if (!isHexNumber(s.substring(s.indexOf(' ') + 1)) && !hasOption) {
                    cheatValid = false;
                    break;
                }
            } else {
                cheatValid = false;
                break;
            }
        }
        
        //Verify options
        verify = new StringBuilder(mEditOption.getText().toString());
        
        if((verify.length() > 0) || hasOption)
        {
            split = verify.toString().split( "\n" );
            verify = new StringBuilder();
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
                verify.append(split[o]).append(y);
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
    private void setValues()
    {
        mEditName.setText(mName);
        mEditComment.setText(mComment);

        if (!mAddresses.isEmpty())
        {
            StringBuilder builder = new StringBuilder();
            
            //Fill in addresses
            String optionAddressString = "";
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
}
