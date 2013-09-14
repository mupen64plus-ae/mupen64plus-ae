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
 * Authors: littleguy77, Paul Lamb
 */
package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.CoreInterface.OnStateCallbackListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Notifier;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

public class GameMenuHandler implements OnStateCallbackListener
{
    private final Activity mActivity;
    
    private MenuItem mSlotMenuItem;
    
    private MenuItem mGameSpeedItem;
    
    private Menu mSlotSubMenu;
    
    private UserPrefs mUserPrefs;
    
    public GameMenuHandler( Activity activity )
    {
        mActivity = activity;
    }
    
    @Override
    public void onStateCallback( int paramChanged, int newValue )
    {
        if( paramChanged == CoreInterface.M64CORE_SPEED_FACTOR )
        {
            mGameSpeedItem.setTitle( mActivity.getString( R.string.menuItem_toggleSpeed, newValue ) );
        }
        else if( paramChanged == CoreInterface.M64CORE_SAVESTATE_SLOT )
        {
            // Refresh the slot item in the top-level options menu
            if( mSlotMenuItem != null )
                mSlotMenuItem.setTitle( mActivity.getString( R.string.menuItem_setSlot, newValue ) );
            
            // Refresh the slot submenu
            if( mSlotSubMenu != null )
            {
                MenuItem item = mSlotSubMenu.getItem( newValue );
                if( item != null )
                    item.setChecked( true );
            }
        }
    }
    
    public void onCreateOptionsMenu( Menu menu )
    {
        // Inflate the in-game menu, record the dynamic menu items/submenus for later
        mActivity.getMenuInflater().inflate( R.menu.game_activity, menu );
        mSlotMenuItem = menu.findItem( R.id.menuItem_setSlot );
        mSlotSubMenu = mSlotMenuItem.getSubMenu();
        mGameSpeedItem = menu.findItem( R.id.menuItem_toggleSpeed );
        
        // Initialize the UI text to something sane
        mGameSpeedItem.setTitle( mActivity.getString( R.string.menuItem_toggleSpeed, 100 ) );
        mSlotMenuItem.setTitle( mActivity.getString( R.string.menuItem_setSlot, 0 ) );
        
        // Get the app data and user prefs after the activity has been created
        mUserPrefs = new UserPrefs( mActivity );
        
        // Initialize the pak menus (reverse order since some get hidden)
        initializePakMenu( menu, 4, mUserPrefs.isPlugged4, mUserPrefs.getPakType( 4 ) );
        initializePakMenu( menu, 3, mUserPrefs.isPlugged3, mUserPrefs.getPakType( 3 ) );
        initializePakMenu( menu, 2, mUserPrefs.isPlugged2, mUserPrefs.getPakType( 2 ) );
        initializePakMenu( menu, 1, mUserPrefs.isPlugged1, mUserPrefs.getPakType( 1 ) );
    }
    
    @TargetApi( 11 )
    private void initializePakMenu( Menu menu, int player, boolean isPlugged, int pakType )
    {
        MenuItem pakMenu = menu.findItem( R.id.menuItem_paks );
        int playerOffset = 3 * ( player - 1 );
        int pakIndex;
        switch( pakType )
        {
            default:
            case CoreInterface.PAK_TYPE_NONE:
                pakIndex = 0;
                break;
            case CoreInterface.PAK_TYPE_MEMORY:
                pakIndex = 1;
                break;
            case CoreInterface.PAK_TYPE_RUMBLE:
                pakIndex = 2;
                break;
        }
        
        if( isPlugged )
        {
            // Checkmark the menu item
            pakMenu.getSubMenu().getItem( playerOffset + pakIndex ).setChecked( true );
            
            // Hide rumble pad menu item if not available
            Vibrator vibrator = (Vibrator) mActivity.getSystemService( Context.VIBRATOR_SERVICE );
            boolean hasPhoneVibrator = AppData.IS_HONEYCOMB
                    ? vibrator.hasVibrator()
                    : vibrator != null;
            boolean permitRumble = AppData.IS_JELLY_BEAN || ( player == 1 && hasPhoneVibrator );
            if( !permitRumble )
            {
                pakMenu.getSubMenu().getItem( playerOffset + 2 ).setVisible( false );
            }
        }
        else
        {
            // Hide all pak options if this controller is not plugged
            pakMenu.getSubMenu().getItem( playerOffset + 2 ).setVisible( false );
            pakMenu.getSubMenu().getItem( playerOffset + 1 ).setVisible( false );
            pakMenu.getSubMenu().getItem( playerOffset + 0 ).setVisible( false );
        }
    }
    
