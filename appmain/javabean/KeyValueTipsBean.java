package com.proxgrind.chameleon.javabean;

import android.view.View;

public class KeyValueTipsBean implements View.OnClickListener {
    private int iconId;
    private String key;
    private String value;

    public KeyValueTipsBean(int iconId, String key, String value) {
        this.iconId = iconId;
        this.key = key;
        this.value = value;
    }

    public KeyValueTipsBean() {
    }

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public void onClick(View v) {

    }
}
