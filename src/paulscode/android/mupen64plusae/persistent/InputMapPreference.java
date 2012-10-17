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
 *   http://stackoverflow.com/questions/4505845/concise-way-of-writing-new-dialogpreference-classes
 */
package paulscode.android.mupen64plusae.persistent;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.input.InputMap;
import paulscode.android.mupen64plusae.input.InputTranslator;
import paulscode.android.mupen64plusae.input.KeyAxisTranslator;
import paulscode.android.mupen64plusae.input.KeyTranslator;
import paulscode.android.mupen64plusae.input.KeyTranslator.ImeFormula;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class InputMapPreference extends DialogPreference implements InputTranslator.Listener,
        OnClickListener
{
    private static final float STRENGTH_THRESHOLD = 0.1f;
    private static final float INITIAL_THRESHOLD = 0.9f;
    
    private InputMap mMap = new InputMap();
    private int mLastInputCode = 0;
    private float mLastStrength = 0;
    private float[] mInitialStrengths;
    private Button[] mN64ToButton = new Button[InputMap.NUM_INPUTS];
    private TextView mFeedbackText;
    private InputTranslator mTranslator;
    
    public InputMapPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        setDialogLayoutResource( R.layout.preferences_inputmap );
    }
    
    @TargetApi( 12 )
    @Override
    protected void onBindDialogView( View view )
    {
        super.onBindDialogView( view );
        
        // Restore existing state
        mMap.deserialize( getPersistedString( "" ) );

        // Create a widget map to simplify button highlighting
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
        
        for( Button b : mN64ToButton )
            b.setOnClickListener( this );
        
        // Set up input listening
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1 )
        {
            // For Android 3.0 and below, we can only listen to keyboards
            KeyTranslator translator = new KeyTranslator();
            
            // Set the formula for decoding special analog IMEs
            translator.setImeFormula( ImeFormula.DEFAULT );
            
            // Connect the upstream end of the funnel
            view.setOnKeyListener( translator );
            mTranslator = translator;
        }
        else
        {
            // For Android 3.1 and above, we can also listen to gamepads, mice, etc.
            KeyAxisTranslator translator = new KeyAxisTranslator();
            
            // Set the formula for decoding special analog IMEs
            translator.setImeFormula( ImeFormula.DEFAULT );
            
            // Connect the upstream end of the funnel
            view.setOnKeyListener( translator );
            view.setOnGenericMotionListener( translator );
            mTranslator = translator;
        }
        
        // Connect the downstream end of the funnel
        mTranslator.registerListener( this );
    }

    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        if( positiveResult )
            persistString( mMap.serialize() );
    }
    
    public void onClick( View v )
    {
        // Cache the current code (in case another thread overwrites it)
        int lastInputCode = mLastInputCode;
        
        // Find the button that was pressed and map it
        for( int i = 0; i < mN64ToButton.length; i++ )
            if( mN64ToButton[i] == v )
                mMap.mapInput( lastInputCode, i );
    }
    
    public void onInput( int inputCode, float strength )
    {
        // Cache the input code and strength
        mLastInputCode = inputCode;
        mLastStrength = strength;
        highlightButton( mLastInputCode, mLastStrength, true );
        mFeedbackText.setText( InputTranslator.getInputName( mLastInputCode ) );
    }
    
    public void onInput( int[] inputCodes, float[] strengths )
    {
        // Get initial strengths first time through
        boolean initialize = false;
        if( mInitialStrengths == null )
        {
            mInitialStrengths = new float[strengths.length];
            initialize = true;
        }
        
        // Cache the strongest input code and strength
        mLastStrength = STRENGTH_THRESHOLD;
        mLastInputCode = 0;
        for( int i = 0; i < inputCodes.length; i++ )
        {
            int inputCode = inputCodes[i];
            float strength = strengths[i];
            
            // Record the initial strength and remove its effect
            if( initialize )
                mInitialStrengths[i] = strength > INITIAL_THRESHOLD ? 1
                        : strength < -INITIAL_THRESHOLD ? -1 : 0;
            strength -= mInitialStrengths[i];
            
            // Highlight the corresponding button if mapped
            highlightButton( inputCode, strength, false );
            
            // Find strongest input and cache it
            if( strength > mLastStrength )
            {
                mLastStrength = strength;
                mLastInputCode = inputCode;
            }
        }
        highlightButton( mLastInputCode, mLastStrength, true );
        mFeedbackText.setText( InputTranslator.getInputName( mLastInputCode ) );
    }
    
    private void highlightButton( int inputCode, float strength, boolean isActive )
    {
        int n64Index = mMap.get( inputCode );
        if( n64Index != InputMap.UNMAPPED )
        {
            Button button = mN64ToButton[n64Index];
            button.setPressed( isActive && strength > STRENGTH_THRESHOLD );
        }
    }
}
