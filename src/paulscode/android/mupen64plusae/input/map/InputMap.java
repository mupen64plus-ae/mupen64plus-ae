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
package paulscode.android.mupen64plusae.input.map;

import paulscode.android.mupen64plusae.InputMapPreference;
import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.PeripheralController;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.SubscriptionManager;
import android.util.SparseIntArray;

/**
 * A class for mapping arbitrary user inputs to N64 buttons/axes.
 * 
 * @see AbstractProvider
 * @see PeripheralController
 * @see InputMapPreference
 */
public class InputMap
{
    /**
     * The interface for listening to map changes.
     */
    public interface Listener
    {
        /**
         * Called when the map data has changed.
         * 
         * @param map The new value of the map.
         */
        public void onMapChanged( InputMap map );
    }
    
    // @formatter:off
    /** Map flag: Input code is not mapped. */
    public static final int UNMAPPED                    = -1;

    /** Map offset: N64 non-button controls. */
    public static final int OFFSET_EXTRAS               = AbstractController.NUM_N64_BUTTONS;
    
    /** N64 control: analog-right. */
    public static final int AXIS_R                      = OFFSET_EXTRAS;
    
    /** N64 control: analog-left. */
    public static final int AXIS_L                      = OFFSET_EXTRAS + 1;
    
    /** N64 control: analog-down. */
    public static final int AXIS_D                      = OFFSET_EXTRAS + 2;
    
    /** N64 control: analog-up. */
    public static final int AXIS_U                      = OFFSET_EXTRAS + 3;
    
    /** N64 control: Rumble Pak. */
    public static final int BTN_RUMBLE                  = OFFSET_EXTRAS + 4;
    
    /** N64 control: Expansion Pak. */
    public static final int BTN_MEMPAK                  = OFFSET_EXTRAS + 5;
    
    /** Total number of N64 controls. */
    public static final int NUM_N64_CONTROLS            = OFFSET_EXTRAS + 6;
    
    /** Map offset: Mupen64Plus functions. */
    public static final int OFFSET_FUNCS                = NUM_N64_CONTROLS;

    /** Mupen64Plus function: stop. */
    public static final int FUNC_STOP                   = OFFSET_FUNCS;
    
    /** Mupen64Plus function: full-screen. */
    public static final int FUNC_FULLSCREEN             = OFFSET_FUNCS + 1;
    
    /** Mupen64Plus function: save state. */
    public static final int FUNC_SAVESTATE              = OFFSET_FUNCS + 2;
    
    /** Mupen64Plus function: load state. */
    public static final int FUNC_LOADSTATE              = OFFSET_FUNCS + 3;
    
    /** Mupen64Plus function: increment slot. */
    public static final int FUNC_INCSLOT                = OFFSET_FUNCS + 4;
    
    /** Mupen64Plus function: reset. */
    public static final int FUNC_RESET                  = OFFSET_FUNCS + 5;
    
    /** Mupen64Plus function: increase speed. */
    public static final int FUNC_SPEEDUP                = OFFSET_FUNCS + 6;
    
    /** Mupen64Plus function: decrease speed. */
    public static final int FUNC_SPEEDDOWN              = OFFSET_FUNCS + 7;
    
    /** Mupen64Plus function: capture screenshot. */
    public static final int FUNC_SCREENSHOT             = OFFSET_FUNCS + 8;
    
    /** Mupen64Plus function: pause. */
    public static final int FUNC_PAUSE                  = OFFSET_FUNCS + 9;
    
    /** Mupen64Plus function: mute. */
    public static final int FUNC_MUTE                   = OFFSET_FUNCS + 10;
    
    /** Mupen64Plus function: volume up. */
    public static final int FUNC_VOLUP                  = OFFSET_FUNCS + 11;
    
    /** Mupen64Plus function: volume down. */
    public static final int FUNC_VOLDOWN                = OFFSET_FUNCS + 12;
    
    /** Mupen64Plus function: fast-forward. */
    public static final int FUNC_FFWD                   = OFFSET_FUNCS + 13;
    
    /** Mupen64Plus function: advance frame. */
    public static final int FUNC_FRAMEADV               = OFFSET_FUNCS + 14;
    
    /** Mupen64Plus function: Gameshark. */
    public static final int FUNC_GAMESHARK              = OFFSET_FUNCS + 15;
    
    /** Total number of mappable controls/functions. */
    public static final int NUM_MAPPABLES               = OFFSET_FUNCS + 16;
    // @formatter:on
    
    /** Map from N64/Mupen function to standardized input code. */
    private int[] mN64ToCode;
    
    /** Map from standardized input code to N64/Mupen function. */
    private SparseIntArray mCodeToN64;
    
    /** Flag indicating whether the map is enabled. */
    private boolean mEnabled;
    
    /** Listener management. */
    private SubscriptionManager<Listener> mPublisher;
    
    /**
     * Instantiates a new input map.
     */
    public InputMap()
    {
        mN64ToCode = new int[NUM_MAPPABLES];
        mCodeToN64 = new SparseIntArray( NUM_MAPPABLES );
        mPublisher = new SubscriptionManager<InputMap.Listener>();
    }
    
