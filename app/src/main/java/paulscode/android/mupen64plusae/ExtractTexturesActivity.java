package paulscode.android.mupen64plusae;

import java.io.File;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.LegacyFilePicker;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

public class ExtractTexturesActivity extends AppCompatActivity implements ExtractTexturesFragment.OnFinishListener
{
    private static final String STATE_EXTRACT_TEXTURES_FRAGMENT= "STATE_EXTRACT_TEXTURES_FRAGMENT";

    private ExtractTexturesFragment mExtractTexturesFragment = null;

    private Uri mFileUri = null;

    private TextView mFileDescriptionTextView = null;
    private static final String URI_TO_IMPORT = "URI_TO_IMPORT";

    ActivityResultLauncher<Intent> mLaunchLegacyFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {

                    final Bundle extras = data.getExtras();

                    if (extras != null) {
                        final String searchUri = extras.getString(ActivityHelper.Keys.SEARCH_PATH);

                        mFileUri = Uri.parse(searchUri);

                        if (mFileDescriptionTextView != null && mFileUri.getPath() != null) {
                            DocumentFile file = FileUtil.getDocumentFileSingle(ExtractTexturesActivity.this, mFileUri);
                            mFileDescriptionTextView.setText(file == null ? "" : file.getName());
                        }
                    }
                }
            });

    ActivityResultLauncher<Intent> mLaunchFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {

                    mFileUri = data.getData();

                    if (mFileDescriptionTextView != null && mFileUri != null) {
                        DocumentFile file = FileUtil.getDocumentFileSingle(ExtractTexturesActivity.this, mFileUri);
                        mFileDescriptionTextView.setText(file == null ? "" : file.getName());
                    }
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
        filePickerButtont.setOnClickListener(v -> startFilePicker());

        mFileDescriptionTextView = findViewById(R.id.fileDescription);

        if (mFileDescriptionTextView != null && mFileUri != null) {
            DocumentFile file = FileUtil.getDocumentFileSingle(this, mFileUri);
            mFileDescriptionTextView.setText(file == null ? "" : file.getName());
        }

        Button cancelButton = findViewById(R.id.buttonCancel);
        cancelButton.setOnClickListener(v -> {
            ExtractTexturesActivity.this.setResult(RESULT_CANCELED, null);
            ExtractTexturesActivity.this.finish();
        });

        Button okButton = findViewById(R.id.buttonOk);
        okButton.setOnClickListener(v -> {
            if (mFileUri != null) {
                mExtractTexturesFragment.extractTextures(mFileUri);
            }
        });
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
            Intent intent = new Intent(this, LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, true );
            intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, true);
            mLaunchLegacyFilePicker.launch(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            mLaunchFilePicker.launch(intent);
        }
    }

    @Override
    public void onFinish() {
        ExtractTexturesActivity.this.finish();
    }
}
