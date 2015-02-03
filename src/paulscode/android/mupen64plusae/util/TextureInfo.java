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
 */
package paulscode.android.mupen64plusae.util;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import android.text.TextUtils;
import android.util.Log;

/**
 * Utility class that encapsulates meta-info about a hi-res texture file.
 * 
 * @see mupen64plus-video-rice/src/TextureFilters.cpp
 * @see FindAllTexturesFromFolder(...)
 */
public class TextureInfo
{
    public static final int PIXEL_FORMAT_INVALID = -1;
    public static final int PIXEL_FORMAT_8BIT = 0;
    public static final int PIXEL_FORMAT_16BIT = 1;
    public static final int PIXEL_FORMAT_24BIT = 2;
    public static final int PIXEL_FORMAT_32BIT = 3;
    
    public static final int TEXTURE_FORMAT_INVALID = -1;
    public static final int TEXTURE_FORMAT_RGBA = 0;
    public static final int TEXTURE_FORMAT_YUV = 1;
    public static final int TEXTURE_FORMAT_CI = 2;
    public static final int TEXTURE_FORMAT_IA = 3;
    public static final int TEXTURE_FORMAT_I = 4;
    
    public static final int IMAGE_FORMAT_INVALID = -1;
    public static final int IMAGE_FORMAT_COLOR_INDEXED_BMP = 0;
    public static final int IMAGE_FORMAT_RGBA_PNG_FOR_CI = 1;
    public static final int IMAGE_FORMAT_RGBA_PNG_FOR_ALL_CI = 2;
    public static final int IMAGE_FORMAT_RGB_PNG = 3;
    public static final int IMAGE_FORMAT_RGB_WITH_ALPHA_TOGETHER_PNG = 4;
    
    public final String romName;
    public final String romCrc;
    public final String paletteCrc;
    public final int pixelFormat;
    public final int textureFormat;
    public final int imageFormat;
    
    private static final Pattern sPattern = Pattern
            .compile( "([^\\/]+)#([0-9a-fA-F]+)#([0-3])#([0-4])#?([^_]*)_"
                    + "(ci\\.bmp|ciByRGBA\\.png|allciByRGBA\\.png|rgb\\.png|all\\.png)" );
    
    public TextureInfo( String pathToImageFile )
    {
        Matcher m = sPattern.matcher( pathToImageFile );
        if( m.find() )
        {
            romName = m.group( 1 );
            romCrc = m.group( 2 );
            paletteCrc = TextUtils.isEmpty( m.group( 5 ) ) ? "FFFFFFFF" : m.group( 5 );
            pixelFormat = SafeMethods.toInt( m.group( 3 ), PIXEL_FORMAT_INVALID );
            textureFormat = SafeMethods.toInt( m.group( 4 ), TEXTURE_FORMAT_INVALID );
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
            romCrc = "";
            paletteCrc = "";
            pixelFormat = PIXEL_FORMAT_INVALID;
            textureFormat = TEXTURE_FORMAT_INVALID;
            imageFormat = IMAGE_FORMAT_INVALID;
        }
    }

    /**
     * Returns the name embedded in a zipped texture pack.
     * 
     * @param filename The path to the zipped texture pack.
     * @return The name, or null if there were any errors.
     */
    public static String getTexturePackName( String filename )
    {
        File archive = new File( filename );       
        if( !archive.exists() )
        {
            Log.e( "getTexturePackName", "Zip file '" + archive.getAbsolutePath() + "' does not exist" );
            return null;
        }
        else if( !archive.isFile() )
        {
            Log.e( "getTexturePackName", "Zip file '" + archive.getAbsolutePath() + "' is not a file" );
            return null;
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
                    TextureInfo info = new TextureInfo( entry.getName() );
                    if( info.imageFormat != IMAGE_FORMAT_INVALID )
                        return info.romName;
                }
            }
        }
        catch( ZipException ze )
        {
            Log.e( "getTexturePackName", "ZipException: ", ze );
            return null;
        }
        catch( IOException ioe )
        {
            Log.e( "getTexturePackName", "IOException: ", ioe );
            return null;
        }
        catch( Exception e )
        {
            Log.e( "getTexturePackName", "Exception: ", e );
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
        Log.e( "getTexturePackName", "No compatible textures found in .zip archive" );
        return null;
    }
}
