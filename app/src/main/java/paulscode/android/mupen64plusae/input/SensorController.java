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

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import paulscode.android.mupen64plusae.input.TouchController.OnStateChangedListener;
import paulscode.android.mupen64plusae.jni.CoreFragment;

/**
 * Emulates a joystick using accelerometer sensor
 */
public class SensorController extends AbstractController implements SensorEventListener {
    private final SensorManager mSensorManager;

    /** The state change listener. */
    private final OnStateChangedListener mListener;

    // Index values to be used by calculateAcceleration
    private final int[] sensorEventValuesRefX, sensorEventAdjacentValuesRefX, sensorEventValuesRefY,
            sensorEventAdjacentValuesRefY;

    private final float angleX, angleY;
    private final float sensitivityX, sensitivityY;
    private boolean isPaused = true;
    private boolean mSensorEnabled = false;

    /**
     * Constructor
     *
     * @param coreFragment Core fragment
     * @param sensorManager Sensor manager
     * @param listener State change listener
     * @param sensorAxisX Sensor X axis
     * @param sensorSensitivityX Sensor X sensitivity
     * @param sensorAngleX Sensor X angle
     * @param sensorAxisY Sensor Y axis
     * @param sensorSensitivityY  Sensor Y sensitivity
     * @param sensorAngleY Sensor Y angle
     */
    public SensorController(CoreFragment coreFragment, SensorManager sensorManager, OnStateChangedListener listener,
                            String sensorAxisX, int sensorSensitivityX, float sensorAngleX, String sensorAxisY,
                            int sensorSensitivityY, float sensorAngleY) {
        super(coreFragment);

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
        angleX = sensorAngleX;
        angleY = sensorAngleY;
        this.sensitivityX = sensorSensitivityX / 100f;
        this.sensitivityY = sensorSensitivityY / 100f;
    }

    public boolean isSensorEnabled() {
        return mSensorEnabled;
    }

    public void setSensorEnabled(boolean sensorEnabled) {
        mSensorEnabled = sensorEnabled;
        updateListener();
    }

    /**
     * Should be called on {@link Activity#onResume()}
     */
    public void onResume() {
        isPaused = false;
        updateListener();
    }

    /**
     * Should be called on {@link Activity#onPause()}
     */
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
        float rawX = getStrength(event.values, sensorEventValuesRefX, sensorEventAdjacentValuesRefX, angleX);
        rawX *= sensitivityX;
        float rawY = getStrength(event.values, sensorEventValuesRefY, sensorEventAdjacentValuesRefY, angleY);
        rawY *= sensitivityY;

        float magnitude = (float) Math.sqrt((rawX * rawX) + (rawY * rawY));
        float factor = magnitude > 1 ? magnitude : 1;
        mState.axisFractionX = rawX / factor;
        mState.axisFractionY = rawY / factor;
        notifyChanged();
        mListener.onAnalogChanged(mState.axisFractionX, mState.axisFractionY);
    }

    /**
     * Converts sensor event to strength, using the {@link SensorEvent#values}
     * defined in the valuesRef and adjacentValuesRef arguments
     * 
     * @see #calculateAcceleration(float[], int[])
     * 
     * @param sensorEventValues
     *            the {@link SensorEvent#values}
     * @param valuesRef
     *            the indexes of the {@link SensorEvent#values} to be used to
     *            calculate the strength
     * @param adjacentValuesRef
     *            the indexes of the {@link SensorEvent#values} to be used as
     *            adjacent value
     * @param idleAngleDegree
     *            the angle that defines joystick IDLE state
     * @return the strength
     */
    private float getStrength(float[] sensorEventValues, int[] valuesRef, int[] adjacentValuesRef,
            float idleAngleDegree) {
        if (valuesRef.length == 0 || adjacentValuesRef.length == 0) {
            return 0; // This axis is disabled
        }
        float value = calculateAcceleration(sensorEventValues, valuesRef);
        float adjacentValue = calculateAcceleration(sensorEventValues, adjacentValuesRef);
        float angle = (float) calculateAngle(value, adjacentValue) - idleAngleDegree / 180 * (float) Math.PI;
        // Fixing angle to have range in [-Pi,Pi]
        while (Math.abs(angle) > Math.PI) {
            angle -= Math.signum(angle) * 2 * Math.PI;
        }
        // If abs(angle)>Pi/2 (90°), decreasing it (135°=> 45°, 180° => 0°)
        if (angle > Math.PI / 2) {
            angle = (float) Math.PI - angle;
        } else if (angle < -Math.PI / 2) {
            angle = -(float) Math.PI - angle;
        }
        // angle's range is now [-Pi/2,Pi/2] (max 90°)
        return angleToStrength(angle);
    }

    /**
     * Calculates the total acceleration of some axis
     * 
     * @param sensorEventValues
     *            the {@link SensorEvent#values}
     * @param valuesRef
     *            the indexes of the {@link SensorEvent#values} we want to take
     *            into account
     * @return the total acceleration value of the axes defined in valuesRef
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
     * Calculates arc tangent of the ratio of the 2 args, avoiding to divide by
     * 0, and returning a result that is not clamped to [-Pi/2,Pi/2] (in case 1
     * or 2 parameters are negative). This method can return any angle's value
     * 
     * @return an angle, which should be fixed if its absolute value > Pi/2
     */
    private static float calculateAngle(float value, float adjacentValue) {
        if (Math.abs(value) <= Math.abs(adjacentValue)) {
            if (adjacentValue > 0) {
                // Classic arc tangent
                return (float) Math.atan(value / adjacentValue);
            } else {
                return (float) Math.PI + (float) Math.atan(value / adjacentValue);
            }
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

    /**
     * Converts an XYZ String to an array of index of {@link SensorEvent#values}
     * ready to be used by {@link #calculateAcceleration(float[], int[])}
     */
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
                // Should not happen because the SensorConfigurationDialog
                // filters the String value
                throw new RuntimeException("Invalid axis definition: " + string);
            }
        }
        return sensorEventValuesRef;
    }
}
