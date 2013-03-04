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

import paulscode.android.mupen64plusae.CoreInterface.OnStateCallbackListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.OnConfirmListener;
import paulscode.android.mupen64plusae.util.Prompt.OnFileListener;
import paulscode.android.mupen64plusae.util.Prompt.OnTextListener;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.SeekBar;
import android.widget.TextView;

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
    
    private UserPrefs mUserPrefs;
    
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
        mGameSpeedItem.setTitle( mActivity.getString( R.string.menuItem_toggleSpeed,
                BASELINE_SPEED_FACTOR ) );
        
        // Get the app data and user prefs after the activity has been created
        mAppData = new AppData( mActivity );
        mUserPrefs = new UserPrefs( mActivity );
        
        // Initialize to the last slot used
        setSlot( mAppData.getLastSlot(), false );
        
        // Initialize the pak menus
        initializePakMenu( menu, 1, mUserPrefs.isPlugged1, mUserPrefs.getPakType( 1 ) );
        initializePakMenu( menu, 2, mUserPrefs.isPlugged2, mUserPrefs.getPakType( 2 ) );
        initializePakMenu( menu, 3, mUserPrefs.isPlugged3, mUserPrefs.getPakType( 3 ) );
        initializePakMenu( menu, 4, mUserPrefs.isPlugged4, mUserPrefs.getPakType( 4 ) );
    }
    
    private void initializePakMenu( Menu menu, int player, boolean isPlugged, int pakType )
    {
        int menuPlayerId;
        int index;
        switch( player )
        {
            default:
            case 1:
                menuPlayerId = R.id.menuItem_pak1;
                break;
            case 2:
                menuPlayerId = R.id.menuItem_pak2;
                break;
            case 3:
                menuPlayerId = R.id.menuItem_pak3;
                break;
            case 4:
                menuPlayerId = R.id.menuItem_pak4;
                break;
        }
        switch( pakType )
        {
            default:
            case CoreInterface.PAK_TYPE_NONE:
                index = 0;
                break;
            case CoreInterface.PAK_TYPE_MEM:
                index = 1;
                break;
            case CoreInterface.PAK_TYPE_RUMBLE:
                index = 2;
                break;
        }
        MenuItem menuPlayer = menu.findItem( menuPlayerId );
        menuPlayer.getSubMenu().getItem( index ).setChecked( true );
        menuPlayer.setVisible( isPlugged );
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
                setPak( 1, CoreInterface.PAK_TYPE_NONE, item, true );
                break;
            case R.id.menuItem_pak2_empty:
                setPak( 2, CoreInterface.PAK_TYPE_NONE, item, true );
                break;
            case R.id.menuItem_pak3_empty:
                setPak( 3, CoreInterface.PAK_TYPE_NONE, item, true );
                break;
            case R.id.menuItem_pak4_empty:
                setPak( 4, CoreInterface.PAK_TYPE_NONE, item, true );
                break;
            case R.id.menuItem_pak1_mem:
                setPak( 1, CoreInterface.PAK_TYPE_MEM, item, true );
                break;
            case R.id.menuItem_pak2_mem:
                setPak( 2, CoreInterface.PAK_TYPE_MEM, item, true );
                break;
            case R.id.menuItem_pak3_mem:
                setPak( 3, CoreInterface.PAK_TYPE_MEM, item, true );
                break;
            case R.id.menuItem_pak4_mem:
                setPak( 4, CoreInterface.PAK_TYPE_MEM, item, true );
                break;
            case R.id.menuItem_pak1_rumble:
                setPak( 1, CoreInterface.PAK_TYPE_RUMBLE, item, true );
                break;
            case R.id.menuItem_pak2_rumble:
                setPak( 2, CoreInterface.PAK_TYPE_RUMBLE, item, true );
                break;
            case R.id.menuItem_pak3_rumble:
                setPak( 3, CoreInterface.PAK_TYPE_RUMBLE, item, true );
                break;
            case R.id.menuItem_pak4_rumble:
                setPak( 4, CoreInterface.PAK_TYPE_RUMBLE, item, true );
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
        
        CoreInterfaceNative.stateSetSpeed( speed );
        mGameSpeedItem.setTitle( mActivity.getString( R.string.menuItem_toggleSpeed, speed ) );
    }
    
    public void setSlot( int value, boolean notify )
    {
        // Sanity check and persist the value
        mSlot = value % NUM_SLOTS;
        mAppData.putLastSlot( mSlot );
        
        // Set the slot in the core
        CoreInterfaceNative.stateSetSlotEmulator( mSlot );
        
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
    
    public void setPak( int player, int pakType, MenuItem item, boolean notify )
    {
        // Persist the value
        mUserPrefs.putPakType( player, pakType );
        
        // Set the pak in the core
        CoreInterfaceNative.setControllerConfig( player - 1, true, pakType );

        // Refresh the pak submenu
        if( item != null )
            item.setChecked( true );

        // Send a toast if requested
        if( notify )
            Notifier.showToast( mActivity, R.string.toast_usingPak, player, item.getTitle() );
    }
    
    private void saveSlot()
    {
        Notifier.showToast( mActivity, R.string.toast_savingSlot, mSlot );
        CoreInterfaceNative.stateSaveEmulator();
    }
    
    private void loadSlot()
    {
        Notifier.showToast( mActivity, R.string.toast_loadingSlot, mSlot );
        CoreInterfaceNative.stateLoadEmulator();
    }
    
    private void saveFileFromPrompt()
    {
        CoreInterface.pauseEmulator( false );
        CharSequence title = mActivity.getText( R.string.menuItem_fileSave );
        CharSequence hint = mActivity.getText( R.string.hintFileSave );
        int inputType = InputType.TYPE_CLASS_TEXT;
        Prompt.promptText( mActivity, title, null, hint, inputType, new OnTextListener()
        {
            @Override
            public void onText( CharSequence text, int which )
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
        Prompt.promptFile( mActivity, title, null, startPath, new OnFileListener()
        {
            @Override
            public void onFile( File file, int which )
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
            Prompt.promptConfirm( mActivity, title, message, new OnConfirmListener()
            {
                @Override
                public void onConfirm()
                {
                    Notifier.showToast( mActivity, R.string.toast_overwritingFile, file.getName() );
                    CoreInterfaceNative.fileSaveEmulator( file.getAbsolutePath() );
                }
            } );
        }
        else
        {
            Notifier.showToast( mActivity, R.string.toast_savingFile, file.getName() );
            CoreInterfaceNative.fileSaveEmulator( file.getAbsolutePath() );
        }
    }
    
    private void loadState( File file )
    {
        Notifier.showToast( mActivity, R.string.toast_loadingFile, file.getName() );
        CoreInterfaceNative.fileLoadEmulator( file.getAbsolutePath() );
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
        
        final LayoutInflater inflater = (LayoutInflater) mActivity
                .getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        final View layout = inflater.inflate( R.layout.seek_bar_preference,
                (ViewGroup) mActivity.findViewById( R.id.rootLayout ) );
        
        final SeekBar seek = (SeekBar) layout.findViewById( R.id.seekbar );
        final TextView text = (TextView) layout.findViewById( R.id.textFeedback );
        final CharSequence title = mActivity.getText( R.string.menuItem_setSpeed );
        
        text.setText( Integer.toString( mSpeedFactor ) );
        seek.setMax( MAX_SPEED_FACTOR - MIN_SPEED_FACTOR );
        seek.setProgress( mSpeedFactor - MIN_SPEED_FACTOR );
        seek.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener()
        {
            public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser )
            {
                text.setText( Integer.toString( progress + MIN_SPEED_FACTOR ) );
            }
            
            public void onStartTrackingTouch( SeekBar seekBar )
            {
            }
            
            public void onStopTrackingTouch( SeekBar seekBar )
            {
            }
        } );
        
        Prompt.prefillBuilder( mActivity, title, null, new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    mSpeedFactor = seek.getProgress() + MIN_SPEED_FACTOR;
                    mCustomSpeed = true;
                    CoreInterfaceNative.stateSetSpeed( mSpeedFactor );
                    
                    mGameSpeedItem.setTitle( mActivity.getString( R.string.menuItem_toggleSpeed,
                            mSpeedFactor ) );
                }
                
                CoreInterface.resumeEmulator();
            }
        } ).setView( layout ).create().show();
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
        CoreInterface.setOnStateCallbackListener( new OnStateCallbackListener()
        {
            @Override
            public void onStateCallback( int paramChanged, int newValue )
            {
                if( paramChanged == CoreInterface.M64CORE_STATE_SAVECOMPLETE )
                {
                    System.exit( 0 ); // Bad, bad..
                    CoreInterface.setOnStateCallbackListener( null );
                    mActivity.finish();
                }
            }
        } );
        CoreInterfaceNative.fileSaveEmulator( mAutoSaveFile );
        // ////
    }
}
