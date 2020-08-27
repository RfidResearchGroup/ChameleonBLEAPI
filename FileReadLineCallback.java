package com.proxgrind.chameleon.callback;

public interface FileReadLineCallback {
    //读取完成的结果数组!
    void onReadFinish(String[] line);

    //读取失败的消息!
    void onReadFail(String msg);
}
