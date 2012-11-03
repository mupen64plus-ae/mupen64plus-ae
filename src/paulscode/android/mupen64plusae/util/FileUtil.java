package paulscode.android.mupen64plusae.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.text.Html;

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
}
