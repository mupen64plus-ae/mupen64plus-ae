/*
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

import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import org.apache.commons.lang3.ArrayUtils;
import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.MenuListView;
import paulscode.android.mupen64plusae.dialog.MenuDialogFragment;
import paulscode.android.mupen64plusae.dialog.MenuDialogFragment.OnDialogMenuItemSelectedListener;
import paulscode.android.mupen64plusae.dialog.SeekBarGroup;
import paulscode.android.mupen64plusae.game.GameOverlay;
import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.map.TouchMap;
import paulscode.android.mupen64plusae.input.map.VisibleTouchMap;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.DisplayWrapper;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;

public class TouchscreenProfileActivity extends AppCompatActivity implements OnTouchListener, OnDialogMenuItemSelectedListener
{
    private static final String TOUCHSCREEN_NOT_AUTOHOLDABLES = "touchscreenNotAutoHoldables";
    private static final String INVERT_TOUCH_X_AXIS = "invertTouchXAxis";
    private static final String INVERT_TOUCH_Y_AXIS = "invertTouchYAxis";
    private static final String NOT_AUTOHOLDABLES_DELIMITER = "~";
    private static final String STATE_MENU_DIALOG_FRAGMENT = "STATE_MENU_DIALOG_FRAGMENT";
    
    private static final String ANALOG = "analog";
    private static final String DPAD = "dpad";
    private static final String GROUP_AB = "groupAB";
    private static final String BUTTON_A = "buttonA";
    private static final String BUTTON_B = "buttonB";
    private static final String GROUP_C = "groupC";
    private static final String BUTTON_CR = "buttonCr";
    private static final String BUTTON_CL = "buttonCl";
    private static final String BUTTON_CD = "buttonCd";
    private static final String BUTTON_CU = "buttonCu";
    private static final String BUTTON_L = "buttonL";
    private static final String BUTTON_R = "buttonR";
    private static final String BUTTON_Z = "buttonZ";
    private static final String BUTTON_S = "buttonS";
    private static final String BUTTON_SENSOR = "buttonSen";
    private static final String TAG_X = "-x";
    private static final String TAG_Y = "-y";
    private static final String SCALE = "-scale";
    
    public static final SparseArray<String> READABLE_NAMES = new SparseArray<>();
    
    // The inital or disabled x/y position of an asset
    private static final int INITIAL_ASSET_POS = 50;
    private static final int DISABLED_ASSET_POS = -1;
    
    // Touchscreen profile objects
    private ConfigFile mConfigFile;
    private Profile mProfile;
    
    // User preferences wrapper
    private GlobalPrefs mGlobalPrefs;

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
    
    // This is to prevent more than one popup appearing at once
    private boolean mPopupBeingShown;

    @Override
    protected void attachBaseContext(Context newBase) {
        if(TextUtils.isEmpty(LocaleContextWrapper.getLocalCode()))
        {
            super.attachBaseContext(newBase);
        }
        else
        {
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase,LocaleContextWrapper.getLocalCode()));
        }
    }
    
    @SuppressLint( "ClickableViewAccessibility" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get the user preferences wrapper
        AppData appData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs( this, appData);
        
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
        {
            Profile profile = new Profile(false, name, "");
            profile.writeTo(mConfigFile);
            mConfigFile.save();
            section = mConfigFile.get( name );
        }
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
        DisplayWrapper.setFullScreen(this);
        
        // Lay out content and get the views
        setContentView( R.layout.touchscreen_profile_activity );
        mSurface = findViewById( R.id.gameSurface );
        mOverlay = findViewById( R.id.gameOverlay );
        
        // Initialize the touchmap and overlay
        mTouchscreenMap = new VisibleTouchMap( getResources() );
        mOverlay.setOnTouchListener( this );

        mPopupBeingShown = false;

        hideSystemBars();
    }
    
    private void refresh()
    {
        // Reposition the assets and refresh the overlay and options menu
        mOverlay.initialize( mTouchscreenMap, true, false, mGlobalPrefs.isTouchscreenAnimated);
        mTouchscreenMap.load( mGlobalPrefs.isCustomTouchscreenSkin ? null : this,
                mGlobalPrefs.touchscreenSkinPath, mProfile,
                mGlobalPrefs.isTouchscreenAnimated, mGlobalPrefs.touchscreenScale,
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
        
        // Update the dummy SurfaceView size in case global settings changed
        // FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mSurface.getLayoutParams();
        // params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        // mSurface.setLayoutParams( params );
        
        // Refresh the touchscreen controls
        refresh();
    }
    
    @Override
    public void onWindowFocusChanged( boolean hasFocus ) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
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

        // Disable A/B buttons to be separated if the skin is not supporting it.
        if(!mTouchscreenMap.isSplitABSkin())
        {
            UpdateButtonMenu(listView, R.id.menuItem_buttonA);
            UpdateButtonMenu(listView, R.id.menuItem_buttonB);
        }

        // Disable C buttons to be separated if the skin is not supporting it.
        if(!mTouchscreenMap.isSplitCSkin())
        {
            UpdateButtonMenu(listView, R.id.menuItem_buttonCR);
            UpdateButtonMenu(listView, R.id.menuItem_buttonCL);
            UpdateButtonMenu(listView, R.id.menuItem_buttonCD);
            UpdateButtonMenu(listView, R.id.menuItem_buttonCU);
        }

        setCheckState( menu, R.id.menuItem_buttonA, BUTTON_A );
        setCheckState( menu, R.id.menuItem_buttonB, BUTTON_B );
        setCheckState( menu, R.id.menuItem_groupAB, GROUP_AB );
        setCheckState( menu, R.id.menuItem_buttonCR, BUTTON_CR );
        setCheckState( menu, R.id.menuItem_buttonCL, BUTTON_CL );
        setCheckState( menu, R.id.menuItem_buttonCD, BUTTON_CD );
        setCheckState( menu, R.id.menuItem_buttonCU, BUTTON_CU );
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
        if (item.getItemId() == R.id.menuItem_globalSettings) {
            ActivityHelper.startTouchscreenPrefsActivity( this );
        } else if (item.getItemId() == R.id.menuItem_sensorConfiguration) {
            new SensorConfigurationDialog(this, mProfile).show();
        } else if (item.getItemId() == R.id.menuItem_exit) {
            finish();
        } else if (item.getItemId() == R.id.menuItem_analog) {
            toggleAsset( ANALOG );
        } else if (item.getItemId() == R.id.menuItem_dpad) {
            toggleAsset( DPAD );
        } else if (item.getItemId() == R.id.menuItem_groupAB) {
            disableAssetNoRefresh( BUTTON_A );
            disableAssetNoRefresh( BUTTON_B );
            toggleAsset( GROUP_AB );
        } else if (item.getItemId() == R.id.menuItem_buttonA) {
            disableAssetNoRefresh( GROUP_AB );
            toggleAsset( BUTTON_A );
        } else if (item.getItemId() == R.id.menuItem_buttonB) {
            disableAssetNoRefresh( GROUP_AB );
            toggleAsset( BUTTON_B );
        } else if (item.getItemId() == R.id.menuItem_groupC) {
            disableAssetNoRefresh( BUTTON_CR );
            disableAssetNoRefresh( BUTTON_CL );
            disableAssetNoRefresh( BUTTON_CD );
            disableAssetNoRefresh( BUTTON_CU );
            toggleAsset( GROUP_C );
        } else if (item.getItemId() == R.id.menuItem_buttonCR) {
            disableAssetNoRefresh( GROUP_C );
            toggleAsset( BUTTON_CR );
        } else if (item.getItemId() == R.id.menuItem_buttonCL) {
            disableAssetNoRefresh( GROUP_C );
            toggleAsset( BUTTON_CL );
        } else if (item.getItemId() == R.id.menuItem_buttonCD) {
            disableAssetNoRefresh( GROUP_C );
            toggleAsset( BUTTON_CD );
        } else if (item.getItemId() == R.id.menuItem_buttonCU) {
            disableAssetNoRefresh( GROUP_C );
            toggleAsset( BUTTON_CU );
        } else if (item.getItemId() == R.id.menuItem_buttonL) {
            toggleAsset( BUTTON_L );
        } else if (item.getItemId() == R.id.menuItem_buttonR) {
            toggleAsset( BUTTON_R );
        } else if (item.getItemId() == R.id.menuItem_buttonZ) {
            toggleAsset( BUTTON_Z );
        } else if (item.getItemId() == R.id.menuItem_buttonS) {
            toggleAsset( BUTTON_S );
        } else if (item.getItemId() == R.id.menuItem_buttonSensor) {
            toggleAsset( BUTTON_SENSOR );
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

    private void disableAssetNoRefresh( String assetName )
    {
        // Change the position of the asset to hide
        mProfile.putInt( assetName + TAG_X, DISABLED_ASSET_POS );
        mProfile.putInt( assetName + TAG_Y, DISABLED_ASSET_POS );
    }
    
    private void setNotHoldable( int n64Index, boolean holdable )
    {
        String index = String.valueOf( n64Index );
        
        // Get the serialized list from the profile
        String serialized = mProfile.get(TOUCHSCREEN_NOT_AUTOHOLDABLES, "" );
        String[] notHoldables = serialized.split(NOT_AUTOHOLDABLES_DELIMITER);
        
        // Modify the list as necessary
        if( holdable )
        {
            notHoldables = ArrayUtils.removeElement( notHoldables, index );
        }
        else if( !ArrayUtils.contains( notHoldables, index ) )
        {
            notHoldables = ArrayUtils.add( notHoldables, index );
        }
        
        // Put the serialized list back into the profile
        serialized = TextUtils.join(NOT_AUTOHOLDABLES_DELIMITER, notHoldables );
        mProfile.put(TOUCHSCREEN_NOT_AUTOHOLDABLES, serialized );
    }
    
    private boolean getNotHoldable( int n64Index )
    {
        String serialized = mProfile.get(TOUCHSCREEN_NOT_AUTOHOLDABLES, "" );
        String[] holdables = serialized.split(NOT_AUTOHOLDABLES_DELIMITER);
        return ArrayUtils.contains( holdables, String.valueOf( n64Index ) );
    }
    
    @SuppressLint( "InlinedApi" )
    private void hideSystemBars()
    {
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        DisplayWrapper.setFullScreen(this);

        if( mGlobalPrefs.isImmersiveModeEnabled )
            DisplayWrapper.enableImmersiveMode(this);
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
            
            if( mGlobalPrefs.isImmersiveModeEnabled )
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
                Point point = mTouchscreenMap.getAnalogDisplacementOriginal( x, y );
                if( mTouchscreenMap.isInCaptureRange( point ) )
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
                int newDragX = ( x - ( initialX - dragFrame.left ) ) * 100/( mOverlay.getWidth() - ( dragFrame.right - dragFrame.left ) );
                int newDragY = ( y - ( initialY - dragFrame.top ) ) * 100/( mOverlay.getHeight() - ( dragFrame.bottom - dragFrame.top ) );

                newDragX = Math.min( Math.max( newDragX, 0 ), 110 );
                newDragY = Math.min( Math.max( newDragY, 0 ), 110 );
                
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
        if(mPopupBeingShown) {
            return;
        }

        // Don't allow a null asset name
        if (assetName == null) {
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
                value -> {
                    mProfile.put( assetName + TAG_X, String.valueOf( value ) );
                    refresh();
                });
        
        final SeekBarGroup posY = new SeekBarGroup( initialY, view, R.id.seekbarY,
                R.id.buttonYdown, R.id.buttonYup, R.id.textY,
                getString( R.string.touchscreenProfileActivity_verticalSlider ),
                value -> {
                    mProfile.put( assetName + TAG_Y, String.valueOf( value ) );
                    refresh();
                });
        
        final SeekBarGroup scale = new SeekBarGroup( initialScale, view, R.id.seekbarScale,
                R.id.buttonScaleDown, R.id.buttonScaleUp, R.id.textScale,
                getString( R.string.touchscreenProfileActivity_ScaleSlider ), 5, 1, 0, 200,
                value -> {
                    mProfile.put( assetName + SCALE, String.valueOf( value ) );
                    refresh();
                });
        
        // Setup the visual feedback checkbox
        CheckBox hide = view.findViewById( R.id.checkBox_hideJoystick );
        CheckBox invertTouchXAxis = view.findViewById( R.id.checkBox_invertTouchXAxis );
        CheckBox invertTouchYAxis = view.findViewById( R.id.checkBox_invertTouchYAxis );


        if( assetName.equals("analog") )
        {
            hide.setChecked(Boolean.parseBoolean(mProfile.get("touchscreenHideAnalogWhenSensor")));
            invertTouchXAxis.setChecked(Boolean.parseBoolean(mProfile.get(INVERT_TOUCH_X_AXIS, "False")));
            invertTouchYAxis.setChecked(Boolean.parseBoolean(mProfile.get(INVERT_TOUCH_Y_AXIS, "False")));

            hide.setOnCheckedChangeListener((buttonView, isChecked) -> mProfile.put("touchscreenHideAnalogWhenSensor", ( isChecked ? "True" : "False" )));
            invertTouchXAxis.setOnCheckedChangeListener((buttonView, isChecked) -> mProfile.put(INVERT_TOUCH_X_AXIS, isChecked ? "True" : "False"));
            invertTouchYAxis.setOnCheckedChangeListener((buttonView, isChecked) -> mProfile.put(INVERT_TOUCH_Y_AXIS, isChecked ? "True" : "False"));
        }
        else
        {
            hide.setVisibility(View.GONE);
            invertTouchXAxis.setVisibility(View.GONE);
            invertTouchYAxis.setVisibility(View.GONE);
        }
        
        // Setup the auto-holdability checkbox
        CheckBox holdable = view.findViewById( R.id.checkBox_holdable );
        if( holdableIndex < 0 )
        {
            // This is not a holdable button
            holdable.setVisibility( View.GONE );
        }
        else
        {
            holdable.setChecked( !getNotHoldable( holdableIndex ) );
            holdable.setOnCheckedChangeListener((buttonView, isChecked) -> setNotHoldable( holdableIndex, isChecked ));
        }
        
        // Setup the listener for the dialog's bottom buttons (ok, cancel, etc.)
        OnClickListener listener = (dialog, which) -> {
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
