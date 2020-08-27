package com.proxgrind.chameleon.javabean;

import androidx.annotation.NonNull;

public class DumpBean {
    private String time;
    private String uid;
    private int position;
    @NonNull
    private String name = "";

    private DumpBean() {
    }

    public DumpBean(@NonNull String name) {
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    @Override
    public String toString() {
        return "DumpBean{" +
                "time='" + time + '\'' +
                ", uid='" + uid + '\'' +
                ", position=" + position +
                ", name='" + name + '\'' +
                '}';
    }
}
