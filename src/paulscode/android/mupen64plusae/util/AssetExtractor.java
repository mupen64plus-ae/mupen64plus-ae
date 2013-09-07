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
 * Authors: littleguy77
 * 
 * References:
 * http://stackoverflow.com/questions/4447477/android-how-to-copy-files-in-assets-to-sdcard
 */
package paulscode.android.mupen64plusae.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.res.AssetManager;
import android.util.Log;

/**
 *  A class for extracting the emulator assets.
 */
public final class AssetExtractor
{
    public interface OnExtractionProgressListener
    {
        public void onExtractionProgress( String nextFileExtracted );
    }
    
    public enum FailureReason
    {
        FILE_UNWRITABLE,
        FILE_UNCLOSABLE,
        ASSET_UNCLOSABLE,
        ASSET_IO_EXCEPTION,
        FILE_IO_EXCEPTION,
    }
    
    public static final class ExtractionFailure
    {
        public final String srcPath;
        public final String dstPath;
        public final FailureReason reason;
        public ExtractionFailure( String srcPath, String dstPath, FailureReason reason )
        {
            this.srcPath = srcPath;
            this.dstPath = dstPath;
            this.reason = reason;
        }
        
        @Override
        public String toString()
        {
            switch( reason )
            {
                case FILE_UNWRITABLE:
                    return "Failed to open file " + dstPath;
                case FILE_UNCLOSABLE:
                    return "Failed to close file " + dstPath;
                case ASSET_UNCLOSABLE:
                    return "Failed to close asset " + srcPath;
                case ASSET_IO_EXCEPTION:
                    return "Failed to extract asset " + srcPath + " to file " + dstPath;
                case FILE_IO_EXCEPTION:
                    return "Failed to add file " + srcPath + " to file " + dstPath;
                default:
                    return "Failed using source " + srcPath + " and destination " + dstPath;
            }
        }
    }
    
    /**
     * Extracts all the assets from a source path to a given destination path.
     * 
     * @param assetManager A handle to the asset manager.
     * @param srcPath      The source path containing the assets.
     * @param dstPath      The destination path for the assets.
     * @param onProgress   A progress listener.
     * 
     * @return A list containing all extraction failures, if any.  Never null.
     */
    public static List<ExtractionFailure> extractAssets( AssetManager assetManager, String srcPath, String dstPath,
            OnExtractionProgressListener onProgress )
    {
        final List<ExtractionFailure> failures = new ArrayList<ExtractionFailure>();
        
        if( srcPath.startsWith( "/" ) )
            srcPath = srcPath.substring( 1 );
        
        String[] srcSubPaths = getAssetList( assetManager, srcPath );
        
        if( srcSubPaths.length > 0 )
        {
            // srcPath is a directory
            
            // Ensure the parent directories exist
            new File( dstPath ).mkdirs();
            
            // Some files are too big for Android 2.2 and below, so we break them into parts.
            // We use a simple naming scheme where we just append .part0, .part1, etc.
            Pattern pattern = Pattern.compile( "(.+)\\.part(\\d+)$" );
            HashMap<String, Integer> fileParts = new HashMap<String, Integer>();
            
            // Recurse into each subdirectory
            for( String srcSubPath : srcSubPaths )
            {
                Matcher matcher = pattern.matcher( srcSubPath );
                if( matcher.matches() )
                {
                    String name = matcher.group(1);
                    if( fileParts.containsKey( name ) )
                        fileParts.put( name, fileParts.get( name ) + 1 );
                    else
                        fileParts.put( name, 1 );
                }
                String suffix = "/" + srcSubPath;
                failures.addAll( extractAssets( assetManager, srcPath + suffix, dstPath + suffix, onProgress ) );
            }
            
            // Combine the large broken files, if any
            combineFileParts( fileParts, dstPath );
        }
        else // srcPath is a file.
        {
            // Call the progress listener before extracting
            if( onProgress != null )
                onProgress.onExtractionProgress( dstPath );
            
            // IO objects, initialize null to eliminate lint error
            OutputStream out = null;
            InputStream in = null;
            
            // Extract the file
            try
            {
                out = new FileOutputStream( dstPath );
                in = assetManager.open( srcPath );
                byte[] buffer = new byte[1024];
                int read;
                
                while( ( read = in.read( buffer ) ) != -1 )
                {
                    out.write( buffer, 0, read );
                }
                out.flush();
            }
            catch( FileNotFoundException e )
            {
                ExtractionFailure failure = new ExtractionFailure( srcPath, dstPath, FailureReason.FILE_UNWRITABLE ); 
                Log.e( "AssetExtractor", failure.toString() );
                failures.add( failure );
            }
            catch( IOException e )
            {
                ExtractionFailure failure = new ExtractionFailure( srcPath, dstPath, FailureReason.ASSET_IO_EXCEPTION ); 
                Log.e( "AssetExtractor", failure.toString() );
                failures.add( failure );
            }
            finally
            {
                if( out != null )
                {
                    try
                    {
                        out.close();
                    }
                    catch( IOException e )
                    {
                        ExtractionFailure failure = new ExtractionFailure( srcPath, dstPath, FailureReason.FILE_UNCLOSABLE ); 
                        Log.e( "AssetExtractor", failure.toString() );
                        failures.add( failure );
                    }
                }
                if( in != null )
                {
                    try
                    {
                        in.close();
                    }
                    catch( IOException e )
                    {
                        ExtractionFailure failure = new ExtractionFailure( srcPath, dstPath, FailureReason.ASSET_UNCLOSABLE ); 
                        Log.e( "AssetExtractor", failure.toString() );
                        failures.add( failure );
                    }
                }
            }
        }
        
        return failures;
    }
    
