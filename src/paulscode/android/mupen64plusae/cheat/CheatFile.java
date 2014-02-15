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
package paulscode.android.mupen64plusae.cheat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;
import android.util.Log;

/**
 * The CheatFile class is used to read and write in the unique syntax of mupencheat.txt
 * (Replaces previous process of using the ConfigFile class to read from mupen64plus.cht)
 * </p>
 * The file must follow a specific syntax:
 * </p>
 * <li>There is a space between each parameter and value.  Value is not quoted and may contain spaces (ex: cn Epic Win).  EXCEPTION: cheat options.
 * <li>ROM sections are not indented, beginning with "crc" followed by the CRC (ex: crc 01A23456-789012B3-C:4A).  
 * <li>The crc line is immediately followed by the ROM "good name" line.
 * <li>ROM "good name" lines are not indented, beginning with "gn" followed by the name (ex: gn Cool Game (U) (V1.0)). 
 * <li>Code names are indented one space.  They begin with "cn" followed by the code name (ex: cn Kill everything).
 * <li>Code description lines are optional.  When present, they immediately follow the code name line.
 * <li>Code description lines are indented two spaces, and begin with "cd" followed by the description. (ex: cd This cheat is for wimps).
 * <li>Code lines immediately follow the code description line (or code name line if description not present).
 * <li>Code lines are indented two spaces, and begin with a memory address followed by a value. (ex: 12345678 9012).
 * <li>Codes that contain options use four question marks for the value, followed by the cheat options (ex: 12345678 ???? 9012:"Do this",ABFE:"Do that").
 * <li>Cheat options are comma-separated.  They consist of the value, a colon, then the option string in quotes (ex: 12345678 ???? 9012:"Do this",ABFE:"Do that").
 * <li>There is a blank line between ROM sections.
 * <li>Leading and trailing whitespace in lines, param names, and values is discarded.
 * <li>Whitespace between words in a value or inside double-quoted cheat option strings is not discarded.
 *
 * @author Paul Lamb
 * 
 * http://www.paulscode.com
 * 
 */
public class CheatFile
{
    /** The name we use for the untitled section (preamble) of the cheat file. */
    public static final String NO_CRC = "[<sectionless!>]";
    
    private String mFilename;  // Name of the cheat file.
    private final HashMap<String, CheatSection> mCheatMap; // Sections mapped by CRC for easy lookup
    private final LinkedList<CheatSection> mCheatList;     // Sections in the proper order for easy saving

    /**
     * Constructor: Reads the entire cheat file, and saves the data in 'mCheatMap'.
     * 
     * @param filename The cheat file to read from.
     */
    public CheatFile( String filename )
    {
        this.mFilename = filename;
        mCheatMap = new HashMap<String, CheatFile.CheatSection>();
        mCheatList = new LinkedList<CheatFile.CheatSection>();
        load( filename );
    }

    /**
     * Looks up a cheat section that matches the specified regex (not necessarily the only match).
     * 
     * @param regex A regular expression to match a section title from.
     * 
     * @return CheatSection containing parameters for a cheat section that matches, or null if no matches were found.
     */
    public CheatSection match( String regex )
    {
        String crc;
        Set<String> keys = mCheatMap.keySet();

        for (String key : keys)
        {
            crc = key;
            if (crc.matches(regex))
                return mCheatMap.get(crc);
        }
        
        return null;
    }
    public void add( CheatSection cheatSection )
    {
    	mCheatMap.put(cheatSection.crc, cheatSection);
    	mCheatList.add(cheatSection);
    }
    /**
     * Looks up a cheat section by CRC.
     * 
     * @param crc ROM CRC.
     * 
     * @return A CheatSection containing cheat codes, or null if not found.
     */
    public CheatSection get( String crc )
    {
        return mCheatMap.get( crc );
    }

    /**
     * Erases any previously loaded data.
     */
    public void clear()
    {
        mCheatMap.clear();
        mCheatList.clear();  // Ready to start fresh
    }

