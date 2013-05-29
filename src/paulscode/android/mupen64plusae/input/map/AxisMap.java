package paulscode.android.mupen64plusae.input.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.TargetApi;
import android.text.TextUtils;
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
    public static final int AXIS_CLASS_OUYA_LX_STICK = 101;
    public static final int AXIS_CLASS_RAPHNET_STICK = 102;
    public static final int AXIS_CLASS_RAPHNET_TRIGGER = 103;
    
    private static final int SIGNATURE_HASH_XBOX360 = 449832952;
    private static final int SIGNATURE_HASH_XBOX360_WIRELESS = -412618953;
    private static final int SIGNATURE_HASH_PS3 = -528816963;
    private static final int SIGNATURE_HASH_NYKO_PLAYPAD = 1245841466;
    private static final int SIGNATURE_HASH_LOGITECH_WINGMAN_RUMBLEPAD = 1247256123;
    
    private static final String NAME_STRING_NYKO_PLAYPAD = "NYKO PLAYPAD";
    private static final String NAME_STRING_OUYA = "OUYA";
    private static final String NAME_STRING_RAPHNET = "raphnet.net GC/N64";
    private static final String NAME_STRING_MAD_CATZ = "Mad Catz";
    
    private static final SparseArray<AxisMap> sAllMaps = new SparseArray<AxisMap>();
    private final String mSignature;
    private final String mSignatureName;
    
    public static AxisMap getMap( InputDevice device )
    {
        if( device == null )
            return null;
        
        int id = device.hashCode();
        AxisMap map = sAllMaps.get( id );
        if( map == null )
        {
            // Add an entry to the map if not found
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
        String signatureName = "Default";
        
        // Use the signature to override faulty auto-classifications
        switch( mSignature.hashCode() )
        {
            case SIGNATURE_HASH_XBOX360:
            case SIGNATURE_HASH_XBOX360_WIRELESS:
                // Resting value is -1 on the analog triggers; fix that
                setClass( MotionEvent.AXIS_Z, AXIS_CLASS_TRIGGER );
                setClass( MotionEvent.AXIS_RZ, AXIS_CLASS_TRIGGER );
                signatureName = mSignature.hashCode() == SIGNATURE_HASH_XBOX360_WIRELESS ?
                    "Xbox 360 wireless" : "Xbox 360 compatible";
                break;
            
            case SIGNATURE_HASH_PS3:
                // Ignore pressure sensitive buttons (buggy on Android)
                setClass( MotionEvent.AXIS_GENERIC_1, AXIS_CLASS_IGNORED );
                setClass( MotionEvent.AXIS_GENERIC_2, AXIS_CLASS_IGNORED );
                setClass( MotionEvent.AXIS_GENERIC_3, AXIS_CLASS_IGNORED );
                setClass( MotionEvent.AXIS_GENERIC_4, AXIS_CLASS_IGNORED );
                setClass( MotionEvent.AXIS_GENERIC_5, AXIS_CLASS_IGNORED );
                setClass( MotionEvent.AXIS_GENERIC_6, AXIS_CLASS_IGNORED );
                setClass( MotionEvent.AXIS_GENERIC_7, AXIS_CLASS_IGNORED );
                setClass( MotionEvent.AXIS_GENERIC_8, AXIS_CLASS_IGNORED );
                signatureName = "PS3 compatible";
                break;
            
            case SIGNATURE_HASH_NYKO_PLAYPAD:
                if( !device.getName().contains( NAME_STRING_NYKO_PLAYPAD ) &&
                        !device.getName().contains( NAME_STRING_MAD_CATZ ) )
                {
                    // The first batch of Nyko Playpad controllers have a quirk in the firmware
                    // where AXIS_HAT_X/Y are sent with (and overpower) AXIS_X/Y, and do not
                    // provide a recognizable name for the controller.  Newer batches of this
                    // controller fix the quirk, making it a vanilla controller.  However the
                    // AXIS_HAT_X/Y channels are now used for the d-pad, so we can't apply a
                    // universal solution for all versions of this controller.  Fortunately, the
                    // new version also returns a good name that we can use to differentiate
                    // controller firmware versions.
                    // 
                    // The Mad Catz C.T.R.L.R. controller has the same axis signature as the Nyko
                    // Playpad, but without the Nyko defect, so we have to check that as well.
                    //
                    // For original firmware, ignore AXIS_HAT_X/Y because they are sent with (and
                    // overpower) AXIS_X/Y
                    //
                    // TODO: Reduce the cludginess of this logic to minimize maintenance
                    setClass( MotionEvent.AXIS_HAT_X, AXIS_CLASS_IGNORED );
                    setClass( MotionEvent.AXIS_HAT_Y, AXIS_CLASS_IGNORED );
                    signatureName = "Nyko PlayPad series (original firmware)";
                }
                break;
            
            case SIGNATURE_HASH_LOGITECH_WINGMAN_RUMBLEPAD:
                // Bug in controller firmware cross-wires throttle and right stick up/down
                setClass( MotionEvent.AXIS_THROTTLE, AXIS_CLASS_STICK );
                signatureName = "Logitech Wingman Rumblepad";
                break;
        }
        // Check if the controller is OUYA, to compensate for the +X axis bias
        if( device.getName().contains( NAME_STRING_OUYA ) )
        {
            setClass( MotionEvent.AXIS_X, AXIS_CLASS_OUYA_LX_STICK );
            signatureName = "OUYA controller";
        }
        // Check if the controller is a raphnet N64/USB adapter, to compensate for range of motion
        // http://raphnet-tech.com/products/gc_n64_usb_adapters/
        if( device.getName().contains( NAME_STRING_RAPHNET ) )
        {
            setClass( MotionEvent.AXIS_X, AXIS_CLASS_RAPHNET_STICK );
            setClass( MotionEvent.AXIS_Y, AXIS_CLASS_RAPHNET_STICK );
            setClass( MotionEvent.AXIS_RX, AXIS_CLASS_RAPHNET_STICK );
            setClass( MotionEvent.AXIS_RY, AXIS_CLASS_RAPHNET_STICK );
            setClass( MotionEvent.AXIS_RZ, AXIS_CLASS_RAPHNET_TRIGGER );
            signatureName = "Raphnet adapter";
        }
        mSignatureName = signatureName;
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
        return mSignatureName;
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
