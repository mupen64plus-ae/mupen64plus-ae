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
    private InputMap mInputMap;
    private KeyTransform mTransform;
    
    @TargetApi( 12 )
    public PeripheralController( View view, InputMap inputMap, ImeFormula formula )
    {
        // Assign the map and listen for changes
        mInputMap = inputMap;
        if( mInputMap != null )
        {
            onMapChanged( mInputMap );
            mInputMap.registerListener( this );
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
        
        // Connect the upstream end of the transform
        view.setOnKeyListener( mTransform );
        
        // Connect the downstream end of the transform
        mTransform.registerListener( this );
    }
    
    public KeyTransform getTransform()
    {
        return mTransform;
    }
    
    @Override
    public void onInput( int inputCode, float strength )
    {
        // Process user inputs from keyboard, gamepad, etc.
        if( mInputMap != null )
        {
            mInputMap.apply( inputCode, strength, this );
            notifyChanged();
        }
    }
    
    @Override
    public void onInput( int[] inputCodes, float[] strengths )
    {
        // Process batch user inputs from gamepad, keyboard, etc.
        if( mInputMap != null )
        {
            for( int i = 0; i < inputCodes.length; i++ )
                mInputMap.apply( inputCodes[i], strengths[i], this );
            notifyChanged();
        }
    }
    
    @Override
    public void onMapChanged( InputMap map )
    {
        // If the button/axis mappings change, update the transform's listening filter
        if( mTransform != null && mTransform instanceof KeyAxisTransform )
        {
            ( (KeyAxisTransform) mTransform ).setInputCodeFilter( map.getMappedInputCodes() );
        }
    }
}
