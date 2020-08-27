package com.proxgrind.chameleon.utils.device.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.proxgrind.devices.BleSerialControl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;

/**
 * 蓝牙状态监听!
 */
public class BluetoothUtils {
    public static BroadcastReceiver startStatusReceicer(Context context, OnStatusChange callback) {
        if (callback != null) {
            if (!check(context, callback)) return null;
            // 创建广播!
            BroadcastReceiver blueToothValueReceiver = new BroadcastReceiver() {
                public int DEFAULT_VALUE_BULUETOOTH = 1000;

                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, DEFAULT_VALUE_BULUETOOTH);
                        switch (state) {
                            case BluetoothAdapter.STATE_OFF:
                                callback.onStateOff(context);
                                break;
                            case BluetoothAdapter.STATE_ON:
                                callback.onStateOn(context);
                                break;
                            case BluetoothAdapter.STATE_TURNING_ON:
                                callback.onStateTurningOn(context);
                                break;
                            case BluetoothAdapter.STATE_TURNING_OFF:
                                callback.onStateTurningOff(context);
                                break;
                            case BluetoothAdapter.STATE_CONNECTING:
                                callback.onConnecting(context);
                                break;
                            case BluetoothAdapter.STATE_CONNECTED:
                                callback.onConnected(context);
                                break;
                            case BluetoothAdapter.STATE_DISCONNECTING:
                                callback.onDisconnecting(context);
                                break;
                            case BluetoothAdapter.STATE_DISCONNECTED:
                                callback.onDisconnected(context);
                                break;
                            default:
                                break;
                        }
                    }
                }
            };
            //注册广播，蓝牙状态监听
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            context.registerReceiver(blueToothValueReceiver, filter);
            return blueToothValueReceiver;
        }
        return null;
    }

    public static BroadcastReceiver startActionReceiver(Context context, OnActionChange callback) {
        if (callback != null) {
            if (!check(context, callback)) return null;
            // 创建广播!
            BroadcastReceiver blueToothValueReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                        callback.onScanStarted(context);
                    }
                    if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                        callback.onScanFinished(context);
                    }
                }
            };
            //注册广播，蓝牙状态监听
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            context.registerReceiver(blueToothValueReceiver, filter);
            return blueToothValueReceiver;
        }
        return null;
    }

    private static boolean check(Context context, OnBlueNoSupport callback) {
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null || manager.getAdapter() == null) {
            callback.onBlueNoSupported(context);
            return false;
        }
        return true;
    }

    public static void stop(BroadcastReceiver receiver, Context context) {
        try {
            context.unregisterReceiver(receiver);
        } catch (Exception ignored) {
        }
    }

    private static class OnBlueNoSupport {
        public void onBlueNoSupported(Context context) {
        }
    }

    public static class OnStatusChange extends OnBlueNoSupport {
        public void onStateOff(Context context) {
        }

        public void onStateOn(Context context) {
        }

        public void onStateTurningOn(Context context) {
        }

        public void onStateTurningOff(Context context) {
        }

        public void onConnecting(Context context) {
        }

        public void onConnected(Context context) {
        }

        public void onDisconnecting(Context context) {
        }

        public void onDisconnected(Context context) {
        }
    }

    public static class OnActionChange extends OnBlueNoSupport {
        public void onScanStarted(Context context) {
        }

        public void onScanFinished(Context context) {
        }
    }

    public static boolean isBlueOpen(Context context) {
        return BleSerialControl.get().getAdapter().isEnabled();
    }
}
