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
import paulscode.android.mupen64plusae.input.TouchController.OnStateChangedListener;
import paulscode.android.mupen64plusae.util.Utility;

/**
 * Emulates a joystick using accelerometer sensor
 */
public class SensorController extends AbstractController implements SensorEventListener {
    private final SensorManager mSensorManager;

    /** The state change listener. */
    private final OnStateChangedListener mListener;
    private final int[] sensorEventValuesRefX, sensorEventAdjacentValuesRefX, sensorEventValuesRefY,
            sensorEventAdjacentValuesRefY;
    private final float sensorSensitivityX, sensorSensitivityY;
    private boolean isPaused = true;
    private boolean mSensorEnabled = false;

    public SensorController(SensorManager sensorManager, OnStateChangedListener listener, String sensorAxisX,
            int sensorSensitivityX, String sensorAxisY, int sensorSensitivityY) {
        mSensorManager = sensorManager;
        mListener = listener;

        String[] x = sensorAxisX.split("/");
        if (x.length == 2 && !x[0].isEmpty() && !x[1].isEmpty()) {
            sensorEventValuesRefX = xyzToSensorEventValuesRef(x[0]);
            sensorEventAdjacentValuesRefX = xyzToSensorEventValuesRef(x[1]);
        } else {
            sensorEventValuesRefX = new int[0];
            sensorEventAdjacentValuesRefX = sensorEventValuesRefX;
        }
        String[] y = sensorAxisY.split("/");
        if (y.length == 2 && !y[0].isEmpty() && !y[1].isEmpty()) {
            sensorEventValuesRefY = xyzToSensorEventValuesRef(y[0]);
            sensorEventAdjacentValuesRefY = xyzToSensorEventValuesRef(y[1]);
        } else {
            sensorEventValuesRefY = new int[0];
            sensorEventAdjacentValuesRefY = sensorEventValuesRefY;
        }
        this.sensorSensitivityX = sensorSensitivityX / 100f;
        this.sensorSensitivityY = sensorSensitivityY / 100f;
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
        // TODO: make angle configurable

        float rawX = getStrength(event.values, sensorEventValuesRefX, sensorEventAdjacentValuesRefX, 0)
                * sensorSensitivityX;
        float rawY = getStrength(event.values, sensorEventValuesRefY, sensorEventAdjacentValuesRefY, 60)
                * sensorSensitivityY;

        float magnitude = (float) Math.sqrt((rawX * rawX) + (rawY * rawY));
        float factor = magnitude > 1 ? magnitude : 1;
        mState.axisFractionX = rawX / factor;
        mState.axisFractionY = rawY / factor;
        notifyChanged();
        mListener.onAnalogChanged(mState.axisFractionX, mState.axisFractionY);
    }

    private float getStrength(float[] sensorEventValues, int[] valuesRef, int[] adjacentValuesRef,
            float idleAngleDegree) {
        if (valuesRef.length == 0 || adjacentValuesRef.length == 0) {
            return 0; // This axis is disabled
        }
        float value = calculateAcceleration(sensorEventValues, valuesRef);
        float adjacentValue = calculateAcceleration(sensorEventValues, adjacentValuesRef);
        float angle = (float) calculateAngle(value, adjacentValue) - idleAngleDegree / 180 * (float) Math.PI;
        return angleToStrength(angle);
    }

    /**
     * Calculates the total acceleration of some axis
     * 
     * @param sensorEventValues
     *            the sensor acceleration on each axis
     * @param valuesRef
     *            the ref of the axis we want to take into account
     * @return the total acceleration value
     */
    private float calculateAcceleration(float[] sensorEventValues, int[] valuesRef) {
        if (valuesRef.length == 1) {
            return sensorEventValues[valuesRef[0]];
        }
        float value = 0;
        for (int ref : valuesRef) {
            value += sensorEventValues[ref] * sensorEventValues[ref];
        }
        return (float) Math.sqrt(value);
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

    private static int[] xyzToSensorEventValuesRef(String string) {
        int[] sensorEventValuesRef = new int[string.length()];
        for (int i = 0; i < sensorEventValuesRef.length; i++) {
            switch (string.charAt(i)) {
            case 'x':
            case 'X':
                sensorEventValuesRef[i] = 0;
                break;
            case 'y':
            case 'Y':
                sensorEventValuesRef[i] = 1;
                break;
            case 'z':
            case 'Z':
                sensorEventValuesRef[i] = 2;
                break;
            default:
                throw new RuntimeException("Invalid axis definition: " + string);
            }
        }
        return sensorEventValuesRef;
    }
}
