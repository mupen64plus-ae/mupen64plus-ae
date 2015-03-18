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
package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.input.map.TouchMap;

import android.content.Context;
import android.graphics.Point;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

// Android's DrawerLayout intercepts touches along the left edge of the screen
// so it can have the drawer peek out when you press and hold on the left edge.

// As this would obviously interfere with gameplay, where the user is expected
// to press and hold on buttons that could be on the left edge of the screen,
// override DrawerLayout to ignore touches on the virtual gamepad!

public class DrawerLayout extends android.support.v4.widget.DrawerLayout
{
    private TouchMap mTouchMap;
    private List<MotionEvent> ignore = new ArrayList<MotionEvent>();
    private long mLastEdgeTime = 0;
    
    public DrawerLayout( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        mTouchMap = null;
    }
    
    public void setTouchMap( TouchMap touchMap )
    {
        mTouchMap = touchMap;
    }
    
    @Override
    public boolean onInterceptTouchEvent( MotionEvent event )
    {
        // Only intercept this touch event if it is not directly over a touchscreen input
        // (So the game sidebar is never accidentally triggered)
        
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        long currentEventTime = System.currentTimeMillis();
        
        boolean upAction = ( actionCode == MotionEvent.ACTION_UP
                || actionCode == MotionEvent.ACTION_CANCEL || actionCode == MotionEvent.ACTION_POINTER_UP );
        
        // If the touch ended along the left edge, ignore edge swipes for a little while
        if( upAction )
        {
            int actionIndex = MotionEventCompat.getActionIndex( event );
            int xLocation = (int) event.getX( actionIndex );
            if( xLocation < 10 )
                mLastEdgeTime = currentEventTime;
        }
        
        if( ignore.contains( event ) )
        {
            if( upAction )
                ignore.remove( event );
            return false;
        }
        else if( actionCode == MotionEvent.ACTION_POINTER_DOWN
                || ( actionCode == MotionEvent.ACTION_DOWN && currentEventTime - mLastEdgeTime < 250 ) )
        {
            // Ignore secondary inputs and inputs too close to the most recent one (0.25 seconds)
            ignore.add( event );
            return false;
        }
        else if( actionCode == MotionEvent.ACTION_DOWN && !isDrawerOpen( GravityCompat.START )
                && mTouchMap != null )
        {
            for( int i = 0; i < event.getPointerCount(); i++ )
            {
                int xLocation = (int) event.getX( i );
                int yLocation = (int) event.getY( i );
                
                // See if it touches the d-pad or the C buttons,
                // as they are small enough to interfere with left edge swipes
                // (fortunately placing the C buttons on the left is unusual)
                int buttonIndex = mTouchMap.getButtonPress( xLocation, yLocation );
                if( buttonIndex != TouchMap.UNMAPPED )
                {
                    if( "dpad".equals( TouchMap.ASSET_NAMES.get( buttonIndex ) )
                            || "groupC".equals( TouchMap.ASSET_NAMES.get( buttonIndex ) ) )
                    {
                        ignore.add( event );
                        return false;
                    }
                }
                
                // See if it touches the analog stick
                Point point = mTouchMap.getAnalogDisplacement( xLocation, yLocation );
                
                int dX = point.x;
                int dY = point.y;
                float displacement = FloatMath.sqrt( ( dX * dX ) + ( dY * dY ) );
                
                // Add a slightly larger hit area around the analog stick,
                // by artificially shrinking the size of the displacement
                displacement = displacement * 0.9f;
                
                if( mTouchMap.isInCaptureRange( displacement ) )
                {
                    ignore.add( event );
                    return false;
                }
            }
        }
        
        // Let the parent DrawerLayout deal with it
        try
        {
            return super.onInterceptTouchEvent( event );
        }
        catch( Exception ex )
        {
            // For some reason this is very prone to crashing here when using multitouch:
            // android.support.v4.widget.ViewDragHelper.shouldInterceptTouchEvent
            // But fortunately this is very unimportant, so we can safely ignore it
            // The source code is here if you want to attempt a fix:
            // https://github.com/android/platform_frameworks_support/blob/master/v4/java/android/support/v4/widget/ViewDragHelper.java
            return false;
        }
    }
}
