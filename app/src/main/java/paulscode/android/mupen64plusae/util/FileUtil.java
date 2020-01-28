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

import androidx.annotation.NonNull;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
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
import java.util.zip.ZipInputStream;

import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import paulscode.android.mupen64plusae.persistent.AppData;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

/**
 * Utility class that provides methods which simplify file I/O tasks.
 */
@SuppressWarnings({"SameParameterValue", "UnusedReturnValue", "unused", "ConstantConditions", "WeakerAccess"})
public final class FileUtil
{
    public static void populate( @NonNull File startPath, boolean includeParent, boolean includeDirectories,
            boolean includeFiles, List<CharSequence> outNames, List<String> outPaths )
    {
        if( !startPath.exists() )
            return;
        
        if( startPath.isFile() )
            startPath = startPath.getParentFile();

        if( startPath.getParentFile() == null )
            includeParent = false;
        
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
            return pathname != null && pathname.isFile();
        }
    }
    
    private static class VisibleDirectoryFilter implements FileFilter
    {
        // Include only non-hidden directories not starting with '.'
        @Override
        public boolean accept( File pathname )
        {
            return pathname != null && pathname.isDirectory();
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
     * {@link File}. The source can be a directory
     *
     * @param src         Source file
     * @param dest        Desired destination
     * 
     * @return True if the copy succeeded, false otherwise.
     */
    public static boolean copyFile( File src, File dest )
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
        
        if( src.isDirectory())
        {
            boolean success = true;

            if (src.exists() && src.list() != null) {
                String[] files = src.list();

                FileUtil.makeDirs(dest.getPath());

                for( String file : files )
                {
                    success = success && copyFile( new File( src, file ), new File( dest, file ) );
                }
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

            try (FileChannel in = new FileInputStream(src).getChannel();
                 FileChannel out = new FileOutputStream(dest).getChannel()) {
                in.transferTo(0, in.size(), out);
            } catch (Exception e) {
                Log.e("copyFile", "Exception: " + e.getMessage());
            }

            return true;
        }
    }

    /**
     * Copies a {@code src} {@link DocumentFile} to a desired destination represented by a {@code dest}
     * {@link File}.
     *
     * @param context     Context to use to read the URI
     * @param src         Source file
     * @param dest        Desired destination
     *
     * @return True if the copy succeeded, false otherwise.
     */
    public static boolean copyFolder( Context context, DocumentFile src, File dest )
    {
        if(src == null)
        {
            Log.e( "copyFile", "src null" );
            return false;
        }

        if( dest == null )
        {
            Log.e( "copyFile", "dest null" );
            return false;
        }

        if (src.isDirectory()) {
            FileUtil.makeDirs(dest.getPath());

            DocumentFile[] files = src.listFiles();
            for (DocumentFile file : files) {
                File newDest = new File(dest.getAbsolutePath() + "/" + file.getName());
                copyFolder(context, file, newDest );
            }
        } else {
            try (ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(src.getUri(), "r");
                 FileChannel in = new FileInputStream(parcelFileDescriptor.getFileDescriptor()).getChannel();
                 FileChannel out = new FileOutputStream(dest).getChannel()) {

                long bytesTransferred = 0;

                while (bytesTransferred < in.size()) {
                    bytesTransferred += in.transferTo(bytesTransferred, in.size(), out);
                }

            } catch (Exception e) {
                Log.e("copyFile", "Exception: " + e.getMessage());
            }
        }

        return true;
    }

    /**
     * Copies any {@code src} {@link DocumentFile} to a desired destination represented by a {@code dest}
     * {@link File} that starts with the given string. The source can't be a directory.
     *
     * @param context     Context to use to read the URI
     * @param src         Source folder
     * @param dest        Desired destination
     * @param startsWith What the file must start with for it to be copied
     *
     * @return True if the copy succeeded, false otherwise.
     */
    public static boolean copyFilesThatStartWith( Context context, DocumentFile src, File dest, String startsWith )
    {
        if(src == null)
        {
            Log.e( "copyFile", "src null" );
            return false;
        }

        if( dest == null )
        {
            Log.e( "copyFile", "dest null" );
            return false;
        }

        DocumentFile[] files = src.listFiles();

        for (DocumentFile file : files) {

            File destFile = new File(dest.getAbsolutePath() + "/" + file.getName());
            if (!file.isDirectory() && file.getName().startsWith(startsWith)) {

                try (ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(file.getUri(), "r");
                     FileChannel in = new FileInputStream(parcelFileDescriptor.getFileDescriptor()).getChannel();
                     FileChannel out = new FileOutputStream(destFile).getChannel()){

                    long bytesTransferred = 0;

                    while (bytesTransferred < in.size()) {
                        bytesTransferred += in.transferTo(bytesTransferred, in.size(), out);
                    }

                } catch (IOException|java.lang.IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    /**
     * Copies a {@code src} {@link Uri} to a desired destination represented by a {@code dest}
     * {@link File}. The source can't be a directory.
     *
     * @param context     Context to use to read the URI
     * @param src         Source file
     * @param dest        Desired destination
     *
     * @return True if the copy succeeded, false otherwise.
     */
    public static boolean copySingleFile( Context context, Uri src, File dest )
    {
        if(src == null || TextUtils.isEmpty(src.toString()))
        {
            Log.e( "copySingleFile", "src null" );
            return false;
        }

        if( dest == null)
        {
            Log.e( "copySingleFile", "dest null" );
            return false;
        }

        File f = dest.getParentFile();
        if( f == null )
        {
            Log.e( "copySingleFile", "dest parent folder null" );
            return false;
        }

        FileUtil.makeDirs(f.getPath());

        ParcelFileDescriptor parcelFileDescriptor;

        try {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(src, "r");

            if (parcelFileDescriptor != null)
            {
                try (FileChannel in = new FileInputStream(parcelFileDescriptor.getFileDescriptor()).getChannel();
                     FileChannel out = new FileOutputStream(dest).getChannel()) {

                    long bytesTransferred = 0;

                    while (bytesTransferred < in.size()) {
                        bytesTransferred += in.transferTo(bytesTransferred, in.size(), out);
                    }

                } catch (Exception e) {
                    Log.e("copyFile", "Exception: " + e.getMessage());
                }

                parcelFileDescriptor.close();
            }

        } catch (IOException|java.lang.IllegalArgumentException e) {
            e.printStackTrace();
        }

        return true;
    }

    public static DocumentFile createFolderIfNotPresent(Context context, DocumentFile root, String folderName)
    {
        if(root == null || TextUtils.isEmpty(root.toString()) )
        {
            Log.e( "copyFile", "dest null" );
            return null;
        }

        if(TextUtils.isEmpty(folderName) )
        {
            Log.e( "copyFile", "dest null" );
            return null;
        }

        boolean success = true;

        DocumentFile newFolder = root.findFile(folderName);

        if (newFolder == null) {
            newFolder = root.createDirectory(folderName);
        }

        return newFolder;
    }

    /**
     * Copies a {@code src} {@link File} to a desired destination represented by a {@code dest}
     * {@link Uri}.
     *
     * @param context     Context to use to read the URI
     * @param src         Source file
     * @param dest        Desired destination
     *
     * @return True if the copy succeeded, false otherwise.
     */
    public static boolean copyFolder( Context context, File src, DocumentFile dest )
    {

        // Do this instead for directly accessible files
        if(dest.getUri().getScheme().equals("file")) {

            File destFile = new File(dest.getUri().getPath() + "/" + src.getName());
            return copyFile( src, destFile );
        }

        if( src == null )
        {
            Log.e( "copyFile", "src null" );
            return false;
        }

        if(dest == null )
        {
            Log.e( "copyFile", "dest null" );
            return false;
        }

        boolean success = true;


        if( src.isDirectory() )
        {
            DocumentFile childFile = createFolderIfNotPresent(context, dest, src.getName());

            File[] files = src.listFiles();

            for( File file : files ) {
                success = success && copyFolder( context, file, childFile );
            }

            return success;
        }
        else
        {
            DocumentFile targetFile = dest.findFile(src.getName());

            if(targetFile == null){
                targetFile = dest.createFile("", src.getName());
            }


            try (ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(targetFile.getUri(), "w");
                 FileChannel in = new FileInputStream(src).getChannel();
                 FileChannel out = new FileOutputStream(parcelFileDescriptor.getFileDescriptor()).getChannel()) {

                long bytesTransferred = 0;

                while (bytesTransferred < in.size()) {
                    bytesTransferred += in.transferTo(bytesTransferred, in.size(), out);
                }

            } catch (Exception e) {
                Log.e("copyFile", "Exception: " + e.getMessage());
            }
        }

        return true;
    }

    /**
     * Copies a {@code src} {@link File} to a desired destination represented by a {@code dest}
     * {@link Uri}.
     *
     * @param context     Context to use to read the URI
     * @param src         Source file
     * @param dest        Desired destination
     *
     * @return True if the copy succeeded, false otherwise.
     */
    public static boolean copyFilesThatStartWith( Context context, File src, DocumentFile dest, String startsWith )
    {
        if( src == null )
        {
            Log.e( "copyFile", "src null" );
            return false;
        }

        if(dest == null )
        {
            Log.e( "copyFile", "dest null" );
            return false;
        }

        File[] srcFiles = src.listFiles();

        if (srcFiles == null) {
            return false;
        }

        boolean success = true;

        for (File file : srcFiles) {
            if (!file.isDirectory() && file.getName().startsWith(startsWith)) {
                DocumentFile targetFile = dest.findFile(file.getName());

                if(targetFile == null){
                    targetFile = dest.createFile("", file.getName());
                }

                try (ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(targetFile.getUri(), "w");
                     FileChannel in = new FileInputStream(file).getChannel();
                     FileChannel out = new FileOutputStream(parcelFileDescriptor.getFileDescriptor()).getChannel()) {

                    long bytesTransferred = 0;

                    while (bytesTransferred < in.size()) {
                        bytesTransferred += in.transferTo(bytesTransferred, in.size(), out);
                    }

                } catch (Exception e) {
                    Log.e("copyFile", "Exception: " + e.getMessage());
                }
            }
        }

        return true;
    }

    /**
     * Copies a {@code src} {@link File} to a desired destination represented by a {@code dest}
     * {@link Uri}. The source can't be a directory.
     *
     * @param context     Context to use to read the URI
     * @param src         Source file
     * @param dest        Desired destination
     *
     * @return True if the copy succeeded, false otherwise.
     */
    public static boolean copyFile( Context context, File src, Uri dest )
    {
        if( src == null )
        {
            Log.e( "copyFile", "src null" );
            return false;
        }

        if(dest == null || TextUtils.isEmpty(dest.toString()) )
        {
            Log.e( "copyFile", "dest null" );
            return false;
        }

        ParcelFileDescriptor parcelFileDescriptor;

        try {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(dest, "rw");

            if (parcelFileDescriptor != null)
            {
                try (FileChannel in = new FileInputStream(src).getChannel();
                     FileChannel out = new FileOutputStream(parcelFileDescriptor.getFileDescriptor()).getChannel()) {

                    long bytesTransferred = 0;

                    while (bytesTransferred < in.size()) {
                        bytesTransferred += in.transferTo(bytesTransferred, in.size(), out);
                    }

                } catch (Exception e) {
                    Log.e("copyFile", "Exception: " + e.getMessage());
                }

                parcelFileDescriptor.close();
            }

        } catch (IOException|java.lang.IllegalArgumentException e) {
            e.printStackTrace();
        }

        return true;
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
     * @param context Context use to access URI
     * @param uri   The archive to extract.
     * @param outputDir Directory to place all of the extracted files.
     */
    public static void unzipAll( @NonNull Context context, @NonNull Uri uri, String outputDir )
    {
        ParcelFileDescriptor parcelFileDescriptor = null;
        ZipInputStream zipfile = null;

        try
        {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");

            if (parcelFileDescriptor != null)
            {
                zipfile = new ZipInputStream( new FileInputStream(parcelFileDescriptor.getFileDescriptor()) );

                ZipEntry entry = zipfile.getNextEntry();
                File outputFile = new File(outputDir);

                while( entry != null ) {
                    entry = zipfile.getNextEntry();
                    if (!entry.isDirectory())
                    {
                        // Check for malformed zip files
                        File securityTestFile = new File(outputDir, entry.getName());
                        String canonicalPath = securityTestFile.getCanonicalPath();
                        if (!canonicalPath.startsWith(outputFile.getCanonicalPath())) {
                            break;
                        }

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
        }
        catch( Exception ze )
        {
            Log.e( "unzipAll", "Exception: ", ze );
        }
        finally
        {
            try
            {
                if( zipfile != null )
                    zipfile.close();

                if (parcelFileDescriptor != null)
                    parcelFileDescriptor.close();
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
    private static void unzipEntry( ZipInputStream zipfile, ZipEntry entry, String outputDir )
            throws IOException
    {
        File outputFile = new File( outputDir, entry.getName() );
        
        BufferedInputStream inputStream = new BufferedInputStream( zipfile );
        BufferedOutputStream outputStream = new BufferedOutputStream( new FileOutputStream(
                outputFile ) );
        byte[] b = new byte[1024];
        int n;
        
        while( ( n = inputStream.read( b, 0, 1024 ) ) >= 0 )
        {
            outputStream.write( b, 0, n );
        }
        
        outputStream.close();
    }

    /**
     * Unzips a ZIP file in its entirety.
     *
     * @param fileUri   The archive to extract.
     * @param outputDir Directory to place all of the extracted files.
     */
    public static void unSevenZAll(@NonNull Context context, @NonNull Uri fileUri, String outputDir )
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        SevenZFile zipfile = null;
        ParcelFileDescriptor parcelFileDescriptor;
        try {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(fileUri, "r");

            if (parcelFileDescriptor != null) {
                FileInputStream fis = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                FileChannel fileChannel = fis.getChannel();

                zipfile = new SevenZFile(fileChannel);
                SevenZArchiveEntry zipEntry;

                while( (zipEntry = zipfile.getNextEntry()) != null)
                {
                    File f = new File( outputDir + "/" + zipEntry.getName() );

                    f = f.getParentFile();
                    if( f != null )
                    {
                        FileUtil.makeDirs(f.getPath());
                        unSevenZEntry( zipfile, zipEntry, outputDir );
                    }
                }
            }
        }
        catch( Exception ze )
        {
            Log.e( "unzipAll", "Exception: ", ze );
        }
        catch (java.lang.OutOfMemoryError e)
        {
            Log.w( "CacheRomInfoService", "Out of memory while extracting 7zip entry: " + fileUri.toString() );
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

    /**
     * Unzips a ZIP file in its entirety.
     *
     * @param archive   The archive to extract.
     * @param outputDir Directory to place all of the extracted files.
     */
    public static void unSevenZAll(@NonNull File archive, String outputDir )
    {
        SevenZFile zipfile = null;
        try
        {
            zipfile = new SevenZFile(archive);
            SevenZArchiveEntry zipEntry;

            while( (zipEntry = zipfile.getNextEntry()) != null)
            {
                File f = new File( outputDir + "/" + zipEntry.getName() );

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
        catch (java.lang.OutOfMemoryError e)
        {
            Log.w( "CacheRomInfoService", "Out of memory while extracting 7zip entry: " + archive );
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
                    RomHeader header = new RomHeader(extractedFile);

                    if( extractedFile != null && header.isValid)
                    {
                        zipStream.close();
                        return extractedFile.getPath();
                    }
                }
                catch( IOException|java.lang.IllegalArgumentException e )
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
                    RomHeader header = new RomHeader(extractedFile);

                    if( extractedFile != null && header.isValid)
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
        catch (java.lang.OutOfMemoryError e)
        {
            Log.w( "CacheRomInfoService", "Out of memory while extracting 7zip entry: " + zipPath );
        }

        return null;
    }

    public static byte[] extractRomHeader( InputStream inStream )
    {
        // Read the first 4 bytes of the entry
        int arraySize = 0xe8;
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

    /**
     * Needed since debug builds append '.debug' to the end of the package
     */
    private static String getFileProvider(Context context)
    {
        return context.getPackageName() + ".filesprovider";
    }

    private static final String LEANBACK_PACKAGE = "com.google.android.tvlauncher";

    /**
     * Leanback lanucher requires a uri for poster art so we create a contentUri and
     * pass that to LEANBACK_PACKAGE
     */
    public static Uri buildBanner(Context context, String coverArtPath)
    {
        Uri contentUri = null;

        try
        {
            File cover = new File(coverArtPath);
            if (cover.exists())
            {
                contentUri = FileProvider.getUriForFile(context, getFileProvider(context), cover);
            }
            else if ((cover = new File(coverArtPath)).exists())
            {
                contentUri = FileProvider.getUriForFile(context, getFileProvider(context), cover);
            }

            context.grantUriPermission(LEANBACK_PACKAGE, contentUri, FLAG_GRANT_READ_URI_PERMISSION);
        }
        catch (Exception e)
        {
            Log.e("FileUtil", "Failed to create banner");
            Log.e("FileUtil", e.getMessage());
        }

        return contentUri;
    }

    public static Uri resourceToUri(Context context, int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resID) + '/' +
                context.getResources().getResourceTypeName(resID) + '/' +
                context.getResources().getResourceEntryName(resID) );
    }

    public static boolean isFileImage(File file)
    {
        boolean isImage = false;
        if (file.exists())
        {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            isImage = options.outWidth != -1 && options.outHeight != -1;
        }

        return isImage;
    }

    public static DocumentFile getDocumentFileTree(Context context, Uri uri)
    {
        DocumentFile file;
        if (uri.getScheme() != null && uri.getScheme().equals("file")) {
            file = uri.getPath() != null ? DocumentFile.fromFile(new File(uri.getPath())) : null;
        } else {
            file = DocumentFile.fromTreeUri(context, uri);
        }

        return file;
    }

    public static DocumentFile getDocumentFileSingle(Context context, Uri uri)
    {
        DocumentFile file;
        if (uri.getScheme() != null && uri.getScheme().equals("file")) {
            file = uri.getPath() != null ? DocumentFile.fromFile(new File(uri.getPath())) : null;
        } else {
            file = DocumentFile.fromSingleUri(context, uri);
        }

        return file;
    }
}
