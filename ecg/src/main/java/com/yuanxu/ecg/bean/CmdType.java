package com.yuanxu.ecg.bean;

import com.yuanxu.ecg.cmd.BindUserIdCmd;
import com.yuanxu.ecg.cmd.QueryDeviceTimeCmd;
import com.yuanxu.ecg.cmd.QueryDeviceTypeCmd;
import com.yuanxu.ecg.cmd.QueryStatusCmd;
import com.yuanxu.ecg.cmd.SetDeviceTimeCmd;
import com.yuanxu.ecg.cmd.StartCollectingCmd;
import com.yuanxu.ecg.cmd.StartTransferringCmd;
import com.yuanxu.ecg.cmd.StopCollectingCmd;
import com.yuanxu.ecg.cmd.StopTransferringCmd;

public enum CmdType {
    QUERY_STATUS(QueryStatusCmd.class, "查询设备状态"),
    QUERY_DEVICE_TIME(QueryDeviceTimeCmd.class, "查询设备时间"),
    QUERY_DEVICE_TYPE(QueryDeviceTypeCmd.class, "查询设备类型"),
    BIND_USER_ID(BindUserIdCmd.class, "绑定用户"),
    SET_DEVICE_TIME(SetDeviceTimeCmd.class, "设置设备时间"),
    START_COLLECTING(StartCollectingCmd.class, "硬件开始采集数据"),
    START_TRANSFERRING(StartTransferringCmd.class, "硬件开始传输数据"),
    STOP_COLLECTING(StopCollectingCmd.class, "硬件停止采集数据"),
    STOP_TRANSFERRING(StopTransferringCmd.class, "硬件停止传输数据");


    private String cmdClassName;//指令类名
    private String cmdAbsoluteClassName;//指令类全路径名
    private String cmdDetail;//指令介绍

    CmdType(Class<?> cmdClass, String cmdDetail) {
        this.cmdDetail = cmdDetail;
        this.cmdClassName = cmdClass.getSimpleName();
        String packageName = cmdClass.getPackage() == null ? "com.yuanxu.ecg.cmd" : cmdClass.getPackage().getName();
        this.cmdAbsoluteClassName = packageName + "." + cmdClass.getSimpleName();
    }

    public String getCmdClassName() {
        return cmdClassName == null ? "" : cmdClassName;
    }

    public String getCmdAbsoluteClassName() {
        return cmdAbsoluteClassName == null ? "" : cmdAbsoluteClassName;
    }

    public String getCmdDetail() {
        return cmdDetail == null ? "" : cmdDetail;
    }
}
