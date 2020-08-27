package com.proxgrind.chameleon.utils.tools;

import com.proxgrind.chameleon.utils.stream.FileUtils;

import java.io.File;

public interface Properties {
    String APP_SDCARD = FileUtils.getAppFilesDir("files").getAbsolutePath();
    String APP_SETTINH_DIR = "settings";
    String APP_SETTINGS_NAME = "settings.ky";
    // APP的配置文件保存目录!
    String APP_CONF_PATH = APP_SDCARD + File.separator + APP_SETTINH_DIR;
    // app的通用配置文件!
    String APP_CONF_FILE = APP_CONF_PATH + File.separator + APP_SETTINGS_NAME;
    // 设备的自动关闭状态设置key!
    String APP_AUTO_CLOSE_KEY = "autoClose";
    // 设备的自动断开时间!
    String APP_AUTO_CLOSE_TIME_KEY = "autoCloseTime";
    // 新手模式的标志!
    String APP_NOVICE_MODE = "noviceMode";
    // 绑定的mac地址
    String APP_BIND_MAC = "bindMac";
    // 是否启用了MAC地址绑定!
    String APP_BIND_MAC_STATUS = "bindMacStatus";
    // 设备备注
    String APP_DEVICE_REMARKS = "device_remarks";
    // UI模式
    String APP_UI_MODE = "ui_mode";
    // DUMP模式
    String APP_DUMP_MODE = "dump_mode";
}
