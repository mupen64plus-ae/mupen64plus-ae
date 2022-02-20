package paulscode.android.mupen64plusae.dialog;

import paulscode.android.mupen64plusae.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.res.Configuration;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ProgressDialog implements View.OnClickListener {

    public interface OnCancelListener
    {
        //This is called if the dialog is canceled
        void OnCancel();
    }
    
    private static final float PROGRESS_PRECISION = 1000f;
    
    private final Activity mActivity;
    private final TextView mTextProgress;
    private final TextView mTextSubprogress;
    private final TextView mTextMessage;
    private final ProgressBar mProgressTotal;
    private final AlertDialog mDialog;
    private final AlertDialog mAbortDialog;
    private boolean mHasExtraView = false;
    
    private long mMaxProgress = -1;
    private long mProgress = 0;
    private OnCancelListener mOnCancelListener = null;

    private final View mLayout;
    private Button mCancelButton;
    private final int mCancelButtonText;
    
    public ProgressDialog( Activity activity, CharSequence title,
            CharSequence subtitle, CharSequence message, boolean cancelable, int cancelText )
    {
        mActivity = activity;
        mLayout = View.inflate(activity, R.layout.progress_dialog, null );
        
        mTextProgress = mLayout.findViewById( R.id.textProgress );
        mTextSubprogress = mLayout.findViewById( R.id.textSubprogress );
        mTextMessage = mLayout.findViewById( R.id.textMessage );
        mProgressTotal = mLayout.findViewById( R.id.progressTotal );
        mCancelButtonText = cancelText;
        
        // Create main dialog
        Builder builder = getBuilder( activity, title, subtitle, message, cancelable, cancelText, mLayout );
        mDialog = builder.create();
        
        // Create canceling dialog
        subtitle = mActivity.getString( R.string.toast_canceling );
        message = mActivity.getString( R.string.toast_pleaseWait );
        View layout = View.inflate( activity, R.layout.progress_dialog, null );
        builder = getBuilder( activity, title, subtitle, message, false, 0, layout );
        mAbortDialog = builder.create();
    }
    
    public ProgressDialog(ProgressDialog original, Activity activity, CharSequence title,
        CharSequence subtitle, CharSequence message, boolean cancelable, int cancelText)
    {
        this(activity, title, subtitle, message, cancelable, cancelText);
        
        if(original != null)
        {            

            mOnCancelListener = original.mOnCancelListener;
            
            setMaxProgress(original.mMaxProgress);
            
            mProgress = original.mProgress;
            
            incrementProgress(0);
            
            mTextProgress.setText(original.mTextProgress.getText());
            mTextSubprogress.setText(original.mTextSubprogress.getText());
            mTextMessage.setText(original.mTextMessage.getText());
        }
    }

    public ProgressDialog(ProgressDialog original, Activity activity, CharSequence title,
                          CharSequence subtitle, CharSequence message, boolean cancelable)
    {
        this(original, activity, title, subtitle, message, cancelable, android.R.string.cancel);
    }
    
    public void show()
    {
        try {
            mAbortDialog.show();
            mDialog.show();

        } catch (android.view.WindowManager.BadTokenException e) {
            e.printStackTrace();
        }
    }
    
    public void dismiss()
    {
        try {
            mAbortDialog.dismiss();
            mDialog.dismiss();
        } catch (android.view.WindowManager.BadTokenException e) {
            e.printStackTrace();
        }
    }
    
    public void setOnCancelListener(OnCancelListener onCancelListener)
    {
        mOnCancelListener = onCancelListener;
    }

    @Override
    public void onClick(View view)
    {
        if (mOnCancelListener != null) {
            mOnCancelListener.OnCancel();
        }
    }
    
    private Builder getBuilder( Activity activity, CharSequence title, CharSequence subtitle,
            CharSequence message, boolean cancelable, int cancelText, View layout )
    {
        TextView textSubtitle = layout.findViewById( R.id.textSubtitle );
        TextView textMessage = layout.findViewById( R.id.textMessage );
        textSubtitle.setText( subtitle );
        textMessage.setText( message );
        
        Builder builder = new Builder( activity ).setTitle( title )
                .setCancelable( false )
                .setPositiveButton( null, null )
                .setNegativeButton( null, null )
                .setView( layout );

        if (cancelable) {
            mCancelButton = layout.findViewById(R.id.buttonCancelProgress);
            String newText = mActivity.getString(cancelText, "");
            mCancelButton.setText(newText);
            mCancelButton.setOnClickListener(this);
        } else {
            Button tempCancelButton = layout.findViewById(R.id.buttonCancelProgress);
            if (tempCancelButton != null) {
                tempCancelButton.setEnabled(false);
                tempCancelButton.setClickable(false);
            }
        }
        return builder;
    }

    public void addView(View view) {

        if (!mHasExtraView) {
            mHasExtraView = true;

            // Setting the width here is a workaround for alert dialogs having a minimum width in
            // landscape mode. Due to this, it's impossible to align the manually added continue
            // button to the right of the dialog or to the left of the additional view
            // with the same layout parameters. So, use a layout file that works when no
            // additional views are added, but if a view is added in landscape mode, increase
            // the width of the alert dialog.

            int orientation = mActivity.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            {
                int width = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        650,
                        mActivity.getResources().getDisplayMetrics()
                );
                mDialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            LinearLayout layout = (LinearLayout) mLayout.findViewById(R.id.additionalViewLocation);
            layout.addView(view);
        }
    }
    
    public void setText( final CharSequence text )
    {
        mActivity.runOnUiThread(() -> mTextProgress.setText( text ));
    }
    
    public void setSubtext( final CharSequence text )
    {
        mActivity.runOnUiThread(() -> mTextSubprogress.setText( text ));
    }
    
    public void setMessage( final CharSequence text )
    {
        mActivity.runOnUiThread(() -> mTextMessage.setText( text ));
    }
    
    public void setMessage( final int resid )
    {
        mActivity.runOnUiThread(() -> mTextMessage.setText( resid ));
    }
    
    public void setMaxProgress( final long size )
    {
        mActivity.runOnUiThread(() -> {
            mMaxProgress = size;
            mProgress = 0;
            mProgressTotal.setProgress( 0 );
            mProgressTotal.setVisibility( mMaxProgress > 0 ? View.VISIBLE : View.GONE );
        });
    }
    
    public void incrementProgress( final long inc )
    {
        mActivity.runOnUiThread(() -> {
            if( mMaxProgress > 0 )
            {
                mProgress += inc;
                int pctProgress = Math.round( ( PROGRESS_PRECISION * mProgress )
                        / mMaxProgress );
                mProgressTotal.setProgress( pctProgress );
            }
        });
    }

    public void setProgress(final long progress) {
        mActivity.runOnUiThread(() -> {
            if( mMaxProgress > 0 )
            {
                mProgress = progress;
                int pctProgress = Math.round( ( PROGRESS_PRECISION * mProgress )
                        / mMaxProgress );
                mProgressTotal.setProgress( pctProgress );
            }
        });
    }

    public void setCancelButtonState(boolean enabled)
    {
        if (mCancelButton != null) {
            mCancelButton.setEnabled(enabled);
            mCancelButton.setClickable(enabled);
        }
    }

    public void appendCancelButtonText(String text)
    {
        if (mCancelButton != null) {
            String newText = mActivity.getString(mCancelButtonText, text);
            mCancelButton.setText(newText);
        }
    }
}
