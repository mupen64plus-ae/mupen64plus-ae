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
package paulscode.android.mupen64plusae.util;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 * A lightweight class that permits multiple event handlers to respond to a single event.
 */
public class Demultiplexer
{
    /**
     * Demultiplexer for {@link android.view.View.OnKeyListener}s.
     * 
     * @see android.view.View.OnKeyListener#onKey(android.view.View, int, android.view.KeyEvent)
     */
    public static class OnKeyListener implements View.OnKeyListener
    {
        /** Listener management. */
        private final SubscriptionManager<View.OnKeyListener> mManager
                = new SubscriptionManager<View.OnKeyListener>();
        
        /**
         * Adds a listener.
         * 
         * @param listener The listener to add.
         */
        public void addListener( View.OnKeyListener listener )
        {
            mManager.subscribe( listener );
        }
        
        /**
         * Removes a listener.
         * 
         * @param listener The listener to remove.
         */
        public void removeListener( View.OnKeyListener listener )
        {
            mManager.unsubscribe( listener );
        }
        
        /**
         * Removes all listeners.
         */
        public void removeAllListeners()
        {
            mManager.unsubscribeAll();
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see android.view.View.OnKeyListener#onKey(android.view.View, int, android.view.KeyEvent)
         */
        @Override
        public boolean onKey( View view, int keyCode, KeyEvent event )
        {
            boolean result = false;
            for( View.OnKeyListener listener : mManager.getSubscribers() )
                result |= listener.onKey( view, keyCode, event );
            return result;
        }
    }
    
    /**
     * Demultiplexer for {@link android.view.View.OnTouchListener}s.
     * 
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    public static class OnTouchListener implements View.OnTouchListener
    {
        /** Listener management. */
        private final SubscriptionManager<View.OnTouchListener> mManager
                = new SubscriptionManager<View.OnTouchListener>();
        
        /**
         * Adds a listener.
         * 
         * @param listener The listener to add.
         */
        public void addListener( View.OnTouchListener listener )
        {
            mManager.subscribe( listener );
        }
        
        /**
         * Removes a listener.
         * 
         * @param listener The listener to remove.
         */
        public void removeListener( View.OnTouchListener listener )
        {
            mManager.unsubscribe( listener );
        }
        
        /**
         * Removes all listeners.
         */
        public void removeAllListeners()
        {
            mManager.unsubscribeAll();
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see android.view.View.OnTouchListener#onTouch(android.view.View,
         * android.view.MotionEvent)
         */
        @Override
        public boolean onTouch( View view, MotionEvent event )
        {
            boolean result = false;
            for( View.OnTouchListener listener : mManager.getSubscribers() )
                result |= listener.onTouch( view, event );
            return result;
        }
    }
}
