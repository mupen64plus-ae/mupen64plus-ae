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
package emulator.android.mupen64plusae.persistent;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;

import emulator.android.mupen64plusae.util.FileUtil;

/**
 * The ConfigFile class is used to load the parameters from a config file.
 * <p>
 * The file must follow a specific syntax:
 * <ul>
 * <li>Parameters are assigned to values with the equal sign (ex: param=value ).
 * <li>Parameters are allowed to have empty assignments (ex: param= ).
 * <li>Each parameter=value pair must be on a single line.
 * <li>Values may be enclosed in double-quotes (optional) (ex: param="value" ).
 * <li>All parameters must be after a section title.
 * <li>Section titles are enclosed in brackets (ex. "[SECTION]")
 * <li>Comments consist of single lines, and begin with # or ; or // (ex: ;comment ).
 * <li>Leading and trailing whitespace in lines, param names, and values is discarded.
 * <li>Whitespace inside brackets or double-quotes is not discarded.
 * </ul>
 * 
 * @author Paul Lamb
 */
public class ConfigFile
{
    /** The name we use for the untitled section (preamble) of the config file. */
    public static final String SECTIONLESS_NAME = "[<sectionless!>]";
    
    /** Name of the config file. */
    private final String mFilename;
    
    /** Sections mapped by title for easy lookup, with insertion order retained. */
    private final LinkedHashMap<String, ConfigSection> mConfigMap;

    /** True if the file was read using a context */
    private boolean mReadUsingContext = false;
    
    /**
     * Reads the entire config file, and saves the data to internal collections for manipulation.
     * 
     * @param filename The config file to read from.
     */
    public ConfigFile( String filename )
    {
        mFilename = filename;
        mConfigMap = new LinkedHashMap<>();
        reload(null);
    }

    /**
     * Reads the entire config file, and saves the data to internal collections for manipulation.
     * When reading a file using a context, the file will be read only.
     *
     * @param context Context to read from assets
     * @param filename The config file to read from.
     */
    public ConfigFile(Context context, String filename )
    {
        mFilename = filename;
        mConfigMap = new LinkedHashMap<>();
        reload(context);
        mReadUsingContext = context != null;
    }


    /**
     * Looks up a config section by its title.
     * 
     * @param sectionTitle Title of the section containing the parameter.
     * 
     * @return A ConfigSection containing parameters, or null if not found.
     */
    public synchronized ConfigSection get( String sectionTitle )
    {
        return mConfigMap.get( sectionTitle );
    }
    
    /**
     * Removes a config section by its title. Note that the removal is not actually persisted to
     * disk until the {@link #save()} method is called.
     * 
     * @param sectionTitle Title of the section containing the parameter.
     */
    public synchronized void remove( String sectionTitle )
    {
        mConfigMap.remove( sectionTitle );
    }
    
    /**
     * Looks up the specified parameter under the specified section title.
     * 
     * @param sectionTitle Title of the section containing the parameter.
     * @param parameter Name of the parameter.
     * 
     * @return The value of the specified parameter, or null if not found.
     */
    public synchronized String get( String sectionTitle, String parameter )
    {
        ConfigSection section = mConfigMap.get( sectionTitle );
        
        // The specified section doesn't exist or is empty.. quit
        if( section == null || section.parameters == null )
            return null;
        
        ConfigParameter confParam = section.parameters.get( parameter );
        
        // The specified parameter doesn't exist.. quit
        if( confParam == null )
            return null;
        
        // Got it
        return confParam.value;
    }
    
    /**
     * Assigns the specified value to the specified parameter under the specified section.
     * 
     * @param sectionTitle The title of the section to contain the parameter.
     * @param parameter The name of the parameter.
     * @param value The value to give the parameter.
     */
    public synchronized void put( String sectionTitle, String parameter, String value )
    {
        ConfigSection section = mConfigMap.get( sectionTitle );
        if( section == null )
        {
            // Add a new section
            section = new ConfigSection( sectionTitle );
            mConfigMap.put( sectionTitle, section );
        }
        section.put( parameter, value );
    }
    
