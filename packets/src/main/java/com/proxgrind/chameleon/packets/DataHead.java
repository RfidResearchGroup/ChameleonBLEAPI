package com.proxgrind.chameleon.packets;

/*
 * Data Head!
 * */
public class DataHead {
    private byte sign = (byte) DataPackets.SIGN_HEX; // 头部标志
    private byte command; // 命令标志
    private byte length; // 命令长度
    private byte status; // 命令状态

    public byte getSign() {
        return sign;
    }

    public DataHead setSign(byte sign) {
        this.sign = sign;
        return this;
    }

    public byte getCommand() {
        return command;
    }

    public DataHead setCommand(byte command) {
        this.command = command;
        return this;
    }

    public byte getLength() {
        return length;
    }

    public DataHead setLength(byte length) {
        this.length = length;
        return this;
    }

    public byte getStatus() {
        return status;
    }

    public DataHead setStatus(byte status) {
        this.status = status;
        return this;
    }

    public byte[] getHead() {
        return new byte[]{sign, command, length, status};
    }

    @Override
    public String toString() {
        return "DataHead{" +
                "sign=" + sign +
                ", command=" + command +
                ", length=" + length +
                ", status=" + status +
                '}';
    }
}
