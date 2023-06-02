package paulscode.android.mupen64plusae_mpn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import org.mupen64plusae_mpn.v3.alpha.R;

import paulscode.android.mupen64plusae_mpn.persistent.AppData;
import paulscode.android.mupen64plusae_mpn.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae_mpn.util.LegacyFilePicker;
import paulscode.android.mupen64plusae_mpn.util.LocaleContextWrapper;

public class ScanRomsActivity extends AppCompatActivity {

    private CheckBox mCheckBox1;
    private CheckBox mCheckBox2;
    private CheckBox mCheckBox3;
    private CheckBox mCheckBox4;

    private Uri mFileUri = null;

    private static final String URI_TO_IMPORT = "URI_TO_IMPORT";

    ActivityResultLauncher<Intent> mLaunchLegacyFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                    handleLegacyFileResult(data, true);
                }
            });

    ActivityResultLauncher<Intent> mLaunchLegacyFolderPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                    handleLegacyFileResult(data, false);
                }
            });

    ActivityResultLauncher<Intent> mLaunchFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                    handleSafFileResult(data, true);
                }
            });

    ActivityResultLauncher<Intent> mLaunchFolderPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                    handleSafFileResult(data, false);
                }
            });

    @Override
    protected void attachBaseContext(Context newBase) {
        if (TextUtils.isEmpty(LocaleContextWrapper.getLocalCode())) {
            super.attachBaseContext(newBase);
        } else {
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase, LocaleContextWrapper.getLocalCode()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String currentUri = null;

        if (savedInstanceState != null) {
            currentUri = savedInstanceState.getString(URI_TO_IMPORT);
        }

        if (currentUri != null) {
            mFileUri = Uri.parse(currentUri);
        }

        setContentView(R.layout.scan_roms_activity);

        Button folderPickerButton = findViewById(R.id.buttonFolderPicker);
        folderPickerButton.setOnClickListener(v -> startFolderPicker());
        Button filePickerButton = findViewById(R.id.buttonFilePicker);
        filePickerButton.setOnClickListener(v -> startFilePicker());

        AppData appData = new AppData(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && appData.useLegacyFileBrowser) {
            GlobalPrefs globalPrefs = new GlobalPrefs(this, appData);

            TextView textViewNoSaf = findViewById(R.id.textNoSafSupport);
            String text = getString(R.string.scanRomsDialog_no_saf) + " " + globalPrefs.externalRomsDirNoSaf;
            textViewNoSaf.setText(text);
        }

        // Set checkbox state
        mCheckBox1 = findViewById(R.id.checkBox1);
        mCheckBox2 = findViewById(R.id.checkBox2);
        mCheckBox3 = findViewById(R.id.checkBox3);
        mCheckBox4 = findViewById(R.id.checkBox4);
        mCheckBox1.setChecked(true);
        mCheckBox2.setChecked(true);
        mCheckBox3.setChecked(false);
        mCheckBox4.setChecked(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        if (mFileUri != null) {
            savedInstanceState.putString(URI_TO_IMPORT, mFileUri.toString());
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    private void startFolderPicker() {
        AppData appData = new AppData(this);
        if (appData.useLegacyFileBrowser) {
            startLegacyFolderPicker();
        } else {
            startSafFolderPicker();
        }
    }

    private void startFilePicker() {
        AppData appData = new AppData(this);
        if (appData.useLegacyFileBrowser) {
            startLegacyFilePicker();
        } else {
            startSafFilePicker();
        }
    }

    private void startLegacyFolderPicker() {
        Intent intent = new Intent(this, LegacyFilePicker.class);
        intent.putExtra(ActivityHelper.Keys.CAN_SELECT_FILE, false);
        intent.putExtra(ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, true);
        mLaunchLegacyFolderPicker.launch(intent);
    }

    private void startSafFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        );
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        mLaunchFolderPicker.launch(intent);
    }

    private void startLegacyFilePicker() {
        Intent intent = new Intent(this, LegacyFilePicker.class);
        intent.putExtra(ActivityHelper.Keys.CAN_SELECT_FILE, true);
        intent.putExtra(ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, true);
        mLaunchLegacyFilePicker.launch(intent);
    }

    private void startSafFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        mLaunchFilePicker.launch(intent);
    }

    private void handleSafFileResult(@NonNull Intent data, boolean isFile)
    {
        Intent scanData = new Intent();
        scanData.putExtra(ActivityHelper.Keys.SEARCH_ZIPS, mCheckBox1.isChecked());
        scanData.putExtra(ActivityHelper.Keys.DOWNLOAD_ART, mCheckBox2.isChecked());
        scanData.putExtra(ActivityHelper.Keys.CLEAR_GALLERY, mCheckBox3.isChecked());
        scanData.putExtra(ActivityHelper.Keys.SEARCH_SUBDIR, mCheckBox4.isChecked());

        // The result data contains a URI for the document or directory that
        // the user selected.
        mFileUri = data.getData();

        if (mFileUri != null) {
            scanData.putExtra(ActivityHelper.Keys.SEARCH_PATH, mFileUri.toString());
            scanData.putExtra(ActivityHelper.Keys.SEARCH_SINGLE_FILE, isFile);

            boolean takePermissionsSuccess = false;
            try {
                getContentResolver().takePersistableUriPermission(mFileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePermissionsSuccess = true;
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            if (takePermissionsSuccess) {
                setResult(RESULT_OK, scanData);
            } else {
                setResult(RESULT_CANCELED, null);
            }
            finish();
        }
    }

    private void handleLegacyFileResult(@NonNull Intent data, boolean isFile)
    {
        Intent scanData = new Intent();
        scanData.putExtra(ActivityHelper.Keys.SEARCH_ZIPS, mCheckBox1.isChecked());
        scanData.putExtra(ActivityHelper.Keys.DOWNLOAD_ART, mCheckBox2.isChecked());
        scanData.putExtra(ActivityHelper.Keys.CLEAR_GALLERY, mCheckBox3.isChecked());
        scanData.putExtra(ActivityHelper.Keys.SEARCH_SUBDIR, mCheckBox4.isChecked());

        // Check which request we're responding to
        final Bundle extras = data.getExtras();

        if (extras != null) {
            final String searchUri = extras.getString(ActivityHelper.Keys.SEARCH_PATH);

            mFileUri = Uri.parse(searchUri);
            scanData.putExtra(ActivityHelper.Keys.SEARCH_PATH, mFileUri.toString());
            scanData.putExtra(ActivityHelper.Keys.SEARCH_SINGLE_FILE, isFile);
            setResult(RESULT_OK, scanData);
            finish();
        }
    }
}
