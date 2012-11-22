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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.input.provider;

import android.util.Log;

/**
 * A class for transforming Xperia PLAY native inputs into a common format.
 */
public class XperiaPlayProvider extends AbstractProvider
{
    public XperiaPlayProvider()
    {
        // Register the call-ins that the JNI code will call
        RegisterThis();
    }
    
   
    
    
    
    private static int[] touchPadPointerY = new int[256];
    private static int[] touchPadPointerX = new int[256];
    private static boolean[] touchPadPointers = new boolean[256];
    private static final int PAD_WIDTH = 966;
    private static final int PAD_HEIGHT = 360;
    
    public native int RegisterThis();

    public void touchPadBeginEvent()
    {
        Log.i( "XperiaPlayProvider", "touchPadBeginEvent" );
    }
    
    public void touchPadPointerDown( int pointer_id )
    {
        Log.i( "XperiaPlayProvider", "touchPadPointerDown" );
        touchPadPointers[pointer_id] = true;
    }
    
    public void touchPadPointerUp( int pointer_id )
    {
        Log.i( "XperiaPlayProvider", "touchPadPointerUp" );
        touchPadPointers[pointer_id] = false;
        touchPadPointerX[pointer_id] = -1;
        touchPadPointerY[pointer_id] = -1;
    }
    
    public void touchPadPointerPosition( int pointer_id, int x, int y )
    {
        Log.i( "XperiaPlayProvider", "touchPadPointerPosition" );
        touchPadPointers[pointer_id] = true;
        touchPadPointerX[pointer_id] = x;
        touchPadPointerY[pointer_id] = PAD_HEIGHT - y;
    }
    
    public void touchPadEndEvent()
    {
        Log.i( "XperiaPlayProvider", "touchPadEndEvent" );
    }
    
    public void touchScreenBeginEvent()
    {
        Log.i( "XperiaPlayProvider", "touchScreenBeginEvent" );
    }
    
    public void touchScreenPointerDown( int pointer_id )
    {
        Log.i( "XperiaPlayProvider", "touchScreenPointerDown" );
    }
    
    public void touchScreenPointerUp( int pointer_id )
    {
        Log.i( "XperiaPlayProvider", "touchScreenPointerUp" );
    }
    
    public void touchScreenPointerPosition( int pointer_id, int x, int y )
    {
        Log.i( "XperiaPlayProvider", "touchScreenPointerPosition" );
    }
    
    public void touchScreenEndEvent()
    {
        Log.i( "XperiaPlayProvider", "touchScreenEndEvent" );
    }
    
    public void onTouchScreen( boolean[] pointers, int[] pointerX, int[] pointerY, int maxPid )
    {
        Log.i( "XperiaPlayProvider", "onTouchScreen" );
    }
    
    public boolean onNativeKey( int action, int keycode )
    {
        Log.i( "XperiaPlayProvider", "onNativeKey" );
        return true;
    }
}
