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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Locale;

import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import android.content.Context;
import android.util.Log;

public class CrashHandler implements UncaughtExceptionHandler
{
    public static void init( Context context )
    {
        DeviceUtil.clearLogCat();
        Thread.setDefaultUncaughtExceptionHandler( new CrashHandler( context ) );
    }
    
    private final Context mContext;
    private final UncaughtExceptionHandler mDefaultHandler;
    
    private CrashHandler( Context context )
    {
        mContext = context;
        
        // Remember the original handler so that we can let it handle the exception when we're done
        UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        if( handler instanceof CrashHandler )
            mDefaultHandler = ( (CrashHandler) handler ).mDefaultHandler;
        else
            mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }
    
    @Override
    public void uncaughtException( Thread thread, Throwable ex )
    {
        try
        {
            // Setup crash log
            GlobalPrefs globalPrefs = new GlobalPrefs( mContext );
            String filename = String.format( Locale.US, "%s/Crash_%s_%03d.txt",
                    globalPrefs.crashLogDir, Utility.getDateString(), System.currentTimeMillis() % 1000 );
            File log = new File( filename );
            log.getParentFile().mkdirs();
            
            // Write to crash log
            try
            {
                final PrintWriter out = new PrintWriter( log );
                out.println( "Uncaught exception in package " + mContext.getPackageName() );
                out.println( "\n****MESSAGE****" );
                out.println( ex.getMessage() );
                out.println( "\n****STACK TRACE****" );
                ex.printStackTrace( out );
                out.println( "\n****LOGCAT****" );
                out.println( DeviceUtil.getLogCat() );
                out.println( "\n****DEVICE****" );
                out.println( DeviceUtil.getCpuInfo() );
                out.println( "\n****PERIPHERALS****" );
                out.println( DeviceUtil.getAxisInfo() );
                out.println( DeviceUtil.getPeripheralInfo() );
                out.close();
            }
            catch( FileNotFoundException fnfe )
            {
                Log.e( "CrashHandler",
                        "Crash log could not be opened for writing: " + fnfe.getMessage() );
            }
        }
        catch( Throwable ignored )
        {
            // Quietly discard our own exceptions to avoid infinite recursion
        }
        
        // Pass the information to the default handler
        if( mDefaultHandler != null )
            mDefaultHandler.uncaughtException( thread, ex );
    }
}