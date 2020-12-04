package com.yuanxu.ecg.handle.cmdhandler;

import com.yuanxu.ecg.cmd.StartTransferringCmd;
import com.yuanxu.ecg.L;

public class StartTransferringCmdHandler extends BaseCmdHandler<StartTransferringCmd> {

    public StartTransferringCmdHandler(StartTransferringCmd cmd) {
        super(cmd);
    }

    @Override
    protected boolean handleCmdResponse(String hexResponse) {
        L.d(getClass().getSimpleName() + "处理====" + hexResponse);
        //传输指令发送后并无指令回应信息，硬件会直接传输心电数据至本设备，
        //而心电数据处理单独在HeartDataHandler中处理，故此处直接返回false
        return false;
    }
}
