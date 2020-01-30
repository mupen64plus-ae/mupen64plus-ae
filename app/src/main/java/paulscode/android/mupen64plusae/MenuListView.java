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
 * Authors: BonzaiThePenguin
 */
package paulscode.android.mupen64plusae;

import android.app.Activity;
import android.content.Context;

import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.widget.TextViewCompat;

import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.mupen64plusae.v3.alpha.R;

/* ExpandableListView which stores its data set as a Menu hierarchy */

@SuppressWarnings("unused")
public class MenuListView extends ExpandableListView
{
    private MenuListAdapter mAdapter;
    private OnClickListener mListener;
    private Menu mListData;
    
    public MenuListView( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        mAdapter = null;
        mListener = null;
        mListData = null;
    }
    
    public void setMenuResource( int menuResource )
    {
        Context context = getContext();
        Menu menu = new MenuBuilder( context );
        Activity activity = (Activity) context;
        activity.getMenuInflater().inflate( menuResource, menu );
        setMenu( menu );
    }
    
    public void setMenu( Menu menu )
    {
        mListData = menu;
        mAdapter = new MenuListAdapter( this, menu );
        setAdapter( mAdapter );
        setChoiceMode( ListView.CHOICE_MODE_SINGLE );
        
        // MenuListView uses its own group indicators
        setGroupIndicator( null );

        // Update the expand/collapse group indicators as needed
        setOnGroupExpandListener(groupPosition -> reload());
        setOnGroupCollapseListener(groupPosition -> reload());
        setOnGroupClickListener((parent, view, groupPosition, itemId) -> {

            MenuItem menuItem = mListData.getItem( groupPosition );
            SubMenu submenu = menuItem.getSubMenu();
            if( submenu == null )
            {
                if( mListener != null )
                    mListener.onClick( menuItem );
            }
            return false;
        });
        
        setOnChildClickListener((parent, view, groupPosition, childPosition, itemId) -> {

            MenuItem menuItem = mListData.getItem( groupPosition ).getSubMenu()
                    .getItem( childPosition );
            if( mListener != null )
                mListener.onClick( menuItem );
            return false;
        });
    }
    
    public Menu getMenu()
    {
        return mListData;
    }
    
    public MenuListAdapter getMenuListAdapter()
    {
        return mAdapter;
    }
    
