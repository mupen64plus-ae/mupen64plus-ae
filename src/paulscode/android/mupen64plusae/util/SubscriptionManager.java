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
package paulscode.android.mupen64plusae.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A small class to encapsulate the management of subscriptions.
 * 
 * @param <Subscriber> the type of object to subscribe
 */
public class SubscriptionManager<Subscriber>
{
    private ArrayList<Subscriber> mSubscribers;
    
    public SubscriptionManager()
    {
        mSubscribers = new ArrayList<Subscriber>();
    }
    
    public void subscribe( Subscriber subscriber )
    {
        if( ( subscriber != null ) && !mSubscribers.contains( subscriber ) )
            mSubscribers.add( subscriber );
    }
    
    public void unsubscribe( Subscriber subscriber )
    {
        if( subscriber != null )
            mSubscribers.remove( subscriber ); 
    }
    
    public void unsubscribeAll()
    {
        mSubscribers.clear();            
    }
    
    public List<Subscriber> getSubscribers()
    {
        return mSubscribers;
    }
}
