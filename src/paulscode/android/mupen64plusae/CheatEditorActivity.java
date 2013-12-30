package paulscode.android.mupen64plusae;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.CheatFile;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatBlock;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatCode;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatOption;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatSection;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class CheatEditorActivity extends ListActivity
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
        final ImageButton add = (ImageButton) findViewById( R.id.imgBtnChtAdd );
        add.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v )
            {
                cheats_name.add( getString( R.string.cheatEditor_empty ) );
                cheats_desc.add( getString( R.string.cheatNotes_none ) );
                cheats_code.add( "" );
                cheats_option.add( "" );
                cheatList = new ArrayAdapter<String>( CheatEditorActivity.this, R.layout.cheat_row, cheats_name );
                setListAdapter( cheatList );
                Toast t = Toast.makeText( CheatEditorActivity.this, getString( R.string.cheatEditor_added ), Toast.LENGTH_SHORT );
                t.show();
            }
        } );
        final ImageButton edit = (ImageButton) findViewById( R.id.imgBtnChtEdit );
        edit.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v )
            {
                AlertDialog alertDialog = new AlertDialog.Builder( CheatEditorActivity.this ).create();
                alertDialog.setTitle( getString( R.string.cheatEditor_edit ) );
                alertDialog.setMessage( getString( R.string.cheatEditor_edit_desc ) );
                alertDialog.show();
            }
        } );
        final ImageButton save = (ImageButton) findViewById( R.id.imgBtnChtSave );
        save.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v )
            {
                save( mUserPrefs.selectedGameHeader.crc );
                CheatEditorActivity.this.finish();
            }
        } );
        final ImageButton info = (ImageButton) findViewById( R.id.imgBtnChtInfo );
        info.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v )
            {
                AlertDialog alertDialog = new AlertDialog.Builder( CheatEditorActivity.this ).create();
                alertDialog.setTitle( getString( R.string.cheatEditor_help ) );
                alertDialog.setMessage( getString( R.string.cheatEditor_readme1 ) + "\n\n" + getString( R.string.cheatEditor_readme2 ) + "\n\n" + getString( R.string.cheatEditor_readme3 ) + "\n\n"
                        + getString( R.string.cheatEditor_readme4 ) + "\n\n" + getString( R.string.cheatEditor_readme5 ) + "\n\n" + getString( R.string.cheatEditor_readme6 ) );
                alertDialog.show();
            }
        } );
        ListView lv = getListView();
        lv.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick( AdapterView<?> av, View v, final int pos, long id )
            {
                final AlertDialog parentDialog = new AlertDialog.Builder( CheatEditorActivity.this ).create();
                parentDialog.setTitle( getString( R.string.cheatEditor_config ) );
                parentDialog.setMessage( getString( R.string.cheatEditor_config_desc ) );
                LinearLayout ll = new LinearLayout( CheatEditorActivity.this );
                ll.setOrientation( LinearLayout.VERTICAL );
                Button en = new Button( CheatEditorActivity.this );
                en.setText( getString( R.string.cheatEditor_title_desc ) );
                en.setOnClickListener( new OnClickListener()
                {
                    
                    @Override
                    public void onClick( View v )
                    {
                        parentDialog.dismiss();
                        AlertDialog alertDialog = new AlertDialog.Builder( CheatEditorActivity.this ).create();
                        alertDialog.setTitle( getString( R.string.cheatEditor_title ) );
                        alertDialog.setMessage( getString( R.string.cheatEditor_title_desc ) );
                        final EditText i = new EditText( CheatEditorActivity.this );
                        i.setText( cheats_name.get( pos ) );
                        alertDialog.setView( i );
                        alertDialog.setButton( AlertDialog.BUTTON_POSITIVE, getString( R.string.cheatEditor_ok ), new DialogInterface.OnClickListener()
                        {
                            
                            public void onClick( DialogInterface dialog, int which )
                            {
                                // Clicked
                                i.setText( i.getText().toString().replace( '\n', ' ' ) );
                                cheats_name.set( pos, i.getText().toString() );
                                cheatList = new ArrayAdapter<String>( CheatEditorActivity.this, R.layout.cheat_row, cheats_name );
                                setListAdapter( cheatList );
                            }
                            
                        } );
                        alertDialog.setButton( AlertDialog.BUTTON_NEGATIVE, getString( R.string.cheatEditor_cancel ), new DialogInterface.OnClickListener()
                        {
                            
                            public void onClick( DialogInterface dialog, int which )
                            {
                                // Clicked
                                
                            }
                            
                        } );
                        alertDialog.show();
                    }
                    
                } );
                Button ed = new Button( CheatEditorActivity.this );
                ed.setText( getString( R.string.cheatEditor_notes_desc ) );
                ed.setOnClickListener( new OnClickListener()
                {
                    
                    @Override
                    public void onClick( View v )
                    {
                        parentDialog.dismiss();
                        AlertDialog alertDialog = new AlertDialog.Builder( CheatEditorActivity.this ).create();
                        alertDialog.setTitle( getString( R.string.cheatEditor_notes ) );
                        alertDialog.setMessage( getString( R.string.cheatEditor_notes_desc ) );
                        final EditText i = new EditText( CheatEditorActivity.this );
                        if( cheats_desc.get( pos ).equals( getString( R.string.cheatNotes_none ) ) || TextUtils.isEmpty( cheats_desc.get( pos ) ) )
                        {
                            i.setText( "" );
                        }
                        else
                        {
                            i.setText( cheats_desc.get( pos ) );
                        }
                        
                        alertDialog.setView( i );
                        alertDialog.setButton( AlertDialog.BUTTON_POSITIVE, getString( R.string.cheatEditor_ok ), new DialogInterface.OnClickListener()
                        {
                            
                            public void onClick( DialogInterface dialog, int which )
                            {
                                // Clicked
                                i.setText( i.getText().toString().replace( '\n', ' ' ) );
                                if( TextUtils.isEmpty( i.getText() ) )
                                {
                                    i.setText( getString( R.string.cheatNotes_none ) );
                                }
                                cheats_desc.set( pos, i.getText().toString() );
                            }
                            
                        } );
                        alertDialog.setButton( AlertDialog.BUTTON_NEGATIVE, getString( R.string.cheatEditor_cancel ), new DialogInterface.OnClickListener()
                        {
                            
                            public void onClick( DialogInterface dialog, int which )
                            {
                                // Clicked
                                
                            }
                            
                        } );
                        alertDialog.show();
                    }
                    
                } );
                Button ec = new Button( CheatEditorActivity.this );
                ec.setText( getString( R.string.cheatEditor_code_desc ) );
                ec.setOnClickListener( new OnClickListener()
                {
                    
                    @Override
                    public void onClick( View v )
                    {
                        parentDialog.dismiss();
                        AlertDialog alertDialog = new AlertDialog.Builder( CheatEditorActivity.this ).create();
                        alertDialog.setTitle( getString( R.string.cheatEditor_code ) );
                        alertDialog.setMessage( getString( R.string.cheatEditor_code_desc ) );
                        final EditText i = new EditText( CheatEditorActivity.this );
                        i.setText( cheats_code.get( pos ) );
                        alertDialog.setView( i );
                        alertDialog.setButton( AlertDialog.BUTTON_POSITIVE, getString( R.string.cheatEditor_ok ), new DialogInterface.OnClickListener()
                        {
                            
                            public void onClick( DialogInterface dialog, int which )
                            {
                                // Clicked
                                String verify = i.getText().toString();
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
                                    cheats_code.set( pos, i.getText().toString().toUpperCase( Locale.US ) );
                                }
                                else
                                {
                                    Toast t = Toast.makeText( CheatEditorActivity.this, getString( R.string.cheatEditor_badCode ), Toast.LENGTH_SHORT );
                                    t.show();
                                }
                                
                            }
                            
                        } );
                        alertDialog.setButton( AlertDialog.BUTTON_NEGATIVE, getString( R.string.cheatEditor_cancel ), new DialogInterface.OnClickListener()
                        {
                            
                            public void onClick( DialogInterface dialog, int which )
                            {
                                // Clicked
                            }
                            
                        } );
                        alertDialog.show();
                    }
                    
                } );
                Button eo = new Button( CheatEditorActivity.this );
                eo.setText( getString( R.string.cheatEditor_option_desc ) );
                eo.setOnClickListener( new OnClickListener()
                {
                    
                    @Override
                    public void onClick( View v )
                    {
                        parentDialog.dismiss();
                        AlertDialog alertDialog = new AlertDialog.Builder( CheatEditorActivity.this ).create();
                        alertDialog.setTitle( getString( R.string.cheatEditor_option ) );
                        alertDialog.setMessage( getString( R.string.cheatEditor_option_desc ) );
                        final EditText i = new EditText( CheatEditorActivity.this );
                        i.setText( cheats_option.get( pos ) );
                        alertDialog.setView( i );
                        alertDialog.setButton( AlertDialog.BUTTON_POSITIVE, getString( R.string.cheatEditor_ok ), new DialogInterface.OnClickListener()
                        {
                            
                            public void onClick( DialogInterface dialog, int which )
                            {
                                // Clicked
                                String verify = i.getText().toString();
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
                                    i.setText( verify );
                                    cheats_option.set( pos, i.getText().toString() );
                                }
                                else
                                {
                                    Toast t = Toast.makeText( CheatEditorActivity.this, getString( R.string.cheatEditor_badOption ), Toast.LENGTH_SHORT );
                                    t.show();
                                }
                            }
                            
                        } );
                        alertDialog.setButton( AlertDialog.BUTTON_NEGATIVE, getString( R.string.cheatEditor_cancel ), new DialogInterface.OnClickListener()
                        {
                            
                            public void onClick( DialogInterface dialog, int which )
                            {
                                // Clicked
                            }
                            
                        } );
                        alertDialog.show();
                    }
                    
                } );
                Button de = new Button( CheatEditorActivity.this );
                de.setText( getString( R.string.cheatEditor_delete ) );
                de.setOnClickListener( new OnClickListener()
                {
                    
                    @Override
                    public void onClick( View v )
                    {
                        parentDialog.dismiss();
                        AlertDialog alertDialog = new AlertDialog.Builder( CheatEditorActivity.this ).create();
                        alertDialog.setTitle( getString( R.string.cheatEditor_delete ) );
                        alertDialog.setMessage( getString( R.string.cheatEditor_confirm ) );
                        
                        alertDialog.setButton( AlertDialog.BUTTON_POSITIVE, getString( R.string.cheatEditor_yes ), new DialogInterface.OnClickListener()
                        {
                            
                            public void onClick( DialogInterface dialog, int which )
                            {
                                // Clicked
                                cheats_name.remove( pos );
                                cheats_desc.remove( pos );
                                cheats_code.remove( pos );
                                cheats_option.remove( pos );
                                cheatList = new ArrayAdapter<String>( CheatEditorActivity.this, R.layout.cheat_row, cheats_name );
                                setListAdapter( cheatList );
                            }
                            
                        } );
                        alertDialog.setButton( AlertDialog.BUTTON_NEGATIVE, getString( R.string.cheatEditor_no ), new DialogInterface.OnClickListener()
                        {
                            
                            public void onClick( DialogInterface dialog, int which )
                            {
                                // Clicked
                            }
                            
                        } );
                        alertDialog.show();
                    }
                    
                } );
                ll.addView( en );
                ll.addView( ed );
                ll.addView( ec );
                if( cheats_code.get( pos ).contains( "?" ) )
                {
                    ll.addView( eo );
                }
                ll.addView( de );
                parentDialog.setView( ll );
                parentDialog.show();
                return true;
            }
        } );
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
        AlertDialog alertDialog = new AlertDialog.Builder( CheatEditorActivity.this ).create();
        alertDialog.setTitle( getString( R.string.cheatEditor_info ) );
        String msg = getString( R.string.cheatEditor_title2 ) + "\n" + cheats_name.get( position ) + "\n" + getString( R.string.cheatEditor_notes2 ) + "\n" + cheats_desc.get( position ) + "\n"
                + getString( R.string.cheatEditor_code2 ) + "\n" + cheats_code.get( position );
        if( !TextUtils.isEmpty( cheats_option.get( position ) ) && cheats_code.get( position ).contains( "?" ) )
        {
            msg += "\n" + getString( R.string.cheatEditor_option2 ) + "\n";
            msg += cheats_option.get( position );
        }
        alertDialog.setMessage( msg );
        alertDialog.show();
        
    }
    
    @Override
    // onBackPressed could probably be used
    public boolean onKeyDown( int KeyCode, KeyEvent event )
    {
        if( KeyCode == KeyEvent.KEYCODE_BACK )
        {
            AlertDialog alertDialog = new AlertDialog.Builder( CheatEditorActivity.this ).create();
            alertDialog.setTitle( getString( R.string.cheatEditor_saveConfirm ) );
            alertDialog.setButton( AlertDialog.BUTTON_POSITIVE, getString( R.string.cheatEditor_yes ), new DialogInterface.OnClickListener()
            {
                
                public void onClick( DialogInterface dialog, int which )
                {
                    // Clicked
                    save( mUserPrefs.selectedGameHeader.crc );
                    CheatEditorActivity.this.finish();
                }
                
            } );
            alertDialog.setButton( AlertDialog.BUTTON_NEGATIVE, getString( R.string.cheatEditor_no ), new DialogInterface.OnClickListener()
            {
                
                public void onClick( DialogInterface dialog, int which )
                {
                    // Clicked
                    CheatEditorActivity.this.finish();
                }
                
            } );
            alertDialog.show();
            return true;
        }
        return super.onKeyDown( KeyCode, event );
    }
    
}
