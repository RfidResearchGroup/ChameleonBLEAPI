package com.proxgrind.chameleon.packets;

public class DataBean {
    private DataHead head;
    private byte[] source;

    public DataHead getHead() {
        return head;
    }

    public DataBean setHead(DataHead head) {
        this.head = head;
        return this;
    }

    public byte[] getSource() {
        return source;
    }

    public DataBean setSource(byte[] source) {
        this.source = source;
        return this;
    }
}
