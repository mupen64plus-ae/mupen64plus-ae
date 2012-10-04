package paulscode.android.mupen64plusae;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

class JoystickListener implements View.OnGenericMotionListener
{
    private SDLSurface parent;

    public JoystickListener( SDLSurface parent )
    {
        this.parent = parent;
    }

    @TargetApi(12)
    public boolean onGenericMotion( View v, MotionEvent event )
    {
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1 )
        {
            // Must be the same order as EButton listing in plugin.h! (input-sdl plug-in)
            final int Z      =  5;
            final int CRight =  8;
            final int CLeft  =  9;
            final int CDown  = 10;
            final int CUp    = 11;

            // Z-button and C-pad, interpret left analog trigger and right analog stick
            parent.mp64pButtons[Z]      = ( event.getAxisValue( MotionEvent.AXIS_Z  ) >  0   );
            parent.mp64pButtons[CLeft]  = ( event.getAxisValue( MotionEvent.AXIS_RX ) < -0.5 );
            parent.mp64pButtons[CRight] = ( event.getAxisValue( MotionEvent.AXIS_RX ) >  0.5 );
            parent.mp64pButtons[CUp]    = ( event.getAxisValue( MotionEvent.AXIS_RY ) < -0.5 );
            parent.mp64pButtons[CDown]  = ( event.getAxisValue( MotionEvent.AXIS_RY ) >  0.5 );

            // Analog X-Y, interpret the left analog stick
            parent.axisX = (int) (  80.0f * event.getAxisValue( MotionEvent.AXIS_X ) );
            parent.axisY = (int) ( -80.0f * event.getAxisValue( MotionEvent.AXIS_Y ) );

            GameActivityCommon.updateVirtualGamePadStates( 0, parent.mp64pButtons, parent.axisX, parent.axisY );
            return true;
        }
        return false;
    }
}

