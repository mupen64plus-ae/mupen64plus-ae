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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.profile;

import java.util.List;

import paulscode.android.mupen64plusae.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ProfileListAdapter<T extends Profile> extends ArrayAdapter<T>
{
    private static final int RESID = R.layout.list_item_two_text_icon;
    
    public ProfileListAdapter( Context context, List<T> profiles )
    {
        super( context, RESID, profiles );
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
        
        Profile item = getItem( position );
        if( item != null )
        {
            TextView text1 = (TextView) view.findViewById( R.id.text1 );
            TextView text2 = (TextView) view.findViewById( R.id.text2 );
            ImageView icon = (ImageView) view.findViewById( R.id.icon );
            
            int stringId = item.isBuiltin
                    ? R.string.listItem_profileBuiltin
                    : R.string.listItem_profileCustom;
            text1.setText( context.getString( stringId, item.name ) );
            text2.setText( item.comment );
            icon.setImageResource( R.drawable.ic_sliders );
        }
        return view;
    }
}