    /**
     * Reads the entire cheat file, and saves the data in 'mCheatMap' and 'mCheatList'.
     * 
     * @param filename The cheat file to read from.
     * 
     * @return True if successful.
     */
    public boolean load( String filename )
    {   
        // Make sure a file was actually specified
        if( TextUtils.isEmpty( filename ) )
            return false;
        
        // Free any previously loaded data
        clear();
        
        FileInputStream fstream;
        try
        {
            fstream = new FileInputStream( filename );
        }
        catch( FileNotFoundException fnfe )
        {
            // File not found... we can't continue
            return false;
        }
        
        DataInputStream in = new DataInputStream( fstream );
        BufferedReader br = new BufferedReader( new InputStreamReader( in ) );

        String crc = NO_CRC;
        CheatSection section = new CheatSection( crc, br ); // Read the 'sectionless' section
        mCheatMap.put( crc, section ); // Save the data to 'mCheatMap'
        mCheatList.add( section ); // Add it to the list as well
        
        // Loop through reading the remaining sections
        while( !TextUtils.isEmpty( section.nextCrc ) )
        {
            // Get the next section name
            crc = section.nextCrc;
 
            // Load the next section
            section = new CheatSection( crc, br );
            mCheatMap.put( crc, section );  // Save the data to 'mCheatMap'
            mCheatList.add( section );  // Add it to the list as well  
        }
        
        try
        {
            // Finished. Close the file.
            in.close();
            br.close();
        }
        catch( IOException ioe )
        {
            // (Don't care)
        }
        
        // Success
        return true;
    }
    
    /**
     * Saves the data from 'mCheatList' back to the cheat file.
     * 
     * @return True if successful. False otherwise.
     */
    public boolean save()
    {
        // No filename was specified.
        if( TextUtils.isEmpty( mFilename ) )
        {
            Log.e( "CheatFile", "Filename not specified in method save()" );
            return false;   // Quit
        }
        
        File f = new File( mFilename );
        
        // Delete it if it already exists.
        if( f.exists() )
        {
            // Some problem deleting the file.
            if( !f.delete() )
            {
                Log.e( "CheatFile", "Error deleting file " + mFilename );
                return false;   // Quit
            }
        }
        
        try
        {
            FileWriter fw = new FileWriter( mFilename );  // For writing to the cheat file
            CheatLine line;
            // Loop through the sections
            for ( CheatSection section : mCheatList )
            {
                if( section != null )
                {
                    section.save( fw );
                    if( !TextUtils.isEmpty( section.crc ) && !section.crc.equals( NO_CRC ) )
                    {
                        // Insert blank line between sections
                        line = new CheatLine( CheatLine.LINE_GARBAGE, "\n", null );
                        line.save( fw );
                        // TODO: This saves an extra blank line at the end of the file.
                        // Make sure that doesn't cause problems during load or in the core.
                    }
                }
            }

            fw.flush();
            fw.close();
        }
        catch( IOException ioe )
        {
            Log.e( "CheatFile", "IOException creating file " + mFilename + ", error message: " + ioe.getMessage() );
            return false;  // Some problem creating the file.. quit
        }
        
        // Success
        return true;
    }
    
    /**
     * Returns a handle to the mCheatMap keyset.
     * 
     * @return keyset containing all the cheat section CRCs.
     */
    public Set<String> keySet()
    {
         return mCheatMap.keySet();
    }

    /**
     * The CheatOption class encapsulates a code and a description.
     */
    public static class CheatOption
    {
        public String code;
        public String name;
        
        /**
         * Constructor: associate the code and description
         * 
         * @param code Code for the option.
         * @param name Human-readable name for the option.
         */
        public CheatOption( String code, String name )
        {
            this.code = code;
            this.name = name;
        }
    }
    
    /**
     * The CheatCode class encapsulates a memory address and code or code options.
     */
    public static class CheatCode
    {
        public String address;
        public String code;
        public LinkedList<CheatOption> options;
        
        /**
         * Constructor: associate the name, description, and codes
         * 
         * @param address The memory address.
         * @param code The code (or "????" if there are options).
         * @param options The cheat options (or null if none).
         */
        public CheatCode( String address, String code, LinkedList<CheatOption> options )
        {
            this.address = address;
            this.code = code;
            this.options = options;
        }
        
        /**
         * Saves the CheatCode.
         * 
         * @param fw The file to save the CheatCode to.
         * 
         * @throws IOException If a writing error occurs.
         */
        private void save( FileWriter fw ) throws IOException
        {
            String line =  "  " + address + " " + code;
            if( options != null )
            {
                boolean firstOption = true;
                for( CheatOption option : options )
                {
                    if( firstOption )
                    {
                        firstOption = false;
                        line += " ";
                    }
                    else
                    {
                        line += ",";
                    }
                    line += option.code + ":\"" + option.name + "\"";
                }
            }
            fw.write( line + "\n" );
        }
    }
    
    /**
     * The CheatBlock class encapsulates a cheat's name, description, and codes.
     */
    public static class CheatBlock
    {
        public String name = null;
        public String nextName = null;
        public String description = null;
        
        private LinkedList<CheatCode> codes;
        
