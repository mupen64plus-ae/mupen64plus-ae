/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: Paul Lamb
 */
package paulscode.android.mupen64plusae.persistent;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.persistent.OptionDialog.Listener;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;

public class CheatPreference extends Preference implements Listener, View.OnLongClickListener,
        View.OnClickListener

{
    private static int DEFAULT_VALUE = 0;
    
    private int mValue = DEFAULT_VALUE;
    private CheckBox mCheckbox;
    private final String[] mOptions;
    private final OptionDialog mOptionsDialog;
    private final AlertDialog mNotesDialog;
    private final AlertDialog mOptionNoteDialog;
    
    public CheatPreference( Context context, String title, String notes, String[] options )
    {
        super( context );
        
        if( TextUtils.isEmpty( title ) )
            title = context.getString( R.string.cheatNotes_title );
        if( TextUtils.isEmpty( notes ) )
            notes = context.getString( R.string.cheatNotes_none );
        
        if( options == null )
        {
            // Binary cheat
            mOptions = null;
            mOptionsDialog = null;
        }
        else
        {
            // Multi-choice cheat
            mOptions = new String[options.length + 1];
            mOptions[0] = context.getString( R.string.cheat_disabled );
            System.arraycopy( options, 0, mOptions, 1, options.length );
            mOptionsDialog = new OptionDialog( context, title, mOptions, this );
        }
        mNotesDialog = new Builder( context ).setTitle( title ).setMessage( notes ).create();
        mOptionNoteDialog = new Builder( context ).setTitle(
                context.getString( R.string.cheatOption_title ) ).create();
        
        setTitle( title );
        setWidgetLayoutResource( R.layout.widget_checkbox );
    }
    
    public void setValue( int value )
    {
        mValue = value;
        persistInt( mValue );
    }
    
    public String getCheatCodeString( int index )
    {
        String result = Integer.toString( index );
        if( mOptions != null || mValue != 0 )
            result += "-" + Integer.toString( mValue - 1 );
        return result;
    }
    
    public boolean isCheatEnabled()
    {
        return mValue != 0;
    }
    
    @Override
    public CharSequence getSummary()
    {
        if( mOptions == null || mValue == 0 )
            return null;
        else
            return mOptions[mValue];
    }
    
    @Override
    protected Object onGetDefaultValue( TypedArray a, int index )
    {
        return a.getInteger( index, DEFAULT_VALUE );
    }
    
    @Override
    protected void onSetInitialValue( boolean restorePersistedValue, Object defaultValue )
    {
        setValue( restorePersistedValue ? getPersistedInt( mValue ) : (Integer) defaultValue );
    }
    
    @Override
    protected void onBindView( View view )
    {
        super.onBindView( view );
        
        // Setup the click handling
        view.setOnLongClickListener( this );
        view.setOnClickListener( this );
        
        // Find and initialize the widgets
        mCheckbox = (CheckBox) view.findViewById( R.id.widgetCheckbox );
        mCheckbox.setFocusable( false );
        mCheckbox.setFocusableInTouchMode( false );
        mCheckbox.setClickable( false );
        
        // Refresh the widgets
        refreshWidgets();
    }
    
    @Override
    public void onClick( View v )
    {
        if( mOptionsDialog == null )
        {
            // Binary cheat
            setValue( 1 - mValue );
            refreshWidgets();
        }
        else
        {
            // Multi-choice cheat
            mOptionsDialog.show( mValue );
        }
    }
    
    @Override
    public void onOptionChoice( int choice )
    {
        setValue( choice );
        refreshWidgets();
    }
    
    @Override
    public boolean onLongClick( View v )
    {
        // Popup a dialog to display the cheat notes
        mNotesDialog.show();
        return true;
    }
    
    @Override
    public void onOptionLongPress( int item )
    {
        // TODO: Look through cheat options to see if any have really long strings.
        // If not, then long-pressing on cheat options for full text isn't needed.
        if( item != 0 )
        {
            mOptionNoteDialog.setMessage( mOptions[item] );
            mOptionNoteDialog.show();
        }
    }
    
    private void refreshWidgets()
    {
        setSummary( getSummary() );
        if( mCheckbox != null )
        {
            mCheckbox.setChecked( isCheatEnabled() );
        }
    }
}
