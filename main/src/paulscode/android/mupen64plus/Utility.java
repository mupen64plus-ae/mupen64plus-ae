package paulscode.android.mupen64plus;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File; 
import java.io.FileInputStream; 
import java.io.FileOutputStream; 
import java.lang.Integer;
import java.lang.NumberFormatException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream; 

import android.content.res.Configuration;

import android.app.*;
import android.content.*;
import android.view.*;
import android.os.*;
import android.util.Log;
import android.graphics.*;
import android.text.method.*;
import android.text.*;
import android.util.AttributeSet;
import android.media.*;
import android.hardware.*;
import android.content.*;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup.LayoutParams;

public class Utility
{
    public static String getHeaderName( String filename )
    {
        MenuActivity.error_log.put( "READ_HEADER", "fail", "" );
        MenuActivity.error_log.save();
        if( filename == null || filename.length() < 1 )
        {
            MenuActivity.error_log.put( "READ_HEADER", "fail", "filename not specified" );
            MenuActivity.error_log.save();
            Log.e( "Utility", "filename not specified in method 'getHeaderName'" ); 
            return null;
        }
        else if( filename.substring( filename.length() - 3, filename.length() ).equalsIgnoreCase( "zip" ) )
        {
            // Create the tmp folder if it doesn't exist:
            File tmpFolder = new File( Globals.DataDir + "/tmp" );
            tmpFolder.mkdir();
            // Clear the folder if anything is in there:
            String[] children = tmpFolder.list();
            for( String child : children )
            {
                deleteFolder( new File( tmpFolder, child ) );
            }
            Globals.errorMessage = null;
            String uzFile = unzipFirstROM( new File( filename ), Globals.DataDir + "/tmp" );
            if( uzFile == null || uzFile.length() < 1 )
            {
                Log.e( "Utility", "Unable to unzip ROM: '" + filename + "'" ); 
                if( Globals.errorMessage != null )
                {
                    MenuActivity.error_log.put( "READ_HEADER", "fail", Globals.errorMessage );
                    MenuActivity.error_log.save();
                    Globals.errorMessage = null;
                }
                else
                {
                    MenuActivity.error_log.put( "READ_HEADER", "fail", "Unable to unzip ROM: '" + filename + "'" );
                    MenuActivity.error_log.save();
                }
                return null;
            }
            else
            {
                String headerName = GameActivityCommon.nativeGetHeaderName( uzFile );
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
            return GameActivityCommon.nativeGetHeaderName( filename );
        }
    }

    public static String getHeaderCRC( String filename )
    {
        MenuActivity.error_log.put( "READ_HEADER", "fail", "" );
        MenuActivity.error_log.save();
        if( filename == null || filename.length() < 1 )
        {
            MenuActivity.error_log.put( "READ_HEADER", "fail", "filename not specified" );
            MenuActivity.error_log.save();
            Log.e( "Utility", "filename not specified in method 'getHeaderCRC'" ); 
            return null;
        }
        else if( filename.substring( filename.length() - 3, filename.length() ).equalsIgnoreCase( "zip" ) )
        {
            // create the tmp folder if it doesn't exist:
            File tmpFolder = new File( Globals.DataDir + "/tmp" );
            tmpFolder.mkdir();
            // clear the folder if anything is in there:
            String[] children = tmpFolder.list();
            for( String child : children )
            {
                deleteFolder( new File( tmpFolder, child ) );
            }
            Globals.errorMessage = null;
            String uzFile = unzipFirstROM( new File( filename ), Globals.DataDir + "/tmp" );
            if( uzFile == null || uzFile.length() < 1 )
            {
                Log.e( "Utility", "Unable to unzip ROM: '" + filename + "'" ); 
                if( Globals.errorMessage != null )
                {
                    MenuActivity.error_log.put( "READ_HEADER", "fail", Globals.errorMessage );
                    MenuActivity.error_log.save();
                    Globals.errorMessage = null;
                }
                else
                {
                    MenuActivity.error_log.put( "READ_HEADER", "fail", "Unable to unzip ROM: '" + filename + "'" );
                    MenuActivity.error_log.save();
                }
                return null;
            }
            else
            {
                String headerCRC = checkCRC( GameActivityCommon.nativeGetHeaderCRC( uzFile ) );
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
            return checkCRC( GameActivityCommon.nativeGetHeaderCRC( filename ) );
        }
    }

    public static String checkCRC( String CRC )
    {
        if( CRC == null )
            return null;
        if( CRC.length() < 3 )
            return null;  // the smallest possible CRCs are "0 0", "1 a", etc.
        int x = CRC.indexOf( " " );
        if( x < 1 || x >= CRC.length() - 1 )
            return null;  // the CRC should always contain a space, and it shouldn't be the last character
        if( CRC.length() == 17 )
            return CRC.toUpperCase();  // we probably have the full CRC, just upper-case it.
        String CRC_1 = "00000000" + CRC.substring( 0, x ).toUpperCase().trim();
        String CRC_2 = "00000000" + CRC.substring( x + 1, CRC.length() ).toUpperCase().trim();
        return CRC_1.substring( CRC_1.length() - 8, CRC_1.length() ) + " " + CRC_2.substring( CRC_2.length() - 8, CRC_2.length() );
    }

    public static String getTexturePackName( String filename )
    {
        int x;
        String textureName, textureExt;
        String supportedExt = ".png";
        File archive = new File( filename );

        if( archive == null )
            Globals.errorMessage = "Zip file null in method getTexturePackName";
        else if( !archive.exists() )
            Globals.errorMessage = "Zip file '" + archive.getAbsolutePath() + "' does not exist";
        else if( !archive.isFile() )
            Globals.errorMessage = "Zip file '" + archive.getAbsolutePath() + "' is not a file (method unzipFirstROM)";

        if( Globals.errorMessage != null )
        {
            Log.e( "Utility", Globals.errorMessage );
            return null;
        }
        try
        {
            ZipFile zipfile = new ZipFile( archive );
            Enumeration e = zipfile.entries();
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
            Globals.errorMessage = "Zip Error!  Ensure file is a valid .zip archive and is not corrupt";
            Log.e( "Utility", "ZipException in method getTexturePackName", ze );
            return null;
        }
        catch( IOException ioe )
        {
            Globals.errorMessage = "IO Error!  Please report, so problem can be fixed in future update";
            Log.e( "Utility", "IOException in method getTexturePackName", ioe );
            return null;
        }
        catch( Exception e )
        {
            Globals.errorMessage = "Error! Please report, so problem can be fixed in future update";
            Log.e( "Utility", "Unzip error", e );
            return null;
        }
        Globals.errorMessage = "No compatible textures found in .zip archive";
        Log.e( "Utility", Globals.errorMessage );
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

    public static String unzipFirstROM( File archive, String outputDir )
    {
        String romName, romExt;
        String supportedExt = ".z64.v64.n64";

        if( archive == null )
            Globals.errorMessage = "Zip file null in method unzipFirstROM";
        else if( !archive.exists() )
            Globals.errorMessage = "Zip file '" + archive.getAbsolutePath() + "' does not exist";
        else if( !archive.isFile() )
            Globals.errorMessage = "Zip file '" + archive.getAbsolutePath() + "' is not a file (method unzipFirstROM)";

        if( Globals.errorMessage != null )
        {
            Log.e( "Utility", Globals.errorMessage );
            return null;
        }
        try
        {
            ZipFile zipfile = new ZipFile( archive );
            Enumeration e = zipfile.entries();

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
            Globals.errorMessage = "Zip Error!  Ensure file is a valid .zip archive and is not corrupt";
            Log.e( "Utility", "ZipException in method unzipFirstROM", ze );
            return null;
        }
        catch( IOException ioe )
        {
            Globals.errorMessage = "IO Error!  Please report, so problem can be fixed in future update";
            Log.e( "Utility", "IOException in method unzipFirstROM", ioe );
            return null;
        }
        catch( Exception e )
        {
            Globals.errorMessage = "Error! Please report, so problem can be fixed in future update";
            Log.e( "Utility", "Unzip error", e );
            return null;
        }
        Globals.errorMessage = "No compatible ROMs found in .zip archive";
        Log.e( "Utility", Globals.errorMessage );
        return null;
    }
 
    public static boolean unzipAll( File archive, String outputDir )
    {
        if( archive == null )
            Globals.errorMessage = "Zip file null in method unzipAll";
        else if( !archive.exists() )
            Globals.errorMessage = "Zip file '" + archive.getAbsolutePath() + "' does not exist";
        else if( !archive.isFile() )
            Globals.errorMessage = "Zip file '" + archive.getAbsolutePath() + "' is not a file (method unzipFirstROM)";

        if( Globals.errorMessage != null )
        {
            Log.e( "Utility", Globals.errorMessage );
            return false;
        }
        try
        {
            File f;
            ZipFile zipfile = new ZipFile( archive );
            Enumeration e = zipfile.entries();
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
            Globals.errorMessage = "Zip Error!  Ensure file is a valid .zip archive and is not corrupt";
            Log.e( "Utility", "ZipException in method unzipAll", ze );
            return false;
        }
        catch( IOException ioe )
        {
            Globals.errorMessage = "IO Error!  Please report, so problem can be fixed in future update";
            Log.e( "Utility", "IOException in method unzipAll", ioe );
            return false;
        }
        catch( Exception e )
        {
            Globals.errorMessage = "Error! Please report, so problem can be fixed in future update";
            Log.e( "Utility", "Unzip error", e );
            return false;
        }
        return true;
    }

    private static String unzipEntry( ZipFile zipfile, ZipEntry entry, String outputDir ) throws IOException
    {
        if( entry.isDirectory() )
        {
            Globals.errorMessage = "Error! .zip entry '" + entry.getName() + "' is a directory, not a file";
            Log.e( "Utility", Globals.errorMessage );
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

    public static int toInt( String val, int fail )
    {
        if( val == null || val.length() < 1 )
            return fail;  // Not a number
        try
        {
            return Integer.valueOf( val ).intValue();  // Convert to integer
        }
        catch( NumberFormatException nfe )
        {}

        return fail;  // Conversion failed
    }
}
