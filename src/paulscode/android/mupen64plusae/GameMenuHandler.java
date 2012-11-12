package paulscode.android.mupen64plusae;

import java.io.File;

import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.OnFileListener;
import paulscode.android.mupen64plusae.util.Prompt.OnTextListener;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class GameMenuHandler
{
    private static final int NUM_SLOTS = 10;

    private final Activity mActivity;
    
    private MenuItem mSlotMenuItem;
    
    private int mSlot = 0;

    public GameMenuHandler( Activity activity )
    {
        mActivity = activity;
    }
    
    public void onCreateOptionsMenu( Menu menu )
    {
        // Inflate the in-game menu, record the 'Slot X' menu object for later
        mActivity.getMenuInflater().inflate( R.menu.game_activity, menu );
        mSlotMenuItem = menu.findItem( R.id.ingameSlot );
        setSlot( 0, false );
    }

    public void onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.slot0:
                setSlot( 0, item );
                break;
            case R.id.slot1:
                setSlot( 1, item );
                break;
            case R.id.slot2:
                setSlot( 2, item );
                break;
            case R.id.slot3:
                setSlot( 3, item );
                break;
            case R.id.slot4:
                setSlot( 4, item );
                break;
            case R.id.slot5:
                setSlot( 5, item );
                break;
            case R.id.slot6:
                setSlot( 6, item );
                break;
            case R.id.slot7:
                setSlot( 7, item );
                break;
            case R.id.slot8:
                setSlot( 8, item );
                break;
            case R.id.slot9:
                setSlot( 9, item );
                break;
            case R.id.ingameQuicksave:
                NativeMethods.stateSaveEmulator();
                break;
            case R.id.ingameQuickload:
                NativeMethods.stateLoadEmulator();
                break;
            case R.id.ingameSave:
                saveStateToFile();
                break;
            case R.id.ingameLoad:
                loadStateFromFile();
                break;
            case R.id.ingameMenu:
                // Save game state and launch MenuActivity
//                saveSession();
                Notifier.clear();
                CoreInterface.shutdown();
                Intent intent = new Intent( mActivity, MenuActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                mActivity.startActivity( intent );
                // mActivity.finish();
                break;
            default:
                break;
        }
    }

    private void setSlot( int value, MenuItem item )
    {
        setSlot( value );
        item.setChecked( true );
    }

    private void setSlot( int value )
    {
        setSlot( value, true );
    }

    private void setSlot( int value, boolean notify )
    {
        mSlot = value % NUM_SLOTS;
        NativeMethods.stateSetSlotEmulator( mSlot );
        if( notify )
            Notifier.showToast( mActivity, R.string.toast_savegameSlot, mSlot );
        if( mSlotMenuItem != null )
            mSlotMenuItem.setTitle( mActivity.getString( R.string.ingameSlot_title, mSlot ) );
    }

    private void loadStateFromFile()
    {
        Prompt.promptFile( mActivity, mActivity.getText( R.string.ingameLoad_title ), null,
                new File( Globals.userPrefs.gameSaveDir ), new OnFileListener()
                {
                    @Override
                    public void onFile( File file )
                    {
                        NativeMethods.fileLoadEmulator( file.getAbsolutePath() );
                    }
                } );
    }

    private void saveStateToFile()
    {
        CharSequence title = mActivity.getText( R.string.ingameSave_title );
        CharSequence hint = mActivity.getText( R.string.gameImplementation_saveHint );
        Prompt.promptText( mActivity, title, null, hint, new OnTextListener()
        {
            @Override
            public void onText( CharSequence text )
            {
                saveStateToFile( text.toString() );
            }
        } );
    }

    private void saveStateToFile( final String filename )
    {
        final File file = new File( Globals.userPrefs.gameSaveDir + "/" + filename );
        if( file.exists() )
        {
            String title = mActivity.getString( R.string._confirmation );
            String message = mActivity
                    .getString( R.string.gameImplementation_confirmFile, filename );
            Prompt.promptConfirm( mActivity, title, message, new OnClickListener()
            {
                @Override
                public void onClick( DialogInterface dialog, int which )
                {
                    if( which == DialogInterface.BUTTON_POSITIVE )
                    {
                        Log.i( "GameLifecycleHandler", "Overwriting file " + filename );
                        NativeMethods.fileSaveEmulator( file.getAbsolutePath() );
                    }
                }
            } );
        }
        else
        {
            Log.i( "GameLifecycleHandler", "Saving file " + filename );
            NativeMethods.fileSaveEmulator( file.getAbsolutePath() );
        }
    }


}
