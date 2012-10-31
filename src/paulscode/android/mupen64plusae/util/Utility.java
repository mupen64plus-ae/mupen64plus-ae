package paulscode.android.mupen64plusae.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.NativeMethods;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.Log;

// TODO: Cleanup, refactor, subdivide this class (I turned it into a dumping ground during
// refactoring (littleguy))
public class Utility
{
    /**
     * The Image class provides a simple interface to common image manipulation methods.
     */
    public static class Image
    {
        public BitmapDrawable drawable = null;
        public Bitmap image = null;
        public Rect drawRect = null;
        
        public int x = 0;
        public int y = 0;
        public int width = 0;
        public int height = 0;
        public int hWidth = 0;
        public int hHeight = 0;
        
        /**
         * Constructor: Loads an image file and sets the initial properties.
         * 
         * @param res
         *            Handle to the app resources.
         * @param filename
         *            Path to the image file.
         */
        public Image( Resources res, String filename )
        {
            image = BitmapFactory.decodeFile( filename );
            drawable = new BitmapDrawable( res, image );
            
            if( image != null )
            {
                width = image.getWidth();
                height = image.getHeight();
            }
            
            hWidth  = (int) ( (float) width  / 2.0f ); 
            hHeight = (int) ( (float) height / 2.0f );
            
            drawRect = new Rect();
        }
        
        /**
         * Constructor: Creates a clone copy of another Image.
         * 
         * @param res
         *            Handle to the app resources.
         * @param clone
         *            Image to copy.
         */
        public Image( Resources res, Image clone )
        {
            if( clone != null )
            {
                image = clone.image;
                drawable = new BitmapDrawable( res, image );
                width = clone.width;
                hWidth = clone.hWidth;
                height = clone.height;
                hHeight = clone.hHeight;
            }
            drawRect = new Rect();
        }
        
        /**
         * Sets the screen position of the image (in pixels).
         * 
         * @param x
         *            X-coordinate.
         * @param y
         *            Y-coordinate.
         */
        public void setPos( int x, int y )
        {
            this.x = x;
            this.y = y;
            if( drawRect != null )
                drawRect.set( x, y, x + width, y + height );
            if( drawable != null )
                drawable.setBounds( drawRect );
        }
        
        /**
         * Centers the image at the specified coordinates, without going beyond the specified screen
         * dimensions.
         * 
         * @param centerX
         *            X-coordinate to center the image at.
         * @param centerY
         *            Y-coordinate to center the image at.
         * @param screenW
         *            Horizontal screen dimension (in pixels).
         * @param screenH
         *            Vertical screen dimension (in pixels).
         */
        public void fitCenter( int centerX, int centerY, int screenW, int screenH )
        {
            int cx = centerX;
            int cy = centerY;
            
            if( cx < hWidth )
                cx = hWidth;
            if( cy < hHeight )
                cy = hHeight;
            if( cx + hWidth > screenW )
                cx = screenW - hWidth;
            if( cy + hHeight > screenH )
                cy = screenH - hHeight;
            
            x = cx - hWidth;
            y = cy - hHeight;
            
            if( drawRect != null )
            {
                drawRect.set( x, y, x + width, y + height );
                if( drawable != null )
                    drawable.setBounds( drawRect );
            }
        }
        
        /**
         * Centers the image at the specified coordinates, without going beyond the edges of the
         * specified rectangle.
         * 
         * @param centerX
         *            X-coordinate to center the image at.
         * @param centerY
         *            Y-coordinate to center the image at.
         * @param rectX
         *            X-coordinate of the bounding rectangle.
         * @param rectY
         *            Y-coordinate of the bounding rectangle.
         * @param rectW
         *            Horizontal bounding rectangle dimension (in pixels).
         * @param rectH
         *            Vertical bounding rectangle dimension (in pixels).
         */
        public void fitCenter( int centerX, int centerY, int rectX, int rectY, int rectW, int rectH )
        {
            int cx = centerX;
            int cy = centerY;
            
            if( cx < rectX + hWidth )
                cx = rectX + hWidth;
            if( cy < rectY + hHeight )
                cy = rectY + hHeight;
            if( cx + hWidth > rectX + rectW )
                cx = rectX + rectW - hWidth;
            if( cy + hHeight > rectY + rectH )
                cy = rectY + rectH - hHeight;
            
            x = cx - hWidth;
            y = cy - hHeight;
            
            if( drawRect != null )
            {
                drawRect.set( x, y, x + width, y + height );
                if( drawable != null )
                    drawable.setBounds( drawRect );
            }
        }
        
        /**
         * Draws the image.
         * 
         * @param canvas
         *            Canvas to draw the image on.
         */
        public void draw( Canvas canvas )
        {
            if( drawable != null )
                drawable.draw( canvas );
        }
    }

