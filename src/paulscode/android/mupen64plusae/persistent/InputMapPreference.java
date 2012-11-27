/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 * 
 * Concise implementation inspired by
 * http://stackoverflow.com/questions/4505845/concise-way-of-writing-new-dialogpreference-classes
 */
package paulscode.android.mupen64plusae.persistent;

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.input.provider.LazyProvider;
import android.annotation.TargetApi;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.TextView;

public class InputMapPreference extends DialogPreference implements AbstractProvider.OnInputListener,
        OnClickListener, OnLongClickListener
{
    private static final float UNMAPPED_BUTTON_ALPHA = 0.2f;
    
    private final InputMap mMap;
    private final LazyProvider mProvider;
    private int mInputCodeToBeMapped;
    private View mToggleWidget;
    private TextView mFeedbackText;
    private Button[] mN64Button;
    
    public InputMapPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        mMap = new InputMap();
        mProvider = new LazyProvider();
        mProvider.registerListener( this );
        mInputCodeToBeMapped = 0;
        mN64Button = new Button[InputMap.NUM_N64_CONTROLS];
        
        setDialogLayoutResource( R.layout.input_map_preference );
        setWidgetLayoutResource( R.layout.widget_toggle );
    }
    
    @Override
    protected void onBindView( View view )
    {
        // Set up the menu item seen in the preferences menu
        super.onBindView( view );
        
        // Restore the persisted map
        mMap.deserialize( getPersistedString( "" ) );
        
        // Get the toggle widget and set its state
        mToggleWidget = view.findViewById( R.id.widgetToggle );
        mToggleWidget.setOnClickListener( this );
        ( (Checkable) mToggleWidget ).setChecked( mMap.isEnabled() );
    }
    
    @Override
    protected void onBindDialogView( View view )
    {
        // Set up the dialog view seen when the preference menu item is clicked
        super.onBindDialogView( view );
        
        // Get the text view object
        mFeedbackText = (TextView) view.findViewById( R.id.textFeedback );
        
        // Create a button map to simplify highlighting
        mN64Button[AbstractController.DPD_R] = (Button) view.findViewById( R.id.buttonDR );
        mN64Button[AbstractController.DPD_L] = (Button) view.findViewById( R.id.buttonDL );
        mN64Button[AbstractController.DPD_D] = (Button) view.findViewById( R.id.buttonDD );
        mN64Button[AbstractController.DPD_U] = (Button) view.findViewById( R.id.buttonDU );
        mN64Button[AbstractController.START] = (Button) view.findViewById( R.id.buttonS );
        mN64Button[AbstractController.BTN_Z] = (Button) view.findViewById( R.id.buttonZ );
        mN64Button[AbstractController.BTN_B] = (Button) view.findViewById( R.id.buttonB );
        mN64Button[AbstractController.BTN_A] = (Button) view.findViewById( R.id.buttonA );
        mN64Button[AbstractController.CPD_R] = (Button) view.findViewById( R.id.buttonCR );
        mN64Button[AbstractController.CPD_L] = (Button) view.findViewById( R.id.buttonCL );
        mN64Button[AbstractController.CPD_D] = (Button) view.findViewById( R.id.buttonCD );
        mN64Button[AbstractController.CPD_U] = (Button) view.findViewById( R.id.buttonCU );
        mN64Button[AbstractController.BTN_R] = (Button) view.findViewById( R.id.buttonR );
        mN64Button[AbstractController.BTN_L] = (Button) view.findViewById( R.id.buttonL );
        mN64Button[InputMap.AXIS_R] = (Button) view.findViewById( R.id.buttonAR );
        mN64Button[InputMap.AXIS_L] = (Button) view.findViewById( R.id.buttonAL );
        mN64Button[InputMap.AXIS_D] = (Button) view.findViewById( R.id.buttonAD );
        mN64Button[InputMap.AXIS_U] = (Button) view.findViewById( R.id.buttonAU );
        mN64Button[InputMap.BTN_RUMBLE] = (Button) view.findViewById( R.id.buttonRumble );
        mN64Button[InputMap.BTN_MEMPAK] = (Button) view.findViewById( R.id.buttonMempak );
        
        // Define the button click callbacks
        for( Button b : mN64Button )
        {
            b.setOnClickListener( this );
            b.setOnLongClickListener( this );
        }
        
        // Setup analog axis listening
        if( Globals.IS_HONEYCOMB_MR1 )
            mProvider.addProvider( new AxisProvider( view ) );
        
        // Refresh the dialog view
        updateViews();
    }
    
    @Override
    protected void onPrepareDialogBuilder( Builder builder )
    {
        super.onPrepareDialogBuilder( builder );
        
        // Setup key listening
        mProvider.addProvider( new KeyProvider( builder, ImeFormula.DEFAULT ) );
        
        // TODO: Fix this temporary solution to provide more screen space
        builder.setTitle( null );
    }
    
    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        // Clicking Cancel or Ok returns us to the parent preference menu. We must return to a clean
        // state so that the toggle doesn't persist unwanted changes.
        
        if( positiveResult )
            // User pressed Ok: clean the state by persisting map
            persistString( mMap.serialize() );
        else
            // User pressed Cancel/Back: clean the state by restoring map
            mMap.deserialize( getPersistedString( "" ) );
        
        // Unregister parent providers, new ones added on next click
        mProvider.removeAllProviders();
    }
    
    @Override
    public void onClick( View view )
    {
        // Handle the toggle button in the preferences menu
        if( view.equals( mToggleWidget ) )
        {
            boolean isEnabled = ( (Checkable) mToggleWidget ).isChecked();
            mMap.setEnabled( isEnabled );
            persistString( mMap.serialize() );
        }
        
        // Else, find the button that was clicked and map it
        // (never unmap, would confuse user)
        else if( mInputCodeToBeMapped != 0 )
        {
            // Find the button that was touched and map it
            for( int i = 0; i < mN64Button.length; i++ )
            {
                if( view.equals( mN64Button[i] ) )
                {
                    mMap.mapInput( i, mInputCodeToBeMapped );
                    mInputCodeToBeMapped = 0;
                    break;
                }
            }
            
            // Refresh the dialog
            updateViews();
        }
    }
    
    @Override
    public boolean onLongClick( View view )
    {
        // Find the button that was long-clicked and unmap it
        for( int i = 0; i < mN64Button.length; i++ )
        {
            if( view.equals( mN64Button[i] ) )
                mMap.unmapInput( i );
        }
        
        // Refresh the dialog
        updateViews();
        return true;
    }
    
    @Override
    public void onInput( int inputCode, float strength, int hardwareId )
    {
        mInputCodeToBeMapped = mProvider.getActiveCode();
        updateViews( inputCode, strength );
    }
    
    @Override
    public void onInput( int[] inputCodes, float[] strengths, int hardwareId )
    {
        // Nothing to do here, just implements the interface
    }
    
    @TargetApi( 11 )
    private void updateViews( int inputCode, float strength )
    {
        // Modify the button appearance to provide feedback to user
        int selectedIndex = mMap.get( inputCode );
        for( int i = 0; i < mN64Button.length; i++ )
        {
            // Highlight the currently active button
            Button button = mN64Button[i];
            button.setPressed( i == selectedIndex && strength > AbstractProvider.STRENGTH_THRESHOLD );
            
            // Fade any buttons that aren't mapped
            // TODO: provide alternative for lower APIs
            if( Globals.IS_HONEYCOMB )
            {
                if( mMap.getMappedInputCodes()[i] == 0 )
                    button.setAlpha( UNMAPPED_BUTTON_ALPHA );
                else
                    button.setAlpha( 1 );
            }
        }
        
        // Update the feedback text
        mFeedbackText.setText( AbstractProvider.getInputName( mInputCodeToBeMapped ) );
    }
    
    private void updateViews()
    {
        // Default update, don't highlight anything
        updateViews( 0, 0 );
    }
}
