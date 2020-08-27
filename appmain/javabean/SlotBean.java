package com.proxgrind.chameleon.javabean;

import androidx.annotation.NonNull;

import com.proxgrind.chameleon.R;
import com.proxgrind.chameleon.utils.system.AppContextUtils;

public class SlotBean {
    private int position;
    private String uid = "00000000";
    private String mode = "NONE";
    private boolean isValid = true;
    private boolean isOffline = true;
    private int size;
    private boolean isSakMode = false;

    public SlotBean(int position) {
        this.position = position;
    }

    public boolean isOffline() {
        return isOffline;
    }

    public void setOffline(boolean offline) {
        isOffline = offline;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isSakMode() {
        return isSakMode;
    }

    public void setSakMode(boolean sakMode) {
        isSakMode = sakMode;
    }

    @NonNull
    @Override
    public String toString() {
        return "SlotBean{" +
                "position=" + position +
                ", uid='" + uid + '\'' +
                ", mode='" + mode + '\'' +
                ", isValid=" + isValid +
                ", isOffline=" + isOffline +
                ", size=" + size +
                ", isSakMode=" + isSakMode +
                '}';
    }
}
