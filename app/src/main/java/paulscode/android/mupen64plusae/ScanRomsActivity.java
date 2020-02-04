package paulscode.android.mupen64plusae;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.util.LegacyFilePicker;

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
        filePickerButtont.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startFilePicker();
            }
        });
                
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
    public void onSaveInstanceState( Bundle savedInstanceState )
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
            Intent intent = new Intent(this, LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, false );
            startActivityForResult( intent, LEGACY_FILE_PICKER_REQUEST_CODE );
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent = Intent.createChooser(intent, getString(R.string.scanRomsDialog_selectRom));
            startActivityForResult(intent, PICK_FOLDER_REQUEST_CODE);
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
                    final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    getContentResolver().takePersistableUriPermission(mFileUri, takeFlags);

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
