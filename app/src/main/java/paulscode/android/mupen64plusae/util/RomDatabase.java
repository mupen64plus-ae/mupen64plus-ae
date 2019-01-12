/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.util;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;

/**
 * This class wraps the <a href=https://github.com/mupen64plus/mupen64plus-core/tree/master/data>ROM
 * database</a> maintained upstream. Upstream uses it for the following:
 * <ul>
 * <li>It provides an absolute identity for different rips of the same ROM (e.g. some rips are "bad"
 * or are hacked with cheats/bugfixes after they are ripped). Having an absolute identity of the rip
 * (via md5) is important when reproducing error reports.</li>
 * <li>It uses a consistent naming convention for the friendly (user-facing) name of each ROM. Again
 * useful when isolating and reproducing bugs from user reports.</li>
 * <li>It contains a bunch of additional meta info, like:
 * <ul>
 * <li>original method of saving data (SRAM, mempak, etc.) (critical to core operation)</li>
 * <li>number of players (minimize unnecessary controller polling)</li>
 * <li>rumble support a subjective rating of emulation quality (though I haven't seen this used
 * much)</li>
 * <li>game-specific tweaks (CountPerOp)</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * The ROM header does not provide all this extra information, nor does it sufficiently
 * differentiate rip variants. The header is also very inconsistent in the names. They are good
 * enough for development purposes, but not pretty enough to display to a user. Here are the header
 * names for some common games:
 * <ul>
 * <li>Banjo-Kazooie</li>
 * <li>BANJO TOOIE</li>
 * <li>CONKER BFD</li>
 * <li>Diddy Kong Racing</li>
 * <li>DONKEY KONG 64</li>
 * <li>GOLDENEYE</li>
 * <li>JET FORCE GEMINI</li>
 * <li>Kirby64</li>
 * <li>MARIOKART64</li>
 * <li>MarioParty</li>
 * <li>PAPER MARIO</li>
 * <li>Perfect Dark</li>
 * <li>SMASH BROTHERS</li>
 * <li>STAR WARS EP1 RACER</li>
 * <li>STARFOX64</li>
 * <li>SUPER MARIO 64</li>
 * <li>TUROK_DINOSAUR_HUNTE</li>
 * <li>Turok 2: Seeds of Ev</li>
 * <li>Turok 3: Shadow of O</li>
 * <li>THE LEGEND OF ZELDA</li>
 * <li>ZELDA MAJORA'S MASK</li>
 * </ul>
 * So we use the same database in the front-end, mostly for the same reasons:
 * <ul>
 * <li>Consistent, meaningful user-facing names that can also be used to determine the exact rip
 * variant from user reports</li>
 * <li>Consistency with upstream naming conventions, in case a user submits a bug report upstream</li>
 * <li>Number of players and rumble support is used in the front-end to present the proper menu
 * options to users (number of controllers in play menu, pak options in in-game menu)</li>
 * </ul>
 * We wrap the database info in a java class, which we use to hold additional derived meta-info,
 * like URLs for the cover art and wiki entries.
 * 
 * @see RomHeader
 * @see assets/mupen64plus_data/mupen64plus.ini
 */
public class RomDatabase
{
    private static final String ART_URL_TEMPLATE = "http://www.zurita.me/CoverArt/%s";
    private static final String WIKI_URL_TEMPLATE = "https://github.com/mupen64plus-ae/mupen64plus-ae-meta/wiki/%s";
    
    private ConfigFile mConfigFile = null;
    private final HashMap<String, ArrayList<ConfigSection>> mCrcMap = new HashMap<>();
    
    private static RomDatabase instance = null;
    private RomDatabase() {
       // Do not allow creation
    }
    public static RomDatabase getInstance() {
       if(instance == null) {
          instance = new RomDatabase();
       }
       return instance;
    }
    
    public void setDatabaseFile( String mupen64plusIni )
    {
        mConfigFile = new ConfigFile( mupen64plusIni );
        for( String key : mConfigFile.keySet() )
        {
            ConfigSection section = mConfigFile.get( key );
            if( section != null )
            {
                String crc = section.get( "CRC" );
                if( crc != null )
                {
                    if( mCrcMap.get( crc ) == null )
                        mCrcMap.put( crc, new ArrayList<ConfigSection>() );
                    mCrcMap.get( crc ).add( section );
                }
            }
        }
    }
    
    public boolean hasDatabaseFile()
    {
        return mConfigFile != null;
    }

    public RomDetail lookupByMd5WithFallback( String md5, String filename, String crc, CountryCode countryCode )
    {
        RomDetail detail = lookupByMd5( md5 );

        if( detail == null )
        {
            File tempFile = new File(filename);
            ArrayList<RomDetail> details = lookupByCrc(filename, crc, countryCode );

            // Catch if none was found or we could not narrow things to only 1 entry
            if (details.size() != 1) {
                detail = new RomDetail(crc, generateGoodNameFromFileName(tempFile.getName()));
            } else {
                detail = details.get(0);
            }
        }
        return detail;
    }

