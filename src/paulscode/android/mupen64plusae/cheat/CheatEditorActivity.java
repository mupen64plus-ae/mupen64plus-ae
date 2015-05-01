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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.Keys;
import paulscode.android.mupen64plusae.cheat.CheatUtils.Cheat;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptTextListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.RomHeader;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CheatEditorActivity extends ListActivity implements View.OnClickListener, OnItemLongClickListener
{
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
    
    private final ArrayList<Cheat> cheats = new ArrayList<Cheat>();
    private CheatListAdapter cheatListAdapter = null;
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private RomHeader mRomHeader = null;
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this );
        mGlobalPrefs.enforceLocale( this );
        
        // Get the ROM header info
        Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "ROM path must be passed via the extras bundle when starting CheatEditorActivity" );
        String romPath = extras.getString( Keys.Extras.ROM_PATH );
        if( TextUtils.isEmpty( romPath ) )
            throw new Error( "ROM path must be passed via the extras bundle when starting CheatEditorActivity" );
        mRomHeader = new RomHeader( new File( romPath ) );
        
        setContentView( R.layout.cheat_editor );
        reload( mRomHeader.crc );
        findViewById( R.id.imgBtnChtAdd ).setOnClickListener( this );
        findViewById( R.id.imgBtnChtEdit ).setOnClickListener( this );
        findViewById( R.id.imgBtnChtSave ).setOnClickListener( this );
        findViewById( R.id.imgBtnChtInfo ).setOnClickListener( this );
        getListView().setOnItemLongClickListener( this );
    }
    
    private void reload( String crc )
    {
        Log.v( "CheatEditorActivity", "building from CRC = " + crc );
        
        if( crc == null )
            return;
        
        // Get the appropriate section of the config file, using CRC as the key
        CheatFile mupencheat_default = new CheatFile( mAppData.mupencheat_default );
        CheatFile usrcheat_txt = new CheatFile( mGlobalPrefs.customCheats_txt );
        cheats.addAll( CheatUtils.populate( crc, mupencheat_default, true, this ) );
        cheats.addAll( CheatUtils.populate( crc, usrcheat_txt, false, this ) );
        cheatListAdapter = new CheatListAdapter( this, cheats );
        setListAdapter( cheatListAdapter );
    }
    
    private void save( String crc )
    {
        CheatFile usrcheat_txt = new CheatFile( mGlobalPrefs.customCheats_txt );
        CheatFile mupencheat_txt = new CheatFile( mAppData.mupencheat_txt );
        CheatUtils.save( crc, usrcheat_txt, cheats, mRomHeader, this, false );
        CheatUtils.save( crc, mupencheat_txt, cheats, mRomHeader, this, true );
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
    
    @Override
    protected void onListItemClick( ListView l, View v, final int position, long id )
    {
        Cheat cheat = cheats.get( position );
        StringBuilder message = new StringBuilder();
        message.append( getString( R.string.cheatEditor_title2 ) + "\n" );
        message.append( cheat.name + "\n" );
        message.append( getString( R.string.cheatEditor_notes2 ) + "\n" );
        message.append( cheat.desc + "\n" );
        message.append( getString( R.string.cheatEditor_code2 ) + "\n" );
        message.append( cheat.code );
        if( !TextUtils.isEmpty( cheat.option ) && cheat.code.contains( "?" ) )
        {
            message.append( "\n" + getString( R.string.cheatEditor_option2 ) );
            message.append( "\n" + cheat.option );
        }
        
        Builder builder = new Builder( this );
        builder.setTitle( R.string.cheatEditor_info );
        builder.setMessage( message.toString() );
        builder.create().show();
    }
    
    @Override
    public void onClick( View v )
    {
        AlertDialog alertDialog;
        
        switch( v.getId() )
        {
            case R.id.imgBtnChtAdd:
                Cheat cheat = new Cheat();
                cheat.name = getString( R.string.cheatEditor_empty );
                cheat.desc = getString( R.string.cheatNotes_none );
                cheat.code = "";
                cheat.option = "";
                cheats.add( cheat );
                cheatListAdapter = new CheatListAdapter( CheatEditorActivity.this, cheats );
                setListAdapter( cheatListAdapter );
                Toast t = Toast.makeText( CheatEditorActivity.this, getString( R.string.cheatEditor_added ), Toast.LENGTH_SHORT );
                t.show();
                break;
                
            case R.id.imgBtnChtEdit:
                alertDialog = new AlertDialog.Builder( CheatEditorActivity.this ).create();
                alertDialog.setTitle( getString( R.string.cheatEditor_edit ) );
                alertDialog.setMessage( getString( R.string.cheatEditor_edit_desc ) );
                alertDialog.show();
                break;
            
            case R.id.imgBtnChtSave:
                save( mRomHeader.crc );
                CheatUtils.reset();
                CheatEditorActivity.this.finish();
                break;
                
            case R.id.imgBtnChtInfo:
                StringBuilder message = new StringBuilder();
                message.append( getString( R.string.cheatEditor_readme1 ) + "\n\n" );
                message.append( getString( R.string.cheatEditor_readme2 ) + "\n\n" );
                message.append( getString( R.string.cheatEditor_readme3 ) + "\n\n" );
                message.append( getString( R.string.cheatEditor_readme4 ) + "\n\n" );
                message.append( getString( R.string.cheatEditor_readme5 ) + "\n\n" );
                message.append( getString( R.string.cheatEditor_readme6 ) );
                
                alertDialog = new AlertDialog.Builder( CheatEditorActivity.this ).create();
                alertDialog.setTitle( getString( R.string.cheatEditor_help ) );
                alertDialog.setMessage( message.toString() );
                alertDialog.show();
                break;
        }
    }

    @SuppressLint( "InflateParams" )
    @Override
    public boolean onItemLongClick( AdapterView<?> av, View v, final int pos, long id )
    {
        final Cheat cheat = cheats.get( pos );
        
        // Inflate the long-click dialog
        LayoutInflater inflater = (LayoutInflater) getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View ll = inflater.inflate( R.layout.cheat_editor_longclick_dialog, null );
        
        // Build the alert dialog
        Builder builder = new Builder( this );
        builder.setTitle( R.string.cheatEditor_config  );
        builder.setMessage( R.string.cheatEditor_config_desc );
        builder.setView( ll );
        final AlertDialog parentDialog = builder.create();
        
        // Define the handler for button clicks
        final View.OnClickListener listener = new View.OnClickListener()
        {
            @Override
            public void onClick( View v )
            {
                parentDialog.dismiss();
                switch( v.getId() )
                {
                    case R.id.btnEditTitle:
                        promptTitle( cheat );
                        break;
                    case R.id.btnEditNotes:
                        promptNotes( cheat );
                        break;
                    case R.id.btnEditCode:
                        promptCode( cheat );
                        break;
                    case R.id.btnEditOption:
                        promptOption( cheat );
                        break;
                    case R.id.btnDelete:
                        promptDelete( pos );
                        break;
                }
            }
        };
        
        // Assign the button click handler
        ll.findViewById( R.id.btnEditTitle ).setOnClickListener( listener );
        ll.findViewById( R.id.btnEditNotes ).setOnClickListener( listener );
        ll.findViewById( R.id.btnEditCode ).setOnClickListener( listener );
        ll.findViewById( R.id.btnEditOption ).setOnClickListener( listener );
        ll.findViewById( R.id.btnDelete ).setOnClickListener( listener );
        if( pos < CheatUtils.numberOfSystemCheats )
        {
            ll.findViewById( R.id.btnDelete ).setEnabled( false );
        }
        
        // Hide the edit option button if not applicable
        if( !cheats.get( pos ).code.contains( "?" ) )
            ll.findViewById( R.id.btnEditOption ).setVisibility( View.GONE );
        
        // Show the long-click dialog
        parentDialog.show();
        return true;
    }

    @Override
    // onBackPressed could probably be used
    public boolean onKeyDown( int KeyCode, KeyEvent event )
    {
        if( KeyCode == KeyEvent.KEYCODE_BACK )
        {
            final OnClickListener listener = new OnClickListener()
            {
                @Override
                public void onClick( DialogInterface dialog, int which )
                {
                    if( which == DialogInterface.BUTTON_POSITIVE )
                    {
                        save( mRomHeader.crc );
                    }
                    CheatUtils.reset();
                    CheatEditorActivity.this.finish();
                }
            };            
            Builder builder = new Builder( this );
            builder.setTitle( R.string.cheatEditor_saveConfirm );
            builder.setPositiveButton( android.R.string.yes, listener );
            builder.setNegativeButton( android.R.string.no, listener );
            builder.create().show();
            return true;
        }
        return super.onKeyDown( KeyCode, event );
    }

    private void promptTitle( final Cheat cheat )
    {
        CharSequence title = getText( R.string.cheatEditor_title );
        CharSequence message = getText( R.string.cheatEditor_title_desc );
        int inputType = InputType.TYPE_CLASS_TEXT;
        Prompt.promptText( this, title, message, cheat.name, null, inputType, new PromptTextListener()
        {
            @Override
            public void onDialogClosed( CharSequence text, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    String str = text.toString().replace( '\n', ' ' );
                    cheat.name = str;
                    cheatListAdapter = new CheatListAdapter( CheatEditorActivity.this, cheats );
                    setListAdapter( cheatListAdapter );
                }
            }
        } );
    }

    private void promptNotes( final Cheat cheat )
    {
        CharSequence title = getText( R.string.cheatEditor_notes );
        CharSequence message = getText( R.string.cheatEditor_notes_desc );
        CharSequence text;
        if( TextUtils.isEmpty( cheat.desc ) || cheat.desc.equals( getString( R.string.cheatNotes_none ) ) )
        {
            text = "";
        }
        else
        {
            text = cheat.desc;
        }
        int inputType = InputType.TYPE_CLASS_TEXT;
        Prompt.promptText( this, title, message, text, null, inputType, new PromptTextListener()
        {
            @Override
            public void onDialogClosed( CharSequence text, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    String str = text.toString().replace( '\n', ' ' );
                    if( TextUtils.isEmpty( str ) )
                    {
                        str = getString( R.string.cheatNotes_none );
                    }
                    cheat.desc = str;
                }
            }
        } );
    }

    private void promptCode( final Cheat cheat )
    {
        CharSequence title = getText( R.string.cheatEditor_code );
        CharSequence message = getText( R.string.cheatEditor_code_desc );
        int inputType = InputType.TYPE_CLASS_TEXT;
        Prompt.promptText( this, title, message, cheat.code, null, inputType, new PromptTextListener()
        {
            @Override
            public void onDialogClosed( CharSequence text, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    String verify = text.toString();
                    String[] split = verify.split( "\n" );
                    boolean bad = false;
                    for( int o = 0; o < split.length; o++ )
                    {
                        if( split[o].length() != 13 )
                        {
                            bad = true;
                            break;
                        }
                        if( split[o].indexOf( ' ' ) != -1 )
                        {
                            if( !isHexNumber( split[o].substring( 0, split[o].indexOf( ' ' ) ) ) )
                            {
                                bad = true;
                                break;
                            }
                            if( !isHexNumber( split[o].substring( split[o].indexOf( ' ' ) + 1 ) ) && !split[o].substring( split[o].indexOf( ' ' ) + 1 ).equals( "????" ) )
                            {
                                bad = true;
                                break;
                            }
                        }
                        else
                        {
                            bad = true;
                            break;
                        }
                    }
                    if( !bad )
                    {
                        cheat.code = text.toString().toUpperCase( Locale.US );
                    }
                    else
                    {
                        Toast t = Toast.makeText( CheatEditorActivity.this, getString( R.string.cheatEditor_badCode ), Toast.LENGTH_SHORT );
                        t.show();
                    }
                }
            }
        } );
    }

    private void promptOption( final Cheat cheat )
    {
        CharSequence title = getText( R.string.cheatEditor_option );
        CharSequence message = getText( R.string.cheatEditor_option_desc );
        int inputType = InputType.TYPE_CLASS_TEXT;
        Prompt.promptText( this, title, message, cheat.option, null, inputType, new PromptTextListener()
        {
            @Override
            public void onDialogClosed( CharSequence text, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    String verify = text.toString();
                    String[] split = verify.split( "\n" );
                    boolean bad = false;
                    verify = "";
                    for( int o = 0; o < split.length; o++ )
                    {
                        if( split[o].length() <= 5 )
                        {
                            bad = true;
                            break;
                        }
                        if( !isHexNumber( split[o].substring( split[o].length() - 4 ) ) )
                        {
                            bad = true;
                            break;
                        }
                        if( split[o].lastIndexOf( ' ' ) != split[o].length() - 5 )
                        {
                            bad = true;
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
                    if( !bad )
                    {
                        cheat.option = verify;
                    }
                    else
                    {
                        Toast t = Toast.makeText( CheatEditorActivity.this, getString( R.string.cheatEditor_badOption ), Toast.LENGTH_SHORT );
                        t.show();
                    }
                }
            }
        } );
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
                    cheats.remove( pos );
                    cheatListAdapter = new CheatListAdapter( CheatEditorActivity.this, cheats );
                    setListAdapter( cheatListAdapter );
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
}
