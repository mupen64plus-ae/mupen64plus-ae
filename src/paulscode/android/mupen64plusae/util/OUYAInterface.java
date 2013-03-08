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
            
            return OuyaFacadeClass.getMethod( "isRunningOnOUYAHardware", OuyaFacadeClass )
                    .invoke( ouyaFacadeObj, new Object[0] ).toString().equals(  "true" );
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
     * Checks if the specified input device is an OUYA controller
     *
     * @return true if the device is an OUYA controller
     */
    public static boolean isOUYAController( int deviceId )
    {
        // If OuyaController.getPlayerNumByDeviceId returns a value other than -1, then it is an OUYA controller
        try
        {
            Class<?> OuyaControllerClass = Class.forName( "tv.ouya.console.api.OuyaController" );
            
            return !OuyaControllerClass.getMethod( "getPlayerNumByDeviceId", new Class[]{ int.class } )
                    .invoke( null, new Object[]{ deviceId } ).toString().equals(  "-1" );
        }
        // If it fails, assume it is not an OUYA controller
        catch( ClassNotFoundException cnfe )
        {}
        catch( NoSuchMethodException nsme )
        {}
        catch( IllegalAccessException iae )
        {}
        catch( InvocationTargetException ite )
        {}
        // NPE will be thrown if running on OUYA and initOUYAController failed
        catch( NullPointerException npe)
        {}
        return false;
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
}