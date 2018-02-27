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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.util;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

/**
 * Utility class that encapsulates meta-info about a hi-res texture file.
 *
 */
@SuppressWarnings("unused")
public class TextureInfo
{
    private static final int PIXEL_FORMAT_INVALID = -1;
    private static final int PIXEL_FORMAT_8BIT = 0;
    private static final int PIXEL_FORMAT_16BIT = 1;
    private static final int PIXEL_FORMAT_24BIT = 2;
    private static final int PIXEL_FORMAT_32BIT = 3;
    
    private static final int TEXTURE_FORMAT_INVALID = -1;
    private static final int TEXTURE_FORMAT_RGBA = 0;
    private static final int TEXTURE_FORMAT_YUV = 1;
    private static final int TEXTURE_FORMAT_CI = 2;
    private static final int TEXTURE_FORMAT_IA = 3;
    private static final int TEXTURE_FORMAT_I = 4;
    
    private static final int IMAGE_FORMAT_INVALID = -1;
    private static final int IMAGE_FORMAT_COLOR_INDEXED_BMP = 0;
    private static final int IMAGE_FORMAT_RGBA_PNG_FOR_CI = 1;
    private static final int IMAGE_FORMAT_RGBA_PNG_FOR_ALL_CI = 2;
    private static final int IMAGE_FORMAT_RGB_PNG = 3;
    private static final int IMAGE_FORMAT_RGB_WITH_ALPHA_TOGETHER_PNG = 4;
    
    private final String romName;
    private final int imageFormat;
    
    private static final Pattern sPattern = Pattern
            .compile( "([^/]+)#([0-9a-fA-F]+)#([0-3])#([0-4])#?([^_]*)_"
                    + "(ci\\.bmp|ciByRGBA\\.png|allciByRGBA\\.png|rgb\\.png|all\\.png)" );

    private TextureInfo( String pathToImageFile )
    {
        Matcher m = sPattern.matcher( pathToImageFile );
        if( m.find() )
        {
            romName = m.group( 1 );
            String romCrc = m.group( 2 );
            String paletteCrc = TextUtils.isEmpty( m.group( 5 ) ) ? "FFFFFFFF" : m.group( 5 );
            int pixelFormat = SafeMethods.toInt( m.group( 3 ), PIXEL_FORMAT_INVALID );
            int textureFormat = SafeMethods.toInt( m.group( 4 ), TEXTURE_FORMAT_INVALID );
            String suffix = m.group( 6 );
            
            if( "ci.bmp".equals( suffix ) )
                imageFormat = IMAGE_FORMAT_COLOR_INDEXED_BMP;
            
            else if( "ciByRBGA.png".equals( suffix ) )
                imageFormat = IMAGE_FORMAT_RGBA_PNG_FOR_CI;
            
            else if( "allciByRGBA.png".equals( suffix ) )
                imageFormat = IMAGE_FORMAT_RGBA_PNG_FOR_ALL_CI;
            
            else if( "rgb.png".equals( suffix ) )
                imageFormat = IMAGE_FORMAT_RGB_PNG;
            
            else if( "all.png".equals( suffix ) )
                imageFormat = IMAGE_FORMAT_RGB_WITH_ALPHA_TOGETHER_PNG;
            
            else
                imageFormat = IMAGE_FORMAT_INVALID;
        }
        else
        {
            romName = "";
            imageFormat = IMAGE_FORMAT_INVALID;
        }
    }

    /**
     * Returns the name embedded in a zipped texture pack.
     * 
     * @param filename The path to the zipped texture pack.
     * @return The name, or null if there were any errors.
     */
    public static String getTexturePackNameFromZip(String filename )
    {
        File archive = new File( filename );
        
        ZipFile zipfile = null;
        Map<String, Integer> romHeaderCount = new HashMap<>();

        try
        {
            zipfile = new ZipFile( archive );
            Enumeration<? extends ZipEntry> e = zipfile.entries();
            while( e.hasMoreElements() )
            {
                ZipEntry entry = e.nextElement();
                if( entry != null && !entry.isDirectory() )
                {
                    TextureInfo info = new TextureInfo( entry.getName() );

                    if( info.imageFormat != IMAGE_FORMAT_INVALID ) {
                        if (romHeaderCount.containsKey(info.romName)) {
                            romHeaderCount.put(info.romName, romHeaderCount.get(info.romName)+1);
                        } else {
                            romHeaderCount.put(info.romName, 1);
                        }

                        //Find the first ROM header that shows 10 times
                        for (Map.Entry<String, Integer> mapEntry : romHeaderCount.entrySet()) {
                            if (mapEntry.getValue() == 10) {
                                return mapEntry.getKey();
                            }
                        }
                    }
                }
            }
        }
        catch( Exception ze )
        {
            Log.e( "TextureInfo", "ZipException: ", ze );
            return null;
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
        Log.e( "TextureInfo", "No compatible textures found in .zip archive" );
        return null;
    }

    /**
     * Returns the name embedded in a 7zipped texture pack.
     *
     * @param filename The path to the zipped texture pack.
     * @return The name, or null if there were any errors.
     */
    public static String getTexturePackNameFromSevenZ(String filename )
    {
        SevenZFile zipfile = null;
        Map<String, Integer> romHeaderCount = new HashMap<>();
        try
        {
            zipfile = new SevenZFile(new File(filename));
            SevenZArchiveEntry zipEntry;

            while( (zipEntry = zipfile.getNextEntry()) != null)
            {
                if( !zipEntry.isDirectory() )
                {
                    TextureInfo info = new TextureInfo( zipEntry.getName() );
                    if( info.imageFormat != IMAGE_FORMAT_INVALID ) {
                        if (romHeaderCount.containsKey(info.romName)) {
                            romHeaderCount.put(info.romName, romHeaderCount.get(info.romName)+1);
                        } else {
                            romHeaderCount.put(info.romName, 1);
                        }

                        //Find the first ROM header that shows 10 times
                        for (Map.Entry<String, Integer> entry : romHeaderCount.entrySet()) {
                            if (entry.getValue() == 10) {
                                return entry.getKey();
                            }
                        }
                    }
                }
            }
        }
        catch( Exception ze )
        {
            Log.e( "TextureInfo", "Exception: ", ze );
        }
        catch (java.lang.OutOfMemoryError e)
        {
            Log.w( "CacheRomInfoService", "Out of memory while extracting 7zip entry: " + filename );
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


        Log.e( "TextureInfo", "No compatible textures found in .zip archive" );
        return null;
    }
}
