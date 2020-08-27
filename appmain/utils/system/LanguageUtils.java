package com.proxgrind.chameleon.utils.system;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public class LanguageUtils {

    /*
     * 获得语言列表!
     * */
    public static LocaleList getLocaleList(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales();
        } else {
            return new LocaleList(context.getResources().getConfiguration().locale);
        }
    }

    /*
     * 获得当前系统默认的语言!
     * */
    public static Locale getDefaultLocale(Context context) {
        return Locale.getDefault();
    }

    /*
     * 设置当前的语言!
     * */
    public static Context setAppLanguage(Context context, String language) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(new Locale(language));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            return context.createConfigurationContext(configuration);
        } else {
            context.getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());
            return context;
        }
    }

}
