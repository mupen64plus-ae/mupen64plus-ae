/*
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

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;

import paulscode.android.mupen64plusae.task.LoadBitmapTask;
import paulscode.android.mupen64plusae.util.CountryCode;
import paulscode.android.mupen64plusae.util.FileUtil;

@SuppressWarnings("WeakerAccess")
public class GalleryItem
{
    public final String md5;
    public final String crc;
    public final String headerName;
    public final CountryCode countryCode;
    public final String goodName;
    public final String displayName;
    public final String artPath;
    public final int lastPlayed;
    public final String romUri;
    public final String zipUri;
    public final WeakReference<Context> context;
    public final boolean isHeading;
    public BitmapDrawable artBitmap;
    public final float scale;
    
    public GalleryItem(Context context, String md5, String crc, String headerName, CountryCode countryCode, String goodName,
                       String displayName, String romUri, String zipUri, String artPath, int lastPlayed, float scale )
    {
        this.md5 = md5;
        this.crc = crc;
        this.headerName = headerName;
        this.countryCode = countryCode;
        this.goodName = goodName;
        this.displayName = displayName;
        this.context = new WeakReference<>(context);
        this.artPath = artPath;
        this.artBitmap = null;
        this.lastPlayed = lastPlayed;
        this.isHeading = false;
        this.scale = scale;
        this.romUri = romUri;
        this.zipUri = zipUri;
    }
    
    GalleryItem( Context context, String headingName)
    {
        this.goodName = headingName;
        this.displayName = headingName;
        this.context = new WeakReference<>(context);
        this.isHeading = true;
        this.md5 = "";
        this.crc = "";
        this.headerName = "";
        this.countryCode = CountryCode.UNKNOWN;
        this.artPath = "";
        this.artBitmap = null;
        this.lastPlayed = 0;
        this.romUri = null;
        this.zipUri = null;
        this.scale = 1.0f;
    }
    
    void loadBitmap(Context context)
    {
        if( artBitmap != null )
            return;

        if( !TextUtils.isEmpty( artPath ) && new File( artPath ).exists())
            artBitmap = new BitmapDrawable( context.getResources(), artPath );
    }

    @NonNull
    public String toString()
    {
        if( !TextUtils.isEmpty( goodName ) )
            return displayName;
        else if( !TextUtils.isEmpty( romUri ) && context.get() != null) {
            DocumentFile file = FileUtil.getDocumentFileSingle(context.get(), Uri.parse(romUri));
            String romName = file == null ? null : file.getName();
            if (romName == null) {
                romName = "unknown file";
            }
            return romName;
        }else
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

    public static class RomFileComparator implements Comparator<GalleryItem>
    {
        @Override
        public int compare( GalleryItem item1, GalleryItem item2 )
        {
            String romFileName1 = item1.romUri != null ? item1.romUri : "";
            String romFileName2 = item2.romUri != null ? item2.romUri : "";

            return romFileName1.compareToIgnoreCase( romFileName2);
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
        private final WeakReference<Activity> mActivity;
        
        ViewHolder( WeakReference<Activity> activity, View view)
        {
            super( view );
            mActivity = activity;
            view.setOnClickListener( this );
            view.setOnLongClickListener( this );
        }
        
        @NonNull
        @Override
        public String toString()
        {
            return item.toString();
        }
        
        @Override
        public void onClick( View view )
        {
            Context tempContext = mActivity.get();
            if ( tempContext instanceof GalleryActivity )
            {
                GalleryActivity activity = (GalleryActivity) tempContext;
                activity.onGalleryItemClick( item );
            }
        }
        
        @Override
        public boolean onLongClick( View view )
        {
            Context tempContext = mActivity.get();

            if ( tempContext instanceof GalleryActivity )
            {
                GalleryActivity activity = (GalleryActivity) tempContext;
                return activity.onGalleryItemLongClick( item );
            }
            return false;
        }
    }
    
    public static class Adapter extends RecyclerView.Adapter<ViewHolder>
    {
        private final WeakReference<Activity> mActivity;
        private final List<GalleryItem> mObjects;
        private final LoadBitmapTask mLoadBitMapTask;
        
        public Adapter( Activity activity, List<GalleryItem> objects )
        {
            mActivity = new WeakReference<>(activity);
            mObjects = objects;
            mLoadBitMapTask = new LoadBitmapTask(activity);
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

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position )
        {
            // Clear the now-offscreen bitmap to conserve memory, also cancel any tasks reading the bitmap
            if( holder.item != null )
            {
                mLoadBitMapTask.cancel(holder.hashCode());
            }

            // Called by RecyclerView to display the data at the specified position.
            View view = holder.itemView;
            GalleryItem item = mObjects.get( position );
            holder.item = item;
            
            if( item != null )
            {
                ImageView artView = view.findViewById( R.id.imageArt );
                
                TextView tv1 = view.findViewById( R.id.text1 );
                tv1.setText( item.toString() );

                Activity tempActivity = mActivity.get();

                if (tempActivity != null) {
                    LinearLayout linearLayout = view.findViewById( R.id.galleryItem );
                    GalleryActivity activity = (GalleryActivity) tempActivity;

                    if( item.isHeading )
                    {
                        tv1.setText( item.toString().toUpperCase() );
                        tv1.setGravity(Gravity.BOTTOM);
                        view.setClickable( false );
                        view.setLongClickable( false );
                        linearLayout.setPadding( 0, 0, 0, 0 );
                        tv1.setPadding( 25, 10, 0, 0 );
                        tv1.setTextSize( TypedValue.COMPLEX_UNIT_DIP, 14.5f );
                        tv1.setLetterSpacing(0.1f);
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
                        tv1.setTextSize( TypedValue.COMPLEX_UNIT_DIP, 13.0f*item.scale );
                        artView.setVisibility( View.VISIBLE );

                        artView.setImageResource( R.drawable.default_coverart );

                        //Load the real cover art in a background task
                        mLoadBitMapTask.loadInBackGround(holder.hashCode(), item.artPath, artView);

                        artView.getLayoutParams().width = activity.galleryWidth;
                        artView.getLayoutParams().height = (int) ( activity.galleryWidth / activity.galleryAspectRatio );

                        LinearLayout layout = view.findViewById( R.id.info );
                        layout.getLayoutParams().width = activity.galleryWidth;
                        layout.getLayoutParams().height = (int)(activity.getResources().getDimension( R.dimen.galleryTextHeight )*item.scale);
                    }
                }
            }
        }
        
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType )
        {
            Context tempActivity = mActivity.get();
            LayoutInflater inflater = (LayoutInflater) tempActivity
                    .getSystemService( Context.LAYOUT_INFLATER_SERVICE );

            View view;
            if (inflater != null) {
                view = inflater.inflate( R.layout.gallery_item_adapter, parent, false );
            } else {
                view = new View(tempActivity, null);
            }
            return new ViewHolder( mActivity, view );
        }
    }
}
