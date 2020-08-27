package com.proxgrind.chameleon.utils.tools;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.proxgrind.chameleon.R;
import com.proxgrind.chameleon.activities.DevicesFastActivity;
import com.proxgrind.chameleon.executor.ChameleonExecutorProxy;
import com.proxgrind.chameleon.javabean.DevBean;
import com.proxgrind.chameleon.javabean.DumpBean;
import com.proxgrind.chameleon.utils.mifare.DumpUtils;
import com.proxgrind.chameleon.utils.stream.FileUtils;
import com.proxgrind.chameleon.utils.system.AppContextUtils;
import com.proxgrind.chameleon.utils.system.AppListUtils;
import com.proxgrind.chameleon.utils.system.AppResourceUtils;
import com.proxgrind.chameleon.utils.system.LogUtils;
import com.proxgrind.chameleon.widget.MaterialAlertDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by DXL on 2017/10/27.
 */
public class Commons {

    //短称路径
    public static final String LOG_TAG = Commons.class.getSimpleName();
    // 子模块包名!
    private static final String SUB_DEBUG_PACK_NAME = "com.proxgrind.debug";
    // Dump文件固定的时间格式!
    public static final String DEFAULT_TIME_DECRATE = "yyyy-MM-dd_HH:mm:ss";
    public static SharedPreferences sp = AppContextUtils.app.getSharedPreferences("SaveMap", Context.MODE_PRIVATE);
    public static SharedPreferences.Editor editor = sp.edit();

    private Commons() {
    }

    public static String[] getPermissionsOfAppRequired() {
        ArrayList<String> perList = new ArrayList<>();
        perList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        perList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        // perList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        perList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        return perList.toArray(new String[0]);
    }

