package com.proxgrind.chameleon.javabean;

public class DetectionLog {
    // 类型
    private int type;
    // 数据
    private byte[] data;
    // 时间戳
    private int timestamp;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
}
