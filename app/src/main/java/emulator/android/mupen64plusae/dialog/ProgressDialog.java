package emulator.android.mupen64plusae.dialog;

import org.mupen64plusae.v3.alpha.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ProgressDialog implements OnClickListener
{
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
    
    private long mMaxProgress = -1;
    private long mProgress = 0;
    private OnCancelListener mOnCancelListener = null;
    
    public ProgressDialog( Activity activity, CharSequence title,
            CharSequence subtitle, CharSequence message, boolean cancelable )
    {
        mActivity = activity;
        View layout = View.inflate(activity, R.layout.progress_dialog, null );
        
        mTextProgress = layout.findViewById( R.id.textProgress );
        mTextSubprogress = layout.findViewById( R.id.textSubprogress );
        mTextMessage = layout.findViewById( R.id.textMessage );
        mProgressTotal = layout.findViewById( R.id.progressTotal );
        
        // Create main dialog
        Builder builder = getBuilder( activity, title, subtitle, message, cancelable, layout );
        mDialog = builder.create();
        
        // Create canceling dialog
        subtitle = mActivity.getString( R.string.toast_canceling );
        message = mActivity.getString( R.string.toast_pleaseWait );
        layout = View.inflate( activity, R.layout.progress_dialog, null );
        builder = getBuilder( activity, title, subtitle, message, false, layout );
        mAbortDialog = builder.create();
    }
    
    public ProgressDialog(ProgressDialog original, Activity activity, CharSequence title,
        CharSequence subtitle, CharSequence message, boolean cancelable)
    {
        this(activity, title, subtitle, message, cancelable);
        
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
    public void onClick( DialogInterface dlg, int which )
    {
        if( which == DialogInterface.BUTTON_NEGATIVE  && mOnCancelListener != null)
        {
            mOnCancelListener.OnCancel();
        }
    }
    
    private Builder getBuilder( Activity activity, CharSequence title, CharSequence subtitle,
            CharSequence message, boolean cancelable, View layout )
    {
        TextView textSubtitle = layout.findViewById( R.id.textSubtitle );
        TextView textMessage = layout.findViewById( R.id.textMessage );
        textSubtitle.setText( subtitle );
        textMessage.setText( message );
        
        Builder builder = new Builder( activity ).setTitle( title ).setCancelable( false )
                .setPositiveButton( null, null ).setView( layout );
        if( cancelable )
            builder.setNegativeButton( android.R.string.cancel, this );
        else
            builder.setNegativeButton( null, null );
        return builder;
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
}
