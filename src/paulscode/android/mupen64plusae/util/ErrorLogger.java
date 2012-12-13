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
 * Authors: paulscode, littleguy77
 */
package paulscode.android.mupen64plusae.util;

import paulscode.android.mupen64plusae.persistent.ConfigFile;

/**
 * A small class to encapsulate the error logging process for Mupen64PlusAE.
 */
public class ErrorLogger
{
    private static ConfigFile mLogfile;
    private static String mMessage = null;
    
    public static void initialize( String filename )
    {
        mLogfile = new ConfigFile( filename );
    }
    
    public static boolean hasError()
    {
        return mMessage != null;
    }
    
    public static String getLastError()
    {
        return mMessage;
    }
    
    public static void setLastError( String message )
    {
        mMessage = message;
    }
    
    public static void clearLastError()
    {
        mMessage = null;
    }
    
    public static void putLastError( String section, String parameter )
    {
        put( section, parameter, mMessage );
    }
    
    public static void put( String section, String parameter, String value )
    {
        if( mLogfile != null )
        {  // TODO: sometimes getting null pointer exception for mLogfile.. figure out why it is null.
            mLogfile.put( section, parameter, value );
            mLogfile.save();
        }
    }
}
