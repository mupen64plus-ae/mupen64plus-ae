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
 * Authors:
 */
package paulscode.android.mupen64plusae.task;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import paulscode.android.mupen64plusae.GalleryItem;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.CountryCode;
import paulscode.android.mupen64plusae.util.RomHeader;

public class GalleryRefreshTask extends AsyncTask<Void, Void, String>
{
    public interface GalleryRefreshFinishedListener
    {
        void onGalleryRefreshFinished(List<GalleryItem> items, List<GalleryItem> recentItems);
    }

    private final GalleryRefreshFinishedListener mListener;
    private final GlobalPrefs mGlobalPrefs;
    private final WeakReference<Context> mContext;
    private final String mSearchQuery;
    private List<GalleryItem> mItems = new ArrayList<>();
    private List<GalleryItem> mRecentItems = new ArrayList<>();

    public GalleryRefreshTask(GalleryRefreshFinishedListener listener, Context context, GlobalPrefs globalPrefs,
                              String searchQuery)
    {
        mListener = listener;
        mContext = new WeakReference<>(context);
        mGlobalPrefs = globalPrefs;
        mSearchQuery = searchQuery;
    }
    
    @Override
    protected String doInBackground( Void... params )
    {
        generateGridItemsAndSaveConfig(mItems, mRecentItems);
        return "";
    }
    
    @Override
    protected void onPostExecute( String result )
    {
        mListener.onGalleryRefreshFinished( mItems, mRecentItems );
    }


    /**
     * Returns true if the given file is present in the provided list of items
     * @param items Item list to search
     * @param romFile ROM File to search for
     * @return True if it's present
     */
    private boolean isRomPathInItemList(List<GalleryItem> items, File romFile)
    {
        for (GalleryItem item : items) {
            if (item.romFile != null && item.romFile.getName().equals(romFile.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes old items that shouldn't be in the recents list any more and limits the recent list to 8 items
     * @param recentItems List of recent items to update
     */
    private void deleteOldItems(List<GalleryItem> recentItems) {

        if ( recentItems.size() != 0 ) {
            Collections.sort( recentItems, new GalleryItem.RecentlyPlayedComparator() );

            //Limit list to 8 items
            final int recentLimit = 8;
            if (recentItems.size() > recentLimit) {
                recentItems.subList(recentLimit, recentItems.size()).clear();
            }
        }

        // Delete extracted zip files not on this list
        List<File> extractedFiles = new ArrayList<>();
        File unzipRomsDir = new File(mGlobalPrefs.unzippedRomsDir);

        File[] files = unzipRomsDir.listFiles();

        if (files != null) {
            Collections.addAll(extractedFiles, files);

            for(File extractedFile : extractedFiles) {
                if (!isRomPathInItemList(recentItems, extractedFile)) {
                    if(!extractedFile.delete()) {
                        Log.w("GalleryActivity", "Unable to delete " + extractedFile.getPath());
                    }
                }
            }
        }
    }

    /**
     * Create a GallaryItem using a config file, md5, and good name
     * @param config Config file
     * @param md5 MD5 in config
     * @param goodName ROM goodname to use
     * @return A gallery item if one was created successfully.
     */
    private GalleryItem createGalleryItem(final ConfigFile config, String md5, String goodName)
    {
        GalleryItem item = null;
        final String romPath = config.get( md5, "romPath" );
        String zipPath = config.get( md5, "zipPath" );
        final String artFullPath = config.get( md5, "artPath" );

        //We get the file name to support the old gallery format
        String artPath = !TextUtils.isEmpty(artFullPath) ? new File(artFullPath).getName() : null;

        if(artPath != null)
            artPath = mGlobalPrefs.coverArtDir + "/" + artPath;

        String crc = config.get( md5, "crc" );
        String headerName = config.get( md5, "headerName" );
        final String countryCodeString = config.get( md5, "countryCode" );
        CountryCode countryCode = CountryCode.UNKNOWN;

        //We can't really do much if the rompath is null
        if (romPath != null)
        {
            if (countryCodeString != null)
            {
                countryCode = CountryCode.getCountryCode(Byte.parseByte(countryCodeString));
            }
            final String lastPlayedStr = config.get(md5, "lastPlayed");

            if (crc == null || headerName == null || countryCodeString == null)
            {
                final File file = new File(romPath);

                if (file.exists())
                {
                    final RomHeader header = new RomHeader(file);

                    crc = header.crc;
                    headerName = header.name;
                    countryCode = header.countryCode;

                    config.put(md5, "crc", crc);
                    config.put(md5, "headerName", headerName);
                    config.put(md5, "countryCode", Byte.toString(countryCode.getValue()));
                }
            }

            int lastPlayed = 0;
            if (lastPlayedStr != null)
                lastPlayed = Integer.parseInt(lastPlayedStr);

            item = new GalleryItem(mContext.get(), md5, crc, headerName, countryCode, goodName, romPath,
                    zipPath, artPath, lastPlayed, mGlobalPrefs.coverArtScale);
        }

        return item;
    }

    /**
     * This will populate a list of Gallery items and recent items
     * @param items Items will be populated here
     * @param recentItems Recent items will be populated here.
     */
    public void generateGridItemsAndSaveConfig(List<GalleryItem> items, List<GalleryItem> recentItems)
    {
        final ConfigFile config = new ConfigFile( mGlobalPrefs.romInfoCache_cfg );
        final String query = mSearchQuery.toLowerCase( Locale.US );
        String[] searches = null;
        if( query.length() > 0 )
            searches = query.split( " " );

        int currentTime = 0;

        if( mGlobalPrefs.isRecentShown )
        {
            currentTime = (int) ( new Date().getTime() / 1000 );
        }

        for( final String md5 : config.keySet() )
        {
            if( !ConfigFile.SECTIONLESS_NAME.equals( md5 ) )
            {
                final ConfigFile.ConfigSection section = config.get( md5 );
                String goodName;
                if( mGlobalPrefs.isFullNameShown || !section.keySet().contains( "baseName" ) )
                    goodName = section.get( "goodName" );
                else
                    goodName = section.get( "baseName" );

                boolean matchesSearch = true;
                if( searches != null && searches.length > 0 && goodName != null)
                {
                    // Make sure the ROM name contains every token in the query
                    final String lowerName = goodName.toLowerCase( Locale.US );
                    for( final String search : searches )
                    {
                        if( search.length() > 0 && !lowerName.contains( search ) )
                        {
                            matchesSearch = false;
                            break;
                        }
                    }
                }

                if( matchesSearch && goodName != null)
                {
                    GalleryItem item = createGalleryItem(config, md5, goodName);

                    if (item != null) {
                        items.add(item);
                        boolean isNotOld = currentTime - item.lastPlayed <= 60 * 60 * 24 * 7; // 7 days
                        if (recentItems != null && mGlobalPrefs.isRecentShown && isNotOld )
                        {
                            recentItems.add(item);
                        }
                    }
                }
            }
        }

        config.save();

        Collections.sort( items, mGlobalPrefs.sortByRomName ?
                new GalleryItem.NameComparator() : new GalleryItem.RomFileComparator() );

        //Don't delete any items when srarching
        if (searches == null) {
            deleteOldItems(recentItems);
        }
    }

}