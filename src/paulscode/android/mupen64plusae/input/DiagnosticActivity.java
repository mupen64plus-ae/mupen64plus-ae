package paulscode.android.mupen64plusae.input;

import static android.view.MotionEvent.*;

import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.persistent.AppData;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

public class DiagnosticActivity extends Activity
{
    public static String getDeviceName( int device )
    {
        String name = AbstractProvider.getHardwareName( device );
        return Integer.toString( device ) + ( name == null ? "" : " (" + name + ")" );
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
    
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.diagnostic_activity );
    }
    
    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event )
    {
        return onKey( event );
    }
    
    @Override
    public boolean onKeyUp( int keyCode, KeyEvent event )
    {
        return onKey( event );
    }
    
    @Override
    public boolean onTouchEvent( MotionEvent event )
    {
        return onMotion( event );
    }
    
    @TargetApi( 12 )
    @Override
    public boolean onGenericMotionEvent( MotionEvent event )
    {
        return onMotion( event );
    }
    
    @TargetApi( Build.VERSION_CODES.HONEYCOMB_MR1 )
    private boolean onKey( KeyEvent event )
    {
        int key = event.getKeyCode();
        
        String message = "KeyEvent:";
        message += "\r\nDevice: " + getDeviceName( event.getDeviceId() );
        message += "\r\nAction: " + getActionName( event.getAction(), false );
        message += "\r\nKeyCode: " + key;
        
        if( AppData.IS_HONEYCOMB_MR1 )
        {
            message += "\r\n\r\n" + KeyEvent.keyCodeToString( key );
        }
        
        TextView view = (TextView) findViewById( R.id.textKey );
        view.setText( message );
        
        if( key == KeyEvent.KEYCODE_BACK )
        {
            if( event.getAction() == 0 )
                return super.onKeyDown( key, event );
            else
                return super.onKeyUp( key, event );
        }
        else
            return true;
    }
    
    @TargetApi( 12 )
    private boolean onMotion( MotionEvent event )
    {
        String message = "MotionEvent:";
        message += "\r\nDevice: " + getDeviceName( event.getDeviceId() );
        message += "\r\nAction: " + getActionName( event.getAction(), true );
        message += "\r\n";
        
        if( AppData.IS_GINGERBREAD )
        {
            for( MotionRange range : getPeripheralMotionRanges( event.getDevice() ) )
            {
                if( AppData.IS_HONEYCOMB_MR1 )
                {
                    int axis = range.getAxis();
                    String name = MotionEvent.axisToString( axis );
                    double value = event.getAxisValue( axis );
                    message += "\r\n" + name + ": " + value;
                }
                else
                {
                    // TODO Something for Gingerbread devices
                }
            }
        }
        
        TextView view = (TextView) findViewById( R.id.textMotion );
        view.setText( message );
        return true;
    }
    
    /**
     * Gets the motion ranges of a peripheral using the appropriate Android API.
     * 
     * @return The motion ranges associated with the peripheral.
     */
    @TargetApi( 12 )
    private static List<MotionRange> getPeripheralMotionRanges( InputDevice device )
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
}
