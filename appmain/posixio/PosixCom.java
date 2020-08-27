package com.proxgrind.chameleon.posixio;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Serializable;

/*
 * author DXL
 * 通信接口，实现了必要的元素传递以及通信实现规定，可以用来桥接通讯!
 * Communication interface, All communication bridger!
 */
public interface PosixCom extends Serializable, Closeable, Flushable {

    //实现了发送消息
    int write(byte[] sendMsg, int offset, int length, int timeout) throws IOException;

    //实现了接收消息
    int read(byte[] recvMsg, int offset, int length, int timeout) throws IOException;

    //实现了刷新消息
    @Override
    void flush() throws IOException;

    //实现了通信关闭
    @Override
    void close() throws IOException;
}
