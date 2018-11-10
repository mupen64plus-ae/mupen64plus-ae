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
package paulscode.android.mupen64plusae.compat;

import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

public abstract class AppCompatListActivity extends AppCompatActivity
{
    
    /**
     * List view
     */
    private ListView mListView;
    
    @Override
    public void setContentView(int layoutResID)
    {
        super.setContentView(layoutResID);
        
        mListView = (ListView) findViewById(android.R.id.list);
        
        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                AppCompatListActivity.this.onListItemClick( mListView, view, position, id );
            }
        });
        
        mListView.setEmptyView(findViewById(android.R.id.empty));
    }
    
    protected ListView getListView()
    {
        return mListView;
    }
    
    protected void onListItemClick( ListView l, View v, int position, long id )
    {
        
    }
    
    protected void setListAdapter(ListAdapter adapter)
    {
        mListView.setAdapter(adapter);        
    }
}
