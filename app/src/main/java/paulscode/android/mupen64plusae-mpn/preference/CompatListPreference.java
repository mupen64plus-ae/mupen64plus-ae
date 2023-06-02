/*
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
 * Authors: fzurita
 */
package paulscode.android.mupen64plusae-mpn.preference;

import org.mupen64plusae-mpn.v3.alpha.R;

import paulscode.android.mupen64plusae-mpn.compat.AppCompatPreferenceActivity.OnPreferenceDialogListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;

public class CompatListPreference extends ListPreference implements OnPreferenceDialogListener
{    
    public CompatListPreference( Context context )
    {
        super( context );
    }
    
    public CompatListPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
    }
    
    @Override
    public void onPrepareDialogBuilder(Context context, Builder builder)
    {
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(context, R.layout.list_preference,
                getEntries());

        int currentIndex = findIndexOfValue(getCurrentValue());
        builder.setTitle(getTitle());
        builder.setSingleChoiceItems(adapter, currentIndex, (dialog, item) -> {
            setValue(getEntryValues()[item].toString());
            dialog.dismiss();
        });
        builder.setPositiveButton(null, null);
    }
    
    public String getCurrentValue()
    {
        return getPersistedString( null );
    }

    @Override
    public void onBindDialogView(View view, FragmentActivity associatedActivity)
    {
        //Nothing to do here
    }

    @Override
    public void onDialogClosed(boolean result)
    {
        //Nothing to do here
    }
}