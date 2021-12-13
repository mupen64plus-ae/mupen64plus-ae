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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.persistent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.documentfile.provider.DocumentFile;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceManager;
import android.text.TextUtils;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.game.GameActivity;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.LegacyFilePicker;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;

public class DataPrefsActivity extends AppCompatPreferenceActivity implements OnPreferenceClickListener,
    SharedPreferences.OnSharedPreferenceChangeListener
{
    public static final int FOLDER_PICKER_REQUEST_CODE = 1;
    public static final int LEGACY_FOLDER_PICKER_REQUEST_CODE = 2;
    private static final int LEGACY_FILE_PICKER_REQUEST_CODE = 3;
    public static final int FILE_PICKER_REQUEST_CODE = 4;

    // These constants must match the keys used in res/xml/preferences.xml
    private static final String SCREEN_ROOT = "screenRoot";

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;

    private SharedPreferences mPrefs = null;

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

        // Get app data and user preferences
        mAppData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs(this, mAppData);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load user preference menu structure from XML and update view
        addPreferencesFromResource(null, R.xml.preferences_data);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        refreshViews();
        mPrefs.registerOnSharedPreferenceChangeListener( this );
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );
    }

    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        // Handle the clicks on certain menu items that aren't actually
        // preferences
        final String key = preference.getKey();

        if (GlobalPrefs.PATH_GAME_SAVES.equals(key)) {
            startFolderPicker();
        } else if (GlobalPrefs.PATH_JAPAN_IPL_ROM.equals(key)) {
            startFilePicker();
        } else {// Let Android handle all other preference clicks
            return false;
        }

        // Saving just in case we can't when they reset
        if(getIntent() != null && getIntent().getBooleanExtra("gameRunning",false) &&
                !key.equals("gameAutoSaves")){
            Intent i = new Intent(GameActivity.RESET_BROADCAST_MESSAGE);
            i.putExtra("saveResetBroadcastMessage", true);
            sendBroadcast(i);
        }

        // Tell Android that we handled the click
        return true;
    }

    @Override
    protected void OnPreferenceScreenChange(String key)
    {
        // Handle certain menu items that require extra processing or aren't
        // actually preferences
        PrefUtil.setOnPreferenceClickListener(this, GlobalPrefs.PATH_GAME_SAVES, this);
        PrefUtil.setOnPreferenceClickListener(this, GlobalPrefs.PATH_JAPAN_IPL_ROM, this);


        Preference currentPreference = findPreference(GlobalPrefs.PATH_GAME_SAVES);
        if (currentPreference != null) {
            String uri = mGlobalPrefs.getString(GlobalPrefs.PATH_GAME_SAVES, "");

            if (!TextUtils.isEmpty(uri)) {
                DocumentFile file = FileUtil.getDocumentFileTree(this, Uri.parse(uri));
                currentPreference.setSummary(file.getName());
            }
        }

        currentPreference = findPreference(GlobalPrefs.PATH_JAPAN_IPL_ROM);
        if (currentPreference != null) {
            String uri = mGlobalPrefs.getString(GlobalPrefs.PATH_JAPAN_IPL_ROM, "");

            if (!TextUtils.isEmpty(uri)) {
                DocumentFile file = FileUtil.getDocumentFileSingle(this, Uri.parse(uri));
                currentPreference.setSummary(file == null ? "" : file.getName());
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        // Saving just in case we can't when they reset
        if(getIntent() != null && getIntent().getBooleanExtra("gameRunning",false) &&
                !key.equals("gameAutoSaves")){
            Intent i = new Intent(GameActivity.RESET_BROADCAST_MESSAGE);
            i.putExtra("saveResetBroadcastMessage", true);
            sendBroadcast(i);
        }

        refreshViews();
    }

    private void refreshViews()
    {
        PrefUtil.enablePreference(this, GlobalPrefs.PATH_GAME_SAVES,
                mPrefs.getString(GlobalPrefs.GAME_DATA_STORAGE_TYPE, "internal").equals("external"));
    }

    private void startFolderPicker()
    {
        Intent intent;
        int requestCode;
        if (mAppData.useLegacyFileBrowser) {
            intent = new Intent(this, LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, false );
            intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, false);
            requestCode = LEGACY_FOLDER_PICKER_REQUEST_CODE;
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION|
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            requestCode = FOLDER_PICKER_REQUEST_CODE;
        }
        startActivityForResult(intent, requestCode);
    }

    private void startFilePicker()
    {
        if (mAppData.useLegacyFileBrowser) {
            Intent intent = new Intent(this, LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, true );
            intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, true);
            startActivityForResult( intent, LEGACY_FILE_PICKER_REQUEST_CODE );
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
            startActivityForResult(intent, FILE_PICKER_REQUEST_CODE);
        }
    }

    @Override
    public void onBackPressed() {
        // Only when in game
        if (this.getIntent().getBooleanExtra("gameRunning", false)) {
            Intent intent = new Intent();
            intent.putExtra("maxAutoSaves", mGlobalPrefs.maxAutoSaves);

            setResult(RESULT_OK, intent);
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == FOLDER_PICKER_REQUEST_CODE) {
                // The result data contains a URI for the document or directory that
                // the user selected.
                if (data != null) {
                    Uri fileUri = data.getData();

                    Preference currentPreference = findPreference(GlobalPrefs.PATH_GAME_SAVES);
                    if (currentPreference != null && fileUri != null) {

                        getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                        DocumentFile file = FileUtil.getDocumentFileTree(this, fileUri);
                        String summary = file.getName();
                        currentPreference.setSummary(summary);
                        mGlobalPrefs.putString(GlobalPrefs.PATH_GAME_SAVES, fileUri.toString());
                    }
                }
            } else if (requestCode == LEGACY_FOLDER_PICKER_REQUEST_CODE) {
                final Bundle extras = data.getExtras();

                if (extras != null) {
                    final String searchUri = extras.getString(ActivityHelper.Keys.SEARCH_PATH);
                    Uri fileUri = Uri.parse(searchUri);
                    Preference currentPreference = findPreference(GlobalPrefs.PATH_GAME_SAVES);

                    if (currentPreference != null && fileUri != null) {
                        DocumentFile file = FileUtil.getDocumentFileTree(this, fileUri);
                        String summary = file.getName();
                        currentPreference.setSummary(summary);
                        mGlobalPrefs.putString(GlobalPrefs.PATH_GAME_SAVES, fileUri.toString());
                    }
                }
            } else if (requestCode == FILE_PICKER_REQUEST_CODE) {
                // The result data contains a URI for the document or directory that
                // the user selected.
                if (data != null) {
                    Uri fileUri = data.getData();

                    Preference currentPreference = findPreference(GlobalPrefs.PATH_JAPAN_IPL_ROM);
                    if (currentPreference != null && fileUri != null) {

                        getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        DocumentFile file = FileUtil.getDocumentFileSingle(this, fileUri);
                        String summary = file == null ? "" : file.getName();
                        currentPreference.setSummary(summary);
                        mGlobalPrefs.putString(GlobalPrefs.PATH_JAPAN_IPL_ROM, fileUri.toString());
                    }
                }
            }else if (requestCode == LEGACY_FILE_PICKER_REQUEST_CODE) {
                final Bundle extras = data.getExtras();

                if (extras != null) {
                    final String searchUri = extras.getString(ActivityHelper.Keys.SEARCH_PATH);
                    Uri fileUri = Uri.parse(searchUri);

                    Preference currentPreference = findPreference(GlobalPrefs.PATH_JAPAN_IPL_ROM);
                    if (currentPreference != null && fileUri != null && fileUri.getPath() != null) {
                        File file = new File(fileUri.getPath());
                        currentPreference.setSummary(file.getName());
                        mGlobalPrefs.putString(GlobalPrefs.PATH_JAPAN_IPL_ROM, fileUri.toString());
                    }
                }
            }
        }
    }
}
