package com.proxgrind.chameleon.javabean;

import java.io.Serializable;
import java.util.Arrays;

public class MifareBean implements Serializable {

    //扇区号
    private int sector;
    //数据块的数据
    private String[] datas;

    public MifareBean() {
    }

    public MifareBean(int sector) {
        this.sector = sector;
    }

    public MifareBean(int sector, String[] datas) {
        this.sector = sector;
        this.datas = datas;
    }

    public int getSector() {
        return sector;
    }

    public void setSector(int sector) {
        this.sector = sector;
    }

    public String[] getDatas() {
        return datas;
    }

    public void setDatas(String[] datas) {
        this.datas = datas;
    }

    @Override
    public String toString() {
        return "M1Bean{" +
                "sector=" + sector +
                ", datas=" + Arrays.toString(datas) +
                '}';
    }
}
