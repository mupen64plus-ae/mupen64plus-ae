/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: xperia64
 */
package emulator.android.mupen64plusae.cheat;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import emulator.android.mupen64plusae.ActivityHelper;
import emulator.android.mupen64plusae.MenuListView;
import emulator.android.mupen64plusae.cheat.CheatUtils.Cheat;
import emulator.android.mupen64plusae.compat.AppCompatListActivity;
import emulator.android.mupen64plusae.dialog.EditCheatAdvancedDialog;
import emulator.android.mupen64plusae.dialog.EditCheatAdvancedDialog.OnAdvancedEditCompleteListener;
import emulator.android.mupen64plusae.dialog.EditCheatDialog;
import emulator.android.mupen64plusae.dialog.EditCheatDialog.OnEditCompleteListener;
import emulator.android.mupen64plusae.dialog.MenuDialogFragment;
import emulator.android.mupen64plusae.dialog.MenuDialogFragment.OnDialogMenuItemSelectedListener;
import emulator.android.mupen64plusae.persistent.AppData;
import emulator.android.mupen64plusae.persistent.GlobalPrefs;
import emulator.android.mupen64plusae.task.ExtractCheatsTask;
import emulator.android.mupen64plusae.task.ExtractCheatsTask.ExtractCheatListener;
import emulator.android.mupen64plusae.util.FileUtil;
import emulator.android.mupen64plusae.util.LocaleContextWrapper;

