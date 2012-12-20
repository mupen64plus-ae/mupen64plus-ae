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
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;

public class OptionCheckBoxPreference extends LongClickCheckBoxPreference implements OptionDialog.Listener, View.OnLongClickListener
{
    private final Context mContext;
    private final OptionDialog mDialog;
    private String[] mOptions = null;
    public int mChoice = -1;
    
    // TODO: Allow implementation in XML, too
    // public OptionCheckBoxPreference( Context context, AttributeSet attrs )
    
    public OptionCheckBoxPreference( Context context, String title, String[] options, String negativeOption )
    {
        super( context );
        mContext = context;
        if( options == null || options.length < 1 )
        {
            mOptions = new String[1];
        }
        else
        {
            mOptions = new String[ options.length + 1 ];

            System.arraycopy( options, 0, mOptions, 1, options.length );
        }

        mOptions[0] = negativeOption;
        mDialog = new OptionDialog( title.hashCode(), title, mOptions, context, this );
    }
    
    @Override
    protected void onClick()
    {
        // The checked state is controlled by the option dialog choice
        mDialog.getDialog().show();
    }
    
    public void onOptionChoice( int choice )
    {
        setChecked( choice != 0 );

        if( choice == 0 )
            setSummary( "" );
        else
            setSummary( mOptions[choice] );

        mChoice = choice - 1;
    }
    public void onOptionLongPress( int item )
    {
        // TODO: Look through cheat options to see if any have really long strings.
        // If not, then long-pressing on cheat options for full text isn't needed.
        if( item != 0 )
        {
            new AlertDialog.Builder( mContext ).setTitle( mContext.getString( R.string.cheatOption_title ) )
                .setMessage( mOptions[item] ).create().show();
        }
    }
}
