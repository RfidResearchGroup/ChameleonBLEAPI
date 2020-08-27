package com.proxgrind.chameleon.javabean;

public class DeviceInfo {
    private int mainVersion;
    private int minorVersion;
    private int versionFlag;
    private String bleVersion;
    private int batteryVoltage;
    private int batteryPercent;
    private int avrMainVersion;
    private int avrMinorVersion;
    private String avrVersion;

    public int getMainVersion() {
        return mainVersion;
    }

    public void setMainVersion(int mainVersion) {
        this.mainVersion = mainVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public int getVersionFlag() {
        return versionFlag;
    }

    public void setVersionFlag(int versionFlag) {
        this.versionFlag = versionFlag;
    }

    public int getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(int batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public int getBatteryPercent() {
        return batteryPercent;
    }

    public void setBatteryPercent(int batteryPercent) {
        this.batteryPercent = batteryPercent;
    }

    public int getAvrMainVersion() {
        return avrMainVersion;
    }

    public void setAvrMainVersion(int avrMainVersion) {
        this.avrMainVersion = avrMainVersion;
    }

    public int getAvrMinorVersion() {
        return avrMinorVersion;
    }

    public void setAvrMinorVersion(int avrMinorVersion) {
        this.avrMinorVersion = avrMinorVersion;
    }

    public String getAvrVersion() {
        return avrVersion;
    }

    public void setAvrVersion(String avrVersion) {
        this.avrVersion = avrVersion;
    }

    public String getBleVersion() {
        return bleVersion;
    }

    public void setBleVersion(String bleVersion) {
        this.bleVersion = bleVersion;
    }
}
