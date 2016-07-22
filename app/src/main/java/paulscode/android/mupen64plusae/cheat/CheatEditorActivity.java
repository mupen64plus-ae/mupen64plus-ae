/**
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
package paulscode.android.mupen64plusae.cheat;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
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

import org.mupen64plusae.v3.fzurita.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.MenuListView;
import paulscode.android.mupen64plusae.cheat.CheatUtils.Cheat;
import paulscode.android.mupen64plusae.compat.AppCompatListActivity;
import paulscode.android.mupen64plusae.dialog.EditCheatAdvancedDialog;
import paulscode.android.mupen64plusae.dialog.EditCheatAdvancedDialog.OnAdvancedEditCompleteListener;
import paulscode.android.mupen64plusae.dialog.EditCheatDialog;
import paulscode.android.mupen64plusae.dialog.EditCheatDialog.OnEditCompleteListener;
import paulscode.android.mupen64plusae.dialog.MenuDialogFragment;
import paulscode.android.mupen64plusae.dialog.MenuDialogFragment.OnDialogMenuItemSelectedListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.task.ExtractCheatsTask;
import paulscode.android.mupen64plusae.task.ExtractCheatsTask.ExtractCheatListener;
import paulscode.android.mupen64plusae.util.FileUtil;

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
        
        public CheatListAdapter( Context context, List<Cheat> cheats )
        {
            super( context, RESID, cheats );
        }
        
        @Override
        public View getView( int position, View convertView, ViewGroup parent )
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
                TextView text1 = (TextView) view.findViewById( R.id.text1 );
                TextView text2 = (TextView) view.findViewById( R.id.text2 );
                ImageView icon = (ImageView) view.findViewById( R.id.icon );
                
                text1.setText( item.name );
                text2.setText( item.desc );
                icon.setImageResource( R.drawable.ic_key );
            }
            return view;
        }
    }
    
    private static final String STATE_MENU_DIALOG_FRAGMENT = "STATE_MENU_DIALOG_FRAGMENT";
    private static final String STATE_CHEAT_EDIT_DIALOG_FRAGMENT = "STATE_CHEAT_EDIT_DIALOG_FRAGMENT";
    
    private final ArrayList<Cheat> userCheats = new ArrayList<Cheat>();
    private final ArrayList<Cheat> systemCheats = new ArrayList<Cheat>();
    private CheatListAdapter cheatListAdapter = null;
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private String mRomCrc = null;
    private String mRomHeaderName = null;
    private byte mRomCountryCode = 0;
    private int mSelectedCheat = 0;
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        mGlobalPrefs.enforceLocale( this );
        
        // Get the ROM header info
        Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "ROM path must be passed via the extras bundle when starting CheatEditorActivity" );
        
        mRomCrc = extras.getString( ActivityHelper.Keys.ROM_CRC );
        mRomHeaderName = extras.getString( ActivityHelper.Keys.ROM_HEADER_NAME );
        mRomCountryCode = extras.getByte( ActivityHelper.Keys.ROM_COUNTRY_CODE );
        
        setContentView( R.layout.cheat_editor );
        reload( mRomCrc, mRomCountryCode );
        findViewById( R.id.imgBtnChtAdd ).setOnClickListener( new View.OnClickListener()
        {
            
            @Override
            public void onClick(View v)
            {
                //Add a cheat
                mSelectedCheat = -1;
                int stringId = R.string.cheatEditor_edit1;
                EditCheatDialog editCheatDialogFragment = EditCheatDialog.newInstance(getString(stringId), null, null,
                    null, null, getCheatTitles());

                FragmentManager fm = getSupportFragmentManager();
                editCheatDialogFragment.show(fm, STATE_CHEAT_EDIT_DIALOG_FRAGMENT);                
            }
        } );
        findViewById( R.id.imgBtnChtAddAvanced ).setOnClickListener( new View.OnClickListener()
        {
            
            @Override
            public void onClick(View v)
            {
                //Add a cheat
                mSelectedCheat = -1;
                int stringId = R.string.cheatEditor_edit2;
                EditCheatAdvancedDialog editCheatDialogFragment = EditCheatAdvancedDialog.newInstance(getString(stringId), null, null,
                    null, null, getCheatTitles());

                FragmentManager fm = getSupportFragmentManager();
                editCheatDialogFragment.show(fm, STATE_CHEAT_EDIT_DIALOG_FRAGMENT);                
            }
        } );
        
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
        cheatsTask.execute((String) null);
        
        //We don't extract user cheats in a separate task since there aren't as many
        CheatFile usrcheat_txt = new CheatFile( mGlobalPrefs.customCheats_txt, true );
        userCheats.clear();        
        userCheats.addAll( CheatUtils.populate( mRomCrc, mRomCountryCode, usrcheat_txt, false, this ) );
        
        cheatListAdapter = new CheatListAdapter( this, userCheats );
        setListAdapter( cheatListAdapter );
    }
    
    @Override
    public void onExtractFinished(ArrayList<Cheat> moreCheats)
    {
        systemCheats.clear();
        systemCheats.addAll( moreCheats );
    }    
    
    private void save( String crc )
    {
        ArrayList<Cheat> combinedCheats;
        combinedCheats = new ArrayList<Cheat>();
        combinedCheats.addAll(systemCheats);
        combinedCheats.addAll(userCheats);
        Collections.sort(combinedCheats);

        FileUtil.makeDirs(new File( mGlobalPrefs.customCheats_txt ).getParentFile().getPath());
        
        CheatFile usrcheat_txt = new CheatFile( mGlobalPrefs.customCheats_txt, true );
        CheatFile mupencheat_txt = new CheatFile( mAppData.mupencheat_txt, true );
        CheatUtils.save( crc, usrcheat_txt, userCheats, mRomHeaderName, mRomCountryCode, this, false );
        CheatUtils.save( crc, mupencheat_txt, combinedCheats, mRomHeaderName, mRomCountryCode, this, true );
        
        setResult(RESULT_OK, null);
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
        switch( item.getItemId() )
        {
            case R.id.menuItem_edit:
                CreateCheatEditorDialog(false);
                break;
            case R.id.menuItem_advaned_edit:
                CreateCheatEditorDialog(true);
                break;
            case R.id.menuItem_delete:
                promptDelete(mSelectedCheat);
                break;
            default:
                return;
        }
    }
    
    private void CreateCheatEditorDialog(boolean advanced)
    {
        int stringId = R.string.cheatEditor_edit1;
        final Cheat cheat = userCheats.get( mSelectedCheat );
        ArrayList<CheatAddressData> addressList = new ArrayList<CheatAddressData>();
        ArrayList<CheatOptionData> optionsList = new ArrayList<CheatOptionData>();
        String addresses = cheat.code;
        String options = cheat.option;
        
        //Convert address string to a list of addresses
        if( !TextUtils.isEmpty( addresses ) )
        {
            String[] addressStrings = null;
            addressStrings = addresses.split("\n");
            
            for(String address : addressStrings)
            {
                CheatAddressData addressData = new CheatAddressData();
                
                String addressString = address.substring(0, 8);
                String valueString = address.substring(address.length()-4, address.length());

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
            String[] optionStrings = null;
            optionStrings = options.split( "\n" );
            
            for(String option : optionStrings)
            {
                CheatOptionData cheatData = new CheatOptionData();
                String valueString = option.substring(option.length()-4, option.length());
                cheatData.value = Integer.valueOf(valueString, 16);
                cheatData.description = option.substring(0, option.length() - 5);
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
        List<String> cheatTitles = new ArrayList<String>();
        
        for(Cheat cheat: userCheats)
        {
            cheatTitles.add(cheat.name);
        }
        return cheatTitles;
    }

    private void promptDelete( final int pos )
    {
        final OnClickListener listener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    userCheats.remove( pos );
                    cheatListAdapter.notifyDataSetChanged();
                    
                    save( mRomCrc );
                }
            }
        };            
        Builder builder = new Builder( this );
        builder.setTitle( R.string.cheatEditor_delete );
        builder.setMessage( R.string.cheatEditor_confirm );
        builder.setPositiveButton( android.R.string.yes, listener );
        builder.setNegativeButton( android.R.string.no, listener );
        builder.create().show();
    }
 
    @Override
    public void onEditComplete(int selectedButton, String name, String comment, List<CheatAddressData> address,
        List<CheatOptionData> options)
    {        
        if( selectedButton == DialogInterface.BUTTON_POSITIVE )
        {
            Cheat cheat = null;
            
            if (mSelectedCheat != -1)
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
            
            String optionAddressString = new String();
            
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