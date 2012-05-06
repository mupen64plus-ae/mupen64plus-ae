package paulscode.android.mupen64plus;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;

// TODO: Comment thoroughly
public class ScancodeDialog extends Dialog implements OnKeyListener
{
    public static IScancodeListener parent = null;
    public static String menuItemName = null;
    public static String menuItemInfo = null;
    public static int menuItemPosition = 0;
    public static int codeType = 0;

    public ScancodeDialog( Context context )
    {
        super(context);
        setContentView( R.layout.scancode_dialog );
        setTitle( "Key Listener" );
        TextView text = (TextView) findViewById( R.id.scancode_text );
        text.setText( "Please press a button.." );

        setOnKeyListener( this );
        text.requestFocus();  // Brings dialog into focus (required for detecting input frem IMEs)

        ImageView image = (ImageView) findViewById( R.id.scancode_image );
        image.setImageResource( R.drawable.icon );
    }

    @Override
    public boolean onKey( DialogInterface dialog, int keyCode, KeyEvent event )
    {
        if( parent != null )
        {
            if( keyCode == KeyEvent.KEYCODE_MENU )
            {
                parent.returnCode( 0, codeType );  // To unmap a button
            }
            else
            {
                if( keyCode > 255 && Globals.analog_100_64 )
                {
                    int k = (int) ( keyCode / 100 );
                    if( k < 0 || k > 255 )
                    {
                        parent.returnCode( 0, codeType );
                    }
                    else
                    {
                        parent.returnCode( k, codeType );
                    }
                }
                else if( keyCode < 0 || keyCode > 255 )
                {
                    parent.returnCode( 0, codeType );
                }
                else
                {
                    parent.returnCode( keyCode, codeType );
                }
            }
        }
        dismiss();
        return true;
    }
}
