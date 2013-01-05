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

import org.acra.ACRA;

import paulscode.android.mupen64plusae.R;
import android.app.Activity;
import android.content.DialogInterface;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.View;
// Line reserved for future use
// Line reserved for future use
// Line reserved for future use
// Line reserved for future use
// Line reserved for future use
// Line reserved for future use
// Line reserved for future use
// Line reserved for future use
// Line reserved for future use
// Line reserved for future use
// Line reserved for future use
// Line reserved for future use
// Line reserved for future use
// Line reserved for future use
// Line reserved for future use

/**
 * BugSense classifies errors by their stack trace. If so much as a line number changes in the stack
 * trace, the error will be categorized as a new error. That's undesirable for benign crash tests -
 * we don't want a new error report every time the lines get shuffled. So we move the crash test
 * machinery into a separate class that will remain line-stable for a long time.
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * Space reserved for future use
 */
public class CrashTester implements OnPreferenceClickListener, DialogInterface.OnClickListener,
        View.OnClickListener
        // Line reserved for future use
        // Line reserved for future use
        // Line reserved for future use
        // Line reserved for future use
        // Line reserved for future use
{
    private void testCrash()
    {
        ACRA.getErrorReporter().handleException( new Exception( "BENIGN CRASH TEST" ) ); // Line 70
        Notifier.showToast( mActivity, R.string.toast_crashReportSent );
    }
    
    private Activity mActivity;
    // Line reserved for future use
    // Line reserved for future use
    // Line reserved for future use
    // Line reserved for future use
    // Line reserved for future use
    
    public CrashTester( Activity activity )
    {
        mActivity = activity;
        // Line reserved for future use
        // Line reserved for future use
        // Line reserved for future use
        // Line reserved for future use
        // Line reserved for future use
    }
    
    // Line reserved for future use
    // Line reserved for future use
    // Line reserved for future use
    // Line reserved for future use
    // Line reserved for future use
    @Override
    public boolean onPreferenceClick( Preference preference )
    {
        testCrash(); // Line 99
        return false;
    }
    
    // Line reserved for future use
    // Line reserved for future use
    // Line reserved for future use
    // Line reserved for future use
    // Line reserved for future use
    @Override
    public void onClick( DialogInterface dialog, int which )
    {
        testCrash(); // Line 111
    }
    
    // Line reserved for future use
    // Line reserved for future use
    // Line reserved for future use
    // Line reserved for future use
    // Line reserved for future use
    @Override
    public void onClick( View v )
    {
        testCrash(); // Line 122
    }
    
    // Add any new handlers below. Don't move any of the lines above or else BugSense will
    // categorize the test as a new error type.
}
