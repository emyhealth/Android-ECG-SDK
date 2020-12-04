package com.yuanxu.ecg.handle.cmdhandler;

import android.text.TextUtils;

import com.yuanxu.ecg.cmd.StopCollectingCmd;
import com.yuanxu.ecg.L;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StopCollectingCmdHandler extends BaseCmdHandler<StopCollectingCmd> {
    private Pattern responsePatter = Pattern.compile(StopCollectingCmd.CMD_PREFIX + "000000([0-9a-fA-F]{2})",Pattern.CASE_INSENSITIVE);

    public StopCollectingCmdHandler(StopCollectingCmd cmd) {
        super(cmd);
    }

    @Override
    protected boolean handleCmdResponse(String hexResponse) {
        L.d(getClass().getSimpleName() + "处理====" + hexResponse);
        Matcher matcher = responsePatter.matcher(hexResponse);
        if (matcher.matches()) {
            String result = matcher.group(1);
            boolean success = !TextUtils.isEmpty(result) && result.equals("01");
            L.d("停止采集命令" + (success ? "成功" : "失败"));
            if (!success) {
                //发送失败，重新发送
                sendCmdToDevice();
            }
            return true;
        }
        return false;
    }
}
