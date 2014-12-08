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
import java.util.List;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.Keys;
import paulscode.android.mupen64plusae.hacks.MogaHack;
import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider.OnInputListener;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.input.provider.MogaProvider;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.ListItemTwoTextIconPopulator;
import paulscode.android.mupen64plusae.util.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.util.Prompt.PromptInputCodeListener;
import paulscode.android.mupen64plusae.util.Prompt.PromptIntegerListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
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

import com.bda.controller.Controller;

public class ControllerProfileActivity extends Activity implements OnInputListener,
        OnClickListener, OnItemClickListener
{
    // Slider limits
    private static final int MIN_DEADZONE = 0;
    private static final int MAX_DEADZONE = 20;
    private static final int MIN_SENSITIVITY = 50;
    private static final int MAX_SENSITIVITY = 200;
    
    // Visual settings
    private static final float UNMAPPED_BUTTON_ALPHA = 0.2f;
    private static final int UNMAPPED_BUTTON_FILTER = 0x66FFFFFF;
    private static final int MIN_LAYOUT_WIDTH_DP = 480;
    
    // Controller profile objects
    private ConfigFile mConfigFile;
    private ControllerProfile mProfile;
    
    // User preferences wrapper
    private UserPrefs mUserPrefs;
    
    // Command information
    private String[] mCommandNames;
    private int[] mCommandIndices;
    
    // Input listening
    private KeyProvider mKeyProvider;
    private MogaProvider mMogaProvider;
    private AxisProvider mAxisProvider;
    private List<Integer> mUnmappableInputCodes;
    private Controller mMogaController = Controller.getInstance( this );
    
    // Widgets
    private final Button[] mN64Buttons = new Button[InputMap.NUM_MAPPABLES];
    private TextView mFeedbackText;
    private ListView mListView;
    
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Initialize MOGA controller API
        // TODO: Remove hack after MOGA SDK is fixed
        // mMogaController.init();
        MogaHack.init( mMogaController, this );
        
        // Get the user preferences wrapper
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
        
        // Get the command info
        mCommandNames = getResources().getStringArray( R.array.inputMapActivity_entries );
        String[] indices = getResources().getStringArray( R.array.inputMapActivity_values );
        mCommandIndices = new int[indices.length];
        for( int i = 0; i < indices.length; i++ )
        {
            mCommandIndices[i] = Integer.parseInt( indices[i] );
        }
        
        // Load the profile; fail fast if there are any programmer usage errors
        Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "Invalid usage: bundle must indicate profile name" );
        String name = extras.getString( Keys.Extras.PROFILE_NAME );
        if( TextUtils.isEmpty( name ) )
            throw new Error( "Invalid usage: profile name cannot be null or empty" );
        mConfigFile = new ConfigFile( mUserPrefs.controllerProfiles_cfg );
        ConfigSection section = mConfigFile.get( name );
        if( section == null )
            throw new Error( "Invalid usage: profile name not found in config file" );
        mProfile = new ControllerProfile( false, section );
        
        // Set up input listeners
        mUnmappableInputCodes = mUserPrefs.unmappableKeyCodes;
        if( !mUserPrefs.isBigScreenMode )
        {
            mKeyProvider = new KeyProvider( ImeFormula.DEFAULT, mUnmappableInputCodes );
            mKeyProvider.registerListener( this );
            mMogaProvider = new MogaProvider( mMogaController );
            mMogaProvider.registerListener( this );
            if( AppData.IS_HONEYCOMB_MR1 )
            {
                mAxisProvider = new AxisProvider();
                mAxisProvider.registerListener( this );
            }
        }
        
        // Set the title of the activity
        if( TextUtils.isEmpty( mProfile.comment ) )
            setTitle( mProfile.name );
        else
            setTitle( mProfile.name + " : " + mProfile.comment );
        
        // Initialize the layout
        if( mUserPrefs.isBigScreenMode )
            initLayoutBigScreenMode();
        else
            initLayoutDefault();
        
        // Refresh everything
        refreshAllButtons();
    }
    
    @Override
    public void onResume()
    {
        super.onResume();
        mMogaController.onResume();
    }
    
    @Override
    public void onPause()
    {
        super.onPause();
        mMogaController.onPause();
        
        // Lazily persist the profile data; only need to do it on pause
        mProfile.writeTo( mConfigFile );
        mConfigFile.save();
    }
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mMogaController.exit();
    }
    
    private void initLayoutBigScreenMode()
    {
        setContentView( R.layout.controller_profile_activity_bigscreen );
        mListView = (ListView) findViewById( R.id.input_map_activity_bigscreen );
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
            setContentView( R.layout.controller_profile_activity_port );
        else
            setContentView( R.layout.controller_profile_activity );
        
        // Initialize and refresh the widgets
        initWidgets();
    }
    
    private void initWidgets()
    {
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
        setupButton( R.id.buttonScreenshot,    InputMap.FUNC_SCREENSHOT );
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
        getMenuInflater().inflate( R.menu.controller_profile_activity, menu );
        menu.findItem( R.id.menuItem_exit ).setVisible( !mUserPrefs.isBigScreenMode );
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.menuItem_unmapAll:
                unmapAll();
                return true;
            case R.id.menuItem_deadzone:
                setDeadzone();
                return true;
            case R.id.menuItem_sensitivity:
                setSensitivity();
                return true;
            case R.id.menuItem_exit:
                finish();
                return true;
            default:
                return false;
        }
    }
    
    private void unmapAll()
    {
        CharSequence title = getString( R.string.confirm_title );
        CharSequence message = getString( R.string.confirmUnmapAll_message, mProfile.name );
        
        Prompt.promptConfirm( this, title, message, new PromptConfirmListener()
        {
            @Override
            public void onConfirm()
            {
                mProfile.putMap( new InputMap() );
                refreshAllButtons();
            }
        } );
    }
    
    private void setDeadzone()
    {
        final CharSequence title = getText( R.string.menuItem_deadzone );
        
        Prompt.promptInteger( this, title, "%1$d %%", mProfile.getDeadzone(), MIN_DEADZONE,
                MAX_DEADZONE, new PromptIntegerListener()
                {
                    @Override
                    public void onDialogClosed( Integer value, int which )
                    {
                        if( which == DialogInterface.BUTTON_POSITIVE )
                        {
                            mProfile.putDeadzone( value );
                        }
                    }
                } );
    }
    
    private void setSensitivity()
    {
        final CharSequence title = getText( R.string.menuItem_sensitivity );
        
        Prompt.promptInteger( this, title, "%1$d %%", mProfile.getSensitivity(), MIN_SENSITIVITY,
                MAX_SENSITIVITY, new PromptIntegerListener()
                {
                    @Override
                    public void onDialogClosed( Integer value, int which )
                    {
                        if( which == DialogInterface.BUTTON_POSITIVE )
                        {
                            mProfile.putSensitivity( value );
                        }
                    }
                } );
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
        final InputMap map = mProfile.getMap();
        String message = getString( R.string.inputMapActivity_popupMessage,
                map.getMappedCodeInfo( index ) );
        String btnText = getString( R.string.inputMapActivity_popupUnmap );
        
        Prompt.promptInputCode( this, mMogaController, title, message, btnText,
                mUnmappableInputCodes, new PromptInputCodeListener()
                {
                    @Override
                    public void onDialogClosed( int inputCode, int hardwareId, int which )
                    {
                        if( which != DialogInterface.BUTTON_NEGATIVE )
                        {
                            if( which == DialogInterface.BUTTON_POSITIVE )
                                map.map( inputCode, index );
                            else
                                map.unmapCommand( index );
                            mProfile.putMap( map );
                            refreshAllButtons();
                        }
                        
                        // Refresh our MOGA provider since the prompt disconnected it
                        mMogaProvider = new MogaProvider( mMogaController );
                        mMogaProvider.registerListener( ControllerProfileActivity.this );
                    }
                } );
    }
    
    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event )
    {
        if( mUserPrefs.isBigScreenMode )
            return super.onKeyDown( keyCode, event );
        else
            return mKeyProvider.onKey( keyCode, event ) || super.onKeyDown( keyCode, event );
    }
    
    @Override
    public boolean onKeyUp( int keyCode, KeyEvent event )
    {
        if( mUserPrefs.isBigScreenMode )
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
        else if( mUserPrefs.isBigScreenMode )
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
        int command = mProfile.getMap().get( inputCode );
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
        final InputMap map = mProfile.getMap();
        for( int i = 0; i < mN64Buttons.length; i++ )
        {
            refreshButton( mN64Buttons[i], 0, map.isMapped( i ) );
        }
        if( mListView != null )
        {
            ArrayAdapter<String> adapter = Prompt.createAdapter( this,
                    Arrays.asList( mCommandNames ), new ListItemTwoTextIconPopulator<String>()
                    {
                        @Override
                        public void onPopulateListItem( String item, int position, TextView text1,
                                TextView text2, ImageView icon )
                        {
                            text1.setText( item );
                            text2.setText( map.getMappedCodeInfo( mCommandIndices[position] ) );
                            icon.setVisibility( View.GONE );
                        }
                    } );
            
            mListView.setAdapter( adapter );
        }
    }
}
