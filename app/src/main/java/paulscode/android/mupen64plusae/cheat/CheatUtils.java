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
 * Authors: xperia64
 */
package paulscode.android.mupen64plusae.cheat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;

import org.mupen64plusae.v3.fzurita.R;

import paulscode.android.mupen64plusae.cheat.CheatFile.CheatBlock;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatCode;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatOption;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatSection;
import paulscode.android.mupen64plusae.util.FileUtil;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class CheatUtils
{
    public static class Cheat implements Comparable<Cheat>
    {
        public String name;
        public String desc;
        public String code;
        public String option;
        public int cheatIndex;
        
        
        @Override
        public int compareTo(Cheat another)
        {
            String lowerCaseName = this.name.toLowerCase(Locale.getDefault());
            String lowercaseOtherName = another.name.toLowerCase(Locale.getDefault());
            return lowerCaseName.compareTo(lowercaseOtherName);
        }
    }
    
    public static void mergeCheatFiles( String defaultpath, String userpath, String volatilepath )
    {        
        // Reset the volatile cheatfile to the default data
        File cheat_volatile = new File( volatilepath );
        File cheat_default = new File( defaultpath );
        FileUtil.copyFile( cheat_default, cheat_volatile );
        
        // Merge user cheats if they exist
        File cheat_user = new File( userpath );
        if( cheat_user.exists() )
        {
            CheatFile cheat_v = new CheatFile( volatilepath, true );
            CheatFile cheat_u = new CheatFile( userpath, true );
            
            for( String key : cheat_u.keySet() )
            {
                if( !CheatFile.NO_KEY.equals( key ) )
                {
                    CheatSection cheat_section_v = cheat_v.get( key );
                    CheatSection cheat_section_u = cheat_u.get( key );
                    assert( cheat_section_u != null );
                    
                    // Create the cheat section in the destination if necessary (i.e. this ROM
                    // is not present in the default cheat file)
                    if( cheat_section_v == null )
                    {
                        String name = cheat_section_u.goodName;
                        String crc = key.substring( 0, 17 );
                        String country = key.substring( 20 );
                        cheat_section_v = new CheatSection( crc, name, country );
                        cheat_v.add( cheat_section_v );
                    }
                    
                    // Append the user cheats to the volatile cheatfile
                    for( int o = 0; o < cheat_section_u.size(); o++ )
                    {
                        cheat_section_v.add( cheat_section_u.get( o ) );
                    }
                }
            }
            cheat_v.save();
        }
    }
    
    public static BufferedReader getCheatsLocation(String regularExpression, String filename)
    {
        // Make sure a file was specified in the constructor
        if( TextUtils.isEmpty( filename ) )
        {
            Log.e( "CheatFile", "Filename not specified in method reload()" );
            return null;
        }
        
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader( new FileReader( filename ) );
            boolean done = false;
            
            String fullLine = null;
            while( !done && ( fullLine = reader.readLine() ) != null)
            {
                if( fullLine.startsWith( "crc " ) )
                {
                    // Start of the next cheat section, return
                    done = fullLine.substring( 4 ).matches( regularExpression );
                }
            }
            
            if(fullLine != null)
            {
                Log.i("CheatUtils", fullLine);
            }
        }
        catch( FileNotFoundException e )
        {
            Log.e( "CheatFile", "Could not open " + filename );
            return null;
        }
        catch( IOException e )
        {
            Log.e( "CheatFile", "Could not read " + filename );
            return null;
        }
        return reader;
    }
    

    public static ArrayList<Cheat> populateWithPosition( BufferedReader startPosition,
        String crc, byte countryCode, Context con )
    {
        CheatSection cheatSection;
        try
        {
            String countryString = String.format("%02x", countryCode).substring(0, 2);
            String key = crc + "-C:" + countryString;
            
            cheatSection = new CheatSection( key, startPosition );
        }
        catch (IOException e)
        {
            cheatSection = null;
        }
        
        try
        {
            startPosition.close();
        }
        catch( IOException ignored )
        {
        }
        
        return populateCommon(cheatSection, crc, con);
    }
    
    public static ArrayList<Cheat> populate( String crc, byte countryCode, CheatFile mupencheat_txt,
            boolean isSystemDefault, Context con )
    {
        String countryString = String.format("%02x", countryCode).substring(0, 2);
        CheatSection cheatSection = mupencheat_txt.match( "^" + crc.replace( ' ', '-') + "-C:" + countryString + ".*");
        
        return populateCommon(cheatSection, crc, con);
    }
    
    private static ArrayList<Cheat> populateCommon(CheatSection cheatSection, String crc, Context con)
    {
        ArrayList<Cheat> cheats = new ArrayList<Cheat>();
        
        if( cheatSection == null )
        {
            Log.w( "CheatEditorActivity", "No cheat section found for '" + crc + "'" );
            return cheats;
        }
        
        for( int i = 0; i < cheatSection.size(); i++ )
        {
            CheatBlock cheatBlock = cheatSection.get( i );
            if( cheatBlock != null )
            {
                Cheat cheat = new Cheat();
                
                cheat.cheatIndex = i;
                
                // Get the short title of the cheat (shown in the menu)
                if( cheatBlock.name == null )
                {
                    // Title not available, just use a default string for the menu
                    cheat.name = con.getString( R.string.cheats_defaultName, i );
                }
                else
                {
                    // Title available, remove the leading/trailing quotation marks
                    cheat.name = cheatBlock.name;
                }
                
                // Get the descriptive note for this cheat (shown on long-click)
                if( cheatBlock.description == null )
                {
                    cheat.desc = "";
                }
                else
                {
                    cheat.desc = cheatBlock.description;
                }
                
                // Get the options for this cheat
                LinkedList<CheatCode> codes = new LinkedList<CheatCode>();
                LinkedList<CheatOption> options = new LinkedList<CheatOption>();
                for( int o = 0; o < cheatBlock.size(); o++ )
                {
                    codes.add( cheatBlock.get( o ) );
                }
                for( int o = 0; o < codes.size(); o++ )
                {
                    if( codes.get( o ).options != null )
                    {
                        options = codes.get( o ).options;
                    }
                }
                String codesAsString = "";
                if( codes != null && !codes.isEmpty() )
                {
                    for( int o = 0; o < codes.size(); o++ )
                    {
                        String y = "";
                        if( o != codes.size() - 1 )
                        {
                            y = "\n";
                        }
                        codesAsString += codes.get( o ).address + " " + codes.get( o ).code + y;
                    }
                }
                cheat.code = codesAsString;
                String optionsAsString = "";
                if( options != null && !options.isEmpty() )
                {
                    for( int o = 0; o < options.size(); o++ )
                    {
                        String y = "";
                        if( o != options.size() - 1 )
                        {
                            y = "\n";
                        }
                        optionsAsString += options.get( o ).name + " " + options.get( o ).code + y;
                    }
                }
                cheat.option = optionsAsString;
                String[] optionStrings = null;
                if( options != null )
                {
                    if( !options.isEmpty() )
                    {
                        // This is a multi-choice cheat
                        
                        optionStrings = new String[options.size()];
                        
                        // Each element is a key-value pair
                        for( int z = 0; z < options.size(); z++ )
                        {
                            // The first non-leading space character is the pair delimiter
                            optionStrings[z] = options.get( z ).name;
                            if( TextUtils.isEmpty( optionStrings[z] ) )
                                optionStrings[z] = con.getString( R.string.cheats_longPress );
                            
                        }
                    }
                }
                
                cheats.add( cheat );
            }
        }
        
        Collections.sort(cheats);
        return cheats;
    }
    
    public static void save(String crc, CheatFile mupencheat_txt, ArrayList<Cheat> cheats, String headerName,
        byte countryCode, Context con, boolean isSystemDefault)
    {
        String countryString = String.format("%02x", countryCode).substring(0, 2);
        CheatSection c = mupencheat_txt.match("^" + crc.replace(' ', '-') + "-C:" + countryString + ".*");
        if (c == null)
        {
            // Game name and country code from header
            c = new CheatSection(crc.replace(' ', '-'), headerName, countryString);
            mupencheat_txt.add(c);
        }

        c.clear();
        int start = 0;
        for (int i = start; i < cheats.size(); i++)
        {
            Cheat cheat = cheats.get(i);
            CheatBlock b = null;
            if (TextUtils.isEmpty(cheat.desc) || cheat.desc.equals(con.getString(R.string.cheatNotes_none)))
            {
                b = new CheatBlock(cheat.name, null);
            }
            else
            {
                b = new CheatBlock(cheat.name, cheat.desc);
            }
            LinkedList<CheatOption> ops = new LinkedList<CheatOption>();
            if (cheat.option != null)
            {
                if (!TextUtils.isEmpty(cheat.option))
                {
                    String[] tmp_ops = cheat.option.split("\n");
                    for (int o = 0; o < tmp_ops.length; o++)
                    {
                        ops.add(new CheatOption(tmp_ops[o].substring(tmp_ops[o].lastIndexOf(' ') + 1), tmp_ops[o]
                            .substring(0, tmp_ops[o].lastIndexOf(' '))));
                    }
                }
            }
            String[] tmp_lines = cheat.code.split("\n");
            if (tmp_lines.length > 0)
            {
                for (int o = 0; o < tmp_lines.length; o++)
                {
                    if (tmp_lines[o].indexOf(' ') != -1)
                    {
                        if (tmp_lines[o].contains("?"))
                        {
                            b.add(new CheatCode(tmp_lines[o].substring(0, tmp_lines[o].lastIndexOf(' ')), tmp_lines[o]
                                .substring(tmp_lines[o].lastIndexOf(' ') + 1), ops));
                        }
                        else
                        {
                            b.add(new CheatCode(tmp_lines[o].substring(0, tmp_lines[o].lastIndexOf(' ')), tmp_lines[o]
                                .substring(tmp_lines[o].lastIndexOf(' ') + 1), null));
                        }
                    }
                }
            }
            c.add(b);
        }
        mupencheat_txt.save();
    }
}
