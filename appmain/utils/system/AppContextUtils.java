package com.proxgrind.chameleon.utils.system;

import android.app.Activity;
import android.app.Application;

public class AppContextUtils {
    private static AppContextUtils thiz = null;
    public static Application app;

    private AppContextUtils() {
    }

    public static AppContextUtils getInstance() {
        if (thiz == null) {
            thiz = new AppContextUtils();
        }
        return thiz;
    }

    public static void register(Application app) {
        AppContextUtils.app = app;
    }

    public void finishAll() {
        for (Activity activity : CrashUtils.activities) {
            activity.finish();
        }
    }
}
