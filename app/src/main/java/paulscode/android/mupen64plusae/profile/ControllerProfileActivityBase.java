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

import java.util.List;

import org.mupen64plusae.v3.fzurita.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptIntegerListener;
import paulscode.android.mupen64plusae.dialog.PromptInputCodeDialog;
import paulscode.android.mupen64plusae.dialog.PromptInputCodeDialog.PromptInputCodeListener;
import paulscode.android.mupen64plusae.hack.MogaHack;
import paulscode.android.mupen64plusae.input.InputEntry;
import paulscode.android.mupen64plusae.input.InputStrengthCalculator;
import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider.OnInputListener;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.MogaProvider;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.bda.controller.Controller;

public abstract class ControllerProfileActivityBase extends AppCompatActivity implements OnInputListener, PromptInputCodeListener,
    PromptConfirmListener
{
    public static final String STATE_SELECTED_POPUP_INDEX = "STATE_SELECTED_POPUP_INDEX";
    public static final String STATE_PROMPT_INPUT_CODE_DIALOG = "STATE_PROMPT_INPUT_CODE_DIALOG";
    private static final int UNMAP_ALL_CONFIRM_DIALOG_ID = 0;
    private static final String UNMAP_ALL_CONFIRM_DIALOG_STATE = "UNMAP_ALL_CONFIRM_DIALOG_STATE";

    // Slider limits
    protected static final int MIN_DEADZONE = 0;
    protected static final int MAX_DEADZONE = 20;
    protected static final int MIN_SENSITIVITY = 50;
    protected static final int MAX_SENSITIVITY = 200;
    
    // Visual settings
    protected static final float UNMAPPED_BUTTON_ALPHA = 0.2f;
    protected static final int UNMAPPED_BUTTON_FILTER = 0x66FFFFFF;
    protected static final int MIN_LAYOUT_WIDTH_DP = 480;
    
    // Controller profile objects
    protected ConfigFile mConfigFile;
    protected ControllerProfile mProfile;
    
    // User preferences wrapper
    protected GlobalPrefs mGlobalPrefs;
    
    // Command information
    protected String[] mCommandNames;
    protected int[] mCommandIndices;
    private SparseArray<InputEntry> mEntryMap = new SparseArray<InputEntry>();
    private InputStrengthCalculator mStrengthCalculator;
    
    // Input listening
    protected KeyProvider mKeyProvider;
    protected MogaProvider mMogaProvider;
    protected AxisProvider mAxisProvider;
    protected List<Integer> mUnmappableInputCodes;
    protected Controller mMogaController = Controller.getInstance( this );
    
    // Widgets
    protected final Button[] mN64Buttons = new Button[InputMap.NUM_MAPPABLES];
    protected TextView mFeedbackText;
    protected boolean mExitMenuItemVisible = false;
    
    private int mSelectedPopupIndex = 0;
    
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Initialize MOGA controller API
        // TODO: Remove hack after MOGA SDK is fixed
        // mMogaController.init();
        MogaHack.init( mMogaController, this );
        
        // Get the user preferences wrapper
        AppData appData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, appData );
        mGlobalPrefs.enforceLocale( this );
        
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
        String name = extras.getString( ActivityHelper.Keys.PROFILE_NAME );
        if( TextUtils.isEmpty( name ) )
            throw new Error( "Invalid usage: profile name cannot be null or empty" );
        mConfigFile = new ConfigFile( mGlobalPrefs.controllerProfiles_cfg );
        ConfigSection section = mConfigFile.get( name );
        if( section == null )
            throw new Error( "Invalid usage: profile name not found in config file" );
        mProfile = new ControllerProfile( false, section );
        
        // Set up input listeners
        mUnmappableInputCodes = mGlobalPrefs.unmappableKeyCodes;
        
        if( savedInstanceState != null )
        {
            mSelectedPopupIndex = savedInstanceState.getInt(STATE_SELECTED_POPUP_INDEX);
        }
        
        // Initialize the layout
        initLayout();
        
        // Add the toolbar to the activity (which supports the fancy menu/arrow animation)
        Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );
        
        // Set the title of the activity
        if( TextUtils.isEmpty( mProfile.comment ) )
            toolbar.setTitle( mProfile.name );
        else
            toolbar.setTitle( mProfile.name + " : " + mProfile.comment );
        
        setSupportActionBar( toolbar );
        
        // Refresh everything
        refreshAllButtons(false);
    }
    
    @Override
    public void onSaveInstanceState( Bundle savedInstanceState )
    {
        savedInstanceState.putInt(STATE_SELECTED_POPUP_INDEX, mSelectedPopupIndex);
        
        super.onSaveInstanceState( savedInstanceState );
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
    
    abstract void initLayout();
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.controller_profile_activity, menu );
        menu.findItem( R.id.menuItem_exit ).setVisible( mExitMenuItemVisible );
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
            case R.id.menuItem_sensitivity_x:
                setSensitivityX();
                return true;
            case R.id.menuItem_sensitivity_y:
                setSensitivityY();
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
        
        ConfirmationDialog confirmationDialog =
            ConfirmationDialog.newInstance(UNMAP_ALL_CONFIRM_DIALOG_ID, title.toString(), message.toString());
        
        FragmentManager fm = getSupportFragmentManager();
        confirmationDialog.show(fm, UNMAP_ALL_CONFIRM_DIALOG_STATE);
    }
    
    @Override
    public void onPromptDialogClosed(int id, int which)
    {
        if( id == UNMAP_ALL_CONFIRM_DIALOG_ID &&
            which == DialogInterface.BUTTON_POSITIVE )
        {
            mProfile.putMap( new InputMap() );
            refreshAllButtons(false);
        }
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
    
    private void setSensitivityX()
    {
        final CharSequence title = getText( R.string.menuItem_sensitivity_x );
        
        Prompt.promptInteger( this, title, "%1$d %%", mProfile.getSensitivityX(), MIN_SENSITIVITY,
                MAX_SENSITIVITY, new PromptIntegerListener()
                {
                    @Override
                    public void onDialogClosed( Integer value, int which )
                    {
                        if( which == DialogInterface.BUTTON_POSITIVE )
                        {
                            mProfile.putSensitivityX( value );
                        }
                    }
                } );
    }

    private void setSensitivityY()
    {
        final CharSequence title = getText( R.string.menuItem_sensitivity_y );

        Prompt.promptInteger( this, title, "%1$d %%", mProfile.getSensitivityY(), MIN_SENSITIVITY,
                MAX_SENSITIVITY, new PromptIntegerListener()
                {
                    @Override
                    public void onDialogClosed( Integer value, int which )
                    {
                        if( which == DialogInterface.BUTTON_POSITIVE )
                        {
                            mProfile.putSensitivityY( value );
                        }
                    }
                } );
    }

    protected void popupListener( CharSequence title, final int index )
    {
        final InputMap map = mProfile.getMap();
        mSelectedPopupIndex = index;
        String message = getString( R.string.inputMapActivity_popupMessage,
                map.getMappedCodeInfo( index ) );
        String btnText = getString( R.string.inputMapActivity_popupUnmap );
        
        PromptInputCodeDialog promptInputCodeDialog = PromptInputCodeDialog.newInstance(
            title.toString(), message, btnText, mUnmappableInputCodes);
        
        FragmentManager fm = getSupportFragmentManager();
        promptInputCodeDialog.show(fm, STATE_PROMPT_INPUT_CODE_DIALOG);
    }
    
    @Override
    public void onDialogClosed( int inputCode, int hardwareId, int which )
    {
        if( which != DialogInterface.BUTTON_NEGATIVE )
        {
            InputMap map = mProfile.getMap();
            if( which == DialogInterface.BUTTON_POSITIVE )
                map.map( inputCode, mSelectedPopupIndex );
            else
                map.unmapCommand( mSelectedPopupIndex );
            mProfile.putMap( map );
            refreshAllButtons(true);
        }
        
        // Refresh our MOGA provider since the prompt disconnected it
        mMogaProvider = new MogaProvider( mMogaController );
        mMogaProvider.registerListener( ControllerProfileActivityBase.this );
    }
    
    @Override
    public Controller getMogaController()
    {
        return mMogaController;
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
            InputEntry entry = mEntryMap.get( inputCode );
            
            if( entry != null )
            {
                // Calculate the strength from all possible inputs that map to the control.
                entry.getStrength().set( strength );
                strength = mStrengthCalculator.calculate( entry.mN64Index );
            }
            
            Button button = mN64Buttons[command];
            refreshButton( button, strength, true );
        }
    }
    
    protected void refreshButton( Button button, float strength, boolean isMapped )
    {
        if( button != null )
        {
            button.setPressed(strength > AbstractProvider.STRENGTH_THRESHOLD);

            // Fade any buttons that aren't mapped
            if (isMapped)
                button.setAlpha(1);
            else
                button.setAlpha(UNMAPPED_BUTTON_ALPHA);
        }
    }

    protected void refreshAllButtons(boolean incrementSelection)
    {
        final InputMap map = mProfile.getMap();
        mStrengthCalculator = new InputStrengthCalculator( map, mEntryMap );
        for( int i = 0; i < mN64Buttons.length; i++ )
        {
            refreshButton( mN64Buttons[i], 0, map.isMapped( i ) );
        }
    }
}
