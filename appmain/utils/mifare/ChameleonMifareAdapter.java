package com.proxgrind.chameleon.utils.mifare;

import android.nfc.tech.MifareClassic;

import com.proxgrind.chameleon.packets.DataPackets;
import com.proxgrind.chameleon.utils.system.LogUtils;
import com.proxgrind.chameleon.utils.tools.HexUtil;

import java.io.IOException;
import java.util.Arrays;

public class ChameleonMifareAdapter implements MifareAdapter {
    private ChameleonBatchAdapterImpl batchAdapter = new ChameleonBatchAdapterImpl();
    private byte[] uid;
    private byte[] atqa;
    private int sak = -1;
    private byte[] ats;

    @Override
    public boolean rescantag() throws IOException {
        // 组包!
        byte[] dataOfParam = HexUtil.bytesMerge(
                batchAdapter.getType(0x00), // 置入类型! 04
                ChameleonBatchAdapterImpl.EMPTY_DATA_ONE, // 置入状态码! 00
                new byte[10], // 卡号
                new byte[1], // 卡号长度
                new byte[2], // ATQA
                new byte[1], // SAK
                new byte[1] // ATS长度
        );
        byte[] dataOfFinal = new DataPackets(0x72, dataOfParam).getData();
        byte[] respDatas = batchAdapter.sendAndReadResponse(dataOfFinal, dataOfFinal.length, 0);
        // byte[] respDatas = HexUtil.hexStringToByteArray("0001505C8E04000000000000040400080B12b12b12b12b12b121b2121b12b12b1212b1bb32513b513513b5131b4141b1");
        if (respDatas != null && respDatas.length > 2) {
            // 得到最终的秘钥索引，并且进行下标 -1 的内容返回!
            byte status = respDatas[1];
            // 只有当索引大于1的时候，才是真正的有应答，当应答FF或者0的时候，则是卡片失联了
            batchAdapter.checkTagStatus(status);
            if (status > 0) {
                // 我们需要进行信息截取!
                if (respDatas.length >= 13) { // 可以截取有效的UID!
                    uid = Arrays.copyOfRange(respDatas, 2, 12);
                    uid = Arrays.copyOf(uid, respDatas[12]);
                    LogUtils.d("UID: " + HexUtil.toHexString(uid));
                }
                if (respDatas.length >= 15) { // 可以截取有效的ATQA
                    atqa = Arrays.copyOfRange(respDatas, 13, 15);
                    LogUtils.d("ATQA: " + HexUtil.toHexString(atqa));
                }
                if (respDatas.length >= 16) { // 可以截取有效的SAK
                    sak = Arrays.copyOfRange(respDatas, 15, 16)[0];
                    LogUtils.d("SAK: " + sak);
                }
                int atsLen = 0;
                if (respDatas.length >= 17) {
                    atsLen = Arrays.copyOfRange(respDatas, 16, 17)[0];
                }
                LogUtils.d("ATS长度: " + atsLen);
                if (atsLen > 0 && respDatas.length - 17 >= atsLen) {
                    ats = Arrays.copyOfRange(respDatas, 17, 17 + atsLen);
                    LogUtils.d("ATS: " + HexUtil.toHexString(ats));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean connect() throws IOException {
        return rescantag();
    }

    @Override
    public void close() throws IOException {
        // 不需要做任何操作!
    }

    @Override
    public byte[] read(int block) throws IOException {
        throw new IOException("Unsupported operation.");
    }

    @Override
    public boolean write(int blockIndex, byte[] data) throws IOException {
        throw new IOException("Unsupported operation.");
    }

    @Override
    public boolean authA(int sectorIndex, byte[] key) throws IOException {
        throw new IOException("Unsupported operation.");
    }

    @Override
    public boolean authB(int sectorIndex, byte[] key) throws IOException {
        throw new IOException("Unsupported operation.");
    }

    @Override
    public void increment(int blockIndex, int value) throws IOException {
        throw new IOException("Unsupported operation.");
    }

    @Override
    public void decrement(int blockIndex, int value) throws IOException {
        throw new IOException("Unsupported operation.");
    }

    @Override
    public void restore(int blockIndex) throws IOException {
        throw new IOException("Unsupported operation.");
    }

    @Override
    public void transfer(int blockIndex) throws IOException {
        throw new IOException("Unsupported operation.");
    }

    @Override
    public byte[] getUid() {
        try {
            if (uid == null)
                rescantag();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uid;
    }

    @Override
    public byte[] getAts() {
        if (ats == null) {
            try {
                rescantag();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ats;
    }

    @Override
    public byte[] getAtqa() {
        if (atqa == null) {
            try {
                rescantag();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return atqa;
    }

    @Override
    public byte[] getSak() {
        if (sak == -1) {
            try {
                rescantag();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new byte[]{(byte) sak};
    }

    @Override
    public int getType() {
        try {
            if (sak == -1)
                rescantag();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        switch (sak) {
            case 0x01:
            case 0x08:
            case 0x09:
                // Seclevel = SL2
            case 0x18:
            case 0x19:
            case 0x28:
                //mIsEmulated = true;
            case 0x38:
            case 0x88:
                return MifareClassic.TYPE_CLASSIC;
            case 0x10:
                // SecLevel = SL2
            case 0x11:
                return MifareClassic.TYPE_PLUS;
            // NXP-tag: false
            case 0x98:
            case 0xB8:
                return MifareClassic.TYPE_PRO;
            default:
                // Stack incorrectly reported a MifareClassic. We cannot handle this
                // gracefully - we have no idea of the memory layout. Bail.
                return -1;
        }
    }

    @Override
    public int getSectorCount() {
        switch (getType()) {
            case 0:
                return 16;
            case 1:
                return 32;
            case 2:
                return 40;
        }
        return 0;
    }

    @Override
    public int getBlockCount() {
        switch (getType()) {
            case 0:
                return MifareClassic.SIZE_1K / MifareClassic.BLOCK_SIZE;
            case 1:
                return MifareClassic.SIZE_2K / MifareClassic.BLOCK_SIZE;
            case 2:
                return MifareClassic.SIZE_4K / MifareClassic.BLOCK_SIZE;
        }
        return 0;
    }

    @Override
    public void setTimeout(int ms) {
    }

    @Override
    public int getTimeout() {
        return 2333;
    }

    @Override
    public BatchAdapter getBatchImpl() {
        return batchAdapter;
    }

    @Override
    public boolean isConnected() {
        try {
            return rescantag();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean isEmulated() {
        return false;
    }

    @Override
    public boolean isSpecialTag() {
        return false;
    }

    @Override
    public boolean isTestSupported() {
        return false;
    }
}
