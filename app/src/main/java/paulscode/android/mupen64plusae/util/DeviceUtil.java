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
 * Authors: Paul Lamb, lioncash, littleguy77
 */
package paulscode.android.mupen64plusae.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.MotionEvent;
import android.view.View;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import paulscode.android.mupen64plusae.input.map.AxisMap;

import static android.content.Context.WIFI_SERVICE;

public final class DeviceUtil
{
    /**
     * Gets the hardware information from /proc/cpuinfo.
     * 
     * @return The hardware string.
     */
    public static String getCpuInfo()
    {
        // From http://android-er.blogspot.com/2009/09/read-android-cpu-info.html
        String result = Utility.executeShellCommand( "/system/bin/cat", "/proc/cpuinfo" );
        
        // Remove the serial number for privacy
        Pattern pattern = Pattern.compile( "^serial\\s*?:.*?$", Pattern.CASE_INSENSITIVE
                | Pattern.MULTILINE );
        result = pattern.matcher( result ).replaceAll( "Serial : XXXX" );
        
        // Additional information in android.os.Build may be useful
        result += "\n";
        result += "Board: " + Build.BOARD + "\n";
        result += "Brand: " + Build.BRAND + "\n";
        result += "Device: " + Build.DEVICE + "\n";
        result += "Display: " + Build.DISPLAY + "\n";
        result += "Host: " + Build.HOST + "\n";
        result += "ID: " + Build.ID + "\n";
        result += "Manufacturer: " + Build.MANUFACTURER + "\n";
        result += "Model: " + Build.MODEL + "\n";
        result += "Product: " + Build.PRODUCT + "\n";
        return result;
    }

    public static String getDeviceName(ContentResolver resolver)
    {
        String deviceName = Settings.System.getString(resolver, "device_name");

        if (deviceName == null) {
            deviceName = Settings.Secure.getString(resolver, "bluetooth_name");
        }

        if (deviceName == null) {
            deviceName = BluetoothAdapter.getDefaultAdapter().getName();
        }

        if (deviceName == null) {
            deviceName = android.os.Build.MODEL;
        }

        if (deviceName == null) {
            deviceName = "Unknown device";
        }

        return deviceName;
    }

    public static InetAddress wifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        InetAddress ipInetAddress;
        try {
            ipInetAddress = InetAddress.getByAddress(ipByteArray);
        } catch (UnknownHostException ex) {
            Log.e("DeviceInfo", "Unable to get host address.");
            ipInetAddress = null;
        }

