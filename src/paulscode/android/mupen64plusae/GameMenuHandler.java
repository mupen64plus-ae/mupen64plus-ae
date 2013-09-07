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

import java.io.File;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.util.Prompt.PromptFileListener;
import paulscode.android.mupen64plusae.util.Prompt.PromptIntegerListener;
import paulscode.android.mupen64plusae.util.Prompt.PromptTextListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Vibrator;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

public class GameMenuHandler
{
    private static final int BASELINE_SPEED_FACTOR = 100;
    
    private static final int DEFAULT_SPEED_FACTOR = 250;
    
    public static final int MAX_SPEED_FACTOR = 300;
    
    public static final int MIN_SPEED_FACTOR = 10;
    
    private static final int NUM_SLOTS = 10;
    
    private final Activity mActivity;
    
    private final String mManualSaveDir;
    
    private MenuItem mSlotMenuItem;
    
    private MenuItem mGameSpeedItem;
    
    private Menu mSlotSubMenu;
    
    private AppData mAppData;
    
    private UserPrefs mUserPrefs;
    
    public int mSlot = 0;
    
    public boolean mCustomSpeed = false;
    
    public int mSpeedFactor = DEFAULT_SPEED_FACTOR;
    
    public static GameMenuHandler sInstance = null;
    
    public GameMenuHandler( Activity activity, String manualSaveDir )
    {
        mActivity = activity;
        mManualSaveDir = manualSaveDir;
        sInstance = this;
    }
    
