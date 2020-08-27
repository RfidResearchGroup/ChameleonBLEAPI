package com.proxgrind.chameleon.utils.mifare;

import com.proxgrind.chameleon.packets.DataPackets;
import com.proxgrind.chameleon.utils.system.LogUtils;
import com.proxgrind.chameleon.utils.tools.HexUtil;
import com.proxgrind.devices.BleSerialControl;

import java.io.IOException;
import java.util.Arrays;

public class ChameleonBatchAdapterImpl implements BatchAdapter {
    // 持有一个原生通讯的句柄!
    private BleSerialControl control = BleSerialControl.get();
    // 一个空的数组!
    public static final byte[] EMPTY_DATA_ONE = new byte[]{0x00};

    /**
     * 读取函数，变色龙的读取实现，依赖起始块
     *
     * @param startBlock   起始块，如果当前的模式是读取单块模式，则此参数是需要读取的块
     * @param isKeyA       当前是否是用keyA来验证并且读取的
     * @param key          当前被用来读取的秘钥
     * @param isReadSector 当前是否是读取全部扇区模式，注意，如果是读取扇区模式，
     *                     遇到4K的大扇区的情况下，需要进行每次4个块的读取，也就是
     *                     从startBlock开始表示读取第一个大块
     *                     从startBlock + 4 开始表示读取第二个大块
     *                     从startBlock + 8 开始表示读取第三个大块
     *                     从startBlock + 12 开始表示读取第四个大块
     */
    @Override
    public byte[][] read(int startBlock, boolean isKeyA, byte[] key, boolean isReadSector) throws IOException {
        // 组包!
        byte[] dataOfParam = HexUtil.bytesMerge(
                getType(isReadSector ? 2 : 1), // 置入类型，如果当前是读取扇区，则设置操作标志为2，否则设置为1
                EMPTY_DATA_ONE, // 置入状态码!
                new byte[]{(byte) startBlock}, // 置入起始块!
                isKeyA(isKeyA), // 置入当前验证的秘钥类型!
                key, // 置入秘钥!
                isReadSector ? new byte[16 * 4] : new byte[16] // 置入空字节
        );
        byte[] dataOfFinal = new DataPackets(0x72, dataOfParam).getData();
        LogUtils.d("发送的数据长度: " + dataOfParam.length);
        // 发送数据!
        byte[] respDatas = sendAndReadResponse(dataOfFinal, dataOfFinal.length, dataOfParam.length);
        if (respDatas != null && respDatas.length == dataOfParam.length) {
            // 得到最终的秘钥索引，并且进行下标 -1 的内容返回!
            byte status = respDatas[1];
            checkTagStatus(status);
            if (status == 0) {
                LogUtils.d("读取失败");
                return null;
            }
            if (status == 1) {
                return HexUtil.splitBytes(
                        Arrays.copyOfRange(respDatas, 10, respDatas.length),
                        16
                );
            }
        }
        return null;
    }

    @Override
    public boolean write(int sector, boolean isKeyA, byte[] key, byte[] dataGroup) throws IOException {
        // 组包!
        byte[] dataOfParam = HexUtil.bytesMerge(
                getType(3), // 置入类型!
                EMPTY_DATA_ONE, // 置入状态码!
                new byte[]{(byte) sector}, // 置入扇区!
                isKeyA(isKeyA), // 置入当前验证的秘钥类型!
                key, // 置入秘钥!
                dataGroup // 置入数据!
        );
        byte[] dataOfFinal = new DataPackets(0x72, dataOfParam).getData();
        // 发送数据!
        byte[] respDatas = sendAndReadResponse(dataOfFinal, dataOfFinal.length, dataOfParam.length);
        if (respDatas != null && respDatas.length == dataOfParam.length) {
            // 得到最终的秘钥索引，并且进行下标 -1 的内容返回!
            byte status = respDatas[1];
            checkTagStatus(status);
            if (status == 0) {
                LogUtils.d("写入失败");
                return false;
            }
            return status == 1;
        }
        return false;
    }

    /**
     * 经过测试，以35个秘钥为一组进行验证比较好!
     */
    @Override
    public byte[] verity(int sector, byte[][] keysGroup, boolean isKeyA) throws IOException {
        // 组包!
        byte[] dataOfParam = HexUtil.bytesMerge(
                getType(0x04), // 置入类型! 04
                EMPTY_DATA_ONE, // 置入状态码! 00
                getBlock(sector), // 置入扇区! ~3F
                isKeyA(isKeyA), // 置入当前验证的秘钥类型! 00
                getKeyCount(keysGroup), // 获得当前的秘钥总数!
                HexUtil.bytesMerge(keysGroup) // 合并秘钥!
        );
        byte[] dataOfFinal = new DataPackets(0x72, dataOfParam).getData();
        byte[] respDatas = sendAndReadResponse(dataOfFinal, dataOfFinal.length, 11);
        if (respDatas != null && respDatas.length == 11) {
            // 得到最终的秘钥索引，并且进行下标 -1 的内容返回!
            byte index = respDatas[1];
            checkTagStatus(index);
            // 只有当索引大于1的时候，才是真正的有应答，当应答FF的时候，则是卡片失联了
            if (index > 0 && index <= keysGroup.length) {
                return keysGroup[index - 1];
            }
        }
        return null;
    }

    public void checkTagStatus(int statusCode) throws IOException {
        if (statusCode == 0xFF || statusCode == -1) {
            throw new IOException("Tag lost.");
        }
    }

    public byte[] sendAndReadResponse(byte[] data, int dataLength, int acceptRespLength) throws IOException {
        // 发送数据!
        int timeout = getTimeout();
        // 刷新缓冲区且发送
        control.flush();
        int maxLen = BleSerialControl.MTU;
        if (control.write(data, 0, dataLength, timeout) == dataLength) {
            // 读取应答数据
            byte[] responseBuffer = new byte[acceptRespLength <= 0 ? maxLen : acceptRespLength];
            // 直接读取
            control.read(
                    responseBuffer,
                    0,
                    Math.max(acceptRespLength, 0),
                    timeout
            );
            return responseBuffer;
        }
        return null;
    }

    /**
     * 获取byte数组型的类型，分别可能是
     * 0 读取
     * 1 写入
     * 2 验证
     */
    public byte[] getType(int type) {
        return new byte[]{(byte) type};
    }

    /**
     * 将单个字节的sector转为数组！
     */
    public byte[] getBlock(int sector) {
        return new byte[]{(byte) MfDataUtils.get_trailer_block(MfDataUtils.sectorToBlock(sector))};
    }

    public byte[] isKeyA(boolean isKeyA) {
        return new byte[]{(byte) (isKeyA ? 0 : 1)};
    }

    public byte[] getKeyCount(byte[][] keys) {
        return new byte[]{(byte) keys.length};
    }

    @Override
    public int getTimeout() {
        return 2333;
    }

    @Override
    public void setTimeout(int timeout) {

    }
}
