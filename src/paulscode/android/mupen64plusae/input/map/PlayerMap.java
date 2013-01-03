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

import paulscode.android.mupen64plusae.util.SafeMethods;
import android.util.Log;
import android.util.SparseIntArray;

public class PlayerMap
{
    /** Flag indicating whether hardware filtering is enabled. */
    boolean mDisabled = true;
    
    /** Map from hardware identifier to player number. */
    final SparseIntArray mMap = new SparseIntArray();
    
    /**
     * Instantiates a new player map.
     */
    public PlayerMap()
    {
    }
    
    /**
     * Instantiates a new player map from a serialization.
     * 
     * @param serializedMap The serialization of the map.
     */
    public PlayerMap( String serializedMap )
    {
        this();
        deserialize( serializedMap );
    }
    
    public boolean testHardware( int hardwareId, int player )
    {
        return mDisabled || mMap.get( hardwareId, 0 ) == player;
    }
    
    public boolean isEnabled()
    {
        return !mDisabled;
    }
    
    public void setEnabled( boolean value )
    {
        mDisabled = !value;
    }
    
    /**
     * Gets a description of the devices mapped to a player.
     * 
     * @param player The index to the player.
     * @return Description of the devices mapped to the given player.
     */
    public String getMappedDeviceInfo( int player )
    {
        String result = "";
        for( int i = 0; i < mMap.size(); i++ )
        {
            if( mMap.valueAt( i ) == player )
            {
                int deviceId = mMap.keyAt( i );
                result += "Device " + deviceId + "\n";
            }
        }
        return result.trim();
    }
    
    public void map( int hardwareId, int player )
    {
        if( player > 0 && player < 5 )
            mMap.put( hardwareId, player );
        else
            Log.w( "InputMap", "Invalid player specified in map(.,.): " + player );
    }
    
    public void unmap( int hardwareId )
    {
        mMap.delete( hardwareId );
    }
    
    public void unmapPlayer( int player )
    {
        for( int index = mMap.size() - 1; index >= 0; index-- )
            if( mMap.valueAt( index ) == player )
                mMap.removeAt( index );
    }
    
    /**
     * Serializes the map data to a string.
     * 
     * @return The string representation of the map data.
     */
    public String serialize()
    {
        // Serialize the map values to a comma-delimited string
        String result = "";
        for( int i = 0; i < mMap.size(); i++ )
        {
            // Putting the player number first makes the string a bit more human readable IMO
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
        mMap.clear();
        
        // Parse the new map values from the comma-delimited string
        if( s != null )
        {
            // Read the input mappings
            String[] pairs = s.split( "," );
            for( String pair : pairs )
            {
                String[] elements = pair.split( ":" );
                if( elements.length == 2 )
                {
                    int value = SafeMethods.toInt( elements[0], -1 );
                    int key = SafeMethods.toInt( elements[1], 0 );
                    map( key, value );
                }
            }
        }
    }
}
