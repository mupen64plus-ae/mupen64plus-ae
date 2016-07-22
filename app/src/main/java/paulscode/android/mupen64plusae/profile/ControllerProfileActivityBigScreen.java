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

import java.util.Arrays;

import org.mupen64plusae.v3.fzurita.R;

import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.ListItemTwoTextIconPopulator;
import paulscode.android.mupen64plusae.input.map.InputMap;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


public class ControllerProfileActivityBigScreen extends ControllerProfileActivityBase implements OnItemClickListener, OnItemLongClickListener
{
    private ListView mListView;
    private ArrayAdapter<String> mListAdapter;
    
    @Override
    void initLayout()
    {
        mExitMenuItemVisible = false;
        setContentView( R.layout.controller_profile_activity_bigscreen );
        mListView = (ListView) findViewById( R.id.input_map_activity_bigscreen );
        mListView.setOnItemClickListener( this );
        mListView.setOnItemLongClickListener(this);
    }
    
    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id )
    {        
        popupListener( mCommandNames[position], mCommandIndices[position] );
    }
    
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
    {        
        final InputMap map = mProfile.getMap();
        map.unmapCommand( mCommandIndices[position] );
        mProfile.putMap( map );
        refreshAllButtons(false);
        
        return true;
    }
    
    private InputMap getMap()
    {
        return mProfile.getMap();
    }
    
    @Override
    protected void refreshAllButtons(boolean incrementSelection)
    {
        super.refreshAllButtons(incrementSelection);

        for (int i = 0; i < mN64Buttons.length; i++)
        {
            refreshButton(mN64Buttons[i], 0, getMap().isMapped(i));
        }
        
        if(mListAdapter == null)
        {
            mListAdapter = Prompt.createAdapter(this, Arrays.asList(mCommandNames),
                new ListItemTwoTextIconPopulator<String>()
                {
                    @Override
                    public void onPopulateListItem(String item, int position, TextView text1, TextView text2, ImageView icon)
                    {
                        text1.setText(item);
                        text2.setText(getMap().getMappedCodeInfo(mCommandIndices[position]));
                        icon.setVisibility(View.GONE);
                    }
                });
            mListView.setAdapter(mListAdapter);
        }
        
        mListAdapter.notifyDataSetChanged();

        if(incrementSelection && mListView.getSelectedItemPosition() != AdapterView.INVALID_POSITION )
        {
            //Update the selected item
            mListView.setSelection(mListView.getSelectedItemPosition() + 1);
        }
    }
}
