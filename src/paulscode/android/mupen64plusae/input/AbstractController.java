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
package paulscode.android.mupen64plusae.input;

import paulscode.android.mupen64plusae.NativeMethods;

public abstract class AbstractController
{
    protected boolean mDpadR;
    protected boolean mDpadL;
    protected boolean mDpadD;
    protected boolean mDpadU;
    protected boolean mBtnStart;
    protected boolean mBtnZ;
    protected boolean mBtnB;
    protected boolean mBtnA;
    protected boolean mBtnCR;
    protected boolean mBtnCL;
    protected boolean mBtnCD;
    protected boolean mBtnCU;
    protected boolean mBtnR;
    protected boolean mBtnL;
    // protected boolean mBtnRumble;
    // protected boolean mBtnMempak;
    protected float mAxisFractionX;
    protected float mAxisFractionY;
    
    private int mPlayerNumber = 1;
    private static final float AXIS_SCALE = 80;
    
    protected void notifyChanged()
    {
        // Array must be the same order as EButton listing in plugin.h! (input-sdl plug-in)
        boolean[] buttons = new boolean[] {
            mDpadR,
            mDpadL,
            mDpadD,
            mDpadU,
            mBtnStart,
            mBtnZ,
            mBtnB,
            mBtnA,
            mBtnCR,
            mBtnCL,
            mBtnCD,
            mBtnCU,
            mBtnR,
            mBtnL };
        int axisX = (int) ( mAxisFractionX * AXIS_SCALE );
        int axisY = (int) ( mAxisFractionY * AXIS_SCALE );
        NativeMethods.updateVirtualGamePadStates( mPlayerNumber, buttons, axisX, axisY );
    }
    
    public void clearState()
    {
        mDpadR = mDpadL = mDpadD = mDpadU
                = mBtnStart = mBtnZ = mBtnB = mBtnA
                = mBtnCR = mBtnCL = mBtnCD = mBtnCU
                = mBtnR = mBtnL = false;
        mAxisFractionX = mAxisFractionY = 0;
        notifyChanged();
    }
    
    public int getPlayerNumber()
    {
        return mPlayerNumber;
    }
    
    public void setPlayerNumber( int playerNumber )
    {
        mPlayerNumber = playerNumber;
    }
}
