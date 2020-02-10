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
import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import paulscode.android.mupen64plusae.GalleryItem;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.CountryCode;
import paulscode.android.mupen64plusae.util.FileUtil;

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
    private ConfigFile mConfig;

    public GalleryRefreshTask(GalleryRefreshFinishedListener listener, Context context, GlobalPrefs globalPrefs,
                              String searchQuery, ConfigFile config)
    {
        mListener = listener;
        mContext = new WeakReference<>(context);
        mGlobalPrefs = globalPrefs;
        mSearchQuery = searchQuery;
        mConfig = config;
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
     * Limits the recent list to 12 items
     * @param recentItems List of recent items to update
     */
    private void deleteOldItems(List<GalleryItem> recentItems) {

        if ( recentItems.size() != 0 ) {
            Collections.sort( recentItems, new GalleryItem.RecentlyPlayedComparator() );

            //Limit list to 12 items
            final int recentLimit = 12;
            if (recentItems.size() > recentLimit) {
                recentItems.subList(recentLimit, recentItems.size()).clear();
            }
        }
    }

    /** Tries the key normally in use. If it doesn't exist, try the new and convert the value to a URI
     *
     * @param config Config file
     * @param md5 MD5
     * @param key Key currently in use
     * @param alternateKey Aleternate legacy key
     * @return URI string from key or alternate key if key doesn't exist
     */
    private String getUriString(final ConfigFile config, String md5, String key, String alternateKey)
    {
        String path = config.get( md5, key);

        // If the above doesn't exist, try the legacy path
        if (TextUtils.isEmpty(path)) {
            String pathString = config.get( md5, alternateKey);
            if (!TextUtils.isEmpty(pathString)) {
                path = Uri.fromFile(new File(pathString)).toString();
            }
        }

        return path;
    }

    /**
     * Create a GallaryItem using a config file, md5, and good name
     * @param config Config file
     * @param md5 MD5 in config
     * @param displayName Text to display for this ROM
     * @return A gallery item if one was created successfully.
     */
    private GalleryItem createGalleryItem(final ConfigFile config, String md5, String displayName)
    {
        GalleryItem item = null;

        String romPath = getUriString(config, md5, "romPathUri", "romPath");
        String zipPath = getUriString(config, md5, "zipPathUri", "zipPath");

        // We only want the file name if Zip Path exists
        if (!TextUtils.isEmpty(zipPath)) {
            romPath = new File(romPath).getName();
            try {
                romPath = java.net.URLDecoder.decode(romPath, "UTF-8");
            } catch (UnsupportedEncodingException|java.lang.IllegalArgumentException e) {
                Log.e("GalleryRefreshTask", "Unable to decode string: " + romPath);
                return null;
            }
        }

        final String artFullPath = config.get( md5, "artPath" );
        final String goodName = config.get( md5, "goodName" );

        //We get the file name to support the old gallery format
        String artPath = !TextUtils.isEmpty(artFullPath) ? new File(artFullPath).getName() : null;

        if(artPath != null)
            artPath = mGlobalPrefs.coverArtDir + "/" + artPath;

        String crc = config.get( md5, "crc" );
        String headerName = config.get( md5, "headerName" );
        final String countryCodeString = config.get( md5, "countryCode" );
        CountryCode countryCode = CountryCode.UNKNOWN;

        if (countryCodeString != null)
        {
            countryCode = CountryCode.getCountryCode(Byte.parseByte(countryCodeString));
        }
        final String lastPlayedStr = config.get(md5, "lastPlayed");

        int lastPlayed = 0;
        if (lastPlayedStr != null)
            lastPlayed = Integer.parseInt(lastPlayedStr);

        // Some BETA ROMs don't have headers
        if (headerName == null)
            headerName = goodName;

        if (crc != null && countryCodeString != null)
        {
            item = new GalleryItem(mContext.get(), md5, crc, headerName, countryCode, goodName, displayName, romPath,
                    zipPath, artPath, lastPlayed, mGlobalPrefs.coverArtScale);
        }
        return item;
    }

    /**
     * This will populate a list of Gallery items and recent items
     * @param items Items will be populated here
     * @param recentItems Recent items will be populated here.
     */
    private void generateGridItemsAndSaveConfig(List<GalleryItem> items, @NonNull List<GalleryItem> recentItems)
    {
        final String query = mSearchQuery.toLowerCase( Locale.US );
        String[] searches = null;
        if( query.length() > 0 )
            searches = query.split( " " );

        for ( final String md5 : mConfig.keySet() ) {
            if ( !ConfigFile.SECTIONLESS_NAME.equals( md5 ) ) {
                final ConfigFile.ConfigSection section = mConfig.get( md5 );

                String romPath = getUriString(mConfig, md5, "romPathUri", "romPath");

                // We can't do much with an invalid Rom path
                if (romPath != null) {

                    String displayName = "";
                    if (mGlobalPrefs.sortByRomName) {
                        if( mGlobalPrefs.isFullNameShown || !section.keySet().contains( "baseName" ) )
                            displayName = section.get( "goodName" );
                        else
                            displayName = section.get( "baseName" );
                    } else {

                        if (mContext.get() != null) {
                            DocumentFile file = FileUtil.getDocumentFileSingle(mContext.get(), Uri.parse(romPath));
                            displayName = file.getName();
                        }

                        if (TextUtils.isEmpty(displayName)) {
                            displayName = romPath;
                        }
                    }

                    boolean matchesSearch = true;
                    if ( searches != null && searches.length > 0 && !TextUtils.isEmpty(displayName)) {
                        // Make sure the ROM name contains every token in the query
                        final String lowerName = displayName.toLowerCase( Locale.US );
                        for ( final String search : searches ) {
                            if ( search.length() > 0 && !lowerName.contains( search ) ) {
                                matchesSearch = false;
                                break;
                            }
                        }
                    }

                    if ( matchesSearch && displayName != null) {
                        GalleryItem item = createGalleryItem(mConfig, md5, displayName);

                        if (item != null && (mGlobalPrefs.getAllowedCountryCodes().contains(item.countryCode) ||
                                searches != null)) {

                            items.add(item);
                            if (item.lastPlayed != 0) {
                                recentItems.add(item);
                            }
                        }
                    }
                }
            }
        }

        Collections.sort( items, mGlobalPrefs.sortByRomName ?
                new GalleryItem.NameComparator() : new GalleryItem.RomFileComparator() );

        //Don't delete any items when searching
        if (searches == null) {
            deleteOldItems(recentItems);
        }
    }

}