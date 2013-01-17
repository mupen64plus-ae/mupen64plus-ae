package paulscode.android.mupen64plusae.util;

import static android.view.MotionEvent.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import paulscode.android.mupen64plusae.persistent.AppData;
import android.annotation.TargetApi;
import android.text.TextUtils;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.MotionEvent;

public class DeviceUtil
{
    /**
     * Gets the hardware information from /proc/cpuinfo.
     * 
     * @return The hardware string.
     */
    public static String getCpuInfo()
    {
        // From http://android-er.blogspot.com/2009/09/read-android-cpu-info.html
        String result = "";
        try
        {
            String[] args = { "/system/bin/cat", "/proc/cpuinfo" };
            Process process = new ProcessBuilder( args ).start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[1024];
            while( in.read( re ) != -1 )
                result = result + new String( re );
            in.close();
        }
        catch( IOException ex )
        {
            ex.printStackTrace();
        }
        
        // Remove the serial number for privacy
        Pattern pattern = Pattern.compile( "^serial\\s*?:.*?$", Pattern.CASE_INSENSITIVE
                | Pattern.MULTILINE );
        result = pattern.matcher( result ).replaceAll( "Serial : XXXX" );
        
        return result;
    }
    
    /**
     * Gets the peripheral information using the appropriate Android API.
     * 
     * @return The peripheral info string.
     */
    @TargetApi( 16 )
    public static String getPeripheralInfo()
    {
        StringBuilder builder = new StringBuilder();
        
        if( AppData.IS_GINGERBREAD )
        {
            int[] ids = InputDevice.getDeviceIds();
            for( int i = 0; i < ids.length; i++ )
            {
                InputDevice device = InputDevice.getDevice( ids[i] );
                builder.append( "Device: " + device.getName() + "\n" );
                builder.append( "Id: " + device.getId() + "\n" );
                if( AppData.IS_JELLYBEAN )
                {
                    builder.append( "Descriptor: " + device.getDescriptor() + "\n" );
                    if( device.getVibrator().hasVibrator() )
                        builder.append( "Vibrator: true\n" );
                }
                builder.append( "Sources: " + getSourcesString( device.getSources() ) + "\n" );
                
                List<MotionRange> ranges = getPeripheralMotionRanges( device );
                if( ranges.size() > 0 )
                {
                    builder.append( "Axes: " + ranges.size() + "\n" );
                    for( int j = 0; j < ranges.size(); j++ )
                    {
                        MotionRange range = ranges.get( j );
                        String axisName = AppData.IS_HONEYCOMB_MR1 ? MotionEvent
                                .axisToString( range.getAxis() ) : "Axis";
                        builder.append( "  " + axisName + ":\n" );
                        builder.append( "      Min: " + range.getMin() + "\n" );
                        builder.append( "      Max: " + range.getMax() + "\n" );
                        builder.append( "      Range: " + range.getRange() + "\n" );
                        builder.append( "      Flat: " + range.getFlat() + "\n" );
                        builder.append( "      Fuzz: " + range.getFuzz() + "\n" );
                        if( AppData.IS_HONEYCOMB_MR1 )
                            builder.append( "      Source: " + getSourceName( range.getSource() ) + "\n" );
                    }
                }
                builder.append( "\n" );
            }
        }
        
        return builder.toString();
    }
    
    /**
     * Gets the motion ranges of a peripheral using the appropriate Android API.
     * 
     * @return The motion ranges associated with the peripheral.
     */
    @TargetApi( 12 )
    public static List<MotionRange> getPeripheralMotionRanges( InputDevice device )
    {
        List<MotionRange> ranges;
        if( AppData.IS_HONEYCOMB_MR1 )
        {
            ranges = device.getMotionRanges();
        }
        else if( AppData.IS_GINGERBREAD )
        {
            // Earlier APIs we have to do it the hard way
            ranges = new ArrayList<MotionRange>();
            boolean finished = false;
            for( int j = 0; j < 256 && !finished; j++ )
            {
                // TODO: Eliminate reliance on try-catch
                try
                {
                    if( device.getMotionRange( j ) != null )
                        ranges.add( device.getMotionRange( j ) );
                }
                catch( Exception e )
                {
                    finished = true;
                }
            }
        }
        else
        {
            ranges = new ArrayList<InputDevice.MotionRange>();
        }
        
        return ranges;
    }
    
