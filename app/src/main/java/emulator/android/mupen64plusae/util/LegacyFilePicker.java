package emulator.android.mupen64plusae.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import emulator.android.mupen64plusae.ActivityHelper;
import emulator.android.mupen64plusae.dialog.Prompt;
import emulator.android.mupen64plusae.persistent.AppData;
import emulator.android.mupen64plusae.persistent.GlobalPrefs;

@SuppressWarnings({"deprecation", "RedundantSuppression"})
public class LegacyFilePicker extends AppCompatActivity implements OnItemClickListener
{
    private List<String> mPaths;

    private File mCurrentPath = null;
    private boolean mCanSelectFile = false;
    private boolean mCanViewExternalStorage = false;

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
    /* Default legacy data path, needed for moving legacy data to internal storage */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        // Get the intent data
        final Bundle extras = this.getIntent().getExtras();
        if( extras == null )
        {
            finish();
            return;
        }

        mCanSelectFile = extras.getBoolean(ActivityHelper.Keys.CAN_SELECT_FILE);
        mCanViewExternalStorage = extras.getBoolean(ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE);

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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                mCurrentPath = new File( Environment.getExternalStorageDirectory().getAbsolutePath() );
            } else {
                AppData appData = new AppData( this );
                GlobalPrefs globalPrefs = new GlobalPrefs( this, appData );
                mCurrentPath = new File(globalPrefs.externalRomsDirNoSaf);
            }
        }

        setContentView(R.layout.legacy_file_picker);

        Button cancelButton = findViewById(R.id.buttonCancel);
        cancelButton.setOnClickListener(v -> {
            LegacyFilePicker.this.setResult(RESULT_CANCELED, null);
            LegacyFilePicker.this.finish();
        });

        Button okButton = findViewById(R.id.buttonOk);
        okButton.setOnClickListener(v -> {

            if ((mCanSelectFile && mCurrentPath.isFile()) || (!mCanSelectFile && mCurrentPath.isDirectory())) {
                Intent data = new Intent();
                data.putExtra(ActivityHelper.Keys.SEARCH_PATH, Uri.fromFile(mCurrentPath).toString());
                LegacyFilePicker.this.setResult(RESULT_OK, data);
                LegacyFilePicker.this.finish();
            }
        });

        PopulateFileList();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState )
    {
        if( mCurrentPath != null )
            savedInstanceState.putString( ActivityHelper.Keys.SEARCH_PATH, mCurrentPath.getAbsolutePath() );

        super.onSaveInstanceState( savedInstanceState );
    }

    private void PopulateFileList()
    {
        setTitle( mCurrentPath.getPath() );

        if (mCurrentPath.isDirectory()) {
            // Populate the file list
            // Get the filenames and absolute paths
            List<CharSequence> names = new ArrayList<>();
            mPaths = new ArrayList<>();

            boolean showParent = mCanViewExternalStorage || !(new File(Environment.getExternalStorageDirectory().getAbsolutePath()).equals(mCurrentPath));
            FileUtil.populate( mCurrentPath, showParent, true, true, names, mPaths );

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
            File selectedFile = new File(mPaths.get(position));

            if((selectedFile.isFile() && mCanSelectFile) || selectedFile.isDirectory())
            {
                mCurrentPath = new File(mPaths.get( position ));
                PopulateFileList();
            }
        }
    }
}