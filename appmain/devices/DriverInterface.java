package com.proxgrind.devices;

import android.app.Application;

import com.proxgrind.chameleon.posixio.PosixCom;
import com.proxgrind.chameleon.utils.system.AppContextUtils;
import com.proxgrind.chameleon.callback.ConnectCallback;

/*
 * 驱动程序类
 * 泛型1 -> 设备实体类
 * 泛型2 -> 适配器类
 */
public interface DriverInterface<Device, Adapter> extends PosixCom {
    Application context = AppContextUtils.app;

    //注册广播之类的事件
    void register(DevCallback<Device> callback);

    //链接到设备
    void connect(Device t, ConnectCallback callback);

    // 设备是否连接!
    boolean isDeviceConnected();

    //得到当前的驱动程序适配器类
    Adapter getAdapter();

    //得到当前的驱动程序的设备类
    Device getDevice();

    //断开与设备的链接（在某些设备上不一定是立刻生效的）
    void disconnect();

    //获得驱动的ID!
    int getUniqueId();

    //解注册广播之类的
    void unregister();
}