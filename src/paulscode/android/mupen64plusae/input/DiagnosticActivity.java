package paulscode.android.mupen64plusae.input;

import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.R;
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
        int action = event.getAction();
        String name = AppData.IS_HONEYCOMB_MR1 ? KeyEvent.keyCodeToString( key ) : "KEYCODE_" + key;
        
        String message = "KeyEvent:";
        message += "\r\nDevice: " + event.getDeviceId();
        message += "\r\nAction: " + ( action == 0 ? "down" : "up" );
        message += "\r\nKeyCode: " + key;
        message += "\r\n\r\n  " + name;
        
        TextView view = (TextView) findViewById( R.id.textKey );
        view.setText( message );
        
        if( key == KeyEvent.KEYCODE_BACK )
        {
            if( action == 0 )
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
        int action = event.getAction();
        
        String message = "MotionEvent:";
        message += "\r\nDevice: " + event.getDeviceId();
        message += "\r\nAction: " + ( action == 2 ? "move" : action );
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
                    message += "\r\n  " + name + ": " + value;
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
