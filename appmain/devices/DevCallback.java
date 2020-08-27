package com.proxgrind.devices;

import java.io.Serializable;

public interface DevCallback<T> extends Serializable {
    // 是否符合设备预期
    boolean isDev(T dev);

    //新设备发现回调
    void onAttach(T dev);

    //设备移除回调
    void onDetach(T dev);
}
