package paulscode.android.mupen64plusae.input;

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.NativeMethods;
import paulscode.android.mupen64plusae.TouchscreenView;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class TouchscreenController extends AbstractController implements OnTouchListener
{

    public boolean[] pointers = new boolean[256];
    public int[] pointerX = new int[256];
    public int[] pointerY = new int[256];
    public static boolean[] previousKeyStates = new boolean[TouchscreenView.MAX_BUTTONS];
    public static int[] touchScreenPointerY = new int[256];
    public static int[] touchScreenPointerX = new int[256];
    public static boolean[] touchScreenPointers = new boolean[256];
    
    @TargetApi( 5 )
    public boolean onTouch( View v, MotionEvent event )
    {
        if( !Globals.userPrefs.isInputEnabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR)
            return false;
        
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        float x = event.getX();
        float y = event.getY();
        float p = event.getPressure();
        
        NativeMethods.onTouch( action, x, y, p );
        
        int maxPid = 0;
        int pid, i;
        
        if( actionCode == MotionEvent.ACTION_POINTER_DOWN )
        {
            pid = event.getPointerId( action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT );
            if( pid > maxPid )
                maxPid = pid;
            pointers[pid] = true;
        }
        else if( actionCode == MotionEvent.ACTION_POINTER_UP )
        {
            pid = event.getPointerId( action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT );
            if( pid > maxPid )
                maxPid = pid;
            pointers[pid] = false;
        }
        else if( actionCode == MotionEvent.ACTION_DOWN )
        {
            for( i = 0; i < event.getPointerCount(); i++ )
            {
                pid = event.getPointerId( i );
                if( pid > maxPid )
                    maxPid = pid;
                pointers[pid] = true;
            }
        }
        else if( actionCode == MotionEvent.ACTION_UP || actionCode == MotionEvent.ACTION_CANCEL )
        {
            for( i = 0; i < 256; i++ )
            {
                pointers[i] = false;
                pointerX[i] = -1;
                pointerY[i] = -1;
            }
        }
        
        for( i = 0; i < event.getPointerCount(); i++ )
        {
            pid = event.getPointerId( i );
            if( pointers[pid] )
            {
                if( pid > maxPid )
                    maxPid = pid;
                pointerX[pid] = (int) event.getX( i );
                pointerY[pid] = (int) event.getY( i );
            }
        }
        // TODO ? Globals.touchscreenInstance.updatePointers( pointers, pointerX, pointerY, maxPid );
        return true;
    }

    /**
     * Simulates key events for the SDLButtons on the touchscreen controller.
     * 
     * @param sdlButtonPressed
     *            true to indicate that a button is pressed
     * @param sdlButtonCodes
     *            key code for each button
     * @param sdlButtonCount
     *            number of buttons
     */
    public static void updateSDLButtonStates( boolean[] sdlButtonPressed, int[] sdlButtonCodes,
            int sdlButtonCount )
    {
        if( Globals.surfaceInstance == null )
            return;
        
        for( int x = 0; x < sdlButtonCount; x++ )
        {
            if( sdlButtonPressed[x] != previousKeyStates[x] )
            {
                previousKeyStates[x] = sdlButtonPressed[x];
                if( sdlButtonPressed[x] )
                    Globals.surfaceInstance.onSDLKey( sdlButtonCodes[x], KeyEvent.ACTION_DOWN );
                else
                    Globals.surfaceInstance.onSDLKey( sdlButtonCodes[x], KeyEvent.ACTION_UP );
            }
        }
    }
    
}
