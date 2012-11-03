package paulscode.android.mupen64plusae.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.text.Html;
import android.util.Log;

public class FileUtil
{
    public static void populate( File startPath, boolean includeParent, boolean includeDirectories,
            boolean includeFiles, List<CharSequence> outEntries, List<String> outValues )
    {
        if( !startPath.exists() )
            return;
        
        if( startPath.isFile() )
            startPath = startPath.getParentFile();
        
        if( startPath.getParentFile() == null )
            includeParent = false;
        
        outEntries.clear();
        outValues.clear();
        if( includeParent )
        {
            outEntries.add( Html.fromHtml( "<b>..</b>" ) );
            outValues.add( startPath.getParentFile().getPath() );
        }
        if( includeDirectories )
        {
            for( File directory : getContents( startPath, new VisibleDirectoryFilter() ) )
            {
                outEntries.add( Html.fromHtml( "<b>" + directory.getName() + "</b>" ) );
                outValues.add( directory.getPath() );
            }
        }
        if( includeFiles )
        {
            for( File file : getContents( startPath, new VisibleFileFilter() ) )
            {
                outEntries.add( Html.fromHtml( file.getName() ) );
                outValues.add( file.getPath() );
            }
        }
    }
    
    public static List<File> getContents( File startPath, FileFilter fileFilter )
    {
        // Get a filtered, sorted list of files
        List<File> results = new ArrayList<File>();
        File[] files = startPath.listFiles( fileFilter );
        if( files != null )
        {
            Collections.addAll( results, files );
            Collections.sort( results, new FileUtil.FileComparer() );
        }
        return results;
    }
    
    private static class FileComparer implements Comparator<File>
    {
        // Compare files first by directory/file then alphabetically (case-insensitive)
        public int compare( File lhs, File rhs )
        {
            if( lhs.isDirectory() && rhs.isFile() )
                return -1;
            else if( lhs.isFile() && rhs.isDirectory() )
                return 1;
            else
                return lhs.getName().toLowerCase().compareTo( rhs.getName().toLowerCase() );
        }
    }
    
    private static class VisibleFileFilter implements FileFilter
    {
        // Include only non-hidden files not starting with '.'
        public boolean accept( File pathname )
        {
            return ( pathname != null ) && ( pathname.isFile() ) && ( !pathname.isHidden() )
                    && ( !pathname.getName().startsWith( "." ) );
        }
    }
    
    private static class VisibleDirectoryFilter implements FileFilter
    {
        // Include only non-hidden directories not starting with '.'
        public boolean accept( File pathname )
        {
            return ( pathname != null ) && ( pathname.isDirectory() ) && ( !pathname.isHidden() )
                    && ( !pathname.getName().startsWith( "." ) );
        }
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
            {
            }
            catch( NullPointerException npe )
            {
            }
            return true;
        }
    }

    /**
     * Loads the specified native library name (without "lib" and ".so").
     * 
     * @param libname absolute path to a native .so file (may optionally be in quotes)
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
     * Loads the native .so file specified.
     * 
     * @param filepath absolute path to a native .so file (may optionally be in quotes)
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
}
