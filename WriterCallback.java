package com.proxgrind.chameleon.callback;

public interface WriterCallback extends BaseCallback {
    void onFinish();

    void onDataInvalid();

    void onTagAbnormal();
}
 