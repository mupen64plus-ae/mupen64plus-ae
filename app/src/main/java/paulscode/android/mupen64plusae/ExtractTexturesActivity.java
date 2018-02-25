package paulscode.android.mupen64plusae;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.util.FileUtil;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class ExtractTexturesActivity extends AppCompatActivity implements OnItemClickListener
{    
    private List<CharSequence> mNames;
    private List<String> mPaths;
    private Button mCancelButton;
    private Button mOkButton;

    private static final String STATE_EXTRACT_TEXTURES_FRAGMENT= "STATE_EXTRACT_TEXTURES_FRAGMENT";

    private ExtractTexturesFragment mExtractTexturesFragment = null;
    
    private File mCurrentPath = null;
 
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);
        
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
            // Pick the root of the storage directory by default
            mCurrentPath = new File( Environment.getExternalStorageDirectory().getAbsolutePath() );
        }

        final FragmentManager fm = getSupportFragmentManager();
        mExtractTexturesFragment = (ExtractTexturesFragment) fm.findFragmentByTag(STATE_EXTRACT_TEXTURES_FRAGMENT);

        if(mExtractTexturesFragment == null)
        {
            mExtractTexturesFragment = new ExtractTexturesFragment();
            fm.beginTransaction().add(mExtractTexturesFragment, STATE_EXTRACT_TEXTURES_FRAGMENT).commit();
        }

        setContentView(R.layout.extract_textures_activity);
        
        mCancelButton = findViewById( R.id.buttonCancel );
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ExtractTexturesActivity.this.setResult(RESULT_CANCELED, null);
                ExtractTexturesActivity.this.finish();
            }
        });
        
        mOkButton = findViewById( R.id.buttonOk );
        mOkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mExtractTexturesFragment.extractTextures(mCurrentPath);
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
        mNames = new ArrayList<>();
        mPaths = new ArrayList<>();
        FileUtil.populate( mCurrentPath, true, true, true, mNames, mPaths );

        if(mCurrentPath.isDirectory())
        {
            ListView listView1 = findViewById( R.id.listView1 );
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