    public static String getActionName( int action, boolean isMotionEvent )
    {
        switch( action )
        {
            case ACTION_DOWN:
                return "DOWN";
            case ACTION_UP:
                return "UP";
            case ACTION_MOVE:
                return isMotionEvent ? "MOVE" : "MULTIPLE";
            case ACTION_CANCEL:
                return "CANCEL";
            case ACTION_OUTSIDE:
                return "OUTSIDE";
            case ACTION_POINTER_DOWN:
                return "POINTER_DOWN";
            case ACTION_POINTER_UP:
                return "POINTER_UP";
            case ACTION_HOVER_MOVE:
                return "HOVER_MOVE";
            case ACTION_SCROLL:
                return "SCROLL";
            case ACTION_HOVER_ENTER:
                return "HOVER_ENTER";
            case ACTION_HOVER_EXIT:
                return "HOVER_EXIT";
            default:
                return "ACTION_" + Integer.toString( action );
        }
    }
    
    public static String getSourceName( int source )
    {
        switch( source )
        {
            case InputDevice.SOURCE_DPAD:
                return "DPAD";
            case InputDevice.SOURCE_GAMEPAD:
                return "GAMEPAD";
            case InputDevice.SOURCE_JOYSTICK:
                return "JOYSTICK";
            case InputDevice.SOURCE_KEYBOARD:
                return "KEYBOARD";
            case InputDevice.SOURCE_MOUSE:
                return "MOUSE";
            case InputDevice.SOURCE_STYLUS:
                return "STYLUS";
            case InputDevice.SOURCE_TOUCHPAD:
                return "TOUCHPAD";
            case InputDevice.SOURCE_TOUCHSCREEN:
                return "TOUCHSCREEN";
            case InputDevice.SOURCE_TRACKBALL:
                return "TRACKBALL";
            case InputDevice.SOURCE_UNKNOWN:
                return "UNKNOWN";
            default:
                return "SOURCE_" + source;
        }
    }
    
    public static String getSourcesString( int sources )
    {
        List<String> types = new ArrayList<String>();
        addString( sources, InputDevice.SOURCE_KEYBOARD, "KEYBOARD", types );
        addString( sources, InputDevice.SOURCE_DPAD, "DPAD", types );
        addString( sources, InputDevice.SOURCE_GAMEPAD, "GAMEPAD", types );
        addString( sources, InputDevice.SOURCE_TOUCHSCREEN, "TOUCHSCREEN", types );
        addString( sources, InputDevice.SOURCE_MOUSE, "MOUSE", types );
        addString( sources, InputDevice.SOURCE_STYLUS, "STYLUS", types );
        addString( sources, InputDevice.SOURCE_TOUCHPAD, "TOUCHPAD", types );
        addString( sources, InputDevice.SOURCE_JOYSTICK, "JOYSTICK", types );
        
        List<String> classes = new ArrayList<String>();
        addString( sources, InputDevice.SOURCE_CLASS_BUTTON, "button", classes );
        addString( sources, InputDevice.SOURCE_CLASS_POINTER, "pointer", classes );
        addString( sources, InputDevice.SOURCE_CLASS_TRACKBALL, "trackball", classes );
        addString( sources, InputDevice.SOURCE_CLASS_POSITION, "position", classes );
        addString( sources, InputDevice.SOURCE_CLASS_JOYSTICK, "joystick", classes );
        
        return TextUtils.join( ", ", types ) + " (" + TextUtils.join( ", ", classes ) + ")";
    }
    
    private static void addString( int sources, int sourceClass, String sourceName,
            List<String> strings )
    {
        if( ( sources & sourceClass ) > 0 )
            strings.add( sourceName );
    }
}
