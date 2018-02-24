/*
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

import android.support.annotation.NonNull;
import android.util.Log;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

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
import java.util.zip.ZipFile;

import paulscode.android.mupen64plusae.persistent.AppData;

/**
 * Utility class that provides methods which simplify file I/O tasks.
 */
@SuppressWarnings({"SameParameterValue", "UnusedReturnValue", "unused"})
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
            outNames.add( AppData.fromHtml( "<b>..</b>" ) );
            outPaths.add( startPath.getParentFile().getPath() );
        }
        
        if( includeDirectories )
        {
            for( File directory : getContents( startPath, new VisibleDirectoryFilter() ) )
            {
                outNames.add( AppData.fromHtml( "<b>" + directory.getName() + "</b>" ) );
                outPaths.add( directory.getPath() );
            }
        }
        
        if( includeFiles )
        {
            for( File file : getContents( startPath, new VisibleFileFilter() ) )
            {
                outNames.add( AppData.fromHtml( file.getName() ) );
                outPaths.add( file.getPath() );
            }
        }
    }
    
    private static List<File> getContents( File startPath, FileFilter fileFilter )
    {
        // Get a filtered, sorted list of files
        List<File> results = new ArrayList<>();
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
     */
    public static void deleteFolder(File folder) {
        if (folder.exists() && folder.isDirectory() && folder.listFiles() != null)
            for (File child : folder.listFiles())
                deleteFolder(child);

        if (!folder.delete()) {
            Log.w("deleteFolder", "Couldn't delete " + folder.getPath());
        }
    }

    /**
     * Deletes a given folder directory in the form of a {@link File}
     *
     * @param folder The folder to delete.
     */
    public static void deleteFolderFilter(File folder, String filter) {
        if (folder.exists() && folder.isDirectory() && folder.listFiles() != null)
            for (File child : folder.listFiles())
                deleteFolderFilter(child,filter);

        if (folder.getName().contains(filter) && !folder.delete()) {
            Log.w("deleteFolderFilter", "Couldn't delete " + folder.getPath());
        }
    }

    /**
     * Deletes all files with the provided extension in a folder
     *
     * @param folder The folder to look for the provided extension.
     * @param extension The extension of files to be deleted
     */
    public static void deleteExtensionFolder(File folder, String extension) {
        if (folder.exists() && folder.isDirectory() && folder.listFiles() != null) {
            for (File child : folder.listFiles()) {
                if (child.getName().endsWith(extension)) {
                    if (!child.delete()) {
                        Log.w("deleteExtensionFolder", "Couldn't delete " + child.getPath());
                    }
                }
            }
        }
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
    private static boolean copyFile( File src, File dest, boolean makeBackups )
    {
        if( src == null )
        {
            Log.e( "copyFile", "src null" );
            return false;
        }
        
        if( dest == null )
        {
            Log.e( "copyFile", "dest null" );
            return false;
        }
        
        if( src.isDirectory() )
        {
            boolean success = true;
            String[] files = src.list();

            FileUtil.makeDirs(dest.getPath());
            
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
                Log.e( "copyFile", "dest parent folder null" );
                return false;
            }

            FileUtil.makeDirs(f.getPath());

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
                Log.e("copyFile", "FileNotFoundException: " + fnfe.getMessage());
                return false;
            }
            catch( IOException ioe )
            {
                Log.e( "copyFile", "IOException: " + ioe.getMessage() );
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
    private static void backupFile( File file )
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
        try (FileInputStream stream = new FileInputStream(file)) {
            FileChannel chan = stream.getChannel();
            MappedByteBuffer buf = chan.map(FileChannel.MapMode.READ_ONLY, 0, chan.size());
            return Charset.forName("UTF-8").decode(buf).toString();
        }
    }
    
    
    /**
     * Unzips a ZIP file in its entirety.
     *
     * @param archive   The archive to extract.
     * @param outputDir Directory to place all of the extracted files.
     */
    public static void unzipAll( @NonNull File archive, String outputDir )
    {
        if( !archive.exists() )
        {
            Log.e( "unzipAll", "Zip file '" + archive.getAbsolutePath() + "' does not exist" );
            return;
        }
        else if( !archive.isFile() )
        {
            Log.e( "unzipAll", "Zip file '" + archive.getAbsolutePath() + "' is not a file" );
            return;
        }
        
        ZipFile zipfile = null;
        try
        {
            zipfile = new ZipFile( archive );
            Enumeration<? extends ZipEntry> e = zipfile.entries();
            while( e.hasMoreElements() )
            {
                ZipEntry entry = e.nextElement();
                if (!entry.isDirectory())
                {
                    File f = new File( outputDir + "/" + entry.toString() );
                    f = f.getParentFile();
                    if( f != null )
                    {
                        FileUtil.makeDirs(f.getPath());
                        unzipEntry( zipfile, entry, outputDir );
                    }
                }
            }
        }
        catch( Exception ze )
        {
            Log.e( "unzipAll", "Exception: ", ze );
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
    }

    // Unzips a specific entry from a ZIP file into the given output directory.
    //
    // Returns the absolute path to the outputted entry.
    // Returns null if the entry passed in happens to be a directory.
    private static void unzipEntry( ZipFile zipfile, ZipEntry entry, String outputDir )
            throws IOException
    {
        if( entry.isDirectory() )
        {
            Log.e( "unzipEntry", "Zip entry '" + entry.getName() + "' is not a file" );
            return;
        }
        
        File outputFile = new File( outputDir, entry.getName() );
        
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
    }

    /**
     * Unzips a ZIP file in its entirety.
     *
     * @param archive   The archive to extract.
     * @param outputDir Directory to place all of the extracted files.
     */
    public static void unSevenZAll(@NonNull File archive, String outputDir )
    {
        if( !archive.exists() )
        {
            Log.e( "unSevenZAll", "Zip file '" + archive.getAbsolutePath() + "' does not exist" );
            return;
        }
        else if( !archive.isFile() )
        {
            Log.e( "unSevenZAll", "Zip file '" + archive.getAbsolutePath() + "' is not a file" );
            return;
        }

        SevenZFile zipfile = null;
        try
        {
            zipfile = new SevenZFile(archive);
            SevenZArchiveEntry zipEntry;

            while( (zipEntry = zipfile.getNextEntry()) != null)
            {
                File f = new File( outputDir + "/" + zipEntry.toString() );

                f = f.getParentFile();
                if( f != null )
                {
                    FileUtil.makeDirs(f.getPath());
                    unSevenZEntry( zipfile, zipEntry, outputDir );
                }
            }
        }
        catch( Exception ze )
        {
            Log.e( "unzipAll", "Exception: ", ze );
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
    }

    // Unzips a specific entry from a ZIP file into the given output directory.
    //
    // Returns the absolute path to the outputted entry.
    // Returns null if the entry passed in happens to be a directory.
    private static void unSevenZEntry( SevenZFile zipFile, SevenZArchiveEntry entry, String outputDir )
            throws IOException
    {
        if( entry.isDirectory() )
        {
            Log.e( "unzipEntry", "Zip entry '" + entry.getName() + "' is not a file" );
            return;
        }

        File outputFile = new File( outputDir, entry.getName() );
        final InputStream zipStream = new BufferedInputStream(new SevenZInputStream(zipFile));
        BufferedOutputStream outputStream = new BufferedOutputStream( new FileOutputStream(
                outputFile ) );
        byte[] b = new byte[1024];
        int n;

        while( ( n = zipStream.read( b, 0, 1024 ) ) >= 0 )
        {
            outputStream.write( b, 0, n );
        }

        outputStream.close();
        zipStream.close();
    }

    public static File extractRomFile( File destDir, String zipEntryName, InputStream inStream )
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
            Log.w( "extractRomFile", e );
            return null;
        }
        
        // This entry appears to be a valid ROM, extract it
        Log.i( "extractRomFile", "Found zip entry " + zipEntryName );
        makeDirs(destDir.getPath());
        String entryName = new File( zipEntryName ).getName();
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
                Log.w( "extractRomFile", e );
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
            Log.w( "extractRomFile", e );
            return null;
        }
    }

    public static String ExtractFirstROMFromZip(String zipPath, String unzippedRomDir)
    {
        try
        {
            ZipFile zipFile = new ZipFile( zipPath );
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while ( entries.hasMoreElements() ) {
                ZipEntry zipEntry = entries.nextElement();

                try
                {
                    InputStream zipStream = zipFile.getInputStream( zipEntry );
                    File extractedFile = FileUtil.extractRomFile( new File( unzippedRomDir ), zipEntry.getName(), zipStream );

                    if( extractedFile != null)
                    {
                        zipStream.close();
                        return extractedFile.getPath();
                    }
                }
                catch( IOException e )
                {
                    Log.w( "ExtractFirstROMFrom", e );
                }
            }
            zipFile.close();
        }
        catch( IOException|ArrayIndexOutOfBoundsException e )
        {
            Log.w( "ExtractFirstROMFrom", e );
        }

        return null;
    }

    public static String ExtractFirstROMFromSevenZ(String zipPath, String unzippedRomDir)
    {
        try
        {
            SevenZFile zipFile = new SevenZFile(new File(zipPath));
            SevenZArchiveEntry zipEntry;

            while ( (zipEntry = zipFile.getNextEntry()) != null ) {

                try
                {
                    final InputStream zipStream = new BufferedInputStream(new SevenZInputStream(zipFile));
                    File extractedFile = FileUtil.extractRomFile( new File( unzippedRomDir ), zipEntry.getName(), zipStream );

                    if( extractedFile != null)
                    {
                        zipFile.close();
                        return extractedFile.getPath();
                    }
                }
                catch( IOException e )
                {
                    Log.w( "ExtractFirstROM", e );
                }
            }
        }
        catch( IOException|ArrayIndexOutOfBoundsException e )
        {
            Log.w( "ExtractFirstROM", e );
        }

        return null;
    }

    public static byte[] extractRomHeader( InputStream inStream )
    {
        // Read the first 4 bytes of the entry
        int arraySize = 0x64;
        int initialReadSize = 4;
        byte[] buffer = new byte[arraySize];
        try
        {
            if( inStream.read( buffer, 0, initialReadSize) != initialReadSize )
                return null;
        }
        catch( IOException e )
        {
            Log.w( "extractRomHeader", e );
            return null;
        }

        try {
            if (inStream.read( buffer, initialReadSize, arraySize - initialReadSize) != arraySize - initialReadSize) {
                Log.w("extractRomHeader", "Unable to read ROM header");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer;
    }

    /*
     * Makes directory structure if it doesn't already exist
     * @param destDir Destination directory to create
     */
    public static void makeDirs( String destDir )
    {
        // Ensure the parent directories exist
        File destFile = new File( destDir );

        if(!destFile.exists())
        {
            if(!destFile.mkdirs()) {
                Log.w("makeDirs", "Unable to make dir " + destFile.getPath());
            }
        }
    }
}
