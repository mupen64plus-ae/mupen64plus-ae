/*
 * Code taken from http://stackoverflow.com/questions/40221711/android-context-getresources-updateconfiguration-deprecated/40704077#40704077
 */

package paulscode.android.mupen64plusae.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class LocaleContextWrapper extends ContextWrapper {

    private static String mLocaleCode = null;

    public LocaleContextWrapper(Context base) {
        super(base);
    }

    public static ContextWrapper wrap(Context context, String language) {
        Configuration config = context.getResources().getConfiguration();

        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setSystemLocale(config, locale);
        } else {
            setSystemLocaleLegacy(config, locale);
        }

        context = context.createConfigurationContext(config);

        return new LocaleContextWrapper(context);
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public static Locale getSystemLocaleLegacy(Configuration config){
        return config.locale;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static Locale getSystemLocale(Configuration config){
        return config.getLocales().get(0);
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public static void setSystemLocaleLegacy(Configuration config, Locale locale){
        config.locale = locale;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static void setSystemLocale(Configuration config, Locale locale){
        config.setLocale(locale);
    }

    public static void setLocaleCode(String localeCode)
    {
        mLocaleCode = localeCode;
    }

    public static String getLocalCode()
    {
        return mLocaleCode;
    }
}