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
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import android.text.Html;
import android.util.Log;

/**
 * Utility class that provides methods which simplify file I/O tasks.
 */
public final class FileUtil
{
    public static void populate( File startPath, boolean includeParent, boolean includeDirectories,
            boolean includeFiles, List<CharSequence> outNames, List<String> outPaths )
    {
        if( !startPath.exists() )
            return;
        
        if( startPath.isFile() )
            startPath = startPath.getParentFile();
        
        if( startPath.getParentFile() == null )
            includeParent = false;
        
        outNames.clear();
        outPaths.clear();
        
        if( includeParent )
        {
            outNames.add( Html.fromHtml( "<b>..</b>" ) );
            outPaths.add( startPath.getParentFile().getPath() );
        }
        
        if( includeDirectories )
        {
            for( File directory : getContents( startPath, new VisibleDirectoryFilter() ) )
            {
                outNames.add( Html.fromHtml( "<b>" + directory.getName() + "</b>" ) );
                outPaths.add( directory.getPath() );
            }
        }
        
        if( includeFiles )
        {
            for( File file : getContents( startPath, new VisibleFileFilter() ) )
            {
                outNames.add( Html.fromHtml( file.getName() ) );
                outPaths.add( file.getPath() );
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
    
    /**
     * Deletes a given folder directory in the form of a {@link File}
     * 
     * @param folder The folder to delete.
     * 
     * @return True if the folder was deleted, false otherwise.
     */
    public static boolean deleteFolder( File folder )
    {
        if( folder.isDirectory() )
        {
            String[] children = folder.list();
            if( children != null )
            {
                for( String child : children )
                {
                    boolean success = deleteFolder( new File( folder, child ) );
                    if( !success )
                        return false;
                }
            }
        }
        
        return folder.delete();
    }
    
    /**
     * Copies a {@code src} {@link File} to a desired destination represented by a {@code dest}
     * {@link File}
     * <p>
     * This method assumes no backups will want to be made.
     * 
     * @param src  Source file.
     * @param dest Desired destination.
     * 
     * @return True if the copy succeeded, false otherwise.
     */
    public static boolean copyFile( File src, File dest )
    {
        return copyFile( src, dest, false );
    }
    
    /**
     * Copies a {@code src} {@link File} to a desired destination represented by a {@code dest}
     * {@link File}
     * <p>
     * This method supports the making of backups of the src File.
     * 
     * @param src         Source file
     * @param dest        Desired destination
     * @param makeBackups True if backups are wanted, false otherwise.
     * 
     * @return True if the copy succeeded, false otherwise.
     */
    public static boolean copyFile( File src, File dest, boolean makeBackups )
    {
        if( src == null )
        {
            Log.e( "FileUtil", "src null in method 'copyFile'" );
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
            
            dest.mkdirs();
            
            for( String file : files )
            {
                success = success && copyFile( new File( src, file ), new File( dest, file ), makeBackups );
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
            
            f.mkdirs();
            if( dest.exists() && makeBackups )
                backupFile( dest );
            
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
            catch( FileNotFoundException fnfe )
            {
                Log.e("FileUtil", "FileNotFoundException in method 'copyFile': " + fnfe.getMessage());
                return false;
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
     * Backs up a given {@link File}.
     * <p>
     * Backups are made in the form: 'filename + [number]', where number can be any number depending
     * on the amount of backups there are.
     * <p>
     * eg. if only the file "thisfile.ext" exists, its backup will be "thisfile.ext.bak1"
     * <p>
     * if "thisfile" and "thisfile.ext.bak1" exists, its backup will be "thisfile.ext.bak2" and so
     * on.
     * 
     * @param file The file to back up.
     */
    public static void backupFile( File file )
    {
        if( file.isDirectory() )
            return;
        
        // Get a unique name for the backup
        String backupName = file.getAbsolutePath() + ".bak";
        File backup = new File( backupName );
        for( int i = 1; backup.exists(); i++ )
            backup = new File( backupName + i );
        
        copyFile( file, backup );
    }
    
    /**
     * Writes a given string to a specified file.
     * 
     * @param file The file to write the string to.
     * @param text The string of text to write to the file.
     * 
     * @throws IOException If a writing error occurs.
     */
    public static void writeStringToFile( File file, String text ) throws IOException
    {
        FileWriter out = new FileWriter( file );
        out.write( text );
        out.close();
    }
    
    /**
     * Creates a string from the contents of a File.
     * 
     * @param file The File to read a string from.
     * 
     * @return The file contents as a string.
     * 
     * @throws IOException If a reading error occurs.
     */
    public static String readStringFromFile( File file ) throws IOException
    {
        // From http://stackoverflow.com/a/326440/254218
        FileInputStream stream = new FileInputStream( file );
        try
        {
            FileChannel chan = stream.getChannel();
            MappedByteBuffer buf = chan.map( FileChannel.MapMode.READ_ONLY, 0, chan.size() );
            return Charset.forName( "UTF-8" ).decode( buf ).toString();
        }
        finally
        {
            stream.close();
        }
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
        {
            Log.e( "unzipAll", "Zip file is null" );
            return false;
        }
        else if( !archive.exists() )
        {
            Log.e( "unzipAll", "Zip file '" + archive.getAbsolutePath() + "' does not exist" );
            return false;
        }
        else if( !archive.isFile() )
        {
            Log.e( "unzipAll", "Zip file '" + archive.getAbsolutePath() + "' is not a file" );
            return false;
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
                    File f = new File( outputDir + "/" + entry.toString() );
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
            Log.e( "unzipAll", "ZipException: ", ze );
            return false;
        }
        catch( IOException ioe )
        {
            Log.e( "unzipAll", "IOException: ", ioe );
            return false;
        }
        catch( Exception e )
        {
            Log.e( "unzipAll", "Exception: ", e );
            return false;
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
            Log.e( "unzipEntry", "Zip entry '" + entry.getName() + "' is not a file" );
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

    public static File extractRomFile( File destDir, ZipEntry zipEntry, InputStream inStream )
    {        
        // Read the first 4 bytes of the entry
        byte[] buffer = new byte[1024];
        try
        {
            if( inStream.read( buffer, 0, 4 ) != 4 )
                return null;
        }
        catch( IOException e )
        {
            Log.w( "FileUtil", e );
            return null;
        }
        
        // This entry appears to be a valid ROM, extract it
        Log.i( "FileUtil", "Found zip entry " + zipEntry.getName() );

        String entryName = new File( zipEntry.getName() ).getName();
        File extractedFile = new File( destDir, entryName );
        try
        {
            // Open the output stream (throws exceptions)
            OutputStream outStream = new FileOutputStream( extractedFile );
            try
            {
                // Buffer the stream
                outStream = new BufferedOutputStream( outStream );
                
                // Write the first four bytes we already peeked at (throws exceptions)
                outStream.write( buffer, 0, 4 );
                
                // Read/write the remainder of the zip entry (throws exceptions)
                int n;
                while( ( n = inStream.read( buffer ) ) >= 0 )
                {
                    outStream.write( buffer, 0, n );
                }
                return extractedFile;
            }
            catch( IOException e )
            {
                Log.w( "FileUtil", e );
                return null;
            }
            finally
            {
                // Flush output stream and guarantee no memory leaks
                outStream.close();
            }
        }
        catch( IOException e )
        {
            Log.w( "FileUtil", e );
            return null;
        }
    }
}
