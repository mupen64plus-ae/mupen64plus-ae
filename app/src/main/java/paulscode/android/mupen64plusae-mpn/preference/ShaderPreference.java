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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae-mpn.preference;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;

import org.mupen64plusae-mpn.v3.alpha.R;

import java.util.ArrayList;

import paulscode.android.mupen64plusae-mpn.compat.AppCompatPreferenceActivity.OnPreferenceDialogListener;
import paulscode.android.mupen64plusae-mpn.game.ShaderLoader;
import paulscode.android.mupen64plusae-mpn.persistent.AppData;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class ShaderPreference extends ListPreference implements OnPreferenceDialogListener
{
    public interface OnRemove {

        /**
         * Called when this preference is requested to be removed
         * @param key key for this preference
         */
        void onRemove(String key);
    }

    private OnRemove mOnRemoveCallback = null;
    public ShaderPreference(Context context )
    {
        super( context );
    }

    public ShaderPreference(Context context, AttributeSet attrs )
    {
        super( context, attrs );
    }
    
    @Override
    public void onPrepareDialogBuilder( Context context, Builder builder )
    {
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
            context, R.layout.list_preference, getEntries());

        int currentIndex = findIndexOfValue(getCurrentValue(null));
        builder.setTitle(getTitle());
        builder.setPositiveButton(null, null);
        builder.setSingleChoiceItems(adapter, currentIndex, (dialog, item) -> {
            setValue(getEntryValues()[item].toString());
            dialog.dismiss();
        });
        builder.setNeutralButton( R.string.preferenceRemove_title, (dialog, which) -> {
            if (mOnRemoveCallback != null) {
                mOnRemoveCallback.onRemove(getKey());
            }
        });
    }

    public void setOnRemoveCallback(OnRemove onRemoveCallback) {
        mOnRemoveCallback = onRemoveCallback;
    }

    /**
     * Populate shader options
     */
    public void populateShaderOptions(Context context)
    {
        ArrayList<CharSequence> entriesList = new ArrayList<>();
        ArrayList<CharSequence> valuesList = new ArrayList<>();

        for (ShaderLoader shaderLoader : ShaderLoader.values()) {
            String entryHtml = context.getString(shaderLoader.getFriendlyName());
            String description = context.getString(shaderLoader.getDescription());
            if (!TextUtils.isEmpty(description))
                entryHtml += "<br><small>" + description + "</small>";


            entriesList.add(AppData.fromHtml(entryHtml));
            valuesList.add(shaderLoader.toString());
        }

        CharSequence[] entriesArray = entriesList.toArray(new CharSequence[0]);
        CharSequence[] valuesArray = valuesList.toArray(new CharSequence[0]);
        setEntries(entriesArray);
        setEntryValues(valuesArray);

        String selectedValue = getPersistedString( null );
        setValue( selectedValue );
    }
    
    public String getCurrentValue(String defaultValue)
    {
        return getPersistedString( defaultValue );
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