        /**
         * Constructor: Creates an empty cheat 
         * 
         * @param name The name of the cheat (required).
         * @param description Description for the cheat (null for none).
         */
        public CheatBlock( String name, String description )
        {
            this.name = name;
            this.description = description;
            codes = new LinkedList<CheatCode>();
        }
        
        /**
         * Constructor: associates name, description, and codes recursively for every cheat in a section 
         * 
         * @param name The name of the cheat.
         * @param br The cheat file to read from.
         * @param lines Handle to the CheatSection lines list (to allow recursion in the constructor).
         * @param cheats Handle to the CheatSection cheats list (to allow recursion in the constructor).
         */
        public CheatBlock( String name, BufferedReader br, LinkedList<CheatLine> lines )
        {
            String fullLine, strLine, cheatName, address, code, options, option;
            CheatCode cheatCode = null;
            LinkedList<CheatOption> cheatOptions;
            Pattern pattern;
            Matcher matcher;
            int x;
            this.name = name;
            codes = new LinkedList<CheatCode>();

            try
            {
                while( ( fullLine = br.readLine() ) != null )
                {
                    strLine = fullLine.trim();
                    if( strLine.length() == 0 )
                    {
                        // End of the cheat section, return
                        return;
                    }
                    if( (strLine.length() < 3) ||
                        (strLine.substring( 0, 2 ).equals( "//" )) )
                    {  // A comment or blank line.
                        lines.add( new CheatLine( CheatLine.LINE_GARBAGE, fullLine + "\n", null ) );
                    }
                    else if( strLine.substring( 0, 2 ).equals( "cd" ) )
                    {  // Cheat code description
                        if( strLine.length() > 3 )
                            description = strLine.substring( 3, strLine.length() ).trim();
                    }
                    else if( strLine.substring( 0, 2 ).equals( "cn" ) )
                    {  // End of this cheat block, save the name of the next block, and return
                        if( strLine.length() > 3 )
                            cheatName = strLine.substring( 3, strLine.length() ).trim();
                        else
                            cheatName = "";
                        nextName = cheatName;
                        return;
                    }
                    else
                    {
                        // Cheat code line
                        x = strLine.indexOf( ' ' );
                        if(  x >= 0 && x < strLine.length() )
                        {
                            address = strLine.substring( 0, x ).trim();
                            code = strLine.substring( x + 1, strLine.length() ).trim();
                            // Check if this cheat has options
                            x = code.indexOf( ' ' );
                            if( x >= 0 && x < code.length() )
                            {
                                // Cheat contains options
                                cheatOptions = new LinkedList<CheatOption>();
                                options = code.substring( x + 1, code.length() ).trim();
                                code = code.substring( 0, x ).trim();
                                
                                pattern = Pattern.compile( "[0-9a-fA-F]{4}:\"([^\\\\\"]*(\\\\\")*)*\\\"" );
                                matcher = pattern.matcher( options );
                                while( matcher.find() )
                                {
                                   option = options.substring( matcher.start(), matcher.end() );
                                   cheatOptions.add( new CheatOption( option.substring( 0, 4 ),
                                           option.substring( 6, option.length() - 1 ) ) );
                                }                                
                                cheatCode = new CheatCode( address, code, cheatOptions );
                            }
                            else
                            {  // Code doesn't have any options
                                cheatCode = new CheatCode( address, code, null );
                            }
                            codes.add( cheatCode );
                        }
                    }
                }
            }
            catch( IOException ioe )
            {
                // (Don't care)
            }
        }
        


        
        /**
         * Returns the number of codes in this cheat.
         * 
         * @return integer.
         */
        public int size()
        {
            return codes.size();
        }
        
        /**
         * Returns the code at the specified index.
         * 
         * @param index Zero-based index.
         * @return null if index is invalid.
         */
        public CheatCode get( int index )
        {
            try
            {
                return codes.get( index );
            }
            catch( IndexOutOfBoundsException iobe )
            {
                return null;
            }
        }
        
        /**
         * Adds a new code to the end of this cheat.
         * 
         * @param cheatCode The code to add.
         * @return false if there was a problem.
         */
        public boolean add( CheatCode cheatCode )
        {
            return codes.add( cheatCode );
        }
        
        /**
         * Adds a new code to the specified index in this cheat.
         * 
         * @param cheatCode The code to add.
         * @return false if there was a problem.
         */
        public boolean add( int index, CheatCode cheatCode )
        {
            try
            {
                codes.add( index, cheatCode );
            }
            catch( IndexOutOfBoundsException iobe )
            {
                return false;
            }
            return true;
        }
        
