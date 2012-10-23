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
import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;

public class GamepadController extends AbstractController implements AbstractTransform.Listener,
        InputMap.Listener
{
    private KeyTransform mTranslator;
    private InputMap mInputMap;
    
    @TargetApi( 12 )
    public GamepadController( View view )
    {
        // Set up input listening
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1 )
        {
            // For Android 3.0 and below, we can only listen to keyboards
            KeyTransform translator = new KeyTransform();
            
            // Connect the upstream end of the translator
            view.setOnKeyListener( translator );
            mTranslator = translator;
        }
        else
        {
            // For Android 3.1 and above, we can also listen to gamepads, mice, etc.
            KeyAxisTransform translator = new KeyAxisTransform();
            
            // Connect the upstream end of the translator
            view.setOnKeyListener( translator );
            view.setOnGenericMotionListener( translator );
            mTranslator = translator;
        }
        
        // Connect the downstream end of the translator
        mTranslator.registerListener( this );
        
        // Set the formula for decoding special analog IMEs
        mTranslator.setImeFormula( KeyTransform.ImeFormula.DEFAULT );
    }
    
    public InputMap getInputMap()
    {
        return mInputMap;
    }
    
    public void setInputMap( InputMap inputMap )
    {
        if( mInputMap != null )
            mInputMap.unregisterListener( this );
        mInputMap = inputMap;
        if( mInputMap != null )
            mInputMap.registerListener( this );
    }
    
    public KeyTransform.ImeFormula getImeFormula()
    {
        if( mTranslator != null )
            return mTranslator.getImeFormula();
        else
            return KeyTransform.ImeFormula.DEFAULT;
    }
    
    public void setImeFormula( KeyTransform.ImeFormula imeFormula )
    {
        if( mTranslator != null )
            mTranslator.setImeFormula( imeFormula );
    }
    
    public void onInput( int inputCode, float strength )
    {
        if( mInputMap != null )
        {
            mInputMap.apply( inputCode, strength, this );
            notifyChanged();
        }
    }
    
    public void onInput( int[] inputCodes, float[] strengths )
    {
        if( mInputMap != null )
        {
            for( int i = 0; i < inputCodes.length; i++ )
                mInputMap.apply( inputCodes[i], strengths[i], this );
            notifyChanged();
        }
    }
    
    public void onMapChanged( InputMap newValue )
    {
        if( mTranslator != null && mTranslator instanceof KeyAxisTransform )
        {
            ( (KeyAxisTransform) mTranslator ).setInputCodeFilter( newValue.getMappedInputCodes() );
        }
    }
}
