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
package paulscode.android.mupen64plusae.game;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import paulscode.android.mupen64plusae.input.TouchController;
import paulscode.android.mupen64plusae.input.map.TouchMap;
import paulscode.android.mupen64plusae.input.map.VisibleTouchMap;
import paulscode.android.mupen64plusae.jni.NativeImports.OnFpsChangedListener;
import paulscode.android.mupen64plusae.util.DeviceUtil;

public class GameOverlay extends View implements TouchController.OnStateChangedListener, OnFpsChangedListener
{
    private VisibleTouchMap mTouchMap;
    private boolean mDrawingEnabled = true;
    private boolean mFpsEnabled = false;
    private boolean mIsAnalogHiddenWhenSensor = false;
    private int mHatRefreshPeriod = 0;
    private int mHatRefreshCount = 0;
    private double mCurrentAlpha = 1.0;
    private boolean mHiding = false;
    
    public GameOverlay(Context context, AttributeSet attribs )
    {
        super( context, attribs );
        requestFocus();
    }
    
    public void initialize(VisibleTouchMap touchMap, boolean drawingEnabled, boolean fpsEnabled,
                           boolean isAnalogHiddenWhenSensor, boolean joystickAnimated )
    {
        mTouchMap = touchMap;
        mDrawingEnabled = drawingEnabled;
        mFpsEnabled = fpsEnabled;
        mIsAnalogHiddenWhenSensor = isAnalogHiddenWhenSensor;
        mHatRefreshPeriod = joystickAnimated ? 3 : 0;
    }
    
    @Override
    public void onAnalogChanged( float axisFractionX, float axisFractionY )
    {
        if( mHatRefreshPeriod > 0 && mDrawingEnabled )
        {
            // Increment the count since last refresh
            mHatRefreshCount++;
            
            // If stick re-centered, always refresh
            if( axisFractionX == 0 && axisFractionY == 0 )
                mHatRefreshCount = 0;
            
            // Update the analog stick assets and redraw if required
            if( mHatRefreshCount % mHatRefreshPeriod == 0 && mTouchMap != null
                    && mTouchMap.updateAnalog( axisFractionX, axisFractionY ) )
            {
                postInvalidate();
            }
        }
    }

    @Override
    public void onAutoHold( boolean autoHold, int index )
    {
        // Update the AutoHold mask, and redraw if required
        if( mTouchMap != null && mTouchMap.updateAutoHold( autoHold , index) )
        {
            postInvalidate();
        }
    }
    
    @Override
    public void onFpsChanged( int fps )
    {
        // Update the FPS indicator assets, and redraw if required
        if( mTouchMap != null && mTouchMap.updateFps( fps ) )
            postInvalidate();
    }
    
    @Override
    protected void onSizeChanged( int w, int h, int oldw, int oldh )
    {
        // Recompute skin layout geometry
        if( mTouchMap != null )
            mTouchMap.resize( w, h, DeviceUtil.getDisplayMetrics( this ) );
        super.onSizeChanged( w, h, oldw, oldh );
    }
    
    @Override
    protected void onDraw( Canvas canvas )
    {
        if( mTouchMap == null || canvas == null )
            return;
        
        if( mDrawingEnabled )
        {
            // Redraw the static buttons
            mTouchMap.drawButtons( canvas );
        
            // Redraw the dynamic analog stick
            mTouchMap.drawAnalog( canvas );
            
            // Redraw the autoHold mask
            mTouchMap.drawAutoHold( canvas );
        }
        
        if( mFpsEnabled )
        {
            // Redraw the dynamic frame rate info
            mTouchMap.drawFps( canvas );
        }
    }

    @Override
    public void onSensorEnabled(boolean sensorEnabled) {
        if (mTouchMap != null && mIsAnalogHiddenWhenSensor) {
            mTouchMap.setAnalogEnabled(!sensorEnabled);
        }
        onAutoHold(sensorEnabled, TouchMap.TOGGLE_SENSOR);
    }

    final Handler mHandler = new Handler();
    Runnable mShowTouchscreen = new Runnable() {
        @Override
        public void run() {
            if(mCurrentAlpha < 1.0) {
                if (mTouchMap != null) {
                    mTouchMap.setTouchControllerAlpha(mCurrentAlpha);
                    postInvalidate();
                    mCurrentAlpha += 0.032;
                }
                mHandler.postDelayed(mShowTouchscreen, 16);

            } else {
                // Show touch controls
                if( mTouchMap != null && mTouchMap.showTouchController() )
                    postInvalidate();
            }
        }
    };

    @Override
    public void onTouchControlsShow() {

        if(mHiding) {
            mHandler.removeCallbacks(mHideTouchscreen);

            mHiding = false;
            mHandler.post(mShowTouchscreen);
        }
    }

    Runnable mHideTouchscreen = new Runnable() {
        @Override
        public void run() {
            if(mCurrentAlpha > 0) {
                if (mTouchMap != null) {
                    mTouchMap.setTouchControllerAlpha(mCurrentAlpha);
                    postInvalidate();
                    mCurrentAlpha -= 0.016;
                }
                mHandler.postDelayed(mHideTouchscreen, 16);

            } else {
                // Hide touch controls
                if( mTouchMap != null && mTouchMap.hideTouchController() )
                    postInvalidate();
            }
        }
    };

    @Override
    public void onTouchControlsHide() {

        if(!mHiding) {
            mHandler.removeCallbacks(mShowTouchscreen);

            mHiding = true;
            mHandler.post(mHideTouchscreen);
        }
    }

    public void onDestroy()
    {
        mHandler.removeCallbacks(mHideTouchscreen);
        mHandler.removeCallbacks(mShowTouchscreen);
    }
}
