package com.proxgrind.chameleon.packets;

import com.proxgrind.chameleon.exceptions.CMDInvalidException;
import com.proxgrind.chameleon.exceptions.DataInvalidException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The data package of Chameleon Mini and Chameleon Tiny Pro
 * Protocol layout
 * ---------------------------------------------------------------------------------------------
 * START HEAD - CMD - DATA LENGTH - STATUS - DATA
 * e.g(en):
 * 1、START HEAD is final, 1byte and value is 0XA5 {@link #SIGN_HEX}
 * 2、CMD is identifies the type of command currently to be executed,
 * The main function of Bluetooth is to forward serial data，
 * for scalability, we define the forwarding of serial data as a command type {@link #TEXT_HEX}
 * 3、DATA LENGTH is your data length, if no data, length final to 0x00
 * 4、STATUS is all data + head checksum
 * 5、DATA is your data
 * *********************************************************************************************
 * e.g(zh)
 * 第一个字节固定0xA5，非此开头的数据将被忽略，
 * 第二个字节为命令，字符串发送固定为0x75，
 * 第三个字节命令长度是数据的，没数据则填00，
 * 校验和，整个结构包头加数据按字节累加为00。
 * ---------------------------------------------------------------------------------------------
 *
 * @author DXL
 * @version 1.0
 */
public class DataPackets {

    // Data sign
    public static final int SIGN_HEX = 0xA5; // 起始头，包头必须有!
    // Serial data forwarding flag
    public static final int TEXT_HEX = 0x75; // 文本格式命令的标志!
    // Auto add carriage return to end!
    private volatile boolean autoCRLF = true;
    // raw data
    private byte[] raw;
    // cmd type
    private int cmd = -1;

    public enum Mode {
        DOPACK,
        UNPACK,
        AUTO
    }

    private Mode mode = Mode.AUTO;

    /**
     * Build a simple command package (without data)
     * you can't build a {@link #TEXT_HEX} with null data
     * You can only build a simple access to information, such as battery packets.
     *
     * @param cmd a cmd byte
     */
    public DataPackets(int cmd) throws DataInvalidException, CMDInvalidException {
        if (cmd == TEXT_HEX)
            throwSerialDataNullException();
        if (cmd == -1)
            throw new CMDInvalidException("The cmd cannot null.");
        this.cmd = cmd;
    }

    /**
     * Build or parse automatically according to the given data
     * if we can parse, the next actions are all parse actions.
     * if first action not parse, will auto goto dopack with data to serial data.
     *
     * @param raw The data from boxed serial to parse,
     *            or to build a new serial data.
     */
    public DataPackets(byte[] raw) throws DataInvalidException {
        checkLength(raw, -1);
        this.raw = raw;
    }

    /**
     *
     */
    public DataPackets(int cmd, byte[] data) throws DataInvalidException {
        if (cmd == TEXT_HEX && data == null)
            throwSerialDataNullException();
        this.cmd = cmd;
        this.raw = data;
    }

    /**
     * get finally data bytes
     *
     * @return data
     */
    public byte[] getData() {
        // data of self build or data parse
        byte[] data;
        switch (mode) {
            case AUTO:
            default:
                if (raw == null) {
                    data = dopack(null, cmd);
                } else if (cmd == -1) {
                    try {
                        data = unpack2Bytes();
                    } catch (DataInvalidException die) {
                        data = dopack(raw, TEXT_HEX);
                    }
                } else {
                    data = dopack(raw, cmd);
                }
                break;

            case DOPACK:
                if (cmd == -1) cmd = TEXT_HEX;
                data = dopack(raw, cmd);
                break;

            case UNPACK:
                data = unpack2Bytes();
        }
        return data;
    }

    /**
     * fast build a packet without data.
     *
     * @param cmd cmd type
     * @return Generated data
     */
    public static byte[] getData(int cmd) {
        return new DataPackets(cmd).getData();
    }

    /**
     * Automatically judge whether to parse or build a serial port data according to the given data
     *
     * @param raw The data will be to parse or build a serial port data packet
     *            Is no add CRLF on end.
     * @return result data bytes
     */
    public static byte[] getData(byte[] raw, boolean isDoPack) {
        return new DataPackets(raw)
                .setMode(isDoPack ? Mode.DOPACK : Mode.UNPACK)
                .getData();
    }

    /**
     * Data box!
     *
     * @param source Data bytes, will be packed into the tail
     * @param cmd    Command flag. There are many commands.
     * @return Data after packing!
     */
    protected byte[] dopack(byte[] source, int cmd) throws DataInvalidException {
        if (source == null) source = new byte[0];
        // 第一步检查数据
        checkLength(source, -1);
        // 第二部构建一个包头!
        DataHead head = new DataHead();
        // 包头默认0xA5，我们跳过设置引导码，直接设置命令!
        head.setCommand((byte) cmd);
        // 判断命令类型，拼接回车换行!
        if (cmd == TEXT_HEX && autoCRLF)
            source = bytesMerge(source, new byte[]{0x0d, 0x0a});
        //拼接最终的数据长度!
        head.setLength((byte) source.length);
        // 最后计算校验和(包头 + 数据)
        byte checkSum = calcChecksum(bytesMerge(head.getHead(), source), false);
        // System.out.println("最终校验值: " + HexUtil.toHexString((byte) (0x00 - checkSum)));
        head.setStatus((byte) (0x00 - checkSum));
        // 返回包头与数据的拼接
        return bytesMerge(head.getHead(), source);
    }

    /**
     * Unpack data packets!
     *
     * @param source Data bytes, will be parsed!
     * @return Unpacked data encapsulation object!
     * @see DataBean
     */
    protected DataBean[] unpack(byte[] source) throws DataInvalidException {
        // 最少也要有一个包头的长度!
        checkLength(source, 4);
        // 存放切割结果的数列!
        ArrayList<DataBean> retList = new ArrayList<>();
        for (int i = 0; i < source.length; ) {
            byte sign = source[i];
            // 如果第一个字节都不是包头规范的，
            // 那么接下来的解析可能都是无效的，那就没啥意义了!
            if (sign != (byte) SIGN_HEX) {
                throw new DataInvalidException("The DataHead sign is invalid!");
            }
            // 包头正确，那么我们可以继续获得当前的命令类型!
            DataHead head = new DataHead();
            head.setCommand(source[i + 1]);
            int len = source[i + 2];
            // 判断一下命令长度的正确性!
            if (source.length - (i + 4) < len) {
                throw new DataInvalidException("The Data length is too small");
            }
            head.setLength(source[i + 2]);
            head.setStatus(source[i + 3]);
            // 创建封包!
            DataBean bean = new DataBean();
            bean.setHead(head);
            // 截取真正的数据体!
            bean.setSource(Arrays.copyOfRange(source, i + 4, len + i + 4));
            // 计算一下校验值!
            byte chekcSum = calcChecksub(
                    // 传入一个校验值，这个校验值是我们解包得到的!
                    head.getStatus(),
                    // 传入一个被合并的字节，我们反校验不包含已有的校验值!
                    bytesMerge(
                            new byte[]{head.getSign(), head.getCommand(), head.getLength()},
                            bean.getSource()
                    ), false);
            //System.out.println("反校验结果: " + HexUtil.toHexString(chekcSum));
            if (chekcSum != 0x00) throw new DataInvalidException("The data cheksum is invalid!");
            //添加进结果集中!
            retList.add(bean);
            // 指针移动，使for能判断当前是否有第二个数据!
            i += (4 + len);
            //System.out.println("解析成功: " + retList.size());
        }
        return retList.toArray(new DataBean[0]);
    }

    /**
     * Check the data length in different scenarios!
     * No matter in any scene, the length cannot be greater than 244, that is, a frame of ble!
     *
     * @param source Data source to be checked!
     * @param limit  The unit of the minimum limit length, including its own value!
     * @throws DataInvalidException Throw this exception when the data frame is not available!
     */
    private void checkLength(byte[] source, int limit) throws DataInvalidException {
        if (source == null)
            throw new DataInvalidException("The source byte array is null!");
        if (source.length < limit && source.length > 244)
            throw new DataInvalidException("The source byte length invalid!");
    }

    /**
     * Throw an exception with null serial data
     */
    private void throwSerialDataNullException() throws DataInvalidException {
        throw new DataInvalidException("To build a serial port to forward packets, data must be nonnull.");
    }

    /**
     * Is auto add carriage return to end!
     * {@link #setAutoCRLF(boolean)}
     */
    public boolean isAutoCRLF() {
        return autoCRLF;
    }

    /**
     * Auto add carriage return to end!
     *
     * @param autoCRLF if true, CRLF will auto add to data end.
     *                 if false, no add.
     */
    public DataPackets setAutoCRLF(boolean autoCRLF) {
        this.autoCRLF = autoCRLF;
        return this;
    }

    private byte[] unpack2Bytes() {
        // data bean from raw data parse
        DataBean[] dataBeans = unpack(raw);
        // try to merge bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (DataBean bean : dataBeans) {
            if (bean != null) {
                byte[] source = bean.getSource();
                if (source != null && source.length > 0) {
                    try {
                        outputStream.write(source);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return outputStream.toByteArray();
    }

    public Mode getMode() {
        return mode;
    }

    public DataPackets setMode(Mode mode) {
        this.mode = mode;
        return this;
    }

    public byte[] bytesMerge(byte[]... arrs) {
        byte[] ret = new byte[byteArraysLength(arrs)];
        int pos = 0;
        for (byte[] tmp : arrs) {
            if (tmp != null && tmp.length > 0) {
                System.arraycopy(tmp, 0, ret, pos, tmp.length);
                pos += tmp.length;
            }
        }
        return ret;
    }

    public int byteArraysLength(byte[]... arrs) {
        int ret = 0;
        for (byte[] tmp : arrs) {
            if (tmp != null)
                ret += tmp.length;
        }
        return ret;
    }

    /**
     * Calculates the checksum of the passed byte buffer.
     *
     * @param buffer
     * @return byte checksum value
     */
    public static byte calcChecksub(byte checkSum, byte[] buffer, boolean sub) {
        if (buffer == null) return 0;
        byte checksum = checkSum;
        int bufPos = 0;
        int byteCount = buffer.length;
        while (byteCount-- != 0) {
            byte b = buffer[bufPos++];
            byte tmp = checksum;
            if (!sub)
                checksum += b;
            else
                checksum -= b;
        }
        return checksum;
    }

    /**
     * Calculates the checksum of the passed byte buffer.
     *
     * @param buffer
     * @return byte checksum value
     */
    public static byte calcChecksum(byte[] buffer, boolean sub) {
        if (buffer == null) return 0;
        byte checksum = 0;
        int bufPos = 0;
        int byteCount = buffer.length;
        while (byteCount-- != 0) {
            byte b = buffer[bufPos++];
            if (!sub)
                checksum += b;
            else
                checksum -= b;
        }
        return checksum;
    }
}
