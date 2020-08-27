package com.proxgrind.chameleon.executor;

import com.proxgrind.chameleon.utils.tools.HexUtil;
import com.proxgrind.chameleon.utils.system.LogUtils;
import com.proxgrind.chameleon.utils.system.SystemUtils;
import com.proxgrind.chameleon.defined.IChameleonExecutor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author DXL
 * BLE执行器实现类，具体封装了执行和获得返回值的函数!
 * 由于BLE的通讯原因，我们必须实现封包和解包!
 */
public class ChameleonBleExecutor extends ChameleonStdExecutor {

    private static ChameleonBleExecutor executor = new ChameleonBleExecutor();

    /**
     * @return 单例
     */
    public static IChameleonExecutor get() {
        return executor;
    }

    /**
     * @param at      指令，ascii编码集，需要在后缀附带\r换行!
     * @param timeout 超时，多久之后接收不到完整的数据帧自动返回?
     * @return 设备的返回信息，可能超时(无返回值),如果超时则返回null，否则返回对应命令的应答!
     */
    @Override
    public byte[] requestChameleon(String at, int timeout, boolean xmodemMode) {
        LogUtils.d("发送的指令: " + at);
        //初始化必须的变量
        ByteArrayOutputStream bos = new ByteArrayOutputStream(512);
        try {
            mCom.flush();
            //请求并且判断结果!
            if (requestChameleon(at, timeout) == -1) return null;
            long currentTime = System.currentTimeMillis();
            //LogUtils.d("当前线程ID: " + Thread.currentThread().getId());
            do {
                if (SystemUtils.isTimeout(currentTime, timeout)) return null;
                //开始接收，每次接收一个字节!
                byte tmpByte = read(0);
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
                            //延迟判断新行，延迟最大限度提升成功率!!!
                            tmpByte = read(5);
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
                            LogUtils.d("XModem模式，终止接收！");
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
            byte tmpByte = read(timeout);
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
        // TODO 不用自己加回车!
        //Log.d(LOG_TAG, "发送的命令: " + at);
        //at = checkAT(at);
        byte[] sendBuf = HexUtil.getAsciiBytes(at);
        // BLE进行了封包，我们此处需要封包! TODO 在外部，给对象进行注入封包!
        //sendBuf = DataPackage.dopack(sendBuf, DataPackage.TEXT_HEX);
        return mCom.write(sendBuf, 0, sendBuf.length, timeout);
    }
}
