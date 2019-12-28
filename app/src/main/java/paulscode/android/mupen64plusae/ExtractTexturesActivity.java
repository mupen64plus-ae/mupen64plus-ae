package paulscode.android.mupen64plusae;

import java.io.File;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.util.ProviderUtil;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ExtractTexturesActivity extends AppCompatActivity implements ExtractTexturesFragment.OnFinishListener
{
    public static final int PICK_TEXTURE_REQUEST_CODE = 2;

    private static final String STATE_EXTRACT_TEXTURES_FRAGMENT= "STATE_EXTRACT_TEXTURES_FRAGMENT";

    private ExtractTexturesFragment mExtractTexturesFragment = null;
    
    private File mCurrentPath = null;
    private Uri mFileUri = null;

    private TextView mFileDescriptionTextView = null;
    private static final String URI_TO_IMPORT = "URI_TO_IMPORT";
    private static final String CURRENT_PATH = "CURRENT_PATH";
 
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
        filePickerButtont.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startFilePicker();
            }
        });

        mFileDescriptionTextView = findViewById(R.id.fileDescription);

        if (mFileDescriptionTextView != null && mFileUri != null) {
            mFileDescriptionTextView.setText(ProviderUtil.getFileName(this, mFileUri));
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

    private void startFilePicker()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent = Intent.createChooser(intent, getString(R.string.pathHiResTexturesTask_select_zip));
        startActivityForResult(intent, PICK_TEXTURE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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
