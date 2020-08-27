package com.proxgrind.chameleon.callback;

import java.io.Serializable;

public interface ConnectCallback extends Serializable {
    //连接成功时的回调
    void onConnectSucces();

    //连接失败时的回调
    void onConnectFail();
}