    //呼叫QQ
    public static void callQQ(Context context, String qq, Runnable onFaild) {
        //这里的228451878是自己指定的QQ号码，可以自己更换
        String url = "mqqwpa://im/chat?chat_type=wpa&uin=" + qq;
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            onFaild.run();
        }
    }

    //打开浏览器，链接到指定的链接
    public static void openUrl(Context context, String url) {
        try {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(uri);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //获取包中的文件的URI
    public static Uri getUriFromResource(String packagePath, String filePath) {
        return Uri.parse("android.resource://" + packagePath + "/" + filePath);
    }

    //移除设备对象从集合中!
    public static boolean removeDevByList(DevBean devBean, List<DevBean> list) {
        if (devBean != null) {
            String name = devBean.getDevName();
            String addr = devBean.getMacAddress();
            if (name == null) return false;
            for (int i = 0; i < list.size(); i++) {
                DevBean tmpBean = list.get(i);
                if (tmpBean == null) return false;
                String n = tmpBean.getDevName();
                String a = tmpBean.getMacAddress();
                if (n == null) return false;
                if (n.equals(name) && a.equals(addr)) {
                    list.remove(tmpBean);
                    return true;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    //该设备实体之中的字段是否是空的!
    public static boolean isDevBeanDataNotNull(DevBean devBean) {
        if (devBean == null) return false;
        return devBean.getMacAddress() != null;
    }

    //判断两个设备是否是一致的
    public static boolean equalDebBean(DevBean a, DevBean b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        if (isDevBeanDataNotNull(a) && isDevBeanDataNotNull(b)) {
            return a.getMacAddress().equals(b.getMacAddress());
        }
        return false;
    }

    //从蓝牙适配器中取出历史连接的设备列表!
    public static DevBean[] getDevsFromBTAdapter(BluetoothAdapter btAdapter) {
        ArrayList<DevBean> devList = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices == null) return null;
        if (pairedDevices.size() > 0) {
            ArrayList<BluetoothDevice> tmpList = new ArrayList<>(pairedDevices);
            for (int i = 0; i < tmpList.size(); ++i) {
                devList.add(new DevBean(tmpList.get(i).getName(),
                        tmpList.get(i).getAddress()));
            }
        } else {
            return null;
        }
        return ArrayUtils.list2Arr(devList);
    }

    //设备是否是USB设备!
    public static boolean isUsbDevice(String address) {
        if (address == null) return false;
        //这三种mac是开发者定义的用于区分USB设备和蓝牙设备的特征符！
        switch (address) {
            case "00:00:00:00:00:00":
            case "00:00:00:00:00:01":
            case "00:00:00:00:00:02":
                return true;
        }
        return false;
    }

    // 获得相应的从值中!
    public static int getPositionFromValue(String str, List<String> list) {
        return list.indexOf(str);
    }

    // 保存Dump到目录!
    public static boolean saveDump2Local(byte[] dump, String name) {
        File dumpDir = FileUtils.getAppFilesDir("dump");
        try {
            FileUtils.writeBytes(dump, FileUtils.newFile(dumpDir, name), false);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static File[] listInternalDump() {
        File dumpDir = FileUtils.getAppFilesDir("dump");
        return dumpDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".dump");
            }
        });
    }

    public static String getTimeDecorate() {
        Date date = new Date(); //获取当前的系统时间。
        SimpleDateFormat dateFormat = new SimpleDateFormat(DEFAULT_TIME_DECRATE, Locale.getDefault()); //使用了默认的格式创建了一个日期格式化对象。
        return dateFormat.format(date);
    }

    public static String getTimeDecorate(String fileName) {
        String time = RegexGroupUtils.matcherGroup(
                fileName,
                ".*\\|(.*)\\..*",
                1,
                0);
        if (time == null) return null;
        try {
            return new SimpleDateFormat(DEFAULT_TIME_DECRATE, Locale.CHINESE).format(time);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getInternalDumpDir() {
        return FileUtils.getAppFilesDir("dump").getAbsolutePath();
    }

    public static String getSdcardDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getSdcardDownloadDir() {
        return getSdcardDir() + File.separator + "Download";
    }

    public static String getSdcardAppDownloadDir() {
        String filePath = getSdcardDownloadDir() + File.separator + AppContextUtils.app.getPackageName();
        FileUtils.createPaths(new File(filePath));
        return filePath;
    }

    public static String getDumpName(String prefix) {
        return getDumpName(prefix, getTimeDecorate());
    }

    public static String getDumpRawName(String name) {
        // 1A0B42C8(data.dump)|2020-02-20_15:49:06.dump
        return RegexGroupUtils.matcherGroup(name, ".*\\((.*)\\)\\|.*", 1, 0);
    }

    public static String getDumpName(String prefix, String centerfix) {
        return prefix + "|" + centerfix + ".dump";
    }

    public static String getDumpFile(String name) {
        return FileUtils.getAppFilesDir("dump") + File.separator + name;
    }

    public static DumpBean[] files2DumpBeans(File[] files, boolean isDescendingOrder, boolean isNeedNameSort) {
        ArrayList<DumpBean> ret = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                DumpBean info = new DumpBean(name);
                // 裁取UID!
                String uid = RegexGroupUtils.matcherGroup(
                        name,
                        "(.*)\\|.*",
                        1,
                        0);
                String time = RegexGroupUtils.matcherGroup(
                        name,
                        ".*\\|(.*)\\..*",
                        1,
                        0);
                if (uid != null && time != null) {
                    info.setUid(uid);
                    info.setTime(time);
                } else {
                    info.setUid(f.getName());
                    if (time != null) {
                        info.setTime(time);
                    } else {
                        info.setTime("0000-00-00_00:00:00");
                    }
                }
                info.setName(f.getName());
                ret.add(info);
            }
        }
        Comparator<DumpBean> nameComparator = new Comparator<DumpBean>() {
            @Override
            public int compare(DumpBean o1, DumpBean o2) {
                if (isNeedNameSort) {
                    String uid1 = o1.getUid();
                    String uid2 = o2.getUid();
                    if (uid1 != null && uid2 != null) {
                        if (isDescendingOrder) {
                            return uid2.compareTo(uid1);
                        } else {
                            // 升序
                            return uid1.compareTo(uid2);
                        }
                    }
                    return 0;
                }
                // 不需要使用名称来排序则直接进行无操作状态返回!
                return 0;
            }
        };
        Comparator<DumpBean> dateComparator = new Comparator<DumpBean>() {
            @Override
            public int compare(DumpBean o1, DumpBean o2) {
                SimpleDateFormat format = new SimpleDateFormat(DEFAULT_TIME_DECRATE, Locale.getDefault());
                try {
                    Date dt1 = format.parse(o1.getTime());
                    Date dt2 = format.parse(o2.getTime());
                    if (dt1 != null && dt2 != null) {
                        if (isDescendingOrder)
                            return Long.compare(dt2.getTime(), dt1.getTime());
                        else
                            return Long.compare(dt1.getTime(), dt2.getTime());
                    } else {
                        return 0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 0;
            }
        };
        List<Comparator<DumpBean>> comparatorList = new ArrayList<>(Arrays.asList(nameComparator, dateComparator));
        Collections.sort(ret, new Comparator<DumpBean>() {
            @Override
            public int compare(DumpBean o1, DumpBean o2) {
                // 用实际的比较器比较!
                for (Comparator<DumpBean> tmpComparator : comparatorList) {
                    if (tmpComparator.compare(o1, o2) > 0) {
                        return 1;
                    } else if (tmpComparator.compare(o1, o2) < 0) {
                        return -1;
                    }
                }
                return 0;
            }
        });
        return ret.toArray(new DumpBean[0]);
    }

    public static boolean addDumpFileToInternal(String name, byte[] content) {
        // 取出内部的文件!
        DumpBean[] dumpBeans = files2DumpBeans(listInternalDump(), false, false);
        boolean canAdd = true;
        for (DumpBean bean : dumpBeans) {
            // 如果已经存在相同的名字的文件的话，则进行MD5检测!
            String rawName = getDumpRawName(bean.getName());
            LogUtils.d("******************************");
            LogUtils.d("名称(bean): " + rawName);
            LogUtils.d("名称(file): " + name);
            LogUtils.d("******************************");
            if (name.equals(rawName)) {
                File dumpFile = new File(getDumpFile(bean.getName()));
                LogUtils.d("已经存在该文件名，将会尝试进行对比：" + dumpFile.getName());
                try {
                    byte[] dumpDigest = FileUtils.readBytes(dumpFile);
                    String digestStr = MD5Utils.digest(dumpDigest);
                    // 如果检测到MD5相同，则跳过添加，否则添加!
                    if (MD5Utils.verify(digestStr, content)) {
                        canAdd = false;
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        if (canAdd) {
            String perviousUID = "No UID.";
            // 截取UID!
            int type = DumpUtils.getType(content);
            if (type == DumpUtils.TYPE_TXT) {
                String[] datas = DumpUtils.getTxt(content);
                if (datas != null) {
                    perviousUID = datas[0].substring(0, 8);
                }
            } else if (type == DumpUtils.TYPE_BIN) {
                perviousUID = HexUtil.toHexString(content, 0, 4);
            } else {
                return false;
            }
            // 截取完整的源文件名字!
            // 获得一个时间修饰!
            String time = Commons.getTimeDecorate();
            // 拼接将要保存到内部的文件名!
            File target = new File(Commons.getDumpFile(
                    perviousUID
                            + "("
                            + name
                            + ")|"
                            + time
                            + ".dump")
            );
            // 直接将已经读取出来的数据字节组写入到内部文件!
            return FileUtils.copy(content, target);
        }
        return false;
    }

    public static int getMaxWidthOnChildren(AbsListView absListView) {
        if (absListView == null || absListView.getAdapter() == null) {
            // pre-condition
            return -1;
        }
        //int totalHeight = 0;
        int maxWidth = 0;
        for (int i = 0; i < absListView.getAdapter().getCount(); i++) {
            View listItem = absListView.getAdapter().getView(i, null, absListView);
            listItem.measure(0, 0);
            //totalHeight += listItem.getMeasuredHeight();
            int width = listItem.getMeasuredWidth();
            if (width > maxWidth) maxWidth = width;
            LogUtils.d("tmp: " + width);
        }
        return maxWidth;
    }

    // 是否快速在30秒内反复重启!
    public static boolean isAppFastRepeatedRestart() {
        File appDir = FileUtils.getAppFilesDir("config");
        if (appDir != null) {
            int maxCount = 5;
            String key = "FastRestart";
            File timeFile = new File(appDir.getAbsolutePath() + File.separator + key + ".config");
            if (!timeFile.exists()) FileUtils.createFile(timeFile);
            // 判断反复重启的思路就是，存入5个时间段的时间戳，如果这五个时间段的总共距离不超过30s，则判定为快速反复重启!
            try {
                String[] values = new String[0];
                if (DiskKVUtil.isKVExists(key, timeFile)
                        && (values = DiskKVUtil.queryKVLine(key, timeFile)).length == maxCount) {
                    // 有五次记录，我们需要看他的时间!
                    long[] times = new long[values.length];
                    // 转换为时间戳!
                    for (int i = 0; i < times.length; i++) {
                        String tmpTimeStr0 = values[i];
                        times[i] = Long.valueOf(tmpTimeStr0);
                    }
                    // 进行排序!
                    Arrays.sort(times);
                    LogUtils.d(Arrays.toString(times));
                    // 获取最小值与当前时间对比!
                    long timeCount = 0;
                    for (int i = times.length - 1; i > 0; ) {
                        LogUtils.d("双方时间戳: " + times[i] + "," + times[i - 1]);
                        timeCount += times[i] - times[--i];
                        LogUtils.d("差值timeCount: " + timeCount);
                    }
                    System.arraycopy(times, 1, times, 0, times.length - 1);
                    //实现左移，然后最后一个位置更新距离开机的时间，如果最后一个时间和最开始时间小于DURATION，即连续5次启动
                    times[times.length - 1] = System.currentTimeMillis();
                    // 转换为时间戳重新写入!
                    for (int i = 0; i < values.length; i++) {
                        values[i] = String.valueOf(times[i]);
                    }
                    DiskKVUtil.update2Disk(key, values, timeFile);
                    boolean isValueFarAway = (System.currentTimeMillis() - times[0]) > ((1000 * 30));
                    if (timeCount < 1000 * 30 && !isValueFarAway) {
                        // 三十秒内超五次
                        return true;
                    }
                } else {
                    if (values.length < maxCount) {
                        LogUtils.d("键值对(操作次数)不到五个，将会自动插入: " + values.length);
                        DiskKVUtil.insertKV(key, String.valueOf(System.currentTimeMillis()), timeFile);
                    } else {
                        DiskKVUtil.deleteKV(key, timeFile);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            LogUtils.d("配置目录不存在！");
        }
        return false;
    }

    // 是否拥有DEBUG的权限!
    public static boolean isCanAccessRoot() {
        // 进行信息检测，查找索引!
        List<ResolveInfo> appList = AppListUtils.getInstalledApplication(AppContextUtils.app, false);
        for (ResolveInfo resolveInfo : appList) {
            if (SUB_DEBUG_PACK_NAME.equalsIgnoreCase(resolveInfo.activityInfo.packageName)) {
                // 有SUB APP，可以通过放行！
                appList.clear();
                return true;
            }
        }
        appList.clear();
        return false;
    }

    public static void showDialogOnOfflineMode(Context context) {
        if (!ChameleonExecutorProxy.getInstance().isConnected()) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.tips)
                    .setMessage(R.string.tips_offline_mode)
                    .setPositiveButton(R.string.go2, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            context.startActivity(new Intent(context, DevicesFastActivity.class));
                        }
                    })
                    .setCancelable(false)
                    .setNegativeButton(R.string.cancel, null).show();
        }
    }

    // 当前的操作是否是在主线程中执行的!
    public static boolean isRunOnMainThread() {
        return Thread.currentThread().getId()
                == Looper.getMainLooper().getThread().getId();
    }

    /**
     * @return 获取本地包
     */
    public static long getVerCode() {
        long verCode = -1;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                verCode = AppContextUtils.app.getPackageManager().getPackageInfo(
                        AppContextUtils.app.getPackageName(), 0).getLongVersionCode();
            } else {
                verCode = AppContextUtils.app.getPackageManager().getPackageInfo(
                        AppContextUtils.app.getPackageName(), 0).versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verCode;
    }

    /**
     * @return 获取本地包
     */
    public static String getVerName() {
        String verCode = "NoName";
        try {
            verCode = AppContextUtils.app.getPackageManager().getPackageInfo(
                    AppContextUtils.app.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verCode;
    }

    public static void logStackTrace() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement stackTraceElement : stack) {
            Log.d("logStackTrace", stackTraceElement.getClassName() + " }:{ " + stackTraceElement.getMethodName());
        }
    }

    public static void showExportSuccessDialog(@Nullable Activity activity, Uri path) {
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 当前的目标使用的是外部路径!
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.tips)
                        .setCancelable(false)
                        .setMessage(activity.getString(R.string.tips_dump_export_success) + ": " + FileUtils.getFilePathByUri(path))
                        .setPositiveButton(R.string.ok, null)
                        .setNegativeButton(R.string.share, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FileUtils.shareFile(activity, path);
                            }
                        })
                        .show();
            }
        });
    }

    // 保存文件到临时目录!
    public static String saveFileTemp(Context context, Uri uri) {
        String internalPath = FileUtils.getAppFilesDir("temp").getPath();
        // 截取完整的源文件名字!
        String sourceName = FileUtils.getFileNameByUri(context, uri, false);
        if (sourceName == null) {
            String tempPath = FileUtils.getFilePathByUri(uri);
            if (tempPath != null) {
                sourceName = new File(tempPath).getName();
            }
        }
        String targetPath = internalPath + File.separator +
                // 尝试拼接源文件名
                (sourceName == null ? UUID.randomUUID() : sourceName);
        try {
            byte[] data = FileUtils.readBytes(uri);
            File targetFile = new File(targetPath);
            FileUtils.createFile(targetFile);
            FileUtils.writeBytes(data, Uri.fromFile(targetFile));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return targetPath;
    }

    public static void showLowBatteryDialog(Context context, int percent) {
        if (percent > 0 && percent < 10) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    new MaterialAlertDialog.Builder(context)
                            .setTitle(R.string.warning)
                            .setMessage(context.getString(R.string.tips_battery_low) + " : " + percent + "%")
                            .setStyle(new MaterialAlertDialog.OnWidgetStyle() {
                                @Override
                                public void onStyle(TextView title, TextView msg, Button btn1, Button btn2) {
                                    title.setTextColor(AppResourceUtils.getColor(R.color.colorTextError));
                                }
                            }).show();
                }
            };
            if (isRunOnMainThread()) {
                runnable.run();
            } else {
                new Handler(Looper.getMainLooper()).post(runnable);
            }
        }
    }

    // 存放MAP
    public static void setMap(String key, LinkedHashMap<String, String> datas) {
        JSONArray mJsonArray = new JSONArray();
        Iterator<Map.Entry<String, String>> iterator = datas.entrySet().iterator();
        JSONObject object = new JSONObject();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            try {
                object.put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
            }
        }
        mJsonArray.put(object);
        editor.putString(key, mJsonArray.toString());
        editor.commit();
    }

    // 取出MAP
    public static LinkedHashMap<String, String> getMap(String key) {
        LinkedHashMap<String, String> datas = new LinkedHashMap<>();
        String result = sp.getString(key, "");
        try {
            JSONArray array = new JSONArray(result);
            for (int i = 0; i < array.length(); i++) {
                JSONObject itemObject = array.getJSONObject(i);
                JSONArray names = itemObject.names();
                if (names != null) {
                    for (int j = 0; j < names.length(); j++) {
                        String name = names.getString(j);
                        String value = itemObject.getString(name);
                        datas.put(name, value);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return datas;
    }

    public static boolean isSelfBackground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses;
        if (activityManager != null) {
            appProcesses = activityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.processName.equals(context.getPackageName())) {
                    if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                        Log.i("后台", appProcess.processName);
                        return true;
                    } else {
                        Log.i("前台", appProcess.processName);
                        return false;
                    }
                }
            }
        }
        return false;
    }

    public static int getAutoDisconnectTime() {
        return sp.getInt(Properties.APP_AUTO_CLOSE_TIME_KEY, 300);
    }

    public static void setAutoDisconnectTime(int time) {
        editor.putInt(Properties.APP_AUTO_CLOSE_TIME_KEY, time).apply();
    }

    public static boolean isAutoDisconnect() {
        return sp.getBoolean(Properties.APP_AUTO_CLOSE_KEY, true);
    }

    public static void setAutoDisconnect(boolean enable) {
        editor.putBoolean(Properties.APP_AUTO_CLOSE_KEY, enable).apply();
    }

    /**
     * 获取当前的UI模式，
     * 0 为跟随系统
     * 1 为暗黑模式
     * 2 为明亮模式
     */
    public static int getUIMode() {
        return sp.getInt(Properties.APP_UI_MODE, 0);
    }

    /**
     * 设置当前的UI模式，
     * 0 为跟随系统
     * 1 为暗黑模式
     * 2 为明亮模式
     *
     * @param mode 新的模式
     */
    public static void setUIMode(int mode) {
        editor.putInt(Properties.APP_UI_MODE, mode).apply();
    }

    /**
     * 获取当前的Dump模式
     * 0 bin模式
     * 1 hex模式
     */
    public static int getDumpMode() {
        return sp.getInt(Properties.APP_DUMP_MODE, 0);
    }

    /**
     * 设置当前的Dump模式
     * 0 bin模式
     * 1 hex模式
     */
    public static void setDumpMode(int mode) {
        editor.putInt(Properties.APP_DUMP_MODE, mode).apply();
    }

}