    /**
     * The Point class is a basic interface for storing 2D float coordinates.
     */
    public static class Point
    {
        public float x;
        public float y;
        
        /**
         * Constructor: Creates a new point at the origin
         */
        public Point()
        {
            x = 0;
            y = 0;
        }
    }

    public static String getHeaderName( String filename )
    {
        ErrorLogger.put( "READ_HEADER", "fail", "" );
        if( filename == null || filename.length() < 1 )
        {
            ErrorLogger.put( "READ_HEADER", "fail", "filename not specified" );
            Log.e( "Utility", "filename not specified in method 'getHeaderName'" ); 
            return null;
        }
        else if( filename.substring( filename.length() - 3, filename.length() ).equalsIgnoreCase( "zip" ) )
        {
            // Create the tmp folder if it doesn't exist:
            File tmpFolder = new File( Globals.paths.dataDir + "/tmp" );
            tmpFolder.mkdir();
            
            // Clear the folder if anything is in there:
            String[] children = tmpFolder.list();
            for( String child : children )
            {
                deleteFolder( new File( tmpFolder, child ) );
            }
            
            ErrorLogger.clearLastError();
            String uzFile = unzipFirstROM( new File( filename ), Globals.paths.dataDir + "/tmp" );
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
                    ErrorLogger.put( "READ_HEADER", "fail", "Unable to unzip ROM: '" + filename + "'" );
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
                {}
                return headerName;
            }
        }
        else
        {
            return NativeMethods.getHeaderName( filename );
        }
    }

    public static String getHeaderCRC( String filename )
    {
        ErrorLogger.put( "READ_HEADER", "fail", "" );
        if( filename == null || filename.length() < 1 )
        {
            ErrorLogger.put( "READ_HEADER", "fail", "filename not specified" );
            Log.e( "Utility", "filename not specified in method 'getHeaderCRC'" ); 
            return null;
        }
        else if( filename.substring( filename.length() - 3, filename.length() ).equalsIgnoreCase( "zip" ) )
        {
            // Create the tmp folder if it doesn't exist:
            File tmpFolder = new File( Globals.paths.dataDir + "/tmp" );
            tmpFolder.mkdir();
            
            // Clear the folder if anything is in there:
            String[] children = tmpFolder.list();
            for( String child : children )
            {
                deleteFolder( new File( tmpFolder, child ) );
            }
            
            ErrorLogger.clearLastError();
            String uzFile = unzipFirstROM( new File( filename ), Globals.paths.dataDir + "/tmp" );
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
                    ErrorLogger.put( "READ_HEADER", "fail", "Unable to unzip ROM: '" + filename + "'" );
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
                {}
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
        if( CRC == null || CRC.length() < 3)
            return null;
        
        int x = CRC.indexOf( " " );
        if( x < 1 || x >= CRC.length() - 1 )
            return null;  // The CRC should always contain a space, and it shouldn't be the last character
        
        if( CRC.length() == 17 )
            return CRC.toUpperCase();  // We probably have the full CRC, just upper-case it.
        
        String CRC_1 = "00000000" + CRC.substring( 0, x ).toUpperCase().trim();
        String CRC_2 = "00000000" + CRC.substring( x + 1, CRC.length() ).toUpperCase().trim();
        return CRC_1.substring( CRC_1.length() - 8, CRC_1.length() ) + " " + CRC_2.substring( CRC_2.length() - 8, CRC_2.length() );
    }

    @SuppressWarnings("unused")
    public static String getTexturePackName( String filename )
    {
        int x;
        String textureName, textureExt;
        String supportedExt = ".png";
        File archive = new File( filename );

        if( archive == null )
            ErrorLogger.setLastError( "Zip file null in method getTexturePackName" );
        else if( !archive.exists() )
            ErrorLogger.setLastError( "Zip file '" + archive.getAbsolutePath() + "' does not exist" );
        else if( !archive.isFile() )
            ErrorLogger.setLastError( "Zip file '" + archive.getAbsolutePath() + "' is not a file (method unzipFirstROM)" );

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
                                                            textureName.length() ).toLowerCase();
                        if( supportedExt.contains( textureExt ) )
                        {
                            x = textureName.indexOf( "#" );
                            if( x > 0 && x < textureName.length() )
                            {
                                textureName = textureName.substring( 0, x );
                                if( textureName != null && textureName.length() > 0 )
                                {
                                    x = textureName.lastIndexOf( "/" );
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
            ErrorLogger.setLastError( "Zip Error!  Ensure file is a valid .zip archive and is not corrupt" );
            Log.e( "Utility", "ZipException in method getTexturePackName", ze );
            return null;
        }
        catch( IOException ioe )
        {
            ErrorLogger.setLastError( "IO Error!  Please report, so problem can be fixed in future update" );
            Log.e( "Utility", "IOException in method getTexturePackName", ioe );
            return null;
        }
        catch( Exception e )
        {
            ErrorLogger.setLastError( "Error! Please report, so problem can be fixed in future update" );
            Log.e( "Utility", "Unzip error", e );
            return null;
        }
        ErrorLogger.setLastError( "No compatible textures found in .zip archive" );
        Log.e( "Utility", ErrorLogger.getLastError() );
        return null;
    }

    public static boolean deleteFolder( File folder )
    {
        if( folder.isDirectory() )
        {
            String[] children = folder.list();
            for( String child : children )
            {
                boolean success = deleteFolder( new File( folder, child ) );
                if( !success )
                    return false;
            }
        }
        return folder.delete();
    }

    public static boolean copyFile( File src, File dest )
    {
        if( src == null )
            return true;

        if( dest == null )
        {
            Log.e( "Updater", "dest null in method 'copyFile'" );
            return false;
        }

        if( src.isDirectory() )
        {
            boolean success = true;
            if( !dest.exists() )
                dest.mkdirs();
            String[] files = src.list();
            for( String file : files )
            {
                success = success && copyFile( new File( src, file ), new File( dest, file ) );
            }
            return success;
        }
        else
        {
            File f = dest.getParentFile();
            if( f == null )
            {
                Log.e( "Updater", "dest parent folder null in method 'copyFile'" );
                return false;
            }
            if( !f.exists() )
                f.mkdirs();

            InputStream in = null;
            OutputStream out = null;
            try
            {
                in = new FileInputStream( src );
                out = new FileOutputStream( dest );

                byte[] buf = new byte[1024];
                int len;
                while( ( len = in.read( buf ) ) > 0 )
                {
                    out.write( buf, 0, len );
                }
            }
            catch( IOException ioe )
            {
                Log.e( "Updater", "IOException in method 'copyFile': " + ioe.getMessage() );
                return false;
            }
            try
            {
                in.close();
                out.close();
            }
            catch( IOException ioe )
            {}
            catch( NullPointerException npe )
            {}
            return true;
        }
    }

    public static String unzipFirstROM( File archive, String outputDir )
    {
        String romName, romExt;
        String supportedExt = ".z64.v64.n64";

        if( archive == null )
            ErrorLogger.setLastError( "Zip file null in method unzipFirstROM" );
        else if( !archive.exists() )
            ErrorLogger.setLastError( "Zip file '" + archive.getAbsolutePath() + "' does not exist" );
        else if( !archive.isFile() )
            ErrorLogger.setLastError( "Zip file '" + archive.getAbsolutePath() + "' is not a file (method unzipFirstROM)" );

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
                        romExt = romName.substring( romName.length() - 4, romName.length() ).toLowerCase();
                        if( supportedExt.contains( romExt ) )
                            return unzipEntry( zipfile, entry, outputDir );
                    }
                }
            }
        }
        catch( ZipException ze )
        {
            ErrorLogger.setLastError( "Zip Error!  Ensure file is a valid .zip archive and is not corrupt" );
            Log.e( "Utility", "ZipException in method unzipFirstROM", ze );
            return null;
        }
        catch( IOException ioe )
        {
            ErrorLogger.setLastError( "IO Error!  Please report, so problem can be fixed in future update" );
            Log.e( "Utility", "IOException in method unzipFirstROM", ioe );
            return null;
        }
        catch( Exception e )
        {
            ErrorLogger.setLastError( "Error! Please report, so problem can be fixed in future update" );
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
            ErrorLogger.setLastError( "Zip file '" + archive.getAbsolutePath() + "' does not exist" );
        else if( !archive.isFile() )
            ErrorLogger.setLastError( "Zip file '" + archive.getAbsolutePath() + "' is not a file (method unzipFirstROM)" );

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
            ErrorLogger.setLastError( "Zip Error!  Ensure file is a valid .zip archive and is not corrupt" );
            Log.e( "Utility", "ZipException in method unzipAll", ze );
            return false;
        }
        catch( IOException ioe )
        {
            ErrorLogger.setLastError( "IO Error!  Please report, so problem can be fixed in future update" );
            Log.e( "Utility", "IOException in method unzipAll", ioe );
            return false;
        }
        catch( Exception e )
        {
            ErrorLogger.setLastError( "Error! Please report, so problem can be fixed in future update" );
            Log.e( "Utility", "Unzip error", e );
            return false;
        }
        return true;
    }

    private static String unzipEntry( ZipFile zipfile, ZipEntry entry, String outputDir ) throws IOException
    {
        if( entry.isDirectory() )
        {
            ErrorLogger.setLastError( "Error! .zip entry '" + entry.getName() + "' is a directory, not a file" );
            Log.e( "Utility", ErrorLogger.getLastError() );
            return null;
        }

        File outputFile = new File( outputDir, entry.getName() );
        String newFile = outputFile.getAbsolutePath();

        BufferedInputStream inputStream = new BufferedInputStream( zipfile.getInputStream( entry ) );
        BufferedOutputStream outputStream = new BufferedOutputStream( new FileOutputStream( outputFile ) );
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

    /**
     * Converts a string into an integer.
     * @param val String containing the number to convert.
     * @param fail Value to use if unable to convert val to an integer.
     * @return The converted integer, or the specified value if unsuccessful.
     */
    public static int toInt( String val, int fail )
    {
        if( val == null || val.length() < 1 )
            return fail;  // Not a number
        try
        {
            return Integer.valueOf( val );  // Convert to integer
        }
        catch( NumberFormatException nfe )
        {}

        return fail;  // Conversion failed
    }
    
    /**
     * Converts a string into a float.
     * @param val String containing the number to convert.
     * @param fail Value to use if unable to convert val to an float.
     * @return The converted float, or the specified value if unsuccessful.
     */
    public static float toFloat( String val, float fail )
    {
        if( val == null || val.length() < 1 )
            return fail;  // Not a number
        try
        {
            return Float.valueOf( val );  // Convert to float
        }
        catch( NumberFormatException nfe )
        {}

        return fail;  // Conversion failed
    }

    public static void safeSleep( int milliseconds )
    {
        try
        {
            Thread.sleep( milliseconds );
        }
        catch( InterruptedException e )
        {
        }
    }

    public static void systemExitFriendly( String message, Activity activity, int milliseconds )
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

    /**
     * Loads the specified native library name (without "lib" and ".so")
     * 
     * @param libname
     *            Full path to a native .so file (may optionally be in quotes).
     */
    public static void loadNativeLibName( String libname )
    {
        Log.v( "GameActivity", "Loading native library '" + libname + "'" );
        try
        {
            System.loadLibrary( libname );
        }
        catch( UnsatisfiedLinkError e )
        {
            Log.e( "GameActivity", "Unable to load native library '" + libname + "'" );
        }
    }

    /**
     * Loads the native .so file specified
     * 
     * @param filepath
     *            Full path to a native .so file (may optionally be in quotes).
     */
    public static void loadNativeLib( String filepath )
    {
        String filename = null;
        if( filepath != null && filepath.length() > 0 )
        {
            filename = filepath.replace( "\"", "" );
            if( filename.equalsIgnoreCase( "dummy" ) )
                return;
            
            Log.v( "GameActivity", "Loading native library '" + filename + "'" );
            try
            {
                System.load( filename );
            }
            catch( UnsatisfiedLinkError e )
            {
                Log.e( "GameActivity", "Unable to load native library '" + filename + "'" );
            }
        }
    }

    /**
     * Determines if the two specified line segments intersect with each other, and calculates where
     * the intersection occurs if they do.
     * 
     * @param seg1pt1_x
     *            X-coordinate for the first end of the first line segment.
     * @param seg1pt1_y
     *            Y-coordinate for the first end of the first line segment.
     * @param seg1pt2_x
     *            X-coordinate for the second end of the first line segment.
     * @param seg1pt2_y
     *            Y-coordinate for the second end of the first line segment.
     * @param seg2pt1_x
     *            X-coordinate for the first end of the second line segment.
     * @param seg2pt1_y
     *            Y-coordinate for the first end of the second line segment.
     * @param seg2pt2_x
     *            X-coordinate for the second end of the second line segment.
     * @param seg2pt2_y
     *            Y-coordinate for the second end of the second line segment.
     * @param crossPt
     *            Changed to the point of intersection if there is one, otherwise unchanged.
     * @return True if the two line segments intersect.
     */
    public static boolean segsCross( float seg1pt1_x, float seg1pt1_y, float seg1pt2_x,
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
            
        float s = ( -vec1_y * ( seg1pt1_x - seg2pt1_x ) + vec1_x * ( seg1pt1_y - seg2pt1_y ) )
                / div;
        float t = ( vec2_x * ( seg1pt1_y - seg2pt1_y ) - vec2_y * ( seg1pt1_x - seg2pt1_x ) ) / div;
        
        if( s >= 0 && s < 1 && t >= 0 && t <= 1 )
        {
            // Segments cross, point of intersection stored in 'crossPt'
            crossPt.x = seg1pt1_x + ( t * vec1_x );
            crossPt.y = seg1pt1_y + ( t * vec1_y );
            return true;
        }
        
        // Segments don't cross
        return false;
    }
}
