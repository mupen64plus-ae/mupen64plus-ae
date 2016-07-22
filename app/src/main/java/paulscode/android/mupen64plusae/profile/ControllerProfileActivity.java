/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.profile;

import org.mupen64plusae.v3.fzurita.R;

import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.input.provider.MogaProvider;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ControllerProfileActivity extends ControllerProfileActivityBase implements OnClickListener
{    
    @Override
    void initLayout()
    {
        mExitMenuItemVisible = true;
        mKeyProvider = new KeyProvider(ImeFormula.DEFAULT, mUnmappableInputCodes);
        mKeyProvider.registerListener(this);
        mMogaProvider = new MogaProvider(mMogaController);
        mMogaProvider.registerListener(this);
        mAxisProvider = new AxisProvider();
        mAxisProvider.registerListener(this);

        // For narrow screens, use an alternate layout
        setContentView(R.layout.controller_profile_activity);

        // Initialize and refresh the widgets
        initWidgets();
    }
    
    private void initWidgets()
    {
        // Get the text view object
        mFeedbackText = (TextView) findViewById( R.id.textFeedback );
        mFeedbackText.setText( "" );
        
        // Create a button list to simplify highlighting and mapping
        // @formatter:off
        setupButton( R.id.buttonDR,     AbstractController.DPD_R );
        setupButton( R.id.buttonDL,     AbstractController.DPD_L );
        setupButton( R.id.buttonDD,     AbstractController.DPD_D );
        setupButton( R.id.buttonDU,     AbstractController.DPD_U );
        setupButton( R.id.buttonS,      AbstractController.START );
        setupButton( R.id.buttonZ,      AbstractController.BTN_Z );
        setupButton( R.id.buttonB,      AbstractController.BTN_B );
        setupButton( R.id.buttonA,      AbstractController.BTN_A );
        setupButton( R.id.buttonCR,     AbstractController.CPD_R );
        setupButton( R.id.buttonCL,     AbstractController.CPD_L );
        setupButton( R.id.buttonCD,     AbstractController.CPD_D );
        setupButton( R.id.buttonCU,     AbstractController.CPD_U );
        setupButton( R.id.buttonR,      AbstractController.BTN_R );
        setupButton( R.id.buttonL,      AbstractController.BTN_L );
        setupButton( R.id.buttonAR,            InputMap.AXIS_R );
        setupButton( R.id.buttonAL,            InputMap.AXIS_L );
        setupButton( R.id.buttonAD,            InputMap.AXIS_D );
        setupButton( R.id.buttonAU,            InputMap.AXIS_U );
        setupButton( R.id.buttonIncrementSlot, InputMap.FUNC_INCREMENT_SLOT );
        setupButton( R.id.buttonSaveSlot,      InputMap.FUNC_SAVE_SLOT );
        setupButton( R.id.buttonLoadSlot,      InputMap.FUNC_LOAD_SLOT );
        setupButton( R.id.buttonPause,         InputMap.FUNC_PAUSE );
        setupButton( R.id.buttonReset,         InputMap.FUNC_RESET );
        setupButton( R.id.buttonStop,          InputMap.FUNC_STOP );
        setupButton( R.id.buttonSpeedDown,     InputMap.FUNC_SPEED_DOWN );
        setupButton( R.id.buttonSpeedUp,       InputMap.FUNC_SPEED_UP );
        setupButton( R.id.buttonFastForward,   InputMap.FUNC_FAST_FORWARD );
        setupButton( R.id.buttonFrameAdvance,  InputMap.FUNC_FRAME_ADVANCE );
        setupButton( R.id.buttonGameshark,     InputMap.FUNC_GAMESHARK );
        setupButton( R.id.buttonSimulateBack,  InputMap.FUNC_SIMULATE_BACK );
        setupButton( R.id.buttonSimulateMenu,  InputMap.FUNC_SIMULATE_MENU );
        setupButton( R.id.buttonScreenshot,    InputMap.FUNC_SCREENSHOT );
        setupButton( R.id.buttonSensorToggle,  InputMap.FUNC_SENSOR_TOGGLE );
        // @formatter:on
    }
    
    private void setupButton( int resId, int index )
    {
        mN64Buttons[index] = (Button) findViewById( resId );
        if( mN64Buttons[index] != null )
            mN64Buttons[index].setOnClickListener( this );
    }
    
    @Override
    public void onClick( View view )
    {
        // Handle button clicks in the mapping screen
        for( int i = 0; i < mN64Buttons.length; i++ )
        {
            // Find the button that was pressed
            if( view.equals( mN64Buttons[i] ) )
            {
                // Popup a dialog to listen to input codes from user
                Button button = (Button) view;
                popupListener( button.getText(), i );
            }
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        return mKeyProvider.onKey(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        return mKeyProvider.onKey(keyCode, event) || super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        return mAxisProvider.onGenericMotion(event) || super.onGenericMotionEvent(event);
    }
}
