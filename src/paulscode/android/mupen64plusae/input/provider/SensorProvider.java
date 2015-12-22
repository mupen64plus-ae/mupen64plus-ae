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
 * Authors: TODO: Implement sensor provider, if desired.
 */
package paulscode.android.mupen64plusae.input.provider;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.bda.controller.MotionEvent;

public class SensorProvider extends AbstractProvider implements SensorEventListener {
	private final int[] mInputCodes;

	public SensorProvider(SensorManager sensorManager) {
		mInputCodes = new int[10];
		// @formatter:off
		mInputCodes[0] = axisToInputCode(MotionEvent.AXIS_X, true);
		mInputCodes[1] = axisToInputCode(MotionEvent.AXIS_X, false);
		mInputCodes[2] = axisToInputCode(MotionEvent.AXIS_Y, true);
		mInputCodes[3] = axisToInputCode(MotionEvent.AXIS_Y, false);
		// mInputCodes[4] = axisToInputCode( MotionEvent.AXIS_Z, true );
		// mInputCodes[5] = axisToInputCode( MotionEvent.AXIS_Z, false );
		// mInputCodes[6] = axisToInputCode( MotionEvent.AXIS_RZ, true );
		// mInputCodes[7] = axisToInputCode( MotionEvent.AXIS_RZ, false );
		// mInputCodes[8] = axisToInputCode( MotionEvent.AXIS_LTRIGGER, true );
		// mInputCodes[9] = axisToInputCode( MotionEvent.AXIS_RTRIGGER, true );
		// @formatter:on
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_GAME);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float[] strengths = new float[mInputCodes.length];
		// strengths doit être entre 0 et 1

		float xStrength = event.values[1];
		boolean isPositive = xStrength > 0;
		xStrength = Math.min(xStrength / 2.5f, 1);
		if (isPositive) {
			strengths[0] = xStrength;
			strengths[1] = 0;
		} else {
			strengths[0] = 0;
			strengths[1] = -xStrength;
		}

		float yStrength = event.values[2];
		yStrength = yStrength / (SensorManager.GRAVITY_EARTH - event.values[1]);
		if (yStrength < -1) {
			yStrength = -1;
		} else if (yStrength > 1) {
			yStrength = 1;
		}
		yStrength = (float) Math.asin(yStrength);
		yStrength = yStrength / (float) Math.PI * 20f - 2.5f;
		if (yStrength > 0) {
			strengths[2] = 0;
			strengths[3] = Math.min(yStrength, 1);
		} else {
			strengths[2] = Math.min(-yStrength, 1);
			strengths[3] = 0;
		}

		notifyListeners(mInputCodes, strengths, 0);
	}
}
