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
package paulscode.android.mupen64plusae.input.provider;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.View;

/**
 * A class to simplify the handling of key and touch events from a native activity. This class
 * impersonates an Android view, so that it can be used as a drop-in replacement in client software.
 */
public class NativeInputSource extends View
{
    // Touchpad dimensions, in pixels
    public static final int PAD_WIDTH = 966;
    public static final int PAD_HEIGHT = 360;
    
    /**
     * Instantiates a new native input source.
     * 
     * @param context The context associated with the input events.
     */
    public NativeInputSource( Context context )
    {
        super( context );
        RegisterThis();
    }
    
    public native int RegisterThis();
    
    /**
     * Called by the native activity when a key event occurs.
     * 
     * @param action  The kind of action being performed, such as {@link MotionEvent#ACTION_DOWN}.
     * @param keyCode The key code.
     * 
     * @return True if the event was handled, false otherwise.
     */
    public boolean onNativeKey( int action, int keyCode )
    {
        return dispatchKeyEvent( new KeyEvent( action, keyCode ) );
    }
    
    /**
     * Called by the native activity when a touch event occurs.
     * 
     * @param source       The source of this event.
     * @param action       The kind of action being performed, such as {@link MotionEvent#ACTION_DOWN}.
     * @param pointerCount The number of pointers that will be in this event.
     * @param pointerIds   An array of <em>pointerCount</em> values providing an identifier for each pointer.
     * @param pointerX     An array of <em>pointerCount</em> values providing the x-coordinate for each pointer.
     * @param pointerY     An array of <em>pointerCount</em> values providing the y-coordinate for each pointer.
     *            
     * @return True if the event was handled, false otherwise.
     */
    @TargetApi( 9 )
    public boolean onNativeTouch( int source, int action, int pointerCount, int[] pointerIds,
            float[] pointerX, float[] pointerY )
    {
        PointerCoords[] pointerCoords = new PointerCoords[pointerCount];
        for( int i = 0; i < pointerCount; i++ )
        {
            pointerCoords[i] = new PointerCoords();
            pointerCoords[i].x = pointerX[i];
            pointerCoords[i].y = pointerY[i];
        }
        return onNativeTouch( 0, 0, action, pointerCount, pointerIds, pointerCoords, 0, 1f, 1f, 0,
                0, source, 0 );
    }
    
    /**
     * Called by the native activity when a touch event occurs.
     * 
     * @param downTime The time (in ms) when the user originally pressed down to start a stream of
     *            position events. This must be obtained from {@link SystemClock#uptimeMillis()}.
     * @param eventTime The the time (in ms) when this specific event was generated. This must be
     *            obtained from {@link SystemClock#uptimeMillis()}.
     * @param action The kind of action being performed, such as {@link MotionEvent#ACTION_DOWN}.
     * @param pointerCount The number of pointers that will be in this event.
     * @param pointerIds An array of <em>pointerCount</em> values providing an identifier for each
     *            pointer.
     * @param pointerCoords An array of <em>pointerCount</em> values providing a
     *            {@link PointerCoords} coordinate object for each pointer.
     * @param metaState The state of any meta / modifier keys that were in effect when the event was
     *            generated.
     * @param xPrecision The precision of the X coordinate being reported.
     * @param yPrecision The precision of the Y coordinate being reported.
     * @param deviceId The id for the device that this event came from. An id of zero indicates that
     *            the event didn't come from a physical device; other numbers are arbitrary and you
     *            shouldn't depend on the values.
     * @param edgeFlags A bitfield indicating which edges, if any, were touched by this MotionEvent.
     * @param source The source of this event.
     * @param flags The motion event flags
     * 
     * @return True if the event was handled, false otherwise.
     */
    @TargetApi( 9 )
    @SuppressWarnings( "deprecation" )
    public boolean onNativeTouch( long downTime, long eventTime, int action, int pointerCount,
            int[] pointerIds, PointerCoords[] pointerCoords, int metaState, float xPrecision,
            float yPrecision, int deviceId, int edgeFlags, int source, int flags )
    {
        // Use the deprecated method, since that is what the native activity uses
        MotionEvent event = MotionEvent.obtain( downTime, eventTime, action, pointerCount,
                pointerIds, pointerCoords, metaState, xPrecision, yPrecision, deviceId, edgeFlags,
                source, flags );
        
        boolean result = dispatchTouchEvent( event );
        event.recycle();
        return result;
    }
}
