package com.proxgrind.chameleon.utils.mifare;

import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Build;

import com.proxgrind.chameleon.utils.stream.IOUtils;
import com.proxgrind.chameleon.utils.system.LogUtils;
import com.proxgrind.chameleon.utils.system.SystemUtils;
import com.proxgrind.chameleon.utils.tools.GlobalTag;

import java.io.File;
import java.io.IOException;

public class StdMifareImpl implements MifareAdapter, GlobalTag.OnNewTagListener {
    private static MifareClassic mMfTag = null;
    private final static StdMifareImpl stdMifareImpl = new StdMifareImpl();

    /*
     * 此方法需要传入一个TAG，这个TAG将被用来初始化一个MifareClassic标签!
     * */
    private StdMifareImpl() {
        mMfTag = getMfOfGlobalTag();
        GlobalTag.addListener(this);
    }

    public static StdMifareImpl getInstance() {
        synchronized (stdMifareImpl) {
            mMfTag = getMfOfGlobalTag();
        }
        return stdMifareImpl;
    }

    private static MifareClassic getMfOfGlobalTag() {
        if (mMfTag != null) IOUtils.close(mMfTag);
        Tag tag = GlobalTag.getTag();
        if (tag != null) {
            return MifareClassic.get(tag);
        }
        return null;
    }