    /**
     * Erases any previously loaded data.
     */
    public synchronized void clear()
    {
        mConfigMap.clear();
    }

    /**
     * Re-loads the entire config file, overwriting any unsaved changes, and saves the data in
     * 'configMap'.
     *
     * @return True if successful.
     * @see #save()
     */
    public boolean reload() {
        return reload(null);
    }
    
    /**
     * Re-loads the entire config file, overwriting any unsaved changes, and saves the data in
     * 'configMap'.
     * 
     * @return True if successful.
     * @see #save()
     */
    private synchronized boolean reload(Context context)
    {
        if (mReadUsingContext) {
            return false;
        }

        // Make sure a file was actually specified
        if( TextUtils.isEmpty( mFilename ) )
            return false;
        
        // Free any previously loaded data
        clear();

        InputStream fstream;

        if (context == null) {
            try
            {
                fstream = new FileInputStream( mFilename );
            }
            catch( FileNotFoundException fnfe )
            {
                // File not found... we can't continue
                return false;
            }
        } else {
            try {
                fstream = context.getAssets().open(mFilename);
            } catch (IOException|NullPointerException e) {
                return false;
            }
        }
        
        DataInputStream in = new DataInputStream( fstream );
        BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
        
        String sectionName = SECTIONLESS_NAME;
        ConfigSection section = new ConfigSection( sectionName, br ); // Read the 'sectionless'
                                                                      // section
        mConfigMap.put( sectionName, section ); // Save the data to 'configMap'
        
        // Loop through reading the remaining sections
        while( !TextUtils.isEmpty( section.nextName ) )
        {
            // Get the next section name
            sectionName = section.nextName;
            
            // Load the next section
            section = new ConfigSection( sectionName, br );
            mConfigMap.put( sectionName, section ); // Save the data to 'configMap'
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
     * Saves the data from 'configMap' back to the config file.
     * 
     * @return True if successful. False otherwise.
     * @see #reload()
     */
    public synchronized boolean save()
    {
        // No filename was specified.
        if( TextUtils.isEmpty( mFilename ) )
        {
            Log.e( "ConfigFile", "Filename not specified in method save()" );
            return false; // Quit
        }
        
        // Ensure parent directories exist before writing file
        FileUtil.makeDirs(new File( mFilename ).getParentFile().getPath());
        
        // Write data to file
        try(FileWriter fw = new FileWriter( mFilename ))
        {
            // Loop through the sections
            for( ConfigSection section : mConfigMap.values() )
            {
                if( section != null )
                    section.save( fw );
            }
        }
        catch( IOException ioe )
        {
            Log.e( "ConfigFile", "IOException creating file " + mFilename + ", error message: "
                    + ioe.getMessage() );
            return false; // Some problem creating the file.. quit
        }
        
        // Success
        return true;
    }
    
    /**
     * Returns a handle to the configMap keyset.
     * 
     * @return keyset containing all the config section titles.
     */
    public synchronized Set<String> keySet()
    {
        return mConfigMap.keySet();
    }
    
    /**
     * The ConfigSection class reads all the parameters in the next section of the config file.
     * Saves the name of the next section (or null if end of file or error). Can also be used to add
     * a new section to an existing configuration.
     */
    public static class ConfigSection
    {
        public String name; // Section name
        private final HashMap<String, ConfigParameter> parameters; // Parameters sorted by name for easy
                                                             // lookup
        private final LinkedList<ConfigLine> lines; // All the lines in this section, including comments
        
        // Name of the next section, or null if there are no sections left to read in the file:
        private String nextName = null;
        
        /**
         * Constructor: Creates an empty config section
         * 
         * @param sectionName The section title.
         */
        public ConfigSection( String sectionName )
        {
            parameters = new HashMap<>();
            lines = new LinkedList<>();
            
            if( !TextUtils.isEmpty( sectionName ) && !sectionName.equals( SECTIONLESS_NAME ) )
                lines.add( new ConfigLine( ConfigLine.LINE_SECTION, "[" + sectionName + "]\n", null ) );
            
            name = sectionName;
        }
        
        /**
         * Constructor: Reads the next section of the config file, and saves it in 'parameters'.
         * 
         * @param sectionName The section title.
         * @param br The config file to read from.
         */
        public ConfigSection( String sectionName, BufferedReader br )
        {
            String fullLine, strLine, p, v;
            ConfigParameter confParam;
            int x, y;
            
            parameters = new HashMap<>();
            lines = new LinkedList<>();
            
            if( !TextUtils.isEmpty( sectionName ) && !sectionName.equals( SECTIONLESS_NAME ) )
                lines.add( new ConfigLine( ConfigLine.LINE_SECTION, "[" + sectionName + "]\n", null ) );
            
            name = sectionName;
            
            // No file to read from. Quit.
            if( br == null )
                return;
            
            try
            {
                while( ( fullLine = br.readLine() ) != null )
                {
                    strLine = fullLine.trim();
                    if( ( strLine.length() < 1 )
                            || ( strLine.startsWith( "#" ) )
                            || ( strLine.startsWith( ";" ) )
                            || ( ( strLine.length() > 1 ) && ( strLine
                                    .startsWith( "//" ) ) ) )
                    
                    { // A comment or blank line.
                        lines.add( new ConfigLine( ConfigLine.LINE_GARBAGE, fullLine + "\n", null ) );
                    }
                    else if( strLine.contains( "=" ) )
                    {
                        // This should be a "parameter=value" pair:
                        x = strLine.indexOf( '=' );
                        
                        if( x < 1 )
                            return; // This shouldn't happen (bad syntax). Quit.
                            
                        if( x < ( strLine.length() - 1 ) )
                        {
                            p = strLine.substring( 0, x ).trim();
                            if( p.length() < 1 )
                                return; // This shouldn't happen (bad syntax). Quit.
                                
                            v = strLine.substring( x + 1).trim();
                            // v = v.replace( "\"", "" ); // I'm doing this later, so I can save
                            // back without losing them
                            
                            if( v.length() > 0 )
                            {
                                // Save the parameter=value pair
                                confParam = parameters.get( p );
                                if( confParam != null )
                                {
                                    confParam.value = v;
                                }
                                else
                                {
                                    confParam = new ConfigParameter( p, v );
                                    lines.add( new ConfigLine( ConfigLine.LINE_PARAM, fullLine
                                            + "\n", confParam ) );
                                    parameters.put( p, confParam ); // Save the pair.
                                }
                            }
                        } // It's ok to have an empty assignment (such as "param=")
                    }
                    else if( strLine.contains( "[" ) )
                    {
                        // This should be the beginning of the next section
                        if( ( strLine.length() < 3 ) || ( !strLine.contains( "]" ) ) )
                            return; // This shouldn't happen (bad syntax). Quit.
                            
                        x = strLine.indexOf( '[' );
                        y = strLine.indexOf( ']' );
                        
                        if( ( x == -1 ) || ( y == -1 ) || ( y <= x + 1 )  )
                            return; // This shouldn't happen (bad syntax). Quit.
                            
                        p = strLine.substring( x + 1, y ).trim();
                        
                        // Save the name of the next section.
                        nextName = p;
                        
                        // Done reading parameters. Return.
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
        }
        
        /**
         * Returns a handle to the parameter keyset.
         * 
         * @return keyset containing all the parameters.
         */
        public synchronized Set<String> keySet()
        {
            return parameters.keySet();
        }
        
        /**
         * Returns the value of the specified parameter.
         * 
         * @param parameter Name of the parameter.
         * 
         * @return Parameter's value, or null if not found.
         */
        public synchronized String get( String parameter )
        {
            // Error: no parameters, or parameter was null
            if( parameters == null || TextUtils.isEmpty( parameter ) )
                return null;
            
            ConfigParameter confParam = parameters.get( parameter );
            
            // Parameter not found
            if( confParam == null )
                return null;
            
            // Got it
            return confParam.value;
        }

        /**
         * Remove instances of this parameter.
         * @param parameter The parameter
         */
        private synchronized void removePreviousInstance(String parameter)
        {
            //Create a copy first
            ArrayList<String> keys = new ArrayList<>(parameters.keySet());

            //Remove from parameters
            for(String key : keys)
            {
                if(key.toLowerCase().equals(parameter.toLowerCase()))
                {
                    parameters.remove(key);
                }
            }

            //Remove from lines
            String lineParameterTest1 = parameter + " = ";
            String lineParameterTest2 = parameter + "=";
            Iterator<ConfigLine> iter = lines.iterator();
            while (iter.hasNext()) {
                ConfigLine line = iter.next();

                if(line.strLine.toLowerCase().startsWith(lineParameterTest1.toLowerCase()) ||
                        line.strLine.toLowerCase().startsWith(lineParameterTest2.toLowerCase()))
                {
                    iter.remove();
                }
            }
        }
        
        /**
         * Adds the specified parameter to this config section, updates the value if it already
         * exists, or removes the parameter.
         * 
         * @param parameter The name of the parameter.
         * @param value The parameter's value, or null to remove.
         */
        public synchronized  void put( String parameter, String value )
        {
            removePreviousInstance(parameter);

            if( !TextUtils.isEmpty( value ) )
            {
                ConfigParameter confParam = new ConfigParameter( parameter, value );
                lines.add( new ConfigLine( ConfigLine.LINE_PARAM, parameter + "=" + value
                        + "\n", confParam ) );
                parameters.put( parameter, confParam );
            }
        }
        
        /**
         * Writes the entire section to file.
         * 
         * @param fw File to write to.
         * 
         * @throws IOException if a writing error occurs.
         */
        public synchronized void save( FileWriter fw ) throws IOException
        {
            for( ConfigLine line : lines )
            {
                if( line != null ) {
                    line.save(fw);
                }
            }
        }
    }
    
    /**
     * The ConfigLine class stores each line of the config file (including comments).
     */
    private static class ConfigLine
    {
        static final int LINE_GARBAGE = 0; // Comment, whitespace, or blank line
        static final int LINE_SECTION = 1; // Section title
        static final int LINE_PARAM = 2; // Parameter=value pair
        
        int lineType; // LINE_GARBAGE, LINE_SECTION, or LINE_PARAM.
        String strLine; // Actual line from the config file.
        ConfigParameter confParam;
        
        /**
         * Constructor: Saves the relevant information about the line.
         * 
         * @param type The type of line.
         * @param line The line itself.
         * @param param Config parameters pertaining to the line.
         */
        ConfigLine( int type, String line, ConfigParameter param )
        {
            lineType = type;
            strLine = line;
            confParam = param;
        }
        
        /**
         * Saves the ConfigLine.
         * 
         * @param fw The file to save the ConfigLine to.
         * 
         * @throws IOException If a writing error occurs.
         */
        public synchronized void save( FileWriter fw ) throws IOException
        {
            int x;
            if( lineType == LINE_PARAM )
            {
                if( !strLine.contains( "=" ) || confParam == null )
                    return; // This shouldn't happen
                    
                x = strLine.indexOf( '=' );
                
                if( x < 1 )
                    return; // This shouldn't happen either
                    
                if( x < strLine.length() )
                    fw.write( strLine.substring( 0, x + 1 ) + confParam.value + "\n" );
            }
            else
            {
                fw.write( strLine );
            }
        }
    }
    
    /**
     * The ConfigParameter class associates a parameter with its value.
     */
    private static class ConfigParameter
    {
        @SuppressWarnings({"unused", "RedundantSuppression"})
        public String parameter;
        public String value;
        
        /**
         * Constructor: Associate the parameter and value
         * 
         * @param parameter The name of the parameter.
         * @param value The value of the parameter.
         */
        ConfigParameter( String parameter, String value )
        {
            this.parameter = parameter;
            this.value = value;
        }
    }
}
