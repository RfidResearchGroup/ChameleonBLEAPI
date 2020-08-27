package com.proxgrind.chameleon.utils.device.ble;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ClsUtils {
    /**
     * 与设备配对 参考源码：platform/packages/apps/Settings.git
     * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
     */
    @SuppressLint("ObsoleteSdkInt")
    static public boolean createBond(Class btClass, BluetoothDevice btDevice) {
        if (btDevice.getBondState() == BluetoothDevice.BOND_BONDED) return false;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                Method createBondMethod = btClass.getMethod("createBond");
                Object obj = createBondMethod.invoke(btDevice);
                return obj instanceof Boolean ? (Boolean) obj : false;
            } else {
                return btDevice.createBond();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 与设备解除配对 参考源码：platform/packages/apps/Settings.git
     * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
     */
    static public boolean removeBond(Class<?> btClass, BluetoothDevice btDevice) {
        try {
            Method removeBondMethod = btClass.getMethod("removeBond");
            Object obj = removeBondMethod.invoke(btDevice);
            return obj instanceof Boolean ? (Boolean) obj : false;
        } catch (Exception e) {
            return false;
        }
    }

    static public boolean setPin(Class<? extends BluetoothDevice> btClass, BluetoothDevice btDevice, String str) throws Exception {
        byte[] key = str != null ? str.getBytes() : null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            try {
                Method removeBondMethod = btClass.getDeclaredMethod("setPin", byte[].class);
                Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice, new Object[]{key});
                Log.d("returnValue", "" + returnValue);
            } catch (SecurityException e) {
                // throw new RuntimeException(e.getMessage());
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // throw new RuntimeException(e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return true;
        } else {
            return btDevice.setPin(key);
        }
    }

    // 取消用户输入
    static public boolean cancelPairingUserInput(Class<?> btClass, BluetoothDevice device) throws Exception {
        Method createBondMethod = btClass.getMethod("cancelPairingUserInput");
//        cancelBondProcess(btClass, device);
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
    }

    // 取消配对
    static public boolean cancelBondProcess(Class<?> btClass, BluetoothDevice device) throws Exception {
        Method createBondMethod = btClass.getMethod("cancelBondProcess");
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
    }

    //确认配对
    static public void setPairingConfirmation(Class<?> btClass, BluetoothDevice device, boolean isConfirm) throws Exception {
        Method setPairingConfirmation = btClass.getDeclaredMethod("setPairingConfirmation", boolean.class);
        setPairingConfirmation.invoke(device, isConfirm);
    }

    /**
     * @param clsShow
     */
    static public void printAllInform(Class clsShow) {
        try {
            // 取得所有方法
            Method[] hideMethod = clsShow.getMethods();
            int i = 0;
            for (; i < hideMethod.length; i++) {
                Log.e("method name", hideMethod[i].getName() + ";and the i is:"
                        + i);
            }
            // 取得所有常量
            Field[] allFields = clsShow.getFields();
            for (i = 0; i < allFields.length; i++) {
                Log.e("Field name", allFields[i].getName());
            }
        } catch (SecurityException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 刷新设备的缓存!
     */
    static public void refreshDeviceCache(BluetoothGatt gatt) {
        if (gatt == null) return;
        /*
         * If the device is bonded this is up to the Service Changed characteristic to notify Android that the services has changed.
         * There is no need for this trick in that case.
         * If not bonded, the Android should not keep the services cached when the Service Changed characteristic is present in the target device database.
         * However, due to the Android bug (still exists in Android 5.0.1), it is keeping them anyway and the only way to clear services is by using this hidden refresh method.
         */
        if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
            /*
             * There is a refresh() method in BluetoothGatt class but for now it's hidden. We will call it using reflections.
             */
            try {
                //noinspection JavaReflectionMemberAccess
                final Method refresh = gatt.getClass().getMethod("refresh");
                refresh.invoke(gatt);
            } catch (Exception ignored) {
            }
        }
    }
}