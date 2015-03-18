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
 * Authors: BonzaiThePenguin
 */
package paulscode.android.mupen64plusae;

import org.mupen64plusae.v3.alpha.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class GameSidebar extends ScrollView
{
    private LinearLayout mLayout;
    private ImageView mInfoArt;
    private LinearLayout mImageLayout;
    private TextView mGameTitle;
    private Context mContext;
    
    public GameSidebar( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        inflater.inflate( R.layout.game_sidebar, this );
        
        mContext = context;
        mLayout = (LinearLayout) findViewById( R.id.layout );
        mInfoArt = (ImageView) findViewById( R.id.imageArt );
        mImageLayout = (LinearLayout) findViewById( R.id.imageLayout );
        mGameTitle = (TextView) findViewById( R.id.gameTitle );
        
        // Have the game cover art scroll at half the speed as the rest of the content
        final ScrollView scroll = this;
        getViewTreeObserver().addOnScrollChangedListener( new OnScrollChangedListener()
        {
            @Override
            public void onScrollChanged()
            {
                int scrollY = scroll.getScrollY();
                mImageLayout.setPadding( 0, scrollY / 2, 0, 0 );
            }
        } );
    }
    
    public void setImage( BitmapDrawable image )
    {
        if( image != null )
            mInfoArt.setImageDrawable( image );
        else
            mInfoArt.setImageResource( R.drawable.default_coverart );
    }
    
    public void setTitle( String title )
    {
        mGameTitle.setText( title );
    }
    
    public void clear()
    {
        mLayout.removeAllViews();
    }
    
    public void addHeading( String heading )
    {
        // Perhaps we should just inflate this from XML?
        TextView headingView = new TextView( mContext );
        headingView.setText( heading );
        headingView.setTextSize( TypedValue.COMPLEX_UNIT_SP, 14.0f );
        
        DisplayMetrics metrics = new DisplayMetrics();
        ( (Activity) mContext ).getWindowManager().getDefaultDisplay().getMetrics( metrics );
        int padding = (int) ( metrics.density * 5 );
        headingView.setPadding( padding, padding, padding, padding );
        mLayout.addView( headingView );
    }
    
    public void addRow( int icon, String title, String summary, Action action )
    {
        addRow( icon, title, summary, action, 0x0, 0 );
    }
    
    public void addRow( int icon, String title, String summary, Action action, int indicator, int indentation )
    {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View view = inflater.inflate( R.layout.list_item_menu, null );
        
        if( indentation != 0 )
        {
            DisplayMetrics metrics = new DisplayMetrics();
            ( (Activity) getContext() ).getWindowManager().getDefaultDisplay().getMetrics( metrics );
            
            view.setPadding( (int) ( indentation * 15 * metrics.density ), view.getPaddingTop(),
                    view.getPaddingRight(), view.getPaddingBottom() );
        }
        
        ImageView iconView = (ImageView) view.findViewById( R.id.icon );
        TextView text1 = (TextView) view.findViewById( R.id.text1 );
        TextView text2 = (TextView) view.findViewById( R.id.text2 );
        iconView.setImageResource( icon );
        text1.setText( title );
        text2.setText( summary );
        if( TextUtils.isEmpty( summary ) )
            text2.setVisibility( View.GONE );
        
        ImageView indicatorView = (ImageView) view.findViewById( R.id.indicator );
        indicatorView.setImageResource( indicator );
        if( indicator == 0x0 )
            indicatorView.setVisibility( View.GONE );
        
        mLayout.addView( view );
        
        if( action == null )
            return;
        
        // Pass the action to the click listener
        final Action finalAction = action;
        
        view.setFocusable( true );
        view.setBackgroundResource( android.R.drawable.list_selector_background );
        view.setOnClickListener( new OnClickListener()
        {
            @Override
            public void onClick( View view )
            {
                finalAction.onAction();
            }
        } );
    }
    
    public abstract static class Action
    {
        abstract public void onAction();
    }
}
