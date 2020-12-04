package com.yuanxu.ecg.callback;

public interface DeviceTimeCallback extends BaseCallback {
    /**
     * 设备时间，格式yy-MM--dd HH:mm:ss
     */
    void onDeviceTime(String deviceTime);
}
