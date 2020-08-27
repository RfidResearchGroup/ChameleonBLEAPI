package com.proxgrind.chameleon.executor;

import com.proxgrind.chameleon.utils.tools.HexUtil;
import com.proxgrind.chameleon.utils.system.LogUtils;
import com.proxgrind.chameleon.utils.system.SystemUtils;
import com.proxgrind.chameleon.defined.IChameleonExecutor;
import com.proxgrind.chameleon.utils.chameleon.ChameleonResult;
import com.proxgrind.devices.DriverInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author DXL
 * BLE执行器实现类，具体封装了执行和获得返回值的函数!
 * 由于BLE的通讯原因，我们必须实现封包和解包!
 */
public class ChameleonStdExecutor implements IChameleonExecutor {

    //static final Object lock = new Object();
    static DriverInterface mCom;
    public static String version = "not";

    /**
     * 设备初始化方法!
     *
     * @param com 传入的串口通信接口!
     */
    @Override
    public boolean initExecutor(DriverInterface com) {
        mCom = com;
        //判断一下设备是否可以正常开启关闭!
        byte[] result = requestChameleon("VERSION?", 2000, false);
        // 重发!
        if (result == null)
            result = requestChameleon("VERSION?", 2000, false);
        // 判断结果!
        if (result != null && ChameleonResult.isCommandResponse(result)) {
            ChameleonResult r = new ChameleonResult(null);
            if (r.processCommandResponse(result)) {
                version = r.getCmdResponseData();
                ChameleonExecutorProxy.setExecutor(this);
                LogUtils.d("ChameleonExecutorProxy初始化成功: " + this);
                return true;
            }
        }
        LogUtils.d("ChameleonExecutorProxy初始化失败: " + new String(result != null ? result : new byte[0]));
        return false;
    }

    /**
     * @param at      指令，ascii编码集，需要在后缀附带\r换行!
     * @param timeout 超时，多久之后接收不到完整的数据帧自动返回?
     * @return 设备的返回信息，可能超时(无返回值),如果超时则返回null，否则返回对应命令的应答!
     */
    @Override
    public byte[] requestChameleon(String at, int timeout, boolean xmodemMode) {
        try {
            //请求并且判断结果!
            if (requestChameleon(at, timeout) == -1) return null;
            //初始化必须的变量
            ByteArrayOutputStream bos = new ByteArrayOutputStream(512);
            long currentTime = System.currentTimeMillis();
            //Log.d(LOG_TAG, "得到锁成功，当前线程ID: " + Thread.currentThread().getId());
            do {
                if (SystemUtils.isTimeout(currentTime, timeout)) return null;
                //开始接收，每次接收一个字节!
                byte tmpByte = read(1);
                if (tmpByte != -1) {
                    //接收完毕,有有效的字节!!!
                    bos.write(tmpByte);
                    //Log.d(LOG_TAG, "打印接收到的字节: " + HexUtil.toHexString(tmpByte));
                    //有有效数据，进行超时拖延!
                    currentTime = System.currentTimeMillis();
                    //判断到换行，则可能是一帧的结束!
                    // FIXME: 2019/4/21 谨记，上传或下载将会打开xmodem通道，此时应当进行判断，断定下一步的操作!
                    if (tmpByte == 0x0A) {
                        //Log.d(LOG_TAG, "有换行，下一步判断是否需要继续接收!");
                        if (!xmodemMode) {
                            //延迟判断新行，最大限度提升成功率!!!
                            tmpByte = read(50);
                            if (tmpByte != -1) {
                                //Log.d(LOG_TAG, "需要");
                                bos.write(tmpByte);
                                currentTime = System.currentTimeMillis();
                            } else {
                                //Log.d(LOG_TAG, "不需要");
                                //接收完毕，直接返回!
                                //Log.d(LOG_TAG, "锁释放完成: " + Thread.currentThread().getId());
                                return bos.toByteArray();
                            }
                        } else {
                            return bos.toByteArray();
                        }
                    }
                }
            } while (true);   //超时中处理!
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 在指定的时间内接收指定的长度的值，
     *
     * @param timeout 超时值
     * @param length  欲接收的长度!
     * @return 接收结果!
     */
    @Override
    public byte[] requestChameleon(int timeout, int length) {
        byte[] ret = new byte[length];
        int pos = 0;
        long currentTime = System.currentTimeMillis();
        //Log.d(LOG_TAG, "得到锁成功，当前线程ID: " + Thread.currentThread().getId());
        do {
            //开始接收，每次接收一个字节!
            byte tmpByte = read(1);
            if (tmpByte != -1) {
                ret[pos] = tmpByte;
                if (++pos == length) break;
            }
        } while (SystemUtils.isTimeout(currentTime, timeout));   //超时中处理!
        return ret;
    }

    /**
     * @param at      指令，ascii编码集，需要在后缀附带\r换行!
     * @param timeout 超时，多久之后接收不到完整的数据帧自动返回?
     * @return 发送成功的字节数，如果发送失败则返回 -1
     */
    @Override
    public int requestChameleon(String at, int timeout) throws IOException {
        if (at == null) return -1;
        //Log.d(LOG_TAG, "尝试得到锁，当前线程ID: " + Thread.currentThread().getId());
        //发送命令必须回应，否则系命令错误!
        at = checkAT(at);
        byte[] sendBuf = HexUtil.getAsciiBytes(at);
        return mCom.write(sendBuf, 0, sendBuf.length, timeout);
    }

    /**
     * @param at 指令!
     * @return 如果命令带\r后缀，则直接返回，否则添加!
     */
    protected String checkAT(String at) {
        return at.endsWith("\r") ? at : (String.format("%s\r", at));
    }

    /**
     * 读取一个字节，简化读取!
     *
     * @param timeout 超时值
     * @return 读取结果，-1为失败!
     */
    protected byte read(int timeout) {
        byte[] b = new byte[1];
        try {
            int len = mCom.read(b, 0, 1, timeout);
            if (len > 0) {
                return b[0];
            }
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 得到通信端口!
     *
     * @return 通信端口!
     */
    @Override
    public DriverInterface getDriver() {
        LogUtils.w("警告，直接操作COM端口可能产生通信数据串行干扰的问题，" +
                "请务必保证同时操作com的只有一个Thread: " + Thread.currentThread().getId());
        return mCom;
    }

    /**
     * 清除可能缓存的数据!
     *
     * @param timeout 超时值
     * @return 被清除的字节个数
     */
    @Override
    public int clear(int timeout) {
        int count = 0;
        long currentTime = System.currentTimeMillis();
        //Log.d(LOG_TAG, "得到锁成功，当前线程ID: " + Thread.currentThread().getId());
        do {
            //开始接收，每次接收一个字节!
            byte tmpByte = read(1);
            if (tmpByte != -1) {
                ++count;
            }
        } while (System.currentTimeMillis() - currentTime < timeout);   //超时中处理!
        return count;
    }

    @Override
    public void close() {
        if (getDriver() != null) {
            try {
                getDriver().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean isConnected() {
        return mCom != null && mCom.isDeviceConnected();
    }
}
