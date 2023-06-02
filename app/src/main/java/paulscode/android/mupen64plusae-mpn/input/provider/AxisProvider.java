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
package paulscode.android.mupen64plusae-mpn.input.provider;

import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import paulscode.android.mupen64plusae-mpn.input.map.AxisMap;

/**
 * A class for transforming Android MotionEvent inputs into a common format.
 */
public class AxisProvider extends AbstractProvider implements View.OnGenericMotionListener
{
    /** The input codes to listen for. */
    private final int[] mInputCodes;
    
    /** The default number of input codes to listen for. */
    private static final int DEFAULT_NUM_INPUTS = 128;

    /** Max flat value to determine if we should override it */
    private static final float MAX_FLAT = 0.5f;

    /** Minimum flat value to determine if we should override it */
    private static final float MIN_FLAT = 0.15f;

    /**
     * Instantiates a new axis provider.
     */
    public AxisProvider()
    {
        // By default, provide data from all possible axes
        mInputCodes = new int[DEFAULT_NUM_INPUTS];
        for( int i = 0; i < mInputCodes.length; i++ )
            mInputCodes[i] = -( i + 1 );
    }

    /**
     * Instantiates a new axis provider.
     *
     * @param view The view receiving MotionEvent data.
     */
    public AxisProvider( View view )
    {
        this();

        // Connect the input source
        view.setOnGenericMotionListener(this);
        // Request focus for proper listening
        view.requestFocus();
    }

    /**
     * Manually dispatches a MotionEvent through the provider's listening chain.
     * 
     * @param event The MotionEvent object containing full information about the event.
     * 
     * @return True if the listener has consumed the event, false otherwise.
     */
    @Override
    public boolean onGenericMotion(View v, MotionEvent event )
    {
        boolean isJoystick = (((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) ||
                ((event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)) &&
            event.getAction() == MotionEvent.ACTION_MOVE;

        // Ignore motion events from non-joysticks (mice are a problem)
        if(!isJoystick)
            return false;

        InputDevice device = event.getDevice();

        if(device == null)
            return false;

        AxisMap axisInfo = AxisMap.getMap( device );

        // Read all the requested axes
        float[] strengths = new float[mInputCodes.length];
        for( int i = 0; i < mInputCodes.length; i++ )
        {
            int inputCode = mInputCodes[i];

            // Compute the axis code from the input code
            int axisCode = inputToAxisCode( inputCode );

            // Get the analog value using the Android API
            float strength = event.getAxisValue(axisCode);

            // Modify strength if necessary
            strength = strength != 0.0f ? normalizeStrength( strength, axisInfo, axisCode ) : 0.0f;

            // If the strength points in the correct direction, record it
            boolean direction1 = inputToAxisDirection( inputCode );
            boolean direction2 = strength > 0;
            if( direction1 == direction2 )
                strengths[i] = Math.abs( strength );
            else
                strengths[i] = 0;
        }

        notifyListeners( mInputCodes, strengths, getHardwareId(event), event.getSource() );

        return true;
    }

    private float normalizeStrength( float strength, AxisMap axisInfo, int axisCode )
    {
        if( axisInfo != null )
        {
            int axisClass = axisInfo.getClass( axisCode );

            if (axisClass == AxisMap.AXIS_CLASS_IGNORED)
            {
                // We should ignore this axis
                strength = 0;
            }
            else if (axisClass == AxisMap.AXIS_CLASS_N64_USB_STICK)
            {
                // Normalize to [-1,1]
                // The Raphnet adapters through v2.x and some other USB adapters assume the N64
                // controller produces values in the range [-127,127].  However, the official N64 spec
                // says that raw values of +/- 80 indicate full strength.  Therefore we rescale by
                // multiplying by 127/80 (dividing by 0.63).
                // http://naesten.dyndns.org:8080/psyq/man/os/osContGetReadData.html
                // http://raphnet-tech.com/products/gc_n64_usb_adapters/
                // https://github.com/mupen64plus-ae/mupen64plus-ae/issues/89
                // https://github.com/mupen64plus-ae/mupen64plus-ae/issues/99
                // https://github.com/mupen64plus-ae/mupen64plus-ae/issues/188
                // http://www.paulscode.com/forum/index.php?topic=1076
                strength = strength / 0.63f;
            }
        }
        return strength;
    }
}
