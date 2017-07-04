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

import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.MotionEvent;
import android.view.View;

import paulscode.android.mupen64plusae.input.map.AxisMap;

/**
 * A class for transforming Android MotionEvent inputs into a common format.
 */
public class AxisProvider extends AbstractProvider implements View.OnGenericMotionListener
{
    /** The input codes to listen for. */
    private int[] mInputCodes;
    
    /** The default number of input codes to listen for. */
    private static final int DEFAULT_NUM_INPUTS = 128;

    /** Max flat value to determine if we should override it */
    private static final float MAX_FLAT = 0.5f;

    /** Flat value override if the os privided one is above MAX_FLAT */
    private static final float FLAT_OVERRIDE = 0.25f;

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
     * Restricts listening to a set of universal input codes.
     * 
     * @param inputCodeFilter The new input codes to listen for.
     */
    public void setInputCodeFilter( int[] inputCodeFilter )
    {
        mInputCodes = inputCodeFilter.clone();
    }

    private static float getCenteredAxis(MotionEvent event, InputDevice device, int axis)
    {
        final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        if (range != null) {
            float flat = range.getFlat();
            final float value = event.getAxisValue(axis);

            //Some devices with bad drivers report invalid flat regions
            if(flat > MAX_FLAT || flat < 0.0)
            {
                flat = FLAT_OVERRIDE;
            }

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value;
            }
        }

        return 0;
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
            float strength = getCenteredAxis(event, device, axisCode);

            // Modify strength if necessary
            strength = strength != 0 ? normalizeStrength( strength, axisInfo, device, axisCode ) : 0.0f;

            // If the strength points in the correct direction, record it
            boolean direction1 = inputToAxisDirection( inputCode );
            boolean direction2 = strength > 0;
            if( direction1 == direction2 )
                strengths[i] = Math.abs( strength );
            else
                strengths[i] = 0;
        }

        // Notify listeners about new input data
        notifyListeners( mInputCodes, strengths, getHardwareId( event ) );

        return true;
    }

    private float normalizeStrength( float strength, AxisMap axisInfo, InputDevice device,
                                     int axisCode )
    {
        if( axisInfo != null )
        {
            int axisClass = axisInfo.getClass( axisCode );
            float tempStrengh = 0;

            if( axisClass == AxisMap.AXIS_CLASS_IGNORED )
            {
                // We should ignore this axis
                strength = 0;
            }
            else if( device != null )
            {
                // We should normalize this axis
                MotionRange motionRange = device.getMotionRange( axisCode, InputDevice.SOURCE_JOYSTICK );
                if( motionRange != null )
                {
                    float flat = motionRange.getFlat();
                    //Some devices with bad drivers report invalid flat regions
                    if(flat > MAX_FLAT || flat < 0.0)
                    {
                        flat = FLAT_OVERRIDE;
                    }

                    switch( axisClass )
                    {
                        case AxisMap.AXIS_CLASS_NORMAL:
                            // Normalize
                            //strength = ( strength - motionRange.getMin() ) / motionRange.getRange() * 2f - 1f;
                            tempStrengh = (Math.abs(strength) - flat) / (1.0f - flat);
                            //Restore sign
                            strength = tempStrengh * Math.signum(strength);
                            break;
                        case AxisMap.AXIS_CLASS_N64_USB_STICK:
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
                            break;
                        case AxisMap.AXIS_CLASS_UNKNOWN:
                        default:
                            // Do nothing
                    }
                }
            }
        }
        return strength;
    }
}
