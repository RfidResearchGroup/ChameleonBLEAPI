package com.proxgrind.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.proxgrind.chameleon.exceptions.DataInvalidException;
import com.proxgrind.chameleon.javabean.DevBean;
import com.proxgrind.chameleon.packets.DataPackets;
import com.proxgrind.chameleon.utils.device.ble.BLERawUtils;
import com.proxgrind.chameleon.utils.device.ble.ClsUtils;
import com.proxgrind.chameleon.utils.tools.HexUtil;
import com.proxgrind.chameleon.utils.system.LogUtils;
import com.proxgrind.chameleon.utils.system.SystemUtils;
import com.proxgrind.chameleon.callback.ConnectCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author DXL
 * @see com.proxgrind.devices.DriverInterface
 * 封装用于BLE通讯的实例!
 */
public class BleSerialControl extends BluetoothGattCallback
        implements DriverInterface<DevBean, BluetoothAdapter> {
    // 日志标签!
    public static final String TAG = "BleSerialControl";
    // MTU上限
    public static final int MTU = 244;
    // UART 服务!
    public static final UUID UART_SERVICE_UUID = UUID.fromString("51510001-7969-6473-6f40-6b6f6c6c6957");
    // UART 写特征
    public static final UUID SEND_CHARACT_UUID = UUID.fromString("51510002-7969-6473-6f40-6b6f6c6c6957");
    // UART 读特征
    public static final UUID RECV_CHARACT_UUID = UUID.fromString("51510003-7969-6473-6f40-6b6f6c6c6957");
    // 控制点!
    public static final UUID CTRL_CHARACT_UUID = UUID.fromString("51510004-7969-6473-6f40-6b6f6c6c6957");
    // UART读特征句柄!
    public static final UUID RECV_DESC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    // Chameleon MINI UUID特征码!
    public static byte[] UUID_RAW_MINI = HexUtil.hexStringToByteArray("57696C6C6F6B406F7364697901005151");
    // Chameleon MINI DFU UUID特征码!
    public static byte[] UUID_RAW_MINI_DFU = HexUtil.hexStringToByteArray("5252");
    // Chameleon Tiny 广播UUID
    public static UUID UUID_COMPLETE_TINY = UUID.fromString("51510004-7969-6473-6f40-6b6f6c6c6957");
    // Chameleon Tiny UUID特征码
    public static byte[] UUID_RAW_TINY = HexUtil.hexStringToByteArray("57696C6C6F6B406F7364697904005151");
    // 解包后保存的数据的队列，此缓冲区大多数是用于串口数据的!!
    private static final Queue<Byte> BUFFER_SERIAL = new ConcurrentLinkedQueue<>();

    // 蓝牙相关
    private DevCallback<BluetoothDevice> devCallback;
    private List<BluetoothGattCallback> gattCallbacks = new ArrayList<>();
    private BluetoothDevice device;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt gatt;
    private BroadcastReceiver bondReceiver;
    private DevBean devBean;
    // 当前设备是否连接
    private volatile boolean isConnected = false;
    // 是否允许数据同步到缓冲区
    private volatile boolean pushDataToBuffer = true;
    // 当前的BLE同步状态
    private volatile int asyncSyncStatus = 0;
    // 绑定广播同步状态
    private volatile int bondAsyncSyncStatus = 0;
    // 最后的一个应答帧
    private volatile byte[] dataFrame;
    // 帧锁
    private static final Object LOCK_DATA_FRAME = new Object();
    // 单例
    private static final BleSerialControl thiz = new BleSerialControl();
    // 数据回调
    private OnDataReceiveListener onDataReceiveListener;

    private BleSerialControl() {
        synchronized (BleSerialControl.class) {
            // 添加一个默认连接用的回调!
            gattCallbacks.add(new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (status == 19) { // 状态码19是绑定信息丢失引起的！
                        LogUtils.d("状态码发现配对异常，需要重新进行连接配对。");
                        asyncSyncStatus = -1;
                        closeNoException();
                        return;
                    }
                    if (newState == BluetoothProfile.STATE_CONNECTED) {//状态变为 已连接
                        LogUtils.d("已连接，开始发现服务!");
                        // 默认打开高性能传输
                        requestIntervalHigh();
                        // 尝试寻找服务!
                        if (!gatt.discoverServices()) {
                            asyncSyncStatus = -1;
                        }
                    }
                    if (newState == BluetoothGatt.STATE_DISCONNECTED) { //状态变为 未连接
                        isConnected = false;
                        closeNoException();
                        LogUtils.w("设备断开了链接!");
                        if (devCallback != null) {
                            // 通知一下设备断开!
                            devCallback.onDetach(gatt.getDevice());
                        }
                        asyncSyncStatus = -1;
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (asyncSyncStatus == -1) return;
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        LogUtils.d("已发现服务，开始对指定句柄开启写通知!");
                        enableNotifyOnUARTService(gatt, RECV_CHARACT_UUID);
                    } else {
                        asyncSyncStatus = -1;
                    }
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    if (asyncSyncStatus == -1) return;
                    // 如果是MINI，则不存在04的控制特征
                    UUID currentCharacteristicUUID = descriptor.getCharacteristic().getUuid();
                    if (isCtrlCharacteristicExists() && !currentCharacteristicUUID.equals(CTRL_CHARACT_UUID)) {
                        // 如果当前设备存在控制点，则判断当前是否需要启用控制点!
                        enableNotifyOnUARTService(gatt, CTRL_CHARACT_UUID);
                        return;
                    }
                    UUID finalResultUUid;
                    if (isCtrlCharacteristicExists()) {
                        finalResultUUid = CTRL_CHARACT_UUID;
                    } else {
                        finalResultUUid = RECV_CHARACT_UUID;
                    }
                    if (currentCharacteristicUUID.equals(finalResultUUid)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            LogUtils.d("写通知开启成功，BLE连接成功！");
                            asyncSyncStatus = 1;
                        } else {
                            asyncSyncStatus = -1;
                        }
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    byte[] finalDatas = characteristic.getValue();
                    if (finalDatas != null) {
                        try {
                            // 我们需要进行解包!
                            finalDatas = DataPackets.getData(finalDatas, false);
                        } catch (DataInvalidException die) {
                            // die.printStackTrace();
                        }
                        if (onDataReceiveListener != null) {
                            onDataReceiveListener.onReceive(finalDatas);
                        }
                        if (pushDataToBuffer) {
                            for (Byte b : finalDatas) {
                                if (!BUFFER_SERIAL.offer(b)) {
                                    // clear and retry add data to queue!
                                    BUFFER_SERIAL.clear();
                                    BUFFER_SERIAL.offer(b);
                                }
                            }
                        }
                        dataFrame = finalDatas;
                    }
                }
            });
            bondReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // LogUtils.d("在广播中进行绑定状态的处理!");
                    String action = intent.getAction(); //得到action
                    if (!BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) return;
                    BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (btDevice == null) return;
                    int status = btDevice.getBondState();
                    if (status == BluetoothDevice.BOND_BONDED) {
                        bondAsyncSyncStatus = 1;
                    }
                }
            };
        }
    }

    public static BleSerialControl get() {
        return thiz;
    }

    @Override
    public void register(DevCallback callback) {
        devCallback = callback;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) return;
        bluetoothAdapter = bluetoothManager.getAdapter();
        context.registerReceiver(bondReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
    }

    @Override
    public void connect(DevBean t, ConnectCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(t.getMacAddress());
                setDevice(device);
                devBean = t;
                if (connectNoCallback(t, device)) {
                    isConnected = true;
                    callback.onConnectSucces();
                } else {
                    isConnected = false;
                    callback.onConnectFail();
                }
            }
        }).start();
    }

    @Override
    public boolean isDeviceConnected() {
        return isConnected;
    }

    @Override
    public BluetoothAdapter getAdapter() {
        return bluetoothAdapter;
    }

    @Override
    public DevBean getDevice() {
        if (devBean != null) return devBean;
        return new DevBean(device.getName(), device.getAddress());
    }

    @Override
    public void disconnect() {
        try {
            closeNoException();
            isConnected = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getUniqueId() {
        return 0x06;
    }

    @Override
    public void unregister() {
        devCallback = null;
        try {
            context.unregisterReceiver(bondReceiver);
        } catch (Exception ignored) {
        }
    }

    @Override
    public int write(byte[] buffer, int offset, int length, int timeout) throws IOException {
        return write(buffer, offset, length, timeout, SEND_CHARACT_UUID);
    }

    public int write(byte[] buffer, int offset, int length, int timeout, UUID sendUUID) throws IOException {
        synchronized (LOCK_DATA_FRAME) {
            dataFrame = null;
        }
        if (length - offset > MTU) return -1;
        if (gatt == null) {
            LogUtils.e("Gatt对象异常!");
            disconnect();
            return -1;
        }
        BluetoothGattService txService = gatt.getService(UART_SERVICE_UUID);
        if (txService == null) {
            LogUtils.e("没有这个BluetoothGattService: " + UART_SERVICE_UUID);
            return -1;
        }
        BluetoothGattCharacteristic characteristic = txService.getCharacteristic(sendUUID);
        if (characteristic == null) {
            LogUtils.e("没有这个BluetoothGattCharacteristic: " + sendUUID);
            return -1;
        }
        LogUtils.d("发送的最终值: " + HexUtil.toHexString(buffer));
        characteristic.setValue(buffer);
        long startTime = System.currentTimeMillis();
        while (gatt != null && !gatt.writeCharacteristic(characteristic)) {
            if (SystemUtils.isTimeout(startTime, timeout)) {
                LogUtils.e("write超时！");
                return -1;
            }
        }
        return length - offset;
    }

    @Override
    public int read(byte[] buffer, int offset, int length, int timeout) throws IOException {
        if (gatt == null) {
            LogUtils.e("Gatt对象异常!");
            disconnect();
            return -1;
        }
        long startTime = System.currentTimeMillis();
        if (length > 0) {
            while (BUFFER_SERIAL.size() == 0) {
                if (SystemUtils.isTimeout(startTime, timeout)) {
                    return -1;
                }
            }
            // 需要重新开始计时
            startTime = System.currentTimeMillis();
            //从轮询缓冲队列中取出对应长度的数据
            for (int i = offset; i < length; ++i) {
                //判断轮询缓冲区的元素是否可用
                if (BUFFER_SERIAL.peek() != null) {
                    Byte b = BUFFER_SERIAL.poll();
                    if (b != null) {
                        buffer[i] = b;
                    } else {
                        if (SystemUtils.isTimeout(startTime, timeout)) {
                            //LogUtils.d( "read超时！");
                            return i - offset;
                        }
                    }
                }
            }
        } else {
            synchronized (LOCK_DATA_FRAME) {
                while (dataFrame == null) {
                    if (SystemUtils.isTimeout(startTime, timeout)) {
                        return -1;
                    }
                }
                System.arraycopy(dataFrame, 0, buffer, 0, dataFrame.length);
                int len = dataFrame.length;
                LogUtils.d("接收到的数据帧: " + HexUtil.toHexString(dataFrame));
                dataFrame = null;
                return len;
            }
        }
        //Log.d(TAG, "最终的接收字节转换: " + new String(buffer));
        //TODO 返回的是当前读取到的缓冲区的数据的长度(实际长度)!
        return length - offset;
    }

    @Override
    public void flush() throws IOException {
        BUFFER_SERIAL.clear();
    }

    @Override
    public void close() throws IOException {
        synchronized (thiz) {
            if (gatt != null) {
                gatt.disconnect();
                gatt.close();
                ClsUtils.refreshDeviceCache(gatt);
                gatt = null;
                isConnected = false;
                LogUtils.d("关闭Gatt执行完成!");
            }
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        for (BluetoothGattCallback callback : gattCallbacks) {
            callback.onConnectionStateChange(gatt, status, newState);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        for (BluetoothGattCallback callback : gattCallbacks) {
            callback.onServicesDiscovered(gatt, status);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        for (BluetoothGattCallback callback : gattCallbacks) {
            callback.onDescriptorWrite(gatt, descriptor, status);
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        for (BluetoothGattCallback callback : gattCallbacks) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                callback.onMtuChanged(gatt, mtu, status);
            }
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        for (BluetoothGattCallback callback : gattCallbacks) {
            callback.onCharacteristicWrite(gatt, characteristic, status);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        for (BluetoothGattCallback callback : gattCallbacks) {
            callback.onCharacteristicChanged(gatt, characteristic);
        }
    }

    public boolean isChameleonMini(byte[] raw) {
        BLERawUtils.Details details = BLERawUtils.find(7, raw);
        if (details != null) { // 查询成功，发现有效的包!
            byte[] bytes = details.getValue();
            if (bytes != null) {
                // 匹配服务的UUID!
                return Arrays.equals(BleSerialControl.UUID_RAW_MINI, bytes);
            }
        }
        return false;
    }

    public boolean isChameleonTiny(byte[] raw) {
        return !isChameleonMini(raw);
    }

    public boolean isDeviceNoBond(String address) {
        return bluetoothAdapter
                .getRemoteDevice(address)
                .getBondState() == BluetoothDevice.BOND_NONE;
    }

    public boolean isDeviceNoBond(DevBean devBean) {
        return isDeviceNoBond(devBean.getMacAddress());
    }

    public boolean connectNoCallback(DevBean t, BluetoothDevice device) {
        synchronized (thiz) {
            if (isConnected) {
                LogUtils.d("isConnected是TRUE状态，可能设备并未断开连接!");
                return true;
            }
            closeNoException();
            if (t == null) {
                return false;
            }
            // 结束搜索!
            bluetoothAdapter.cancelDiscovery();
            // 重置链接任务状态!
            resetTaskStatus();
            // 尝试绑定，如果返回True则是进入绑定进程
            boolean isMINI = isChameleonMini((byte[]) t.getObject());
            if (!isMINI && ClsUtils.createBond(t.getClass(), device)) {
                // 此时，我们需要在广播中继续链接GATT
                // 需要只等待绑定完成，而不是直接链接
                LogUtils.d("配对开始，将在等待广播结果！");
                if (isBondSuccessful()) {
                    connectGatt(device);
                } else {
                    resetTaskStatus();
                    return false;
                }
            } else {
                // 不可以进入绑定，可能是绑定过了，或者是MINI，直接链接。
                connectGatt(device);
                LogUtils.d("无法配对，可能是绑定过了，或者是MINI, 将直接链接尝试！");
            }
            LogUtils.d("开始等待连接任务执行完成（BLE异步回调初始化）");
            // 等待连接结果!
            boolean ret = isTaskExeSuccessful(isMINI ? 1000 * 6 : 1000 * 16);
            resetTaskStatus();
            if (ret) {
                return true;
            } else {
                // 连接失败，关闭蓝牙链接!
                closeNoException();
                return false;
            }
        }
    }

    /**
     * Connect to gatt
     */
    private void connectGatt(BluetoothDevice t) {
        gatt = t.connectGatt(context, false, this);
    }

    /**
     * Reset some params
     */
    private void resetTaskStatus() {
        asyncSyncStatus = 0;
        bondAsyncSyncStatus = 0;
    }

    /**
     * Check connect task is successful.
     */
    private boolean isTaskExeSuccessful(int timeout) {
        long startTime = System.currentTimeMillis();
        while (asyncSyncStatus == 0) {
            if (SystemUtils.isTimeout(startTime, timeout)) {
                closeNoException();
                asyncSyncStatus = 0;
                LogUtils.d("isTaskExeSuccessful(),连接任务超时。");
                return false;
            }
        }
        // 如果结果值是 1，则成功!
        return asyncSyncStatus == 1;
    }

    /**
     * Check bond task is successful.
     */
    private boolean isBondSuccessful() {
        long startTime = System.currentTimeMillis();
        int timeout = 1000 * 18;
        while (bondAsyncSyncStatus == 0) {
            if (asyncSyncStatus == -1) {
                LogUtils.d("连接任务出现问题，提前终止绑定过程!");
                return false;
            }
            if (SystemUtils.isTimeout(startTime, timeout)) {
                closeNoException();
                bondAsyncSyncStatus = 0;
                LogUtils.d("isBondSuccessful(),连接任务超时。");
                return false;
            }
        }
        // 如果结果值是 1，则成功!
        return bondAsyncSyncStatus == 1;
    }

    /**
     * Get on data receive listener!
     */
    public OnDataReceiveListener getOnDataReceiveListener() {
        return onDataReceiveListener;
    }

    public void setOnDataReceiveListener(OnDataReceiveListener onDataReceiveListener) {
        this.onDataReceiveListener = onDataReceiveListener;
    }

    /**
     * Close device and no exception throw.
     */
    private void closeNoException() {
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Enable Notification
     */
    private void enableNotifyOnUARTService(BluetoothGatt gatt, UUID uuid) {
        BluetoothGattService service = gatt.getService(UART_SERVICE_UUID);
        if (service == null) {
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);
        if (characteristic == null) {
            return;
        }
        gatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(RECV_DESC_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        long startTime = System.currentTimeMillis();
        while (!gatt.writeDescriptor(descriptor)) {
            if (SystemUtils.isTimeout(startTime, 2000)) {
                asyncSyncStatus = -1;
                LogUtils.d("enableNotify()超时。");
                return;
            }
        }
        //  Log.d(TAG, "将会尝试开启接收来自设备的广播");
    }

    /**
     * The chameleon tiny pro is have ctrl characteristic exists.
     */
    public boolean isCtrlCharacteristicExists() {
        if (gatt == null) return false;
        BluetoothGattService service = gatt.getService(UART_SERVICE_UUID);
        if (service == null) return false;
        return service.getCharacteristic(CTRL_CHARACT_UUID) != null;
    }

    /**
     * The new device
     */
    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public boolean isPushDataToBuffer() {
        return pushDataToBuffer;
    }

    public void setPushDataToBuffer(boolean pushDataToBuffer) {
        this.pushDataToBuffer = pushDataToBuffer;
    }

    public void addGattCallback(BluetoothGattCallback callback) {
        if (!gattCallbacks.contains(callback))
            gattCallbacks.add(callback);
    }

    public void removeGattCallback(BluetoothGattCallback callback) {
        gattCallbacks.remove(callback);
    }

    private boolean requestInterval(int v) {
        if (gatt == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return gatt.requestConnectionPriority(v);
        }
        return false;
    }

    public boolean requestIntervalHigh() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return requestInterval(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        }
        return false;
    }

    public boolean requestIntervalBalanced() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return requestInterval(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
        }
        return false;
    }

    public boolean requestIntervalLowPower() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return requestInterval(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER);
        }
        return false;
    }

    public interface OnDataReceiveListener {
        void onReceive(byte[] frame);
    }
}
