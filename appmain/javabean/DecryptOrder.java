package com.proxgrind.chameleon.javabean;

public class DecryptOrder {
    private String uid;
    private String[] keys;

    public DecryptOrder() {
    }

    public DecryptOrder(String uid, String[] keys) {
        this.uid = uid;
        this.keys = keys;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String[] getKeys() {
        return keys;
    }

    public void setKeys(String[] keys) {
        this.keys = keys;
    }
}
