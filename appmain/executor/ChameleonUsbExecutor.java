package com.proxgrind.chameleon.executor;

import com.proxgrind.chameleon.defined.IChameleonExecutor;

/**
 * @author DXL
 * USB执行器实现类，具体封装了执行和获得返回值的函数!
 */
public class ChameleonUsbExecutor extends ChameleonStdExecutor implements IChameleonExecutor {

    private static final ChameleonUsbExecutor executor = new ChameleonUsbExecutor();

    /**
     * @return 单例
     */
    public static IChameleonExecutor get() {
        return executor;
    }
}
