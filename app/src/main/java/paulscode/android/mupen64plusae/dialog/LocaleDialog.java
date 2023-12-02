package paulscode.android.mupen64plusae.dialog;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.WordUtils;
import paulscode.android.mupen64plusae.R;

import java.util.Locale;

import paulscode.android.mupen64plusae.ActivityHelper;

public class LocaleDialog extends DialogFragment
{
    public static final String KEY_LOCALE_OVERRIDE = "localeOverride";
    public static final String DEFAULT_LOCALE_OVERRIDE = "";
    private static final String STATE_TITLE = "STATE_TITLE";

    private SharedPreferences mPreferences;
    private String mLocaleCode;
    private String[] mLocaleNames;
    private String[] mLocaleCodes;

    public static LocaleDialog newInstance(String title)
    {
        LocaleDialog frag = new LocaleDialog();
        Bundle args = new Bundle();
        args.putString(STATE_TITLE, title);

        frag.setArguments(args);
        return frag;
    }

    private Locale createLocale( String code )
    {
        final String[] codes = code.split( "_" );
        switch( codes.length )
        {
            case 1: // Language code provided
                return new Locale( codes[0] );
            case 2: // Language and country code provided
                return new Locale( codes[0], codes[1] );
            case 3: // Language, country, and variant code provided
                return new Locale( codes[0], codes[1], codes[2] );
            default: // Invalid input
                return null;
        }
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private void createLocales(){
        mPreferences = PreferenceManager.getDefaultSharedPreferences( requireContext() );
        mLocaleCode = mPreferences.getString( KEY_LOCALE_OVERRIDE, DEFAULT_LOCALE_OVERRIDE );
        final Locale[] availableLocales = Locale.getAvailableLocales();
        String[] values = requireContext().getResources().getStringArray( R.array.localeOverride_values );
        String[] entries = new String[values.length];
        for( int i = values.length - 1; i > 0; i-- )
        {
            final Locale locale = createLocale( values[i] );

            // Get intersection of languages (available on device) and (translated for Mupen)
            if( locale != null && ArrayUtils.contains( availableLocales, locale ) )
            {
                // Get the name of the language, as written natively
                entries[i] = WordUtils.capitalize( locale.getDisplayName( locale ) );
            }
            else
            {
                // Remove the item from the list
                entries = ArrayUtils.remove( entries, i );
                values = ArrayUtils.remove( values, i );
            }
        }
        entries[0] = requireContext().getString( R.string.localeOverride_entrySystemDefault );
        values[0] = Resources.getSystem().getConfiguration().locale.getLanguage();

        mLocaleNames = entries;
        mLocaleCodes = values;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final String title = getArguments() != null ? getArguments().getString(STATE_TITLE) : "";

        // Locale
        createLocales();

        Builder builder = new Builder(requireActivity());
        builder.setTitle(title);
        builder.setCancelable(false);
        final int currentIndex = ArrayUtils.indexOf( mLocaleCodes, mLocaleCode );
        builder.setSingleChoiceItems( mLocaleNames, currentIndex, (dialog, which) -> {
            dialog.dismiss();
            if( which >= 0 && which != currentIndex )
            {
                mPreferences.edit().putString( KEY_LOCALE_OVERRIDE, mLocaleCodes[which] ).apply();
                try{
                    requireActivity().finishAffinity();
                } catch(Exception e ){ e.printStackTrace(); }
                ActivityHelper.startSplashActivity(requireActivity());
            }
        });
        builder.setPositiveButton(null, null);

        return builder.create();
    }
}