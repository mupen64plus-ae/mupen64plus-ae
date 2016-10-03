package paulscode.android.mupen64plusae;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.util.FileUtil;

public class ScanRomsActivity extends AppCompatActivity implements OnItemClickListener
{    
    private List<CharSequence> mNames;
    private List<String> mPaths;
    private CheckBox mCheckBox1;
    private CheckBox mCheckBox2;
    private CheckBox mCheckBox3;
    private CheckBox mCheckBox4;
    private Button mCancelButton;
    private Button mOkButton;
    
    private File mCurrentPath = null;
    private SharedPreferences mPrefs = null;

    private static final String ROM_SCAN_START_PATH = "RomScanStartPath";
 
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences( this );
        
        String currentPath = null;
        
        if(savedInstanceState != null)
        {
            currentPath = savedInstanceState.getString( ActivityHelper.Keys.SEARCH_PATH );
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
         
        setContentView(R.layout.scan_roms_activity);
                
        // Set checkbox state
        mCheckBox1 = (CheckBox) findViewById( R.id.checkBox1 );
        mCheckBox2 = (CheckBox) findViewById( R.id.checkBox2 );
        mCheckBox3 = (CheckBox) findViewById( R.id.checkBox3 );
        mCheckBox4 = (CheckBox) findViewById( R.id.checkBox4 );
        mCheckBox1.setChecked( true );
        mCheckBox2.setChecked( true );
        mCheckBox3.setChecked( false );
        mCheckBox4.setChecked( true );
        
        mCancelButton = (Button) findViewById( R.id.buttonCancel );
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ScanRomsActivity.this.setResult(RESULT_CANCELED, null);
                ScanRomsActivity.this.finish();
            }
        });
        
        mOkButton = (Button) findViewById( R.id.buttonOk );
        mOkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent data = new Intent();
                data.putExtra(ActivityHelper.Keys.SEARCH_PATH, mCurrentPath.getPath());
                data.putExtra(ActivityHelper.Keys.SEARCH_ZIPS, mCheckBox1.isChecked());
                data.putExtra(ActivityHelper.Keys.DOWNLOAD_ART, mCheckBox2.isChecked());
                data.putExtra(ActivityHelper.Keys.CLEAR_GALLERY, mCheckBox3.isChecked());
                data.putExtra(ActivityHelper.Keys.SEARCH_SUBDIR, mCheckBox4.isChecked());
                ScanRomsActivity.this.setResult(RESULT_OK, data);

                //Save the selected directory
                mPrefs.edit().putString( ROM_SCAN_START_PATH, mCurrentPath.getPath() ).commit();
                ScanRomsActivity.this.finish();
            }
        });

        PopulateFileList();
    }
    
    @Override
    public void onSaveInstanceState( Bundle savedInstanceState )
    {
        if( mCurrentPath != null )
            savedInstanceState.putString( ActivityHelper.Keys.SEARCH_PATH, mCurrentPath.getAbsolutePath() );

        super.onSaveInstanceState( savedInstanceState );
    }
    
    private void PopulateFileList()
    {
        setTitle( mCurrentPath.getPath() );
        // Populate the file list
        // Get the filenames and absolute paths
        mNames = new ArrayList<CharSequence>();
        mPaths = new ArrayList<String>();
        FileUtil.populate( mCurrentPath, true, true, true, mNames, mPaths );

        if(mCurrentPath.isDirectory())
        {
            ListView listView1 = (ListView) findViewById( R.id.listView1 );
            ArrayAdapter<String> adapter = Prompt.createFilenameAdapter( this, mPaths, mNames );
            listView1.setAdapter( adapter );
            listView1.setOnItemClickListener( this );   
        }
    }

    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id )
    {
        mCurrentPath = new File(mPaths.get( position ));
        PopulateFileList();
    }
}
