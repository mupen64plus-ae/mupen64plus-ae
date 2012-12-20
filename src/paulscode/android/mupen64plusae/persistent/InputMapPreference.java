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
 * 
 * Concise implementation inspired by
 * http://stackoverflow.com/questions/4505845/concise-way-of-writing-new-dialogpreference-classes
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
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.TextView;

public class InputMapPreference extends DialogPreference implements
        AbstractProvider.OnInputListener, DialogInterface.OnClickListener, View.OnClickListener
{
    private static final float UNMAPPED_BUTTON_ALPHA = 0.2f;
    private static final int UNMAPPED_BUTTON_FILTER = 0x66FFFFFF;
    private static final int MARGIN = 140;
    private static final int MIN_LAYOUT_WIDTH_DP = 440 + MARGIN;
    private static final int MIN_LAYOUT_HEIGHT_DP = 320 + MARGIN;
    
    private final InputMap mMap;
    private final LazyProvider mProvider;
    private View mToggleWidget;
    private TextView mFeedbackText;
    private Button[] mN64Button;
    private List<Integer> mUnmappableKeyCodes;
    
    public InputMapPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        mMap = new InputMap();
        mProvider = new LazyProvider();
        mProvider.registerListener( this );
        mN64Button = new Button[InputMap.NUM_PLAYER_INPUTS];
        setWidgetLayoutResource( R.layout.widget_toggle );
        
        // Select the appropriate dialog layout according to device configuration. Although you can
        // do this through the resource directory structure and layout aliases, we'll do it this way
        // for now since it's easier to maintain in the short term while the design is in flux.
        // TODO: Consider using resource directories to handle device variation, once design is set.
        WindowManager manager = (WindowManager) getContext().getSystemService( Context.WINDOW_SERVICE );
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics( metrics );
        float scalefactor = (float) DisplayMetrics.DENSITY_DEFAULT / (float) metrics.densityDpi;
        int widthDp = Math.round( metrics.widthPixels * scalefactor );
        int heightDp = Math.round( metrics.heightPixels * scalefactor );
        
        if( widthDp >= MIN_LAYOUT_WIDTH_DP && heightDp >= MIN_LAYOUT_HEIGHT_DP )
        {
            // Geometric layout for large screens
            setDialogLayoutResource( R.layout.input_map_preference );
        }
        else
        {
            // Hide the dialog title to yield more screen space
            setDialogTitle( null );
            
            // Choose the appropriate layout depending on device and orientation
            UserPrefs prefs = new UserPrefs( context );
            int orientation = getContext().getResources().getConfiguration().orientation;
            if( prefs.isXperiaEnabled )
            {
                // Special layout for Xperia PLAY devices (hide analog/c-pad icons)
                setDialogLayoutResource( orientation == Configuration.ORIENTATION_PORTRAIT
                        ? R.layout.input_map_preference_port_xplay
                        : R.layout.input_map_preference_land_xplay );
            }
            else
            {
                // Scrollable layout for small screens
                setDialogLayoutResource( orientation == Configuration.ORIENTATION_PORTRAIT
                        ? R.layout.input_map_preference_port
                        : R.layout.input_map_preference_land );
            }
        }
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
        mToggleWidget = view.findViewById( R.id.widgetToggle );
        mToggleWidget.setOnClickListener( this );
        ( (Checkable) mToggleWidget ).setChecked( mMap.isEnabled() );
    }
    
    @Override
    protected void onBindDialogView( View view )
    {
        // Set up the dialog view seen when the preference menu item is clicked
        super.onBindDialogView( view );
        
        // Get the text view object
        mFeedbackText = (TextView) view.findViewById( R.id.textFeedback );
        
        // Create a button map to simplify highlighting
        mN64Button[AbstractController.DPD_R] = (Button) view.findViewById( R.id.buttonDR );
        mN64Button[AbstractController.DPD_L] = (Button) view.findViewById( R.id.buttonDL );
        mN64Button[AbstractController.DPD_D] = (Button) view.findViewById( R.id.buttonDD );
        mN64Button[AbstractController.DPD_U] = (Button) view.findViewById( R.id.buttonDU );
        mN64Button[AbstractController.START] = (Button) view.findViewById( R.id.buttonS );
        mN64Button[AbstractController.BTN_Z] = (Button) view.findViewById( R.id.buttonZ );
        mN64Button[AbstractController.BTN_B] = (Button) view.findViewById( R.id.buttonB );
        mN64Button[AbstractController.BTN_A] = (Button) view.findViewById( R.id.buttonA );
        mN64Button[AbstractController.CPD_R] = (Button) view.findViewById( R.id.buttonCR );
        mN64Button[AbstractController.CPD_L] = (Button) view.findViewById( R.id.buttonCL );
        mN64Button[AbstractController.CPD_D] = (Button) view.findViewById( R.id.buttonCD );
        mN64Button[AbstractController.CPD_U] = (Button) view.findViewById( R.id.buttonCU );
        mN64Button[AbstractController.BTN_R] = (Button) view.findViewById( R.id.buttonR );
        mN64Button[AbstractController.BTN_L] = (Button) view.findViewById( R.id.buttonL );
        mN64Button[InputMap.AXIS_R] = (Button) view.findViewById( R.id.buttonAR );
        mN64Button[InputMap.AXIS_L] = (Button) view.findViewById( R.id.buttonAL );
        mN64Button[InputMap.AXIS_D] = (Button) view.findViewById( R.id.buttonAD );
        mN64Button[InputMap.AXIS_U] = (Button) view.findViewById( R.id.buttonAU );
        mN64Button[InputMap.BTN_RUMBLE] = (Button) view.findViewById( R.id.buttonRumble );
        mN64Button[InputMap.BTN_MEMPAK] = (Button) view.findViewById( R.id.buttonMempak );
        
        // Define the button click callbacks
        for( Button b : mN64Button )
        {
            if( b != null )
                b.setOnClickListener( this );
        }
        
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
        
        // Add a button for calibrating analog axes, if applicable
        if( AppData.IS_HONEYCOMB_MR1 )
            builder.setNeutralButton( R.string.inputMapPreference_calibrate, this );
    }
    
    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        // Clicking Cancel or Ok returns us to the parent preference menu. We must return to a clean
        // state so that the toggle doesn't persist unwanted changes.
        
        if( positiveResult )
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
        
        // Unregister parent providers, new ones added on next click
        mProvider.removeAllProviders();
    }
    
    @Override
    public void onClick( DialogInterface dialog, int which )
    {
        // Handle clicks on the main dialog buttons
        super.onClick( dialog, which );
        
        if( which == DialogInterface.BUTTON_NEUTRAL )
        {
            // Calibration button clicked on the main dialog
            // Due to a quirk in Android, analog axes whose center-point is not zero (e.g. an analog
            // trigger whose rest position is -1) still produce a zero value at rest until they have
            // been wiggled a little bit. After that point, their rest position is correctly
            // recorded. The problem is that LazyProvider calibrates the rest position of each
            // analog channel based on the first measurement it receives. As a workaround, we
            // provide a calibration button, which makes the user go through a little dance to
            // ensure all analog axes are pressed, then re-calibrates itself.
            // TODO: Find a solution that is automatic (e.g. LazyProvider calibrates per channel)
            
            // Remember the dirty state of the preference
            final String dirtyMap = mMap.serialize();
            
            // Prepare calibration dialog strings
            String title = getContext().getString( R.string.inputMapPreference_calibrate );
            String message = getContext().getString( R.string.inputMapPreference_calibrateMessage );
            
            // Prepare calibration dialog callbacks
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick( DialogInterface dialog2, int which2 )
                {
                    // Button clicked on the calibration dialog
                    
                    // Reset the strength biases if OK clicked
                    if( which2 == DialogInterface.BUTTON_POSITIVE )
                        mProvider.resetBiasesLast();
                    
                    // Reopen the mapping screen and restore the dirty state
                    InputMapPreference.this.onClick();
                    mMap.deserialize( dirtyMap );
                }
            };
            
            // Create and show the calibration dialog
            new Builder( getContext() ).setTitle( title ).setMessage( message )
                    .setNegativeButton( android.R.string.cancel, listener )
                    .setPositiveButton( android.R.string.ok, listener )
                    .setCancelable(false)
                    .create().show();
        }
    }
    
    @Override
    public void onClick( View view )
    {
        // Handle clicks on the widgets and icons
        
        // Handle the toggle button in the preferences menu
        if( view.equals( mToggleWidget ) )
        {
            boolean isEnabled = ( (Checkable) mToggleWidget ).isChecked();
            mMap.setEnabled( isEnabled );
            persistString( mMap.serialize() );
        }
        
        // Else, find the button that was clicked and map it
        else
        {
            // Find the button that was pressed
            Button button;
            for( int i = 0; i < mN64Button.length; i++ )
            {
                if( view.equals( mN64Button[i] ) )
                {
                    // Popup a dialog to listen to input codes from user
                    final int index = i;
                    button = (Button) view;
                    String message = getContext().getString( R.string.inputMapPreference_popupMessage );
                    String btnText = getContext().getString( R.string.inputMapPreference_popupPosButtonText );
                    Prompt.promptInputCode( getContext(), button.getText(), message, btnText,
                            mUnmappableKeyCodes, new OnInputCodeListener()
                            {
                                @Override
                                public void OnInputCode( int inputCode, int hardwareId )
                                {
                                    if( inputCode == 0 )
                                        mMap.unmapInput( index );
                                    else
                                        mMap.mapInput( index, inputCode );
                                    updateViews();
                                }
                            } );
                }
            }
            
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
        // Nothing to do here, just implements the interface
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
                button.setPressed( i == selectedIndex && strength > AbstractProvider.STRENGTH_THRESHOLD );
                
                // Fade any buttons that aren't mapped
                if( AppData.IS_HONEYCOMB )
                {
                    if( mMap.getMappedInputCodes()[i] == 0 )
                        button.setAlpha( UNMAPPED_BUTTON_ALPHA );
                    else
                        button.setAlpha( 1 );
                }
                else
                {
                    // For older API's try something similar (not quite the same)
                    if( mMap.getMappedInputCodes()[i] == 0 )
                        button.getBackground().setColorFilter( UNMAPPED_BUTTON_FILTER,
                                PorterDuff.Mode.MULTIPLY );
                    else
                        button.getBackground().clearColorFilter();
                    button.invalidate();
                }
            }
        }
        
        // Update the feedback text (not all layouts include this, so check null)
        if( mFeedbackText != null )
        {
            mFeedbackText.setText( strength > 0.5
                    ? AbstractProvider.getInputName( inputCode )
                    : "" );
        }
    }
    
    private void updateViews()
    {
        // Default update, don't highlight anything
        updateViews( 0, 0 );
    }
}
