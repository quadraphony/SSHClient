package ssh2.matss.sshtunnel.utils;

/*
 * Copyright (c) 2012-2021 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */


import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

import ssh2.matss.library.AppConstants;


public class LocaleHelper {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ DEFAULT, PORTUGUES_BR, SPANISH })
    public @interface LocaleDef {
        String[] SUPPORTED_LOCALES = { DEFAULT, PORTUGUES_BR, SPANISH };
    }

    //static final String ENGLISH = "en";
    static final String DEFAULT = "default";
    static final String PORTUGUES_BR = "pt-BR";
    static final String SPANISH = "es";

    /**
     * set current pref locale
     */
    public static Context setLocale(Context mContext) {
        return updateResources(mContext, getLanguagePref(mContext));
    }

    /**
     * Set new Locale with context
     */
    public static Context setNewLocale(Context mContext, @LocaleDef String language) {
        setLanguagePref(mContext, language);
        return updateResources(mContext, language);
    }

    /**
     * Get saved Locale from SharedPreferences
     *
     * @param mContext current context
     * @return current locale key by default return english locale
     */
    public static String getLanguagePref(Context mContext) {
        return PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(AppConstants.APP_LANG, "");
    }

    /**
     * set pref key
     */
    private static void setLanguagePref(Context mContext, String localeKey) {
        PreferenceManager.getDefaultSharedPreferences(mContext).
                edit().putString(AppConstants.APP_LANG, localeKey).apply();
    }

    /**
     * update resource
     */
    private static Context updateResources(Context context, String language) {
        Locale locale = Locale.getDefault();

        if (!language.equals(DEFAULT)) {
            /* handle locales with the country in it, i.e. zh_CN, zh_TW, etc */
            String localeSplit[] = language.split("_");
            if (localeSplit.length > 1) {
                locale = new Locale(localeSplit[0], localeSplit[1]);
            } else {
                locale = new Locale(language);
            }
        }

        Locale.setDefault(locale);
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        if (Build.VERSION.SDK_INT >= 17) {
            config.setLocale(locale);
            context = context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
        return context;
    }

    /**
     * get current locale
     */
    public static Locale getLocale(Resources res) {
        Configuration config = res.getConfiguration();
        return Build.VERSION.SDK_INT >= 24 ? config.getLocales().get(0) : config.locale;
    }
}