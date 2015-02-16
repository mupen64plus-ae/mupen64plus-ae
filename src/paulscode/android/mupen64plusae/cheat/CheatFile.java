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
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;
import android.util.Log;

/**
 * The CheatFile class is used to read and write in the unique syntax of mupencheat.txt.
 * <p>
 * The file must follow a specific syntax:
 * <ul>
 * <li>There is a space between each parameter and value. Value is not quoted and may contain spaces
 * (ex: cn Epic Win). EXCEPTION: cheat options.
 * <li>ROM sections are not indented, beginning with "crc" followed by the CRC and country code (ex:
 * crc 01A23456-789012B3-C:4A).
 * <li>The crc line is immediately followed by the ROM "good name" line.
 * <li>ROM "good name" lines are not indented, beginning with "gn" followed by the name (ex: gn Cool
 * Game (U) (V1.0)).
 * <li>Code names are indented one space. They begin with "cn" followed by the code name (ex: cn
 * Kill everything).
 * <li>Code description lines are optional. When present, they immediately follow the code name
 * line.
 * <li>Code description lines are indented two spaces, and begin with "cd" followed by the
 * description. (ex: cd This cheat is for wimps).
 * <li>Code lines immediately follow the code description line (or code name line if description not
 * present).
 * <li>Code lines are indented two spaces, and begin with a memory address followed by a value. (ex:
 * 12345678 9012).
 * <li>Codes that contain options use four question marks for the value, followed by the cheat
 * options (ex: 12345678 ???? 9012:"Do this",ABFE:"Do that").
 * <li>Cheat options are comma-separated. They consist of the value, a colon, then the option string
 * in quotes (ex: 12345678 ???? 9012:"Do this",ABFE:"Do that").
 * <li>There is a blank line between ROM sections.
 * <li>Leading and trailing whitespace in lines, param names, and values is discarded.
 * <li>Whitespace between words in a value or inside double-quoted cheat option strings is not
 * discarded.
 * </ul>
 * 
 * @author Paul Lamb
 */
public class CheatFile
{
    /** The name we use for the untitled section (preamble) of the cheat file. */
    public static final String NO_KEY = "[<sectionless!>]";
    
    /** The regular expression matcher for a cheat code. */
    private static final Matcher CODE_MATCHER = Pattern.compile(
            "^  ([0-9a-fA-F]{8}) ([0-9a-fA-F]{4}|[\\?]{4} )(.*)" ).matcher( "" );
    
    /**
     * The regular expression matcher for a single cheat option. Inspired by
     * http://stackoverflow.com/a/5696141/254218
     */
    private static final Matcher OPTION_MATCHER = Pattern.compile(
            "([0-9a-fA-F]{4}):\"([^\\\\\"]*+(?:\\\\\"[^\\\\\"]*+)*+)\"" ).matcher( "" );
    
    /** Path of the cheat file. */
    private final String mFilename;
    
    /** All cheat sections in this cheat file, in correct order. */
    private final LinkedHashMap<String, CheatSection> mSections;
    
    /**
     * Constructs a {@link CheatFile} object and reads the data from disk into memory.
     * 
     * @param filename the path of the file to load
     */
    public CheatFile( String filename )
    {
        mFilename = filename;
        mSections = new LinkedHashMap<String, CheatFile.CheatSection>();
        reload();
    }
    
