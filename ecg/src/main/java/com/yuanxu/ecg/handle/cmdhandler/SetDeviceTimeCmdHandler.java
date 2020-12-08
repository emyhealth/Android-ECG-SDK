package com.yuanxu.ecg.handle.cmdhandler;

import android.text.TextUtils;

import com.yuanxu.ecg.cmd.SetDeviceTimeCmd;
import com.yuanxu.ecg.L;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetDeviceTimeCmdHandler extends BaseCmdHandler<SetDeviceTimeCmd> {
    private Pattern responsePatter = Pattern.compile(SetDeviceTimeCmd.CMD_PREFIX + "000000([0-9a-fA-F]{2})", Pattern.CASE_INSENSITIVE);

    public SetDeviceTimeCmdHandler(SetDeviceTimeCmd cmd) {
        super(cmd);
    }

    @Override
    public void sendCmdToDevice() {
        super.sendCmdToDevice();
        L.d("发送设置设备时间指令：" + cmd.getHexStringCmd());
    }

    @Override
    protected boolean handleCmdResponse(String hexResponse) {
        L.d(getClass().getSimpleName() + "处理====" + hexResponse);
        Matcher matcher = responsePatter.matcher(hexResponse);
        if (matcher.matches()) {
            String result = matcher.group(1);
            boolean success = !TextUtils.isEmpty(result) && result.equals("01");
            L.d("设置系统时间命令" + (success ? "成功" : "失败"));
            if (success) {
                if (next != null && nextHandlerSendCmdWhenCanHandle && next instanceof BaseCmdHandler) {
                    ((BaseCmdHandler) next).sendCmdToDevice();
                }
            } else {
                //发送失败，重新发送
                sendCmdToDevice();
            }
            return true;
        }
        return false;
    }
}
