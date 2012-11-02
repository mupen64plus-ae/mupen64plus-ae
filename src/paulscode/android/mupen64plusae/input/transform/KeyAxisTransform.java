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
package paulscode.android.mupen64plusae.input.transform;

import android.annotation.TargetApi;
import android.view.MotionEvent;
import android.view.View;

@TargetApi( 12 )
public class KeyAxisTransform extends KeyTransform implements View.OnGenericMotionListener
{
    private int[] mInputCodes;
    
    private static final int DEFAULT_NUM_INPUTS = 128;
    
    public KeyAxisTransform()
    {
        // By default, listen to all possible gamepad axes
        mInputCodes = new int[DEFAULT_NUM_INPUTS];
        for( int i = 0; i < mInputCodes.length; i++ )
            mInputCodes[i] = -( i + 1 );
    }
    
    public void setInputCodeFilter( int[] inputCodeFilter )
    {
        mInputCodes = inputCodeFilter.clone();
    }
    
    @Override
    public boolean onGenericMotion( View v, MotionEvent event )
    {
        // Read all the requested axes
        float[] strengths = new float[mInputCodes.length];
        for( int i = 0; i < mInputCodes.length; i++ )
        {
            int inputCode = mInputCodes[i];
            
            // Compute the axis code from the input code
            int axisCode = inputToAxisCode( inputCode );
            
            // Get the analog value using the native Android API
            float strength = event.getAxisValue( axisCode );
            
            // If the strength points in the correct direction, record it
            boolean direction1 = inputToAxisDirection( inputCode );
            boolean direction2 = strength > 0;
            if( direction1 == direction2 )
                strengths[i] = Math.abs( strength );
            else
                strengths[i] = 0;
        }
        
        // Notify listeners of input data
        notifyListeners( mInputCodes, strengths );
        
        return true;
    }
}