    /**
     * Reloads the entire cheat file from disk into memory, overwriting any unsaved changes.
     * 
     * @return true if successful
     * @see #save()
     */
    public boolean reload()
    {
        // Make sure a file was specified in the constructor
        if( TextUtils.isEmpty( mFilename ) )
        {
            Log.e( "CheatFile", "Filename not specified in method reload()" );
            return false;
        }
        
        // Free any previously loaded data
        clear();
        
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader( new FileReader( mFilename ) );
            
            // Read the 'sectionless' preamble section from disk
            String key = NO_KEY;
            CheatSection section = new CheatSection( key, reader );
            mSections.put( key, section );
            
            // Read the remaining sections from disk
            while( !TextUtils.isEmpty( section.nextKey ) )
            {
                key = section.nextKey;
                section = new CheatSection( key, reader );
                mSections.put( key, section );
            }
        }
        catch( FileNotFoundException e )
        {
            Log.e( "CheatFile", "Could not open " + mFilename );
            return false;
        }
        catch( IOException e )
        {
            Log.e( "CheatFile", "Could not read " + mFilename );
            return false;
        }
        finally
        {
            if( reader != null )
            {
                try
                {
                    reader.close();
                }
                catch( IOException ignored )
                {
                }
            }
        }
        return true;
    }
    
    /**
     * Writes the cheat data from memory back to disk.
     * 
     * @return true if successful
     * @see #reload()
     */
    public boolean save()
    {
        // Make sure a filename was specified in the constructor
        if( TextUtils.isEmpty( mFilename ) )
        {
            Log.e( "CheatFile", "Filename not specified in method save()" );
            return false;
        }
        
        Writer writer = null;
        try
        {
            writer = new BufferedWriter( new FileWriter( mFilename ) );
            
            // Write each section to disk
            for( CheatSection section : mSections.values() )
            {
                section.save( writer );
            }
        }
        catch( IOException e )
        {
            Log.e( "CheatFile", "Error saving to " + mFilename + ": " + e.getMessage() );
            return false;
        }
        finally
        {
            if( writer != null )
            {
                try
                {
                    writer.close();
                }
                catch( IOException ignored )
                {
                }
            }
        }
        return true;
    }
    
    /**
     * Returns the set of keys contained in this cheat file.
     * 
     * @return keyset containing all the cheat section CRCs
     */
    public Set<String> keySet()
    {
        return mSections.keySet();
    }
    
    /**
     * Returns the cheat section with the specified key.
     * 
     * @param key the key of the cheat section to get
     * 
     * @return the cheat section with the given key, or null if not found
     */
    public CheatSection get( String key )
    {
        return mSections.get( key );
    }
    
    /**
     * Returns the first cheat section whose key matches the specified regular expression (not
     * necessarily the only match).
     * 
     * @param pattern a regular expression to match the key to
     * 
     * @return cheat section whose key matches the given regex, or null if no match was found
     */
    public CheatSection match( String pattern )
    {
        for( String key : mSections.keySet() )
        {
            if( key.matches( pattern ) )
                return mSections.get( key );
        }
        return null;
    }
    
    /**
     * Adds a cheat section to memory. If a cheat section with that key already exists in memory, it
     * will be overwritten. Note that the operation is not actually persisted to disk until the
     * {@link #save()} method is called.
     * 
     * @param section the cheat section to add/replace in memory
     */
    public void add( CheatSection section )
    {
        if( section != null )
        {
            mSections.put( section.key, section );
        }
    }
    
    /**
     * Removes all cheat sections from memory. Note that the operation is not actually persisted to
     * disk until the {@link #save()} method is called.
     */
    public void clear()
    {
        mSections.clear();
    }
    
    /**
     * The CheatSection class encapsulates all cheat data for a given ROM CRC and country code.
     */
    public static class CheatSection
    {
        /** The CRC and country code of the ROM this section pertains to. */
        public final String key;
        
        // TODO: Make this final
        /** The "good name" of the ROM this section pertains to. */
        public String goodName = "";
        
        /** All cheat blocks in this section. */
        private final LinkedList<CheatBlock> blocks;
        
        /** All elements in this section (cheat blocks and cheat lines). */
        private final LinkedList<CheatElement> elements;
        
        /** The raw lines of text associated with this section, emptied after {@link #lazyLoad()}. */
        private final LinkedList<String> fullLines;
        
        // TODO: Make this final
        /** The key of the next cheat section, or null if no more sections are left. */
        private String nextKey = null;
        
        /**
         * Constructs an empty {@link CheatSection} object.
         * 
         * @param crc the CRC of the ROM this section pertains to
         * @param name the "good name" of the ROM this section pertains to
         * @param country the country code of the ROM this section pertains to
         */
        public CheatSection( String crc, String name, String country )
        {
            this.key = crc + "-C:" + country;
            this.goodName = name;
            this.blocks = new LinkedList<CheatBlock>();
            this.elements = new LinkedList<CheatElement>();
            this.fullLines = new LinkedList<String>();
            
            // Generate the header lines for this section
            if( !TextUtils.isEmpty( crc ) && !crc.equals( NO_KEY ) )
            {
                elements.add( new CheatLine( "crc " + key ) );
                elements.add( new CheatLine( "gn " + name ) );
            }
        }
        
        /**
         * Constructs a {@link CheatSection} object, populating its fields by reading from disk.
         * 
         * @param key the key (ROM CRC and country code) for this ROM section
         * @param reader the object providing disk read access
         * @throws IOException if a read error occurs
         */
        private CheatSection( String key, BufferedReader reader ) throws IOException
        {
            this.key = key;
            this.blocks = new LinkedList<CheatBlock>();
            this.elements = new LinkedList<CheatElement>();
            this.fullLines = new LinkedList<String>();
            
            if( !key.equals( NO_KEY ) )
            {
                elements.add( new CheatLine( "crc " + key ) );
            }
            
            String fullLine;
            while( ( fullLine = reader.readLine() ) != null )
            {
                if( fullLine.startsWith( "crc " ) )
                {
                    // Start of the next cheat section, return
                    nextKey = fullLine.substring( 4 );
                    return;
                }
                else
                {
                    fullLines.add( fullLine );
                }
            }
        }
        
        /**
         * Populates the cheat section fields using the disk data cached in the constructor.
         */
        private void lazyLoad()
        {
            Iterator<String> iterator = fullLines.iterator();
            while( iterator.hasNext() )
            {
                String fullLine = iterator.next();
                if( fullLine.length() == 0 || fullLine.startsWith( "//" ) )
                {
                    // A comment or blank line
                    elements.add( new CheatLine( fullLine ) );
                }
                else if( fullLine.startsWith( "gn " ) )
                {
                    // ROM "good name"
                    goodName = fullLine.substring( 3 );
                    elements.add( new CheatLine( fullLine ) );
                }
                else if( fullLine.startsWith( " cn " ) )
                {
                    // Cheat block
                    String name = fullLine.substring( 4 );
                    while( !TextUtils.isEmpty( name ) )
                    {
                        CheatBlock block = new CheatBlock( name, iterator, elements );
                        elements.add( block );
                        blocks.add( block );
                        name = block.nextName;
                    }
                }
                else
                {
                    // This shouldn't happen (bad syntax), finish
                    Log.w( "CheatSection", "Unknown syntax: " + fullLine );
                    break;
                }
            }
            fullLines.clear();
        }
        
        /**
         * Writes the entire cheat section to disk.
         * 
         * @param writer the object providing disk write access
         * 
         * @throws IOException if a write error occurs
         */
        private void save( Writer writer ) throws IOException
        {
            for( CheatElement element : elements )
            {
                element.save( writer );
            }
            if( fullLines.size() == 0 && !TextUtils.isEmpty( key ) && !key.equals( NO_KEY ) )
            {
                // Insert blank line between sections
                writer.append( '\n' );
            }
            for( String fullLine : fullLines )
            {
                writer.append( fullLine ).append( '\n' );
            }
        }
        
        /**
         * Returns the number of cheat blocks in this cheat section.
         * 
         * @return the number of cheat blocks
         */
        public int size()
        {
            lazyLoad();
            return blocks.size();
        }
        
        /**
         * Returns the cheat block at the specified index in this cheat section.
         * 
         * @param index zero-based index
         * @return null if index is invalid
         */
        public CheatBlock get( int index )
        {
            lazyLoad();
            try
            {
                return blocks.get( index );
            }
            catch( IndexOutOfBoundsException e )
            {
                return null;
            }
        }
        
        /**
         * Adds a new cheat block to the end of this cheat section.
         * 
         * @param block the cheat block to add
         * @return false if block is null
         */
        public boolean add( CheatBlock block )
        {
            if( block == null )
                return false;
            
            lazyLoad();
            boolean success = blocks.add( block );
            success &= elements.add( block );
            return success;
        }
        
        /**
         * Adds a new cheat block at the specified index in this cheat section.
         * 
         * @param index zero-based index
         * @param block the cheat block to add
         * @return false if index is invalid or block is null
         */
        public boolean add( int index, CheatBlock block )
        {
            if( block == null )
                return false;
            
            lazyLoad();
            try
            {
                blocks.add( index, block );
                int i = 0;
                for( CheatElement element : elements )
                {
                    if( element instanceof CheatBlock )
                    {
                        if( i == index )
                        {
                            elements.add( elements.indexOf( element ), block );
                            break;
                        }
                        i++;
                    }
                }
            }
            catch( IndexOutOfBoundsException e )
            {
                return false;
            }
            return true;
        }
        
        /**
         * Removes the cheat block at the specified index in this cheat section.
         * 
         * @param index zero-based index
         * @return false if index is invalid
         */
        public boolean remove( int index )
        {
            lazyLoad();
            try
            {
                blocks.remove( index );
                int i = 0;
                for( CheatElement element : elements )
                {
                    if( element instanceof CheatBlock )
                    {
                        if( i == index )
                        {
                            elements.remove( element );
                            break;
                        }
                        i++;
                    }
                }
            }
            catch( IndexOutOfBoundsException e )
            {
                return false;
            }
            return true;
        }
        
        /**
         * Removes all cheat blocks from this cheat section. Comments and other non-cheat lines are
         * not removed.
         */
        public void clear()
        {
            lazyLoad();
            blocks.clear();
            Iterator<CheatElement> iterator = elements.iterator();
            CheatElement element;
            while( iterator.hasNext() )
            {
                element = iterator.next();
                if( element instanceof CheatBlock )
                {
                    iterator.remove();
                }
            }
        }
    }
    
    /**
     * The CheatElement class is an abstract base class for all elements of a cheat section.
     */
    private abstract static class CheatElement
    {
        abstract protected void save( Writer writer ) throws IOException;
    }
    
    /**
     * The CheatLine class encapsulates any line of text that is not associated with a cheat block
     * (i.e. comments and the lines containing key and good name).
     */
    private static class CheatLine extends CheatElement
    {
        private final String text;
        
        private CheatLine( String text )
        {
            this.text = text;
        }
        
        @Override
        protected void save( Writer writer ) throws IOException
        {
            writer.append( text ).append( '\n' );
        }
    }
    
    /**
     * The CheatBlock class encapsulates a cheat's name, description, and codes.
     */
    public static class CheatBlock extends CheatElement
    {
        /** The human-readable name of the cheat. */
        public final String name;
        
        // TODO: Make this final
        /** The human-readable description of the cheat. */
        public String description = null;
        
        // TODO: Make this final
        /** The name of the next cheat. */
        private String nextName = null;
        
        /** The cheat codes in this cheat block. */
        private final LinkedList<CheatCode> codes;
        
        /**
         * Constructs an empty {@CheatBlock} object.
         * 
         * @param name the name of the cheat (required)
         * @param description the description for the cheat (null for none).
         */
        public CheatBlock( String name, String description )
        {
            this.name = name;
            this.description = description;
            this.codes = new LinkedList<CheatCode>();
        }
        
        /**
         * Constructs a {@CheatBlock} object, populating its fields by reading from
         * disk.
         * 
         * @param name the name of the cheat (required)
         * @param iterator iterator for the raw lines of text in this cheat block
         * @param elements reference to the list of cheat lines in the cheat section (to allow
         *            recursion)
         */
        private CheatBlock( String name, Iterator<String> iterator,
                LinkedList<CheatElement> elements )
        {
            this.name = name;
            this.codes = new LinkedList<CheatCode>();
            
            while( iterator.hasNext() )
            {
                String fullLine = iterator.next();
                if( fullLine.length() == 0 )
                {
                    // End of the cheat section, return
                    return;
                }
                else if( fullLine.startsWith( "//" ) )
                {
                    // Comment
                    elements.add( new CheatLine( fullLine ) );
                }
                else if( fullLine.startsWith( "  cd " ) )
                {
                    // Cheat block description
                    description = fullLine.substring( 5 );
                }
                else if( fullLine.startsWith( " cn " ) )
                {
                    // End of this cheat block, save the name of the next block, and return
                    nextName = fullLine.substring( 4 );
                    return;
                }
                else if( CODE_MATCHER.reset( fullLine ).matches() )
                {
                    // Cheat code
                    String address = CODE_MATCHER.group( 1 );
                    String value = CODE_MATCHER.group( 2 );
                    
                    // Cheat options
                    LinkedList<CheatOption> options = null;
                    if( value.startsWith( "????" ) )
                    {
                        // Cheat options
                        options = new LinkedList<CheatOption>();
                        OPTION_MATCHER.reset( CODE_MATCHER.group( 3 ) );
                        while( OPTION_MATCHER.find() )
                        {
                            options.add( new CheatOption( OPTION_MATCHER.group( 1 ), OPTION_MATCHER
                                    .group( 2 ) ) );
                        }
                    }
                    CheatCode code = new CheatCode( address, value, options );
                    codes.add( code );
                }
                else
                {
                    // This shouldn't happen (bad syntax), quit
                    Log.w( "CheatBlock", "Unknown syntax: " + fullLine );
                    return;
                }
            }
        }
        
        /**
         * Writes the entire cheat block to disk.
         * 
         * @param writer the object providing disk write access
         * 
         * @throws IOException if a write error occurs
         */
        @Override
        protected void save( Writer writer ) throws IOException
        {
            writer.append( " cn " ).append( name ).append( '\n' );
            if( description != null )
            {
                writer.append( "  cd " ).append( description ).append( '\n' );
            }
            // Loop through the codes and save them
            for( CheatCode code : codes )
            {
                writer.append( "  " ).append( code.address ).append( ' ' ).append( code.code );
                if( code.options != null )
                {
                    String delimiter = " ";
                    for( CheatOption option : code.options )
                    {
                        writer.append( delimiter ).append( option.code ).append( ':' );
                        writer.append( '"' ).append( option.name ).append( '"' );
                        delimiter = ",";
                    }
                }
                writer.append( '\n' );
            }
        }
        
        /**
         * Returns the number of cheat codes in this cheat block.
         * 
         * @return the number of cheat codes
         */
        public int size()
        {
            return codes.size();
        }
        
        /**
         * Returns the cheat code at the specified index in this cheat block.
         * 
         * @param index zero-based index
         * @return null if index is invalid
         */
        public CheatCode get( int index )
        {
            try
            {
                return codes.get( index );
            }
            catch( IndexOutOfBoundsException e )
            {
                return null;
            }
        }
        
        /**
         * Adds a new cheat code to the end of this cheat block.
         * 
         * @param code the cheat code to add
         * @return false if code is null
         */
        public boolean add( CheatCode code )
        {
            if( code == null )
                return false;
            
            return codes.add( code );
        }
        
        /**
         * Adds a new cheat code at the specified index in this cheat block.
         * 
         * @param index zero-based index
         * @param code the cheat code to add
         * @return false if index is invalid or code is null
         */
        public boolean add( int index, CheatCode code )
        {
            if( code == null )
                return false;
            
            try
            {
                codes.add( index, code );
            }
            catch( IndexOutOfBoundsException e )
            {
                return false;
            }
            return true;
        }
        
        /**
         * Removes the cheat code at the specified index in this cheat block.
         * 
         * @param index zero-based index
         * @return false if index is invalid
         */
        public boolean remove( int index )
        {
            try
            {
                codes.remove( index );
            }
            catch( IndexOutOfBoundsException e )
            {
                return false;
            }
            return true;
        }
        
        /**
         * Removes all cheat codes from this cheat block.
         */
        public void clear()
        {
            codes.clear();
        }
    }
    
    /**
     * The CheatCode class encapsulates a memory address and code or code options.
     */
    public static class CheatCode
    {
        /** ROM address where cheat is applied. */
        public final String address;
        
        /** Code to apply. */
        public final String code;
        
        // TODO: Make this private
        /** Options associated with this cheat code (may be empty). */
        public final LinkedList<CheatOption> options;
        
        /**
         * Constructs a {@link CheatCode} object.
         * 
         * @param address the ROM address where cheat is to be applied
         * @param code the code to apply (or "????" if there are options)
         * @param options the options associated with this cheat (or null if none)
         */
        public CheatCode( String address, String code, LinkedList<CheatOption> options )
        {
            this.address = address;
            this.code = code;
            this.options = options;
        }
        
        /**
         * Returns the cheat option at the specified index in this cheat block.
         * 
         * @param index zero-based index
         * @return null if index is invalid
         */
        public CheatOption get( int index )
        {
            try
            {
                return options.get( index );
            }
            catch( IndexOutOfBoundsException e )
            {
                return null;
            }
        }
        
        /**
         * Adds a new cheat option to the end of this cheat block.
         * 
         * @param option the cheat option to add
         * @return false if option is null
         */
        public boolean add( CheatOption option )
        {
            if( option == null )
                return false;
            
            return options.add( option );
        }
        
        /**
         * Adds a new cheat option at the specified index in this cheat block.
         * 
         * @param index zero-based index
         * @param option the cheat option to add
         * @return false if index is invalid or option is null
         */
        public boolean add( int index, CheatOption option )
        {
            if( option == null )
                return false;
            
            try
            {
                options.add( index, option );
            }
            catch( IndexOutOfBoundsException e )
            {
                return false;
            }
            return true;
        }
        
        /**
         * Removes the cheat option at the specified index in this cheat block.
         * 
         * @param index zero-based index
         * @return false if index is invalid
         */
        public boolean remove( int index )
        {
            try
            {
                options.remove( index );
            }
            catch( IndexOutOfBoundsException e )
            {
                return false;
            }
            return true;
        }
        
        /**
         * Removes all cheat options from this cheat block.
         */
        public void clear()
        {
            options.clear();
        }
    }
    
    /**
     * The CheatOption class encapsulates a code and a description.
     */
    public static class CheatOption
    {
        /** The code to apply if this cheat option is selected. */
        public final String code;
        
        /** The human-readable name of this cheat option. */
        public final String name;
        
        /**
         * Constructs a {@link CheatOption} object.
         * 
         * @param code the code to apply if this cheat option is selected
         * @param name the human-readable name for the option
         */
        public CheatOption( String code, String name )
        {
            this.code = code;
            this.name = name;
        }
    }
}
