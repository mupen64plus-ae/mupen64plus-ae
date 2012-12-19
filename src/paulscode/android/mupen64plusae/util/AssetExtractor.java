/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 * 
 * References:
 * http://stackoverflow.com/questions/4447477/android-how-to-copy-files-in-assets-to-sdcard
 */
package paulscode.android.mupen64plusae.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.res.AssetManager;
import android.util.Log;

public class AssetExtractor
{
    public interface OnExtractionProgressListener
    {
        public void onExtractionProgress( String nextFileExtracted );
    }
    
    public static void extractAssets( AssetManager assetManager, String srcPath, String dstPath,
            OnExtractionProgressListener onProgress )
    {
        if( srcPath.startsWith( "/" ) )
            srcPath = srcPath.substring( 1 );
        
        String[] srcSubPaths = getAssetList( assetManager, srcPath );
        
        if( srcSubPaths.length > 0 )
        {
            // srcPath is a directory
            
            // Ensure the parent directories exist
            new File( dstPath ).mkdirs();
            
            // Recurse into each subdirectory
            for( String srcSubPath : srcSubPaths )
            {
                String suffix = "/" + srcSubPath;
                extractAssets( assetManager, srcPath + suffix, dstPath + suffix, onProgress );
            }
        }
        else
        {
            // srcPath is a file
            
            // Call the progress listener before extracting
            if( onProgress != null )
                onProgress.onExtractionProgress( dstPath );
            
            // Extract the file
            try
            {
                InputStream in = assetManager.open( srcPath );
                OutputStream out = new FileOutputStream( dstPath );
                byte[] buffer = new byte[1024];
                int read;

                while( ( read = in.read( buffer ) ) != -1 )
                {
                    out.write( buffer, 0, read );
                }

                in.close();
                out.flush();
                out.close();
            }
            catch( IOException e )
            {
                Log.w( "AssetExtractor", "Failed to extract asset file: " + srcPath );
            }
        }
    }
    
    public static int countAssets( AssetManager assetManager, String srcPath )
    {
        int count = 0;

        // TODO: This function takes a surprisingly long time to complete.
        if( srcPath.startsWith( "/" ) )
            srcPath = srcPath.substring( 1 );
        
        String[] srcSubPaths = getAssetList( assetManager, srcPath );
        if( srcSubPaths.length > 0 )
        {
            // srcPath is a directory
            for( String srcSubPath : srcSubPaths )
            {
                count += countAssets( assetManager, srcPath + "/" + srcSubPath );
            }
        }
        else
        {
            // srcPath is a file
            count++;
        }

        return count;
    }

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
}
