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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;

import paulscode.android.mupen64plusae.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.DownloadFromGoogleDriveFragment;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.LegacyFilePicker;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;
import paulscode.android.mupen64plusae.util.Notifier;

public class DataPrefsActivity extends AppCompatPreferenceActivity implements OnPreferenceClickListener,
    SharedPreferences.OnSharedPreferenceChangeListener
{
    public static final int GOOGLE_SIGNIN_REQUEST_CODE = 5;

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private SharedPreferences mPrefs = null;

    ActivityResultLauncher<Intent> mLaunchGameDataFolderPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {

                    Uri fileUri = getUri(data);

                    Preference currentPreference = findPreference(GlobalPrefs.PATH_GAME_SAVES);
                    if (currentPreference != null && fileUri != null) {

                        if (!mAppData.useLegacyFileBrowser) {
                            getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        }

                        DocumentFile file = FileUtil.getDocumentFileTree(this, fileUri);
                        String summary = file.getName();
                        currentPreference.setSummary(summary);
                        mGlobalPrefs.putString(GlobalPrefs.PATH_GAME_SAVES, fileUri.toString());
                    }
                }
            });

    ActivityResultLauncher<Intent> mLaunchIdlFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {

                    Uri fileUri = getUri(data);

                    Preference currentPreference = findPreference(GlobalPrefs.PATH_JAPAN_IPL_ROM);
                    if (currentPreference != null && fileUri != null) {

                        if (!mAppData.useLegacyFileBrowser) {
                            getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }

                        DocumentFile file = FileUtil.getDocumentFileSingle(this, fileUri);
                        String summary = file == null ? "" : file.getName();
                        currentPreference.setSummary(summary);
                        mGlobalPrefs.putString(GlobalPrefs.PATH_JAPAN_IPL_ROM, fileUri.toString());
                    }
                }
            });

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

    private static final String STATE_DOWNLOAD_FROM_GOOGLE_DRIVE_FRAGMENT = "STATE_DOWNLOAD_FROM_GOOGLE_DRIVE_FRAGMENT";
    DownloadFromGoogleDriveFragment mDownloadFromGoogleDriveFragment = null;

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
        mDownloadFromGoogleDriveFragment = (DownloadFromGoogleDriveFragment) fm.findFragmentByTag(STATE_DOWNLOAD_FROM_GOOGLE_DRIVE_FRAGMENT);

        if(mDownloadFromGoogleDriveFragment == null)
        {
            mDownloadFromGoogleDriveFragment = new DownloadFromGoogleDriveFragment();
            fm.beginTransaction().add(mDownloadFromGoogleDriveFragment, STATE_DOWNLOAD_FROM_GOOGLE_DRIVE_FRAGMENT).commit();
        }

        // Get app data and user preferences
        mAppData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs(this, mAppData);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected String getSharedPrefsName() {
        return null;
    }

    @Override
    protected int getSharedPrefsId()
    {
        // Load user preference menu structure from XML and update view
        if (mAppData.isPro) {
            return R.xml.preferences_data_pro;
        } else {
            return R.xml.preferences_data;
        }
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
        } else if (GlobalPrefs.SIGN_IN_TO_GOOGLE_DRIVE.equals(key)) {
            signInToGoogleDrive();
        } else if (GlobalPrefs.DOWNLOAD_GOOGLE_DRIVE_BACKUP.equals(key)) {
            downloadFromGoogleDrive();
        } else {
            // Let Android handle all other preference clicks
            return false;
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
        PrefUtil.setOnPreferenceClickListener(this, GlobalPrefs.SIGN_IN_TO_GOOGLE_DRIVE, this);
        PrefUtil.setOnPreferenceClickListener(this, GlobalPrefs.DOWNLOAD_GOOGLE_DRIVE_BACKUP, this);

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

        if (mAppData.useLegacyFileBrowser) {
            intent = new Intent(this, LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, false );
            intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, false);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION|
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        }
        mLaunchGameDataFolderPicker.launch(intent);
    }

    private void startFilePicker()
    {
        Intent intent;
        if (mAppData.useLegacyFileBrowser) {
            intent = new Intent(this, LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, true );
            intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, true);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        }
        mLaunchIdlFilePicker.launch(intent);
    }

    private void signInToGoogleDrive()
    {
        Scope driveFileScope = new Scope(Scopes.DRIVE_FILE);
        Scope emailScope = new Scope(Scopes.EMAIL);

        if (!GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(this),
                driveFileScope, emailScope)) {
            GoogleSignIn.requestPermissions(
                    this,
                    GOOGLE_SIGNIN_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    driveFileScope, emailScope);
        } else {
            Notifier.showToast( this, R.string.alreadyHaveGoogleDrivePermissions );
            Log.e("DataPrefs", "Already have permission");
        }
    }

    private void downloadFromGoogleDrive()
    {
        mDownloadFromGoogleDriveFragment.downloadFromGoogleDrive();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("DataPrefs", "onActivityResult, request_code=" + requestCode + "result=" + resultCode);

        if (resultCode == RESULT_OK) {

            if (requestCode == GOOGLE_SIGNIN_REQUEST_CODE) {
                Log.e("DataPrefs", "Sign in success");
            }
        }
    }
}