        return ipInetAddress;
    }
    
    public static String getLogCat()
    {
        return Utility.executeShellCommand( "logcat", "-d", "-v", "long" );
    }
    
    public static void clearLogCat()
    {
        Utility.executeShellCommand( "logcat", "-c" );
    }
    
    public static String getAxisInfo()
    {
        StringBuilder builder = new StringBuilder();

        int[] ids = InputDevice.getDeviceIds();
        for (int id : ids) {
            InputDevice device = InputDevice.getDevice(id);
            AxisMap axisMap = AxisMap.getMap(device);
            if (!TextUtils.isEmpty(axisMap.getSignature())) {
                builder.append("Device: ").append(device.getName()).append("\n");
                builder.append("Type: ").append(axisMap.getSignatureName()).append("\n");
                builder.append("Signature: ").append(axisMap.getSignature()).append("\n");
                builder.append("Hash: ").append(axisMap.getSignature().hashCode()).append("\n");

                List<MotionRange> ranges = getPeripheralMotionRanges(device);
                for (MotionRange range : ranges) {
                    if (range.getSource() == InputDevice.SOURCE_JOYSTICK) {
                        int axisCode = range.getAxis();
                        String axisName = MotionEvent.axisToString(axisCode);
                        String className = getAxisClassName(axisMap.getClass(axisCode));
                        builder.append("  ").append(axisName).append(": ").append(className).append("\n");
                    }
                }
                builder.append("\n");
            }
        }

        return builder.toString();
    }
    
    /**
     * Gets the peripheral information using the appropriate Android API.
     * 
     * @return The peripheral info string.
     */
    public static String getPeripheralInfo()
    {
        StringBuilder builder = new StringBuilder();

        int[] ids = InputDevice.getDeviceIds();
        for (int id : ids) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null) {
                if (0 < (device.getSources() & (InputDevice.SOURCE_CLASS_BUTTON | InputDevice.SOURCE_CLASS_JOYSTICK))) {
                    builder.append("Device: ").append(device.getName()).append("\n");
                    builder.append("Id: ").append(device.getId()).append("\n");

                    builder.append("Descriptor: ").append(device.getDescriptor()).append("\n");
                    if (device.getVibrator().hasVibrator())
                        builder.append("Vibrator: true\n");

                    builder.append("Class: ").append(getSourceClassesString(device.getSources())).append("\n");

                    List<MotionRange> ranges = getPeripheralMotionRanges(device);
                    if (ranges.size() > 0) {
                        builder.append("Axes: ").append(ranges.size()).append("\n");
                        for (MotionRange range : ranges) {
                            String axisName = MotionEvent.axisToString(range.getAxis());
                            String source = getSourceName(range.getSource());
                            builder.append("  ").append(axisName).append(" (").append(source).append(")");
                            builder.append(": ( ").append(range.getMin()).append(" , ").append(range.getMax()).append(" )\n");
                        }
                    }
                    builder.append("\n");
                }
            }
        }

        return builder.toString();
    }
    
    /**
     * Gets the motion ranges of a peripheral using the appropriate Android API.
     * 
     * @return The motion ranges associated with the peripheral.
     */
    public static List<MotionRange> getPeripheralMotionRanges( InputDevice device )
    {
        List<MotionRange> ranges;
        ranges = device.getMotionRanges();
        
        return ranges;
    }
    
    /**
     * Gets the name of an axis class.
     * 
     * @param axisClass The axis class to get the name of.
     * 
     * @return The name of the axis class.
     */
    public static String getAxisClassName( int axisClass )
    {
        switch( axisClass )
        {
            case AxisMap.AXIS_CLASS_UNKNOWN:
                return "Unknown";
            case AxisMap.AXIS_CLASS_IGNORED:
                return "Ignored";
            case AxisMap.AXIS_CLASS_NORMAL:
                return "Normal";
            default:
                return "";
        }
    }
    
    /**
     * Gets the name of an action.
     * 
     * @param action        The action being performed.
     * @param isMotionEvent Whether or not the action is a motion event.
     * 
     * @return The name of the action being performed.
     */
    public static String getActionName( int action, boolean isMotionEvent )
    {
        switch( action )
        {
            case MotionEvent.ACTION_DOWN:
                return "DOWN";
            case MotionEvent.ACTION_UP:
                return "UP";
            case MotionEvent.ACTION_MOVE:
                return isMotionEvent ? "MOVE" : "MULTIPLE";
            case MotionEvent.ACTION_CANCEL:
                return "CANCEL";
            case MotionEvent.ACTION_OUTSIDE:
                return "OUTSIDE";
            case MotionEvent.ACTION_POINTER_DOWN:
                return "POINTER_DOWN";
            case MotionEvent.ACTION_POINTER_UP:
                return "POINTER_UP";
            case MotionEvent.ACTION_HOVER_MOVE:
                return "HOVER_MOVE";
            case MotionEvent.ACTION_SCROLL:
                return "SCROLL";
            case MotionEvent.ACTION_HOVER_ENTER:
                return "HOVER_ENTER";
            case MotionEvent.ACTION_HOVER_EXIT:
                return "HOVER_EXIT";
            default:
                return "ACTION_" + action;
        }
    }
    
    /**
     * Gets the name of the source performing an action.
     * 
     * @param source A number representing the source.
     * 
     * @return The name of the source.
     */
    public static String getSourceName( int source )
    {
        switch( source )
        {
            case InputDevice.SOURCE_CLASS_BUTTON:
                return "BUTTON";
            case InputDevice.SOURCE_CLASS_POINTER:
                return "POINTER";
            case InputDevice.SOURCE_CLASS_TRACKBALL:
                return "TRACKBALL";
            case InputDevice.SOURCE_CLASS_POSITION:
                return "POSITION";
            case InputDevice.SOURCE_CLASS_JOYSTICK:
                return "JOYSTICK";
            case InputDevice.SOURCE_DPAD:
                return "dpad";
            case InputDevice.SOURCE_GAMEPAD:
                return "gamepad";
            case InputDevice.SOURCE_JOYSTICK:
                return "joystick";
            case InputDevice.SOURCE_KEYBOARD:
                return "keyboard";
            case InputDevice.SOURCE_MOUSE:
                return "mouse";
            case InputDevice.SOURCE_STYLUS:
                return "stylus";
            case InputDevice.SOURCE_TOUCHSCREEN:
                return "touchscreen";
            case InputDevice.SOURCE_TRACKBALL:
                return "trackball";
            case InputDevice.SOURCE_UNKNOWN:
                return "unknown";
            default:
                return "source_" + source;
        }
    }
    
    @SuppressLint( "InlinedApi" )
    public static String getSourcesString( int sources )
    {
        List<String> names = new ArrayList<>();
        addString( sources, InputDevice.SOURCE_KEYBOARD, names );
        addString( sources, InputDevice.SOURCE_DPAD, names );
        addString( sources, InputDevice.SOURCE_GAMEPAD, names );
        addString( sources, InputDevice.SOURCE_TOUCHSCREEN, names );
        addString( sources, InputDevice.SOURCE_MOUSE, names );
        addString( sources, InputDevice.SOURCE_STYLUS, names );
        addString( sources, InputDevice.SOURCE_JOYSTICK, names );
        return TextUtils.join( ", ", names );
    }
    
    @SuppressLint( "InlinedApi" )
    public static String getSourceClassesString( int sources )
    {
        List<String> names = new ArrayList<>();
        addString( sources, InputDevice.SOURCE_CLASS_BUTTON, names );
        addString( sources, InputDevice.SOURCE_CLASS_POINTER, names );
        addString( sources, InputDevice.SOURCE_CLASS_TRACKBALL, names );
        addString( sources, InputDevice.SOURCE_CLASS_POSITION, names );
        addString( sources, InputDevice.SOURCE_CLASS_JOYSTICK, names );
        return TextUtils.join( ", ", names );
    }
    
    private static void addString( int sources, int sourceClass, List<String> strings )
    {
        if( ( sources & sourceClass ) > 0 )
            strings.add( getSourceName( sourceClass ) );
    }

    /**
     * Returns display metrics for the specified view.
     * 
     * @param view An instance of View (must be the child of an Activity).
     * 
     * @return DisplayMetrics instance, or null if there was a problem.
     */
    public static DisplayMetrics getDisplayMetrics( View view )
    {
        if( view == null )
            return null;

        Context context = view.getContext();
        return context.getResources().getDisplayMetrics();
    }
}
