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
import java.util.Comparator;
import java.util.List;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.task.LoadBitmapTask;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GalleryItem
{
    public final String md5;
    public final String crc;
    public final String headerName;
    public final byte countryCode;
    public final String goodName;
    public final String artPath;
    public final int lastPlayed;
    public final File romFile;
    public final File zipFile;
    public final boolean isExtracted;
    public final Context context;
    public final boolean isHeading;
    public BitmapDrawable artBitmap;
    
    public GalleryItem( Context context, String md5, String crc, String headerName, byte countryCode, String goodName, String romPath,
            String zipPath, boolean extracted, String artPath, int lastPlayed )
    {
        this.md5 = md5;
        this.crc = crc;
        this.headerName = headerName;
        this.countryCode = countryCode;
        this.goodName = goodName;
        this.context = context;
        this.artPath = artPath;
        this.artBitmap = null;
        this.lastPlayed = lastPlayed;
        this.isHeading = false;
        this.isExtracted = extracted;
        
        this.romFile = TextUtils.isEmpty( romPath ) ? null : new File( romPath );
        this.zipFile = TextUtils.isEmpty( zipPath ) ? null : new File( zipPath );
    }
    
    public GalleryItem( Context context, String headingName)
    {
        this.goodName = headingName;
        this.context = context;
        this.isHeading = true;
        this.md5 = null;
        this.crc = null;
        this.headerName = null;
        this.countryCode = 0;
        this.artPath = null;
        this.artBitmap = null;
        this.lastPlayed = 0;
        this.isExtracted = false;
        this.romFile = null;
        this.zipFile = null;
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
    public String toString()
    {
        if( !TextUtils.isEmpty( goodName ) )
            return goodName;
        else if( romFile != null && !TextUtils.isEmpty( romFile.getName() ) )
            return romFile.getName();
        else
            return "unknown file";
    }
    
    public static class NameComparator implements Comparator<GalleryItem>
    {
        @Override
        public int compare( GalleryItem item1, GalleryItem item2 )
        {
            return item1.toString().compareToIgnoreCase( item2.toString() );
        }
    }
    
    public static class RecentlyPlayedComparator implements Comparator<GalleryItem>
    {
        @Override
        public int compare( GalleryItem item1, GalleryItem item2 )
        {
            return item2.lastPlayed - item1.lastPlayed;
        }
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder implements OnClickListener,
            OnLongClickListener
    {
        public GalleryItem item;
        private Context mContext;
        LoadBitmapTask mLoadBitmapTask = null;
        
        public ViewHolder( Context context, View view )
        {
            super( view );
            mContext = context;
            view.setOnClickListener( this );
            view.setOnLongClickListener( this );
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
        
        @Override
        public boolean onLongClick( View view )
        {
            if( mContext instanceof GalleryActivity )
            {
                GalleryActivity activity = (GalleryActivity) mContext;
                return activity.onGalleryItemLongClick( item );
            }
            return false;
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
        
        @Override
        public int getItemViewType( int position )
        {
            return mObjects.get( position ).isHeading ? 1 : 0;
        }
        
        public void onBindViewHolder( ViewHolder holder, int position )
        {
            // Clear the now-offscreen bitmap to conserve memory, also cancel any tasks reading the bitmap
            if( holder.item != null )
            {
                if(holder.mLoadBitmapTask != null)
                {
                    holder.mLoadBitmapTask.cancel(true);
                    holder.mLoadBitmapTask = null;
                }
                holder.item.clearBitmap();
            }
            
            // Called by RecyclerView to display the data at the specified position.
            View view = holder.itemView;
            GalleryItem item = mObjects.get( position );
            holder.item = item;
            
            if( item != null )
            {
                ImageView artView = (ImageView) view.findViewById( R.id.imageArt );
                
                TextView tv1 = (TextView) view.findViewById( R.id.text1 );
                tv1.setText( item.toString() );
                
                LinearLayout linearLayout = (LinearLayout) view.findViewById( R.id.galleryItem );
                GalleryActivity activity = (GalleryActivity) item.context;
                
                if( item.isHeading )
                {
                    view.setClickable( false );
                    view.setLongClickable( false );
                    linearLayout.setPadding( 0, 0, 0, 0 );
                    tv1.setPadding( 5, 10, 0, 0 );
                    tv1.setTextSize( TypedValue.COMPLEX_UNIT_DIP, 18.0f );
                    artView.setVisibility( View.GONE );
                }
                else
                {
                    view.setClickable( true );
                    view.setLongClickable( true );
                    linearLayout.setPadding( activity.galleryHalfSpacing,
                            activity.galleryHalfSpacing, activity.galleryHalfSpacing,
                            activity.galleryHalfSpacing );
                    tv1.setPadding( 0, 0, 0, 0 );
                    tv1.setTextSize( TypedValue.COMPLEX_UNIT_DIP, 13.0f );
                    artView.setVisibility( View.VISIBLE );
                    
                    artView.setImageResource( R.drawable.default_coverart );

                    //Load the real cover art in a background task
                    holder.mLoadBitmapTask = new LoadBitmapTask(item.context, item.artPath, artView); 
                    holder.mLoadBitmapTask.execute((String) null);
                    
                    artView.getLayoutParams().width = activity.galleryWidth;
                    artView.getLayoutParams().height = (int) ( activity.galleryWidth / activity.galleryAspectRatio );
                    
                    LinearLayout layout = (LinearLayout) view.findViewById( R.id.info );
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
