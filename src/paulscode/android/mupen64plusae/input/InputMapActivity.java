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
package paulscode.android.mupen64plusae.input;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider.OnInputListener;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.DeviceUtil;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.ListItemTwoTextIconPopulator;
import paulscode.android.mupen64plusae.util.Prompt.OnConfirmListener;
import paulscode.android.mupen64plusae.util.Prompt.OnFileListener;
import paulscode.android.mupen64plusae.util.Prompt.OnInputCodeListener;
import paulscode.android.mupen64plusae.util.Prompt.OnTextListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class InputMapActivity extends Activity implements OnInputListener, OnClickListener, OnItemClickListener
{
    // Visual settings
    private static final float UNMAPPED_BUTTON_ALPHA = 0.2f;
    private static final int UNMAPPED_BUTTON_FILTER = 0x66FFFFFF;
    private static final int MIN_LAYOUT_WIDTH_DP = 480;
    
    // The key name and default value for the player number, obtained from the intent extras map
    public static final String KEYEXTRA_PLAYER = "paulscode.android.mupen64plusae.EXTRA_PLAYER";
    private static final int DEFAULT_PLAYER = 0;
    private int mPlayer;
    
    // User preferences wrapper
    private UserPrefs mUserPrefs;
    
    // Command information
    private String[] mCommandNames;
    private int[] mCommandIndices;
    
    // Input mapping and listening
    private final InputMap mMap = new InputMap();
    private KeyProvider mKeyProvider;
    private AxisProvider mAxisProvider;
    private List<Integer> mUnmappableInputCodes;
    
    // Widgets
    private final Button[] mN64Buttons = new Button[InputMap.NUM_MAPPABLES];
    private TextView mFeedbackText;
    private View mSpecialFuncsView;
    private MenuItem mMenuSpecialVisibility;
    private ListView mListView;
    
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get the user preferences wrapper
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
        
        // Make sure the profiles directory exists
        new File( mUserPrefs.profileDir ).mkdirs();
        
        // Get the command info
        mCommandNames = getResources().getStringArray( R.array.inputMapActivity_entries );
        String[] indices = getResources().getStringArray( R.array.inputMapActivity_values );
        mCommandIndices = new int[indices.length];
        for( int i = 0; i < indices.length; i++ )
        {
            mCommandIndices[i] = Integer.parseInt( indices[i] );
        }
        
        // Get the player number and get the associated preference values
        Bundle extras = getIntent().getExtras();
        mPlayer = extras == null ? DEFAULT_PLAYER : extras.getInt( KEYEXTRA_PLAYER, DEFAULT_PLAYER );
        
        // Update the member variables from the persisted values
        mMap.deserialize( mUserPrefs.getInputMapString( mPlayer ) );
        
        // Set up input listeners
        mUnmappableInputCodes = mUserPrefs.unmappableKeyCodes;
        if( !mUserPrefs.isOuyaMode )
        {
            mKeyProvider = new KeyProvider( ImeFormula.DEFAULT, mUnmappableInputCodes );
            mKeyProvider.registerListener( this );
            if( AppData.IS_HONEYCOMB_MR1 )
            {
                mAxisProvider = new AxisProvider();
                mAxisProvider.registerListener( this );
            }
        }
        
        // Set the title of the activity
        CharSequence title = getResources().getString( R.string.inputMapActivity_title, mPlayer );
        setTitle( title );
        
        // Initialize the layout
        if( mUserPrefs.isOuyaMode )
            initLayoutOuya();
        else
            initLayoutDefault();
        
        // Refresh everything
        refreshAllButtons();
    }
    
    private void initLayoutOuya()
    {
        setContentView( R.layout.input_map_activity_ouya );
        mListView = (ListView) findViewById( R.id.input_map_activity_ouya );
        mListView.setOnItemClickListener( this );
    }
    
    private void initLayoutDefault()
    {
        // Select the appropriate window layout according to device configuration. Although you can
        // do this through the resource directory structure and layout aliases, we'll do it this way
        // for now since it's easier to maintain in the short term while the design is in flux.
        // TODO: Consider using resource directories to handle device variation, once design is set.
        WindowManager manager = (WindowManager) getSystemService( Context.WINDOW_SERVICE );
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics( metrics );
        float scalefactor = (float) DisplayMetrics.DENSITY_DEFAULT / (float) metrics.densityDpi;
        int widthDp = Math.round( metrics.widthPixels * scalefactor );
        
        // For narrow screens, use an alternate layout
        if( widthDp < MIN_LAYOUT_WIDTH_DP )
            setContentView( R.layout.input_map_activity_port );
        else
            setContentView( R.layout.input_map_activity );
        
        // Initialize and refresh the widgets
        initWidgets();
    }
    
    private void initWidgets()
    {
        // Hide some widgets that do not apply
        if( mUserPrefs.isTouchpadEnabled && mPlayer == 1 )
        {
            // First player and Xperia PLAY touchpad is enabled, hide the a- and c-pads
            findViewById( R.id.aPadDefault ).setVisibility( View.GONE );
            findViewById( R.id.cPadDefault ).setVisibility( View.GONE );
        }
        else
        {
            // All other cases, hide the Xperia PLAY stuff
            findViewById( R.id.aPadXperiaPlay ).setVisibility( View.GONE );
            findViewById( R.id.cPadXperiaPlay ).setVisibility( View.GONE );
        }
        
        // Get the special functions button group
        mSpecialFuncsView = findViewById( R.id.include_all_special_keys );
        refreshSpecialVisibility();
        
        // Get the text view object
        mFeedbackText = (TextView) findViewById( R.id.textFeedback );
        mFeedbackText.setText( "" );
        
        // Create a button list to simplify highlighting and mapping
        // @formatter:off
        setupButton( R.id.buttonDR,     AbstractController.DPD_R );
        setupButton( R.id.buttonDL,     AbstractController.DPD_L );
        setupButton( R.id.buttonDD,     AbstractController.DPD_D );
        setupButton( R.id.buttonDU,     AbstractController.DPD_U );
        setupButton( R.id.buttonS,      AbstractController.START );
        setupButton( R.id.buttonZ,      AbstractController.BTN_Z );
        setupButton( R.id.buttonB,      AbstractController.BTN_B );
        setupButton( R.id.buttonA,      AbstractController.BTN_A );
        setupButton( R.id.buttonCR,     AbstractController.CPD_R );
        setupButton( R.id.buttonCL,     AbstractController.CPD_L );
        setupButton( R.id.buttonCD,     AbstractController.CPD_D );
        setupButton( R.id.buttonCU,     AbstractController.CPD_U );
        setupButton( R.id.buttonR,      AbstractController.BTN_R );
        setupButton( R.id.buttonL,      AbstractController.BTN_L );
        setupButton( R.id.buttonAR,            InputMap.AXIS_R );
        setupButton( R.id.buttonAL,            InputMap.AXIS_L );
        setupButton( R.id.buttonAD,            InputMap.AXIS_D );
        setupButton( R.id.buttonAU,            InputMap.AXIS_U );
        setupButton( R.id.buttonIncrementSlot, InputMap.FUNC_INCREMENT_SLOT );
        setupButton( R.id.buttonSaveSlot,      InputMap.FUNC_SAVE_SLOT );
        setupButton( R.id.buttonReset,         InputMap.FUNC_RESET );
        setupButton( R.id.buttonLoadSlot,      InputMap.FUNC_LOAD_SLOT );
        setupButton( R.id.buttonPause,         InputMap.FUNC_PAUSE );
        setupButton( R.id.buttonStop,          InputMap.FUNC_STOP );
        setupButton( R.id.buttonSpeedDown,     InputMap.FUNC_SPEED_DOWN );
        setupButton( R.id.buttonSpeedUp,       InputMap.FUNC_SPEED_UP );
        setupButton( R.id.buttonFastForward,   InputMap.FUNC_FAST_FORWARD );
        setupButton( R.id.buttonFrameAdvance,  InputMap.FUNC_FRAME_ADVANCE );
        setupButton( R.id.buttonGameshark,     InputMap.FUNC_GAMESHARK );
        setupButton( R.id.buttonSimulateBack,  InputMap.FUNC_SIMULATE_BACK );
        setupButton( R.id.buttonSimulateMenu,  InputMap.FUNC_SIMULATE_MENU );
        // @formatter:on
    }
    
    private void setupButton( int resId, int index )
    {
        mN64Buttons[index] = (Button) findViewById( resId );
        if( mN64Buttons[index] != null )
            mN64Buttons[index].setOnClickListener( this );
    }
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.input_map_activity, menu );
        mMenuSpecialVisibility = menu.findItem( R.id.menuItem_specialVisibility );
        refreshSpecialVisibility();
        
        // Hide menu items that do not apply
        mMenuSpecialVisibility.setVisible( !mUserPrefs.isOuyaMode );
        menu.findItem( R.id.menuItem_exit ).setVisible( !mUserPrefs.isOuyaMode );
        menu.findItem( R.id.menuItem_axisInfo ).setVisible( AppData.IS_HONEYCOMB_MR1 );
        
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.menuItem_unmapAll:
                loadProfile( "", item.getTitle() );
                break;
            case R.id.menuItem_default:
                loadProfile( UserPrefs.DEFAULT_INPUT_MAP_STRING, item.getTitle() );
                break;
            case R.id.menuItem_ouya:
                loadProfile( InputMap.DEFAULT_INPUT_MAP_STRING_OUYA, item.getTitle() );
                break;
            case R.id.menuItem_n64Adapter:
                loadProfile( InputMap.DEFAULT_INPUT_MAP_STRING_N64_ADAPTER, item.getTitle() );
                break;
            case R.id.menuItem_ps3:
                loadProfile( InputMap.DEFAULT_INPUT_MAP_STRING_PS3, item.getTitle() );
                break;
            case R.id.menuItem_xbox360:
                loadProfile( InputMap.DEFAULT_INPUT_MAP_STRING_XBOX360, item.getTitle() );
                break;
            case R.id.menuItem_xperiaPlay:
                loadProfile( InputMap.DEFAULT_INPUT_MAP_STRING_XPERIA_PLAY, item.getTitle() );
                break;
            case R.id.menuItem_load:
                loadProfile();
                break;
            case R.id.menuItem_save:
                saveProfile();
                break;
            case R.id.menuItem_specialVisibility:
                mUserPrefs.putSpecialVisibility( mPlayer,
                        !mUserPrefs.getSpecialVisibility( mPlayer ) );
                refreshSpecialVisibility();
                break;
            case R.id.menuItem_axisInfo:
                showAxisInfo();
                break;
            case R.id.menuItem_controllerInfo:
                showControllerInfo();
                break;
            case R.id.menuItem_controllerDiagnostics:
                startActivity( new Intent( this, DiagnosticActivity.class ) );
                break;
            case R.id.menuItem_exit:
                finish();
                break;
            default:
                return false;
        }
        return true;
    }
    
    private void loadProfile( final String mapString, CharSequence profileName )
    {
        CharSequence title = getString( R.string.confirm_title );
        CharSequence message = TextUtils.isEmpty( mapString )
                ? getString( R.string.confirmUnmapAll_message, getTitle() )
                : getString( R.string.confirmLoadProfile_message, profileName, getTitle() );
        
        Prompt.promptConfirm( this, title, message, new OnConfirmListener()
        {
            @Override
            public void onConfirm()
            {
                mMap.deserialize( mapString );
                mUserPrefs.putInputMapString( mPlayer, mMap.serialize() );
                refreshAllButtons();
            }
        } );
    }
    
    private void loadProfile()
    {
        CharSequence title = getText( R.string.menuItem_fileLoad );
        File startPath = new File( mUserPrefs.profileDir );
        
        Prompt.promptFile( this, title, null, startPath, new OnFileListener()
        {
            @Override
            public void onFile( File file, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                    loadProfile( file );
            }
        } );
    }
    
    private void loadProfile( File file )
    {
        try
        {
            mMap.deserialize( FileUtil.readStringFromFile( file ) );
            mUserPrefs.putInputMapString( mPlayer, mMap.serialize() );
            refreshAllButtons();
        }
        catch( IOException e )
        {
            Log.e( "InputMapActivity", "Error loading profile: ", e );
            Notifier.showToast( this, R.string.toast_fileReadError );
        }
    }
    
    private void saveProfile()
    {
        CharSequence title = getText( R.string.menuItem_fileSave );
        CharSequence hint = getText( R.string.hintFileSave );
        int inputType = InputType.TYPE_CLASS_TEXT;
        
        Prompt.promptText( this, title, null, hint, inputType, new OnTextListener()
        {
            @Override
            public void onText( CharSequence text, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    String filename = text.toString();
                    final File file = new File( mUserPrefs.profileDir + "/" + filename );
                    if( file.exists() )
                    {
                        String title = getString( R.string.confirm_title );
                        String message = getString( R.string.confirmOverwriteFile_message, filename );
                        Prompt.promptConfirm( InputMapActivity.this, title, message,
                                new OnConfirmListener()
                                {
                                    @Override
                                    public void onConfirm()
                                    {
                                        saveProfile( file );
                                    }
                                } );
                    }
                    else
                    {
                        saveProfile( file );
                    }
                }
            }
        } );
    }
    
    private void saveProfile( File file )
    {
        try
        {
            Notifier.showToast( this, R.string.toast_savingFile, file.getName() );
            FileUtil.writeStringToFile( file, mMap.serialize() );
        }
        catch( IOException e )
        {
            Log.e( "InputMapActivity", "Error saving profile: ", e );
            Notifier.showToast( this, R.string.toast_fileWriteError );
        }
    }
    
    private void refreshSpecialVisibility()
    {
        boolean specialVisibility = mUserPrefs.getSpecialVisibility( mPlayer );
        
        if( mSpecialFuncsView != null )
        {
            int specialKeyVisibility = specialVisibility ? View.VISIBLE : View.GONE;
            mSpecialFuncsView.setVisibility( specialKeyVisibility );
        }
        
        if( mMenuSpecialVisibility != null )
        {
            mMenuSpecialVisibility.setTitle( specialVisibility
                    ? R.string.menuItem_specialVisibility_hide
                    : R.string.menuItem_specialVisibility_show );
        }
    }
    
    private void showAxisInfo()
    {
        String title = getString( R.string.menuItem_axisInfo );
        String message = DeviceUtil.getAxisInfo();
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    private void showControllerInfo()
    {
        String title = getString( R.string.menuItem_controllerInfo );
        String message = DeviceUtil.getPeripheralInfo();
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id )
    {
        popupListener( mCommandNames[position], mCommandIndices[position] );
    }
    
    @Override
    public void onClick( View view )
    {
        // Handle button clicks in the mapping screen
        for( int i = 0; i < mN64Buttons.length; i++ )
        {
            // Find the button that was pressed
            if( view.equals( mN64Buttons[i] ) )
            {
                // Popup a dialog to listen to input codes from user
                Button button = (Button) view;
                popupListener( button.getText(), i );
            }
        }
    }
    
    private void popupListener( CharSequence title, final int index )
    {
        String message = getString( R.string.inputMapActivity_popupMessage,
                mMap.getMappedCodeInfo( index ) );
        String btnText = getString( R.string.inputMapActivity_popupUnmap );
        
        Prompt.promptInputCode( this, title, message, btnText,
                mUnmappableInputCodes, new OnInputCodeListener()
                {
                    @Override
                    public void OnInputCode( int inputCode, int hardwareId )
                    {
                        if( inputCode == 0 )
                            mMap.unmapCommand( index );
                        else
                            mMap.map( inputCode, index );
                        mUserPrefs.putInputMapString( mPlayer, mMap.serialize() );
                        refreshAllButtons();
                    }
                } );
    }
    
    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event )
    {
        if( mUserPrefs.isOuyaMode )
            return super.onKeyDown( keyCode, event );
        else
            return mKeyProvider.onKey( keyCode, event ) || super.onKeyDown( keyCode, event );
    }
    
    @Override
    public boolean onKeyUp( int keyCode, KeyEvent event )
    {
        if( mUserPrefs.isOuyaMode )
            return super.onKeyUp( keyCode, event );
        else
            return mKeyProvider.onKey( keyCode, event ) || super.onKeyUp( keyCode, event );
    }
    
    @TargetApi( 12 )
    @Override
    public boolean onGenericMotionEvent( MotionEvent event )
    {
        if( !AppData.IS_HONEYCOMB_MR1 )
            return false;
        else if( mUserPrefs.isOuyaMode )
            return super.onGenericMotionEvent( event );
        else
            return mAxisProvider.onGenericMotion( event ) || super.onGenericMotionEvent( event );
    }
    
    @Override
    public void onInput( int inputCode, float strength, int hardwareId )
    {
        refreshButton( inputCode, strength );
        refreshFeedbackText( inputCode, strength );
    }
    
    @Override
    public void onInput( int[] inputCodes, float[] strengths, int hardwareId )
    {
        float maxStrength = AbstractProvider.STRENGTH_THRESHOLD;
        int strongestInputCode = 0;
        for( int i = 0; i < inputCodes.length; i++ )
        {
            int inputCode = inputCodes[i];
            float strength = strengths[i];
            
            // Cache the strongest input
            if( strength > maxStrength )
            {
                maxStrength = strength;
                strongestInputCode = inputCode;
            }
            
            refreshButton( inputCode, strength );
        }
        refreshFeedbackText( strongestInputCode, maxStrength );
    }
    
    private void refreshFeedbackText( int inputCode, float strength )
    {
        // Update the feedback text (not all layouts include this, so check null)
        if( mFeedbackText != null )
        {
            mFeedbackText.setText( strength > AbstractProvider.STRENGTH_THRESHOLD
                    ? AbstractProvider.getInputName( inputCode )
                    : "" );
        }
    }
    
    private void refreshButton( int inputCode, float strength )
    {
        int command = mMap.get( inputCode );
        if( command != InputMap.UNMAPPED )
        {
            Button button = mN64Buttons[command];
            refreshButton( button, strength, true );
        }
    }
    
    @TargetApi( 11 )
    private void refreshButton( Button button, float strength, boolean isMapped )
    {
        if( button != null )
        {
            button.setPressed( strength > AbstractProvider.STRENGTH_THRESHOLD );
            
            // Fade any buttons that aren't mapped
            if( AppData.IS_HONEYCOMB )
            {
                if( isMapped )
                    button.setAlpha( 1 );
                else
                    button.setAlpha( UNMAPPED_BUTTON_ALPHA );
            }
            else
            {
                // For older APIs try something similar (not quite the same)
                if( isMapped )
                    button.getBackground().clearColorFilter();
                else
                    button.getBackground().setColorFilter( UNMAPPED_BUTTON_FILTER,
                            PorterDuff.Mode.MULTIPLY );
                button.invalidate();
            }
        }
    }
    
    private void refreshAllButtons()
    {
        for( int i = 0; i < mN64Buttons.length; i++ )
        {
            refreshButton( mN64Buttons[i], 0, mMap.isMapped( i ) );
        }
        if( mListView != null )
        {
            ArrayAdapter<String> adapter = Prompt.createAdapter( this, Arrays.asList( mCommandNames ),
                    new ListItemTwoTextIconPopulator<String>()
                    {
                        @Override
                        public void onPopulateListItem( String item, int position, TextView text1,
                                TextView text2, ImageView icon )
                        {
                            text1.setText( item );
                            text2.setText( mMap.getMappedCodeInfo( mCommandIndices[position] ) );
                            icon.setVisibility( View.GONE );
                        }
                    } );
            
            mListView.setAdapter( adapter );
        }
    }
}