        /**
         * Removes the code at the specified index.
         * 
         * @param index Zero-based index.
         * @return false if there was a problem.
         */
        public boolean remove( int index )
        {
            try
            {
                codes.remove( index );
            }
            catch( IndexOutOfBoundsException iobe )
            {
                return false;
            }
            return true;
        }
        
        /**
         * Removes all codes.
         * 
         */
        public void clear()
        {
            codes.clear();
        }
        
        /**
         * Saves the CheatBlock.
         * 
         * @param fw The file to save the CheatBlock to.
         * 
         * @throws IOException If a writing error occurs.
         */
        public void save( FileWriter fw ) throws IOException
        {
            fw.write( " cn " + name + "\n" );
            if( description != null )
            {
                fw.write( "  cd " + description + "\n" );
            }
            // Loop through the codes and save them
            for( CheatCode code : codes )
            {
                if( code != null )
                    code.save( fw );
            }
        }
    }
    
    /**
     * The CheatLine class stores each line of the cheat file (including comments).
     */
    private static class CheatLine
    {
        public static final int LINE_GARBAGE = 0;          // Comment, whitespace, or blank line
        public static final int LINE_CRC = 1;              // CRC section
        public static final int LINE_GOOD_NAME = 2;        // ROM "good name"
        public static final int CHEAT_BLOCK = 3;           // Encapsulated cheat block (strLine will be empty)

        public int lineType = 0;             // LINE_GARBAGE, LINE_SECTION, or LINE_PARAM.
        public String strLine = "";          // Actual line from cheat file (or empty if this is a cheat block).
        public CheatBlock cheatBlock = null; // Null unless this is a cheat block.
        
        /**
         * Constructor: Saves the relevant information about the line.
         * 
         * @param type   The type of line.
         * @param line   The line itself (empty if cheat block).
         * @param param  CheatBlock, if relevant.
         */
        public CheatLine( int type, String line, CheatBlock block )
        {
            lineType = type;
            strLine = line;
            cheatBlock = block;
        }
        
        /**
         * Saves the CheatLine.
         * 
         * @param fw The file to save the CheatLine to.
         * 
         * @throws IOException If a writing error occurs.
         */
        public void save( FileWriter fw ) throws IOException
        {
            if( lineType == CHEAT_BLOCK )
            {
                if( cheatBlock != null )
                    cheatBlock.save( fw );
            }
            else
            {
                fw.write( strLine );
            }
        }
    }

    /**
     * The CheatSection class reads all the parameters in the next section of the cheat file.
     * Saves the name of the next section (or null if end of file or error).
     * Can also be used to add a new section to an existing cheat file.
     */
    public static class CheatSection
    {
        public String crc;  // ROM crc
        public String goodName = "";  // ROM "good name"
        private LinkedList<CheatBlock> cheats;  // All the cheats in this section
        private LinkedList<CheatLine> lines;  // All the lines in the cheat section (including comments)
        
        // The next cheat section, or null if there are no sections left to read in the file:
        public String nextCrc = null;
        /**
         * Constructor: Creates an empty cheat section
         * 
         * @param crc The ROM CRC.
         */
        public CheatSection( String crc, String name, String countryCode )
        {
            cheats = new LinkedList<CheatBlock>();
            lines = new LinkedList<CheatLine>();
            
            if( !TextUtils.isEmpty( crc ) && !crc.equals( NO_CRC ) )
            {
                lines.add( new CheatLine( CheatLine.LINE_CRC, "crc " + crc + "-C:"+countryCode+"\n", null ) );
                lines.add( new CheatLine( CheatLine.LINE_GOOD_NAME, "gn "+ name +"\n", null ) );
            }
            this.crc = crc;
            this.goodName = name;
        }

