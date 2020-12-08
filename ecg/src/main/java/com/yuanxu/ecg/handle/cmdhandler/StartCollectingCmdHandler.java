package com.yuanxu.ecg.handle.cmdhandler;

import android.text.TextUtils;

import com.yuanxu.ecg.cmd.StartCollectingCmd;
import com.yuanxu.ecg.L;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StartCollectingCmdHandler extends BaseCmdHandler<StartCollectingCmd> {

    private Pattern responsePatter = Pattern.compile(StartCollectingCmd.CMD_PREFIX + "000000([0-9a-fA-F]{2})", Pattern.CASE_INSENSITIVE);

    public StartCollectingCmdHandler(StartCollectingCmd cmd) {
        super(cmd);
    }

    @Override
    public void sendCmdToDevice() {
        super.sendCmdToDevice();
        L.d("发送开始采集指令：" + cmd.getHexStringCmd());
    }

    @Override
    protected boolean handleCmdResponse(String hexResponse) {
        L.d(getClass().getSimpleName() + "处理====" + hexResponse);
        Matcher matcher = responsePatter.matcher(hexResponse);
        if (matcher.matches()) {
            String result = matcher.group(1);
            boolean success = !TextUtils.isEmpty(result) && result.equals("01");
            L.d("发送单机采集命令" + (success ? "成功" : "失败"));
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
