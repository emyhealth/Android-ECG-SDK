package com.yuanxu.ecg.handle;

import android.os.Message;

import com.yuanxu.ecg.MsgCenter;

public class HeartDataHandler extends BaseHandler {
    /**
     * 数据信息头（十六进制字符串）
     */
    public static final String DATA_HEAD_HEX_STR = "FDFE";

    @Override
    protected boolean handle(String hexResponse) {
        if (!hexResponse.startsWith(DATA_HEAD_HEX_STR) && !hexResponse.startsWith(DATA_HEAD_HEX_STR.toLowerCase())) {
            return false;
        }
        Message msg = Message.obtain();
        msg.what = MsgCenter.MSG_WHAT_DATA;
        msg.obj = hexResponse.substring(4);
        MsgCenter.getInstance().sendMsg(msg);
        return true;
    }

    private String getDataHeadHexStr() {
        return DATA_HEAD_HEX_STR;
    }
}
