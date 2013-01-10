package paulscode.android.mupen64plusae.util;

import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;

public class PrefUtil
{
    @SuppressWarnings( "deprecation" )
    public static void setOnPreferenceClickListener( PreferenceActivity activity,
            String key, OnPreferenceClickListener listener )
    {
        Preference preference = activity.findPreference( key );
        if( preference != null )
            preference.setOnPreferenceClickListener( listener );
    }
    
    @SuppressWarnings( "deprecation" )
    public static void enablePreference( PreferenceActivity activity, String key, boolean enabled )
    {
        Preference preference = activity.findPreference( key );
        if( preference != null )
            preference.setEnabled( enabled );
    }
    
    @SuppressWarnings( "deprecation" )
    public static void removePreference( PreferenceActivity activity, String keyParent,
            String keyChild )
    {
        Preference parent = activity.findPreference( keyParent );
        Preference child = activity.findPreference( keyChild );
        if( parent instanceof PreferenceGroup && child != null )
            ( (PreferenceGroup) parent ).removePreference( child );
    }
    
    @SuppressWarnings( "deprecation" )
    public static void refreshSummary( PreferenceActivity activity, String key )
    {
        Preference preference = activity.findPreference( key );
        if( preference instanceof ListPreference )
            preference.setSummary( ( (ListPreference) preference ).getEntry() );
    }
}
