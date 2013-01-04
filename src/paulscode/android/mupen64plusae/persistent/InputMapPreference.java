/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.persistent;

import java.util.List;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.input.provider.LazyProvider;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.OnInputCodeListener;
import android.annotation.TargetApi;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

public class InputMapPreference extends DialogPreference implements
        AbstractProvider.OnInputListener, DialogInterface.OnClickListener, View.OnClickListener,
        CompoundButton.OnCheckedChangeListener
{
    private static final float UNMAPPED_BUTTON_ALPHA = 0.2f;
    private static final int UNMAPPED_BUTTON_FILTER = 0x66FFFFFF;
    private static final int MIN_LAYOUT_WIDTH_DP = 480;
    private static final int MIN_LAYOUT_HEIGHT_DP = 480;
    private static final String INPUT_MAP1 = "inputMap1";
    
    private final InputMap mMap;
    private final LazyProvider mProvider;
    private CompoundButton mToggleWidget;
    private TextView mFeedbackText;
    private final Button[] mN64Button;
    private List<Integer> mUnmappableKeyCodes;
    private boolean mSpecialKeysVisible = false;
    private boolean mDoReclick = false;
    
    public InputMapPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        mMap = new InputMap();
        mProvider = new LazyProvider();
        mProvider.registerListener( this );
        mN64Button = new Button[InputMap.NUM_MAPPABLES];
        setWidgetLayoutResource( R.layout.widget_toggle );
        
        // Select the appropriate dialog layout according to device configuration. Although you can
        // do this through the resource directory structure and layout aliases, we'll do it this way
        // for now since it's easier to maintain in the short term while the design is in flux.
        // TODO: Consider using resource directories to handle device variation, once design is set.
        WindowManager manager = (WindowManager) getContext().getSystemService(
                Context.WINDOW_SERVICE );
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics( metrics );
        float scalefactor = (float) DisplayMetrics.DENSITY_DEFAULT / (float) metrics.densityDpi;
        int widthDp = Math.round( metrics.widthPixels * scalefactor );
        int heightDp = Math.round( metrics.heightPixels * scalefactor );
        
        // For short screens, hide the dialog title to yield more space
        if( heightDp < MIN_LAYOUT_HEIGHT_DP )
            setDialogTitle( null );
        
        // For narrow screens, use an alternate layout
        if( widthDp < MIN_LAYOUT_WIDTH_DP )
            setDialogLayoutResource( R.layout.input_map_preference_port );
        else
            setDialogLayoutResource( R.layout.input_map_preference );
    }
    
    @Override
    protected Object onGetDefaultValue( TypedArray a, int index )
    {
        return a.getString( index );
    }
    
    @Override
    protected void onSetInitialValue( boolean restorePersistedValue, Object defaultValue )
    {
        if( restorePersistedValue )
        {
            // Restore persisted value
            mMap.deserialize( getPersistedString( "" ) );
        }
        else
        {
            // Set default state from the XML attribute
            String value = (String) defaultValue;
            persistString( value );
            mMap.deserialize( value );
        }
        updateViews();
    }
    
    @Override
    protected void onBindView( View view )
    {
        // Set up the menu item seen in the preferences menu
        super.onBindView( view );
        
        // Restore the persisted map
        mMap.deserialize( getPersistedString( "" ) );
        
        // Get the toggle widget and set its state
        mToggleWidget = (CompoundButton) view.findViewById( R.id.widgetToggle );
        mToggleWidget.setOnCheckedChangeListener( this );
        mToggleWidget.setChecked( mMap.isEnabled() );
    }
    
    @Override
    protected void onBindDialogView( View view )
    {
        // Set up the dialog view seen when the preference menu item is clicked
        super.onBindDialogView( view );
        
        // Hide some widgets that do not apply
        UserPrefs prefs = new UserPrefs( getContext() );
        if( prefs.isTouchpadEnabled && getKey().equals( INPUT_MAP1 ) )
        {
            // First player and Xperia PLAY touchpad is enabled, hide the a- and c-pads
            view.findViewById( R.id.aPadDefault ).setVisibility( View.GONE );
            view.findViewById( R.id.cPadDefault ).setVisibility( View.GONE );
        }
        else
        {
            // All other cases, hide the Xperia PLAY stuff
            view.findViewById( R.id.aPadXperiaPlay ).setVisibility( View.GONE );
            view.findViewById( R.id.cPadXperiaPlay ).setVisibility( View.GONE );
        }
        int specialKeyVisibility = mSpecialKeysVisible ? View.VISIBLE : View.GONE;
        view.findViewById( R.id.include_all_special_keys ).setVisibility( specialKeyVisibility );
        
        // Get the text view object
        mFeedbackText = (TextView) view.findViewById( R.id.textFeedback );
        
        // Create a button list to simplify highlighting and mapping
        // @formatter:off
        setupButton( view, R.id.buttonA,  AbstractController.BTN_A );
        setupButton( view, R.id.buttonB,  AbstractController.BTN_B );
        setupButton( view, R.id.buttonL,  AbstractController.BTN_L );
        setupButton( view, R.id.buttonR,  AbstractController.BTN_R );
        setupButton( view, R.id.buttonZ,  AbstractController.BTN_Z );
        setupButton( view, R.id.buttonS,  AbstractController.START );
        setupButton( view, R.id.buttonCR, AbstractController.CPD_R );
        setupButton( view, R.id.buttonCL, AbstractController.CPD_L );
        setupButton( view, R.id.buttonCD, AbstractController.CPD_D );
        setupButton( view, R.id.buttonCU, AbstractController.CPD_U );
        setupButton( view, R.id.buttonDR, AbstractController.DPD_R );
        setupButton( view, R.id.buttonDL, AbstractController.DPD_L );
        setupButton( view, R.id.buttonDD, AbstractController.DPD_D );
        setupButton( view, R.id.buttonDU, AbstractController.DPD_U );
        setupButton( view, R.id.buttonAR, InputMap.AXIS_R );
        setupButton( view, R.id.buttonAL, InputMap.AXIS_L );
        setupButton( view, R.id.buttonAD, InputMap.AXIS_D );
        setupButton( view, R.id.buttonAU, InputMap.AXIS_U );
        setupButton( view, R.id.buttonRumble,        InputMap.BTN_RUMBLE );
        setupButton( view, R.id.buttonMempak,        InputMap.BTN_MEMPAK );
        setupButton( view, R.id.buttonIncrementSlot, InputMap.FUNC_INCREMENT_SLOT );
        setupButton( view, R.id.buttonSaveSlot,      InputMap.FUNC_SAVE_SLOT );
        setupButton( view, R.id.buttonLoadSlot,      InputMap.FUNC_LOAD_SLOT );
        setupButton( view, R.id.buttonReset,         InputMap.FUNC_RESET );
        setupButton( view, R.id.buttonStop,          InputMap.FUNC_STOP );
        setupButton( view, R.id.buttonPause,         InputMap.FUNC_PAUSE );
        setupButton( view, R.id.buttonFastForward,   InputMap.FUNC_FAST_FORWARD );
        setupButton( view, R.id.buttonFrameAdvance,  InputMap.FUNC_FRAME_ADVANCE );
        setupButton( view, R.id.buttonSpeedUp,       InputMap.FUNC_SPEED_UP );
        setupButton( view, R.id.buttonSpeedDown,     InputMap.FUNC_SPEED_DOWN );
        setupButton( view, R.id.buttonGameshark,     InputMap.FUNC_GAMESHARK );
        // @formatter:on
        
        // Setup analog axis listening, if applicable
        if( AppData.IS_HONEYCOMB_MR1 )
            mProvider.addProvider( new AxisProvider( view ) );
        
        // Refresh the dialog view
        updateViews();
    }
    
    @Override
    protected void onPrepareDialogBuilder( Builder builder )
    {
        super.onPrepareDialogBuilder( builder );
        
        // Setup key listening
        mUnmappableKeyCodes = ( new UserPrefs( getContext() ) ).unmappableKeyCodes;
        mProvider.addProvider( new KeyProvider( builder, ImeFormula.DEFAULT, mUnmappableKeyCodes ) );
        
        // Add neutral button to toggle special function visibility
        int resId = mSpecialKeysVisible
                ? R.string.inputMapPreference_hideSpecial
                : R.string.inputMapPreference_showSpecial;
        builder.setNeutralButton( resId, new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                mSpecialKeysVisible = !mSpecialKeysVisible;
                mDoReclick = true;
            }
        } );
    }
    
    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        // Unregister parent providers, new ones added on next click
        mProvider.removeAllProviders();
        
        // Clicking Cancel or Ok returns us to the parent preference menu. We must return to a clean
        // state so that the toggle doesn't persist unwanted changes.
        
        if( mDoReclick )
        {
            // User pressed neutral button: keep dirty state by immediately reopening
            mDoReclick = false;
            onClick();
        }
        else if( positiveResult )
        {
            // User pressed Ok: clean the state by persisting map
            persistString( mMap.serialize() );
            notifyChanged();
        }
        else
        {
            // User pressed Cancel/Back: clean the state by restoring map
            mMap.deserialize( getPersistedString( "" ) );
        }
    }
    
    @Override
    public void onClick( View view )
    {
        // Handle button clicks in the mapping screen
        Button button;
        for( int i = 0; i < mN64Button.length; i++ )
        {
            // Find the button that was pressed
            if( view.equals( mN64Button[i] ) )
            {
                // Popup a dialog to listen to input codes from user
                final int index = i;
                button = (Button) view;
                String message = getContext().getString( R.string.inputMapPreference_popupMessage,
                        mMap.getMappedCodeInfo( index ) );
                String btnText = getContext().getString( R.string.inputMapPreference_popupUnmap );
                
                Prompt.promptInputCode( getContext(), button.getText(), message, btnText,
                        mUnmappableKeyCodes, new OnInputCodeListener()
                        {
                            @Override
                            public void OnInputCode( int inputCode, int hardwareId )
                            {
                                if( inputCode == 0 )
                                    mMap.unmapCommand( index );
                                else
                                    mMap.map( inputCode, index );
                                updateViews();
                            }
                        } );
            }
        }
    }
    
    @Override
    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
    {
        // Handle the toggle button in the preferences menu
        if( buttonView.equals( mToggleWidget ) )
        {
            mMap.setEnabled( isChecked );
            persistString( mMap.serialize() );
        }
    }
    
    @Override
    public void onInput( int inputCode, float strength, int hardwareId )
    {
        updateViews( inputCode, strength );
    }
    
    @Override
    public void onInput( int[] inputCodes, float[] strengths, int hardwareId )
    {
        // Nothing to do here, just implement the interface
    }
    
    @TargetApi( 11 )
    private void updateViews( int inputCode, float strength )
    {
        // Modify the button appearance to provide feedback to user
        int selectedIndex = mMap.get( inputCode );
        for( int i = 0; i < mN64Button.length; i++ )
        {
            // Highlight the currently active button
            Button button = mN64Button[i];
            if( button != null )
            {
                button.setPressed( i == selectedIndex
                        && strength > AbstractProvider.STRENGTH_THRESHOLD );
                
                // Fade any buttons that aren't mapped
                if( AppData.IS_HONEYCOMB )
                {
                    if( mMap.isMapped( i ) )
                        button.setAlpha( 1 );
                    else
                        button.setAlpha( UNMAPPED_BUTTON_ALPHA );
                }
                else
                {
                    // For older APIs try something similar (not quite the same)
                    if( mMap.isMapped( i ) )
                        button.getBackground().clearColorFilter();
                    else
                        button.getBackground().setColorFilter( UNMAPPED_BUTTON_FILTER,
                                PorterDuff.Mode.MULTIPLY );
                    button.invalidate();
                }
            }
        }
        
        // Update the feedback text (not all layouts include this, so check null)
        if( mFeedbackText != null )
        {
            mFeedbackText.setText( strength > AbstractProvider.STRENGTH_THRESHOLD
                    ? AbstractProvider.getInputName( inputCode )
                    : "" );
        }
    }
    
    private void updateViews()
    {
        // Default update, don't highlight anything
        updateViews( 0, 0 );
    }
    
    private void setupButton( View parentView, int resId, int index )
    {
        mN64Button[index] = (Button) parentView.findViewById( resId );
        if( mN64Button[index] != null )
            mN64Button[index].setOnClickListener( this );
    }
}
