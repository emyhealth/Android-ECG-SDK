package com.yuanxu.ecg.handle.cmdhandler;

import android.os.Message;

import com.yuanxu.ecg.MsgCenter;
import com.yuanxu.ecg.cmd.QueryStatusCmd;
import com.yuanxu.ecg.L;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryStatusCmdHandler extends BaseCmdHandler<QueryStatusCmd> {
    /**
     * 状态类型
     */
    public static final String STATUS_IDLE = "00";//待机
    public static final String STATUS_COLLECTING_REAL_TIME = "01";//实时采集
    public static final String STATUS_COLLECTING_SYNC = "02";//同步采集
    public static final String STATUS_COLLECTING_SINGLE = "03";//单机采集

    private Pattern responsePatter = Pattern.compile(QueryStatusCmd.CMD_PREFIX + "([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})",
            Pattern.CASE_INSENSITIVE);

    public QueryStatusCmdHandler(QueryStatusCmd cmd) {
        super(cmd);
    }

    @Override
    public void sendCmdToDevice() {
        super.sendCmdToDevice();
        L.d("发送查询设备状态指令：" + cmd.getHexStringCmd());
    }

    @Override
    protected boolean handleCmdResponse(String hexResponse) {
        L.d(getClass().getSimpleName() + "处理====" + hexResponse);
        Matcher matcher = responsePatter.matcher(hexResponse);
        if (matcher.matches()) {
            String batteryCode = matcher.group(1);
            String status = matcher.group(2);
            String batteryX = matcher.group(3);
            String batteryY = matcher.group(4);
            boolean idle = STATUS_IDLE.equals(status);
            L.d("设备状态为" + (idle ? "待机" : "非待机状态") + " status=" + status);
            if (idle) { //待机状态，可以执行采集等其他指令
                if (next != null && nextHandlerSendCmdWhenCanHandle &&
                        next instanceof BaseCmdHandler) {
                    ((BaseCmdHandler) next).sendCmdToDevice();
                }
            } else {//非待机状态
                Message msg = Message.obtain();
                msg.what = MsgCenter.MSG_WHAT_DEVICE_NOT_IDLE;
                msg.obj = status;
                MsgCenter.getInstance().sendMsg(msg);
            }
            return true;
        }
        return false;
    }
}
