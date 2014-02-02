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

import java.util.ArrayList;
import java.util.LinkedList;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatBlock;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatCode;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatOption;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatSection;
import paulscode.android.mupen64plusae.util.RomHeader;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class CheatUtils
{
    public static class Cheat
    {
        public String name;
        public String desc;
        public String code;
        public String option;
    }
    
    public static int numberOfSystemCheats = 0;
    
    public static ArrayList<Cheat> populate( String crc, CheatFile mupencheat_txt,
            boolean isSystemDefault, Context con )
    {
        ArrayList<Cheat> cheats = new ArrayList<Cheat>();
        CheatSection cheatSection = mupencheat_txt.match( "^" + crc.replace( ' ', '-' ) + ".*" );
        if( cheatSection == null )
        {
            Log.w( "CheatEditorActivity", "No cheat section found for '" + crc + "'" );
            return cheats;
        }
        if( isSystemDefault )
        {
            numberOfSystemCheats = cheatSection.size();
        }
        
        for( int i = 0; i < cheatSection.size(); i++ )
        {
            CheatBlock cheatBlock = cheatSection.get( i );
            if( cheatBlock != null )
            {
                Cheat cheat = new Cheat();
                
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
                    cheat.desc = con.getString( R.string.cheatNotes_none );
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
        return cheats;
    }
    
    public static void save( String crc, CheatFile mupencheat_txt, ArrayList<Cheat> cheats,
            RomHeader mRomHeader, Context con, boolean isSystemDefault )
    {
        CheatSection c = mupencheat_txt.match( "^" + crc.replace( ' ', '-' ) + ".*" );
        if( c == null )
        {
            // Game name and country code from header
            c = new CheatSection( crc.replace( ' ', '-' ), mRomHeader.name, String.format( "%02x",
                    mRomHeader.countryCode ).substring( 0, 2 ) );
            mupencheat_txt.add( c );
        }
        {
            c.clear();
            int start = 0;
            if( !isSystemDefault )
            {
                start = CheatUtils.numberOfSystemCheats;
            }
            for( int i = start; i < cheats.size(); i++ )
            {
                Cheat cheat = cheats.get( i );
                CheatBlock b = null;
                if( TextUtils.isEmpty( cheat.desc )
                        || cheat.desc.equals( con.getString( R.string.cheatNotes_none ) ) )
                {
                    b = new CheatBlock( cheat.name, null );
                }
                else
                {
                    b = new CheatBlock( cheat.name, cheat.desc );
                }
                LinkedList<CheatOption> ops = new LinkedList<CheatOption>();
                if( cheat.option != null )
                {
                    if( !TextUtils.isEmpty( cheat.option ) )
                    {
                        String[] tmp_ops = cheat.option.split( "\n" );
                        for( int o = 0; o < tmp_ops.length; o++ )
                        {
                            ops.add( new CheatOption( tmp_ops[o].substring( tmp_ops[o]
                                    .lastIndexOf( ' ' ) + 1 ), tmp_ops[o].substring( 0,
                                    tmp_ops[o].lastIndexOf( ' ' ) ) ) );
                        }
                    }
                }
                String[] tmp_lines = cheat.code.split( "\n" );
                if( tmp_lines.length > 0 )
                {
                    for( int o = 0; o < tmp_lines.length; o++ )
                    {
                        if( tmp_lines[o].indexOf( ' ' ) != -1 )
                        {
                            if( tmp_lines[o].contains( "?" ) )
                            {
                                b.add( new CheatCode( tmp_lines[o].substring( 0,
                                        tmp_lines[o].lastIndexOf( ' ' ) ), tmp_lines[o]
                                        .substring( tmp_lines[o].lastIndexOf( ' ' ) + 1 ), ops ) );
                            }
                            else
                            {
                                b.add( new CheatCode( tmp_lines[o].substring( 0,
                                        tmp_lines[o].lastIndexOf( ' ' ) ), tmp_lines[o]
                                        .substring( tmp_lines[o].lastIndexOf( ' ' ) + 1 ), null ) );
                            }
                        }
                    }
                }
                c.add( b );
            }
            mupencheat_txt.save();
        }
    }
    
    public static void reset()
    {
        numberOfSystemCheats = 0;
    }
}
