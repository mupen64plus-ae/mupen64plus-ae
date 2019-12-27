package paulscode.android.mupen64plusae;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.ProviderUtil;

public class ScanRomsActivity extends AppCompatActivity implements OnItemClickListener
{
    public static final int PICK_FOLDER_REQUEST_CODE = 2;

    private List<String> mPaths;
    private CheckBox mCheckBox1;
    private CheckBox mCheckBox2;
    private CheckBox mCheckBox3;
    private CheckBox mCheckBox4;

    private File mCurrentPath = null;
    private Uri mFileUri = null;
    private SharedPreferences mPrefs = null;

    private static final String URI_TO_IMPORT = "URI_TO_IMPORT";
    private static final String CURRENT_PATH = "CURRENT_PATH";
    private static final String ROM_SCAN_START_PATH = "RomScanStartPath";
 
    @Override
    // The following is needed for Environment.getExternalStorageDirectory() This is still needed for Android 4.4
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences( this );


        if (AppData.IS_LOLLIPOP) {
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
        } else {
            setContentView(R.layout.scan_roms_activity_kitkat);

            Button resetButton = findViewById(R.id.buttonReset);
            resetButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mCurrentPath = new File( Environment.getExternalStorageDirectory().getAbsolutePath() );
                    PopulateFileList();
                }
            });

            String currentPath = null;

            if(savedInstanceState != null)
            {
                currentPath = savedInstanceState.getString( CURRENT_PATH );
            }

            if( currentPath != null )
            {
                mCurrentPath = new File(currentPath);
            }
            else
            {
                String romScanStartPath = mPrefs.getString(ROM_SCAN_START_PATH, null);

                if(romScanStartPath == null || !new File(romScanStartPath).exists())
                {
                    // Pick the root of the storage directory by default
                    mCurrentPath = new File( Environment.getExternalStorageDirectory().getAbsolutePath() );
                }
                //Else use saved directory
                else
                {
                    mCurrentPath = new File( romScanStartPath );

                    if(mCurrentPath.isFile())
                    {
                        mCurrentPath = mCurrentPath.getParentFile();
                    }
                }
            }

            PopulateFileList();

            Button okButton = findViewById(R.id.buttonOk);
            okButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent data = new Intent();
                    data.putExtra(ActivityHelper.Keys.SEARCH_PATH, mCurrentPath.getPath());
                    data.putExtra(ActivityHelper.Keys.SEARCH_ZIPS, mCheckBox1.isChecked());
                    data.putExtra(ActivityHelper.Keys.DOWNLOAD_ART, mCheckBox2.isChecked());
                    data.putExtra(ActivityHelper.Keys.CLEAR_GALLERY, mCheckBox3.isChecked());
                    data.putExtra(ActivityHelper.Keys.SEARCH_SUBDIR, mCheckBox4.isChecked());
                    ScanRomsActivity.this.setResult(RESULT_OK, data);

                    //Save the selected directory
                    mPrefs.edit().putString( ROM_SCAN_START_PATH, mFileUri.toString() ).apply();
                    ScanRomsActivity.this.finish();
                }
            });
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

        Button cancelButton = findViewById(R.id.buttonCancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ScanRomsActivity.this.setResult(RESULT_CANCELED, null);
                ScanRomsActivity.this.finish();
            }
        });
    }
    
    @Override
    // The following is needed for Environment.getExternalStorageDirectory() This is still needed for Android 4.4
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public void onSaveInstanceState( Bundle savedInstanceState )
    {
        if( mCurrentPath != null )
            savedInstanceState.putString( CURRENT_PATH, mCurrentPath.getAbsolutePath() );

        if (mFileUri != null) {
            savedInstanceState.putString( URI_TO_IMPORT, mFileUri.toString() );
        }
        super.onSaveInstanceState( savedInstanceState );
    }
    
    private void PopulateFileList()
    {
        setTitle( mCurrentPath.getPath() );
        // Populate the file list
        // Get the filenames and absolute paths
        List<CharSequence> names = new ArrayList<>();
        mPaths = new ArrayList<>();
        FileUtil.populate( mCurrentPath, true, true, true, names, mPaths );

        if(mCurrentPath.isDirectory())
        {
            ListView listView1 = findViewById( R.id.listView1 );
            ArrayAdapter<String> adapter = Prompt.createFilenameAdapter( this, mPaths, names);
            listView1.setAdapter( adapter );
            listView1.setOnItemClickListener( this );   
        }
    }

    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id )
    {
        if(position < mPaths.size())
        {
            mCurrentPath = new File(mPaths.get( position ));
            mFileUri = Uri.fromFile(mCurrentPath);
            PopulateFileList();
        }
    }

    private void startFilePicker()
    {
        if (AppData.IS_LOLLIPOP) {
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
                    scanData.putExtra(ActivityHelper.Keys.SEARCH_PATH, mCurrentPath.getPath());
                    scanData.putExtra(ActivityHelper.Keys.SEARCH_ZIPS, mCheckBox1.isChecked());
                    scanData.putExtra(ActivityHelper.Keys.DOWNLOAD_ART, mCheckBox2.isChecked());
                    scanData.putExtra(ActivityHelper.Keys.CLEAR_GALLERY, mCheckBox3.isChecked());
                    scanData.putExtra(ActivityHelper.Keys.SEARCH_SUBDIR, mCheckBox4.isChecked());
                    ScanRomsActivity.this.setResult(RESULT_OK, scanData);

                    //Save the selected directory
                    mPrefs.edit().putString( ROM_SCAN_START_PATH, mFileUri.toString() ).apply();
                    ScanRomsActivity.this.finish();
                }
            }
        }
    }
}
