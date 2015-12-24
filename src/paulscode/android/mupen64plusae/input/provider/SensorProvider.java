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

        // Assuming we are in landcape mode

        // The most common axe used by the accelerometer: left-right
        updateStrengthAxisX(strengths, gX, gY, gZ);

        // A less common axe used by the accelerometer: the other axe up-down
        updateStrengthAxisY(strengths, gX, gZ);

        notifyListeners(mInputCodes, strengths, 0);
    }

    private void updateStrengthAxisX(float[] strengths, float gX, float gY, float gZ) {
        // Acceleration value of XZ
        float gXZ = (float) Math.sqrt(gX * gX + gZ * gZ);
        float yAngle = (float) calculateAngle(gY, gXZ);

        // Accelerometer's Y axis is mapped to joystick's AXIS_X
        float xStrength = angleToStrength(yAngle);
        boolean isPositive = xStrength > 0;
        if (isPositive) {
            strengths[0] = xStrength;
            strengths[1] = 0;
        } else {
            strengths[0] = 0;
            strengths[1] = -xStrength;
        }
    }

    private void updateStrengthAxisY(float[] strengths, float gX, float gZ) {
        float xAngle = (float) calculateAngle(gX, gZ);

        // Default angle for strength=0: Pi/3 (=60°, more vertical than
        // horizontal)
        xAngle = xAngle - (float) Math.PI / 3;// TODO: make it configurable
        float yStrength = -angleToStrength(xAngle);
        if (yStrength > 0) {
            strengths[2] = 0;
            strengths[3] = yStrength;
        } else {
            strengths[2] = -yStrength;
            strengths[3] = 0;
        }
    }

    /**
     * @return arc tangent of the ratio of the 2 args, avoiding to divide by 0
     */
    private float calculateAngle(float value, float adjacentValue) {
        if (Math.abs(value) <= Math.abs(adjacentValue)) {
            // Classic arc tangent
            return (float) Math.atan(value / adjacentValue);
        } else {
            // Calculating arc tangent with the opposite ratio. Result is the
            // same as above, unless if adjacentValue=0 (avoiding error).
            return (float) (Math.signum(value) * Math.PI / 2 - Math.atan(adjacentValue / value));
        }
    }

    private float angleToStrength(float angle) {
        // Angle which corresponds to strength=1 (with a factor of magnitude,
        // which is configured on the controller)
        float strengthMaxDegree = 15; // 15°
        return angle / (float) Math.PI * 180 / strengthMaxDegree;
    }
}
