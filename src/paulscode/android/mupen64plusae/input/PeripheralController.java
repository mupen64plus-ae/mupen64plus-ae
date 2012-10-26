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
 */
package paulscode.android.mupen64plusae.input;

import paulscode.android.mupen64plusae.input.transform.AbstractTransform;
import paulscode.android.mupen64plusae.input.transform.KeyAxisTransform;
import paulscode.android.mupen64plusae.input.transform.KeyTransform;
import paulscode.android.mupen64plusae.input.transform.KeyTransform.ImeFormula;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;

public class PeripheralController extends AbstractController implements AbstractTransform.Listener,
        InputMap.Listener
{
    private KeyTransform mTransform;
    private InputMap mInputMap;
    
    @TargetApi( 12 )
    public PeripheralController( View view )
    {
        // Set up input listening
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1 )
        {
            // For Android 3.0 and below, we can only listen to keyboards
            KeyTransform transform = new KeyTransform();
            
            // Connect the upstream end of the transform
            view.setOnKeyListener( transform );
            mTransform = transform;
        }
        else
        {
            // For Android 3.1 and above, we can also listen to gamepads, mice, etc.
            KeyAxisTransform transform = new KeyAxisTransform();
            
            // Connect the upstream end of the transform
            view.setOnKeyListener( transform );
            view.setOnGenericMotionListener( transform );
            mTransform = transform;
        }
        
        // Set the formula for decoding special analog IMEs
        mTransform.setImeFormula( ImeFormula.DEFAULT );
        
        // Connect the downstream end of the transform
        mTransform.registerListener( this );
        
        // Request focus for proper listening
        view.requestFocus();
    }
    
    public InputMap getInputMap()
    {
        // Get the button/axis mappings
        return mInputMap;
    }
    
    public void setInputMap( InputMap inputMap )
    {
        // Replace the button/axis mappings
        if( mInputMap != null )
            mInputMap.unregisterListener( this );
        mInputMap = inputMap;
        onMapChanged( mInputMap );
        if( mInputMap != null )
            mInputMap.registerListener( this );
    }
    
    public KeyTransform.ImeFormula getImeFormula()
    {
        // Get the IME keycode->analog formula
        if( mTransform != null )
            return mTransform.getImeFormula();
        else
            return KeyTransform.ImeFormula.DEFAULT;
    }
    
    public void setImeFormula( KeyTransform.ImeFormula imeFormula )
    {
        // Set the IME keycode->analog formula
        if( mTransform != null )
            mTransform.setImeFormula( imeFormula );
    }
    
    /**
     * {@inheritDoc}
     */
    public void onInput( int inputCode, float strength )
    {
        // Process user inputs from keyboard, gamepad, etc.
        if( mInputMap != null )
        {
            mInputMap.apply( inputCode, strength, this );
            notifyChanged();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void onInput( int[] inputCodes, float[] strengths )
    {
        // Process batch user inputs from keyboard, gamepad, etc.
        if( mInputMap != null )
        {
            for( int i = 0; i < inputCodes.length; i++ )
                mInputMap.apply( inputCodes[i], strengths[i], this );
            notifyChanged();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void onMapChanged( InputMap newValue )
    {
        // If the button/axis mappings change, update the transform's listening filter
        if( mTransform != null && mTransform instanceof KeyAxisTransform )
        {
            ( (KeyAxisTransform) mTransform ).setInputCodeFilter( newValue.getMappedInputCodes() );
        }
    }
}