    private ArrayList<RomDetail> lookupByCrc(String fileName, String crc, CountryCode countryCode) {

        ArrayList<RomDetail> romDetails = new ArrayList<>();

        //First try to find a unique match
        ArrayList<ConfigSection> sections = mCrcMap.get( crc );
        if( sections != null ) {
            for( int i = 0; i < sections.size(); i++ )
                romDetails.add(new RomDetail( sections.get( i ) ));
        }

        if (romDetails.size() > 1) {
            ArrayList<RomDetail> romDetailsCountryFiltered = new ArrayList<>();

            // CRC in the database more than once;
            // Attempt to auto-select the correct match based on country code of rom
            for (RomDetail romDetail : romDetails) {

                if (romDetail.goodName.contains(countryCode.toString())) {
                    romDetailsCountryFiltered.add(romDetail);
                }
            }

            romDetails = romDetailsCountryFiltered;

        } else if (romDetails.size() == 0) {
            // CRC not in the database; create best guess
            Log.w("RomDetail", "No meta-info entry found for ROM " + fileName);
            Log.w("RomDetail", "CRC: " + crc);
            Log.i("RomDetail", "Constructing a best guess for the meta-info");

            File tempFile = new File(fileName);
            romDetails.add(new RomDetail(crc, generateGoodNameFromFileName(tempFile.getName())));
        }

        return romDetails;
    }

    private static String generateGoodNameFromFileName(String fileName)
    {
        int lastIndexOfPeriod = fileName.lastIndexOf('.');

        if(lastIndexOfPeriod != -1)
        {
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        }

        return fileName;
    }

    
    private RomDetail lookupByMd5( String md5 )
    {
        ConfigSection section = mConfigFile.get( md5 );
        return section == null ? null : new RomDetail( section );
    }
    
    public class RomDetail
    {
        public final String crc;
        public final String md5;
        public final String goodName;
        public final String baseName;
        public final String artName;
        public final String artUrl;
        public final String wikiUrl;
        final String saveType;
        public final int status;
        public final int players;
        public final boolean rumble;
        
        private RomDetail( ConfigSection section )
        {
            crc = section.get( "CRC" );
            md5 = section.name;
            
            // Use an empty goodname (not null) for certain homebrew ROMs
            if( "00000000 00000000".equals( crc ) )
                goodName = "";
            else
                goodName = section.get( "GoodName" );
            
            if( goodName != null )
            {
                // Extract basename (goodname without the extra parenthetical tags)
                baseName = goodName.split( " \\(" )[0].trim();
                
                // Generate the cover art URL string
                artName = baseName.replaceAll( "['\\.!]", "" ).replaceAll( "\\W+", "_" ) + ".png";
                artUrl = String.format( ART_URL_TEMPLATE, artName );
                
                // Generate wiki page URL string
                String _wikiUrl;
                _wikiUrl = String.format( WIKI_URL_TEMPLATE, baseName.replaceAll( " ", "_" ) );
                if( goodName.contains( "(Kiosk" ) )
                    _wikiUrl += "_(Kiosk_Demo)";
                wikiUrl = _wikiUrl;
            }
            else
            {
                Log.e( "RomDetail.ctor",
                        "mupen64plus.ini appears to be corrupt.  GoodName field is not defined for selected ROM." );
                baseName = null;
                artName = null;
                artUrl = null;
                wikiUrl = null;
            }
            
            // Some ROMs have multiple entries. Instead of duplicating common data, the ini file
            // just references another entry.
            String refMd5 = section.get( "RefMD5" );
            if( !TextUtils.isEmpty( refMd5 ) )
                section = mConfigFile.get( refMd5 );
            
            if( section != null )
            {
                saveType = section.get( "SaveType" );
                String statusString = section.get( "Status" );
                String playersString = section.get( "Players" );
                String rumbleString = section.get( "Rumble" );
                status = TextUtils.isEmpty( statusString ) ? 0 : Integer.parseInt( statusString );
                players = TextUtils.isEmpty( playersString ) ? 4 : Integer.parseInt( playersString );
                rumble = TextUtils.isEmpty( rumbleString ) || "Yes".equals( rumbleString );
            }
            else
            {
                Log.e( "RomDetail.ctor",
                        "mupen64plus.ini appears to be corrupt.  RefMD5 field does not refer to a known ROM." );
                saveType = null;
                status = 0;
                players = 4;
                rumble = true;
            }
        }
        
        private RomDetail( String assumedCrc, String assumedGoodName )
        {
            crc = assumedCrc;
            md5 = "";
            goodName = assumedGoodName;
            baseName = goodName.split( " \\(" )[0].trim();
            // Generate the cover art URL string
            artName = baseName.replaceAll( "['\\.!]", "" ).replaceAll( "\\W+", "_" ) + ".png";
            artUrl = String.format( ART_URL_TEMPLATE, artName );

            // Generate wiki page URL string
            String _wikiUrl;
            _wikiUrl = String.format( WIKI_URL_TEMPLATE, baseName.replaceAll( " ", "_" ) );
            if( goodName.contains( "(Kiosk" ) )
                _wikiUrl += "_(Kiosk_Demo)";
            wikiUrl = _wikiUrl;
            saveType = null;
            status = 0;
            players = 4;
            rumble = true;
        }
    }
}
