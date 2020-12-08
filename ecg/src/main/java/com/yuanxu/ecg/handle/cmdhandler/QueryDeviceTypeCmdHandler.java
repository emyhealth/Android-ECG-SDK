package com.yuanxu.ecg.handle.cmdhandler;

import android.os.Message;

import com.yuanxu.ecg.MsgCenter;
import com.yuanxu.ecg.cmd.QueryDeviceTypeCmd;
import com.yuanxu.ecg.L;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryDeviceTypeCmdHandler extends BaseCmdHandler<QueryDeviceTypeCmd> {
    private Pattern responsePatter = Pattern.compile(QueryDeviceTypeCmd.CMD_PREFIX + "(\\w{28})", Pattern.CASE_INSENSITIVE);

    public QueryDeviceTypeCmdHandler(QueryDeviceTypeCmd cmd) {
        super(cmd);
    }

    @Override
    public void sendCmdToDevice() {
        super.sendCmdToDevice();
        L.d("发送查询设备类型指令：" + cmd.getHexStringCmd());
    }

    @Override
    protected boolean handleCmdResponse(String hexResponse) {
        L.d(getClass().getSimpleName() + "处理====" + hexResponse);
        Matcher matcher = responsePatter.matcher(hexResponse);
        if (matcher.matches()) {
            String result = matcher.group(1);
            Message msg = Message.obtain();
            msg.what = MsgCenter.MSG_WHAT_DEVICE_TYPE;
            msg.obj = result;
            MsgCenter.getInstance().sendMsg(msg);
            return true;
        }
        return false;
    }
}
