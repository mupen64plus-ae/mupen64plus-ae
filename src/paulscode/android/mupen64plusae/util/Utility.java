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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.View;

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
    
    public static Point constrainToOctagon( int dX, int dY, int halfWidth )
    {
        final float dC = halfWidth;
        final float dA = dC * FloatMath.sqrt( 0.5f );
        final float signX = (dX < 0) ? -1 : 1;
        final float signY = (dY < 0) ? -1 : 1;
        
        Point crossPt = new Point();
        crossPt.x = dX;
        crossPt.y = dY;
        
        if( ( signX * dX ) > ( signY * dY ) )
            segsCross( 0, 0, dX, dY, signX * dC, 0, signX * dA, signY * dA, crossPt );
        else
            segsCross( 0, 0, dX, dY, 0, signY * dC, signX * dA, signY * dA, crossPt );
        
        return crossPt;
    }
    
    /**
     * Determines if the two specified line segments intersect with each other, and calculates where
     * the intersection occurs if they do.
     * 
     * @param seg1pt1_x X-coordinate for the first end of the first line segment.
     * @param seg1pt1_y Y-coordinate for the first end of the first line segment.
     * @param seg1pt2_x X-coordinate for the second end of the first line segment.
     * @param seg1pt2_y Y-coordinate for the second end of the first line segment.
     * @param seg2pt1_x X-coordinate for the first end of the second line segment.
     * @param seg2pt1_y Y-coordinate for the first end of the second line segment.
     * @param seg2pt2_x X-coordinate for the second end of the second line segment.
     * @param seg2pt2_y Y-coordinate for the second end of the second line segment.
     * @param crossPt Changed to the point of intersection if there is one, otherwise unchanged.
     * 
     * @return True if the two line segments intersect.
     */
    private static boolean segsCross( float seg1pt1_x, float seg1pt1_y, float seg1pt2_x,
            float seg1pt2_y, float seg2pt1_x, float seg2pt1_y, float seg2pt2_x, float seg2pt2_y,
            Point crossPt )
    {
        float vec1_x = seg1pt2_x - seg1pt1_x;
        float vec1_y = seg1pt2_y - seg1pt1_y;
        
        float vec2_x = seg2pt2_x - seg2pt1_x;
        float vec2_y = seg2pt2_y - seg2pt1_y;
        
        float div = ( -vec2_x * vec1_y + vec1_x * vec2_y );
        
        // Segments don't cross
        if( div == 0 )
            return false;
        
        float s = ( -vec1_y * ( seg1pt1_x - seg2pt1_x ) + vec1_x * ( seg1pt1_y - seg2pt1_y ) ) / div;
        float t = ( vec2_x  * ( seg1pt1_y - seg2pt1_y ) - vec2_y * ( seg1pt1_x - seg2pt1_x ) ) / div;
        
        if( s >= 0 && s < 1 && t >= 0 && t <= 1 )
        {
            // Segments cross, point of intersection stored in 'crossPt'
            crossPt.x = (int) ( seg1pt1_x + ( t * vec1_x ) );
            crossPt.y = (int) ( seg1pt1_y + ( t * vec1_y ) );
            return true;
        }
        
        // Segments don't cross
        return false;
    }
    
    /**
     * Launches a URI from a resource in a given context.
     * 
     * @param context The context to launch a URI from.
     * @param resId   The ID of the resource to create the URI from.
     */
    public static void launchUri( Context context, int resId )
    {
        String uri = context.getString( resId );
        Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse( uri ) );
        context.startActivity( intent );
    }
    
    /**
     * Reads the header name of a given ROM.
     * 
     * @param filename The path to the ROM (or ZIP file the ROM is contained in).
     * @param tempDir  The desired temporary directory to perform the reading in.
     * 
     * @return The header name of the given ROM, or null if an error occurs.
     */
    public static String getHeaderName( String filename, String tempDir )
    {
        ErrorLogger.put( "READ_HEADER", "fail", "" );
        if( filename == null || filename.length() < 1 )
        {
            ErrorLogger.put( "READ_HEADER", "fail", "filename not specified" );
            Log.e( "Utility", "filename not specified in method 'getHeaderName'" );
            return null;
        }
        else if( filename.toLowerCase( Locale.US ).endsWith( ".zip" ) )
        {
            // Create the tmp folder if it doesn't exist:
            File tmpFolder = new File( tempDir );
            tmpFolder.mkdir();
            
            // Clear the folder if anything is in there:
            String[] children = tmpFolder.list();
            if( children != null )
            {
                for( String child : children )
                {
                    FileUtil.deleteFolder( new File( tmpFolder, child ) );
                }
            }
            
            ErrorLogger.clearLastError();
            String uzFile = unzipFirstROM( new File( filename ), tempDir );
            if( uzFile == null || uzFile.length() < 1 )
            {
                Log.e( "Utility", "Unable to unzip ROM: '" + filename + "'" );
                if( ErrorLogger.hasError() )
                {
                    ErrorLogger.putLastError( "READ_HEADER", "fail" );
                    ErrorLogger.clearLastError();
                }
                else
                {
                    ErrorLogger.put( "READ_HEADER", "fail", "Unable to unzip ROM: '" + filename
                            + "'" );
                }
                return null;
            }
            else
            {
                String headerName = new RomHeader( new File( uzFile ) ).name;
                try
                {
                    new File( uzFile ).delete();
                }
                catch( NullPointerException ignored )
                {
                }
                return headerName;
            }
        }
        else
        {
            return new RomHeader( new File( filename ) ).name;
        }
    }

    /**
     * Gets the two CRC values from the N64 ROM's header.
     *
     * @param filename The filename of the rom
     * @param tempDir Temporary directory the ROM will be extracted in so that the header can be checked.
     *
     * @return The CRC values of the ROM as a string. <p>
     *         If the extracted ROM is null, then null will be returned.
     */
    public static String getHeaderCRC( String filename, String tempDir )
    {
        ErrorLogger.put( "READ_HEADER", "fail", "" );
        if( TextUtils.isEmpty( filename ) )
        {
            ErrorLogger.put( "READ_HEADER", "fail", "filename not specified" );
            Log.e( "Utility", "filename not specified in method 'getHeaderCRC'" );
            return null;
        }
        else if( filename.toLowerCase( Locale.US ).endsWith( ".zip" ) )
        {
            // Create the tmp folder if it doesn't exist:
            File tmpFolder = new File( tempDir );
            while( tmpFolder.exists() && !tmpFolder.isDirectory() )
            {
                // A file with this name already exists, choose another name
                tempDir += "_";
                tmpFolder = new File( tempDir );
            }
            tmpFolder.mkdir();
            
            // Clear the folder if anything is in there:
            String[] children = tmpFolder.list();
            if( children != null)
            {
                for( String child : children )
                {
                    FileUtil.deleteFolder( new File( tmpFolder, child ) );
                }
            }
            
            ErrorLogger.clearLastError();
            String uzFile = unzipFirstROM( new File( filename ), tempDir );
            if( TextUtils.isEmpty( uzFile ) )
            {
                Log.e( "Utility", "Unable to unzip ROM: '" + filename + "'" );
                if( ErrorLogger.hasError() )
                {
                    ErrorLogger.putLastError( "READ_HEADER", "fail" );
                    ErrorLogger.clearLastError();
                }
                else
                {
                    ErrorLogger.put( "READ_HEADER", "fail", "Unable to unzip ROM: '" + filename
                            + "'" );
                }

                return null;
            }
            else
            {
                File file = new File( uzFile );
                String headerCRC = new RomHeader( file ).crc;
                file.delete();
                return headerCRC;
            }
        }
        else
        {
            return new RomHeader( new File( filename ) ).crc;
        }
    }
    
    public static String getTexturePackName( String filename )
    {
        int x;
        String textureName, textureExt;
        String supportedExt = ".png";
        File archive = new File( filename );
        
        if( !archive.exists() )
            ErrorLogger
                    .setLastError( "Zip file '" + archive.getAbsolutePath() + "' does not exist" );
        else if( !archive.isFile() )
            ErrorLogger.setLastError( "Zip file '" + archive.getAbsolutePath()
                    + "' is not a file (method unzipFirstROM)" );
        
        if( ErrorLogger.hasError() )
        {
            Log.e( "Utility", ErrorLogger.getLastError() );
            return null;
        }
        ZipFile zipfile = null;
        try
        {
            zipfile = new ZipFile( archive );
            Enumeration<? extends ZipEntry> e = zipfile.entries();
            while( e.hasMoreElements() )
            {
                ZipEntry entry = e.nextElement();
                if( entry != null && !entry.isDirectory() )
                {
                    textureName = entry.getName();
                    if( textureName != null && textureName.length() > 3 )
                    {
                        textureExt = textureName.substring( textureName.length() - 4,
                                textureName.length() ).toLowerCase( Locale.US );
                        if( supportedExt.contains( textureExt ) )
                        {
                            x = textureName.indexOf( '#' );
                            if( x > 0 && x < textureName.length() )
                            {
                                textureName = textureName.substring( 0, x );
                                if( textureName.length() > 0 )
                                {
                                    x = textureName.lastIndexOf( '/' );
                                    if( x >= 0 && x < textureName.length() )
                                        return textureName.substring( x + 1, textureName.length() );
                                }
                            }
                        }
                    }
                }
            }
        }
        catch( ZipException ze )
        {
            ErrorLogger
                    .setLastError( "Zip Error!  Ensure file is a valid .zip archive and is not corrupt" );
            Log.e( "Utility", "ZipException in method getTexturePackName", ze );
            return null;
        }
        catch( IOException ioe )
        {
            ErrorLogger
                    .setLastError( "IO Error!  Please report, so problem can be fixed in future update" );
            Log.e( "Utility", "IOException in method getTexturePackName", ioe );
            return null;
        }
        catch( Exception e )
        {
            ErrorLogger
                    .setLastError( "Error! Please report, so problem can be fixed in future update" );
            Log.e( "Utility", "Unzip error", e );
            return null;
        }
        finally
        {
            if( zipfile != null )
                try
                {
                    zipfile.close();
                }
                catch( IOException ignored )
                {
                }
        }
        ErrorLogger.setLastError( "No compatible textures found in .zip archive" );
        Log.e( "Utility", ErrorLogger.getLastError() );
        return null;
    }
    
    /**
     * Unzips the first ROM found in a given ZipFile.
     * 
     * @param archive   The archive to search for the ROM in.
     * @param outputDir The output directory for the ROM (if found).
     * 
     * @return The absolute path to the outputted ROM, or null if an error occurs.
     */
    public static String unzipFirstROM( File archive, String outputDir )
    {
        String romName, romExt;
        String supportedExt = ".z64.v64.n64";
        
        if( archive == null )
            ErrorLogger.setLastError( "Zip file null in method unzipFirstROM" );
        else if( !archive.exists() )
            ErrorLogger
                    .setLastError( "Zip file '" + archive.getAbsolutePath() + "' does not exist" );
        else if( !archive.isFile() )
            ErrorLogger.setLastError( "Zip file '" + archive.getAbsolutePath()
                    + "' is not a file (method unzipFirstROM)" );
        
        if( ErrorLogger.hasError() )
        {
            Log.e( "Utility", ErrorLogger.getLastError() );
            return null;
        }
        ZipFile zipfile = null;
        try
        {
            zipfile = new ZipFile( archive );
            Enumeration<? extends ZipEntry> e = zipfile.entries();

            while( e.hasMoreElements() )
            {
                ZipEntry entry = e.nextElement();
                if( entry != null && !entry.isDirectory() )
                {
                    romName = entry.getName();
                    if( romName != null && romName.length() > 3 )
                    {
                        romExt = romName.substring( romName.length() - 4, romName.length() )
                                .toLowerCase( Locale.US );
                        if( supportedExt.contains( romExt ) )
                            return unzipEntry( zipfile, entry, outputDir );
                    }
                }
            }
        }
        catch( ZipException ze )
        {
            ErrorLogger
                    .setLastError( "Zip Error!  Ensure file is a valid .zip archive and is not corrupt" );
            Log.e( "Utility", "ZipException in method unzipFirstROM", ze );
            return null;
        }
        catch( IOException ioe )
        {
            ErrorLogger
                    .setLastError( "IO Error!  Please report, so problem can be fixed in future update" );
            Log.e( "Utility", "IOException in method unzipFirstROM", ioe );
            return null;
        }
        catch( Exception e )
        {
            ErrorLogger
                    .setLastError( "Error! Please report, so problem can be fixed in future update" );
            Log.e( "Utility", "Unzip error", e );
            return null;
        }
        finally
        {
            if( zipfile != null )
                try
                {
                    zipfile.close();
                }
                catch( IOException ignored )
                {
                }
        }
        
        ErrorLogger.setLastError( "No compatible ROMs found in .zip archive" );
        Log.e( "Utility", ErrorLogger.getLastError() );

        return null;
    }

    /**
     * Unzips a ZIP file in its entirety.
     *
     * @param archive   The archive to extract.
     * @param outputDir Directory to place all of the extracted files.
     *
     * @return True if extraction was successful, false otherwise.
     */
    public static boolean unzipAll( File archive, String outputDir )
    {
        if( archive == null )
            ErrorLogger.setLastError( "Zip file null in method unzipAll" );
        else if( !archive.exists() )
            ErrorLogger
                    .setLastError( "Zip file '" + archive.getAbsolutePath() + "' does not exist" );
        else if( !archive.isFile() )
            ErrorLogger.setLastError( "Zip file '" + archive.getAbsolutePath()
                    + "' is not a file (method unzipFirstROM)" );
        
        if( ErrorLogger.hasError() )
        {
            Log.e( "Utility", ErrorLogger.getLastError() );
            return false;
        }

        try
        {
            File f;
            ZipFile zipfile = new ZipFile( archive );
            Enumeration<? extends ZipEntry> e = zipfile.entries();

            while( e.hasMoreElements() )
            {
                ZipEntry entry = e.nextElement();
                if( entry != null && !entry.isDirectory() )
                {
                    f = new File( outputDir + "/" + entry.toString() );
                    f = f.getParentFile();
                    if( f != null )
                    {
                        f.mkdirs();
                        unzipEntry( zipfile, entry, outputDir );
                    }
                }
            }
        }
        catch( ZipException ze )
        {
            ErrorLogger
                    .setLastError( "Zip Error!  Ensure file is a valid .zip archive and is not corrupt" );
            Log.e( "Utility", "ZipException in method unzipAll", ze );
            return false;
        }
        catch( IOException ioe )
        {
            ErrorLogger
                    .setLastError( "IO Error!  Please report, so problem can be fixed in future update" );
            Log.e( "Utility", "IOException in method unzipAll", ioe );
            return false;
        }
        catch( Exception e )
        {
            ErrorLogger
                    .setLastError( "Error! Please report, so problem can be fixed in future update" );
            Log.e( "Utility", "Unzip error", e );
            return false;
        }

        return true;
    }

    // Unzips a specific entry from a ZIP file into the given output directory.
    //
    // Returns the absolute path to the outputted entry.
    // Returns null if the entry passed in happens to be a directory.
    private static String unzipEntry( ZipFile zipfile, ZipEntry entry, String outputDir )
            throws IOException
    {
        if( entry.isDirectory() )
        {
            ErrorLogger.setLastError( "Error! .zip entry '" + entry.getName()
                    + "' is a directory, not a file" );
            Log.e( "Utility", ErrorLogger.getLastError() );
            return null;
        }
        
        File outputFile = new File( outputDir, entry.getName() );
        String newFile = outputFile.getAbsolutePath();
        
        BufferedInputStream inputStream = new BufferedInputStream( zipfile.getInputStream( entry ) );
        BufferedOutputStream outputStream = new BufferedOutputStream( new FileOutputStream(
                outputFile ) );
        byte[] b = new byte[1024];
        int n;
        
        while( ( n = inputStream.read( b, 0, 1024 ) ) >= 0 )
        {
            outputStream.write( b, 0, n );
        }
        
        outputStream.close();
        inputStream.close();
        
        return newFile;
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
        if( !( context instanceof Activity ) )
            return null;
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics( metrics );
        return metrics;
    }
    
    /**
     * @author Kevin Kowalewski
     * @see <a href="http://stackoverflow.com/questions/1101380/determine-if-running-on-a-rooted-device">
     *    Determining if an app is running on a rooted device</a>
     */
    public static class Root
    {
        public static class ExecShell
        {
            private static final String LOG_TAG = ExecShell.class.getName();

            /**
             * Enum which represents a shell command to execute
             */
            public static enum SHELL_CMD
            {
                check_su_binary( new String[] { "/system/xbin/which", "su" } ), ;
                
                String[] command;
                
                SHELL_CMD( String[] command )
                {
                    this.command = command;
                }
            }

            /**
             * Executes a given shell command.
             *
             * @param shellCmd The shell command to execute
             *
             * @return An {@link ArrayList} which contains response strings
             */
            public ArrayList<String> executeCommand( SHELL_CMD shellCmd )
            {
                String line = null;
                ArrayList<String> fullResponse = new ArrayList<String>();
                Process localProcess = null;
                
                try
                {
                    localProcess = Runtime.getRuntime().exec( shellCmd.command );
                }
                catch( IOException couldNotExecCommand )
                {
                    return null;
                }
                
                BufferedReader in = new BufferedReader( new InputStreamReader(
                        localProcess.getInputStream() ) );
                
                try
                {
                    while( ( line = in.readLine() ) != null )
                    {
                        Log.d( LOG_TAG, "--> Line received: " + line );
                        fullResponse.add( line );
                    }

                    in.close();
                }
                catch( IOException e )
                {
                    e.printStackTrace();
                }

                Log.d( LOG_TAG, "--> Full response was: " + fullResponse );
                
                return fullResponse;
            }
            
        }

        /**
         * Checks if the device is rooted.
         *
         * @return True if the device is rooted, false otherwise.
         */
        public boolean isDeviceRooted()
        {
            return( checkRootMethod1() || checkRootMethod2() || checkRootMethod3() );
        }

        private boolean checkRootMethod1()
        {
            String buildTags = android.os.Build.TAGS;

            if( buildTags != null && buildTags.contains( "test-keys" ) )
            {
                return true;
            }
            return false;
        }

        // Checks if the SuperUser application is installed on the device in the /system/app directory
        private boolean checkRootMethod2()
        {
            try
            {
                File file = new File( "/system/app/Superuser.apk" );
                if( file.exists() )
                {
                    return true;
                }
            }
            catch( NullPointerException ignored )
            {
            }
            
            return false;
        }

        // Checks if the su binary can be found in the environment paths
        private boolean checkRootMethod3()
        {
            if( new ExecShell().executeCommand( ExecShell.SHELL_CMD.check_su_binary ) != null )
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }
}