public class CheatEditorActivity extends AppCompatListActivity implements ExtractCheatListener,
    OnDialogMenuItemSelectedListener, OnEditCompleteListener, OnAdvancedEditCompleteListener
{

    static public final class CheatOptionData
    {
        public String description;
        public int value;
    }
    
    static public final class CheatAddressData
    {
        public long address;
        public int value;
    }
    private static class CheatListAdapter extends ArrayAdapter<Cheat>
    {
        private static final int RESID = R.layout.list_item_two_text_icon;
        
        CheatListAdapter( Context context, List<Cheat> cheats )
        {
            super( context, RESID, cheats );
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
            
            Cheat item = getItem( position );
            if( item != null )
            {
                TextView text1 = view.findViewById( R.id.text1 );
                TextView text2 = view.findViewById( R.id.text2 );
                ImageView icon = view.findViewById( R.id.icon );
                
                text1.setText( item.name );
                text2.setText( item.desc );
                icon.setImageResource( R.drawable.ic_key );
            }
            return view;
        }
    }
    
    private static final String STATE_MENU_DIALOG_FRAGMENT = "STATE_MENU_DIALOG_FRAGMENT";
    private static final String STATE_CHEAT_EDIT_DIALOG_FRAGMENT = "STATE_CHEAT_EDIT_DIALOG_FRAGMENT";
    
    private final ArrayList<Cheat> userCheats = new ArrayList<>();
    private final ArrayList<Cheat> systemCheats = new ArrayList<>();
    private CheatListAdapter cheatListAdapter = null;
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private String mRomCrc = null;
    private String mRomHeaderName = null;
    private byte mRomCountryCode = 0;
    private int mSelectedCheat = 0;

    @Override
    protected void attachBaseContext(Context newBase) {
        if(TextUtils.isEmpty(LocaleContextWrapper.getLocalCode()))
        {
            super.attachBaseContext(newBase);
        }
        else
        {
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase,LocaleContextWrapper.getLocalCode()));
        }
    }
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        
        // Get the ROM header info
        Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "ROM path must be passed via the extras bundle when starting CheatEditorActivity" );
        
        mRomCrc = extras.getString( ActivityHelper.Keys.ROM_CRC );
        mRomHeaderName = extras.getString( ActivityHelper.Keys.ROM_HEADER_NAME );
        mRomCountryCode = extras.getByte( ActivityHelper.Keys.ROM_COUNTRY_CODE );

        Log.v( "CheatEditorActivity", "CRC = " + mRomCrc
                + ", header name = " + mRomHeaderName
                + ", country code = " + mRomCountryCode);
        
        setContentView( R.layout.cheat_editor );
        reload( mRomCrc, mRomCountryCode );
        findViewById( R.id.imgBtnChtAdd ).setOnClickListener(v -> {
            //Add a cheat
            mSelectedCheat = -1;
            int stringId = R.string.cheatEditor_edit1;
            EditCheatDialog editCheatDialogFragment = EditCheatDialog.newInstance(getString(stringId), null, null,
                null, null, getCheatTitles());

            FragmentManager fm = getSupportFragmentManager();
            editCheatDialogFragment.show(fm, STATE_CHEAT_EDIT_DIALOG_FRAGMENT);
        });
        findViewById( R.id.imgBtnChtAddAvanced ).setOnClickListener(v -> {
            //Add a cheat
            mSelectedCheat = -1;
            int stringId = R.string.cheatEditor_edit2;
            EditCheatAdvancedDialog editCheatDialogFragment = EditCheatAdvancedDialog.newInstance(getString(stringId), null, null,
                null, null, getCheatTitles());

            FragmentManager fm = getSupportFragmentManager();
            editCheatDialogFragment.show(fm, STATE_CHEAT_EDIT_DIALOG_FRAGMENT);
        });
        
        //default state is cancelled unless we save
        setResult(RESULT_CANCELED, null);
    }
    
    private void reload( String crc, byte countryCode )
    {
        Log.v( "CheatEditorActivity", "building from CRC = " + crc );
        
        if( crc == null )
            return;
        
        //Do this in a separate task since it takes longer
        ExtractCheatsTask cheatsTask = new ExtractCheatsTask(this, this, mAppData.mupencheat_default, crc, countryCode);
        cheatsTask.doInBackground();
        
        //We don't extract user cheats in a separate task since there aren't as many
        CheatFile usrcheat_txt = new CheatFile( mGlobalPrefs.customCheats_txt, true );
        userCheats.clear();

        ArrayList<Cheat> cheats = CheatUtils.populate( mRomCrc, mRomCountryCode, usrcheat_txt, this );
        Collections.sort(cheats);

        userCheats.addAll(cheats);
        
        cheatListAdapter = new CheatListAdapter( this, userCheats );
        setListAdapter( cheatListAdapter );
    }
    
    @Override
    public void onExtractFinished(ArrayList<Cheat> moreCheats)
    {
        runOnUiThread(() -> {
            systemCheats.clear();
            systemCheats.addAll( moreCheats );
        });
    }    
    
    private void save( String crc )
    {
        ArrayList<Cheat> combinedCheats;
        combinedCheats = new ArrayList<>();
        combinedCheats.addAll(systemCheats);
        combinedCheats.addAll(userCheats);
        Collections.sort(combinedCheats);

        File cheatsParent = new File( mGlobalPrefs.customCheats_txt ).getParentFile();

        if (cheatsParent != null) {
            FileUtil.makeDirs(cheatsParent.getPath());

            CheatFile usrcheat_txt = new CheatFile( mGlobalPrefs.customCheats_txt, true );
            CheatFile mupencheat_txt = new CheatFile( mAppData.mupencheat_txt, true );
            CheatUtils.save( crc, usrcheat_txt, userCheats, mRomHeaderName, mRomCountryCode, this, false );
            CheatUtils.save( crc, mupencheat_txt, combinedCheats, mRomHeaderName, mRomCountryCode, this, true );

            setResult(RESULT_OK, null);
        } else {
            setResult(RESULT_CANCELED, null);
        }
    }
    
    @Override
    protected void onListItemClick( ListView l, View v, final int position, long id )
    {
        int resId = R.menu.cheat_editor_activity;
        int stringId = R.string.touchscreenProfileActivity_menuTitle;
        mSelectedCheat = position; 
        
        MenuDialogFragment menuDialogFragment = MenuDialogFragment.newInstance(0,
           getString(stringId), resId);
        
        FragmentManager fm = getSupportFragmentManager();
        menuDialogFragment.show(fm, STATE_MENU_DIALOG_FRAGMENT);
    }
    
    @Override
    public void onPrepareMenuList(MenuListView listView)
    {
        //Nothing to do here
    }
    
    @Override
    public void onDialogMenuItemSelected( int dialogId, MenuItem item)
    {
        if (item.getItemId() == R.id.menuItem_edit) {
            CreateCheatEditorDialog(false);
        } else if (item.getItemId() == R.id.menuItem_advaned_edit) {
            CreateCheatEditorDialog(true);
        } else if (item.getItemId() == R.id.menuItem_delete) {
            promptDelete(mSelectedCheat);
        }
    }
    
    private void CreateCheatEditorDialog(boolean advanced)
    {
        int stringId = R.string.cheatEditor_edit1;
        final Cheat cheat = userCheats.get( mSelectedCheat );
        ArrayList<CheatAddressData> addressList = new ArrayList<>();
        ArrayList<CheatOptionData> optionsList = new ArrayList<>();
        String addresses = cheat.code;
        String options = cheat.option;
        
        //Convert address string to a list of addresses
        if( !TextUtils.isEmpty( addresses ) )
        {
            String[] addressStrings;
            addressStrings = addresses.split("\n");
            
            for(String address : addressStrings)
            {
                CheatAddressData addressData = new CheatAddressData();
                
                String addressString = address.substring(0, 8);
                String valueString = address.substring(address.length()-4);

                addressData.address = Long.valueOf(addressString, 16);
                if(!valueString.contains("?"))
                {
                    addressData.value = Integer.valueOf(valueString, 16);
                    addressList.add(addressData);
                }
                else
                {
                    //The cheat with the option goes at the front
                    addressData.value = -1;
                    addressList.add(0, addressData);
                }
            }

        }
        
        //Convert options into a list of options
        if( !TextUtils.isEmpty( options ) )
        {
            String[] optionStrings;
            optionStrings = options.split( "\n" );

            final int optionLength = 4;

            for(String option : optionStrings)
            {
                if (option.length() < optionLength + 1) {
                    continue;
                }

                CheatOptionData cheatData = new CheatOptionData();
                String valueString = option.substring(option.length() - optionLength);
                cheatData.value = Integer.valueOf(valueString, 16);
                cheatData.description = option.substring(0, option.length() - optionLength - 1);
                optionsList.add(cheatData);
            }
        }
        
        if(advanced)
        {
            EditCheatAdvancedDialog editCheatDialogFragment =
                EditCheatAdvancedDialog.newInstance(getString(stringId), cheat.name, cheat.desc,
                    addressList, optionsList, getCheatTitles());
            
            FragmentManager fm = getSupportFragmentManager();
            editCheatDialogFragment.show(fm, STATE_CHEAT_EDIT_DIALOG_FRAGMENT);
        }
        else
        {
            EditCheatDialog editCheatDialogFragment =
                EditCheatDialog.newInstance(getString(stringId), cheat.name, cheat.desc,
                    addressList, optionsList, getCheatTitles());
            
            FragmentManager fm = getSupportFragmentManager();
            editCheatDialogFragment.show(fm, STATE_CHEAT_EDIT_DIALOG_FRAGMENT);
        }
    }
    
    private List<String> getCheatTitles()
    {
        List<String> cheatTitles = new ArrayList<>();
        
        for(Cheat cheat: userCheats)
        {
            cheatTitles.add(cheat.name);
        }
        return cheatTitles;
    }

    private void promptDelete( final int pos )
    {
        final OnClickListener listener = (dialog, which) -> {
            if( which == DialogInterface.BUTTON_POSITIVE )
            {
                if (pos < userCheats.size()) {
                    userCheats.remove( pos );
                    cheatListAdapter.notifyDataSetChanged();

                    save( mRomCrc );
                }
            }
        };
        Builder builder = new Builder( this );
        builder.setTitle( R.string.cheatEditor_delete );
        builder.setMessage( R.string.cheatEditor_confirm );
        builder.setPositiveButton( android.R.string.ok, listener );
        builder.setNegativeButton( android.R.string.cancel, listener );
        builder.create().show();
    }
 
    @Override
    public void onEditComplete(int selectedButton, String name, String comment, List<CheatAddressData> address,
        List<CheatOptionData> options)
    {        
        if( selectedButton == DialogInterface.BUTTON_POSITIVE )
        {
            Cheat cheat;
            
            if (mSelectedCheat != -1 && mSelectedCheat < userCheats.size())
            {
                cheat = userCheats.get(mSelectedCheat);
            }
            else
            {
                cheat = new Cheat();
            }
            
            cheat.option = "";
            
            if(!name.isEmpty())
            {
                cheat.name = name;
            }

            cheat.desc = comment.replace( '\n', ' ' );
            
            String optionAddressString = "";
            
            //Build the codes
            StringBuilder builder = new StringBuilder();
            for(CheatAddressData data : address)
            {
                if(data.value != -1)
                {
                    builder.append(String.format("%08X %04X\n", data.address, data.value));
                }
                else
                {
                    optionAddressString = String.format("%08X ????\n", data.address);
                }
            }
            
            cheat.code = builder.toString() + optionAddressString;
            
            //Build the options
            builder = new StringBuilder();
            for (CheatOptionData data : options)
            {
                builder.append(String.format("%s %04X\n", data.description, data.value));
            }
            cheat.option = builder.toString();

            boolean ValidCheat = !name.isEmpty() && cheat.code.length() > 12;
            
            if(ValidCheat)
            {
                if(mSelectedCheat == -1 )
                {
                    userCheats.add(cheat);
                }

                Collections.sort(userCheats);
                save( mRomCrc );
                cheatListAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onAdvancedEditComplete(int selectedButton, String name, String comment, List<CheatAddressData> address,
        List<CheatOptionData> options)
    {
        onEditComplete(selectedButton, name, comment, address, options);        
    }
}