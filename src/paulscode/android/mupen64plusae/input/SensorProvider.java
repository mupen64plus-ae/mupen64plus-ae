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
 * Authors: javaxubuntu
 */
package paulscode.android.mupen64plusae.input;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import paulscode.android.mupen64plusae.util.Utility;

/**
 * Emulates a joystick using accelerometer sensor
 */
public class SensorProvider extends AbstractController implements SensorEventListener {
    private SensorManager mSensorManager;
    private boolean isPaused = true;
    private boolean mSensorEnabled = false;

    public SensorProvider(SensorManager sensorManager) {
        mSensorManager = sensorManager;
    }

    public boolean isSensorEnabled() {
        return mSensorEnabled;
    }

    public void setSensorEnabled(boolean sensorEnabled) {
        mSensorEnabled = sensorEnabled;
        updateListener();
    }

    public void onResume() {
        isPaused = false;
        updateListener();
    }

    public void onPause() {
        isPaused = true;
        updateListener();
    }

    private void updateListener() {
        if (mSensorEnabled && isPaused == false) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_GAME);
        } else {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float gX = event.values[0];
        float gY = event.values[1];
        float gZ = event.values[2];

        // Assuming we are in landcape mode

        // The most common axe used by the accelerometer: left-right
        float rawX = getStrengthAxisX(gX, gY, gZ);

        // A less common axe used by the accelerometer: the other axe up-down
        float rawY = getStrengthAxisY(gX, gZ);

        float magnitude = (float) Math.sqrt((rawX * rawX) + (rawY * rawY));

        // TODO:add a configurable sensitivityFraction
        // Copy-paste from PeripheralController
        // Normalize the vector
        float normalizedX = rawX / magnitude;
        float normalizedY = rawY / magnitude;

        magnitude = Utility.clamp(magnitude, 0f, 1f);

        mState.axisFractionX = normalizedX * magnitude;
        mState.axisFractionY = normalizedY * magnitude;
        notifyChanged();
    }

    private float getStrengthAxisX(float gX, float gY, float gZ) {
        // Acceleration value of XZ
        float gXZ = (float) Math.sqrt(gX * gX + gZ * gZ);
        float yAngle = (float) calculateAngle(gY, gXZ);

        // Accelerometer's Y axis is mapped to joystick's AXIS_X
        return angleToStrength(yAngle);
    }

    private float getStrengthAxisY(float gX, float gZ) {
        float xAngle = (float) calculateAngle(gX, gZ);

        // Default angle for strength=0: Pi/3 (=60°, more vertical than
        // horizontal)
        // TODO: make it configurable and disablable
        xAngle = xAngle - (float) Math.PI / 3;
        return -angleToStrength(xAngle);
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
