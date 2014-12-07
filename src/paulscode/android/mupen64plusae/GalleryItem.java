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
package paulscode.android.mupen64plusae;

import java.io.File;
import java.util.List;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.util.RomDetail;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class GalleryItem implements Comparable<GalleryItem>
{
    public final RomDetail detail;
    public final File romFile;
    public final BitmapDrawable artBitmap;
    
    public GalleryItem( Context context, String md5, String romPath, String artPath )
    {
        detail = RomDetail.lookupByMd5( md5 );
        
        romFile = TextUtils.isEmpty( romPath ) ? null : new File( romPath );
        
        if( !TextUtils.isEmpty( artPath ) && new File( artPath ).exists() )
            artBitmap = new BitmapDrawable( context.getResources(), artPath );
        else
            artBitmap = null;
    }
    
    @Override
    public int compareTo( GalleryItem that )
    {
        return this.toString().compareToIgnoreCase( that.toString() );
    }
    
    @Override
    public String toString()
    {
        if( detail != null && !TextUtils.isEmpty( detail.goodName ) )
            return detail.goodName;
        else if( romFile != null && !TextUtils.isEmpty( romFile.getName() ) )
            return romFile.getName();
        else
            return "unknown file";
    }
    
    public static class Adapter extends ArrayAdapter<GalleryItem>
    {
        private final Context mContext;
        
        public Adapter( Context context, int textViewResourceId, List<GalleryItem> objects )
        {
            super( context, textViewResourceId, objects );
            mContext = context;
        }
        
        @Override
        public View getView( int position, View convertView, ViewGroup parent )
        {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            View view = convertView;
            if( view == null )
                view = inflater.inflate( R.layout.gallery_item_adapter, parent, false );
            
            GalleryItem item = getItem( position );
            if( item != null )
            {
                ImageView artView = (ImageView) view.findViewById( R.id.imageArt );
                if( item.artBitmap != null )
                    artView.setImageDrawable( item.artBitmap );
                else
                    artView.setImageResource( R.drawable.default_coverart );
                
                TextView tv1 = (TextView) view.findViewById( R.id.text1 );
                tv1.setText( item.toString() );
            }
            
            return view;
        }
    }
}
