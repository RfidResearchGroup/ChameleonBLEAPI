package com.proxgrind.chameleon.defined;

import java.io.IOException;

import com.proxgrind.devices.DriverInterface;

/**
 * 接口，定义变色龙的执行器的功能!
 *
 * @author DXL
 */
public interface IChameleonExecutor {
    boolean initExecutor(DriverInterface com);

    byte[] requestChameleon(String at, int timeout, boolean xmodemMode);

    byte[] requestChameleon(int timeout, int length);

    int requestChameleon(String at, int timeout) throws IOException;

    DriverInterface getDriver();

    int clear(int timeout);

    void close();

    String getVersion();

    boolean isConnected();
}
