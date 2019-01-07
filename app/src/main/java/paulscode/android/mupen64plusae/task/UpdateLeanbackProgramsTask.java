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
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import androidx.tvprovider.media.tv.PreviewProgram;
import androidx.tvprovider.media.tv.TvContractCompat;
import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.GalleryActivity;
import paulscode.android.mupen64plusae.GalleryItem;
import paulscode.android.mupen64plusae.SplashActivity;
import paulscode.android.mupen64plusae.util.FileUtil;

public class UpdateLeanbackProgramsTask extends AsyncTask<Void, Void, String>
{
    private final WeakReference<Context> mContext;
    private final List<GalleryItem> mItems;
    private final long mChannelId;

    public UpdateLeanbackProgramsTask(Context context, List<GalleryItem> items, long channelId)
    {

        mContext = new WeakReference<>(context);
        mItems = items;
        mChannelId = channelId;
    }
    
    @Override
    protected String doInBackground( Void... params )
    {
        // Clear out the programs first
        mContext.get().getContentResolver().delete(
                TvContractCompat.buildPreviewProgramsUriForChannel(mChannelId),
                null, null);

        // Add recently played games to channel
        for (GalleryItem item : mItems) {
            Uri coverArtUri;
            int aspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_3_2;

            // Create cover art link
            if (!TextUtils.isEmpty(item.artPath) && new File(item.artPath).exists()) {
                coverArtUri = FileUtil.buildBanner(mContext.get(), item.artPath);

                // Determine aspect ratio
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(item.artPath, options);
                int width = options.outWidth;
                int height = options.outHeight;

                if (height > width)
                {
                    aspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_2_3;
                }

            } else {
                coverArtUri = FileUtil.resourceToUri(mContext.get(), R.drawable.default_coverart);
            }

            Intent gameIntent = new Intent(mContext.get(), SplashActivity.class);

            gameIntent.putExtra(GalleryActivity.KEY_IS_LEANBACK, true);
            gameIntent.putExtra(ActivityHelper.Keys.ROM_PATH, item.romFile != null ? item.romFile.getAbsolutePath() : null);
            gameIntent.putExtra(ActivityHelper.Keys.ZIP_PATH, item.zipFile != null ? item.zipFile.getAbsolutePath() : null);
            gameIntent.putExtra(ActivityHelper.Keys.ROM_MD5, item.md5);
            gameIntent.putExtra(ActivityHelper.Keys.ROM_CRC, item.crc);
            gameIntent.putExtra(ActivityHelper.Keys.ROM_HEADER_NAME, item.headerName);
            gameIntent.putExtra(ActivityHelper.Keys.ROM_COUNTRY_CODE, item.countryCode.getValue());
            gameIntent.putExtra(ActivityHelper.Keys.ROM_ART_PATH, item.artPath);
            gameIntent.putExtra(ActivityHelper.Keys.ROM_GOOD_NAME, item.goodName);

            PreviewProgram.Builder builder = new PreviewProgram.Builder();
            builder.setChannelId(mChannelId)
                    .setType(TvContractCompat.PreviewPrograms.TYPE_GAME)
                    .setTitle(item.goodName)
                    .setPosterArtUri(coverArtUri)
                    .setPosterArtAspectRatio(aspectRatio)
                    .setIntent(gameIntent);

            mContext.get().getContentResolver().insert(TvContractCompat.PreviewPrograms.CONTENT_URI,
                    builder.build().toContentValues());
        }

        return "";
    }

    @Override
    protected void onPostExecute( String result )
    {
        // Nothing to do here
    }
}