    public void reload()
    {
        mAdapter.notifyDataSetChanged();

        // HACK: When a using a controller a reload doesn't work until an additional key is pressed.
        // I'm not sure why that is. I tried invalidate and postInvalidate.
        onKeyDown(KeyEvent.KEYCODE_0, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0));
    }
    
    public void setOnClickListener( OnClickListener listener )
    {
        mListener = listener;
    }
    
    public OnClickListener getOnClickListener()
    {
        return mListener;
    }
    
    public View getViewFromMenuId(int menuId)
    {
        return mAdapter.getViewFromMenuId(menuId);
    }
    
    public static class MenuListAdapter extends BaseExpandableListAdapter
    {
        private MenuListView mListView;
        private Menu mListData;
        private SparseArray<View> mMenuViews;
        private SparseArray<View> mMenuViewsExpanded;
        
        MenuListAdapter( MenuListView listView, Menu listData )
        {
            mListView = listView;
            mListData = listData;
            mMenuViews = new SparseArray<>();
            mMenuViewsExpanded = new SparseArray<>();
        }
        
        @Override
        public boolean isChildSelectable( int groupPosition, int childPosition )
        {
            return getChild( groupPosition, childPosition ).isEnabled();
        }
        
        @Override
        public MenuItem getChild( int groupPosition, int childPosition )
        {
            return getGroup( groupPosition ).getSubMenu().getItem( childPosition );
        }
        
        @Override
        public long getChildId( int groupPosition, int childPosition )
        {
            return getChild( groupPosition, childPosition ).getItemId();
        }
        
        @Override
        public int getChildrenCount( int groupPosition )
        {
            SubMenu submenu = mListData.getItem( groupPosition ).getSubMenu();
            return ( submenu != null ) ? submenu.size() : 0;
        }
        
        @Override
        public View getChildView( int groupPosition, final int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent )
        {
            LayoutInflater inflater = (LayoutInflater) mListView.getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE );

            // Don't use convertView, it can't be trusted, it doesn't work when using controllers
            View view = null;
            
            MenuItem item = getChild( groupPosition, childPosition );

            if( item != null && inflater != null)
            {
                view = mMenuViews.get(item.getItemId());

                if (view == null) {
                    view = inflater.inflate( R.layout.list_item_menu, mListView, false );
                }

                TextView text1 = view.findViewById( R.id.text1 );
                TextView text2 = view.findViewById( R.id.text2 );
                ImageView icon = view.findViewById( R.id.icon );
                ImageView indicator = view.findViewById( R.id.indicator );
                
                text1.setText( item.getTitle() );
                TextViewCompat.setTextAppearance(text1, R.style.Theme_Mupen64plusaeTheme);

                if(item.getTitleCondensed().equals(item.getTitle()))
                {
                    text2.setVisibility( View.GONE );
                }
                else
                {
                    text2.setText( item.getTitleCondensed() );
                }

                try {
                    icon.setImageDrawable( item.getIcon() );
                } catch (android.content.res.Resources.NotFoundException e) {
                    Log.i("MenuListView", "Item does not have an icon");
                }
                
                // Indent child views by 30 points
                DisplayMetrics metrics = new DisplayMetrics();
                ( (Activity) mListView.getContext() ).getWindowManager().getDefaultDisplay()
                        .getMetrics( metrics );
                
                view.setPadding((int) ( 30 * metrics.density ), view.getPaddingTop(),
                        view.getPaddingRight(), view.getPaddingBottom() );
                
                if( !item.isCheckable() )
                    indicator.setImageResource( 0x0 );
                else if( item.isChecked() )
                    indicator.setImageResource( R.drawable.ic_check );
                else
                    indicator.setImageResource( R.drawable.ic_box );

                mMenuViews.put(item.getItemId(), view);
            }

            return view;
        }
        
        @Override
        public MenuItem getGroup( int groupPosition )
        {
            return mListData.getItem( groupPosition );
        }
        
        @Override
        public long getGroupId( int groupPosition )
        {
            return getGroup( groupPosition ).getItemId();
        }
        
        @Override
        public int getGroupCount()
        {
            return mListData.size();
        }
        
        @Override
        public View getGroupView( int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent )
        {
            LayoutInflater inflater = (LayoutInflater) mListView.getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE );

            // Don't use convertView, it can't be trusted, it doesn't work when using controllers
            View view = null;
            
            MenuItem item = getGroup( groupPosition );
            if( item != null )
            {
                if (isExpanded) {
                    view = mMenuViewsExpanded.get(item.getItemId());
                } else {
                    view = mMenuViews.get(item.getItemId());
                }

                if (view == null && inflater != null) {
                    view = inflater.inflate( R.layout.list_item_menu, mListView, false );
                }

                if (view == null) {
                    return null;
                }

                TextView text1 = view.findViewById( R.id.text1 );
                TextView text2 = view.findViewById( R.id.text2 );
                ImageView icon = view.findViewById( R.id.icon );
                ImageView indicator = view.findViewById( R.id.indicator );
                
                text1.setText( item.getTitle() );

                if(item.getTitleCondensed().equals(item.getTitle()))
                {
                    text2.setVisibility( View.GONE );
                }
                else
                {
                    text2.setText( item.getTitleCondensed() );
                }

                icon.setImageDrawable( item.getIcon() );
                TextViewCompat.setTextAppearance(text1, R.style.darkParentMenuItem);

                if( item.getSubMenu() == null )
                    indicator.setImageResource( 0x0 );
                else if( isExpanded ) {
                    indicator.setImageResource( R.drawable.ic_arrow_u );
                }
                else {
                    indicator.setImageResource(R.drawable.ic_arrow_d);
                }

                if (isExpanded) {
                    mMenuViewsExpanded.put(item.getItemId(), view);
                } else {
                    mMenuViews.put(item.getItemId(), view);
                }
            }
            
            return view;
        }
        
        @Override
        public boolean hasStableIds()
        {
            return true;
        }
        
        View getViewFromMenuId(int menuId)
        {
            return mMenuViews.get(menuId);
        }
    }

    public interface OnClickListener
    {        
        void onClick( MenuItem menuItem );
    }
}
