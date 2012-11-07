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
 * Authors: TODO: *Implement XperiaPlayController, merging commonalities with TouchscreenController
 */
package paulscode.android.mupen64plusae.input;

import paulscode.android.mupen64plusae.input.transform.TouchMap;

public class XperiaPlayController extends AbstractController
{
    // Pixel dimensions of the pad (assuming they're constant)
    public static final int PAD_WIDTH = 966;
    public static final int PAD_HEIGHT = 360;
    
    private static int[] touchPadPointerY = new int[256];
    private static int[] touchPadPointerX = new int[256];
    private static boolean[] touchPadPointers = new boolean[256];
    @SuppressWarnings( "unused" )
    private TouchMap mTouchMap;
    
    public XperiaPlayController( TouchMap touchMap )
    {
        mTouchMap = touchMap;
    }
    
    public void touchPadBeginEvent()
    {
    }
    
    public void touchPadPointerDown( int pointer_id )
    {
        touchPadPointers[pointer_id] = true;
    }
    
    public void touchPadPointerUp( int pointer_id )
    {
        touchPadPointers[pointer_id] = false;
        touchPadPointerX[pointer_id] = -1;
        touchPadPointerY[pointer_id] = -1;
    }
    
    public void touchPadPointerPosition( int pointer_id, int x, int y )
    {
        touchPadPointers[pointer_id] = true;
        touchPadPointerX[pointer_id] = x;
        touchPadPointerY[pointer_id] = XperiaPlayController.PAD_HEIGHT - y;
    }
    
    public void touchPadEndEvent()
    {
    }
    
    public void touchScreenBeginEvent()
    {
    }
    
    public void touchScreenPointerDown( int pointer_id )
    {
    }
    
    public void touchScreenPointerUp( int pointer_id )
    {
    }
    
    public void touchScreenPointerPosition( int pointer_id, int x, int y )
    {
    }
    
    public void touchScreenEndEvent()
    {
    }
    
    public void onTouchScreen( boolean[] pointers, int[] pointerX, int[] pointerY, int maxPid )
    {
    }
    
    public boolean onNativeKey( int action, int keycode )
    {
        return true;
    }
}
