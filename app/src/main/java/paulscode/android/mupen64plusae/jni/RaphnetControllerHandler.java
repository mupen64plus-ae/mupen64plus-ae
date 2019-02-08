/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2015 Paul Lamb
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
 * Authors: fzurita
 */

package paulscode.android.mupen64plusae.jni;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

class RaphnetControllerHandler
{
    static
    {
        System.loadLibrary( "mupen64plus-input-raphnet" );
    }

    interface DeviceReadyListener
    {
        /**
         * Called when the raphnet device is ready to be used
         */
        void onDeviceReady();
    }

    private static final String ACTION_USB_PERMISSION = "org.mupen64plusae.v3.alpha.USB_PERMISSION";
    private static final int RAPHNET_VENDOR_ID = 0x289b;
    private static final int RAPHNET_PRODUCT_ID = 100;
    private static final int RAPHNET_INTERFACE = 0;

    private Context mContext;
    private boolean mIsReady = false;
    private boolean mDevicesFound = false;
    private DeviceReadyListener mDeviceReadyListener;
    private UsbManager mUsbManager;
    private UsbDeviceConnection mDeviceConnection;
    private UsbInterface mUsbInterface;
    private boolean isConnected = false;

    /**
     * Initialize input-raphnet plugin.
     * @param fileDiscriptor USB file discriptor
     */
    static native void init(int fileDiscriptor, int vendorId, int productId);

    RaphnetControllerHandler(Context context, DeviceReadyListener deviceReadyListener) {
        mContext = context;
        mDeviceReadyListener = deviceReadyListener;
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            mIsReady = true;

            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            Log.d("RaphnetController", "permission granted for device " + device);

                            mUsbInterface = device.getInterface(RAPHNET_INTERFACE);
                            mDeviceConnection = mUsbManager.openDevice(device);
                            mDeviceConnection.claimInterface(mUsbInterface, true);
                            init(mDeviceConnection.getFileDescriptor(), device.getVendorId(), device.getProductId());
                            isConnected = true;
                        }
                    }
                    else {
                        Log.d("RaphnetController", "permission denied for device " + device);
                    }
                }

                mDeviceReadyListener.onDeviceReady();
            }

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && device.getVendorId() == RAPHNET_VENDOR_ID) {
                    Log.d("RaphnetController", "Device detached " + device);

                    if (isConnected) {
                        isConnected = false;
                        mDeviceConnection.releaseInterface(mUsbInterface);
                        mDeviceConnection.close();
                    }
                }
            }
        }
    };

    void requestDeviceAccess()
    {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        mContext.registerReceiver(mUsbReceiver, filter);

        synchronized (this) {
            if (mUsbManager != null) {
                HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

                for (UsbDevice device : deviceList.values()) {

                    if (device.getVendorId() == RAPHNET_VENDOR_ID) {
                        mUsbManager.requestPermission(device, permissionIntent);
                        mDevicesFound = true;
                    }
                }
            }
        }
    }

    boolean isReady()
    {
        return mIsReady || !mDevicesFound;
    }

    boolean isAvailable()
    {
        return mDevicesFound;
    }

    void shutdownAccess()
    {
        if (isConnected) {
            isConnected = false;
            mDeviceConnection.releaseInterface(mUsbInterface);
            mDeviceConnection.close();
        }

        mContext.unregisterReceiver(mUsbReceiver);
    }

    static boolean raphnetDevicesPresent(Context context)
    {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        boolean deviceFound = false;

        if (manager != null) {
            HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
            while(deviceIterator.hasNext() && !deviceFound){
                UsbDevice device = deviceIterator.next();
                deviceFound = device.getVendorId() == RAPHNET_VENDOR_ID;
            }
        }

        return deviceFound;
    }
}