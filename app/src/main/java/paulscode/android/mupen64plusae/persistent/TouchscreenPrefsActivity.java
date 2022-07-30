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
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import org.mupen64plusae.v3.alpha.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.LegacyFilePicker;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.RomHeader;

@SuppressWarnings("SameParameterValue")
public class TouchscreenPrefsActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener
{

    private static final String TAG = "TouchscreenPrefs";

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private SharedPreferences mPrefs = null;
    private Vibrator mVibrator = null;

    private static final String ACTION_IMPORT_TOUCHSCREEN_GRAPHICS = "actionImportTouchscreenGraphics";
    private static final int PICK_FILE_IMPORT_TOUCHSCREEN_GRAPHICS_REQUEST_CODE = 5;
    private static final int FEEDBACK_VIBRATE_TIME = 50;
    private final ArrayList<String> mValidSkinFiles = new ArrayList<>();

    ActivityResultLauncher<Intent> mLaunchFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                    Uri fileUri = getUri(data);
                    importCustomSkin(fileUri);
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

        // Get app data and user preferences
        mAppData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs(this, mAppData);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Vibrator to show on haptic feedback
        Vibrator vibrator;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) this.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) this.getSystemService( Context.VIBRATOR_SERVICE );
        }
        mVibrator = vibrator;

        mValidSkinFiles.add("analog-back.png");
        mValidSkinFiles.add("analog-fore.png");
        mValidSkinFiles.add("analog.png");
        mValidSkinFiles.add("buttonL-holdL.png");
        mValidSkinFiles.add("buttonL-mask.png");
        mValidSkinFiles.add("buttonL.png");
        mValidSkinFiles.add("buttonR-holdR.png");
        mValidSkinFiles.add("buttonR-mask.png");
        mValidSkinFiles.add("buttonR.png");
        mValidSkinFiles.add("buttonS-holdS.png");
        mValidSkinFiles.add("buttonS-mask.png");
        mValidSkinFiles.add("buttonS.png");
        mValidSkinFiles.add("buttonSen-holdSen.png");
        mValidSkinFiles.add("buttonSen-mask.png");
        mValidSkinFiles.add("buttonSen.png");
        mValidSkinFiles.add("buttonZ-holdZ.png");
        mValidSkinFiles.add("buttonZ-mask.png");
        mValidSkinFiles.add("buttonZ.png");
        mValidSkinFiles.add("dpad-mask.png");
        mValidSkinFiles.add("dpad.png");
        mValidSkinFiles.add("fps-0.png");
        mValidSkinFiles.add("fps-1.png");
        mValidSkinFiles.add("fps-2.png");
        mValidSkinFiles.add("fps-3.png");
        mValidSkinFiles.add("fps-4.png");
        mValidSkinFiles.add("fps-5.png");
        mValidSkinFiles.add("fps-6.png");
        mValidSkinFiles.add("fps-7.png");
        mValidSkinFiles.add("fps-8.png");
        mValidSkinFiles.add("fps-9.png");
        mValidSkinFiles.add("fps.png");
        mValidSkinFiles.add("groupAB-holdA.png");
        mValidSkinFiles.add("groupAB-holdB.png");
        mValidSkinFiles.add("groupAB-mask.png");
        mValidSkinFiles.add("groupAB.png");
        mValidSkinFiles.add("buttonA-holdA.png");
        mValidSkinFiles.add("buttonA-mask.png");
        mValidSkinFiles.add("buttonA.png");
        mValidSkinFiles.add("buttonB-holdB.png");
        mValidSkinFiles.add("buttonB-mask.png");
        mValidSkinFiles.add("buttonB.png");
        mValidSkinFiles.add("groupC-holdCd.png");
        mValidSkinFiles.add("groupC-holdCl.png");
        mValidSkinFiles.add("groupC-holdCr.png");
        mValidSkinFiles.add("groupC-holdCu.png");
        mValidSkinFiles.add("groupC-mask.png");
        mValidSkinFiles.add("groupC.png");
        mValidSkinFiles.add("buttonCr-holdCr.png");
        mValidSkinFiles.add("buttonCr-mask.png");
        mValidSkinFiles.add("buttonCr.png");
        mValidSkinFiles.add("buttonCl-holdCl.png");
        mValidSkinFiles.add("buttonCl-mask.png");
        mValidSkinFiles.add("buttonCl.png");
        mValidSkinFiles.add("buttonCd-holdCd.png");
        mValidSkinFiles.add("buttonCd-mask.png");
        mValidSkinFiles.add("buttonCd.png");
        mValidSkinFiles.add("buttonCu-holdCu.png");
        mValidSkinFiles.add("buttonCu-mask.png");
        mValidSkinFiles.add("buttonCu.png");
        mValidSkinFiles.add("skin.ini");
    }

    @Override
    protected String getSharedPrefsName() {
        return null;
    }

    @Override
    protected int getSharedPrefsId()
    {
        return R.xml.preferences_touchscreen;
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void OnPreferenceScreenChange(String key)
    {
        refreshViews();
        PrefUtil.setOnPreferenceClickListener(this, ACTION_IMPORT_TOUCHSCREEN_GRAPHICS, this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        // Just refresh the preference screens in place
        refreshViews();

        if(key.equals("touchscreenFeedback") && mGlobalPrefs.isTouchscreenFeedbackEnabled &&
                mVibrator != null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mVibrator.vibrate(VibrationEffect.createOneShot(FEEDBACK_VIBRATE_TIME, 100));
            } else {
                mVibrator.vibrate(FEEDBACK_VIBRATE_TIME);
            }
        }
    }

    private void refreshViews()
    {
        // Refresh the preferences object
        mGlobalPrefs = new GlobalPrefs(this, mAppData);

        PrefUtil.enablePreference(this, GlobalPrefs.KEY_TOUCHSCREEN_SKIN_CUSTOM_PATH,
                !TextUtils.isEmpty(mGlobalPrefs.touchscreenSkin) &&
                        mGlobalPrefs.touchscreenSkin.equals("Custom"));
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // Handle the clicks on certain menu items that aren't actually
        // preferences
        final String key = preference.getKey();

        if (ACTION_IMPORT_TOUCHSCREEN_GRAPHICS.equals(key)) {
            startFilePickerForSingle(PICK_FILE_IMPORT_TOUCHSCREEN_GRAPHICS_REQUEST_CODE, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {// Let Android handle all other preference clicks
            return false;
        }

        // Tell Android that we handled the click
        return true;
    }

    private boolean importCustomSkin(Uri uri) {

        RomHeader header = new RomHeader( getApplicationContext(), uri );

        if (!header.isZip) {
            Log.e(TAG, "Invalid custom skin file");
            Notifier.showToast(getApplicationContext(), R.string.importExportActivity_invalidCustomSkinFile);
            return false;
        }

        boolean validZip = true;

        ZipInputStream zipfile = null;

        try(ParcelFileDescriptor parcelFileDescriptor = getApplicationContext().getContentResolver().openFileDescriptor(uri, "r"))
        {
            if (parcelFileDescriptor != null) {
                zipfile = new ZipInputStream( new BufferedInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()) ));

                ZipEntry entry = zipfile.getNextEntry();

                while( entry != null && validZip)
                {
                    validZip = mValidSkinFiles.contains(entry.getName());

                    if (!validZip) {
                        Log.e(TAG, entry.getName());
                    }
                    entry = zipfile.getNextEntry();
                }
                zipfile.close();
            }
        }
        catch( Exception|OutOfMemoryError e )
        {
            Log.w(TAG, e);
        }
        finally
        {
            try {
                if( zipfile != null ) {
                    zipfile.close();
                }
            } catch (IOException ignored) {
            }
        }

        if (!validZip) {
            Notifier.showToast(getApplicationContext(), R.string.importExportActivity_invalidCustomSkinFile);
            Log.e(TAG, "Invalid custom skin zip");
            return false;
        }

        File customSkinDir = new File(mGlobalPrefs.touchscreenCustomSkinsDir);
        FileUtil.deleteFolder(customSkinDir);
        FileUtil.makeDirs(mGlobalPrefs.touchscreenCustomSkinsDir);
        FileUtil.unzipAll(getApplicationContext(), uri, mGlobalPrefs.touchscreenCustomSkinsDir);
        return true;
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Uri fileUri = getUri(data);

            if (requestCode == PICK_FILE_IMPORT_TOUCHSCREEN_GRAPHICS_REQUEST_CODE) {
                importCustomSkin(fileUri);
            }
        }
    }

    private void startFilePickerForSingle(int permissions)
    {
        AppData appData = new AppData( this );
        Intent intent;
        if (appData.useLegacyFileBrowser) {
            intent = new Intent(this, LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, true);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, true );
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(permissions);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        }

        mLaunchFilePicker.launch(intent);
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