    public void onCreateOptionsMenu( Menu menu )
    {
        // Inflate the in-game menu, record the dynamic menu items/submenus for later
        mActivity.getMenuInflater().inflate( R.menu.game_activity, menu );
        mSlotMenuItem = menu.findItem( R.id.menuItem_setSlot );
        mSlotSubMenu = mSlotMenuItem.getSubMenu();
        mGameSpeedItem = menu.findItem( R.id.menuItem_toggleSpeed );
        mGameSpeedItem.setTitle( mActivity.getString( R.string.menuItem_toggleSpeed,
                BASELINE_SPEED_FACTOR ) );
        
        // Get the app data and user prefs after the activity has been created
        mAppData = new AppData( mActivity );
        mUserPrefs = new UserPrefs( mActivity );
        
        // Initialize to the last slot used
        setSlot( mAppData.getLastSlot(), false );
        
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
                setSlot( 0, true );
                break;
            case R.id.menuItem_slot1:
                setSlot( 1, true );
                break;
            case R.id.menuItem_slot2:
                setSlot( 2, true );
                break;
            case R.id.menuItem_slot3:
                setSlot( 3, true );
                break;
            case R.id.menuItem_slot4:
                setSlot( 4, true );
                break;
            case R.id.menuItem_slot5:
                setSlot( 5, true );
                break;
            case R.id.menuItem_slot6:
                setSlot( 6, true );
                break;
            case R.id.menuItem_slot7:
                setSlot( 7, true );
                break;
            case R.id.menuItem_slot8:
                setSlot( 8, true );
                break;
            case R.id.menuItem_slot9:
                setSlot( 9, true );
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
                toggleSpeed();
                break;
            case R.id.menuItem_slotSave:
                saveSlot();
                break;
            case R.id.menuItem_slotLoad:
                loadSlot();
                break;
            case R.id.menuItem_fileSave:
                saveFileFromPrompt();
                break;
            case R.id.menuItem_fileLoad:
                loadFileFromPrompt();
                break;
            case R.id.menuItem_setSpeed:
                setSpeed();
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
    
    private void toggleSpeed()
    {
        mCustomSpeed = !mCustomSpeed;
        int speed = mCustomSpeed ? mSpeedFactor : BASELINE_SPEED_FACTOR;
        
        CoreInterfaceNative.emuSetSpeed( speed );
        mGameSpeedItem.setTitle( mActivity.getString( R.string.menuItem_toggleSpeed, speed ) );
    }
    
    public void setSlot( int value, boolean notify )
    {
        // Sanity check and persist the value
        mSlot = value % NUM_SLOTS;
        mAppData.putLastSlot( mSlot );
        
        // Set the slot in the core
        CoreInterfaceNative.emuSetSlot( mSlot );
        
        // Refresh the slot item in the top-level options menu
        if( mSlotMenuItem != null )
            mSlotMenuItem.setTitle( mActivity.getString( R.string.menuItem_setSlot, mSlot ) );
        
        // Refresh the slot submenu
        if( mSlotSubMenu != null )
        {
            MenuItem item = mSlotSubMenu.getItem( mSlot );
            if( item != null )
                item.setChecked( true );
        }
        
        // Send a toast if requested
        if( notify )
            Notifier.showToast( mActivity, R.string.toast_usingSlot, mSlot );
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
    
    private void saveSlot()
    {
        Notifier.showToast( mActivity, R.string.toast_savingSlot, mSlot );
        CoreInterfaceNative.emuSaveSlot();
    }
    
    private void loadSlot()
    {
        Notifier.showToast( mActivity, R.string.toast_loadingSlot, mSlot );
        CoreInterfaceNative.emuLoadSlot();
    }
    
    private void saveFileFromPrompt()
    {
        CoreInterface.pauseEmulator( false );
        CharSequence title = mActivity.getText( R.string.menuItem_fileSave );
        CharSequence hint = mActivity.getText( R.string.hintFileSave );
        int inputType = InputType.TYPE_CLASS_TEXT;
        Prompt.promptText( mActivity, title, null, hint, inputType, new PromptTextListener()
        {
            @Override
            public void onDialogClosed( CharSequence text, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                    saveState( text.toString() );
                CoreInterface.resumeEmulator();
            }
        } );
    }
    
    private void loadFileFromPrompt()
    {
        CoreInterface.pauseEmulator( false );
        CharSequence title = mActivity.getText( R.string.menuItem_fileLoad );
        File startPath = new File( mManualSaveDir );
        Prompt.promptFile( mActivity, title, null, startPath, new PromptFileListener()
        {
            @Override
            public void onDialogClosed( File file, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                    loadState( file );
                CoreInterface.resumeEmulator();
            }
        } );
    }
    
    private void saveState( final String filename )
    {
        final File file = new File( mManualSaveDir + "/" + filename );
        if( file.exists() )
        {
            String title = mActivity.getString( R.string.confirm_title );
            String message = mActivity.getString( R.string.confirmOverwriteFile_message, filename );
            Prompt.promptConfirm( mActivity, title, message, new PromptConfirmListener()
            {
                @Override
                public void onConfirm()
                {
                    Notifier.showToast( mActivity, R.string.toast_overwritingFile, file.getName() );
                    CoreInterfaceNative.emuSaveFile( file.getAbsolutePath() );
                }
            } );
        }
        else
        {
            Notifier.showToast( mActivity, R.string.toast_savingFile, file.getName() );
            CoreInterfaceNative.emuSaveFile( file.getAbsolutePath() );
        }
    }
    
    private void loadState( File file )
    {
        Notifier.showToast( mActivity, R.string.toast_loadingFile, file.getName() );
        CoreInterfaceNative.emuLoadFile( file.getAbsolutePath() );
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
    
    private void setSpeed()
    {
        CoreInterface.pauseEmulator( false );
        
        final CharSequence title = mActivity.getText( R.string.menuItem_setSpeed );
        
        Prompt.promptInteger( mActivity, title, "%1$d %%", mSpeedFactor, MIN_SPEED_FACTOR,
                MAX_SPEED_FACTOR, new PromptIntegerListener()
        {            
            @Override
            public void onDialogClosed( Integer value, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    mSpeedFactor = value;
                    mCustomSpeed = true;
                    CoreInterfaceNative.emuSetSpeed( mSpeedFactor );
                    
                    mGameSpeedItem.setTitle( mActivity.getString( R.string.menuItem_toggleSpeed,
                            mSpeedFactor ) );
                }
                
                CoreInterface.resumeEmulator();
            }
        } );
    }
}
