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
package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import android.annotation.TargetApi;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.TextView;

public class InputMapPreference extends DialogPreference implements AbstractProvider.Listener,
        OnClickListener, OnLongClickListener
{
    private static final float STRENGTH_THRESHOLD = 0.5f;
    private static final float STRENGTH_HYSTERESIS = 0.25f;
    
    private InputMap mMap = new InputMap();
    private int mInputCodeToBeMapped = 0;
    private int mLastInputCode = 0;
    private float mLastStrength = 0;
    private float[] mStrengthBiases;
    private View mToggleWidget;
    private TextView mFeedbackText;
    private Button[] mN64Button = new Button[InputMap.NUM_N64INPUTS];
    private KeyProvider mTransform;
    
    public InputMapPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        setDialogLayoutResource( R.layout.input_map_preference );
        setWidgetLayoutResource( R.layout.widget_toggle );
    }
    
    @Override
    protected void onBindView( View view )
    {
        // Set up the menu item seen in the preferences menu
        super.onBindView( view );
        
        // Restore existing state
        mMap.deserialize( getPersistedString( "" ) );
        
        // Get the toggle widget and set its state
        mToggleWidget = view.findViewById( R.id.widgetToggle );
        mToggleWidget.setOnClickListener( this );
        ( (Checkable) mToggleWidget ).setChecked( mMap.isEnabled() );
    }
    
    @TargetApi( 12 )
    @Override
    protected void onBindDialogView( View view )
    {
        // Set up the dialog view seen when the preference menu item is clicked
        super.onBindDialogView( view );
        
        // Restore existing state (might have canceled last dialog)
        mMap.deserialize( getPersistedString( "" ) );
        
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
        
        // Set up input listening
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1 )
        {
            // For Android 3.0 and below, we can only listen to keyboards
            mTransform = new KeyProvider();
        }
        else
        {
            // For Android 3.1 and above, we can also listen to gamepads, mice, etc.
            AxisProvider transform = new AxisProvider();
            
            // Connect the extra upstream end of the transform
            view.setOnGenericMotionListener( transform );
            mTransform = transform;
        }
        
        // Set the formula for decoding special analog IMEs
        mTransform.setImeFormula( ImeFormula.DEFAULT );
        
        // Request focus for proper listening
        view.requestFocus();
        
        // Refresh the dialog view
        updateViews();
    }
    
    @Override
    protected void onPrepareDialogBuilder( Builder builder )
    {
        super.onPrepareDialogBuilder( builder );
        
        // Connect the upstream end of the transform
        builder.setOnKeyListener( mTransform );
        
        // Connect the downstream end of the transform
        mTransform.registerListener( this );
    }
    
    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        // Persist the result if user pressed Ok
        if( positiveResult )
            persistString( mMap.serialize() );
        
        // Refresh the biases next time the dialog opens
        mStrengthBiases = null;
    }
    
    @Override
    public void onInput( int[] inputCodes, float[] strengths )
    {
        // Get strength biases first time through
        boolean refreshBiases = false;
        if( mStrengthBiases == null )
        {
            mStrengthBiases = new float[strengths.length];
            refreshBiases = true;
        }
        
        // Find the strongest input
        float maxStrength = STRENGTH_THRESHOLD;
        int strongestInputCode = 0;
        for( int i = 0; i < inputCodes.length; i++ )
        {
            int inputCode = inputCodes[i];
            float strength = strengths[i];
            
            // Record the strength bias and remove its effect
            if( refreshBiases )
                mStrengthBiases[i] = strength;
            strength -= mStrengthBiases[i];
            
            // Cache the strongest input
            if( strength > maxStrength )
            {
                maxStrength = strength;
                strongestInputCode = inputCode;
            }
        }
        
        // Call the overloaded method with the strongest found
        onInput( strongestInputCode, maxStrength );
    }
    
    @Override
    public void onInput( int inputCode, float strength )
    {
        // Determine the input conditions
        boolean isActive = strength > STRENGTH_THRESHOLD;
        boolean inputChanged = inputCode != mLastInputCode;
        boolean strengthChanged = Math.abs( strength - mLastStrength ) > STRENGTH_HYSTERESIS;
        
        // Cache the input code to be mapped
        if( isActive )
            mInputCodeToBeMapped = inputCode;
        
        // Update the user feedback views
        // To keep the touchscreen responsive, ignore small strength changes
        if( strengthChanged || inputChanged )
        {
            mLastInputCode = inputCode;
            mLastStrength = strength;
            updateViews();
        }
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
        // Else, handle the mapping buttons in the dialog (never unmap, would confuse user)
        else if( mInputCodeToBeMapped != 0 )
        {            
            // Find the button that was touched and map it
            for( int i = 0; i < mN64Button.length; i++ )
            {
                if( view.equals( mN64Button[i] ) )
                    mMap.mapInput( i, mInputCodeToBeMapped );
            }
            
            // Refresh the dialog
            updateViews();
        }
    }
    
    @Override
    public boolean onLongClick( View view )
    {
        // Find the Button view that was long-touched and unmap it
        for( int i = 0; i < mN64Button.length; i++ )
        {
            if( view.equals( mN64Button[i]) )
                mMap.unmapInput( i );
        }
        
        // Refresh the dialog
        updateViews();
        return true;
    }
    
    @TargetApi( 11 )
    private void updateViews()
    {
        // Modify the button appearance to provide feedback to user
        int selectedIndex = mMap.get( mLastInputCode );
        for( int i = 0; i < mN64Button.length; i++ )
        {
            Button button = mN64Button[i];
            
            // Highlight the currently active button
            button.setPressed( i == selectedIndex && mLastStrength > STRENGTH_THRESHOLD );
            
            // Fade any buttons that aren't mapped
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
            {
                if( mMap.getMappedInputCodes()[i] == 0 )
                    button.setAlpha( 0.2f );
                else
                    button.setAlpha( 1 );
            }
        }
        
        // Update the feedback text
        mFeedbackText.setText( AbstractProvider.getInputName( mInputCodeToBeMapped ) );
    }
}
