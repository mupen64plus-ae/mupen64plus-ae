/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.input.provider;

import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.util.SubscriptionManager;
import android.annotation.TargetApi;
import android.view.InputDevice;
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
    /**
     * The interface for listening to a provider.
     */
    public interface OnInputListener
    {
        /**
         * Called when a single input has been dispatched.
         * 
         * @param inputCode The universal input code that was dispatched.
         * @param strength The input strength, between 0 and 1, inclusive.
         * @param hardwareId The identifier of the source device.
         */
        public void onInput( int inputCode, float strength, int hardwareId );
        
        /**
         * Called when multiple inputs have been dispatched simultaneously.
         * 
         * @param inputCodes The universal input codes that were dispatched.
         * @param strengths The input strengths, between 0 and 1, inclusive.
         * @param hardwareId The identifier of the source device.
         */
        public void onInput( int[] inputCodes, float[] strengths, int hardwareId );
    }
    
    /** The strength threshold above which an input is said to be "on". */
    public static final float STRENGTH_THRESHOLD = 0.5f;
    
    /** Listener management. */
    private SubscriptionManager<AbstractProvider.OnInputListener> mPublisher;
    
    /**
     * Instantiates a new abstract provider.
     */
    protected AbstractProvider()
    {
        mPublisher = new SubscriptionManager<AbstractProvider.OnInputListener>();
    }
    
    /**
     * Registers a listener to start receiving input notifications.
     * 
     * @param listener The listener to register. Null values are safe.
     */
    public void registerListener( AbstractProvider.OnInputListener listener )
    {
        mPublisher.subscribe( listener );
    }
    
    /**
     * Unregisters a listener to stop receiving input notifications.
     * 
     * @param listener The listener to unregister. Null values are safe.
     */
    public void unregisterListener( AbstractProvider.OnInputListener listener )
    {
        mPublisher.unsubscribe( listener );
    }
    
    /**
     * Unregisters all listeners.
     */
    public void unregisterAllListeners()
    {
        mPublisher.unsubscribeAll();
    }
    
    /**
     * Gets the human-readable name of the hardware.
     *
     * @param id The unique hardware identifier.
     * @return The name of the hardware, or null if hardware not found.
     */
    @TargetApi( 9 )
    public static String getHardwareName( int id )
    {
        if( AppData.IS_GINGERBREAD )
        {
            InputDevice device = InputDevice.getDevice( id );
            return device == null ? null : device.getName();
        }
        else
        {
            return null;
        }
    }

    /**
     * Gets the human-readable name of the input.
     * 
     * @param inputCode The universal input code.
     * @return The name of the input.
     */
    @TargetApi( 12 )
    public static String getInputName( int inputCode )
    {   
        if( inputCode > 0 )
        {
            if( AppData.IS_HONEYCOMB_MR1 )
                return KeyEvent.keyCodeToString( inputCode );
            else
                return "KEYCODE_" + inputCode;
        }
        else if( inputCode < 0 )
        {
            int axis = inputToAxisCode( inputCode );
            String direction = inputToAxisDirection( inputCode )
                    ? " (+)"
                    : " (-)";
            if( AppData.IS_HONEYCOMB_MR1 )
                return MotionEvent.axisToString( axis ) + direction;
            else
                return "AXIS_" + axis + direction;
        }
        else
            return "NULL";
    }
    
    /**
     * Gets the human-readable name of the input, appended with strength information.
     * 
     * @param inputCode The universal input code.
     * @param strength The input strength, between 0 and 1, inclusive.
     * @return The name of the input.
     */
    public static String getInputName( int inputCode, float strength )
    {
        return getInputName( inputCode ) + ( inputCode == 0
                ? ""
                : String.format( " %4.2f", strength ) );
    }    
    
    /**
     * Utility for child classes. Converts an Android axis code to a universal input code.
     * 
     * @param axisCode The Android axis code.
     * @param positiveDirection Set true for positive Android axis, false for negative Android axis.
     * @return The corresponding universal input code.
     */
    protected static int axisToInputCode( int axisCode, boolean positiveDirection )
    {
        // Axis codes are encoded to negative values (versus buttons which are positive). Axis codes
        // are bit shifted by one so that the lowest bit can encode axis direction.
        return -( ( axisCode ) * 2 + ( positiveDirection ? 1 : 2 ) );
    }
    
    /**
     * Utility for child classes. Converts a universal input code to an Android axis code.
     * 
     * @param inputCode The universal input code.
     * @return The corresponding Android axis code.
     */
    protected static int inputToAxisCode( int inputCode )
    {
        return ( -inputCode - 1 ) / 2;
    }
    
    /**
     * Utility for child classes. Converts a universal input code to an Android axis direction.
     * 
     * @param inputCode The universal input code.
     * @return True if the input code represents positive Android axis direction, false otherwise.
     */
    protected static boolean inputToAxisDirection( int inputCode )
    {
        return ( ( -inputCode ) % 2 ) == 1;
    }
    
    /**
     * Obtains unique hardware id from an input event.
     * 
     * @param event The event generated by the hardware.
     * @return The unique identifier of the hardware.
     */
    public static int getHardwareId( KeyEvent event )
    {
        // This might be replaced by something else in the future... so we abstract it
        return event.getDeviceId();
    }
    
    /**
     * Obtains unique hardware id from an input event.
     * 
     * @param event The event generated by the hardware.
     * @return The unique identifier of the hardware.
     */
    public static int getHardwareId( MotionEvent event )
    {
        // This might be replaced by something else in the future... so we abstract it
        return event.getDeviceId();
    }
    
    /**
     * Notifies listeners that a single input was dispatched. Subclasses should invoke this method
     * to publish their input data.
     * 
     * @param inputCode The universal input code that was dispatched.
     * @param strength The input strength, between 0 and 1, inclusive.
     * @param hardwareId The identifier of the source device.
     */
    protected void notifyListeners( int inputCode, float strength, int hardwareId )
    {
        for( OnInputListener listener : mPublisher.getSubscribers() )
            listener.onInput( inputCode, strength, hardwareId );
    }
    
    /**
     * Notifies listeners that multiple inputs were dispatched simultaneously. Subclasses should
     * invoke this method to publish their input data.
     * 
     * @param inputCodes The universal input codes that were dispatched.
     * @param strengths The input strengths, between 0 and 1, inclusive.
     * @param hardwareId The identifier of the source device.
     */
    protected void notifyListeners( int[] inputCodes, float[] strengths, int hardwareId )
    {
        for( OnInputListener listener : mPublisher.getSubscribers() )
            listener.onInput( inputCodes.clone(), strengths.clone(), hardwareId );
    }
}
