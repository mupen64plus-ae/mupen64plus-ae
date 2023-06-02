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
 * Authors: fzurita
 */
package paulscode.android.mupen64plusae-mpn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import org.mupen64plusae-mpn.v3.alpha.R;

import java.io.File;

import paulscode.android.mupen64plusae-mpn.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae-mpn.persistent.AppData;
import paulscode.android.mupen64plusae-mpn.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae-mpn.preference.PrefUtil;
import paulscode.android.mupen64plusae-mpn.util.FileUtil;
import paulscode.android.mupen64plusae-mpn.util.LegacyFilePicker;
import paulscode.android.mupen64plusae-mpn.util.LocaleContextWrapper;
import paulscode.android.mupen64plusae-mpn.util.Notifier;

public class ImportExportActivity extends AppCompatPreferenceActivity implements Preference.OnPreferenceClickListener
{
    // These constants must match the keys used in res/xml/preferences.xml
    private static final String ACTION_EXPORT_GAME_DATA = "actionExportGameData";
    private static final String ACTION_EXPORT_CHEATS_AND_PROFILES = "actionExportCheatsAndProfiles";
    private static final String ACTION_EXPORT_EXTRACTED_TEXTURES = "actionExportExtractedTextures";
    private static final String ACTION_IMPORT_GAME_DATA = "actionImportGameData";
    private static final String ACTION_IMPORT_CHEATS_AND_PROFILES = "actionImportCheatsAndProfiles";

    private static final String STATE_COPY_TO_SD_FRAGMENT= "STATE_COPY_TO_SD_FRAGMENT";
    private CopyToSdFragment mCopyToSdFragment = null;
    private static final String STATE_COPY_FROM_SD_FRAGMENT= "STATE_COPY_FROM_SD_FRAGMENT";
    private CopyFromSdFragment mCopyFromSdFragment = null;

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;

