package paulscode.android.mupen64plusae.input.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.TargetApi;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.MotionEvent;

@TargetApi( 9 )
public class AxisMap extends SerializableMap
{
    public static final int AXIS_CLASS_UNKNOWN = 0;
    public static final int AXIS_CLASS_IGNORED = 1;
    public static final int AXIS_CLASS_STICK = 2;
    public static final int AXIS_CLASS_TRIGGER = 3;
    
    private static final int SIGNATURE_HASH_XBOX360 = 449832952;
    private static final int SIGNATURE_HASH_NYKO_PLAYPAD = 1245841466;
    private static final int SIGNATURE_HASH_LOGITECH_WINGMAN_RUMBLEPAD = 1247256123;
    
    private static final SparseArray<AxisMap> sAllMaps = new SparseArray<AxisMap>();
    private final String mSignature;
    
    public static AxisMap getMap( InputDevice device )
    {
        if( device == null )
            return null;
        
        int id = device.hashCode();
        AxisMap map = sAllMaps.get( id );
        if( map == null )
        {
            // Add an entry to the map if not found
            Log.v( "AxisMap", "Auto-classifying " + device.getName() );
            map = new AxisMap( device );
            sAllMaps.put( id, map );
        }
        return map;
    }
    
    @TargetApi( 12 )
    public AxisMap( InputDevice device )
    {
        // Auto-classify the axes
        List<MotionRange> motionRanges = device.getMotionRanges();
        List<Integer> axisCodes = new ArrayList<Integer>();
        for( MotionRange motionRange : motionRanges )
        {
            if( motionRange.getSource() == InputDevice.SOURCE_JOYSTICK )
            {
                int axisCode = motionRange.getAxis();
                int axisClass = detectClass( motionRange );
                setClass( axisCode, axisClass );
                axisCodes.add( axisCode );
            }
        }
        
        // Construct the signature based on the available axes
        Collections.sort( axisCodes );
        mSignature = TextUtils.join( ",", axisCodes );
        
        // Use the signature to override faulty auto-classifications
        switch( mSignature.hashCode() )
        {
            case SIGNATURE_HASH_XBOX360:
                setClass( MotionEvent.AXIS_Z, AXIS_CLASS_TRIGGER );
                setClass( MotionEvent.AXIS_RZ, AXIS_CLASS_TRIGGER );
                break;
            
            case SIGNATURE_HASH_NYKO_PLAYPAD:
                setClass( MotionEvent.AXIS_HAT_X, AXIS_CLASS_IGNORED );
                setClass( MotionEvent.AXIS_HAT_Y, AXIS_CLASS_IGNORED );
                break;
            
            case SIGNATURE_HASH_LOGITECH_WINGMAN_RUMBLEPAD:
                setClass( MotionEvent.AXIS_THROTTLE, AXIS_CLASS_STICK );
                break;
        }
    }
    
    public void setClass( int axisCode, int axisClass )
    {
        if( axisClass == AXIS_CLASS_UNKNOWN )
            mMap.delete( axisCode );
        else
            mMap.put( axisCode, axisClass );
    }
    
    public int getClass( int axisCode )
    {
        return mMap.get( axisCode );
    }
    
    public String getSignature()
    {
        return mSignature;
    }
    
    public String getSignatureName()
    {
        switch( mSignature.hashCode() )
        {
            case SIGNATURE_HASH_XBOX360:
                return "Xbox 360 compatible";
            case SIGNATURE_HASH_NYKO_PLAYPAD:
                return "Nyko PlayPad series";
            case SIGNATURE_HASH_LOGITECH_WINGMAN_RUMBLEPAD:
                return "Logitech Wingman Rumblepad";
            default:
                return "Default";
        }
    }
    
    @TargetApi( 12 )
    private static int detectClass( MotionRange motionRange )
    {
        if( motionRange != null )
        {
            if( motionRange.getMin() == -1 )
                return AXIS_CLASS_STICK;
            else if( motionRange.getMin() == 0 )
                return AXIS_CLASS_TRIGGER;
        }
        return AXIS_CLASS_UNKNOWN;
    }
}
