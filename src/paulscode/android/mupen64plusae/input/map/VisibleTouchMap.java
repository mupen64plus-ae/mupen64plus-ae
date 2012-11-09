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
 * Authors: paulscode, lioncash, littleguy77
 */
package paulscode.android.mupen64plusae.input.map;

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.util.Image;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.SubscriptionManager;
import paulscode.android.mupen64plusae.util.Utility;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.util.Log;

public class VisibleTouchMap extends TouchMap
{
    public interface Listener
    {
        public void onAllChanged( VisibleTouchMap touchMap );
        
        public void onHatChanged( VisibleTouchMap touchMap, float x, float y );
        
        public void onFpsChanged( VisibleTouchMap touchMap, int fps );
    }
    
    // Listener management
    SubscriptionManager<Listener> mPublisher;
    
    // Frame rate indicator
    private Image fpsImage;
    private int fpsXpercent;
    private int fpsYpercent;
    private int fpsNumXpercent;
    private int fpsNumYpercent;
    private int fpsRecalcPeriod;
    private int fpsValue;
    private String fpsFont;
    private Image[] numeralImages;
    private Image[] fpsDigits;
    
    public VisibleTouchMap( Resources resources )
    {
        super( resources );
        
        for( int i = 0; i < numeralImages.length; i++ )
            numeralImages[i] = new Image( resources, Globals.paths.fontsDir + fpsFont + "/" + i
                    + ".png" );
        
        mPublisher = new SubscriptionManager<VisibleTouchMap.Listener>();
    }
    
    public void registerListener( Listener listener )
    {
        mPublisher.subscribe( listener );
    }
    
    public void unregisterListener( Listener listener )
    {
        mPublisher.unsubscribe( listener );
    }
    
    @Override
    public void clear()
    {
        super.clear();
        fpsImage = null;
        fpsXpercent = fpsYpercent = 0;
        fpsNumXpercent = fpsNumYpercent = 50;
        fpsRecalcPeriod = 15;
        fpsValue = 0;
        fpsFont = "Mupen64Plus-AE-Contrast-Blue";
        numeralImages = new Image[10];
        fpsDigits = new Image[4];
    }
    
    @Override
    public void resize( int w, int h )
    {
        super.resize( w, h );
        
        // Position the FPS box
        if( fpsImage != null )
        {
            // Position the background image and draw it
            fpsImage.fitCenter( (int) ( (float) w * ( (float) fpsXpercent / 100.0f ) ),
                    (int) ( (float) h * ( (float) fpsYpercent / 100.0f ) ), w, h );
        }
        
        // Notify listeners that everything has changed
        for( Listener listener : mPublisher.getSubscribers() )
            listener.onAllChanged( this );
    }
    
    public void updateAnalog( float axisFractionX, float axisFractionY )
    {
        // Move the analog hat based on analog state
        analogForeX = analogBackImage.hWidth + (int) ( axisFractionX * (float) analogMaximum );
        analogForeY = analogBackImage.hHeight - (int) ( axisFractionY * (float) analogMaximum );
        
        // Notify listeners that analog hat changed
        for( Listener listener : mPublisher.getSubscribers() )
            listener.onHatChanged( this, axisFractionX, axisFractionY );
    }
    
    public void updateFps( int fps )
    {
        // Clamp to positive, four digits max [0 - 9999]
        fps = Utility.clamp(fps, 0, 9999);
        
        // Quick return if user has disabled FPS or it hasn't changed
        if( !Globals.userPrefs.isFrameRateEnabled || fpsValue == fps )
            return;
        
        // Store the new value
        fpsValue = fps;
        
        // Assemble a sprite for the FPS value
        String fpsString = Integer.toString( fpsValue );
        for( int i = 0; i < 4; i++ )
        {
            // Create a new sequence of numeral images
            if( i < fpsString.length() )
            {
                try
                {
                    // Clone the numeral from the font images
                    fpsDigits[i] = new Image( mResources, numeralImages[Integer.valueOf( fpsString
                            .substring( i, i + 1 ) )] );
                }
                catch( NumberFormatException nfe )
                {
                    // Skip this digit, there was a problem
                    fpsDigits[i] = null;
                }
            }
            else
            {
                // Skip this digit
                fpsDigits[i] = null;
            }
        }
        
        // Notify listeners that FPS sprite changed
        for( Listener listener : mPublisher.getSubscribers() )
            listener.onFpsChanged( this, fpsValue );
    }
    
