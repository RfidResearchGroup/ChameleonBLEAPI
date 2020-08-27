package com.proxgrind.chameleon.defined;

import com.proxgrind.chameleon.detection.ResultBean;

public interface DecryptCallback {
    void onMsg(String msg);

    void onKey(ResultBean result);
}