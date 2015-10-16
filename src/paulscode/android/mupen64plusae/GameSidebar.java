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
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class GameSidebar extends ScrollView
{
    private MenuListView mDrawerList;
    private ImageView mInfoArt;
    private LinearLayout mImageLayout;
    private TextView mGameTitle;
    private GameSidebarActionHandler mActionHandler;
    
    public GameSidebar( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        inflater.inflate( R.layout.game_sidebar, this );

        mDrawerList = (MenuListView) findViewById( R.id.drawerNavigation );
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
        
        // Configure the list in the navigation drawer
        mDrawerList = (MenuListView) findViewById( R.id.drawerNavigation );

        
        // Handle menu item selections
        mDrawerList.setOnClickListener( new MenuListView.OnClickListener()
        {
            @Override
            public void onClick( MenuItem menuItem )
            {
                mActionHandler.onGameSidebarAction( menuItem );
            }
        } );
    }
    
    public void setActionHandler(GameSidebarActionHandler actionHandler, int menuResource)
    {
        mActionHandler = actionHandler;
        mDrawerList.setMenuResource( menuResource );
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
    
    public interface GameSidebarActionHandler
    {
        abstract public void onGameSidebarAction(MenuItem menuItem);
    }
}
