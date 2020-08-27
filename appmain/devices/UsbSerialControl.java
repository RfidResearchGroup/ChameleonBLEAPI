package com.proxgrind.devices;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.proxgrind.chameleon.R;
import com.proxgrind.chameleon.callback.ConnectCallback;
import com.proxgrind.chameleon.utils.system.LogUtils;
import com.proxgrind.chameleon.utils.system.SystemUtils;
import com.proxgrind.chameleon.utils.tools.Commons;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/*
 * Usb 2 uart Serial implements
 */
public class UsbSerialControl implements DriverInterface<String, UsbManager> {

    //日志标签
    private static final String LOG_TAG = UsbSerialControl.class.getSimpleName();
    private final int UNIQUE_ID = 0x04;
    //广播名称
    private final String USB_PERMISSION_ACTION = "cn.rrg.nfctools.UsbSerialPer";
    //设备名称!
    private final String usbName = context.getString(R.string.usb_name);
    //串口对象
    private UsbSerialDevice mPort = null;
    //单例模式
    private static UsbSerialControl mThiz = null;
    //回调接口
    private DevCallback<String> mCallback = null;
    //广播接收，由于是单例，因此实际上广播接收也可以设置为单例!
    private BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //取到广播的意图
            String action = intent.getAction();
            //对比意图，根据意图做出回调选择
            if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action) ||
                    UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) ||
                    "cn.rrg.devices.usb_attach_uart".equals(action)) {
                Log.d(LOG_TAG, "收到UsbSerial设备寻找的广播!");
                if (mCallback != null) {
                    if (initUsbSerial(context)) {
                        //初始化成功则回调串口设备加入方法
                        mCallback.onAttach(usbName);
                    } else {
                        //不成则打印到LOG
                        Log.e(LOG_TAG, "no usb permission!");
                    }
                }
            }

            //在申请权限的时候如果成功那么应当进行设备的初始化
            if (USB_PERMISSION_ACTION.equals(action)) {
                //get permission success
                if (initUsbSerial(context)) {
                    //初始化成功则回调串口设备加入方法
                    mCallback.onAttach(usbName);
                } else {
                    //不成则打印到LOG
                    Log.e(LOG_TAG, "no usb permission!");
                }
            }

            //在设备移除时应当释放USB设备
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                //判断并且释放USB串口
                if (mThiz != null) {
                    try {
                        mThiz.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //回调设备移除接口
                if (mPort != null)
                    if (mCallback != null) {
                        try {
                            mCallback.onDetach(usbName);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                isConnected = false;
                LogUtils.d("设备移除了。");
            }
        }
    };
    ;
    //注册状态
    private static volatile boolean isRegister = false;
    //轮询队列
    private static final Queue<Byte> recvBufQueue = new ConcurrentLinkedQueue<>();
    // 数据回调!
    private static final UsbSerialInterface.UsbReadCallback readCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] bytes) {
            //进行加锁，提高数据吞吐稳定性
            for (byte b : bytes) {
                recvBufQueue.add(b);
            }
            //LogUtils.d("USB自动接收到的数据: " + new String(bytes));
        }
    };
    private volatile boolean isConnected = false;

    /*私有化构造方法，懒汉单例模式*/
    private UsbSerialControl() {
        //you can't invoke this constructor
        //beacause this class is single-instance!
    }

    public static UsbSerialControl get() {
        synchronized (LOG_TAG) {
            if (mThiz != null) {
                return mThiz;
            } else {
                mThiz = new UsbSerialControl();
            }
            return mThiz;
        }
    }

    //串口备初始化函数
    private boolean initUsbSerial(Context context) {
        //得到Usb管理器
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) return false;
        //尝试取出所有可用的列表
        if (usbManager.getDeviceList() == null || usbManager.getDeviceList().size() <= 0) {
            Log.d(LOG_TAG, "initUsbSerial() 未发现设备!");
            return false;
        }
        //迭代集合里面的设备对象
        List<UsbDevice> devList = new ArrayList<>(usbManager.getDeviceList().values());
        //取出第一个USB对象
        UsbDevice usbDevice = devList.get(0);
        //判断设备是否支持
        if (!UsbSerialDevice.isSupported(usbDevice)) {
            Log.d(LOG_TAG, "UsbSerial支持检测结果: false");
            return false;
        }
        //如果对于这个设备没有权限!
        if (!usbManager.hasPermission(usbDevice)) {
            //发送广播申请权限
            PendingIntent intent =
                    PendingIntent.getBroadcast(context,
                            0, new Intent(USB_PERMISSION_ACTION), 0);
            //Log.d(LOG_TAG, "尝试获得USB权限!");
            usbManager.requestPermission(usbDevice, intent);
            //当没有权限的时候应当直接返回
            return false;
        }
        //一切正常返回true!
        return connect();
    }

    @Override
    public int write(byte[] buffer, int offset, int length, int timeout) throws IOException {
        //TODO 注释防止外泄
        if (mPort == null) {
            //Log.e(LOG_TAG, "port is null");
            return -1;
        }
        //构建一个可用字节的缓冲区
        byte[] tmpBuf = new byte[length - offset];
        //将可用字节灌装到定义的缓冲区
        System.arraycopy(buffer, offset, tmpBuf, 0, tmpBuf.length);
        //在同步块中进行提交操作!
        mPort.write(tmpBuf);
        //Log.d(LOG_TAG, "send: " + new String(buffer) + ", len" + buffer.length);
        //Log.d(LOG_TAG, "发送的字节: " + HexUtil.toHexString(tmpBuf, 0, length - offset));
        return length - offset;
    }

    @Override
    public int read(byte[] buffer, int offset, int length, int timeout) throws IOException {
        if (mPort == null) {
            //Log.e(LOG_TAG, "port is null");
            return -1;
        }
        long startTime = System.currentTimeMillis();
        //Log.d(LOG_TAG, "数据缓冲区内长度正常，开始拷贝...");
        while (recvBufQueue.size() < length) {
            if (SystemUtils.isTimeout(startTime, timeout)) {
                return -1;
            }
        }
        int len = 0;
        //从轮询缓冲队列中取出对应长度的数据
        for (int i = offset; i < length; ++i) {
            //判断轮询缓冲区的元素是否可用
            if (recvBufQueue.peek() != null) {
                Byte b = recvBufQueue.poll();
                if (b != null) {
                    buffer[i] = b;
                    ++len;
                }
            }
        }
        //Log.d(LOG_TAG, "接收到的数据: " + HexUtil.toHexString(buffer, offset, length));
        //TODO 返回的是当前读取到的缓冲区的数据的长度(实际长度)!
        return len;
    }

    @Override
    public void flush() throws IOException {
        //don't support flush
    }

    @Override
    public void close() throws IOException {
        isConnected = false;
        if (mPort == null) {
            Log.e(LOG_TAG, "port is null");
            return;
        }
        mPort.close();
    }

    @Override
    public void register(DevCallback<String> callback) {
        mCallback = callback;
        if (isRegister) {
            LogUtils.d("USB驱动可能已经注册过了。");
            return;
        }
        IntentFilter filter = new IntentFilter(USB_PERMISSION_ACTION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction("cn.rrg.devices.usb_attach_uart");
        try {
            context.registerReceiver(usbReceiver, filter);
            LogUtils.d("USB注册完成!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        isRegister = true;
    }

    @Override
    public void connect(String t, ConnectCallback callback) {
        if (connect()) {
            isConnected = true;
            callback.onConnectSucces();
        } else {
            isConnected = false;
            callback.onConnectFail();
        }
    }

    @Override
    public boolean isDeviceConnected() {
        return mPort != null && isConnected;
    }

    public boolean connect() {
        if (mPort != null) {
            mPort.close();
            mPort = null;
        }
        isConnected = false;
        //得到Usb管理器
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            return false;
        }
        //迭代集合里面的设备对象
        List<UsbDevice> devList = new ArrayList<>(usbManager.getDeviceList().values());
        if (devList.size() <= 0) {
            return false;
        }
        //取出第一个USB对象
        UsbDevice usbDevice = devList.get(0);
        //USB链接!
        UsbDeviceConnection connection = usbManager.openDevice(usbDevice);
        //判断是否非空，为空证明没有权限
        if (connection == null) {
            //发送广播申请权限
            PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(USB_PERMISSION_ACTION), 0);
            //Log.d(LOG_TAG, "尝试获得USB权限!");
            usbManager.requestPermission(usbDevice, intent);
            //当没有权限的时候应当直接返回
            return false;
        }
        Log.d(LOG_TAG, "开始尝试打开设备!");
        //得到串口端口对象
        mPort = UsbSerialDevice.createUsbSerialDevice(usbDevice, connection);
        //尝试打开串口
        if (mPort.open()) {
            //设置波特率
            mPort.setBaudRate(115200);
            //设置数据位
            mPort.setDataBits(UsbSerialDevice.DATA_BITS_8);
            //设置停止位
            mPort.setStopBits(UsbSerialDevice.STOP_BITS_1);
            //奇偶校验值
            mPort.setParity(UsbSerialDevice.PARITY_NONE);
            //数据流控制
            mPort.setFlowControl(UsbSerialDevice.FLOW_CONTROL_OFF);
            //新数据回调
            mPort.read(readCallback);
            Log.d(LOG_TAG, "Usb链接成功，通信创建成功!!");
            return true;
        }
        return false;
    }

    public boolean connectAndSetConnected() {
        return isConnected = connect();
    }

    @Override
    public UsbManager getAdapter() {
        return (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    @Override
    public String getDevice() {
        return mPort != null ? mPort.getClass().getSimpleName() : null;
    }

    @Override
    public void disconnect() {
        //TODO 暂时不做处理
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isConnected = false;
    }

    @Override
    public int getUniqueId() {
        return UNIQUE_ID;
    }

    @Override
    public void unregister() {
        //广播解注册
        if (isRegister) {
            try {
                context.unregisterReceiver(usbReceiver);
                isRegister = false;
                LogUtils.d("解注册USB完成!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
