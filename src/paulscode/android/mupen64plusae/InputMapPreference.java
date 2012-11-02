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
import paulscode.android.mupen64plusae.input.transform.AbstractTransform;
import paulscode.android.mupen64plusae.input.transform.InputMap;
import paulscode.android.mupen64plusae.input.transform.KeyAxisTransform;
import paulscode.android.mupen64plusae.input.transform.KeyTransform;
import paulscode.android.mupen64plusae.input.transform.KeyTransform.ImeFormula;
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
import android.widget.TextView;

public class InputMapPreference extends DialogPreference implements AbstractTransform.Listener,
        OnClickListener, OnLongClickListener
{
    private static final float STRENGTH_THRESHOLD = 0.5f;
    private static final float STRENGTH_HYSTERESIS = 0.25f;
    
    private InputMap mMap = new InputMap();
    private int mInputCodeToBeMapped = 0;
    private int mLastInputCode = 0;
    private float mLastStrength = 0;
    private float[] mStrengthBiases;
    private Button[] mN64ToButton = new Button[InputMap.NUM_INPUTS];
    private TextView mFeedbackText;
    private KeyTransform mTransform;
    
    public InputMapPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        setDialogLayoutResource( R.layout.input_map_preference );
    }
    
    @TargetApi( 12 )
    @Override
    protected void onBindDialogView( View view )
    {
        // Our first guaranteed opportunity to access a non-null View
        super.onBindDialogView( view );
        
        // Restore existing state
        mMap.deserialize( getPersistedString( "" ) );
        
        // Get the textview object
        mFeedbackText = (TextView) view.findViewById( R.id.textFeedback );
        
        // Create a button map to simplify highlighting
        mN64ToButton[AbstractController.DPD_R] = (Button) view.findViewById( R.id.buttonDR );
        mN64ToButton[AbstractController.DPD_L] = (Button) view.findViewById( R.id.buttonDL );
        mN64ToButton[AbstractController.DPD_D] = (Button) view.findViewById( R.id.buttonDD );
        mN64ToButton[AbstractController.DPD_U] = (Button) view.findViewById( R.id.buttonDU );
        mN64ToButton[AbstractController.START] = (Button) view.findViewById( R.id.buttonS );
        mN64ToButton[AbstractController.BTN_Z] = (Button) view.findViewById( R.id.buttonZ );
        mN64ToButton[AbstractController.BTN_B] = (Button) view.findViewById( R.id.buttonB );
        mN64ToButton[AbstractController.BTN_A] = (Button) view.findViewById( R.id.buttonA );
        mN64ToButton[AbstractController.CPD_R] = (Button) view.findViewById( R.id.buttonCR );
        mN64ToButton[AbstractController.CPD_L] = (Button) view.findViewById( R.id.buttonCL );
        mN64ToButton[AbstractController.CPD_D] = (Button) view.findViewById( R.id.buttonCD );
        mN64ToButton[AbstractController.CPD_U] = (Button) view.findViewById( R.id.buttonCU );
        mN64ToButton[AbstractController.BTN_R] = (Button) view.findViewById( R.id.buttonR );
        mN64ToButton[AbstractController.BTN_L] = (Button) view.findViewById( R.id.buttonL );
        mN64ToButton[InputMap.AXIS_R] = (Button) view.findViewById( R.id.buttonAR );
        mN64ToButton[InputMap.AXIS_L] = (Button) view.findViewById( R.id.buttonAL );
        mN64ToButton[InputMap.AXIS_D] = (Button) view.findViewById( R.id.buttonAD );
        mN64ToButton[InputMap.AXIS_U] = (Button) view.findViewById( R.id.buttonAU );

        // Define the button click callbacks
        for( Button b : mN64ToButton )
        {
            b.setOnClickListener( this );
            b.setOnLongClickListener( this );
        }
        
        // Set up input listening
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1 )
        {
            // For Android 3.0 and below, we can only listen to keyboards
            mTransform = new KeyTransform();
        }
        else
        {
            // For Android 3.1 and above, we can also listen to gamepads, mice, etc.
            KeyAxisTransform transform = new KeyAxisTransform();
            
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
        boolean strengthChanged = Math.abs(strength - mLastStrength) > STRENGTH_HYSTERESIS;
        
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
    public void onClick( View v )
    {
        // Find the Button view that was touched and map it
        for( int i = 0; i < mN64ToButton.length; i++ )
        {
            if( mN64ToButton[i] == v )
                mMap.mapInput( i, mInputCodeToBeMapped );
        }
        
        // Refresh the dialog
        updateViews();
    }
    
    @Override
    public boolean onLongClick( View view )
    {
        // Find the Button view that was long-touched and unmap it
        for( int i = 0; i < mN64ToButton.length; i++ )
        {
            if( mN64ToButton[i] == view )
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
        for( int i = 0; i < mN64ToButton.length; i++ )
        {
            Button button = mN64ToButton[i];
            
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
        mFeedbackText.setText( AbstractTransform.getInputName( mInputCodeToBeMapped ) );
    }
}
