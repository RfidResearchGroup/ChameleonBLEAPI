package com.proxgrind.chameleon.utils.system;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

public class AppListUtils {

    //获取用户安装的APP
    public static List<ResolveInfo> getInstalledApplication(Context context, boolean needSysAPP) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
        if (!needSysAPP) {
            List<ResolveInfo> resolveInfosWithoutSystem = new ArrayList<>();
            for (int i = 0; i < resolveInfos.size(); i++) {
                ResolveInfo resolveInfo = resolveInfos.get(i);
                try {
                    if (!isSysApp(context, resolveInfo.activityInfo.packageName)) {
                        resolveInfosWithoutSystem.add(resolveInfo);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return resolveInfosWithoutSystem;
        }
        return resolveInfos;
    }

    //判断是否系统应用
    public static boolean isSysApp(Context context, String packageName) throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        return (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1;
    }
}
