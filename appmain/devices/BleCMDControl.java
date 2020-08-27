package com.proxgrind.devices;

import com.proxgrind.chameleon.javabean.DeviceInfo;
import com.proxgrind.chameleon.packets.DataPackets;
import com.proxgrind.chameleon.utils.system.LogUtils;
import com.proxgrind.chameleon.utils.tools.HexUtil;

import java.io.IOException;
import java.util.Arrays;

/*
 * 同步任务!
 * 将会给对象上锁!
 * */
public class BleCMDControl {
    /**
     * 命令定义区域，我们需要进行命令包标志的定义！
     */
    private static final int BLE_INFO = 0X69;

    private static final BleSerialControl control = BleSerialControl.get();
    private static final int BLE_INFO_PACKET_SIZE_MAX_MINI = 77;
    private static final int BLE_INFO_PACKET_SIZE_MAX_TINY = 25;
    private static final int TRANSFER_TIMEOUT = 2000;
    private static final int REVEICE_TIMEOUT = TRANSFER_TIMEOUT;

    public static DeviceInfo getBLEInfo() {
        DeviceInfo info = new DeviceInfo();
        // 谨记上锁!
        synchronized (control) {
            if (control.isDeviceConnected()) {
                try {
                    if (isChameleonMini()) {
                        getInfoFromMini(info);
                    } else {
                        getInfoFromTiny(info);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                LogUtils.d("设备未连接，可能通讯是USB");
            }
            return info;
        }
    }

    public static boolean isChameleonMini() {
        return !control.isCtrlCharacteristicExists();
    }

    private static void getInfoFromMini(DeviceInfo info) throws IOException {
        control.setPushDataToBuffer(false);
        // 打包一个命令!
        byte[] infoCMD = DataPackets.getData(BLE_INFO);
        control.write(infoCMD, 0, infoCMD.length, TRANSFER_TIMEOUT);
        byte[] recvs = new byte[BLE_INFO_PACKET_SIZE_MAX_MINI];
        if (control.read(recvs, 0, 0, REVEICE_TIMEOUT) >= BLE_INFO_PACKET_SIZE_MAX_MINI) {
            // 主版本号
            info.setMainVersion(recvs[0]);
            // 次版本号
            info.setMinorVersion(recvs[1]);
            // 版本特性
            info.setVersionFlag(HexUtil.byteArrayToInt(new byte[]{recvs[5], recvs[4], recvs[3], recvs[2]}));
            // 版本字符串（BLE）
            info.setBleVersion(new String(Arrays.copyOfRange(recvs, 6, 37)));
            // BLE电池电压!
            info.setBatteryVoltage(HexUtil.byteArrayToInt(new byte[]{recvs[41], recvs[40], recvs[39], recvs[38]}));
            // BLE电池百分比!
            info.setBatteryPercent(recvs[42]);
            // AVR相关参数!
            info.setAvrMainVersion(recvs[43]);
            info.setAvrMinorVersion(recvs[44]);
            info.setAvrVersion(new String(Arrays.copyOfRange(recvs, 44, 74)));
        } else {
            LogUtils.d("接收到的长度字节不够!");
        }
        control.setPushDataToBuffer(true);
    }

    private static void getInfoFromTiny(DeviceInfo info) throws IOException {
        LogUtils.d("请求Tiny的信息");
        control.setPushDataToBuffer(false);
        byte[] infoCMD = new byte[]{(byte) 0XA5, 0X69, 0X00, 0X00};
        // Tiny分离出来了一个控制点特征单独用来进行UART之外的操作！
        // 因此我们需要往Tiny的控制特征写，而不是往标准的UART写接口写。
        control.write(infoCMD, 0, infoCMD.length, TRANSFER_TIMEOUT, BleSerialControl.CTRL_CHARACT_UUID);
        byte[] recvs = new byte[1024];
        if (control.read(recvs, 0, 0, REVEICE_TIMEOUT) >= BLE_INFO_PACKET_SIZE_MAX_TINY) {
            // BLE电池电压!
            info.setBatteryVoltage(HexUtil.toIntFrom2Byte(new byte[]{recvs[1], recvs[0]}));
            // BLE电池百分比!
            info.setBatteryPercent(recvs[2]);
            // BLE版本号
            info.setBleVersion(new String(Arrays.copyOfRange(recvs, 3, 18)));
        } else {
            LogUtils.d("接收到的长度字节不够!");
        }
        control.setPushDataToBuffer(true);
    }

    public static void openAirplaneMode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] infoCMD = new byte[]{(byte) 0XA5, 0X6B, 0X00, 0X00};
                try {
                    control.write(infoCMD, 0, infoCMD.length, TRANSFER_TIMEOUT, BleSerialControl.CTRL_CHARACT_UUID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 进行数据的传输与应答帧的接收
     */
    public static byte[] tranRecv(int cmd, byte[] data, int timeout) {
        BleSerialControl control = BleSerialControl.get();
        if (!control.isDeviceConnected()) return null;
        // 关闭缓冲区转发
        control.setPushDataToBuffer(false);
        // 拼包并且发送数据!
        byte[] sendPack = new DataPackets(cmd, data).getData();
        byte[] ret = new byte[BleSerialControl.MTU];
        try {
            control.write(sendPack, 0, sendPack.length, timeout);
            int status = control.read(ret, 0, 0, timeout);
            if (status == -1) {
                //超时了，我们返回空的字节组表示超时
                return new byte[0];
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        // 回复缓冲区转发
        control.setPushDataToBuffer(true);
        return ret;
    }
}
