package com.yuanxu.ecg.callback;

public interface BaseCallback {
    int FAIL_BLUETOOTH_NOT_AVAILABLE = 100;//蓝牙不可用
    int FAIL_OTHER = 101;//其他原因

    /**
     * 失败
     */
    void onFailure(int failCode, String info);
}
