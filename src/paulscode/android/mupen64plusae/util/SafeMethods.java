/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: (various)
 */
package paulscode.android.mupen64plusae.util;

import android.app.Activity;
import android.os.Handler;

/**
 * A boilerplate class for safely doing things, with simple exception handling.
 */
public class SafeMethods
{
    /**
     * Safely converts a string into an integer.
     * 
     * @param val String containing the number to convert.
     * @param fail Value to use if unable to convert val to an integer.
     * @return The converted integer, or the specified value if unsuccessful.
     */
    public static int toInt( String val, int fail )
    {
        if( val == null || val.length() < 1 )
            return fail; // Not a number
        try
        {
            return Integer.valueOf( val ); // Convert to integer
        }
        catch( NumberFormatException nfe )
        {
        }
        
        return fail; // Conversion failed
    }

    /**
     * Safely converts a string into a float.
     * 
     * @param val String containing the number to convert.
     * @param fail Value to use if unable to convert val to an float.
     * @return The converted float, or the specified value if unsuccessful.
     */
    public static float toFloat( String val, float fail )
    {
        if( val == null || val.length() < 1 )
            return fail; // Not a number
        try
        {
            return Float.valueOf( val ); // Convert to float
        }
        catch( NumberFormatException nfe )
        {
        }
        
        return fail; // Conversion failed
    }
    
    /**
     * Safely sleep.
     * 
     * @param milliseconds The sleep duration.
     */
    public static void sleep( int milliseconds )
    {
        try
        {
            Thread.sleep( milliseconds );
        }
        catch( InterruptedException e )
        {
        }
    }

    /**
     * Safely (?) exit the app.
     * 
     * @param message The message to be toasted to the user.
     * @param activity The activity to run the toast from.
     * @param milliseconds Time delay before exiting (so toast can be seen).
     */
    public static void exit( String message, Activity activity, int milliseconds )
    {
        Notifier.showToast( message, activity );
        final Handler handler = new Handler();
        handler.postDelayed( new Runnable()
        {
            public void run()
            {
                System.exit( 0 );
            }
        }, milliseconds );
    }
    
}
