package com.proxgrind.chameleon.xmodem;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.proxgrind.chameleon.posixio.PosixCom;
import com.proxgrind.chameleon.utils.tools.HexUtil;
import com.proxgrind.chameleon.utils.system.LogUtils;
import com.proxgrind.chameleon.utils.system.SystemUtils;

/**
 * @author DXL
 * @version 1.1
 */
public class XModem128 extends AbstractXModem {

    // 开始
    private byte SOH = 0x01;
    // 超时
    private final int TIMEOUT = 1000;

    public XModem128(PosixCom com) {
        super(com);
    }

    final byte calcChecksum(byte[] datas, int count) {
        byte checksum = 0;
        int pos = 0;
        while (count-- != 0) {
            checksum += datas[pos++];
        }
        return checksum;
    }

    @Override
    public boolean send(InputStream sources) throws IOException {
        // 错误包数
        int errorCount;
        // 包序号
        byte blockNumber = 0x01;
        // 读取到缓冲区的字节数量
        int nbytes;
        // 初始化数据缓冲区
        byte[] sector = new byte[mBlockSize];

        /*if (read(3000) != mNAK) {
            LogUtils.d("接收端没有NAK回复发起接收");
            return false;
        }*/
        // 读取字节初始化
        while ((nbytes = sources.read(sector)) > 0) {
            // 如果最后一包数据小于128个字节，以0xff补齐
            if (nbytes < mBlockSize) {
                for (int i = nbytes; i < mBlockSize; i++) {
                    sector[i] = (byte) 0x1A;
                }
            }
            // 同一包数据最多发送10次
            errorCount = 0;
            while (errorCount < mErrorMax) {
                // 不分包且一次性发送!
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                buffer.write(SOH);
                buffer.write(blockNumber);
                buffer.write((255 - blockNumber));
                buffer.write(sector);
                buffer.write(calcChecksum(sector, sector.length));
                mCom.write(buffer.toByteArray(), 0, buffer.size(), TIMEOUT);
                flush(); //6、刷新缓冲区，发送数据!

                // 获取应答数据
                byte data = read(TIMEOUT);
                // 如果收到应答数据则跳出循环，发送下一包数据
                // 未收到应答，错误包数+1，继续重发
                if (data == mNAK) {
                    Log.d(LOG_TAG, "NAK已收到，将会无条件重新传输!");
                    //重传!
                    ++errorCount;
                } else if (data == mCAN) {
                    //终止命令!
                    Log.d(LOG_TAG, "CAN已收到，将会无条件结束发送!");
                    return false;
                } else if (data == mACK) {
                    Log.d(LOG_TAG, "ACK已收到，可以进行下一轮发送!");
                    break;
                } else {
                    Log.d(LOG_TAG, "未知的值: " + HexUtil.toHexString(data));
                    //重传!
                    ++errorCount;
                }
            }
            // 包序号自增
            blockNumber = (byte) ((++blockNumber) % 256);
        }
        boolean isAck = false;
        while (!isAck) {
            write(mEOT, TIMEOUT);
            isAck = read(TIMEOUT) == mACK;
        }
        // 接收与发送一个字节，告知结束发送!
        return true;
    }

    @Override
    public boolean recv(OutputStream target) throws IOException {
        // 错误包数
        int errorCount = 0;
        // 包序号
        byte blocknumber = 0x01;
        // 数据
        byte data;
        // 校验和
        int checkSum;
        // 初始化数据缓冲区
        byte[] sector = new byte[mBlockSize];
        // 握手，发起传输!
        write(mNAK, TIMEOUT);
        while (true) {
            if (errorCount > mErrorMax) {
                LogUtils.d("错误重试次数已达上限!");
                return false;
            }
            // 获取应答数据
            data = read(TIMEOUT);
            if (data == -1) {
                LogUtils.d("收到 -1，可能无数据或者读取超时了。");
                return false;
            }
            if (data == SOH) {
                try {
                    // 获取包序号
                    data = read(TIMEOUT);
                    // 获取包序号的反码
                    byte _blocknumber = read(TIMEOUT);
                    //Log.d(LOG_TAG, "包序号: " + data);
                    //Log.d(LOG_TAG, "包序号的反码: " + _blocknumber);
                    // 判断包序号是否正确
                    if (data != blocknumber) {
                        LogUtils.d("包序号不正常!");
                        errorCount++;
                        continue;
                    }
                    // 判断包序号的反码是否正确
                    if (data + _blocknumber != (byte) 255) {
                        LogUtils.d("包序号的反码不正常!");
                        errorCount++;
                        continue;
                    }
                    // 获取数据
                    for (int i = 0; i < mBlockSize; i++) {
                        sector[i] = read(TIMEOUT);
                    }
                    //Log.d(LOG_TAG, "获取到的数据: " + HexUtil.toHexString(sector));
                    // 获取校验和
                    checkSum = read(TIMEOUT);
                    //Log.d(LOG_TAG, "接收到的校验和: " + checkSum);
                    // 判断校验和是否正确
                    int crc = SystemUtils.calcChecksum(sector, false);
                    //Log.d(LOG_TAG, "计算到的校验和: " + crc);
                    if (crc != checkSum) {
                        LogUtils.d("包数据的校验不正常!");
                        errorCount++;
                        continue;
                    }
                    //Log.d(LOG_TAG, "接收一帧完成!");
                    // 发送应答
                    write(mACK, TIMEOUT);
                    // 包序号自增
                    blocknumber++;
                    // 将数据写入本地
                    target.write(sector);
                    // 错误包数归零
                    errorCount = 0;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 如果出错发送重传标识
                    if (errorCount != 0) {
                        Log.d(LOG_TAG, "错误，将发送重传标志!");
                        write(mNAK, TIMEOUT);
                    }
                }
            } else if (data == mEOT) {
                LogUtils.d("收到0x04 EOT码，结束传输!");
                write(mACK, TIMEOUT);
                return true;
            } else {
                LogUtils.d("未知回复: " + HexUtil.toHexString(data));
                return false;
            }
        }
    }
}