    public int getFpsRecalcPeriod()
    {
        return fpsRecalcPeriod;
    }
    
    public void drawButtons( Canvas canvas )
    {
        // Draw the buttons onto the canvas
        for( Image button : buttonImages )
            button.draw( canvas );
    }
    
    public void drawAnalog( Canvas canvas )
    {
        if( analogBackImage == null )
            return;
        
        // Draw the background image first
        analogBackImage.draw( canvas );
        
        // Then draw the movable part of the stick
        if( analogForeImage != null )
        {
            // Reposition the image and draw it
            int hX = analogForeX;
            int hY = analogForeY;
            if( hX == -1 )
                hX = analogBackImage.hWidth;
            if( hY == -1 )
                hY = analogBackImage.hHeight;
            analogForeImage.fitCenter( analogBackImage.x + hX, analogBackImage.y + hY, analogBackImage.x,
                    analogBackImage.y, analogBackImage.width, analogBackImage.height );
            analogForeImage.draw( canvas );
        }
    }
    
    public void drawFps( Canvas canvas )
    {
        // Redraw the FPS indicator
        int x = 0;
        int y = 0;
        
        if( fpsImage != null )
        {
            x = fpsImage.x + (int) ( (float) fpsImage.width * ( (float) fpsNumXpercent / 100.0f ) );
            y = fpsImage.y + (int) ( (float) fpsImage.height * ( (float) fpsNumYpercent / 100.0f ) );
            fpsImage.draw( canvas );
        }
        
        int totalWidth = 0;
        
        // Calculate the width of the FPS text
        for( int i = 0; i < fpsDigits.length; i++ )
        {
            if( fpsDigits[i] != null )
                totalWidth += fpsDigits[i].width;
        }
        
        // Calculate the starting position of the FPS text
        x = x - (int) ( (float) totalWidth / 2f );
        
        // Draw each digit of the FPS number
        for( int i = 0; i < fpsDigits.length; i++ )
        {
            if( fpsDigits[i] != null )
            {
                fpsDigits[i].setPos( x, y - fpsDigits[i].hHeight );
                fpsDigits[i].draw( canvas );
                x += fpsDigits[i].width;
            }
        }
    }
    
    @Override
    protected void loadAssetSection( String directory, String filename, ConfigSection section,
            String assetType )
    {
        if( assetType.contains( "fps" ) )
            loadFpsIndicator( directory, filename, section );
        else
            super.loadAssetSection( directory, filename, section, assetType );            
    }
    
    private void loadFpsIndicator( final String layoutFolder, String filename, ConfigSection section )
    {
        fpsImage = new Image( mResources, layoutFolder + "/" + filename + ".png" );
        
        // Position (percentages of the screen dimensions)
        fpsXpercent = SafeMethods.toInt( section.get( "x" ), 0 );
        fpsYpercent = SafeMethods.toInt( section.get( "y" ), 0 );
        
        // Number position (percentages of the FPS indicator dimensions)
        fpsNumXpercent = SafeMethods.toInt( section.get( "numx" ), 50 );
        fpsNumYpercent = SafeMethods.toInt( section.get( "numy" ), 50 );
        
        // Refresh rate (in frames.. integer greater than 1)
        fpsRecalcPeriod = SafeMethods.toInt( section.get( "rate" ), 15 );
        
        // Need at least 2 frames to calculate FPS
        if( fpsRecalcPeriod < 2 )
            fpsRecalcPeriod = 2;
        
        // Numeral font
        fpsFont = section.get( "font" );
        if( fpsFont != null && fpsFont.length() > 0 )
        {
            // Load the font images
            int i = 0;
            try
            {
                // Make sure we can load them (they might not even exist)
                for( i = 0; i < 10; i++ )
                    numeralImages[i] = new Image( mResources, Globals.paths.fontsDir + fpsFont
                            + "/" + i + ".png" );
            }
            catch( Exception e )
            {
                // Problem, let the user know
                Log.e( "GamePad", "Problem loading font '" + Globals.paths.fontsDir + fpsFont + "/"
                        + i + ".png', error message: " + e.getMessage() );
            }
        }
    }
}
