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
 * Authors: Paul Lamb
 */
package paulscode.android.mupen64plusae.persistent;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;

public class OptionDialog
{
    public interface Listener
    {
        public void onOptionChoice( int choice );
        
        public void onOptionLongPress( int item );
    }
    
    private final Listener mListener;
    private final String[] mOptions;
    private final Builder mBuilder;
    
    public OptionDialog( Context context, String title, String[] options, Listener listener )
    {
        mListener = listener;
        mOptions = options;
        mBuilder = new Builder( context ).setTitle( title );
    }
    
    public final void show( int checkedItem )
    {
        // Setup click handling for items in the dialog's list
        OnClickListener clickListener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                dialog.dismiss();
                if( mListener != null )
                    mListener.onOptionChoice( which );
            }
        };
        
        // Setup long-click handling for items in the dialog's list
        OnItemLongClickListener longClickListener = new OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick( AdapterView<?> av, View view, int position, long id )
            {
                if( mListener != null )
                    mListener.onOptionLongPress( position );
                return true;
            }
        };
        
        // Create and show the dialog
        mBuilder.setSingleChoiceItems( mOptions, checkedItem, clickListener );
        AlertDialog dialog = mBuilder.create();
        dialog.getListView().setOnItemLongClickListener( longClickListener );
        dialog.show();
    }
}
