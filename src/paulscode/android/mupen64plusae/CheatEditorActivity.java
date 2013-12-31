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
package paulscode.android.mupen64plusae;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.CheatFile;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatBlock;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatCode;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatOption;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatSection;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.PromptTextListener;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class CheatEditorActivity extends ListActivity implements View.OnClickListener, OnItemLongClickListener
{
    
    private ArrayList<String> cheats_name = new ArrayList<String>();
    private ArrayList<String> cheats_desc = new ArrayList<String>();
    private ArrayList<String> cheats_code = new ArrayList<String>();
    private ArrayList<String> cheats_option = new ArrayList<String>();
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    private ArrayAdapter<String> cheatList = null;
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mAppData = new AppData( this );
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
        
        setContentView( R.layout.cheat_editor );
        reload( mUserPrefs.selectedGameHeader.crc );
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
        CheatFile mupencheat_txt = new CheatFile( mAppData.mupencheat_txt );
        CheatSection cheatSection = mupencheat_txt.match( "^" + crc.replace( ' ', '-' ) + ".*" );
        if( cheatSection == null )
        {
            Log.w( "CheatEditorActivity", "No cheat section found for '" + crc + "'" );
            return;
        }
        
        // Set the title of the menu to the game name, if available
        // String ROM_name = configSection.get( "Name" );
        // if( !TextUtils.isEmpty( ROM_name ) )
        // setTitle( ROM_name );
        
        // Layout the menu, populating it with appropriate cheat options
        CheatBlock cheat;
        for( int i = 0; i < cheatSection.size(); i++ )
        {
            cheat = cheatSection.get( i );
            if( cheat != null )
            {
                // Get the short title of the cheat (shown in the menu)
                String title;
                if( cheat.name == null )
                {
                    // Title not available, just use a default string for the menu
                    title = getString( R.string.cheats_defaultName, i );
                }
                
                else
                {
                    // Title available, remove the leading/trailing quotation marks
                    title = cheat.name;
                }
                cheats_name.add( title );
                // Get the descriptive note for this cheat (shown on long-click)
                final String notes = cheat.description;
                if( notes == null )
                {
                    cheats_desc.add( getString( R.string.cheatNotes_none ) );
                }
                else
                {
                    cheats_desc.add( notes );
                }
                // Get the options for this cheat
                LinkedList<CheatCode> codes = new LinkedList<CheatCode>();
                LinkedList<CheatOption> options = new LinkedList<CheatOption>();
                for( int o = 0; o < cheat.size(); o++ )
                {
                    codes.add( cheat.get( o ) );
                }
                for( int o = 0; o < codes.size(); o++ )
                {
                    if( codes.get( o ).options != null )
                    {
                        options = codes.get( o ).options;
                    }
                    
                }
                String codesAsString = "";
                if( codes != null )
                {
                    if( !codes.isEmpty() )
                    {
                        for( int o = 0; o < codes.size(); o++ )
                        {
                            String y = "";
                            if( o != codes.size() - 1 )
                            {
                                y = "\n";
                            }
                            codesAsString += codes.get( o ).address + " " + codes.get( o ).code + y;
                        }
                    }
                }
                cheats_code.add( codesAsString );
                String optionsAsString = "";
                if( options != null )
                {
                    if( !options.isEmpty() )
                    {
                        for( int o = 0; o < options.size(); o++ )
                        {
                            String y = "";
                            if( o != options.size() - 1 )
                            {
                                y = "\n";
                            }
                            optionsAsString += options.get( o ).name + " " + options.get( o ).code + y;
                        }
                    }
                }
                cheats_option.add( optionsAsString );
                String[] optionStrings = null;
                if( options != null )
                {
                    if( !options.isEmpty() )
                    {
                        // This is a multi-choice cheat
                        
                        optionStrings = new String[options.size()];
                        
                        // Each element is a key-value pair
                        for( int z = 0; z < options.size(); z++ )
                        {
                            // The first non-leading space character is the pair delimiter
                            optionStrings[z] = options.get( z ).name;
                            if( TextUtils.isEmpty( optionStrings[z] ) )
                                optionStrings[z] = getString( R.string.cheats_longPress );
                            
                        }
                    }
                }
                cheatList = new ArrayAdapter<String>( this, R.layout.cheat_row, cheats_name );
                setListAdapter( cheatList );
            }
        }
    }
    
    private void save( String crc )
    {
        CheatFile mupencheat_txt = new CheatFile( mAppData.mupencheat_txt );
        CheatSection c = mupencheat_txt.match( "^" + crc.replace( ' ', '-' ) + ".*" );
        if( c == null )
        {
            // Game name and country code from header
            c = new CheatSection( crc.replace( ' ', '-' ), mUserPrefs.selectedGameHeader.name, Integer.toHexString( ( mUserPrefs.selectedGameHeader.countryCode ) ).substring( 0, 2 ) );
            mupencheat_txt.add( c );
        }
        if( cheats_name.size() == cheats_desc.size() && cheats_desc.size() == cheats_code.size() && cheats_code.size() == cheats_option.size() )
        {
            c.clear();
            for( int i = 0; i < cheats_name.size(); i++ )
            {
                String desc = cheats_desc.get( i );
                CheatBlock b = null;
                if( desc.equals( getString( R.string.cheatNotes_none ) ) || TextUtils.isEmpty( desc ) )
                {
                    b = new CheatBlock( cheats_name.get( i ), null );
                }
                else
                {
                    b = new CheatBlock( cheats_name.get( i ), desc );
                }
                LinkedList<CheatOption> ops = new LinkedList<CheatOption>();
                if( cheats_option.get( i ) != null )
                {
                    if( !TextUtils.isEmpty( cheats_option.get( i ) ) )
                    {
                        String[] tmp_ops = cheats_option.get( i ).split( "\n" );
                        for( int o = 0; o < tmp_ops.length; o++ )
                        {
                            ops.add( new CheatOption( tmp_ops[o].substring( tmp_ops[o].lastIndexOf( ' ' ) + 1 ), tmp_ops[o].substring( 0, tmp_ops[o].lastIndexOf( ' ' ) ) ) );
                        }
                    }
                }
                String[] tmp_lines = cheats_code.get( i ).split( "\n" );
                if( tmp_lines.length > 0 )
                {
                    for( int o = 0; o < tmp_lines.length; o++ )
                    {
                        if( tmp_lines[o].indexOf( ' ' ) != -1 )
                        {
                            if( tmp_lines[o].contains( "?" ) )
                            {
                                b.add( new CheatCode( tmp_lines[o].substring( 0, tmp_lines[o].lastIndexOf( ' ' ) ), tmp_lines[o].substring( tmp_lines[o].lastIndexOf( ' ' ) + 1 ), ops ) );
                            }
                            else
                            {
                                b.add( new CheatCode( tmp_lines[o].substring( 0, tmp_lines[o].lastIndexOf( ' ' ) ), tmp_lines[o].substring( tmp_lines[o].lastIndexOf( ' ' ) + 1 ), null ) );
                            }
                        }
                    }
                }
                c.add( b );
            }
            mupencheat_txt.save();
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
    
    @Override
    protected void onListItemClick( ListView l, View v, final int position, long id )
    {
        StringBuilder message = new StringBuilder();
        message.append( getString( R.string.cheatEditor_title2 ) + "\n" );
        message.append( cheats_name.get( position ) + "\n" );
        message.append( getString( R.string.cheatEditor_notes2 ) + "\n" );
        message.append( cheats_desc.get( position ) + "\n" );
        message.append( getString( R.string.cheatEditor_code2 ) + "\n" );
        message.append( cheats_code.get( position ) );
        if( !TextUtils.isEmpty( cheats_option.get( position ) ) && cheats_code.get( position ).contains( "?" ) )
        {
            message.append( "\n" + getString( R.string.cheatEditor_option2 ) );
            message.append( "\n" + cheats_option.get( position ) );
        }
        
        Builder builder = new Builder( this );
        builder.setTitle( R.string.cheatEditor_info );
        builder.setMessage( message.toString() );
        builder.create().show();
        AlertDialog alertDialog = new AlertDialog.Builder( CheatEditorActivity.this ).create();
        alertDialog.setTitle( getString( R.string.cheatEditor_info ) );
    }
    
    @Override
    public void onClick( View v )
    {
        AlertDialog alertDialog;
        
        switch( v.getId() )
        {
            case R.id.imgBtnChtAdd:
                cheats_name.add( getString( R.string.cheatEditor_empty ) );
                cheats_desc.add( getString( R.string.cheatNotes_none ) );
                cheats_code.add( "" );
                cheats_option.add( "" );
                cheatList = new ArrayAdapter<String>( CheatEditorActivity.this, R.layout.cheat_row, cheats_name );
                setListAdapter( cheatList );
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
                save( mUserPrefs.selectedGameHeader.crc );
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

    @Override
    public boolean onItemLongClick( AdapterView<?> av, View v, final int pos, long id )
    {
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
                        promptTitle( pos );
                        break;
                    case R.id.btnEditNotes:
                        promptNotes( pos );
                        break;
                    case R.id.btnEditCode:
                        promptCode( pos );
                        break;
                    case R.id.btnEditOption:
                        promptOption( pos );
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
        
        // Hide the edit option button if not applicable
        if( !cheats_code.get( pos ).contains( "?" ) )
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
                        save( mUserPrefs.selectedGameHeader.crc );
                    }
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

    private void promptTitle( final int pos )
    {
        CharSequence title = getText( R.string.cheatEditor_title );
        CharSequence message = getText( R.string.cheatEditor_title_desc );
        CharSequence text = cheats_name.get( pos );
        int inputType = InputType.TYPE_CLASS_TEXT;
        Prompt.promptText( this, title, message, text, null, inputType, new PromptTextListener()
        {
            @Override
            public void onDialogClosed( CharSequence text, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    String str = text.toString().replace( '\n', ' ' );
                    cheats_name.set( pos, str );
                    cheatList = new ArrayAdapter<String>( CheatEditorActivity.this, R.layout.cheat_row, cheats_name );
                    setListAdapter( cheatList );
                }
            }
        } );
    }

    private void promptNotes( final int pos )
    {
        final String desc = cheats_desc.get( pos );
        CharSequence title = getText( R.string.cheatEditor_notes );
        CharSequence message = getText( R.string.cheatEditor_notes_desc );
        CharSequence text;
        if( desc.equals( getString( R.string.cheatNotes_none ) ) || TextUtils.isEmpty( desc ) )
        {
            text = "";
        }
        else
        {
            text = desc;
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
                    cheats_desc.set( pos, str );
                }
            }
        } );
    }

    private void promptCode( final int pos )
    {
        CharSequence title = getText( R.string.cheatEditor_code );
        CharSequence message = getText( R.string.cheatEditor_code_desc );
        CharSequence text = cheats_code.get( pos );
        int inputType = InputType.TYPE_CLASS_TEXT;
        Prompt.promptText( this, title, message, text, null, inputType, new PromptTextListener()
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
                        cheats_code.set( pos, text.toString().toUpperCase( Locale.US ) );
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

    private void promptOption( final int pos )
    {
        CharSequence title = getText( R.string.cheatEditor_option );
        CharSequence message = getText( R.string.cheatEditor_option_desc );
        CharSequence text = cheats_option.get( pos );
        int inputType = InputType.TYPE_CLASS_TEXT;
        Prompt.promptText( this, title, message, text, null, inputType, new PromptTextListener()
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
                        cheats_option.set( pos, verify );
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
                    cheats_name.remove( pos );
                    cheats_desc.remove( pos );
                    cheats_code.remove( pos );
                    cheats_option.remove( pos );
                    cheatList = new ArrayAdapter<String>( CheatEditorActivity.this, R.layout.cheat_row, cheats_name );
                    setListAdapter( cheatList );
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
