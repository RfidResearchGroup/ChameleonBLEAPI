package com.proxgrind.chameleon.utils.device.ble;

import androidx.annotation.NonNull;

import com.proxgrind.chameleon.utils.system.LogUtils;
import com.proxgrind.chameleon.utils.tools.ArrayUtils;
import com.proxgrind.chameleon.utils.tools.HexUtil;

import java.util.ArrayList;

public class BLERawUtils {
    // 解析!
    public static Details[] parse(byte[] raw) {
        ArrayList<Details> details = new ArrayList<>(3);
        // 02 01 02
        // 02 0A 04
        // 03 02 F0 FF
        // 0C FF 5A 69 63 6F 78 DC 0D 30 02 74 CC
        // 06 09 58 54 34 32 33
        // 0000000000000000000000000000000000000000000000000000000000000000
        for (int i = 0; i < raw.length; ) {
            // 分三段取长度字节!
            int len = raw[i];
            // 必定超过一个字节!
            if (len > 1) {
                // 足够一帧数据!
                if (len < raw.length + i) {
                    // 建立存储对象!
                    Details tmp = new Details();
                    tmp.setLength(len);
                    tmp.setType(raw[i + 1]);
                    // 建立一个存放值字节的数组，长度减去类型可以得到有效的值长度!
                    byte[] bytes = new byte[len - 1];
                    // 进行值拷贝!
                    try {
                        System.arraycopy(raw, i + 2, bytes, 0, bytes.length);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // 存放进对象中!
                    tmp.setValue(bytes);
                    // 添加到结果集!
                    details.add(tmp);
                    i += (len + 1);
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        return ArrayUtils.list2Arr(details);
    }

    // 按照某个类型搜索值
    public static Details find(int typeByte, Details[] details) {
        if (details != null) {
            for (Details tmp : details) {
                /*LogUtils.d("len: "
                        + HexUtil.toHexString(tmp.getLength()) + ", "
                        + "type: " + HexUtil.toHexString(tmp.getType()) + ", "
                        + "value: " + HexUtil.toHexString(tmp.getValue()) + ", "
                        + "str: " + new String(tmp.getValue())
                );*/
                if (tmp.getType() == typeByte) {
                    return tmp;
                }
            }
        }
        return null;
    }

    // 直接获得某个类型的值!
    public static Details find(int typeByte, byte[] raw) {
        // 进行raw分包查询!
        BLERawUtils.Details[] details = BLERawUtils.parse(raw);
        return BLERawUtils.find(typeByte, details);
    }

    // 存放详细信息的类!
    public static class Details {
        public int getLength() {
            return length;
        }

        private void setLength(int length) {
            this.length = length;
        }

        public int getType() {
            return type;
        }

        private void setType(int type) {
            this.type = type;
        }

        public byte[] getValue() {
            return value;
        }

        private void setValue(byte[] value) {
            this.value = value;
        }

        int length;
        int type;
        byte[] value;

        @NonNull
        @Override
        public String toString() {
            return "[Type=" + type + "]" + "[Value=]" + HexUtil.toHexString(value) + "]" + "[Length=" + length + "]";
        }
    }
}
