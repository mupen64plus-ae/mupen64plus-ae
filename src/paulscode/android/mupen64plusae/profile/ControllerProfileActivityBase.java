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

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptInputCodeListener;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptIntegerListener;
import paulscode.android.mupen64plusae.hack.MogaHack;
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
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.bda.controller.Controller;

public abstract class ControllerProfileActivityBase extends AppCompatActivity implements OnInputListener,
        OnClickListener, OnItemClickListener
{
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
        
        // Set the title of the activity
        if( TextUtils.isEmpty( mProfile.comment ) )
            setTitle( mProfile.name );
        else
            setTitle( mProfile.name + " : " + mProfile.comment );
        
        // Initialize the layout
        initLayout();
        
        // Refresh everything
        refreshAllButtons(false);
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
                refreshAllButtons(false);
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
                            refreshAllButtons(true);
                        }
                        
                        // Refresh our MOGA provider since the prompt disconnected it
                        mMogaProvider = new MogaProvider( mMogaController );
                        mMogaProvider.registerListener( ControllerProfileActivityBase.this );
                    }
                } );
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
    protected void refreshButton( Button button, float strength, boolean isMapped )
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
    
    abstract void refreshAllButtons(boolean incrementSelection);
}
