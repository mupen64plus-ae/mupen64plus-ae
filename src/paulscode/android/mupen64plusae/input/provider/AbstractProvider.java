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
package paulscode.android.mupen64plusae.input.provider;

import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.util.SubscriptionManager;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * The base class for transforming arbitrary input data into a common format.
 * 
 * @see KeyProvider
 * @see AxisProvider
 * @see SensorProvider
 * @see InputMap
 */
public abstract class AbstractProvider
{
    public interface Listener
    {
        public void onInput( int inputCode, float strength );
        
        public void onInput( int[] inputCodes, float[] strengths );
    }
    
    private SubscriptionManager<AbstractProvider.Listener> mPublisher;
    
    protected AbstractProvider()
    {
        mPublisher = new SubscriptionManager<AbstractProvider.Listener>();
    }
    
    public void registerListener( AbstractProvider.Listener listener )
    {
        mPublisher.subscribe( listener );
    }
    
    public void unregisterListener( AbstractProvider.Listener listener )
    {
        mPublisher.unsubscribe( listener );
    }
    
    public void unregisterAllListeners()
    {
        mPublisher.unsubscribeAll();
    }
    
    @TargetApi( 12 )
    public static String getInputName( int inputCode )
    {
        // TODO: Localize strings.
        boolean isHoneycombMR1 = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1;

        if( inputCode > 0 )
        {
            if( isHoneycombMR1 )
                return "KEYCODE_" + inputCode;
            else
                return KeyEvent.keyCodeToString( inputCode );
        }
        else if( inputCode < 0 )
        {
            int axis = inputToAxisCode( inputCode );
            String direction = inputToAxisDirection( inputCode )
                    ? " (+)"
                    : " (-)";
            if( isHoneycombMR1 )
                return "AXIS_" + axis + direction;
            else
                return MotionEvent.axisToString( axis ) + direction;
        }
        else
            return "NULL";
    }
    
    public static String getInputName( int inputCode, float strength )
    {
        return getInputName( inputCode ) + ( inputCode == 0
                ? ""
                : String.format( " %4.2f", strength ) );
    }
    
    protected void notifyListeners( int inputCode, float strength )
    {
        for( Listener listener : mPublisher.getSubscribers() )
            listener.onInput( inputCode, strength );
    }
    
    protected void notifyListeners( int[] inputCodes, float[] strengths )
    {
        for( Listener listener : mPublisher.getSubscribers() )
            listener.onInput( inputCodes.clone(), strengths.clone() );
    }
    
    protected static int axisToInputCode( int axisCode, boolean positiveDirection )
    {
        // Axis codes are encoded to negative values (versus buttons which are
        // positive). Axis codes are bit shifted by one so that the lowest bit
        // can encode axis direction.
        return -( ( axisCode ) * 2 + ( positiveDirection
                ? 1
                : 2 ) );
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
