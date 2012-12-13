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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class OptionDialog
{
    public interface Listener
    {
        public void onOptionChoice( int choice );
        public void onOptionLongPress( int item );
    }

    public final int id;
    private final Listener mListener;
    private final AlertDialog.Builder mBuilder;
    private AlertDialog mDialog = null;
    private final String[] mOptions;
    
    public final AlertDialog getDialog()
    {
        if( mDialog == null )
        {
            mDialog = mBuilder.create();
        }
        ListView listView = mDialog.getListView();
        if( listView == null )
        {
            Log.e( "OptionDialog", "getListView() returned null in method getDialog" );
        }
        else
        {
            listView.setOnItemLongClickListener
            (
                new AdapterView.OnItemLongClickListener()
                {
                    public boolean onItemLongClick( AdapterView<?> adapterView, View view, int position, long id )
                    {
                        mListener.onOptionLongPress( position );
                        return true;
                    }
                }
            );
        }

        return mDialog;
    }
    
    public OptionDialog( final int id, String title, String[] options, Context context, Listener listener )
    {
        this.id = id;
        mOptions = options;
        mListener = listener;
        mBuilder = new AlertDialog.Builder( context );
        mBuilder.setTitle( title );
        mBuilder.setSingleChoiceItems( mOptions, -1,
                                       new DialogInterface.OnClickListener()
                                       {
                                           public void onClick( DialogInterface dialog, int which )
                                           {
                                               dialog.dismiss();
                                               if( mListener != null )
                                               {
                                                   mListener.onOptionChoice( which );
                                               }
                                           }
                                       } );
    }
}
