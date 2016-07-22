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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.mupen64plusae.v3.fzurita.R;

import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import android.content.Context;
import android.util.Log;

public class PlayerMap extends SerializableMap
{
    /** Flag indicating whether hardware filtering is enabled. */
    boolean mDisabled = true;
    
    /** A map where the device unique name maps to an id.
     *  When the device reconnects and it is given a new id,
     *  this map allows the old id to be looked up so it can be replaced with the new id. */
    private HashMap<String, Collection<Integer>> deviceNameToId = new HashMap<String, Collection<Integer>>();
    
    public static boolean isDeviceId( String deviceName )
    {
        try
        {
            if( Integer.parseInt( deviceName ) != 0 )
                return true;
        }
        catch( NumberFormatException ignored )
        {
        }
        
        return false;
    }
    
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
        super();
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
     * Gets a human-readable summary of the devices mapped to a player.
     * 
     * @param context The activity context.
     * @param player  The index to the player.
     * 
     * @return Description of the devices mapped to the given player.
     */
    public String getDeviceSummary( Context context, int player )
    {
        String result = "";
        for( int i = 0; i < mMap.size(); i++ )
        {
            if( mMap.valueAt( i ) == player )
            {
                int deviceId = mMap.keyAt( i );
                String name = AbstractProvider.isHardwareAvailable( deviceId ) ?
                        AbstractProvider.getHardwareName( deviceId ) :
                        context.getString( R.string.playerMap_deviceNotConnected );
                if( name == null )
                    result += context.getString( R.string.playerMap_deviceWithoutName, deviceId );
                else
                    result += context.getString( R.string.playerMap_deviceWithName, deviceId, name );
                result += "\n";
            }
        }
        return result.trim();
    }
    
    /**
     * Attempts to reconnect the specified device.
     */
    public boolean reconnectDevice( int hardwareId )
    {
        // If the device is not mapped to any player...
        if( mMap.get( hardwareId ) == 0 && AbstractProvider.isHardwareAvailable( hardwareId ) )
        {
            // ...and if the device was previously mapped to a player...
            String uniqueName = AbstractProvider.getUniqueName( hardwareId );
            Collection<Integer> ids = deviceNameToId.get( uniqueName );
            
            if( ids != null )
            {
                for( Integer oldId : ids )
                {
                    if( oldId != null && !AbstractProvider.isHardwareAvailable( oldId ) )
                    {
                        int player = mMap.get( oldId );
                        
                        if( player > 0 )
                        {
                            // ...then replace the old id with the new one.
                            Log.v( "PlayerMap", "Reconnecting device " + oldId + " as " + hardwareId + " for player " + player );
                            mMap.delete( oldId );
                            mMap.append( hardwareId, player );
                            ids.remove( oldId );
                            ids.add( hardwareId );
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    public boolean isMapped( int player )
    {
        return mMap.indexOfValue( player ) >= 0;
    }
    
    public void map( int hardwareId, int player )
    {
        if( player > 0 && player < 5 )
        {
            unmap( hardwareId );
            mMap.put( hardwareId, player );
            
            String name = AbstractProvider.getUniqueName( hardwareId );
            Collection<Integer> ids = deviceNameToId.get( name );
            
            if( ids == null )
            {
                ids = new HashSet<Integer>();
                deviceNameToId.put( name, ids );
            }
            
            ids.add( hardwareId );
        }
        else
            Log.w( "InputMap", "Invalid player specified in map(.,.): " + player );
    }
    
    public void unmap( int hardwareId )
    {
        mMap.delete( hardwareId );
        
        for( Entry<String, Collection<Integer>> entry : deviceNameToId.entrySet() )
        {
            for( Integer id : entry.getValue() )
            {
                if( id == hardwareId )
                {
                    entry.getValue().remove( hardwareId );
                    return;
                }
            }
        }
    }
    
    public void unmapPlayer( int player )
    {
        for( int index = mMap.size() - 1; index >= 0; index-- )
        {
            if( mMap.valueAt( index ) == player )
                unmap( mMap.keyAt( index ) );
        }
    }
    
    @Override
    public void unmapAll()
    {
        deviceNameToId.clear();
        super.unmapAll();
    }
    
    public void removeUnavailableMappings()
    {
        for( int i = mMap.size() - 1; i >= 0; i-- )
        {
            int id = mMap.keyAt( i );
            if( !AbstractProvider.isHardwareAvailable( id ) )
            {
                Log.v( "PlayerMap", "Removing device " + id + " from map" );
                unmap( id );
            }
        }
    }
    
    /**
     * Serializes the map data to a string.
     * 
     * @return The string representation of the map data.
     */
    @Override
    public String serialize()
    {
        // Serialize the map data to a multi-delimited string
        String result = "";
        
        for( Entry<String, Collection<Integer>> entry : deviceNameToId.entrySet() )
        {
            for( Integer id : entry.getValue() )
            {
                // If the device name is the id, then store the latest id, otherwise the device's unique name
                String device = ( isDeviceId( entry.getKey() ) ) ?
                        id.toString() :
                        ( entry.getKey() + "#" + id.toString() );
                int player = mMap.get( id );
                
                // Putting the value first makes the string a bit more human readable IMO
                if( player > 0 && device != null )
                    result += player + "$" + device + ",";
            }
        }
        
        Log.v("PlayerMap", "Serializing: " + result);
        return result;
    }
    
    /**
     * Deserializes the map data from a string.
     * 
     * @param s The string representation of the map data.
     */
    @Override
    public void deserialize( String s )
    {
        // Reset the map
        unmapAll();
        
        // Parse the new map data from the multi-delimited string
        if( s != null )
        {
            int psuedoId = -100;
            
            // Read the input mappings
            String[] pairs = s.split( "," );
            for( String pair : pairs )
            {
                String[] elements = pair.split( "\\$" );
                if( elements.length == 2 )
                {
                    try
                    {
                        int player = Integer.parseInt( elements[0] );
                        String[] deviceProperties = elements[1].split("#");
                        String deviceName = elements[1];
                        int id = 0;
                        
                        // Try to parse the hardware id and determine if the same name 
                        // is still at that id (some devices share the name and can
                        // only be distinguished from each other by id)
                        if( deviceProperties.length == 2 )
                        {
                            deviceName = deviceProperties[0];
                            
                            try { id = Integer.parseInt( deviceProperties[1] ); }
                            catch( NumberFormatException ignored ) { }
                            
                            // Check to make sure the device is valid
                            if( !deviceName.equals( AbstractProvider.getUniqueName( id ) ) )
                                id = AbstractProvider.getHardwareId( deviceName );
                        }
                        
                        // Try to get the hardware id from the device name (for backwards compatibility)
                        else
                            id = AbstractProvider.getHardwareId( deviceName );
                        
                        // If the specified device is not currently connected.
                        if( id == 0 )
                        {
                            // Do not add disconnected devices if the key specified is a device id.
                            if( isDeviceId( deviceName ) )
                                continue;
                            
                            id = psuedoId;
                            psuedoId--;
                        }
                        else
                        {
                            // This will update the device name when transitioning from device ids and add it to the map.
                            deviceName = AbstractProvider.getUniqueName( id );
                        }
                        
                        // Add the device to the appropriate places in the lists
                        Collection<Integer> ids = deviceNameToId.get( deviceName );
                        
                        if( ids == null )
                        {
                            ids = new HashSet<Integer>();
                            deviceNameToId.put( deviceName, ids );
                        }
                        
                        ids.add( id );
                        mMap.put( id, player );
                    }
                    catch( NumberFormatException ignored )
                    {
                    }
                }
            }
        }
    }
}
