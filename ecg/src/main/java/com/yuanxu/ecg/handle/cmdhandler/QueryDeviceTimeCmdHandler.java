package com.yuanxu.ecg.handle.cmdhandler;

import android.os.Message;

import com.yuanxu.ecg.MsgCenter;
import com.yuanxu.ecg.cmd.QueryDeviceTimeCmd;
import com.yuanxu.ecg.L;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryDeviceTimeCmdHandler extends BaseCmdHandler<QueryDeviceTimeCmd> {
    private Pattern responsePatter = Pattern.compile(QueryDeviceTimeCmd.CMD_PREFIX + "(\\w{12})",Pattern.CASE_INSENSITIVE);

    public QueryDeviceTimeCmdHandler(QueryDeviceTimeCmd cmd) {
        super(cmd);
    }

    @Override
    protected boolean handleCmdResponse(String hexResponse) {
        L.d(getClass().getSimpleName() + "处理====" + hexResponse);
        Matcher matcher = responsePatter.matcher(hexResponse);
        if (matcher.matches()) {
            String result = matcher.group(1);
            Message msg = Message.obtain();
            msg.what = MsgCenter.MSG_WHAT_DEVICE_TIME;
            msg.obj = result;
            MsgCenter.getInstance().sendMsg(msg);
            return true;
        }
        return false;
    }

}
