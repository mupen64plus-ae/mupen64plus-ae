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
 * Authors: Pierre Renié
 */
package paulscode.android.mupen64plusae.input.provider;

import com.bda.controller.MotionEvent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Emulates a joystick using accelerometer sensor
 */
public class SensorProvider extends AbstractProvider implements SensorEventListener {
    private final int[] mInputCodes;

    public SensorProvider(SensorManager sensorManager) {
        mInputCodes = getEmulatedJoystickCodes();
    }

    public static int[] getEmulatedJoystickCodes() {
        int[] inputCodes = new int[4];
        inputCodes[0] = axisToInputCode(MotionEvent.AXIS_X, true);
        inputCodes[1] = axisToInputCode(MotionEvent.AXIS_X, false);
        inputCodes[2] = axisToInputCode(MotionEvent.AXIS_Y, true);
        inputCodes[3] = axisToInputCode(MotionEvent.AXIS_Y, false);
        return inputCodes;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] strengths = new float[mInputCodes.length];

        float gX = event.values[0];
        float gY = event.values[1];
        float gZ = event.values[2];
        float yAngle = (float) calculateAngle(gY, (float) Math.sqrt(gX * gX + gZ * gZ));
        float xStrength = angleToStrength(yAngle);// Range is now [-5, 5]
        boolean isPositive = xStrength > 0;
        if (isPositive) {
            strengths[0] = xStrength;
            strengths[1] = 0;
        } else {
            strengths[0] = 0;
            strengths[1] = -xStrength;
        }

        // TODO: make AXIS_Y configurable and disablable or remove this feature
        float xAngle = (float) calculateAngle(gX, gZ);
        xAngle = xAngle - (float) Math.PI / 3;
        float yStrength = -angleToStrength(xAngle);
        if (yStrength > 0) {
            strengths[2] = 0;
            strengths[3] = yStrength;
        } else {
            strengths[2] = -yStrength;
            strengths[3] = 0;
        }

        notifyListeners(mInputCodes, strengths, 0);
    }

    private float calculateAngle(float value, float adjacentValue) {
        if (Math.abs(value) <= Math.abs(adjacentValue)) {
            return (float) Math.atan(value / adjacentValue);
        } else {
            return (float) (Math.signum(value) * Math.PI / 2 - Math.atan(adjacentValue / value));
        }
    }

    private float angleToStrength(float angle) {
        // Angle which corresponds to strength=1 (with a factor of magnitude,
        // which is configured on the controller)
        float amplitudeMaxDegree = 15; // 15°
        return angle / (float) Math.PI * 180 / amplitudeMaxDegree;
    }
}
