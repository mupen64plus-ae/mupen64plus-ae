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
 * Authors: Paul Lamb
 */

package paulscode.android.mupen64plusae.util;

import paulscode.android.mupen64plusae.persistent.AppData;
import android.content.Context;
import java.lang.reflect.InvocationTargetException;

/**
 * The OUYAInterface class consolidates all interactions with the OUYA SDK (a.k.a. ODK)
 * <p>
 * <b>Developers:</b> In order to dispel all possible concerns about GPL compliance and to satisfy f-droid's
 * specific requirements, all method calls into the ODK should be made indirectly, using Class.forName,
 * Class.getMethod, and Method.invoke.  This will allow the app to be built unmodified without linking to
 * ODK jars.  Any exceptions caught should be handled gracefully in order not to break the app on non-OUYA
 * devices running a build without the ODK jars.
 */
public class OUYAInterface
{
    
    /** True if device is an OUYA */
    public static final boolean IS_OUYA_HARDWARE = isRunningOnOUYAHardware();
    
    /** Code for the left trigger axis */
    public static final int AXIS_L2 = getOuyaControllerStaticIntField( "AXIS_L2" );
    
    /** Code for the left joystick x axis */
    public static final int AXIS_LS_X = getOuyaControllerStaticIntField( "AXIS_LS_X" );    
    
    /** Code for the left joystick y axis */
    public static final int AXIS_LS_Y = getOuyaControllerStaticIntField( "AXIS_LS_Y" );    
    
    /** Code for the right trigger axis */
    public static final int AXIS_R2 = getOuyaControllerStaticIntField( "AXIS_R2" );    
    
    /** Code for the right joystick x axis */
    public static final int AXIS_RS_X = getOuyaControllerStaticIntField( "AXIS_RS_X" );    
    
    /** Code for the right joystick y axis */
    public static final int AXIS_RS_Y = getOuyaControllerStaticIntField( "AXIS_RS_Y" );    
    
    /** Code for the A button */
    public static final int BUTTON_A = getOuyaControllerStaticIntField( "BUTTON_A" );    
    
    /** Code for the D-pad down button */
    public static final int BUTTON_DPAD_DOWN = getOuyaControllerStaticIntField( "BUTTON_DPAD_DOWN" );    
    
    /** Code for the D-pad left button */
    public static final int BUTTON_DPAD_LEFT = getOuyaControllerStaticIntField( "BUTTON_DPAD_LEFT" );    
    
    /** Code for the D-pad right button */
    public static final int BUTTON_DPAD_RIGHT = getOuyaControllerStaticIntField( "BUTTON_DPAD_RIGHT" );    
    
    /** Code for the D-pad up button */
    public static final int BUTTON_DPAD_UP = getOuyaControllerStaticIntField( "BUTTON_DPAD_UP" );    
    
    /** Code for the left bumper button */
    public static final int BUTTON_L1 = getOuyaControllerStaticIntField( "BUTTON_L1" );    
    
    /** Code for the left trigger button */
    public static final int BUTTON_L2 = getOuyaControllerStaticIntField( "BUTTON_L2" );    
    
    /** Code for left joystick button */
    public static final int BUTTON_L3 = getOuyaControllerStaticIntField( "BUTTON_L3" );    
    
    /** Code for a short press of the system button */
    public static final int BUTTON_MENU = getOuyaControllerStaticIntField( "BUTTON_MENU" );    
    
    /** Code for the O button */
    public static final int BUTTON_O = getOuyaControllerStaticIntField( "BUTTON_O" );    
    
    /** Code for the right bumper button */
    public static final int BUTTON_R1 = getOuyaControllerStaticIntField( "BUTTON_R1" );    
    
    /**  Code for the right trigger button */
    public static final int BUTTON_R2 = getOuyaControllerStaticIntField( "BUTTON_R2" );    
    
    /**  Code for right joystick button */
    public static final int BUTTON_R3 = getOuyaControllerStaticIntField( "BUTTON_R3" );    
    
    /**  Code for the U button */
    public static final int BUTTON_U = getOuyaControllerStaticIntField( "BUTTON_U" );    
    
    /**  Code for the Y button */
    public static final int BUTTON_Y = getOuyaControllerStaticIntField( "BUTTON_Y" );
    
    /**  The maximum number of connected controllers */
    public static final int MAX_CONTROLLERS = getOuyaControllerStaticIntField( "MAX_CONTROLLERS" );
    
    /**  The deadzone amount to use for the analog sticks */
    public static final float STICK_DEADZONE = getOuyaControllerStaticFloatField( "STICK_DEADZONE" );    
    
