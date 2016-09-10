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

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GameSidebar extends MenuListView
{
    private ImageView mInfoArt;
    private LinearLayout mImageLayout;
    private TextView mGameTitle;
    private GameSidebarActionHandler mActionHandler;
    private View mHeader;
    
    boolean mFocusScrolling;
    
    public GameSidebar( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        mHeader = inflater.inflate( R.layout.game_sidebar_header, this, false );

        mInfoArt = (ImageView) mHeader.findViewById( R.id.imageArt );
        mImageLayout = (LinearLayout) mHeader.findViewById( R.id.imageLayout );
        mGameTitle = (TextView) mHeader.findViewById( R.id.gameTitle );
        mFocusScrolling = false;

        // Disable scrolling portrait effect
        setOnItemSelectedListener(new OnItemSelectedListener()
        {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                mFocusScrolling = true;
                mImageLayout.setPadding( 0, 0, 0, 0 );
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                mFocusScrolling = false;
            }
            
        });
     
        // Scrolling portrait effect
        setOnScrollListener(new OnScrollListener()
        {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState)
            {
                // Nothing to do here
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
            {
                //Portrait effect doesn't work with focus scrolling for some reason
                if(!mFocusScrolling)
                {
                    PerformPortraitEffect();
                }
            }

        });
        
        setClipToPadding(true);
        addHeaderView(mHeader, null, false);
    }
    
    private void PerformPortraitEffect()
    {
        if(GameSidebar.this.getChildAt(0) == mHeader)
        {
            int top =  GameSidebar.this.getChildAt(0).getTop();
            int scrollY = top*-1;
            mImageLayout.setPadding( 0, scrollY / 2, 0, 0 );
        }
    }
    
    public void setActionHandler(GameSidebarActionHandler actionHandler, int menuResource)
    {
        mActionHandler = actionHandler;
        setMenuResource( menuResource );
        
        setNextFocusDownId(getId());
        setNextFocusLeftId(getId());
        setNextFocusRightId(getId());
        setNextFocusUpId(getId());
        
        // Handle menu item selections
        setOnClickListener( new MenuListView.OnClickListener()
        {
            @Override
            public void onClick( MenuItem menuItem )
            {
                mActionHandler.onGameSidebarAction( menuItem );
            }
        } );
        
        setOnKeyListener(actionHandler);
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
    
    public interface GameSidebarActionHandler extends OnKeyListener
    {
        abstract public void onGameSidebarAction(MenuItem menuItem);
    }
}