    /**
     * Check if the device supports the MIFARE Classic technology.
     * In order to do so, there is a first check ensure the device actually has a NFC hardware
     * After this, this function will check if there are files
     * like "/dev/bcm2079x-i2c" or "/system/lib/libnfc-bcrm*". Files like
     * these are indicators for a NFC controller manufactured by Broadcom.
     * Broadcom chips don't support MIFARE Classic.
     *
     * @return True if the device supports MIFARE Classic. False otherwise.
     */
    public static boolean hasMifareClassicSupport(Context context) {

        // Check for the MifareClassic class.
        // It is most likely there on all NFC enabled phones.
        // Therefore this check is not needed.
        /*
        try {
            Class.forName("android.nfc.tech.MifareClassic");
        } catch( ClassNotFoundException e ) {
            // Class not found. Devices does not support MIFARE Classic.
            return false;
        }
        */

        // Check if ther is any NFC hardware at all.
        if (NfcAdapter.getDefaultAdapter(context) == null) {
            return false;
        }

        // Check if there is the NFC device "bcm2079x-i2c".
        // Chips by Broadcom don't support MIFARE Classic.
        // This could fail because on a lot of devices apps don't have
        // the sufficient permissions.
        // Another exception:
        // The Lenovo P2 has a device at "/dev/bcm2079x-i2c" but is still
        // able of reading/writing MIFARE Classic tags. I don't know why...
        // https://github.com/ikarus23/MifareClassicTool/issues/152
        boolean isLenovoP2 = Build.MANUFACTURER.equals("LENOVO")
                && Build.MODEL.equals("Lenovo P2a42");
        File device = new File("/dev/bcm2079x-i2c");
        if (!isLenovoP2 && device.exists()) {
            return false;
        }

        // Check if there is the NFC device "pn544".
        // The PN544 NFC chip is manufactured by NXP.
        // Chips by NXP support MIFARE Classic.
        device = new File("/dev/pn544");
        if (device.exists()) {
            return true;
        }

        // Check if there are NFC libs with "brcm" in their names.
        // "brcm" libs are for devices with Broadcom chips. Broadcom chips
        // don't support MIFARE Classic.
        File libsFolder = new File("/system/lib");
        File[] libs = libsFolder.listFiles();
        if (libs != null) {
            for (File lib : libs) {
                if (lib.isFile()
                        && lib.getName().startsWith("libnfc")
                        && lib.getName().contains("brcm")
                    // Add here other non NXP NFC libraries.
                ) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    public static boolean isNfcOpened(Context context) {
        if (hasMifareClassicSupport(context)) {
            //判断设备是否支持NFC！
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);
            // 可能出现adapter为空的情况!
            if (adapter != null) {
                return adapter.isEnabled();
            } else {
                LogUtils.d("isNfcOpened：adapter is null. ");
            }
        }
        LogUtils.d("isNfcOpened：MifareClassic unsupported. ");
        return false;
    }

    public MifareClassic getMf() {
        return mMfTag;
    }

    @Override
    public boolean rescantag() throws IOException {
        Tag tag = GlobalTag.getTag();
        if (mMfTag != null) mMfTag.close();
        if (tag != null) {
            mMfTag = MifareClassic.get(tag);
        }
        return mMfTag != null;
    }

    @Override
    public boolean connect() throws IOException {
        if (mMfTag.isConnected()) mMfTag.close();
        if (mMfTag != null && !isConnected()) {
            mMfTag.connect();
            return true;
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        if (mMfTag != null)
            if (isConnected())
                mMfTag.close();
    }

    @Override
    public byte[] read(int block) throws IOException {
        if (mMfTag != null)
            return mMfTag.readBlock(block);
        return null;
    }

    @Override
    public boolean write(int blockIndex, byte[] data) throws IOException {
        if (mMfTag != null) {
            mMfTag.writeBlock(blockIndex, data);
            return true;
        }
        return false;
    }

    @Override
    public boolean authA(int sectorIndex, byte[] key) throws IOException {
        if (mMfTag != null) {
            return mMfTag.authenticateSectorWithKeyA(sectorIndex, key);
        }
        return false;
    }

    @Override
    public boolean authB(int sectorIndex, byte[] key) throws IOException {
        if (mMfTag != null)
            return mMfTag.authenticateSectorWithKeyB(sectorIndex, key);
        return false;
    }

    @Override
    public void increment(int blockIndex, int value) throws IOException {
        if (mMfTag != null)
            mMfTag.increment(blockIndex, value);
    }

    @Override
    public void decrement(int blockIndex, int value) throws IOException {
        if (mMfTag != null)
            mMfTag.decrement(blockIndex, value);
    }

    @Override
    public void restore(int blockIndex) throws IOException {
        if (mMfTag != null)
            mMfTag.restore(blockIndex);
    }

    @Override
    public void transfer(int blockIndex) throws IOException {
        if (mMfTag != null)
            mMfTag.transfer(blockIndex);
    }

    @Override
    public byte[] getUid() {
        if (mMfTag != null)
            return mMfTag.getTag().getId();
        return null;
    }

    @Override
    public byte[] getAts() {
        return new byte[0];
    }

    @Override
    public byte[] getAtqa() {
        return new byte[0];
    }

    @Override
    public byte[] getSak() {
        return new byte[0];
    }

    @Override
    public int getType() {
        if (mMfTag != null)
            return mMfTag.getType();
        return -1;
    }

    @Override
    public int getSectorCount() {
        if (mMfTag != null)
            return mMfTag.getSectorCount();
        return -1;
    }

    @Override
    public int getBlockCount() {
        if (mMfTag != null)
            return mMfTag.getBlockCount();
        return -1;
    }

    @Override
    public void setTimeout(int ms) {
        if (mMfTag != null)
            mMfTag.setTimeout(ms);
    }

    @Override
    public int getTimeout() {
        if (mMfTag != null)
            return mMfTag.getTimeout();
        return -1;
    }

    @Override
    public BatchAdapter getBatchImpl() {
        // 标准NFC不支持批量验证，所以需要返回null
        return null;
    }

    @Override
    public boolean isConnected() {
        synchronized (stdMifareImpl) {
            if (mMfTag == null) {
                LogUtils.d("MfTag对象为空，将会直接返回NULL");
                return false;
            }
            LogUtils.d("MfTag对象不为空，将会直接返回链接状态: " + mMfTag.isConnected());
            return mMfTag.isConnected();
        }
    }

    @Override
    public boolean isEmulated() {
        return false;
    }

    @Override
    public boolean isSpecialTag() {
        //自带的NFC不支持特殊的后门标签直接读写，因此直接返回false即可!
        return false;
    }

    @Override
    public boolean isTestSupported() {
        return false;
    }

    @Override
    public void onNewTag(Tag tag) {
        synchronized (stdMifareImpl) {
            if (mMfTag != null) {
                if (mMfTag.isConnected()) IOUtils.close(mMfTag);
                mMfTag = getMfOfGlobalTag();
                LogUtils.d("StdMifareImpl收到了卡片通知，将会进行卡片更新处理!");
            }
        }
    }
}
