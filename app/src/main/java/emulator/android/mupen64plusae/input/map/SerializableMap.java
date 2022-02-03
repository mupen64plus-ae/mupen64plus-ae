package emulator.android.mupen64plusae.input.map;

import android.util.SparseIntArray;

public class SerializableMap
{
    /** Storage for the map. */
    final SparseIntArray mMap = new SparseIntArray();

    /**
     * Instantiates a new map.
     */
    public SerializableMap()
    {
    }
    
    /**
     * Instantiates a new map from a serialization.
     * 
     * @param serializedMap The serialization of the map.
     */
    SerializableMap( String serializedMap )
    {
        this();
        deserialize( serializedMap );
    }
    
    /**
     * Removes all entries from the map.
     */
    public void unmapAll()
    {
        mMap.clear();
    }
    
    /**
     * Serializes the map data to a string.
     * 
     * @return The string representation of the map data.
     */
    public String serialize()
    {
        // Serialize the map data to a multi-delimited string
        StringBuilder result = new StringBuilder();
        for( int i = 0; i < mMap.size(); i++ )
        {
            // Putting the value first makes the string a bit more human readable IMO
            result.append(mMap.valueAt(i)).append(":").append(mMap.keyAt(i)).append(",");
        }
        return result.toString();
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
                    try
                    {
                        int value = Integer.parseInt( elements[0] );
                        int key = Integer.parseInt( elements[1] );
                        mMap.put( key, value );
                    }
                    catch( NumberFormatException ignored )
                    {
                    }
                }
            }
        }
    }
}
