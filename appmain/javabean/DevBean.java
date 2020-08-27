package com.proxgrind.chameleon.javabean;

import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * Created by DXL on 2017/11/14.
 */
public class DevBean implements Serializable {

    private String devName = "";
    private String macAddress = "";
    private Object object;

    public DevBean(String name, String addr) {
        if (name != null)
            this.devName = name;
        if (addr != null)
            this.macAddress = addr;
    }

    public DevBean(String devName, String macAddress, Object object) {
        this.devName = devName;
        this.macAddress = macAddress;
        this.object = object;
    }

    public String getDevName() {
        return devName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    @Override
    public String toString() {
        return "DevBean{" +
                "devName='" + devName + '\'' +
                ", macAddress='" + macAddress + '\'' +
                ", object=" + object +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        if (obj instanceof DevBean) {
            return (((DevBean) obj).getMacAddress()
                    .equals(getMacAddress()));
        }
        return false;
    }
}
