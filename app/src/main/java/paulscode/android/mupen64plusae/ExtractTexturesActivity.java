package paulscode.android.mupen64plusae;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.ProviderUtil;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class ExtractTexturesActivity extends AppCompatActivity implements OnItemClickListener,
        ExtractTexturesFragment.OnFinishListener
{
    public static final int PICK_TEXTURE_REQUEST_CODE = 2;

    private List<String> mPaths;

    private static final String STATE_EXTRACT_TEXTURES_FRAGMENT= "STATE_EXTRACT_TEXTURES_FRAGMENT";

    private ExtractTexturesFragment mExtractTexturesFragment = null;
    
    private File mCurrentPath = null;
    private Uri mFileUri = null;

    private TextView mFileDescriptionTextView = null;
    private static final String URI_TO_IMPORT = "URI_TO_IMPORT";
    private static final String CURRENT_PATH = "CURRENT_PATH";
 
    @Override
    // The following is needed for Environment.getExternalStorageDirectory() This is still needed for Android 4.4
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getSupportFragmentManager();
        mExtractTexturesFragment = (ExtractTexturesFragment) fm.findFragmentByTag(STATE_EXTRACT_TEXTURES_FRAGMENT);

        if(mExtractTexturesFragment == null)
        {
            mExtractTexturesFragment = new ExtractTexturesFragment();
            fm.beginTransaction().add(mExtractTexturesFragment, STATE_EXTRACT_TEXTURES_FRAGMENT).commit();
        }

        if (AppData.IS_LOLLIPOP) {
            String currentUri = null;

            if(savedInstanceState != null)
            {
                currentUri = savedInstanceState.getString( URI_TO_IMPORT );
            }

            if (currentUri != null) {
                mFileUri = Uri.parse(currentUri);
            }

            setContentView(R.layout.extract_textures_activity);

            Button filePickerButtont = findViewById(R.id.buttonFilePicker);
            filePickerButtont.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    startFilePicker();
                }
            });

            mFileDescriptionTextView = findViewById(R.id.fileDescription);

            if (mFileDescriptionTextView != null && mFileUri != null) {
                mFileDescriptionTextView.setText(ProviderUtil.getFileName(this, mFileUri));
            }
        } else {
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
                // Pick the root of the storage directory by default
                mCurrentPath = new File( Environment.getExternalStorageDirectory().getAbsolutePath() );
            }

            setContentView(R.layout.extract_textures_activity_kitkat);

            PopulateFileList();
        }

        Button cancelButton = findViewById(R.id.buttonCancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ExtractTexturesActivity.this.setResult(RESULT_CANCELED, null);
                ExtractTexturesActivity.this.finish();
            }
        });

        Button okButton = findViewById(R.id.buttonOk);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mExtractTexturesFragment.extractTextures(mFileUri);
            }
        });
    }
    
    @Override
    public void onSaveInstanceState( Bundle savedInstanceState )
    {
        if (mCurrentPath != null)
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
        List<CharSequence> mNames = new ArrayList<>();
        mPaths = new ArrayList<>();
        FileUtil.populate( mCurrentPath, true, true, true, mNames, mPaths );

        if(mCurrentPath.isDirectory())
        {
            ListView listView1 = findViewById( R.id.listView1 );
            ArrayAdapter<String> adapter = Prompt.createFilenameAdapter( this, mPaths, mNames);
            listView1.setAdapter( adapter );
            listView1.setOnItemClickListener( this );
        }
    }

    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id )
    {
        if (position < mPaths.size()) {
            mCurrentPath = new File(mPaths.get( position ));
            mFileUri = Uri.fromFile(mCurrentPath);
            PopulateFileList();
        }
    }

    private void startFilePicker()
    {
        if (AppData.IS_LOLLIPOP) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent = Intent.createChooser(intent, getString(R.string.pathHiResTexturesTask_select_zip));
            startActivityForResult(intent, PICK_TEXTURE_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == PICK_TEXTURE_REQUEST_CODE)
        {
            if (resultCode == RESULT_OK) {

                // The result data contains a URI for the document or directory that
                // the user selected.
                if (data != null) {
                    mFileUri = data.getData();

                    if (mFileDescriptionTextView != null) {
                        mFileDescriptionTextView.setText(ProviderUtil.getFileName(this, mFileUri));
                    }
                }
            }
        }
    }

    @Override
    public void onFinish() {
        ExtractTexturesActivity.this.finish();
    }
}
