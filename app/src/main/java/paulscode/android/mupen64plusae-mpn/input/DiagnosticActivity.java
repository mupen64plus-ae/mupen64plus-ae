/*
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
package paulscode.android.mupen64plusae-mpn.input;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.InputDevice.MotionRange;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

import org.mupen64plusae-mpn.v3.alpha.R;

import java.util.Locale;

import paulscode.android.mupen64plusae-mpn.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae-mpn.util.DeviceUtil;
import paulscode.android.mupen64plusae-mpn.util.LocaleContextWrapper;

public class DiagnosticActivity extends AppCompatActivity
{
    @Override
    protected void attachBaseContext(Context newBase) {
        if(TextUtils.isEmpty(LocaleContextWrapper.getLocalCode()))
        {
            super.attachBaseContext(newBase);
        }
        else
        {
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase,LocaleContextWrapper.getLocalCode()));
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
        onKey( event );
        return keyCode != KeyEvent.KEYCODE_BACK || super.onKeyDown( keyCode, event );
    }
    
    @Override
    public boolean onKeyUp( int keyCode, KeyEvent event )
    {
        onKey( event );

        return keyCode != KeyEvent.KEYCODE_BACK || super.onKeyUp( keyCode, event );
    }
    
    private void onKey( KeyEvent event )
    {
        int keyCode = event.getKeyCode();
        
        String message = "KeyEvent:";
        message += "\nDevice: " + getHardwareSummary( AbstractProvider.getHardwareId( event ) );
        message += "\nAction: " + DeviceUtil.getActionName( event.getAction(), false );
        message += "\nKeyCode: " + keyCode;
        message += "\n\n" + KeyEvent.keyCodeToString( keyCode );
        
        TextView view = findViewById( R.id.textKey );
        view.setText( message );
    }
    
    @Override
    public boolean onTouchEvent( MotionEvent event )
    {
        onMotion( event );
        return true;
    }
    
    @Override
    public boolean onGenericMotionEvent( MotionEvent event )
    {
        onMotion( event );
        return true;
    }
    
    private void onMotion(MotionEvent event)
    {
        StringBuilder message = new StringBuilder();

        message.append("MotionEvent:");
        message.append("\nDevice: ").append(getHardwareSummary(AbstractProvider.getHardwareId(event)));
        message.append("\nAction: ").append(DeviceUtil.getActionName(event.getAction(), true));
        message.append("\n");

        if (event.getDevice() != null) {
            for (MotionRange range : event.getDevice().getMotionRanges()) {
                int axis = range.getAxis();
                String name = MotionEvent.axisToString(axis);
                String source = DeviceUtil.getSourceName(range.getSource()).toLowerCase(Locale.US);
                float value = event.getAxisValue(axis);
                message.append(String.format(Locale.US,"\n%s (%s): %+.2f", name, source, value));
            }
        }

        TextView view = findViewById(R.id.textMotion);
        view.setText(message);
    }
    
    private static String getHardwareSummary( int hardwareId )
    {
        String name = AbstractProvider.getHardwareName( hardwareId );
        return hardwareId + ( name == null ? "" : " (" + name + ")" );
    }
}
