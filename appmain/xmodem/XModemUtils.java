package com.proxgrind.chameleon.xmodem;

import com.proxgrind.chameleon.posixio.PosixCom;
import com.proxgrind.chameleon.utils.chameleon.ChameleonUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class XModemUtils {
    public static byte[] recv128(PosixCom com) {
        if (com == null) return new byte[0];
        XModem128 x128 = new XModem128(com);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(64);
        try {
            ChameleonUtils.autoCRLF = false;
            x128.recv(outputStream);
            ChameleonUtils.autoCRLF = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    public static boolean send128(PosixCom com, byte[] datas) {
        if (com == null || datas == null) return false;
        XModem128 x128 = new XModem128(com);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(datas);
        try {
            boolean ret;
            ChameleonUtils.autoCRLF = false;
            ret = x128.send(inputStream);
            ChameleonUtils.autoCRLF = true;
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
