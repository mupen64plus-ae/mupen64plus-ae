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
 * Authors: Paul Lamb, lioncash
 */
package paulscode.android.mupen64plusae.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class which collects a bunch of commonly used methods into one class.
 */
public final class Utility
{
    public static final float MINIMUM_TABLET_SIZE = 6.5f;
    
    /**
     * Clamps a value to the limit defined by min and max.
     * 
     * @param val The value to clamp to min and max.
     * @param min The lowest number val can be equal to.
     * @param max The largest number val can be equal to.
     * 
     * @return If the value is lower than min, min is returned. <br/>
     *         If the value is higher than max, max is returned.
     */
    public static<T extends Comparable<? super T>> T clamp( T val, T min, T max )
    {
        final T temp;

        //  val < max
        if ( val.compareTo(max) < 0 )
            temp = val;
        else
            temp = max;

        // temp > min
        if ( temp.compareTo(min) > 0 )
            return temp;
        else
            return min;
    }
    
    public static String getDateString()
    {
        return getDateString( new Date() );
    }

    public static String getDateString( Date date )
    {
        return new SimpleDateFormat( "yyyy-MM-dd_HH-mm-ss", Locale.US ).format( date );
    }
    
    public static String executeShellCommand(String... args)
    {
        try
        {
            Process process = Runtime.getRuntime().exec( args );
            BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
            StringBuilder result = new StringBuilder();
            String line;
            while( ( line = reader.readLine() ) != null )
            {
                result.append( line + "\n" );
            }
            return result.toString();
        }
        catch( IOException ignored )
        {
        }
        return "";
    }
}