    ActivityResultLauncher<Intent> mLaunchExportGameDataFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                    Uri fileUri = getUri(data);
                    mCopyToSdFragment.copyToSd(new File(mAppData.gameDataDir), fileUri);
                }
            });

    ActivityResultLauncher<Intent> mLaunchExportCheatsAndProfilesFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                    Uri fileUri = getUri(data);
                    mCopyToSdFragment.copyToSd(new File(mGlobalPrefs.profilesDir), fileUri);
                }
            });

    ActivityResultLauncher<Intent> mLaunchExportDumpedTexturesFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                    Uri fileUri = getUri(data);
                    mCopyToSdFragment.copyToSd(new File(mGlobalPrefs.textureDumpDir), fileUri);
                }
            });

    ActivityResultLauncher<Intent> mLaunchImportGameDataFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                    Uri fileUri = getUri(data);
                    DocumentFile sourceLocation = FileUtil.getDocumentFileTree(getApplicationContext(), fileUri);
                    File destination = new File(mAppData.gameDataDir);
                    if (sourceLocation.getName() != null && sourceLocation.getName().equals(destination.getName())) {
                        mCopyFromSdFragment.copyFromSd(fileUri, new File(mAppData.gameDataDir));
                    } else {
                        Notifier.showToast( this, R.string.importExportActivity_invalidGameDataFolder );
                    }
                }
            });

    ActivityResultLauncher<Intent> mLaunchImportCheatsAndProfilesFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                    Uri fileUri = getUri(data);
                    DocumentFile sourceLocation = FileUtil.getDocumentFileTree(getApplicationContext(), fileUri);
                    File destination = new File(mGlobalPrefs.profilesDir);
                    if (sourceLocation.getName() != null && sourceLocation.getName().equals(destination.getName())) {
                        mCopyFromSdFragment.copyFromSd(fileUri, new File(mGlobalPrefs.profilesDir));
                    } else {
                        Notifier.showToast( this, R.string.importExportActivity_invalidProfilesFolder );
                    }
                }
            });

    @Override
    protected void attachBaseContext(Context newBase) {
        if(TextUtils.isEmpty(LocaleContextWrapper.getLocalCode()))
        {
            super.attachBaseContext(newBase);
        }
        else
        {
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase,LocaleContextWrapper.getLocalCode()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getSupportFragmentManager();
        mCopyToSdFragment = (CopyToSdFragment) fm.findFragmentByTag(STATE_COPY_TO_SD_FRAGMENT);

        if(mCopyToSdFragment == null)
        {
            mCopyToSdFragment = new CopyToSdFragment();
            fm.beginTransaction().add(mCopyToSdFragment, STATE_COPY_TO_SD_FRAGMENT).commit();
        }

        mCopyFromSdFragment = (CopyFromSdFragment) fm.findFragmentByTag(STATE_COPY_FROM_SD_FRAGMENT);

        if(mCopyFromSdFragment == null)
        {
            mCopyFromSdFragment = new CopyFromSdFragment();
            fm.beginTransaction().add(mCopyFromSdFragment, STATE_COPY_FROM_SD_FRAGMENT).commit();
        }

        // Get app data and user preferences
        mAppData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs(this, mAppData);

        PreferenceManager.setDefaultValues( this, R.xml.import_export_data, false );

        // Refresh the preference data wrapper
        mGlobalPrefs = new GlobalPrefs(this, mAppData);
    }

    @Override
    protected String getSharedPrefsName() {
        return null;
    }

    @Override
    protected int getSharedPrefsId()
    {
        return R.xml.import_export_data;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void OnPreferenceScreenChange(String key)
    {
        PrefUtil.setOnPreferenceClickListener(this, ACTION_EXPORT_GAME_DATA, this);
        PrefUtil.setOnPreferenceClickListener(this, ACTION_EXPORT_CHEATS_AND_PROFILES, this);
        PrefUtil.setOnPreferenceClickListener(this, ACTION_EXPORT_EXTRACTED_TEXTURES, this);
        PrefUtil.setOnPreferenceClickListener(this, ACTION_IMPORT_GAME_DATA, this);
        PrefUtil.setOnPreferenceClickListener(this, ACTION_IMPORT_CHEATS_AND_PROFILES, this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        // Handle the clicks on certain menu items that aren't actually
        // preferences
        final String key = preference.getKey();

        switch (key) {
            case ACTION_EXPORT_GAME_DATA:
                startFilePicker(mLaunchExportGameDataFilePicker, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, false);
                break;
            case ACTION_EXPORT_CHEATS_AND_PROFILES:
                startFilePicker(mLaunchExportCheatsAndProfilesFilePicker, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, false);
                break;
            case ACTION_EXPORT_EXTRACTED_TEXTURES:
                startFilePicker(mLaunchExportDumpedTexturesFilePicker, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, false);
                break;
            case ACTION_IMPORT_GAME_DATA:
                startFilePicker(mLaunchImportGameDataFilePicker, Intent.FLAG_GRANT_READ_URI_PERMISSION, true);
                break;
            case ACTION_IMPORT_CHEATS_AND_PROFILES:
                startFilePicker(mLaunchImportCheatsAndProfilesFilePicker, Intent.FLAG_GRANT_READ_URI_PERMISSION, true);
                break;
            default:
                // Let Android handle all other preference clicks
                return false;
        }

        // Tell Android that we handled the click
        return true;
    }

    private void startFilePicker(ActivityResultLauncher<Intent> launcher,
                                 int permissions, boolean canViewExtStorage)
    {
        AppData appData = new AppData( this );
        Intent intent;
        if (appData.useLegacyFileBrowser) {
            intent = new Intent(this, LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, false );
            intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, canViewExtStorage);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(permissions);
            intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        }

        launcher.launch(intent);
    }

    private Uri getUri(Intent data)
    {
        AppData appData = new AppData( this );
        Uri returnValue = null;
        if (appData.useLegacyFileBrowser) {
            final Bundle extras = data.getExtras();

            if (extras != null) {
                final String searchUri = extras.getString(ActivityHelper.Keys.SEARCH_PATH);
                returnValue = Uri.parse(searchUri);
            }
        } else {
            returnValue = data.getData();
        }

        return returnValue;
    }
}