    /**
     * Gets a string array list of assets in a given source path.
     *  
     * @param assetManager A handle to the asset manager.
     * @param srcPath      The path containing the assets.
     * 
     * @return A list of all the assets in the source path.
     */
    private static String[] getAssetList( AssetManager assetManager, String srcPath )
    {
        String[] srcSubPaths = null;
        
        try
        {
            srcSubPaths = assetManager.list( srcPath );
        }
        catch( IOException e )
        {
            Log.w( "AssetExtractor", "Failed to get asset file list." );
        }
        
        return srcSubPaths;
    }

    private static List<ExtractionFailure> combineFileParts( Map<String, Integer> filePieces, String dstPath )
    {
        List<ExtractionFailure> failures = new ArrayList<ExtractionFailure>();
        for (String name : filePieces.keySet() )
        {
            String src = null;
            String dst = dstPath + "/" + name;
            OutputStream out = null;
            InputStream in = null;
            try
            {
                out = new FileOutputStream( dst );
                byte[] buffer = new byte[1024];
                int read;
                for( int i = 0; i < filePieces.get( name ); i++ )
                {
                    src = dst + ".part" + i;
                    try
                    {
                        in = new FileInputStream( src );
                        while( ( read = in.read( buffer ) ) != -1 )
                        {
                            out.write( buffer, 0, read );
                        }
                        out.flush();
                    }
                    catch( IOException e )
                    {
                        ExtractionFailure failure = new ExtractionFailure( src, dst, FailureReason.FILE_IO_EXCEPTION ); 
                        Log.e( "AssetExtractor", failure.toString() );
                        failures.add( failure );
                    }
                    finally
                    {
                        if( in != null )
                        {
                            try
                            {
                                in.close();
                                new File( src ).delete();
                            }
                            catch( IOException e )
                            {
                                ExtractionFailure failure = new ExtractionFailure( src, dst, FailureReason.FILE_UNCLOSABLE ); 
                                Log.e( "AssetExtractor", failure.toString() );
                                failures.add( failure );
                            }
                        }
                    }
                }
            }
            catch( FileNotFoundException e )
            {
                ExtractionFailure failure = new ExtractionFailure( src, dst, FailureReason.FILE_UNWRITABLE ); 
                Log.e( "AssetExtractor", failure.toString() );
                failures.add( failure );
            }
            finally
            {
                if( out != null )
                {
                    try
                    {
                        out.close();
                    }
                    catch( IOException e )
                    {
                        ExtractionFailure failure = new ExtractionFailure( src, dst, FailureReason.FILE_UNCLOSABLE ); 
                        Log.e( "AssetExtractor", failure.toString() );
                        failures.add( failure );
                    }
                }
            }
        }
        return failures;
    }
}
