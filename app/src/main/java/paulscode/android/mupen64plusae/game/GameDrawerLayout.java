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
package paulscode.android.mupen64plusae.game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.input.map.TouchMap;

// Android's DrawerLayout intercepts touches along the left edge of the screen
// so it can have the drawer peek out when you press and hold on the left edge.

// As this would obviously interfere with gameplay, where the user is expected
// to press and hold on buttons that could be on the left edge of the screen,
// override DrawerLayout to ignore touches on the virtual gamepad!

public class GameDrawerLayout extends androidx.drawerlayout.widget.DrawerLayout
{
    private TouchMap mTouchMap;
    private List<MotionEvent> ignore = new ArrayList<MotionEvent>();
    private long mLastEdgeTime = 0;
    private boolean mForceDrawer = false;
    
    public GameDrawerLayout( Context context, AttributeSet attrs )
    {
        super( context, attrs );

        addDrawerListener(new DrawerLayout.DrawerListener(){

            @Override
            public void onDrawerClosed(View arg0)
            {
                mForceDrawer = false;
            }

            @Override
            public void onDrawerOpened(View arg0)
            {

            }

            @Override
            public void onDrawerSlide(View arg0, float arg1)
            {

            }

            @Override
            public void onDrawerStateChanged(int newState)
            {

            }

        });
        mTouchMap = null;
    }
    
    public void setTouchMap( TouchMap touchMap )
    {
        mTouchMap = touchMap;
    }

    public void setSwipGestureEnabled( boolean enable )
    {
        if(enable)
        {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
        else
        {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    @Override
    public boolean onInterceptTouchEvent( MotionEvent event )
    {
        final int edgeXSidebarTrigger = 30;
        final int edgeIgnorePeriod = 250;

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
            int actionIndex = event.getActionIndex();
            int xLocation = (int) event.getX( actionIndex );
            if( xLocation < edgeXSidebarTrigger )
                mLastEdgeTime = currentEventTime;
        }

        long lastEdgeTime = currentEventTime - mLastEdgeTime;

        if( ignore.contains( event ) )
        {
            if( upAction )
                ignore.remove( event );
            return false;
        }
        else if( actionCode == MotionEvent.ACTION_POINTER_DOWN
                || ( actionCode == MotionEvent.ACTION_DOWN && lastEdgeTime < edgeIgnorePeriod ) )
        {

            // Ignore secondary inputs and inputs too close to the most recent one (0.25 seconds)
            ignore.add( event );
            return false;
        }
        else if(mForceDrawer ||
                (actionCode == MotionEvent.ACTION_DOWN) && lastEdgeTime >= edgeIgnorePeriod )
        {
            int actionIndex = event.getActionIndex();
            int xLocation = (int) event.getX( actionIndex );

            if( xLocation < edgeXSidebarTrigger || mForceDrawer)
            {
                mForceDrawer = true;

                // Let the parent DrawerLayout deal with it
                try
                {
                    return super.onInterceptTouchEvent( event );
                }
                catch( Exception ex )
                {
                    return false;
                }
            }
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
                Point displacementPoint = mTouchMap.getAnalogDisplacementOriginal( xLocation, yLocation);
                // Add a slightly larger hit area around the analog stick,
                // by artificially shrinking the size of the displacement
                displacementPoint.x *= 0.9;
                displacementPoint.y *= 0.9;

                if( mTouchMap.isInCaptureRange( displacementPoint ) )
                {
                    ignore.add( event );
                    return false;
                }
            }
        }

        if(actionCode != MotionEvent.ACTION_DOWN && actionCode != MotionEvent.ACTION_MOVE)
        {
            // Let the parent DrawerLayout deal with it
            try
            {
                return super.onInterceptTouchEvent( event );
            }
            catch( Exception ex )
            {
                return false;
            }
        }

        return false;
    }
    
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // Let the parent DrawerLayout deal with it
        try
        {
            return super.onTouchEvent( event );
        }
        catch( Exception ex )
        {
            // For some reason this is very prone to crashing here when using multitouch:
            //  at android.support.v4.widget.ViewDragHelper.clearMotionHistory(ViewDragHelper.java:794)
            // But fortunately this is very unimportant, so we can safely ignore it
            // The source code is here if you want to attempt a fix:
            // https://github.com/android/platform_frameworks_support/blob/master/v4/java/android/support/v4/widget/ViewDragHelper.java
            return false;
        }   
    }
}