    /**
     * Instantiates a new input map from a serialization.
     * 
     * @param serializedMap The serialization of the map.
     */
    public InputMap( String serializedMap )
    {
        this();
        deserialize( serializedMap );
    }
    
    /**
     * Gets the N64/Mupen function mapped to a given input code.
     * 
     * @param inputCode The standardized input code.
     * @return The N64/Mupen function the code is mapped to, or UNMAPPED.
     * @see AbstractProvider
     * @see InputMap.UNMAPPED
     */
    public int get( int inputCode )
    {
        return mCodeToN64.get( inputCode, UNMAPPED );
    }
    
    /**
     * Gets a <b>copy</b> of the map from N64/Mupen functions to standardized input codes. Note that
     * changing the values in the returned map does not change the information stored in the
     * InputMap instance.
     * 
     * @return A copy of the map.
     */
    public int[] getMappedInputCodes()
    {
        return mN64ToCode.clone();
    }
    
    /**
     * Checks if the map is enabled.
     * 
     * @return True, if the map is enabled.
     */
    public boolean isEnabled()
    {
        return mEnabled;
    }
    
    /**
     * Enables or disables the map. Note that this does <b>not</b> change the map data itself, but
     * rather indicates whether client code should or should not use the map.
     * 
     * @param value True to enable the map; false to disable.
     */
    public void setEnabled( boolean value )
    {
        mEnabled = value;
    }
    
    /**
     * Maps an N64 control or Mupen64Plus function to an input code.
     * 
     * @param n64Index The index to the N64 control or Mupen64Plus function.
     * @param inputCode The standardized input code to be mapped.
     */
    public void mapInput( int n64Index, int inputCode )
    {
        // Call the private method, notifying listeners of the change.
        mapInput( inputCode, n64Index, true );
    }
    
    /**
     * Unmaps an N64 control or Mupen64Plus function.
     * 
     * @param n64Index The index to the N64 control or Mupen64Plus function.
     */
    public void unmapInput( int n64Index )
    {
        mapInput( 0, n64Index, true );
    }
    
    /**
     * Registers a listener to start receiving map change notifications.
     * 
     * @param listener The listener to register. Null values are safe.
     */
    public void registerListener( Listener listener )
    {
        mPublisher.subscribe( listener );
    }
    
    /**
     * Unregisters a listener to stop receiving map change notifications.
     * 
     * @param listener The listener to unregister. Null values are safe.
     */
    public void unregisterListener( Listener listener )
    {
        mPublisher.unsubscribe( listener );
    }
    
    /**
     * Serializes the map data to a string.
     * 
     * @return The string representation of the map data.
     */
    public String serialize()
    {
        // Serialize the map values to a comma-delimited string
        String result = mEnabled + ":";
        for( int i = 0; i < mN64ToCode.length; i++ )
        {
            result += mN64ToCode[i] + ",";
        }
        return result;
    }
    
    /**
     * Deserializes the map data from a string.
     * 
     * @param s The string representation of the map data.
     */
    public void deserialize( String s )
    {
        // Reset the map
        for( int i = 0; i < mN64ToCode.length; i++ )
            mapInput( 0, i, false );
        
        // Parse the new map values from the comma-delimited string
        if( s != null )
        {
            String[] groups = s.split( ":" );
            if( groups.length > 1 )
            {
                // Read the enabled state
                mEnabled = SafeMethods.toBoolean( groups[0], false );
                s = groups[1];
            }
            else
            {
                // Safety valve in case format changes
                mEnabled = false;
            }
            
            // Read the input mappings
            String[] inputs = s.split( "," );
            for( int i = 0; i < Math.min( NUM_MAPPABLES, inputs.length ); i++ )
                mapInput( SafeMethods.toInt( inputs[i], 0 ), i, false );
        }
        
        // Notify the listeners that the map has changed
        notifyListeners();
    }
    
    /**
     * Maps an N64 control or Mupen64Plus function to an input code.
     * 
     * @param n64Index The index to the N64 control or Mupen64Plus function.
     * @param inputCode The standardized input code to be mapped.
     * @param notify Whether to notify listeners of the change. False provides an optimization when
     *            mapping in batches, but be sure to call notifyListeners() when finished.
     */
    private void mapInput( int inputCode, int n64Index, boolean notify )
    {
        // Map the input if a valid index was given
        if( n64Index >= 0 && n64Index < NUM_MAPPABLES )
        {
            // Get the old code that was mapped to the new index
            int oldInputCode = mN64ToCode[n64Index];
            
            // Get the old index that was mapped to the new code
            int oldN64Index = get( inputCode );
            
            // Unmap the new code from the old index
            if( oldN64Index != UNMAPPED )
                mN64ToCode[oldN64Index] = 0;
            
            // Unmap the old code from the new index
            mCodeToN64.delete( oldInputCode );
            
            // Map the new code to the new index
            mN64ToCode[n64Index] = inputCode;
            
            // Map the new index to the new code
            if( inputCode != 0 )
                mCodeToN64.put( inputCode, n64Index );
        }
        
        // Notify listeners that the map has changed
        if( notify )
            notifyListeners();
    }
    
    /**
     * Notifies all listeners that the map data has changed.
     */
    protected void notifyListeners()
    {
        for( Listener listener : mPublisher.getSubscribers() )
            listener.onMapChanged( this );
    }
}
