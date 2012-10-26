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
 * Override implementations inspired by
 * http://stackoverflow.com/questions/4505845/concise-way-of-writing-new-dialogpreference-classes
 */
package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.input.InputMap;
import paulscode.android.mupen64plusae.input.transform.AbstractTransform;
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
import android.widget.Button;
import android.widget.TextView;

public class InputMapPreference extends DialogPreference implements AbstractTransform.Listener,
        OnClickListener
{
    private static final float STRENGTH_ACTIVE_THRESHOLD = 0.1f;
    private static final float STRENGTH_BIAS_THRESHOLD = 0.9f;
    
    private InputMap mMap = new InputMap();
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
        
        // Create a button map to simplify highlighting
        mN64ToButton[InputMap.DPD_R] = (Button) view.findViewById( R.id.buttonDR );
        mN64ToButton[InputMap.DPD_L] = (Button) view.findViewById( R.id.buttonDL );
        mN64ToButton[InputMap.DPD_D] = (Button) view.findViewById( R.id.buttonDD );
        mN64ToButton[InputMap.DPD_U] = (Button) view.findViewById( R.id.buttonDU );
        mN64ToButton[InputMap.START] = (Button) view.findViewById( R.id.buttonS );
        mN64ToButton[InputMap.BTN_Z] = (Button) view.findViewById( R.id.buttonZ );
        mN64ToButton[InputMap.BTN_B] = (Button) view.findViewById( R.id.buttonB );
        mN64ToButton[InputMap.BTN_A] = (Button) view.findViewById( R.id.buttonA );
        mN64ToButton[InputMap.CPD_R] = (Button) view.findViewById( R.id.buttonCR );
        mN64ToButton[InputMap.CPD_L] = (Button) view.findViewById( R.id.buttonCL );
        mN64ToButton[InputMap.CPD_D] = (Button) view.findViewById( R.id.buttonCD );
        mN64ToButton[InputMap.CPD_U] = (Button) view.findViewById( R.id.buttonCU );
        mN64ToButton[InputMap.BTN_R] = (Button) view.findViewById( R.id.buttonL );
        mN64ToButton[InputMap.BTN_L] = (Button) view.findViewById( R.id.buttonR );
        mN64ToButton[InputMap.AXIS_R] = (Button) view.findViewById( R.id.buttonAR );
        mN64ToButton[InputMap.AXIS_L] = (Button) view.findViewById( R.id.buttonAL );
        mN64ToButton[InputMap.AXIS_D] = (Button) view.findViewById( R.id.buttonAD );
        mN64ToButton[InputMap.AXIS_U] = (Button) view.findViewById( R.id.buttonAU );
        mFeedbackText = (TextView) view.findViewById( R.id.textFeedback );
        mFeedbackText.requestFocus();
        
        // Define the button click callbacks
        for( Button b : mN64ToButton )
            b.setOnClickListener( this );
        
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
        
        // Initialize the feedback elements
        provideFeedback( 0, 0 );
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
    public void onClick( View v )
    {
        // Find the Button that was clicked and map it
        for( int i = 0; i < mN64ToButton.length; i++ )
            if( mN64ToButton[i] == v )
                mMap.mapInput( mLastInputCode, i );
        
        // Provide visual feedback to user
        provideFeedback( mLastInputCode, mLastStrength );
    }
    
    @Override
    public void onInput( int inputCode, float strength )
    {
        // Cache the input code and strength
        mLastInputCode = inputCode;
        mLastStrength = strength;
        
        // Provide visual feedback to user
        provideFeedback( mLastInputCode, mLastStrength );
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
        
        // Cache the strongest input code and strength
        mLastStrength = STRENGTH_ACTIVE_THRESHOLD;
        mLastInputCode = 0;
        for( int i = 0; i < inputCodes.length; i++ )
        {
            int inputCode = inputCodes[i];
            float strength = strengths[i];
            
            // Record the strength bias and remove its effect
            if( refreshBiases )
                mStrengthBiases[i] = strength > STRENGTH_BIAS_THRESHOLD ? 1 : 0;
            strength -= mStrengthBiases[i];
            
            // Find strongest input and cache it
            if( strength > mLastStrength )
            {
                mLastStrength = strength;
                mLastInputCode = inputCode;
            }
        }
        
        // Provide visual feedback to user
        provideFeedback( mLastInputCode, mLastStrength );
    }
    
    @TargetApi( 11 )
    private void provideFeedback( int inputCode, float strength )
    {
        // Modify the button appearance to signify things
        int selectedIndex = mMap.get( inputCode );
        for( int i = 0; i < mN64ToButton.length; i++ )
        {
            Button button = mN64ToButton[i];
            
            // Highlight the currently active button
            button.setPressed( i == selectedIndex && strength > STRENGTH_ACTIVE_THRESHOLD );
            
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
        mFeedbackText.setText( AbstractTransform.getInputName( inputCode ) );
    }
}
