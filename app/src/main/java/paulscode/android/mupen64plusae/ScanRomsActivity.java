package paulscode.android.mupen64plusae;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.LegacyFilePicker;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;

public class ScanRomsActivity extends AppCompatActivity
{
    private static final int LEGACY_FILE_PICKER_REQUEST_CODE = 1;
    private static final int PICK_FOLDER_REQUEST_CODE = 2;

    private CheckBox mCheckBox1;
    private CheckBox mCheckBox2;
    private CheckBox mCheckBox3;
    private CheckBox mCheckBox4;

    private Uri mFileUri = null;

    private static final String URI_TO_IMPORT = "URI_TO_IMPORT";

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
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        String currentUri = null;

        if(savedInstanceState != null)
        {
            currentUri = savedInstanceState.getString( URI_TO_IMPORT );
        }

        if (currentUri != null) {
            mFileUri = Uri.parse(currentUri);
        }

        setContentView(R.layout.scan_roms_activity);

        Button filePickerButtont = findViewById(R.id.buttonFilePicker);
        filePickerButtont.setOnClickListener(v -> startFilePicker());

        AppData appData = new AppData( this );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && appData.useLegacyFileBrowser) {
            GlobalPrefs globalPrefs = new GlobalPrefs( this, appData );

            TextView textViewNoSaf = findViewById(R.id.textNoSafSupport);
            String text = getString(R.string.scanRomsDialog_no_saf) + " " + globalPrefs.externalRomsDirNoSaf;
            textViewNoSaf.setText(text);
        }
                
        // Set checkbox state
        mCheckBox1 = findViewById( R.id.checkBox1 );
        mCheckBox2 = findViewById( R.id.checkBox2 );
        mCheckBox3 = findViewById( R.id.checkBox3 );
        mCheckBox4 = findViewById( R.id.checkBox4 );
        mCheckBox1.setChecked( true );
        mCheckBox2.setChecked( true );
        mCheckBox3.setChecked( false );
        mCheckBox4.setChecked( true );
    }
    
    @Override
    public void onSaveInstanceState( @NonNull Bundle savedInstanceState )
    {
        if (mFileUri != null) {
            savedInstanceState.putString( URI_TO_IMPORT, mFileUri.toString() );
        }
        super.onSaveInstanceState( savedInstanceState );
    }

    private void startFilePicker()
    {
        AppData appData = new AppData( this );
        if (appData.useLegacyFileBrowser) {
            startLegacyFilePicker();
        } else {
            startSafFilePicker();
        }
    }

    private void startLegacyFilePicker()
    {
        Intent intent = new Intent(this, LegacyFilePicker.class);
        intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, false );
        intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, true);
        startActivityForResult( intent, LEGACY_FILE_PICKER_REQUEST_CODE );
    }

    private void startSafFilePicker()
    {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                    );
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            startActivityForResult(intent, PICK_FOLDER_REQUEST_CODE);
        } catch (android.content.ActivityNotFoundException e) {
            startLegacyFilePicker();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {

            Intent scanData = new Intent();
            scanData.putExtra(ActivityHelper.Keys.SEARCH_ZIPS, mCheckBox1.isChecked());
            scanData.putExtra(ActivityHelper.Keys.DOWNLOAD_ART, mCheckBox2.isChecked());
            scanData.putExtra(ActivityHelper.Keys.CLEAR_GALLERY, mCheckBox3.isChecked());
            scanData.putExtra(ActivityHelper.Keys.SEARCH_SUBDIR, mCheckBox4.isChecked());

            // Check which request we're responding to
            if (requestCode == PICK_FOLDER_REQUEST_CODE)
            {
                // The result data contains a URI for the document or directory that
                // the user selected.
                mFileUri = data.getData();

                if (mFileUri != null) {
                    scanData.putExtra(ActivityHelper.Keys.SEARCH_PATH, mFileUri.toString());

                    try {
                        getContentResolver().takePersistableUriPermission(mFileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }

                    ScanRomsActivity.this.setResult(RESULT_OK, scanData);
                    ScanRomsActivity.this.finish();
                }
            } else if (requestCode == LEGACY_FILE_PICKER_REQUEST_CODE) {
                final Bundle extras = data.getExtras();

                if (extras != null) {
                    final String searchUri = extras.getString(ActivityHelper.Keys.SEARCH_PATH);

                    mFileUri = Uri.parse(searchUri);
                    scanData.putExtra(ActivityHelper.Keys.SEARCH_PATH, mFileUri.toString());
                    ScanRomsActivity.this.setResult(RESULT_OK, scanData);
                    ScanRomsActivity.this.finish();
                }
            }
        }
    }
}
