package com.proxgrind.chameleon.executor;

import java.io.IOException;
import java.util.ArrayList;

import com.proxgrind.chameleon.defined.BasicTypesCallback;
import com.proxgrind.chameleon.posixio.PosixCom;
import com.proxgrind.chameleon.defined.IChameleonExecutor;
import com.proxgrind.chameleon.utils.system.SystemUtils;
import com.proxgrind.devices.DriverInterface;

/**
 * 这个类实际是个代理类，留了一个接口来初始化其中引用的实际（执行器）的实现!
 * 抽出这个类主要是为了实现将具体的类隐匿。
 * 因此我们需要将初始化的必要接口留下来，此类也必须是单例!
 *
 * @author DXL
 */
public class ChameleonExecutorProxy implements IChameleonExecutor {

    private static IChameleonExecutor mExecutor;
    private static ChameleonExecutorProxy mImpl;
    private ArrayList<BasicTypesCallback.ByteType> entrys = new ArrayList<>();

    public static void setExecutor(IChameleonExecutor executor) throws RuntimeException {
        mExecutor = executor;
    }

    public static ChameleonExecutorProxy getInstance() {
        synchronized (ChameleonExecutorProxy.class) {
            if (mImpl == null) mImpl = new ChameleonExecutorProxy();
        }
        return mImpl;
    }

    @Override
    public boolean initExecutor(DriverInterface com) {
        if (mExecutor != null)
            return mExecutor.initExecutor(com);
        return false;
    }

    @Override
    public byte[] requestChameleon(String at, int timeout, boolean xmodemMode) {
        if (mExecutor != null)
            return callback(mExecutor.requestChameleon(at, timeout, xmodemMode));
        else return "Device Disconnected".getBytes();
    }

    @Override
    public byte[] requestChameleon(int timeout, int length) {
        if (mExecutor != null)
            return callback(mExecutor.requestChameleon(timeout, length));
        else return "Device Disconnected".getBytes();
    }

    @Override
    public int requestChameleon(String at, int timeout) throws IOException {
        if (mExecutor != null)
            return mExecutor.requestChameleon(at, timeout);
        else
            return -1;
    }

    @Override
    public DriverInterface getDriver() {
        if (mExecutor == null) return null;
        return mExecutor.getDriver();
    }

    @Override
    public int clear(int timeout) {
        if (mExecutor != null)
            return mExecutor.clear(timeout);
        else return -1;
    }

    @Override
    public void close() {
        try {
            DriverInterface driverInterface = getDriver();
            if (driverInterface != null) {
                driverInterface.disconnect();
                driverInterface.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getVersion() {
        if (mExecutor != null)
            return mExecutor.getVersion();
        else
            return "Device Disconnected";
    }

    @Override
    public boolean isConnected() {
        if (mExecutor != null)
            return mExecutor.isConnected();
        return false;
    }

    public void addEntry(BasicTypesCallback.ByteType entry) {
        this.entrys.add(entry);
    }

    public void removeEntry(BasicTypesCallback.ByteType entry) {
        this.entrys.remove(entry);
    }

    public byte[] callback(byte[] in) {
        if (entrys.size() > 0)
            for (BasicTypesCallback.ByteType type : entrys) {
                type.onBytes(in);
            }
        return in;
    }

    public IChameleonExecutor getExecutorInternalImpl() {
        return mExecutor;
    }
}
