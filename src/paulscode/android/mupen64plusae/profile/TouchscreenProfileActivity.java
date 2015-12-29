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

import java.io.File;

import org.apache.commons.lang.ArrayUtils;
import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.MenuListView;
import paulscode.android.mupen64plusae.dialog.MenuDialogFragment;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.MenuDialogFragment.OnDialogMenuItemSelectedListener;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptFileListener;
import paulscode.android.mupen64plusae.dialog.SeekBarGroup;
import paulscode.android.mupen64plusae.game.GameOverlay;
import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.map.TouchMap;
import paulscode.android.mupen64plusae.input.map.VisibleTouchMap;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class TouchscreenProfileActivity extends AppCompatActivity implements OnTouchListener, OnDialogMenuItemSelectedListener
{
    private static final String TOUCHSCREEN_AUTOHOLDABLES = "touchscreenAutoHoldables";
    private static final String AUTOHOLDABLES_DELIMITER = "~";
    private static final String STATE_MENU_DIALOG_FRAGMENT = "STATE_MENU_DIALOG_FRAGMENT";
    
    private static final String ANALOG = "analog";
    private static final String DPAD = "dpad";
    private static final String GROUP_AB = "groupAB";
    private static final String BUTTON_A = "buttonA";
    private static final String BUTTON_B = "buttonB";
    private static final String GROUP_C = "groupC";
    private static final String BUTTON_L = "buttonL";
    private static final String BUTTON_R = "buttonR";
    private static final String BUTTON_Z = "buttonZ";
    private static final String BUTTON_S = "buttonS";
    private static final String BUTTON_SENSOR = "buttonSen";
    private static final String TAG_X = "-x";
    private static final String TAG_Y = "-y";
    private static final String SCALE = "-scale";
    
    public static final SparseArray<String> READABLE_NAMES = new SparseArray<String>();
    
    // The inital or disabled x/y position of an asset
    private static final int INITIAL_ASSET_POS = 50;
    private static final int DISABLED_ASSET_POS = -1;
    
    // Touchscreen profile objects
    private ConfigFile mConfigFile;
    private Profile mProfile;
    
    // User preferences wrapper
    private GlobalPrefs mGlobalPrefs;
    private AppData mAppData;
    
    // Visual elements
    private VisibleTouchMap mTouchscreenMap;
    private GameOverlay mOverlay;
    private ImageView mSurface;
    
    // Live drag and drop editing
    private int initialX;
    private int initialY;
    private int dragIndex;
    private boolean dragging;
    private String dragAsset;
    private int dragX;
    private int dragY;
    private Rect dragFrame;
    
    // The directory of the selected touchscreen skin.
    private String touchscreenSkin;
    
    // This is to prevent more than one popup appearing at once
    private boolean mPopupBeingShown;
    
    @SuppressLint( "ClickableViewAccessibility" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get the user preferences wrapper
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        mGlobalPrefs.enforceLocale( this );
        
        // Load the profile; fail fast if there are any programmer usage errors
        Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "Invalid usage: bundle must indicate profile name" );
        String name = extras.getString( ActivityHelper.Keys.PROFILE_NAME );
        if( TextUtils.isEmpty( name ) )
            throw new Error( "Invalid usage: profile name cannot be null or empty" );
        mConfigFile = new ConfigFile( mGlobalPrefs.touchscreenProfiles_cfg );
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
        READABLE_NAMES.put( TouchMap.TOGGLE_SENSOR, getString( R.string.controller_buttonSensor ) );
        
        // Enable full-screen mode
        getWindow().setFlags( LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN );
        
        // Lay out content and get the views
        setContentView( R.layout.touchscreen_profile_activity );
        mSurface = (ImageView) findViewById( R.id.gameSurface );
        mOverlay = (GameOverlay) findViewById( R.id.gameOverlay );
        
        String layout = mProfile.get( "touchscreenSkin", "Outline" );
        if( layout.equals( "Custom" ) )
            touchscreenSkin =  mProfile.get( "touchscreenCustomSkinPath", "" );
        else
            touchscreenSkin = mAppData.touchscreenSkinsDir + layout;
        
        // Initialize the touchmap and overlay
        mTouchscreenMap = new VisibleTouchMap( getResources() );
        mOverlay.setOnTouchListener( this );
        
        mPopupBeingShown = false;
    }
    
    private void refresh()
    {
        // Reposition the assets and refresh the overlay and options menu
        boolean isTouchscreenAnimated = Boolean.valueOf(mProfile.get( "touchscreenAnimated", "False" ));
        mOverlay.initialize( mTouchscreenMap, true, mGlobalPrefs.isFpsEnabled, false, isTouchscreenAnimated);
        mTouchscreenMap.load( touchscreenSkin, mProfile,
                isTouchscreenAnimated, true, mGlobalPrefs.touchscreenScale,
                mGlobalPrefs.touchscreenTransparency );
        mOverlay.postInvalidate();
        invalidateOptionsMenu();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        
        // Refresh in case the global settings changed
        AppData appData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, appData );
        
        // Update the dummy GameSurface size in case global settings changed
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mSurface.getLayoutParams();
        params.gravity = mGlobalPrefs.displayPosition | Gravity.CENTER_HORIZONTAL;
        mSurface.setLayoutParams( params );
        
        // Update the screen orientation in case global settings changed
        this.setRequestedOrientation( mGlobalPrefs.displayOrientation );
        
        // Refresh the touchscreen controls
        refresh();
    }
    
    @Override
    public void onWindowFocusChanged( boolean hasFocus )
    {
        super.onWindowFocusChanged( hasFocus );
        if( hasFocus )
        {
            hideSystemBars();
        }
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
    public void onPrepareMenuList(MenuListView listView)
    {
        Menu menu = listView.getMenu();
        setCheckState( menu, R.id.menuItem_analog, ANALOG );
        setCheckState( menu, R.id.menuItem_dpad, DPAD );
        
        if(mTouchscreenMap.isABSplit())
        {
            UpdateButtonMenu(listView, R.id.menuItem_groupAB);
        }
        else
        {
            UpdateButtonMenu(listView, R.id.menuItem_buttonA);
            UpdateButtonMenu(listView, R.id.menuItem_buttonB);
        }        
        
        setCheckState( menu, R.id.menuItem_buttonA, BUTTON_A );
        setCheckState( menu, R.id.menuItem_buttonB, BUTTON_B );
        setCheckState( menu, R.id.menuItem_groupAB, GROUP_AB );
        setCheckState( menu, R.id.menuItem_groupC, GROUP_C );
        setCheckState( menu, R.id.menuItem_buttonL, BUTTON_L );
        setCheckState( menu, R.id.menuItem_buttonR, BUTTON_R );
        setCheckState( menu, R.id.menuItem_buttonZ, BUTTON_Z );
        setCheckState( menu, R.id.menuItem_buttonS, BUTTON_S );
        setCheckState( menu, R.id.menuItem_buttonSensor, BUTTON_SENSOR );
    }
    
    private void UpdateButtonMenu(MenuListView listView, int menuItemId)
    {
        MenuItem buttonGroupItem = listView.getMenu().findItem(R.id.menuItem_buttons);
        
        if(buttonGroupItem != null && listView.getMenu().findItem(menuItemId) != null)
        {
            buttonGroupItem.getSubMenu().removeItem(menuItemId);
        }
    }
    
    @Override
    public void onDialogMenuItemSelected( int dialogId, MenuItem item)
    {        
        switch( item.getItemId() )
        {
            case R.id.menuItem_globalSettings:
                ActivityHelper.startGlobalPrefsActivity( this, 1 );
                return;
            case R.id.menuItem_sensorConfiguration:
                new SensorConfigurationDialog(this, mProfile).show();
                return;
            case R.id.menuItem_exit:
                finish();
                return;
            case R.id.menuItem_analog:
                toggleAsset( ANALOG );
                return;
            case R.id.menuItem_dpad:
                toggleAsset( DPAD );
                return;
            case R.id.menuItem_groupAB:
                toggleAsset( GROUP_AB );
                return;
            case R.id.menuItem_buttonA:
                toggleAsset( BUTTON_A );
                return;
            case R.id.menuItem_buttonB:
                toggleAsset( BUTTON_B );
                return;
            case R.id.menuItem_groupC:
                toggleAsset( GROUP_C );
                return;
            case R.id.menuItem_buttonL:
                toggleAsset( BUTTON_L );
                return;
            case R.id.menuItem_buttonR:
                toggleAsset( BUTTON_R );
                return;
            case R.id.menuItem_buttonZ:
                toggleAsset( BUTTON_Z );
                return;
            case R.id.menuItem_buttonS:
                toggleAsset( BUTTON_S );
                return;
            case R.id.menuItem_buttonSensor:
                toggleAsset( BUTTON_SENSOR );
                return;
            case R.id.menuItem_outline:
                touchscreenSkin = mAppData.touchscreenSkinsDir + "Outline";
                mProfile.put( "touchscreenSkin", "Outline" );
                refresh();
                return;
            case R.id.menuItem_shaded:
                touchscreenSkin = mAppData.touchscreenSkinsDir + "Shaded";
                mProfile.put( "touchscreenSkin", "Shaded" );
                refresh();
                return;
            case R.id.menuItem_custom:
                loadCustomSkinFromPrompt();
                return;
            default:
                return;
        }
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
    
    private void toggleAsset( String assetName )
    {
        // Change the position of the asset to show/hide
        int newPosition = hasAsset( assetName ) ? DISABLED_ASSET_POS : INITIAL_ASSET_POS;
        mProfile.putInt( assetName + TAG_X, newPosition );
        mProfile.putInt( assetName + TAG_Y, newPosition );
        refresh();
    }
    
    private void loadCustomSkinFromPrompt()
    {
        CharSequence title = this.getText( R.string.touchscreenStyle_entryCustom );
        File startPath = new File( mGlobalPrefs.touchscreenCustomSkinsDir );
        Prompt.promptDirectory( this, title, null, startPath, new PromptFileListener()
        {
            @Override
            public void onDialogClosed( File file, int which )
            {
                if( which >= 0 )
                {
                    touchscreenSkin = file.getAbsolutePath();
                    mProfile.put( "touchscreenSkin", "Custom" );
                    mProfile.put( "touchscreenCustomSkinPath", touchscreenSkin );
                    refresh();
                }
            }
        } );
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
    private void hideSystemBars()
    {
        getSupportActionBar().hide();
        View view = mSurface.getRootView();
        if( view != null )
        {
            if( AppData.IS_KITKAT && mGlobalPrefs.isImmersiveModeEnabled )
                view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
            else
                view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE ); // == STATUS_BAR_HIDDEN for Honeycomb
        }
    }

    
    @SuppressLint( "ClickableViewAccessibility" )
    @Override
    public boolean onTouch( View v, MotionEvent event )
    {
        int x = (int) event.getX();
        int y = (int) event.getY();
        
        if( ( event.getAction() & MotionEvent.ACTION_MASK ) == MotionEvent.ACTION_DOWN )
        {
            initialX = x;
            initialY = y;
            dragIndex = TouchMap.UNMAPPED;
            dragging = false;
            dragAsset = "";
            
            if( AppData.IS_KITKAT && mGlobalPrefs.isImmersiveModeEnabled )
            {
                // ignore edge swipes.
                // unfortunately KitKat lacks a way to do this on its own,
                // so just ignore all touches along the edges.
                // http://stackoverflow.com/questions/20530333/ignore-immersive-mode-swipe
                View view = getWindow().getDecorView();
                if ( y < 10 || y > view.getHeight() - 10 || x < 10 || x > view.getWidth() - 10 )
                    return false;
            }
            
            // Get the N64 index of the button that was pressed
            int index = mTouchscreenMap.getButtonPress( x, y );
            if( index != TouchMap.UNMAPPED )
            {
                dragIndex = index;
                dragAsset = TouchMap.ASSET_NAMES.get( index );
                dragFrame = mTouchscreenMap.getButtonFrame( dragAsset );
            }
            else
            {
                // See if analog was pressed
                Point point = mTouchscreenMap.getAnalogDisplacement( x, y );
                int dX = point.x;
                int dY = point.y;
                float displacement = (float) Math.sqrt( ( dX * dX ) + ( dY * dY ) );
                if( mTouchscreenMap.isInCaptureRange( displacement ) )
                {
                    dragAsset = ANALOG;
                    dragFrame = mTouchscreenMap.getAnalogFrame();
                }
                else
                {
                    int resId = R.menu.touchscreen_profile_activity;
                    int stringId = R.string.touchscreenProfileActivity_menuTitle;
                    
                    MenuDialogFragment menuDialogFragment = MenuDialogFragment.newInstance(0,
                       getString(stringId), resId);
                    
                    FragmentManager fm = getSupportFragmentManager();
                    menuDialogFragment.show(fm, STATE_MENU_DIALOG_FRAGMENT);
                }
            }
            
            dragX = mProfile.getInt( dragAsset + TAG_X, INITIAL_ASSET_POS );
            dragY = mProfile.getInt( dragAsset + TAG_Y, INITIAL_ASSET_POS );
            
            return true;
        }
        else if( ( event.getAction() & MotionEvent.ACTION_MASK ) == MotionEvent.ACTION_MOVE )
        {
            if ( dragIndex != TouchMap.UNMAPPED || ANALOG.equals(dragAsset) )
            {
                if ( !dragging )
                {
                    int dX = x - initialX;
                    int dY = y - initialY;
                    float displacement = (float) Math.sqrt( ( dX * dX ) + ( dY * dY ) );
                    if ( displacement >= 10 )
                        dragging = true;
                }
                if ( !dragging )
                    return false;
                
                // drag this button or analog stick around
                
                // calculate the X and Y percentage
                View view = getWindow().getDecorView();
                int newDragX = ( x - ( initialX - dragFrame.left ) ) * 100/( view.getWidth() - ( dragFrame.right - dragFrame.left ) );
                int newDragY = ( y - ( initialY - dragFrame.top ) ) * 100/( view.getHeight() - ( dragFrame.bottom - dragFrame.top ) );
                
                newDragX = Math.min( Math.max( newDragX, 0 ), 100 );
                newDragY = Math.min( Math.max( newDragY, 0 ), 100 );
                
                if ( newDragX != dragX || newDragY != dragY )
                {
                    dragX = newDragX;
                    dragY = newDragY;
                    mProfile.put( dragAsset + TAG_X, String.valueOf( newDragX ) );
                    mProfile.put( dragAsset + TAG_Y, String.valueOf( newDragY ) );
                    mTouchscreenMap.refreshButtonPosition( mProfile, dragAsset );
                    mOverlay.postInvalidate();
                }
            }
        }
        else if( ( event.getAction() & MotionEvent.ACTION_MASK ) == MotionEvent.ACTION_UP )
        {
            // if this touch was part of a drag/swipe gesture then don't tap the button
            if ( dragging )
                return false;
            
            // show the editor for the tapped button
            if ( ANALOG.equals( dragAsset ) )
            {
                // play the standard button sound effect
                View view = getWindow().getDecorView();
                view.playSoundEffect( SoundEffectConstants.CLICK );
                
                popupDialog( dragAsset, getString( R.string.controller_analog ), -1 );
            }
            else if( dragIndex != TouchMap.UNMAPPED )
            {
                int index = dragIndex;
                String title = READABLE_NAMES.get( dragIndex );
                
                // D-pad buttons and TOGGLE_SENSOR are not holdable
                if( DPAD.equals( dragAsset ) || TouchMap.TOGGLE_SENSOR == index )
                    index = -1;
                
                // play the standard button sound effect
                View view = getWindow().getDecorView();
                view.playSoundEffect( SoundEffectConstants.CLICK );
                
                popupDialog( dragAsset, title, index );
            }
            
            return true;
        }
        
        return false;
    }
    
    private void popupDialog( final String assetName, String title, final int holdableIndex )
    {
        //Prevent more than one pop at a time
        if(mPopupBeingShown)
        {
            return;
        }

        mPopupBeingShown = true;
        
        // Get the original position of the asset
        final int initialX = mProfile.getInt( assetName + TAG_X, INITIAL_ASSET_POS );
        final int initialY = mProfile.getInt( assetName + TAG_Y, INITIAL_ASSET_POS );
        final int initialScale = mProfile.getInt( assetName + SCALE, 100 );
        
        // Inflate the dialog's main view area
        View view = View.inflate( this, R.layout.touchscreen_profile_activity_popup, null );
        
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
        
        final SeekBarGroup scale = new SeekBarGroup( initialScale, view, R.id.seekbarScale,
                R.id.buttonScaleDown, R.id.buttonScaleUp, R.id.textScale,
                getString( R.string.touchscreenProfileActivity_ScaleSlider ), 5, 1, 0, 200,
                new SeekBarGroup.Listener()
                {
                    @Override
                    public void onValueChanged( int value )
                    {
                        mProfile.put( assetName + SCALE, String.valueOf( value ) );
                        refresh();
                    }
                } );
        
        // Setup the visual feedback checkbox
        CheckBox feedback = (CheckBox) view.findViewById( R.id.checkBox_feedback );
        CheckBox hide = (CheckBox) view.findViewById( R.id.checkBox_hideJoystick );
        if( assetName.equals("analog") )
        {
            feedback.setChecked(Boolean.valueOf(mProfile.get("touchscreenAnimated", "False")));
            hide.setChecked(Boolean.valueOf(mProfile.get("touchscreenHideAnalogWhenSensor")));
            feedback.setOnCheckedChangeListener( new OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
                {
                    mProfile.put( "touchscreenAnimated", ( isChecked ? "True" : "False" ) );
                    refresh();
                }
            } );
            hide.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mProfile.put("touchscreenHideAnalogWhenSensor", String.valueOf(isChecked));
                }
            } );
        }
        else
        {
            feedback.setVisibility( View.GONE);
            hide.setVisibility(View.GONE);
        }
        
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
                    scale.revertValue();
                }
                else if( which == DialogInterface.BUTTON_NEUTRAL )
                {
                    // Remove the asset from this profile
                    toggleAsset( assetName );
                }
                
                mPopupBeingShown = false;
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
