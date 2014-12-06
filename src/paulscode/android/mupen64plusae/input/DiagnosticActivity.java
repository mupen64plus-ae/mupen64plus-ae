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
package paulscode.android.mupen64plusae.input;

import java.util.Locale;

import org.mupen64plusae.v3.alpha.R;
import paulscode.android.mupen64plusae.hacks.MogaHack;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.DeviceUtil;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.InputDevice.MotionRange;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

import com.bda.controller.Controller;
import com.bda.controller.ControllerListener;
import com.bda.controller.StateEvent;

public class DiagnosticActivity extends Activity implements ControllerListener
{
    private Controller mMogaController = Controller.getInstance( this );
    
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        new UserPrefs( this ).enforceLocale( this );
        setContentView( R.layout.diagnostic_activity );
        
        // TODO: Remove hack after MOGA SDK is fixed
        // mMogaController.init();
        MogaHack.init( mMogaController, this );
        mMogaController.setListener( this, new Handler() );
    }
    
    @Override
    public void onResume()
    {
        super.onResume();
        mMogaController.onResume();
    }
    
    @Override
    public void onPause()
    {
        super.onPause();
        mMogaController.onPause();
    }
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mMogaController.exit();
    }
    
    @Override
    public void onStateEvent( StateEvent event )
    {
    }
    
    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event )
    {
        onKey( event );
        if( keyCode == KeyEvent.KEYCODE_BACK )
            return super.onKeyDown( keyCode, event );
        return true;
    }
    
    @Override
    public boolean onKeyUp( int keyCode, KeyEvent event )
    {
        onKey( event );
        if( keyCode == KeyEvent.KEYCODE_BACK )
            return super.onKeyUp( keyCode, event );
        return true;
    }
    
    @TargetApi( 12 )
    private void onKey( KeyEvent event )
    {
        int keyCode = event.getKeyCode();
        
        String message = "KeyEvent:";
        message += "\nDevice: " + getHardwareSummary( AbstractProvider.getHardwareId( event ) );
        message += "\nAction: " + DeviceUtil.getActionName( event.getAction(), false );
        message += "\nKeyCode: " + keyCode;
        
        if( AppData.IS_HONEYCOMB_MR1 )
        {
            message += "\n\n" + KeyEvent.keyCodeToString( keyCode );
        }
        
        TextView view = (TextView) findViewById( R.id.textKey );
        view.setText( message );
    }
    
    @Override
    @TargetApi( 12 )
    public void onKeyEvent( com.bda.controller.KeyEvent event )
    {
        int keyCode = event.getKeyCode();
        
        String message = "KeyEvent:";
        message += "\nDevice: " + getHardwareSummary( AbstractProvider.getHardwareId( event ) );
        message += "\nAction: MOGA_" + DeviceUtil.getActionName( event.getAction(), false );
        message += "\nKeyCode: " + keyCode;
        
        if( AppData.IS_HONEYCOMB_MR1 )
        {
            message += "\n\n" + KeyEvent.keyCodeToString( keyCode );
        }        
        
        TextView view = (TextView) findViewById( R.id.textKey );
        view.setText( message );
    }
    
    @Override
    public boolean onTouchEvent( MotionEvent event )
    {
        onMotion( event );
        return true;
    }
    
    @TargetApi( 12 )
    @Override
    public boolean onGenericMotionEvent( MotionEvent event )
    {
        onMotion( event );
        return true;
    }
    
    @TargetApi( 12 )
    private void onMotion( MotionEvent event )
    {
        String message = "MotionEvent:";
        message += "\nDevice: " + getHardwareSummary( AbstractProvider.getHardwareId( event ) );
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
                    String source = DeviceUtil.getSourceName( range.getSource() ).toLowerCase(
                            Locale.US );
                    float value = event.getAxisValue( axis );
                    message += String.format( "\n%s (%s): %+.2f", name, source, value );
                }
                else
                {
                    // TODO Something for Gingerbread devices
                }
            }
        }
        
        TextView view = (TextView) findViewById( R.id.textMotion );
        view.setText( message );
    }
    
    @Override
    public void onMotionEvent( com.bda.controller.MotionEvent event )
    {
        String message = "MotionEvent:";
        message += "\nDevice: " + getHardwareSummary( AbstractProvider.getHardwareId( event ) );
        message += "\nAction: MOGA_MOTION";
        message += "\n";
        // @formatter:off
        message += String.format( "\nAXIS_X (moga): %+.2f",        event.getAxisValue( com.bda.controller.MotionEvent.AXIS_X ) );
        message += String.format( "\nAXIS_Y (moga): %+.2f",        event.getAxisValue( com.bda.controller.MotionEvent.AXIS_Y ) );
        message += String.format( "\nAXIS_Z (moga): %+.2f",        event.getAxisValue( com.bda.controller.MotionEvent.AXIS_Z ) );
        message += String.format( "\nAXIS_RZ (moga): %+.2f",       event.getAxisValue( com.bda.controller.MotionEvent.AXIS_RZ ) );
        message += String.format( "\nAXIS_LTRIGGER (moga): %+.2f", event.getAxisValue( com.bda.controller.MotionEvent.AXIS_LTRIGGER ) );
        message += String.format( "\nAXIS_RTRIGGER (moga): %+.2f", event.getAxisValue( com.bda.controller.MotionEvent.AXIS_RTRIGGER ) );
        // @formatter:on
        TextView view = (TextView) findViewById( R.id.textMotion );
        view.setText( message );
    }
    
    private static String getHardwareSummary( int hardwareId )
    {
        String name = AbstractProvider.getHardwareName( hardwareId );
        return Integer.toString( hardwareId ) + ( name == null ? "" : " (" + name + ")" );
    }
}
