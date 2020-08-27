package com.proxgrind.chameleon.javabean;

import com.proxgrind.chameleon.utils.chameleon.ChameleonUtils;

public class SlotAction {
    private String leftClick = ChameleonUtils.DEFAULT_BTN_DEFAULT[6];
    private String rightClick = ChameleonUtils.DEFAULT_BTN_DEFAULT[6];
    private String leftLongClick = ChameleonUtils.DEFAULT_BTN_DEFAULT[6];
    private String rightLongClick = ChameleonUtils.DEFAULT_BTN_DEFAULT[6];
    private boolean isUidMode;

    public String getRightClick() {
        return rightClick;
    }

    public void setRightClick(String rightClick) {
        this.rightClick = rightClick;
    }

    public String getLeftClick() {
        return leftClick;
    }

    public void setLeftClick(String leftClick) {
        this.leftClick = leftClick;
    }

    public String getLeftLongClick() {
        return leftLongClick;
    }

    public void setLeftLongClick(String leftLongClick) {
        this.leftLongClick = leftLongClick;
    }

    public String getRightLongClick() {
        return rightLongClick;
    }

    public void setRightLongClick(String rightLongClick) {
        this.rightLongClick = rightLongClick;
    }

    public boolean isUidMode() {
        return isUidMode;
    }

    public void setUidMode(boolean uidMode) {
        isUidMode = uidMode;
    }
}
