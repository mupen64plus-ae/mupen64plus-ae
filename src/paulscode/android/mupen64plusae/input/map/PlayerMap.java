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

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import android.content.Context;
import android.util.Log;

public class PlayerMap extends SerializableMap
{
    /** Flag indicating whether hardware filtering is enabled. */
    boolean mDisabled = true;
    
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
        super( serializedMap );
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
                String name = AbstractProvider.getHardwareName( deviceId );
                if( name == null )
                    result += context.getString( R.string.playerMap_deviceWithoutName, deviceId );
                else
                    result += context.getString( R.string.playerMap_deviceWithName, deviceId, name );
                result += "\n";
            }
        }
        return result.trim();
    }
    
    public boolean isMapped( int player )
    {
        return mMap.indexOfValue( player ) >= 0;
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
        {
            if( mMap.valueAt( index ) == player )
                mMap.removeAt( index );
        }
    }
    
    public void removeUnavailableMappings()
    {
        for( int i = mMap.size() - 1; i >= 0; i-- )
        {
            int id = mMap.keyAt( i );
            if( !AbstractProvider.isHardwareAvailable( id ) )
            {
                Log.v( "PlayerMap", "Removing device " + id + " from map" );
                mMap.removeAt( i );
            }
        }
    }
    
    @Override
    public void deserialize( String s )
    {
        super.deserialize( s );
        removeUnavailableMappings();
    }
}