        // TODO: Clean this method up a bit?
        /**
         * Constructor: Reads the next section of the cheat file, and saves it in 'cheats'.
         * 
         * @param crc The ROM CRC.
         * @param br The cheat file to read from.
         */
        public CheatSection( String crc, BufferedReader br )
        {
            String fullLine, strLine, cheatName;
            CheatBlock cheatBlock;
            
            cheats = new LinkedList<CheatBlock>();
            lines = new LinkedList<CheatLine>();

            if( !TextUtils.isEmpty( crc ) && !crc.equals( NO_CRC ) )
            {
                lines.add( new CheatLine( CheatLine.LINE_CRC, "crc " + crc + "\n", null ) );
            }
            this.crc = crc;
            
            // No file to read from. Quit.
            if( br == null )
                return;

            try
            {
                while( ( fullLine = br.readLine() ) != null )
                {
                    strLine = fullLine.trim();
                    if( (strLine.length() < 3) ||
                        (strLine.substring( 0, 2 ).equals( "//" )) )
                    {  // A comment or blank line.
                        lines.add( new CheatLine( CheatLine.LINE_GARBAGE, fullLine + "\n", null ) );
                    }
                    else if( strLine.substring( 0, 2 ).equals( "gn" ) )
                    {
                        if( strLine.length() > 3 )
                            goodName = strLine.substring( 3, strLine.length() ).trim();
                        lines.add( new CheatLine( CheatLine.LINE_GOOD_NAME, fullLine + "\n", null ) );
                    }
                    else if( strLine.substring( 0, 2 ).equals( "cn" ) )
                    {
                        if( strLine.length() > 3 )
                            cheatName = strLine.substring( 3, strLine.length() ).trim();
                        else
                            cheatName = "";
                        
                        while( !TextUtils.isEmpty( cheatName ) )                        
                        {
                            cheatBlock = new CheatBlock( cheatName, br, lines );
                            lines.add(  new CheatLine( CheatLine.CHEAT_BLOCK, "", cheatBlock ) );
                            cheats.add(  cheatBlock );
                            cheatName = cheatBlock.nextName;
                        }
                    }
                    else if( strLine.substring( 0, 3 ).equals( "crc" ) )
                    {
                        if( strLine.length() > 4 )
                            nextCrc = strLine.substring( 4, strLine.length() ).trim();
                        // Done reading section.
                        return;
                    }
                    else
                    {   
                        // This shouldn't happen (bad syntax). Quit.
                        return;
                    }
                }
            }
            catch( IOException ioe )
            {
                // (Don't care)
            }
            
            // Reached end of file or error.. either way, just quit
            return;
        }
        
        /**
         * Returns the number of cheats in this section.
         * 
         * @return Cheats count.
         */
        public int size()
        {
            return cheats.size();
        }
        
        /**
         * Returns the cheat at the specified index.
         * 
         * @param index Zero-based index.
         * @return null if index is invalid.
         */
        public CheatBlock get( int index )
        {
            try
            {
                return cheats.get( index );
            }
            catch( IndexOutOfBoundsException iobe )
            {
                return null;
            }
        }
        
        /**
         * Adds a new cheat to the end of this section.
         * 
         * @param cheatBlock The cheat to add.
         * @return false if there was a problem.
         */
        public boolean add( CheatBlock cheatBlock )
        {
            boolean success = cheats.add( cheatBlock );
            success = success && lines.add( new CheatLine( CheatLine.CHEAT_BLOCK, "", cheatBlock ) );
            return success;
        }
        
        /**
         * Adds a new cheat to the specified index in this section.
         * 
         * @param cheatBlock The cheat to add.
         * @return false if there was a problem.
         */
        public boolean add( int index, CheatBlock cheatBlock )
        {
            CheatLine cheatLine = new CheatLine( CheatLine.CHEAT_BLOCK, "", cheatBlock );
            try
            {
                cheats.add( index, cheatBlock );
                int i = 0;
                for( CheatLine line : lines )
                {
                    if( line.lineType == CheatLine.CHEAT_BLOCK )
                    {
                        if( i == index )
                        {
                            lines.add( lines.indexOf( line ), cheatLine );
                            break;
                        }
                        i++;
                    }
                }
            }
            catch( IndexOutOfBoundsException iobe )
            {
                return false;
            }
            return true;
        }
        
        /**
         * Removes the cheat at the specified index.
         * 
         * @param index Zero-based index.
         * @return false if there was a problem.
         */
        public boolean remove( int index )
        {
            try
            {
                cheats.remove( index );
                int i = 0;
                for( CheatLine line : lines )
                {
                    if( line.lineType == CheatLine.CHEAT_BLOCK )
                    {
                        if( i == index )
                        {
                            lines.remove( line );
                            break;
                        }
                        i++;
                    }
                }
            }
            catch( IndexOutOfBoundsException iobe )
            {
                return false;
            }
            return true;
        }
        
        /**
         * Removes all cheats.
         */
        public void clear()
        {
            cheats.clear();
            Iterator<CheatLine> iterator = lines.iterator();
            CheatLine line;
            while( iterator.hasNext() )
            {
                line = iterator.next();
                if( line.lineType == CheatLine.CHEAT_BLOCK )
                {
                    iterator.remove();
                }
            }
        }
        
        /**
         * Writes the entire section to file.
         * 
         * @param fw File to write to.
         * 
         * @throws IOException if a writing error occurs.
         */
        private void save( FileWriter fw ) throws IOException
        {
            for( CheatLine line : lines )
            {
                if( line != null )
                    line.save( fw );
            }
        }
    }
}
