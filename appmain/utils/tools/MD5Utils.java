package com.proxgrind.chameleon.utils.tools;

import java.security.MessageDigest;

//MD5加密不可逆
public class MD5Utils {
    /**
     * 对字节内容进行加密
     *
     * @param source 摘要对象!
     * @return 密文
     */
    public static String digest(byte[] source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexUtil.toHexString(digest.digest(source));
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    /**
     * 对字节内容进行摘要检查!
     *
     * @param digest 已经确定的摘要信息!
     * @param source 未知的摘要对象!
     * @return 摘要信息对比结果!
     */
    public static boolean verify(String digest, byte[] source) {
        return digest(source).equals(digest);
    }
}
