package paulscode.android.mupen64plusae.dialog;

import org.mupen64plusae.v3.alpha.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ProgressDialog implements OnClickListener
{
    private static final float PROGRESS_PRECISION = 1000f;
    
    private final Activity mActivity;
    private final AsyncTask<?, ?, ?> mTask;
    private final TextView mTextProgress;
    private final TextView mTextSubprogress;
    private final TextView mTextMessage;
    private final ProgressBar mProgressSubtotal;
    private final ProgressBar mProgressTotal;
    private final AlertDialog mDialog;
    private final AlertDialog mAbortDialog;
    
    private long mMaxProgress = -1;
    private long mMaxSubprogress = -1;
    private long mProgress = 0;
    private long mSubprogress = 0;
    
    @SuppressLint( "InflateParams" )
    public ProgressDialog( Activity activity, AsyncTask<?, ?, ?> task, CharSequence title,
            CharSequence subtitle, CharSequence message, boolean cancelable )
    {
        mActivity = activity;
        mTask = task;
        
        // Create main dialog
        final LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View layout = inflater.inflate( R.layout.progress_dialog, null );
        Builder builder = getBuilder( activity, title, subtitle, message, cancelable, layout );
        mTextProgress = (TextView) layout.findViewById( R.id.textProgress );
        mTextSubprogress = (TextView) layout.findViewById( R.id.textSubprogress );
        mTextMessage = (TextView) layout.findViewById( R.id.textMessage );
        mProgressSubtotal = (ProgressBar) layout.findViewById( R.id.progressSubtotal );
        mProgressTotal = (ProgressBar) layout.findViewById( R.id.progressTotal );
        mDialog = builder.create();
        
        // Create canceling dialog
        subtitle = mActivity.getString( R.string.toast_canceling );
        message = mActivity.getString( R.string.toast_pleaseWait );
        layout = inflater.inflate( R.layout.progress_dialog, null );
        builder = getBuilder( activity, title, subtitle, message, false, layout );
        mAbortDialog = builder.create();
    }
    
    public void show()
    {
        mAbortDialog.show();
        mDialog.show();
    }
    
    public void dismiss()
    {
        mAbortDialog.dismiss();
        mDialog.dismiss();
    }
    
    @Override
    public void onClick( DialogInterface dlg, int which )
    {
        if( which == DialogInterface.BUTTON_NEGATIVE )
        {
            mTask.cancel( true );
        }
    }
    
    private Builder getBuilder( Activity activity, CharSequence title, CharSequence subtitle,
            CharSequence message, boolean cancelable, View layout )
    {
        TextView textSubtitle = (TextView) layout.findViewById( R.id.textSubtitle );
        TextView textMessage = (TextView) layout.findViewById( R.id.textMessage );
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
        mActivity.runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                mTextProgress.setText( text );
            }
        } );
    }
    
    public void setSubtext( final CharSequence text )
    {
        mActivity.runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                mTextSubprogress.setText( text );
            }
        } );
    }
    
    public void setMessage( final CharSequence text )
    {
        mActivity.runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                mTextMessage.setText( text );
            }
        } );
    }
    
    public void setMessage( final int resid )
    {
        mActivity.runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                mTextMessage.setText( resid );
            }
        } );
    }
    
    public void setMaxProgress( final long size )
    {
        mActivity.runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                mMaxProgress = size;
                mProgress = 0;
                mProgressTotal.setProgress( 0 );
                mProgressTotal.setVisibility( mMaxProgress > 0 ? View.VISIBLE : View.GONE );
            }
        } );
    }
    
    public void setMaxSubprogress( final long size )
    {
        mActivity.runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                mMaxSubprogress = size;
                mSubprogress = 0;
                mProgressSubtotal.setProgress( 0 );
                mProgressSubtotal.setVisibility( mMaxSubprogress > 0 ? View.VISIBLE : View.GONE );
            }
        } );
    }
    
    public void incrementProgress( final long inc )
    {
        mActivity.runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                if( mMaxProgress > 0 )
                {
                    mProgress += inc;
                    int pctProgress = Math.round( ( PROGRESS_PRECISION * mProgress )
                            / mMaxProgress );
                    mProgressTotal.setProgress( pctProgress );
                }
            }
        } );
    }
    
    public void incrementSubprogress( final long inc )
    {
        mActivity.runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                if( mMaxSubprogress > 0 )
                {
                    mSubprogress += inc;
                    int pctSubprogress = Math.round( ( PROGRESS_PRECISION * mSubprogress )
                            / mMaxSubprogress );
                    mProgressSubtotal.setProgress( pctSubprogress );
                }
            }
        } );
    }
}
