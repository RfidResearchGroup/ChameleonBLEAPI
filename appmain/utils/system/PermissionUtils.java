package com.proxgrind.chameleon.utils.system;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.proxgrind.chameleon.utils.tools.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 权限操作工具!
 *
 * @author DXL
 */
public class PermissionUtils {
    private static final String LOG_TAG = PermissionUtils.class.getSimpleName();
    //需要检查的权限!
    private String[] permissions;
    //丢失的权限!
    private String[] permissionLose;

    //上下文!
    private Context context;
    //回调!
    private Callback callback;
    // 是否自动请求!
    private boolean isAutoRequest = false;

    //权限请求的时候的返回值!
    private int requestCode = 0x665;
    // 使用队列进行顺序请求!
    private Queue<String> perQueue = new LinkedList<>();

    public PermissionUtils(Context context) {
        this.context = context;
    }

    /**
     * 检查权限，进行判断!
     */
    public void checks() {
        //在开始检查权限之前的操作!
        if (callback != null)
            callback.onStartChecks(this);
        //在检查的时候的回调!
        boolean isAllVaild = true;
        if (permissions == null) {
            Log.d(LOG_TAG, "传入的初始权限为空!");
            if (callback != null)
                callback.onEndChecks();
            return;
        }
        for (String per : permissions) {
            //迭代检查权限!
            if (!check(per)) {
                isAllVaild = false;
            }
        }
        ArrayList<String> list = new ArrayList<>();
        //所有的权限都正常时的回调!!
        if (isAllVaild) {
            if (callback != null)
                callback.onPermissionNormal(this);
        } else {
            //先迭代进行权限丢失的处理!
            for (String per : permissions) {
                //迭代请求权限!
                if (!check(per)) {
                    if (isAutoRequest) {
                        request(per);
                    } else {
                        //如果检查到的权限无法通过处理，则进行其他操作!
                        if (callback != null)
                            callback.whatPermissionLose(per, this);
                        list.add(per);
                    }
                }
            }
            //缓存丢失的权限!
            permissionLose = ArrayUtils.list2Arr(list);
            //如果检查到的权限无法通过处理，则进行其他操作!
            if (callback != null)
                callback.onPermissionLose(this);
        }
        //在检查完毕之后的回调!
        if (callback != null)
            callback.onEndChecks();
    }

    public boolean check(String per) {
        boolean ret;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ret = context.checkSelfPermission(per) == PackageManager.PERMISSION_GRANTED;
        } else {
            ret = context.checkCallingOrSelfPermission(per) == PackageManager.PERMISSION_GRANTED;
        }
        updateQueue();
        return ret;
    }

    public void request(String per) {
        // 尝试使用开发者实现的申请实现!
        if (callback.onRequest(per)) return;
        if (callback != null) {
            if (context instanceof Activity) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ((Activity) context).requestPermissions(new String[]{per}, requestCode);
                }
            }
        }
    }

    private void updateQueue() {
        // 添加到队列中！
        perQueue.clear();
        if (permissionLose != null)
            perQueue.addAll(Arrays.asList(permissionLose));
    }

    public void request() {
        final boolean isAutoCheckTmp = isAutoRequest;
        setAutoRequest(false);
        checks();
        updateQueue();
        String per = perQueue.poll();
        if (per != null && !
                check(per)) {
            // LogUtils.d("申请了权限: " + per);
            request(per);
        }
        setAutoRequest(isAutoCheckTmp);
    }

    public void requests() {
        final boolean isAutoCheckTmp = isAutoRequest;
        setAutoRequest(true);
        checks();
        setAutoRequest(isAutoCheckTmp);
    }

    public boolean isAutoRequest() {
        return isAutoRequest;
    }

    public void setAutoRequest(boolean autoRequest) {
        isAutoRequest = autoRequest;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public void setPermissions(String[] permissions) {
        this.permissions = permissions;
    }

    public void removePermissions(String[] permissions) {
        if (permissions == null || permissions.length == 0) return;
        String[] pers = getPermissions();
        ArrayList<String> newPersList = new ArrayList<>();
        for (String per : pers) {
            for (String per1 : permissions) {
                if (!per1.equalsIgnoreCase(per)) {
                    newPersList.add(per);
                }
            }
        }
        setPermissions(newPersList.toArray(new String[0]));
    }

    public void removePermission(String permission) {
        if (permission == null) return;
        removePermissions(new String[]{permission});
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public String[] getPermissionLose() {
        return permissionLose;
    }

    public interface Callback {
        /**
         * 在权限开始检查的时候的的回调!
         *
         * @param util 工具类对象!
         */
        void onStartChecks(PermissionUtils util);

        /**
         * 在权限丢失时的回调!
         *
         * @param util 工具类对象!
         */
        void onPermissionLose(PermissionUtils util);

        /**
         * 在权限正常的时候的回调!
         *
         * @param util 工具类对象!
         */
        void onPermissionNormal(PermissionUtils util);

        /**
         * 在权限丢失时的请求回调!
         *
         * @param per  丢失的权限!
         * @param util 工具类对象!
         */
        void whatPermissionLose(String per, PermissionUtils util);

        /**
         * 在权限开始检查的时候的的回调!
         */
        void onEndChecks();

        /**
         * 在需要自定义申请过程的时候的的回调!
         */
        boolean onRequest(String per);
    }
}
