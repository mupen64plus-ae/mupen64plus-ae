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

import android.os.Handler;

import com.bda.controller.Controller;
import com.bda.controller.ControllerListener;
import com.bda.controller.KeyEvent;
import com.bda.controller.MotionEvent;
import com.bda.controller.StateEvent;

/**
 * A class for transforming MOGA input events into a common format.
 */
public class MogaProvider extends AbstractProvider implements ControllerListener
{
    private final Controller mController;
    private final int[] mInputCodes;
    
    /**
     * Instantiates a new MOGA provider.
     */
    public MogaProvider( Controller controller )
    {
        mController = controller;
        mController.setListener( this, new Handler() );

        mInputCodes = new int[10];
        //@formatter:off
        mInputCodes[0] = axisToInputCode( MotionEvent.AXIS_X,        true  );
        mInputCodes[1] = axisToInputCode( MotionEvent.AXIS_X,        false );
        mInputCodes[2] = axisToInputCode( MotionEvent.AXIS_Y,        true  );
        mInputCodes[3] = axisToInputCode( MotionEvent.AXIS_Y,        false );
        mInputCodes[4] = axisToInputCode( MotionEvent.AXIS_Z,        true  );
        mInputCodes[5] = axisToInputCode( MotionEvent.AXIS_Z,        false );
        mInputCodes[6] = axisToInputCode( MotionEvent.AXIS_RZ,       true  );
        mInputCodes[7] = axisToInputCode( MotionEvent.AXIS_RZ,       false );
        mInputCodes[8] = axisToInputCode( MotionEvent.AXIS_LTRIGGER, true  );
        mInputCodes[9] = axisToInputCode( MotionEvent.AXIS_RTRIGGER, true  );
        //@formatter:on
    }
    
    @Override
    public void onKeyEvent( KeyEvent event )
    {
        int inputCode = event.getKeyCode();
        float strength = event.getAction() == KeyEvent.ACTION_DOWN ? 1 : 0;
        int hardwareId = getHardwareId( event );
        
        // Notify listeners about new input data
        notifyListeners( inputCode, strength, hardwareId );
    }
    
    @Override
    public void onMotionEvent( MotionEvent event )
    {
        // Read all the requested axes
        float[] strengths = new float[mInputCodes.length];
        for( int i = 0; i < mInputCodes.length; i++ )
        {
            int inputCode = mInputCodes[i];
            
            // Compute the axis code from the input code
            int axisCode = inputToAxisCode( inputCode );
            
            // Get the analog value using the MOGA API
            float strength = event.getAxisValue( axisCode );
            
            // If the strength points in the correct direction, record it
            boolean direction1 = inputToAxisDirection( inputCode );
            boolean direction2 = strength > 0;
            if( direction1 == direction2 )
                strengths[i] = Math.abs( strength );
            else
                strengths[i] = 0;
        }
        int hardwareId = getHardwareId( event );
        
        // Notify listeners about new input data
        notifyListeners( mInputCodes, strengths, hardwareId );
    }
    
    @Override
    public void onStateEvent( StateEvent arg0 )
    {
    }
}
