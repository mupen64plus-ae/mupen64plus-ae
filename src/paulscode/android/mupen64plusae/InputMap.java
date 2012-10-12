package paulscode.android.mupen64plusae;

import android.util.SparseIntArray;

public class InputMap
{
    // Indices into N64 inputs array
    public static final int UNMAPPED = -1;
    public static final int DPD_R = 0;
    public static final int DPD_L = 1;
    public static final int DPD_D = 2;
    public static final int DPD_U = 3;
    public static final int START = 4;
    public static final int BTN_Z = 5;
    public static final int BTN_B = 6;
    public static final int BTN_A = 7;
    public static final int CPD_R = 8;
    public static final int CPD_L = 9;
    public static final int CPD_D = 10;
    public static final int CPD_U = 11;
    public static final int BTN_R = 12;
    public static final int BTN_L = 13;
    public static final int AXIS_R = 14;
    public static final int AXIS_L = 15;
    public static final int AXIS_D = 16;
    public static final int AXIS_U = 17;
    public static final int NUM_INPUTS = 18;
    
    private int[] mN64ToCode;
    private SparseIntArray mCodeToN64;
    
    public InputMap()
    {
        mN64ToCode = new int[NUM_INPUTS];
        mCodeToN64 = new SparseIntArray( NUM_INPUTS );
    }
    
    public int get( int inputCode )
    {
        return mCodeToN64.get( inputCode, UNMAPPED );
    }
    
    public void mapInput( int inputCode, int n64Index )
    {
        if( n64Index >= 0 && n64Index < NUM_INPUTS )
        {
            // Get the last code mapped to this index
            int oldInputCode = mN64ToCode[n64Index];
            
            // Map the new code to this index
            mN64ToCode[n64Index] = inputCode;
            
            // Refresh the reverse map
            mCodeToN64.delete( oldInputCode );
            if( inputCode != 0 )
                mCodeToN64.put( inputCode, n64Index );
        }
    }
    
    public String serialize()
    {
        String result = "";
        for( int i = 0; i < mN64ToCode.length; i++ )
        {
            result += mN64ToCode[i] + ",";
        }
        return result;
    }
    
    public void deserialize( String s )
    {
        // Reset the map
        for( int i = 0; i < mN64ToCode.length; i++ )
            mapInput( 0, i );
        
        // Read the new map values from the string
        if( s != null )
        {
            String[] strings = s.split( "," );
            for( int i = 0; i < Math.min( NUM_INPUTS, strings.length ); i++ )
                mapInput( tryParse(strings[i], 0), i);            
        }
    }
    
    private int tryParse( String s, int defaultValue )
    {
        try
        {
            return Integer.parseInt( s );
        }
        catch( NumberFormatException ex )
        {
            return defaultValue;
        }
    }
}
