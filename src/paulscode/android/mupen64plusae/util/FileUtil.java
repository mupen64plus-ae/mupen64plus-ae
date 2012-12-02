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

/**
 * Utility class that provides methods
 * which simplify file I/O tasks.
 */
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
        @Override
        public int compare( File lhs, File rhs )
        {
            if( lhs.isDirectory() && rhs.isFile() )
                return -1;
            else if( lhs.isFile() && rhs.isDirectory() )
                return 1;
            else
                return lhs.getName().compareToIgnoreCase( rhs.getName() );
        }
    }
    
    private static class VisibleFileFilter implements FileFilter
    {
        // Include only non-hidden files not starting with '.'
        @Override
        public boolean accept( File pathname )
        {
            return ( pathname != null ) && ( pathname.isFile() ) && ( !pathname.isHidden() )
                    && ( !pathname.getName().startsWith( "." ) );
        }
    }
    
    private static class VisibleDirectoryFilter implements FileFilter
    {
        // Include only non-hidden directories not starting with '.'
        @Override
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
        {
            Log.e("FileUtil", "src null in method 'copyFile'");
            return false;
        }
        
        if( dest == null )
        {
            Log.e( "FileUtil", "dest null in method 'copyFile'" );
            return false;
        }
        
        if( src.isDirectory() )
        {
            boolean success = true;
            String[] files = src.list();
            
            if( !dest.exists() )
                dest.mkdirs();
            
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
                Log.e( "FileUtil", "dest parent folder null in method 'copyFile'" );
                return false;
            }
            
            if( !f.exists() )
                f.mkdirs();
            
            try
            {
                final InputStream in = new FileInputStream( src );
                final OutputStream out = new FileOutputStream( dest );
                
                byte[] buf = new byte[1024];
                int len;
                while( ( len = in.read( buf ) ) > 0 )
                {
                    out.write( buf, 0, len );
                }
                
                in.close();
                out.close();
            }
            catch( IOException ioe )
            {
                Log.e( "FileUtil", "IOException in method 'copyFile': " + ioe.getMessage() );
                return false;
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
        Log.v( "FileUtil", "Loading native library '" + libname + "'" );
        try
        {
            System.loadLibrary( libname );
        }
        catch( UnsatisfiedLinkError e )
        {
            Log.e( "FileUtil", "Unable to load native library '" + libname + "'" );
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
            
            Log.v( "FileUtil", "Loading native library '" + filename + "'" );
            try
            {
                System.load( filename );
            }
            catch( UnsatisfiedLinkError e )
            {
                Log.e( "FileUtil", "Unable to load native library '" + filename + "'" );
            }
        }
    }
}
