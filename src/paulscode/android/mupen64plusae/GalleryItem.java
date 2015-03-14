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

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GalleryItem implements Comparable<GalleryItem>
{
    public static final double ASPECT_RATIO = 121.0 / 175.0;
    
    public final String md5;
    public final String goodName;
    public final String artPath;
    public final File romFile;
    public final Context context;
    public BitmapDrawable artBitmap;
    
    public GalleryItem( Context context, String md5, String goodName, String romPath, String artPath )
    {
        this.md5 = md5;
        this.goodName = goodName;
        this.context = context;
        this.artPath = artPath;
        this.artBitmap = null;
        
        romFile = TextUtils.isEmpty( romPath ) ? null : new File( romPath );
    }
    
    public void loadBitmap()
    {
        if( artBitmap != null )
            return;
        
        if( !TextUtils.isEmpty( artPath ) && new File( artPath ).exists() )
            artBitmap = new BitmapDrawable( context.getResources(), artPath );
    }
    
    public void clearBitmap()
    {
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
        if( !TextUtils.isEmpty( goodName ) )
            return goodName;
        else if( romFile != null && !TextUtils.isEmpty( romFile.getName() ) )
            return romFile.getName();
        else
            return "unknown file";
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder implements OnClickListener
    {
        public GalleryItem item;
        private Context mContext;
        
        public ViewHolder( Context context, View view )
        {
            super( view );
            mContext = context;
            view.setOnClickListener( this );
        }
        
        @Override
        public String toString()
        {
            return item.toString();
        }
        
        @Override
        public void onClick( View view )
        {
            if( mContext instanceof GalleryActivity )
            {
                GalleryActivity activity = (GalleryActivity) mContext;
                activity.onGalleryItemClick( item );
            }
        }
    }
    
    public static class Adapter extends RecyclerView.Adapter<ViewHolder>
    {
        private final Context mContext;
        private final List<GalleryItem> mObjects;
        
        public Adapter( Context context, List<GalleryItem> objects )
        {
            mContext = context;
            mObjects = objects;
        }
        
        @Override
        public int getItemCount()
        {
            return mObjects.size();
        }
        
        @Override
        public long getItemId( int position )
        {
            return 0;
        }
        
        public void onBindViewHolder( ViewHolder holder, int position )
        {
            // Clear the now-offscreen bitmap to conserve memory
            if( holder.item != null )
                holder.item.clearBitmap();
            
            // Called by RecyclerView to display the data at the specified position.
            View view = holder.itemView;
            GalleryItem item = mObjects.get( position );
            holder.item = item;
            
            if( item != null )
            {
                item.loadBitmap();
                
                ImageView artView = (ImageView) view.findViewById( R.id.imageArt );
                if( item.artBitmap != null )
                    artView.setImageDrawable( item.artBitmap );
                else
                    artView.setImageResource( R.drawable.default_coverart );
                
                LinearLayout layout = (LinearLayout) view.findViewById( R.id.info );
                TextView tv1 = (TextView) view.findViewById( R.id.text1 );
                tv1.setText( item.toString() );
                
                if( item.context instanceof GalleryActivity )
                {
                    GalleryActivity activity = (GalleryActivity) item.context;
                    artView.getLayoutParams().width = activity.galleryWidth;
                    artView.getLayoutParams().height = (int) ( activity.galleryWidth * ASPECT_RATIO );
                    layout.getLayoutParams().width = activity.galleryWidth;
                }
            }
        }
        
        public ViewHolder onCreateViewHolder( ViewGroup parent, int viewType )
        {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            View view = inflater.inflate( R.layout.gallery_item_adapter, parent, false );
            return new ViewHolder( mContext, view );
        }
    }
}
