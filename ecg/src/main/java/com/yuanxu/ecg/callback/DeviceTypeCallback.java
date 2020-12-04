package com.yuanxu.ecg.callback;

public interface DeviceTypeCallback extends BaseCallback {
    /**
     * 设备型号
     */
    void onDeviceType(String type);
}
