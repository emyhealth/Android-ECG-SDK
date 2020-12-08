package com.yuanxu.ecg.handle.cmdhandler;

import android.os.Message;
import android.text.TextUtils;

import com.yuanxu.ecg.MsgCenter;
import com.yuanxu.ecg.cmd.StopTransferringCmd;
import com.yuanxu.ecg.L;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StopTransferringCmdHandler extends BaseCmdHandler<StopTransferringCmd> {
    private Pattern responsePatter = Pattern.compile(StopTransferringCmd.CMD_PREFIX + "000000([0-9a-fA-F]{2})", Pattern.CASE_INSENSITIVE);

    public StopTransferringCmdHandler(StopTransferringCmd cmd) {
        super(cmd);
    }

    @Override
    public void sendCmdToDevice() {
        super.sendCmdToDevice();
        L.d("发送停止传输指令：" + cmd.getHexStringCmd());
    }

    @Override
    protected boolean handleCmdResponse(String hexResponse) {
        L.d(getClass().getSimpleName() + "处理====" + hexResponse);
        Matcher matcher = responsePatter.matcher(hexResponse);
        if (matcher.matches()) {
            String result = matcher.group(1);
            boolean success = !TextUtils.isEmpty(result) && result.equals("01");
            L.d("停止实时传输命令" + (success ? "成功" : "失败"));
            if (success) {
                if (next != null && nextHandlerSendCmdWhenCanHandle && next instanceof BaseCmdHandler) {
                    ((BaseCmdHandler) next).sendCmdToDevice();
                }
                Message msg = Message.obtain();
                msg.what = MsgCenter.MSG_WHAT_STOP_TRANSFERRING_SUCCESS;
                MsgCenter.getInstance().sendMsg(msg);
            } else {
                //发送失败，重新发送
                sendCmdToDevice();
            }
            return true;
        }
        return false;
    }
}
