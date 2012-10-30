/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: paulscode, lioncash
 */
package paulscode.android.mupen64plusae.input;

import paulscode.android.mupen64plusae.Globals;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

// TODO: Straighten out this class (I left it a mess (littleguy))
public class TouchscreenController extends AbstractController implements OnTouchListener
{
    // Controlled by IME special keys used for analog input:
    private boolean[] pointers = new boolean[256];
    private int[] pointerX = new int[256];
    private int[] pointerY = new int[256];

    public TouchscreenController( View view )
    {
        for( int i = 0; i < 256; i++ )
        {
            pointers[i] = false;
            pointerX[i] = -1;
            pointerY[i] = -1;
        }
        
        view.setOnTouchListener( this );
    }

    @TargetApi( 5 )
    @Override
    public boolean onTouch( View v, MotionEvent event )
    {
        if( !Globals.userPrefs.isInputEnabled  || Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR)
            return false;
        
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        float x = event.getX();
        float y = event.getY();
        float p = event.getPressure();
        
        int maxPid = 0;
        int pid;
        
        if( actionCode == MotionEvent.ACTION_POINTER_DOWN )
        {
            pid = event.getPointerId( action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT );
            if( pid > maxPid )
                maxPid = pid;
            pointers[pid] = true;
        }
        else if( actionCode == MotionEvent.ACTION_POINTER_UP )
        {
            pid = event.getPointerId( action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT );
            if( pid > maxPid )
                maxPid = pid;
            pointers[pid] = false;
        }
        else if( actionCode == MotionEvent.ACTION_DOWN )
        {
            for( int i = 0; i < event.getPointerCount(); i++ )
            {
                pid = event.getPointerId( i );
                if( pid > maxPid )
                    maxPid = pid;
                pointers[pid] = true;
            }
        }
        else if( actionCode == MotionEvent.ACTION_UP || actionCode == MotionEvent.ACTION_CANCEL )
        {
            for( int i = 0; i < 256; i++ )
            {
                pointers[i] = false;
                pointerX[i] = -1;
                pointerY[i] = -1;
            }
        }
        
        for( int i = 0; i < event.getPointerCount(); i++ )
        {
            pid = event.getPointerId( i );
            if( pointers[pid] )
            {
                if( pid > maxPid )
                    maxPid = pid;
                pointerX[pid] = (int) event.getX( i );
                pointerY[pid] = (int) event.getY( i );
            }
        }
        Globals.touchscreenView.updatePointers( pointers, pointerX, pointerY, maxPid );
        return true;
    }    
}
