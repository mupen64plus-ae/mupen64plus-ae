package paulscode.android.mupen64plusae.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.NativeMethods;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Point;
import android.util.FloatMath;
import android.util.Log;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.MotionEvent;

/**
 * Utility class which collects a bunch of commonly used methods into one class.
 */
public class Utility
{
    /**
     * Clamps an integer value to the limit defined by min and max.
     * 
     * @param val The value to clamp to min and max.
     * @param min The lowest number val can be equal to.
     * @param max The largest number val can be equal to.
     * 
     * @return If the number is lower than min, min is returned. <br/>
     *         If the number is higher than max, max is returned.
     */
    public static int clamp( int val, int min, int max )
    {
        return Math.max( Math.min( val, max ), min );
    }
    
    /**
     * Clamps a float value to the limit defined by min and max.
     * 
     * @param val The value to clamp between min and max.
     * @param min The lowest number val can be equal to.
     * @param max The largest number val can be equal to.
     * 
     * @return If the number is lower than min, min is returned. <br/>
     *         If the number is larger than max, max is returned.
     */
    public static float clamp( float val, float min, float max )
    {
        return Math.max( Math.min( val, max ), min );
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
     * Gets the hardware information from /proc/cpuinfo.
     * 
     * @return The hardware string.
     */
    public static String getCpuInfo()
    {
        // TODO: Simplify this... somehow.
        
        // From http://android-er.blogspot.com/2009/09/read-android-cpu-info.html
        String result = "";
        try
        {
            String[] args = { "/system/bin/cat", "/proc/cpuinfo" };
            Process process = new ProcessBuilder( args ).start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[1024];
            while( in.read( re ) != -1 )
                result = result + new String( re );
            in.close();
        }
        catch( IOException ex )
        {
            ex.printStackTrace();
        }
        return result;
    }
    
    /**
     * Gets the peripheral information from the appropriate Android API.
     * 
     * @return The peripheral info string.
     */
    @TargetApi( 16 )
    public static String getPeripheralInfo( Activity activity )
    {
        StringBuilder builder = new StringBuilder();
        
        if( Globals.IS_GINGERBREAD )
        {
            int[] ids = InputDevice.getDeviceIds();
            for( int i = 0; i < ids.length; i++ )
            {
                InputDevice device = InputDevice.getDevice( ids[i] );
                
                if( !Globals.IS_JELLYBEAN || !device.isVirtual() )
                {
                    List<MotionRange> ranges;
                    if( Globals.IS_HONEYCOMB_MR1 )
                    {
                        ranges = device.getMotionRanges();
                    }
                    else
                    {
                        // Earlier APIs we have to do it the hard way
                        // TODO: Smelly... must be a better way
                        ranges = new ArrayList<MotionRange>();
                        boolean finished = false;
                        for( int j = 0; j < 256 && !finished; j++ )
                        {
                            try
                            {
                                if( device.getMotionRange( j ) != null )
                                    ranges.add( device.getMotionRange( j ) );
                            }
                            catch( Exception e )
                            {
                                finished = true;
                                Log.i( "Utility", "Number of axes = " + j );
                                // In Xperia PLAY (API 9) I found this to be 9
                                // Not sure if this is device or API specific
                            }
                        }
                    }
                    
                    if( ranges.size() > 0 )
                    {
                        builder.append( "Device: " + device.getName() + "\r\n" );
                        builder.append( "Id: " + device.getId() + "\r\n" );
                        if( Globals.IS_JELLYBEAN && device.getVibrator().hasVibrator() )
                        {
                            builder.append( "Vibrator: true\r\n" );
                        }
                        builder.append( "Axes: " + ranges.size() + "\r\n" );
                        for( int j = 0; j < ranges.size(); j++ )
                        {
                            MotionRange range = ranges.get( j );
                            String axisName = Globals.IS_HONEYCOMB_MR1
                                    ? MotionEvent.axisToString( range.getAxis() )
                                    : "Axis " + j;
                            builder.append( "  " + axisName + ": ( " + range.getMin() + " , "
                                    + range.getMax() + " )\r\n" );
                        }
                        builder.append( "\r\n" );
                    }
                }
            }
        }
        
        // if( Globals.IS_HONEYCOMB_MR1 )
        // {
        // builder.append( "USB Devices:\r\n\r\n" );
        // UsbManager manager = (UsbManager) activity.getSystemService( Context.USB_SERVICE );
        // HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        // for( String key : deviceList.keySet() )
        // {
        // UsbDevice device = deviceList.get( key );
        // builder.append( "DeviceName: " + device.getDeviceName() + "\r\n" );
        // builder.append( "DeviceId: " + device.getDeviceId() + "\r\n" );
        // builder.append( "DeviceClass: " + device.getDeviceClass() + "\r\n" );
        // builder.append( "DeviceSubclass: " + device.getDeviceSubclass() + "\r\n" );
        // builder.append( "DeviceProtocol: " + device.getDeviceProtocol() + "\r\n" );
        // builder.append( "VendorId: " + device.getVendorId() + "\r\n" );
        // builder.append( "ProductId: " + device.getProductId() + "\r\n" );
        // builder.append( "\r\n" );
        // }
        // }
        
        return builder.toString();
    }
    
    public static String getHeaderName( String filename, String tempDir )
    {
        ErrorLogger.put( "READ_HEADER", "fail", "" );
        if( filename == null || filename.length() < 1 )
        {
            ErrorLogger.put( "READ_HEADER", "fail", "filename not specified" );
            Log.e( "Utility", "filename not specified in method 'getHeaderName'" );
            return null;
        }
        else if( filename.substring( filename.length() - 3, filename.length() ).equalsIgnoreCase(
                "zip" ) )
        {
            // Create the tmp folder if it doesn't exist:
            File tmpFolder = new File( tempDir );
            tmpFolder.mkdir();
            
            // Clear the folder if anything is in there:
            String[] children = tmpFolder.list();
            for( String child : children )
            {
                FileUtil.deleteFolder( new File( tmpFolder, child ) );
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
                String headerName = NativeMethods.getHeaderName( uzFile );
                try
                {
                    new File( uzFile ).delete();
                }
                catch( Exception e )
                {
                }
                return headerName;
            }
        }
        else
        {
            return NativeMethods.getHeaderName( filename );
        }
    }
    
    public static String getHeaderCRC( String filename, String tempDir )
    {
        ErrorLogger.put( "READ_HEADER", "fail", "" );
        if( filename == null || filename.length() < 1 )
        {
            ErrorLogger.put( "READ_HEADER", "fail", "filename not specified" );
            Log.e( "Utility", "filename not specified in method 'getHeaderCRC'" );
            return null;
        }
        else if( filename.substring( filename.length() - 3, filename.length() ).equalsIgnoreCase(
                "zip" ) )
        {
            // Create the tmp folder if it doesn't exist:
            File tmpFolder = new File( tempDir );
            tmpFolder.mkdir();
            
            // Clear the folder if anything is in there:
            String[] children = tmpFolder.list();
            for( String child : children )
            {
                FileUtil.deleteFolder( new File( tmpFolder, child ) );
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
                String headerCRC = checkCRC( NativeMethods.getHeaderCRC( uzFile ) );
                try
                {
                    new File( uzFile ).delete();
                }
                catch( Exception e )
                {
                }
                return headerCRC;
            }
        }
        else
        {
            return checkCRC( NativeMethods.getHeaderCRC( filename ) );
        }
    }
    
    public static String checkCRC( String CRC )
    {
        // The smallest possible CRCs are "0 0", "1 a", etc.
        if( CRC == null || CRC.length() < 3 )
            return null;
        
        // The CRC should always contain a space, and it
        // shouldn't be the last character
        int x = CRC.indexOf( ' ' );
        if( x < 1 || x >= CRC.length() - 1 )
            return null;
        
        // We probably have the full CRC, just upper-case it.
        if( CRC.length() == 17 )
            return CRC.toUpperCase( Locale.ENGLISH );
        
        String CRC_1 = "00000000" + CRC.substring( 0, x ).toUpperCase( Locale.ENGLISH ).trim();
        String CRC_2 = "00000000" + CRC.substring( x + 1, CRC.length() ).toUpperCase( Locale.ENGLISH ).trim();
        return CRC_1.substring( CRC_1.length() - 8, CRC_1.length() ) + " "
                + CRC_2.substring( CRC_2.length() - 8, CRC_2.length() );
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
        try
        {
            ZipFile zipfile = new ZipFile( archive );
            Enumeration<? extends ZipEntry> e = zipfile.entries();
            while( e.hasMoreElements() )
            {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if( entry != null && !entry.isDirectory() )
                {
                    textureName = entry.getName();
                    if( textureName != null && textureName.length() > 3 )
                    {
                        textureExt = textureName.substring( textureName.length() - 4,
                                textureName.length() ).toLowerCase( Locale.ENGLISH );
                        if( supportedExt.contains( textureExt ) )
                        {
                            x = textureName.indexOf( '#' );
                            if( x > 0 && x < textureName.length() )
                            {
                                textureName = textureName.substring( 0, x );
                                if( textureName != null && textureName.length() > 0 )
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
        ErrorLogger.setLastError( "No compatible textures found in .zip archive" );
        Log.e( "Utility", ErrorLogger.getLastError() );
        return null;
    }
    
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
        try
        {
            ZipFile zipfile = new ZipFile( archive );
            Enumeration<? extends ZipEntry> e = zipfile.entries();
            
            while( e.hasMoreElements() )
            {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if( entry != null && !entry.isDirectory() )
                {
                    romName = entry.getName();
                    if( romName != null && romName.length() > 3 )
                    {
                        romExt = romName.substring( romName.length() - 4, romName.length() )
                                .toLowerCase( Locale.ENGLISH );
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
        ErrorLogger.setLastError( "No compatible ROMs found in .zip archive" );
        Log.e( "Utility", ErrorLogger.getLastError() );
        return null;
    }
    
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
                ZipEntry entry = (ZipEntry) e.nextElement();
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
        byte b[] = new byte[1024];
        int n;
        
        while( ( n = inputStream.read( b, 0, 1024 ) ) >= 0 )
        {
            outputStream.write( b, 0, n );
        }
        
        outputStream.close();
        inputStream.close();
        
        return newFile;
    }
}
