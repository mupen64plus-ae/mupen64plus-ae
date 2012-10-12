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

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;

public abstract class InputTranslator
{
    public interface Listener
    {
        public void onInput( int inputCode, float strength );
        
        public void onInput( int[] inputCodes, float[] strengths );
    }
    
    private List<InputTranslator.Listener> mListeners;
    
    public InputTranslator()
    {
        mListeners = new ArrayList<InputTranslator.Listener>();
    }
    
    public void registerListener( InputTranslator.Listener listener )
    {
        if( ( listener != null ) && !mListeners.contains( listener ) )
            mListeners.add( listener );
    }
    
    public void unregisterListener( InputTranslator.Listener listener )
    {
        if( listener != null )
            while( mListeners.remove( listener ) )
                ;
    }
    
    @TargetApi( 12 )
    public static String getInputName( int inputCode )
    {
        // TODO: Localize strings.
        if( inputCode > 0 )
        {
            if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1 )
                return "KEY " + inputCode;
            else
                return KeyEvent.keyCodeToString( inputCode );
        }
        else if( inputCode < 0 )
        {
            int axis = inputToAxisCode( inputCode );
            String direction = inputToAxisDirection( inputCode ) ? " (+)" : " (-)";
            if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1 )
                return "AXIS " + axis + direction;
            else
                return MotionEvent.axisToString( axis ) + direction;
        }
        else
            return "NULL";
    }
    
    public static String getInputName( int inputCode, float strength )
    {
        return getInputName( inputCode ) + String.format( " %.3f", strength );
    }
    
    protected void notifyListeners( int inputCode, float strength )
    {
        for( Listener listener : mListeners )
            listener.onInput( inputCode, strength );
    }
    
    protected void notifyListeners( int[] inputCodes, float[] strengths )
    {
        for( Listener listener : mListeners )
            listener.onInput( inputCodes.clone(), strengths.clone() );
    }
    
    protected static int axisToInputCode( int axisCode, boolean positiveDirection )
    {
        // Axis codes are encoded to negative values (versus buttons which are
        // positive). Axis codes are bit shifted by one so that the lowest bit
        // can encode axis direction.
        return -( ( axisCode ) * 2 + ( positiveDirection ? 1 : 2 ) );
    }
    
    protected static int inputToAxisCode( int inputCode )
    {
        return ( -inputCode - 1 ) / 2;
    }
    
    protected static boolean inputToAxisDirection( int inputCode )
    {
        return ( ( -inputCode ) % 2 ) == 1;
    }
}
