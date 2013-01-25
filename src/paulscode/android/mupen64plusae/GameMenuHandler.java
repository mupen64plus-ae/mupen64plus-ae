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

import paulscode.android.mupen64plusae.CoreInterface.OnEmuStateChangeListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.OnConfirmListener;
import paulscode.android.mupen64plusae.util.Prompt.OnFileListener;
import paulscode.android.mupen64plusae.util.Prompt.OnTextListener;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.Utility;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
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
    
    private final String mAutoSaveFile;
    
    private MenuItem mSlotMenuItem;
    
    private MenuItem mGameSpeedItem;
    
    private Menu mSlotSubMenu;
    
    private AppData mAppData;
    
    public int mSlot = 0;
    
    public boolean mCustomSpeed = false;
    
    public int mSpeedFactor = DEFAULT_SPEED_FACTOR;
    
    public static GameMenuHandler sInstance = null;
    
    public GameMenuHandler( Activity activity, String manualSaveDir, String autoSaveFile )
    {
        mActivity = activity;
        mManualSaveDir = manualSaveDir;
        mAutoSaveFile = autoSaveFile;
        sInstance = this;
    }
    
    public void onCreateOptionsMenu( Menu menu )
    {
        // Inflate the in-game menu, record the dynamic menu items/submenus for later
        mActivity.getMenuInflater().inflate( R.menu.game_activity, menu );
        mSlotMenuItem = menu.findItem( R.id.menuItem_setSlot );
        mSlotSubMenu = mSlotMenuItem.getSubMenu();
        mGameSpeedItem = menu.findItem( R.id.menuItem_toggleSpeed );
        mGameSpeedItem.setTitle( mActivity.getString( R.string.ingameToggleSpeed_title,
                BASELINE_SPEED_FACTOR ) );
        
        // Get the app data after the activity has been created
        mAppData = new AppData( mActivity );
        
        // Initialize to the last slot used
        setSlot( mAppData.getLastSlot(), false );
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
            case R.id.menuItem_mainMenu:
                quitToMenu();
                break;
            default:
                break;
        }
    }
    
    private void toggleSpeed()
    {
        mCustomSpeed = !mCustomSpeed;
        int speed = mCustomSpeed ? mSpeedFactor : BASELINE_SPEED_FACTOR;
        
        NativeMethods.stateSetSpeed( speed );
        mGameSpeedItem.setTitle( mActivity.getString( R.string.ingameToggleSpeed_title, speed ) );
    }
    
    public void setSlot( int value, boolean notify )
    {
        // Sanity check and persist the value
        mSlot = value % NUM_SLOTS;
        mAppData.putLastSlot( mSlot );
        
        // Set the slot in the core
        NativeMethods.stateSetSlotEmulator( mSlot );
        
        // Refresh the slot item in the top-level options menu
        if( mSlotMenuItem != null )
            mSlotMenuItem.setTitle( mActivity.getString( R.string.ingameSetSlot_title, mSlot ) );
        
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
    
    private void saveSlot()
    {
        Notifier.showToast( mActivity, R.string.toast_savingSlot, mSlot );
        NativeMethods.stateSaveEmulator();
    }
    
    private void loadSlot()
    {
        Notifier.showToast( mActivity, R.string.toast_loadingSlot, mSlot );
        NativeMethods.stateLoadEmulator();
    }
    
    private void saveFileFromPrompt()
    {
        NativeMethods.pauseEmulator();
        CharSequence title = mActivity.getText( R.string.ingameFileSave_title );
        CharSequence hint = mActivity.getText( R.string.hintFileSave );
        int inputType = InputType.TYPE_CLASS_TEXT;
        Prompt.promptText( mActivity, title, null, hint, inputType, new OnTextListener()
        {
            @Override
            public void onText( CharSequence text, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                    saveState( text.toString() );
                NativeMethods.resumeEmulator();
            }
        } );
    }
    
    private void loadFileFromPrompt()
    {
        NativeMethods.pauseEmulator();
        CharSequence title = mActivity.getText( R.string.ingameFileLoad_title );
        File startPath = new File( mManualSaveDir );
        Prompt.promptFile( mActivity, title, null, startPath, new OnFileListener()
        {
            @Override
            public void onFile( File file, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                    loadState( file );
                NativeMethods.resumeEmulator();
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
            Prompt.promptConfirm( mActivity, title, message, new OnConfirmListener()
            {
                @Override
                public void onConfirm()
                {
                    Notifier.showToast( mActivity, R.string.toast_overwritingFile, file.getName() );
                    NativeMethods.fileSaveEmulator( file.getAbsolutePath() );
                }
            } );
        }
        else
        {
            Notifier.showToast( mActivity, R.string.toast_savingFile, file.getName() );
            NativeMethods.fileSaveEmulator( file.getAbsolutePath() );
        }
    }
    
    private void loadState( File file )
    {
        Notifier.showToast( mActivity, R.string.toast_loadingFile, file.getName() );
        NativeMethods.fileLoadEmulator( file.getAbsolutePath() );
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
        NativeMethods.pauseEmulator();
        CharSequence title = mActivity.getText( R.string.ingameSetSpeed_title );
        CharSequence hint = mActivity.getText( R.string.hintSetSpeed );
        int inputType = InputType.TYPE_CLASS_NUMBER;
        Prompt.promptText( mActivity, title, null, hint, inputType, new OnTextListener()
        {
            @Override
            public void onText( CharSequence text, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    if( text.length() != 0 )
                    {
                        mSpeedFactor = SafeMethods.toInt( text.toString(), DEFAULT_SPEED_FACTOR );
                        mSpeedFactor = Utility.clamp( mSpeedFactor, MIN_SPEED_FACTOR,
                                MAX_SPEED_FACTOR );
                        mCustomSpeed = true;
                        NativeMethods.stateSetSpeed( mSpeedFactor );
                        
                        mGameSpeedItem.setTitle( mActivity.getString(
                                R.string.ingameToggleSpeed_title, mSpeedFactor ) );
                    }
                }
                NativeMethods.resumeEmulator();
            }
        } );
    }
    
    private void quitToMenu()
    {
        // Return to previous activity (MenuActivity)
        // It's easier just to finish so that everything will be reloaded next time
        // mActivity.finish();
        
        // TODO: Uncomment the line above and delete the block below
        
        // ////
        // paulscode: temporary workaround for ASDP bug after emulator shuts down
        Notifier.showToast( mActivity, R.string.toast_savingSession );
        CoreInterface.setOnEmuStateChangeListener( new OnEmuStateChangeListener()
        {
            @Override
            public void onEmuStateChange( int newState )
            {
                if( newState == CoreInterface.EMULATOR_STATE_RUNNING )
                {
                    System.exit( 0 ); // Bad, bad..
                    CoreInterface.setOnEmuStateChangeListener( null );
                    mActivity.finish();
                }
            }
        } );
        NativeMethods.fileSaveEmulator( mAutoSaveFile );
        // ////
    }
}
