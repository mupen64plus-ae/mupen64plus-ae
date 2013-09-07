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

import paulscode.android.mupen64plusae.persistent.ConfigFile;

/**
 * A small class to encapsulate the error logging process for Mupen64PlusAE.
 */
public final class ErrorLogger
{
    private static ConfigFile mLogfile;
    private static String mMessage = null;
    
    /**
     * Initializes this ErrorLogger.
     * 
     * @param filename The config file to log to.
     */
    public static void initialize( String filename )
    {
        mLogfile = new ConfigFile( filename );
    }
    
    /**
     * Checks if the logger has any errors.
     * 
     * @return True if the logger has any errors. False otherwise.
     */
    public static boolean hasError()
    {
        return (mMessage != null);
    }
    
    /**
     * Gets the last error
     * 
     * @return The last error.
     */
    public static String getLastError()
    {
        return mMessage;
    }
    
    /**
     * Sets a given message as the last error.
     * 
     * @param message The message to set as the last error.
     */
    public static void setLastError( String message )
    {
        mMessage = message;
    }
    
    /**
     * Clears the last error.
     */
    public static void clearLastError()
    {
        mMessage = null;
    }
    
    /**
     * Writes the last error to the config file.
     * 
     * @param section   The title of the section to contain the last error.
     * @param parameter The name of the parameter to be assigned the last error.
     */
    public static void putLastError( String section, String parameter )
    {
        put( section, parameter, mMessage );
    }
    
    /**
     * Writes a given value to a given parameter which is in the given section.
     * 
     * @param section   The title of the section to contain the parameter.
     * @param parameter The name of the parameter that will be assigned the value.
     * @param value     The string to assign to the parameter.
     */
    public static void put( String section, String parameter, String value )
    {
        if( mLogfile != null )
        {  // TODO: sometimes getting null pointer exception for mLogfile.. figure out why it is null.
            mLogfile.put( section, parameter, value );
            mLogfile.save();
        }
    }
}
