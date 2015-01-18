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

import org.apache.commons.lang.ArrayUtils;
import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.GameOverlay;
import paulscode.android.mupen64plusae.Keys;
import paulscode.android.mupen64plusae.SettingsGlobalActivity;
import paulscode.android.mupen64plusae.dialog.SeekBarGroup;
import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.map.TouchMap;
import paulscode.android.mupen64plusae.input.map.VisibleTouchMap;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.FloatMath;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class TouchscreenProfileActivity extends Activity implements OnTouchListener
{
    private static final String TOUCHSCREEN_AUTOHOLDABLES = "touchscreenAutoholdables";
    private static final String AUTOHOLDABLES_DELIMITER = "~";
    
    private static final String ANALOG = "analog";
    private static final String DPAD = "dpad";
    private static final String GROUP_AB = "groupAB";
    private static final String GROUP_C = "groupC";
    private static final String BUTTON_L = "buttonL";
    private static final String BUTTON_R = "buttonR";
    private static final String BUTTON_Z = "buttonZ";
    private static final String BUTTON_S = "buttonS";
    private static final String TAG_X = "-x";
    private static final String TAG_Y = "-y";
    
    public static final SparseArray<String> READABLE_NAMES = new SparseArray<String>();
    
    // The inital or disabled x/y position of an asset
    private static final int INITIAL_ASSET_POS = 50;
    private static final int DISABLED_ASSET_POS = -1;
    
    // Touchscreen profile objects
    private ConfigFile mConfigFile;
    private Profile mProfile;
    
    // User preferences wrapper
    private UserPrefs mUserPrefs;
    
    // Visual elements
    private VisibleTouchMap mTouchscreenMap;
    private GameOverlay mOverlay;
    private ImageView mSurface;
    
    @TargetApi( 11 )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get the user preferences wrapper
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
        
        // Load the profile; fail fast if there are any programmer usage errors
        Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "Invalid usage: bundle must indicate profile name" );
        String name = extras.getString( Keys.Extras.PROFILE_NAME );
        if( TextUtils.isEmpty( name ) )
            throw new Error( "Invalid usage: profile name cannot be null or empty" );
        mConfigFile = new ConfigFile( mUserPrefs.touchscreenProfiles_cfg );
        ConfigSection section = mConfigFile.get( name );
        if( section == null )
            throw new Error( "Invalid usage: profile name not found in config file" );
        mProfile = new Profile( false, section );
        
        // Define the map from N64 button indices to readable button names
        READABLE_NAMES.put( AbstractController.DPD_R, getString( R.string.controller_dpad ) );
        READABLE_NAMES.put( AbstractController.DPD_L, getString( R.string.controller_dpad ) );
        READABLE_NAMES.put( AbstractController.DPD_D, getString( R.string.controller_dpad ) );
        READABLE_NAMES.put( AbstractController.DPD_U, getString( R.string.controller_dpad ) );
        READABLE_NAMES.put( AbstractController.START, getString( R.string.controller_buttonS ) );
        READABLE_NAMES.put( AbstractController.BTN_Z, getString( R.string.controller_buttonZ ) );
        READABLE_NAMES.put( AbstractController.BTN_B, getString( R.string.controller_buttonB ) );
        READABLE_NAMES.put( AbstractController.BTN_A, getString( R.string.controller_buttonA ) );
        READABLE_NAMES.put( AbstractController.CPD_R, getString( R.string.controller_buttonCr ) );
        READABLE_NAMES.put( AbstractController.CPD_L, getString( R.string.controller_buttonCl ) );
        READABLE_NAMES.put( AbstractController.CPD_D, getString( R.string.controller_buttonCd ) );
        READABLE_NAMES.put( AbstractController.CPD_U, getString( R.string.controller_buttonCu ) );
        READABLE_NAMES.put( AbstractController.BTN_R, getString( R.string.controller_buttonR ) );
        READABLE_NAMES.put( AbstractController.BTN_L, getString( R.string.controller_buttonL ) );
        READABLE_NAMES.put( TouchMap.DPD_LU, getString( R.string.controller_dpad ) );
        READABLE_NAMES.put( TouchMap.DPD_LD, getString( R.string.controller_dpad ) );
        READABLE_NAMES.put( TouchMap.DPD_RD, getString( R.string.controller_dpad ) );
        READABLE_NAMES.put( TouchMap.DPD_RU, getString( R.string.controller_dpad ) );
        
        // For Honeycomb, let the action bar overlay the rendered view (rather than squeezing it)
        // For earlier APIs, remove the title bar to yield more space
        Window window = getWindow();
        if( mUserPrefs.isActionBarAvailable )
            window.requestFeature( Window.FEATURE_ACTION_BAR_OVERLAY );
        else
            window.requestFeature( Window.FEATURE_NO_TITLE );
        
        // Enable full-screen mode
        window.setFlags( LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN );
        
        // Lay out content and get the views
        setContentView( R.layout.touchscreen_profile_activity );
        mSurface = (ImageView) findViewById( R.id.gameSurface );
        mOverlay = (GameOverlay) findViewById( R.id.gameOverlay );
        
        // Configure the action bar introduced in higher Android versions
        if( mUserPrefs.isActionBarAvailable )
        {
            getActionBar().hide();
            ColorDrawable color = new ColorDrawable( Color.parseColor( "#303030" ) );
            color.setAlpha( mUserPrefs.displayActionBarTransparency );
            getActionBar().setBackgroundDrawable( color );
        }
        
        // Initialize the touchmap and overlay
        mTouchscreenMap = new VisibleTouchMap( getResources() );
        mOverlay.setOnTouchListener( this );
        mOverlay.initialize( mTouchscreenMap, true, 1, mUserPrefs.touchscreenRefresh );
    }
    
    @TargetApi( 11 )
    private void refresh()
    {
        // Reposition the assets and refresh the overlay and options menu
        mTouchscreenMap.load( mUserPrefs.touchscreenSkin, mProfile,
                mUserPrefs.touchscreenRefresh > 0, true, mUserPrefs.touchscreenScale,
                mUserPrefs.touchscreenTransparency );
        mOverlay.postInvalidate();
        if( AppData.IS_HONEYCOMB )
            invalidateOptionsMenu();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        
        // Refresh in case the global settings changed
        mUserPrefs = new UserPrefs( this );
        
        // Update the dummy GameSurface size in case global settings changed
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mSurface.getLayoutParams();
        params.width = mUserPrefs.videoSurfaceWidth;
        params.height = mUserPrefs.videoSurfaceHeight;
        params.gravity = mUserPrefs.displayPosition | Gravity.CENTER_HORIZONTAL;
        mSurface.setLayoutParams( params );
        
        // Refresh the touchscreen controls
        refresh();
    }
    
    @Override
    public void onWindowFocusChanged( boolean hasFocus )
    {
        super.onWindowFocusChanged( hasFocus );
        if( hasFocus )
            hideSystemBars();
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        
        // Lazily persist the profile data; only need to do it on pause
        mProfile.writeTo( mConfigFile );
        mConfigFile.save();
    }
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.touchscreen_profile_activity, menu );
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onPrepareOptionsMenu( Menu menu )
    {
        setCheckState( menu, R.id.menuItem_analog, ANALOG );
        setCheckState( menu, R.id.menuItem_dpad, DPAD );
        setCheckState( menu, R.id.menuItem_groupAB, GROUP_AB );
        setCheckState( menu, R.id.menuItem_groupC, GROUP_C );
        setCheckState( menu, R.id.menuItem_buttonL, BUTTON_L );
        setCheckState( menu, R.id.menuItem_buttonR, BUTTON_R );
        setCheckState( menu, R.id.menuItem_buttonZ, BUTTON_Z );
        setCheckState( menu, R.id.menuItem_buttonS, BUTTON_S );
        return super.onPrepareOptionsMenu( menu );
    }
    
    private void setCheckState( Menu menu, int id, String assetName )
    {
        MenuItem item = menu.findItem( id );
        if( item != null )
            item.setChecked( hasAsset( assetName ) );
    }
    
    private boolean hasAsset( String assetName )
    {
        // Get the asset position from the profile and see if it's valid
        int x = mProfile.getInt( assetName + TAG_X, DISABLED_ASSET_POS );
        int y = mProfile.getInt( assetName + TAG_Y, DISABLED_ASSET_POS );
        return ( x > DISABLED_ASSET_POS ) && ( y > DISABLED_ASSET_POS );
    }
    
    @Override
    public boolean onMenuItemSelected( int featureId, MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.menuItem_globalSettings:
                Intent intent = new Intent( this, SettingsGlobalActivity.class );
                intent.putExtra( Keys.Extras.MENU_DISPLAY_MODE, 1 );
                startActivity( intent );
                return true;
            case R.id.menuItem_exit:
                finish();
                return true;
            case R.id.menuItem_analog:
                toggleAsset( ANALOG );
                return true;
            case R.id.menuItem_dpad:
                toggleAsset( DPAD );
                return true;
            case R.id.menuItem_groupAB:
                toggleAsset( GROUP_AB );
                return true;
            case R.id.menuItem_groupC:
                toggleAsset( GROUP_C );
                return true;
            case R.id.menuItem_buttonL:
                toggleAsset( BUTTON_L );
                return true;
            case R.id.menuItem_buttonR:
                toggleAsset( BUTTON_R );
                return true;
            case R.id.menuItem_buttonZ:
                toggleAsset( BUTTON_Z );
                return true;
            case R.id.menuItem_buttonS:
                toggleAsset( BUTTON_S );
                return true;
            default:
                return super.onMenuItemSelected( featureId, item );
        }
    }
    
    private void toggleAsset( String assetName )
    {
        // Change the position of the asset to show/hide
        int newPosition = hasAsset( assetName ) ? DISABLED_ASSET_POS : INITIAL_ASSET_POS;
        mProfile.putInt( assetName + TAG_X, newPosition );
        mProfile.putInt( assetName + TAG_Y, newPosition );
        refresh();
    }
    
    private void setHoldable( int n64Index, boolean holdable )
    {
        String index = String.valueOf( n64Index );
        
        // Get the serialized list from the profile
        String serialized = mProfile.get( TOUCHSCREEN_AUTOHOLDABLES, "" );
        String[] holdables = serialized.split( AUTOHOLDABLES_DELIMITER );
        
        // Modify the list as necessary
        if( !holdable )
        {
            holdables = (String[]) ArrayUtils.removeElement( holdables, index );
        }
        else if( !ArrayUtils.contains( holdables, index ) )
        {
            holdables = (String[]) ArrayUtils.add( holdables, index );
        }
        
        // Put the serialized list back into the profile
        serialized = TextUtils.join( AUTOHOLDABLES_DELIMITER, holdables );
        mProfile.put( TOUCHSCREEN_AUTOHOLDABLES, serialized );
    }
    
    private boolean getHoldable( int n64Index )
    {
        String serialized = mProfile.get( TOUCHSCREEN_AUTOHOLDABLES, "" );
        String[] holdables = serialized.split( AUTOHOLDABLES_DELIMITER );
        return ArrayUtils.contains( holdables, String.valueOf( n64Index ) );
    }
    
    @SuppressLint( "InlinedApi" )
    @TargetApi( 11 )
    @Override
    public void onBackPressed()
    {
        // Only applies to Honeycomb devices
        if( !AppData.IS_HONEYCOMB )
            return;
        
        // Toggle the action bar
        ActionBar actionBar = getActionBar();
        if( actionBar.isShowing() )
        {
            hideSystemBars();
        }
        else
        {
            actionBar.show();
        }
    }
    
    @SuppressLint( "InlinedApi" )
    @TargetApi( 11 )
    private void hideSystemBars()
    {
        // Only applies to Honeycomb devices
        if( !AppData.IS_HONEYCOMB )
            return;
        
        getActionBar().hide();
        View view = mSurface.getRootView();
        if( view != null )
        {
            if( AppData.IS_KITKAT && mUserPrefs.isImmersiveModeEnabled )
                view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
            else
                view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE ); // == STATUS_BAR_HIDDEN for Honeycomb
        }
    }

    
    @Override
    public boolean onTouch( View v, MotionEvent event )
    {
        if( ( event.getAction() & MotionEvent.ACTION_MASK ) != MotionEvent.ACTION_DOWN )
            return false;
        
        int x = (int) event.getX();
        int y = (int) event.getY();
        
        // Get the N64 index of the button that was pressed
        int index = mTouchscreenMap.getButtonPress( x, y );
        if( index != TouchMap.UNMAPPED )
        {
            String assetName = TouchMap.ASSET_NAMES.get( index );
            String title = READABLE_NAMES.get( index );
            
            // D-pad buttons are not holdable
            if( DPAD.equals( assetName ) )
                index = -1;
            
            popupDialog( assetName, title, index );
        }
        else
        {
            // See if analog was pressed
            Point point = mTouchscreenMap.getAnalogDisplacement( x, y );
            int dX = point.x;
            int dY = point.y;
            float displacement = FloatMath.sqrt( ( dX * dX ) + ( dY * dY ) );
            if( mTouchscreenMap.isInCaptureRange( displacement ) )
                popupDialog( ANALOG, getString( R.string.controller_analog ), -1 );
        }
        
        return true;
    }
    
    @SuppressLint( "InflateParams" )
    private void popupDialog( final String assetName, String title, final int holdableIndex )
    {
        // Get the original position of the asset
        final int initialX = mProfile.getInt( assetName + TAG_X, INITIAL_ASSET_POS );
        final int initialY = mProfile.getInt( assetName + TAG_Y, INITIAL_ASSET_POS );
        
        // Inflate the dialog's main view area
        View view = getLayoutInflater().inflate( R.layout.touchscreen_profile_activity_popup, null );
        
        // Setup the dialog's compound seekbar widgets
        final SeekBarGroup posX = new SeekBarGroup( initialX, view, R.id.seekbarX,
                R.id.buttonXdown, R.id.buttonXup, R.id.textX,
                getString( R.string.touchscreenProfileActivity_horizontalSlider ),
                new SeekBarGroup.Listener()
                {
                    @Override
                    public void onValueChanged( int value )
                    {
                        mProfile.put( assetName + TAG_X, String.valueOf( value ) );
                        refresh();
                    }
                } );
        
        final SeekBarGroup posY = new SeekBarGroup( initialY, view, R.id.seekbarY,
                R.id.buttonYdown, R.id.buttonYup, R.id.textY,
                getString( R.string.touchscreenProfileActivity_verticalSlider ),
                new SeekBarGroup.Listener()
                {
                    @Override
                    public void onValueChanged( int value )
                    {
                        mProfile.put( assetName + TAG_Y, String.valueOf( value ) );
                        refresh();
                    }
                } );
        
        // Setup the auto-holdability checkbox
        CheckBox holdable = (CheckBox) view.findViewById( R.id.checkBox_holdable );
        if( holdableIndex < 0 )
        {
            // This is not a holdable button
            holdable.setVisibility( View.GONE );
        }
        else
        {
            holdable.setChecked( getHoldable( holdableIndex ) );
            holdable.setOnCheckedChangeListener( new OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
                {
                    setHoldable( holdableIndex, isChecked );
                }
            } );
        }
        
        // Setup the listener for the dialog's bottom buttons (ok, cancel, etc.)
        OnClickListener listener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_NEGATIVE )
                {
                    // Revert asset to original position if user cancels
                    posX.revertValue();
                    posY.revertValue();
                }
                else if( which == DialogInterface.BUTTON_NEUTRAL )
                {
                    // Remove the asset from this profile
                    toggleAsset( assetName );
                }
            }
        };
        
        // Create and show the popup dialog
        Builder builder = new Builder( this );
        builder.setTitle( title );
        builder.setView( view );
        builder.setNegativeButton( getString( android.R.string.cancel ), listener );
        builder.setNeutralButton( getString( R.string.touchscreenProfileActivity_remove ), listener );
        builder.setPositiveButton( getString( android.R.string.ok ), listener );
        builder.setCancelable( false );
        builder.create().show();
    }
}