    public void onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.menuItem_slot0:
                CoreInterface.setSlot( 0 );
                break;
            case R.id.menuItem_slot1:
                CoreInterface.setSlot( 1 );
                break;
            case R.id.menuItem_slot2:
                CoreInterface.setSlot( 2 );
                break;
            case R.id.menuItem_slot3:
                CoreInterface.setSlot( 3 );
                break;
            case R.id.menuItem_slot4:
                CoreInterface.setSlot( 4 );
                break;
            case R.id.menuItem_slot5:
                CoreInterface.setSlot( 5 );
                break;
            case R.id.menuItem_slot6:
                CoreInterface.setSlot( 6 );
                break;
            case R.id.menuItem_slot7:
                CoreInterface.setSlot( 7 );
                break;
            case R.id.menuItem_slot8:
                CoreInterface.setSlot( 8 );
                break;
            case R.id.menuItem_slot9:
                CoreInterface.setSlot( 9 );
                break;
            case R.id.menuItem_pak1_empty:
                setPak( 1, CoreInterface.PAK_TYPE_NONE, item );
                break;
            case R.id.menuItem_pak2_empty:
                setPak( 2, CoreInterface.PAK_TYPE_NONE, item );
                break;
            case R.id.menuItem_pak3_empty:
                setPak( 3, CoreInterface.PAK_TYPE_NONE, item );
                break;
            case R.id.menuItem_pak4_empty:
                setPak( 4, CoreInterface.PAK_TYPE_NONE, item );
                break;
            case R.id.menuItem_pak1_mem:
                setPak( 1, CoreInterface.PAK_TYPE_MEMORY, item );
                break;
            case R.id.menuItem_pak2_mem:
                setPak( 2, CoreInterface.PAK_TYPE_MEMORY, item );
                break;
            case R.id.menuItem_pak3_mem:
                setPak( 3, CoreInterface.PAK_TYPE_MEMORY, item );
                break;
            case R.id.menuItem_pak4_mem:
                setPak( 4, CoreInterface.PAK_TYPE_MEMORY, item );
                break;
            case R.id.menuItem_pak1_rumble:
                setPak( 1, CoreInterface.PAK_TYPE_RUMBLE, item );
                break;
            case R.id.menuItem_pak2_rumble:
                setPak( 2, CoreInterface.PAK_TYPE_RUMBLE, item );
                break;
            case R.id.menuItem_pak3_rumble:
                setPak( 3, CoreInterface.PAK_TYPE_RUMBLE, item );
                break;
            case R.id.menuItem_pak4_rumble:
                setPak( 4, CoreInterface.PAK_TYPE_RUMBLE, item );
                break;
            case R.id.menuItem_toggleSpeed:
                CoreInterface.toggleSpeed();
                break;
            case R.id.menuItem_slotSave:
                CoreInterface.saveSlot();
                break;
            case R.id.menuItem_slotLoad:
                CoreInterface.loadSlot();
                break;
            case R.id.menuItem_fileSave:
                CoreInterface.saveFileFromPrompt();
                break;
            case R.id.menuItem_fileLoad:
                CoreInterface.loadFileFromPrompt();
                break;
            case R.id.menuItem_setSpeed:
                CoreInterface.setCustomSpeedFromPrompt();
                break;
            case R.id.menuItem_setIme:
                setIme();
                break;
            case R.id.menuItem_exit:
                mActivity.finish();
                break;
            default:
                break;
        }
    }
    
    public void setPak( int player, int pakType, MenuItem item )
    {
        // Persist the value
        mUserPrefs.putPakType( player, pakType );
        
        // Set the pak in the core
        CoreInterfaceNative.setControllerConfig( player - 1, true, pakType );
        
        // Ensure the item is valid
        if( item != null )
        {
            // Refresh the pak submenu
            item.setChecked( true );
        
            // Send a toast message
            Notifier.showToast( mActivity, item.getTitle().toString() + "." );
        }
    }
    
    private void setIme()
    {
        InputMethodManager imeManager = (InputMethodManager) mActivity
                .getSystemService( Context.INPUT_METHOD_SERVICE );
        if( imeManager != null )
        {
            imeManager.showInputMethodPicker();
        }
    }
}
