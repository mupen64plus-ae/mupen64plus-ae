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
package paulscode.android.mupen64plusae.compat;

import android.app.ListActivity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

public class AppCompatListActivity extends ListActivity
{
    // Material Design theming (since we cannot inherit from AppCompatActivity)
    protected AppCompatDelegate mDelegate;
    
    public AppCompatDelegate getDelegate()
    {
        if( mDelegate == null )
            mDelegate = AppCompatDelegate.create( this, null );
        return mDelegate;
    }
    
    @Override
    public void addContentView( View view, LayoutParams params )
    {
        getDelegate().addContentView( view, params );
    }
    
    @Override
    public MenuInflater getMenuInflater()
    {
        return getDelegate().getMenuInflater();
    }
    
    public ActionBar getSupportActionBar()
    {
        return getDelegate().getSupportActionBar();
    }
    
    @Override
    public void invalidateOptionsMenu()
    {
        getDelegate().invalidateOptionsMenu();
    }
    
    @Override
    public void onConfigurationChanged( Configuration newConfig )
    {
        super.onConfigurationChanged( newConfig );
        getDelegate().onConfigurationChanged( newConfig );
    }
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        getDelegate().installViewFactory();
        getDelegate().onCreate( savedInstanceState );
        super.onCreate( savedInstanceState );
    }
    
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        getDelegate().onDestroy();
    }
    
    @Override
    protected void onPostCreate( Bundle savedInstanceState )
    {
        super.onPostCreate( savedInstanceState );
        getDelegate().onPostCreate( savedInstanceState );
    }
    
    @Override
    protected void onPostResume()
    {
        super.onPostResume();
        getDelegate().onPostResume();
    }
    
    @Override
    protected void onStop()
    {
        super.onStop();
        getDelegate().onStop();
    }
    
    @Override
    protected void onTitleChanged( CharSequence title, int color )
    {
        super.onTitleChanged( title, color );
        getDelegate().setTitle( title );
    }
    
    @Override
    public void setContentView( int layoutResID )
    {
        getDelegate().setContentView( layoutResID );
    }
    
    @Override
    public void setContentView( View view )
    {
        getDelegate().setContentView( view );
    }
    
    @Override
    public void setContentView( View view, LayoutParams params )
    {
        getDelegate().setContentView( view, params );
    }
    
    public void setSupportActionBar( Toolbar toolbar )
    {
        getDelegate().setSupportActionBar( toolbar );
    }
}
