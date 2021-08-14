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
package paulscode.android.mupen64plusae.input;

import java.util.ArrayList;

import paulscode.android.mupen64plusae.jni.CoreFragment;

/**
 * The abstract base class for implementing all N64 controllers.
 * <p/>
 * Subclasses should implement the following pattern:
 * <ul>
 * <li>Register a listener to the upstream input (e.g. touch, keyboard, mouse, joystick, etc.).</li>
 * <li>Translate the input data into N64 controller button/axis states, and set the values of the
 * protected fields mState.buttons and mState.axisFraction* accordingly.</li>
 * <li>Call the protected method notifyChanged().</li>
 * </ul>
 * This abstract class will call the emulator's native libraries to update game state whenever
 * notifyChanged() is called. Subclasses should not call any native methods themselves. (If they do,
 * then this abstract class should be expanded to cover those needs.)
 * <p>
 * Note that this class is stateful, in that it remembers controller button/axis state between calls
 * from the subclass. For best performance, subclasses should only call notifyChanged() when the
 * input state has actually changed, and should bundle the protected field modifications before
 * calling notifyChanged(). For example,
 * 
 * <pre>
 * {@code
 * buttons[0] = true; notifyChanged(); buttons[1] = false; notifyChanged(); // Inefficient
 * buttons[0] = true; buttons[1] = false; notifyChanged(); // Better
 * }
 * </pre>
 * 
 * @see PeripheralController
 * @see TouchController
 */
public abstract class AbstractController
{
    /**
     * A small class that encapsulates controller state.
     */
    protected static class State
    {
        /** The pressed state of each controller button. */
        public boolean[] buttons = new boolean[NUM_N64_BUTTONS];
        
        /** The fractional value of the analog-x axis, between -1 and 1, inclusive. */
        float axisFractionX = 0;
        
        /** The fractional value of the analog-y axis, between -1 and 1, inclusive. */
        float axisFractionY = 0;
    }
    
    // Constants must match EButton listing in plugin.h! (input-sdl plug-in)
    
    /** N64 button: dpad-right. */
    public static final int DPD_R = 0;
    
    /** N64 button: dpad-left. */
    public static final int DPD_L = 1;
    
    /** N64 button: dpad-down. */
    public static final int DPD_D = 2;
    
    /** N64 button: dpad-up. */
    public static final int DPD_U = 3;
    
    /** N64 button: start. */
    public static final int START = 4;
    
    /** N64 button: trigger-z. */
    public static final int BTN_Z = 5;
    
    /** N64 button: b. */
    public static final int BTN_B = 6;
    
    /** N64 button: a. */
    public static final int BTN_A = 7;
    
    /** N64 button: cpad-right. */
    public static final int CPD_R = 8;
    
    /** N64 button: cpad-left. */
    public static final int CPD_L = 9;
    
    /** N64 button: cpad-down. */
    public static final int CPD_D = 10;
    
    /** N64 button: cpad-up. */
    public static final int CPD_U = 11;
    
    /** N64 button: shoulder-r. */
    public static final int BTN_R = 12;
    
    /** N64 button: shoulder-l. */
    public static final int BTN_L = 13;

    /** Total number of N64 buttons. */
    public static final int NUM_N64_BUTTONS = 16;
    
    /** The state of all four player controllers. */
    private static final ArrayList<State> sStates = new ArrayList<>();
    
    /** The state of this controller. */
    State mState;
    
    /** The player number, between 1 and 4, inclusive. */
    int mPlayerNumber = 1;

    private CoreFragment mCoreFragment;
    
    static
    {
        sStates.add( new State() );
        sStates.add( new State() );
        sStates.add( new State() );
        sStates.add( new State() );
    }
    
    /**
     * Instantiates a new abstract controller.
     */
    AbstractController(CoreFragment coreFragment)
    {
        mCoreFragment = coreFragment;
        mState = sStates.get( 0 );
    }
    
    /**
     * Notifies the core that the N64 controller state has changed.
     */
    void notifyChanged(boolean isKeyboard)
    {
        mCoreFragment.setControllerState( mPlayerNumber - 1, mState.buttons, mState.axisFractionX, mState.axisFractionY, isKeyboard );
    }
    
    /**
     * Sets the player number.
     * 
     * @param player The new player number, between 1 and 4, inclusive.
     */
    void setPlayerNumber( int player )
    {
        mPlayerNumber = player;
        mState = sStates.get( mPlayerNumber - 1 );
    }
}
