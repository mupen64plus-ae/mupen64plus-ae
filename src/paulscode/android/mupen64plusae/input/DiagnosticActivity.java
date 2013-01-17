package paulscode.android.mupen64plusae.input;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.util.DeviceUtil;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
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
        message += "\nDevice: " + getDeviceName( event.getDeviceId() );
        message += "\nAction: " + DeviceUtil.getActionName( event.getAction(), false );
        message += "\nKeyCode: " + key;
        
        if( AppData.IS_HONEYCOMB_MR1 )
        {
            message += "\n\n" + KeyEvent.keyCodeToString( key );
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
        message += "\nDevice: " + getDeviceName( event.getDeviceId() );
        message += "\nAction: " + DeviceUtil.getActionName( event.getAction(), true );
        message += "\n";
        
        if( AppData.IS_GINGERBREAD )
        {
            for( MotionRange range : DeviceUtil.getPeripheralMotionRanges( event.getDevice() ) )
            {
                if( AppData.IS_HONEYCOMB_MR1 )
                {
                    int axis = range.getAxis();
                    String name = MotionEvent.axisToString( axis );
                    String source = DeviceUtil.getSourceName( range.getSource() );
                    double value = event.getAxisValue( axis );
                    message += "\n" + name + " (" + source + "): " + value;
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
    
    private static String getDeviceName( int device )
    {
        String name = AbstractProvider.getHardwareName( device );
        return Integer.toString( device ) + ( name == null ? "" : " (" + name + ")" );
    }
}
