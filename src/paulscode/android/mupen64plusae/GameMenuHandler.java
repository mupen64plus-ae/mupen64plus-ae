package paulscode.android.mupen64plusae;

import java.io.File;

import paulscode.android.mupen64plusae.CoreInterface.OnEmuStateChangeListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.OnFileListener;
import paulscode.android.mupen64plusae.util.Prompt.OnTextListener;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.Utility;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

public class GameMenuHandler
{
    private static final int BASELINE_SPEED_FACTOR = 100;
    
    private static final int DEFAULT_SPEED_FACTOR = 250;
    
    private static final int MAX_SPEED_FACTOR = 300;
    
    private static final int MIN_SPEED_FACTOR = 10;
    
    private static final int NUM_SLOTS = 10;
    
    private final Activity mActivity;
    
    private final String mManualSaveDir;
    
    private final String mAutoSaveFile;
    
    private MenuItem mSlotMenuItem;
    
    private MenuItem mGameSpeedItem;
    
    private Menu mSlotSubMenu;
    
    private AppData mAppData;
    
    private int mSlot = 0;
    
    private boolean mCustomSpeed = false;
    
    private int mSpeedFactor = DEFAULT_SPEED_FACTOR;
    
    public GameMenuHandler( Activity activity, String manualSaveDir, String autoSaveFile )
    {
        mActivity = activity;
        mManualSaveDir = manualSaveDir;
        mAutoSaveFile = autoSaveFile;
    }
    
    public void onCreateOptionsMenu( Menu menu )
    {
        // Inflate the in-game menu, record the dynamic menu items/submenus for later
        mActivity.getMenuInflater().inflate( R.menu.game_activity, menu );
        mSlotMenuItem = menu.findItem( R.id.ingameSetSlot );
        mSlotSubMenu = mSlotMenuItem.getSubMenu();
        mGameSpeedItem = menu.findItem( R.id.ingameToggleSpeed );
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
            case R.id.slot0:
                setSlot( 0, true );
                break;
            case R.id.slot1:
                setSlot( 1, true );
                break;
            case R.id.slot2:
                setSlot( 2, true );
                break;
            case R.id.slot3:
                setSlot( 3, true );
                break;
            case R.id.slot4:
                setSlot( 4, true );
                break;
            case R.id.slot5:
                setSlot( 5, true );
                break;
            case R.id.slot6:
                setSlot( 6, true );
                break;
            case R.id.slot7:
                setSlot( 7, true );
                break;
            case R.id.slot8:
                setSlot( 8, true );
                break;
            case R.id.slot9:
                setSlot( 9, true );
                break;
            case R.id.ingameToggleSpeed:
                toggleSpeed();
                break;
            case R.id.ingameSlotSave:
                saveSlot();
                break;
            case R.id.ingameSlotLoad:
                loadSlot();
                break;
            case R.id.ingameFileSave:
                saveFileFromPrompt();
                break;
            case R.id.ingameFileLoad:
                loadFileFromPrompt();
                break;
            case R.id.ingameSetSpeed:
                setSpeed();
                break;
            case R.id.ingameSetIme:
                setIme();
                break;
            case R.id.ingameMainMenu:
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
    
    private void setSlot( int value, boolean notify )
    {
        // Sanity check and persist the value
        mSlot = value % NUM_SLOTS;
        mAppData.setLastSlot( mSlot );
        
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
            Prompt.promptConfirm( mActivity, title, message, new OnClickListener()
            {
                @Override
                public void onClick( DialogInterface dialog, int which )
                {
                    if( which == DialogInterface.BUTTON_POSITIVE )
                    {
                        Notifier.showToast( mActivity, R.string.toast_overwritingFile,
                                file.getName() );
                        NativeMethods.fileSaveEmulator( file.getAbsolutePath() );
                    }
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
