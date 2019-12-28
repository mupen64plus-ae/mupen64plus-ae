package paulscode.android.mupen64plusae;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import org.mupen64plusae.v3.alpha.R;

public class ScanRomsActivity extends AppCompatActivity
{
    public static final int PICK_FOLDER_REQUEST_CODE = 2;

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

        Button cancelButton = findViewById(R.id.buttonCancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ScanRomsActivity.this.setResult(RESULT_CANCELED, null);
                ScanRomsActivity.this.finish();
            }
        });
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
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent = Intent.createChooser(intent, getString(R.string.scanRomsDialog_selectRom));
        startActivityForResult(intent, PICK_FOLDER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check which request we're responding to
        if (requestCode == PICK_FOLDER_REQUEST_CODE)
        {
            if (resultCode == RESULT_OK) {

                // The result data contains a URI for the document or directory that
                // the user selected.
                if (data != null) {
                    mFileUri = data.getData();

                    final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    getContentResolver().takePersistableUriPermission(mFileUri, takeFlags);

                    Intent scanData = new Intent();
                    scanData.putExtra(ActivityHelper.Keys.SEARCH_PATH, mFileUri.toString());
                    scanData.putExtra(ActivityHelper.Keys.SEARCH_ZIPS, mCheckBox1.isChecked());
                    scanData.putExtra(ActivityHelper.Keys.DOWNLOAD_ART, mCheckBox2.isChecked());
                    scanData.putExtra(ActivityHelper.Keys.CLEAR_GALLERY, mCheckBox3.isChecked());
                    scanData.putExtra(ActivityHelper.Keys.SEARCH_SUBDIR, mCheckBox4.isChecked());
                    ScanRomsActivity.this.setResult(RESULT_OK, scanData);

                    ScanRomsActivity.this.finish();
                }
            }
        }
    }
}