    /**
     * Checks if the app is running on OUYA hardware.
     *
     * @return true if the app is running on OUYA hardware
     */
    public static boolean isRunningOnOUYAHardware()
    {
        // The OUYA is built on ICS, so quick check for that:
        if( !AppData.IS_ICE_CREAM_SANDWICH )
            return false;
        // Retrieve the result from OuyaFacade.isRunningOnOUYAHardware
        try
        {
            Class<?> OuyaFacadeClass = Class.forName( "tv.ouya.console.api.OuyaFacade" );
            
            Object ouyaFacadeObj = ( OuyaFacadeClass.getMethod(  "getInstance", new Class[0] )
                    .invoke( null, new Object[0] ) );
            
            return OuyaFacadeClass.getMethod( "isRunningOnOUYAHardware", new Class[0] )
                    .invoke( ouyaFacadeObj, new Object[0] ).toString().equals( "true" );
        }
        // If it fails, assume this is not an OUYA
        catch( ClassNotFoundException cnfe )
        {}
        catch( NoSuchMethodException nsme )
        {}
        catch( IllegalAccessException iae )
        {}
        catch( InvocationTargetException ite )
        {}
        return false;
    }
    
    /**
     * Returns the OUYA player number for the specified controller
     *
     * @return zero-based player number, or -1 if device is not a controller assigned to a player
     */
    public static int getPlayerNumByDeviceId( int deviceId )
    {
        try
        {
            Class<?> OuyaControllerClass = Class.forName( "tv.ouya.console.api.OuyaController" );
            
            return Integer.parseInt( OuyaControllerClass.getMethod( "getPlayerNumByDeviceId", new Class[]{ int.class } )
                    .invoke( null, new Object[]{ deviceId } ).toString() );
        }
        // If it fails, assume it is not assigned to a player
        catch( ClassNotFoundException cnfe )
        {}
        catch( NoSuchMethodException nsme )
        {}
        catch( IllegalAccessException iae )
        {}
        catch( InvocationTargetException ite )
        {}
        catch( NumberFormatException nfe )
        {}
        // NPE will be thrown if running on OUYA and initOUYAController failed
        catch( NullPointerException npe)
        {}
        return -1;
    }
    
    /**
     * Initializes the OuyaController interface
     *
     */
    public static void initOUYAController( Context context )
    {
         // NOTE: All OUYAController interface methods should handle NullPointerException, which will
         // be thrown if this method fails when the app is running on OUYA
        try
        {
            Class<?> OuyaControllerClass = Class.forName( "tv.ouya.console.api.OuyaController" );
            
            OuyaControllerClass.getMethod( "init", new Class[]{ Context.class } )
                    .invoke( null, new Object[]{ context } );
        }
        // Nothing further needed if it fails (controller can not be initialized)
        catch( ClassNotFoundException cnfe )
        {}
        catch( NoSuchMethodException nsme )
        {}
        catch( IllegalAccessException iae )
        {}
        catch( InvocationTargetException ite )
        {}
    }
    
    /**
     * Checks whether or not the value of a static field in the OuyaController
     * class is accessible.
     *
     * @return True if the field is accessible
     */
    public static boolean checkOuyaControllerStaticFieldAccessible( String fieldName )
    {
        try
        {
            Class.forName( "tv.ouya.console.api.OuyaController" ).getField( fieldName ).get( null );
            return true;
        }        
        // If it fails, return -1 (the ODK jar probably isn't linked)
        catch( ClassNotFoundException cnfe )
        {}
        catch( NoSuchFieldException nsme )
        {}
        catch( IllegalAccessException iae )
        {}
        return false;
    }    
    
    /**
     * Returns the value of a static int field of the OuyaController class.
     * 
     * NOTE: If the value -1 is significant, use this method in conjunction with
     * checkOuyaControllerStaticFieldAccessible
     *
     * @return The field's value, or -1 if unable to access it
     */
    public static int getOuyaControllerStaticIntField( String fieldName )
    {
        try
        {
            return Class.forName( "tv.ouya.console.api.OuyaController" ).getField( fieldName ).getInt( null );
        }        
        // If it fails, return -1 (the ODK jar probably isn't linked)
        catch( ClassNotFoundException cnfe )
        {}
        catch( NoSuchFieldException nsme )
        {}
        catch( IllegalAccessException iae )
        {}
        return -1;
    }
    
    /**
     * Returns the value of a static float field of the OuyaController class.
     * 
     * NOTE: If the value -1 is significant, use this method in conjunction with
     * checkOuyaControllerStaticFieldAccessible
     *
     * @return The field's value, or -1 if unable to access it
     */
    public static float getOuyaControllerStaticFloatField( String fieldName )
    {
        try
        {
            return Class.forName( "tv.ouya.console.api.OuyaController" ).getField( fieldName ).getFloat( null );
        }        
        // If it fails, return -1 (the ODK jar probably isn't linked)
        catch( ClassNotFoundException cnfe )
        {}
        catch( NoSuchFieldException nsme )
        {}
        catch( IllegalAccessException iae )
        {}
        return -1;
    }
}
