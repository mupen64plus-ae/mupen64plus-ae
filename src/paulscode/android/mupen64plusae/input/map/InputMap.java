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
package paulscode.android.mupen64plusae.input.map;

import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.InputMapActivity;
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
 * @see InputMapActivity
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
    
    /** Total number of N64 controls. */
    public static final int NUM_N64_CONTROLS            = OFFSET_EXTRAS + 4;
    
    /** Map offset: Mupen64Plus global functions. */
    public static final int OFFSET_GLOBAL_FUNCS         = NUM_N64_CONTROLS;

    /** Mupen64Plus function: increment slot. */
    public static final int FUNC_INCREMENT_SLOT         = OFFSET_GLOBAL_FUNCS;
    
    /** Mupen64Plus function: save state. */
    public static final int FUNC_SAVE_SLOT              = OFFSET_GLOBAL_FUNCS + 1;
    
    /** Mupen64Plus function: load state. */
    public static final int FUNC_LOAD_SLOT              = OFFSET_GLOBAL_FUNCS + 2;
    
    /** Mupen64Plus function: reset. */
    public static final int FUNC_RESET                  = OFFSET_GLOBAL_FUNCS + 3;
    
    /** Mupen64Plus function: stop. */
    public static final int FUNC_STOP                   = OFFSET_GLOBAL_FUNCS + 4;
    
    /** Mupen64Plus function: pause. */
    public static final int FUNC_PAUSE                  = OFFSET_GLOBAL_FUNCS + 5;
    
    /** Mupen64Plus function: fast-forward. */
    public static final int FUNC_FAST_FORWARD           = OFFSET_GLOBAL_FUNCS + 6;
    
    /** Mupen64Plus function: advance frame. */
    public static final int FUNC_FRAME_ADVANCE          = OFFSET_GLOBAL_FUNCS + 7;
    
    /** Mupen64Plus function: increase speed. */
    public static final int FUNC_SPEED_UP               = OFFSET_GLOBAL_FUNCS + 8;
    
    /** Mupen64Plus function: decrease speed. */
    public static final int FUNC_SPEED_DOWN             = OFFSET_GLOBAL_FUNCS + 9;
    
    /** Mupen64Plus function: Gameshark. */
    public static final int FUNC_GAMESHARK              = OFFSET_GLOBAL_FUNCS + 10;
    
    /** Total number of mappable controls/functions. */
    public static final int NUM_MAPPABLES               = OFFSET_GLOBAL_FUNCS + 11;
    // @formatter:on
    
    /** Map from standardized input code to N64/Mupen command. */
    private final SparseIntArray mMap;
    
    /** Listener management. */
    private final SubscriptionManager<Listener> mPublisher;
    
    /**
     * Instantiates a new input map.
     */
    public InputMap()
    {
        mMap = new SparseIntArray();
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
     * Gets the N64/Mupen command mapped to a given input code.
     * 
     * @param inputCode The standardized input code.
     * @return The N64/Mupen command the code is mapped to, or UNMAPPED.
     * @see AbstractProvider
     * @see InputMap#UNMAPPED
     */
    public int get( int inputCode )
    {
        return mMap.get( inputCode, UNMAPPED );
    }
    
    /**
     * Maps an input code to an N64/Mupen command.
     * 
     * @param inputCode The standardized input code to be mapped.
     * @param command The index to the N64/Mupen command.
     */
    public void map( int inputCode, int command )
    {
        // Call the private method, notifying listeners of the change.
        mapInput( inputCode, command, true );
    }
    
    /**
     * Unmaps an input code.
     * 
     * @param inputCode The standardized input code to be unmapped.
     */
    public void unmap( int inputCode )
    {
        mMap.delete( inputCode );
    }
    
    /**
     * Unmaps an N64/Mupen command.
     * 
     * @param command The index to the N64/Mupen command.
     */
    public void unmapCommand( int command )
    {
        // Remove any matching key-value pairs (count down to accommodate removal)
        for( int i = mMap.size() - 1; i >= 0; i-- )
        {
            if( mMap.valueAt( i ) == command )
                mMap.removeAt( i );
        }
        notifyListeners();
    }
    
    public void unmapAll()
    {
        mMap.clear();        
    }

    /**
     * Checks if an N64/Mupen command is mapped to at least one input code.
     * 
     * @param command The index to the N64/Mupen64 command.
     * @return True, if the mapping exists.
     */
    public boolean isMapped( int command )
    {
        return mMap.indexOfValue( command ) >= 0;
    }
    
    /**
     * Gets a description of the input codes mapped to an N64/Mupen command.
     * 
     * @param command The index to the N64/Mupen command.
     * @return Description of the input codes mapped to the given command.
     */
    public String getMappedCodeInfo( int command )
    {
        String result = "";
        for( int i = 0; i < mMap.size(); i++ )
        {
            if( mMap.valueAt( i ) == command )
            {
                result += AbstractProvider.getInputName( mMap.keyAt( i ) ) + "\n";
            }
        }
        return result.trim();
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
        // Serialize the map data to a multi-delimited string
        String result = "";
        for( int i = 0; i < mMap.size(); i++ )
        {
            // Putting the n64 command first makes the string a bit more human readable IMO
            result += mMap.valueAt( i ) + ":" + mMap.keyAt( i ) + ",";
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
        unmapAll();
        
        // Parse the new map data from the multi-delimited string
        if( s != null )
        {
            // Read the input mappings
            String[] pairs = s.split( "," );
            for( String pair : pairs )
            {
                String[] elements = pair.split( ":" );
                if( elements.length == 2 )
                {
                    int value = SafeMethods.toInt( elements[0], UNMAPPED );
                    int key = SafeMethods.toInt( elements[1], 0 );
                    mapInput( key, value, false );
                }
            }
        }
        
        // Notify the listeners that the map has changed
        notifyListeners();
    }
    
    /**
     * Maps an input code to an N64 control or Mupen64Plus function.
     * 
     * @param inputCode The standardized input code to be mapped.
     * @param command The index to the N64/Mupen command.
     * @param notify Whether to notify listeners of the change. False provides an optimization when
     *            mapping in batches, but be sure to call notifyListeners() when finished.
     */
    private void mapInput( int inputCode, int command, boolean notify )
    {
        // Map the input if a valid index was given
        if( command >= 0 && command < NUM_MAPPABLES && inputCode != 0 )
            mMap.put( inputCode, command );
        
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
