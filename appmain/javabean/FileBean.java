package com.proxgrind.chameleon.javabean;

public class FileBean {
    private boolean isFile;
    private String name;
    private String info;
    private String path;

    public FileBean() {
    }

    public FileBean(boolean isFile, String name, String path, String info) {
        this.isFile = isFile;
        this.name = name;
        this.info = info;
        this.path = path;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "FileBean{" +
                "isFile=" + isFile +
                ", name='" + name + '\'' +
                ", info='" + info + '\'' +
                ", path='" + path + '\'' +
                '}';
    }

    // 回调!
    public void onClick() {
    }
}
