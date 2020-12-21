package com.yuanxu.ecg;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.yuanxu.ecg.handle.cmdhandler.QueryStatusCmdHandler;

import java.util.ArrayList;
import java.util.List;

public class MsgCenter {
    public static final int MSG_WHAT_CMD_SEND = 100;//发送指令
    public static final int MSG_WHAT_CMD_ERROR = 101;//指令有误（长度不足、命令字不存在、逻辑错误）
    public static final int MSG_WHAT_CMD_EXECUTE_FAIL = 102;//指令执行失败（索要数据不存在、当前时刻不适合执行此命令、设备错误）
    public static final int MSG_WHAT_DEVICE_TIME = 103;//设备时间
    public static final int MSG_WHAT_DEVICE_TYPE = 104;//设备类型
    public static final int MSG_WHAT_DEVICE_NOT_IDLE = 105;//设备非待机状态
    public static final int MSG_WHAT_DATA = 106;//数据
    public static final int MSG_WHAT_STOP_TRANSFERRING_SUCCESS = 107;//停止传输发送成功


    private Handler handler;
    private List<MsgListener> msgListeners;


    private MsgCenter() {

    }

    private static class MsgCenterHolder {
        static final MsgCenter sMsgCenter = new MsgCenter();
    }

    public static MsgCenter getInstance() {
        return MsgCenterHolder.sMsgCenter;
    }

    public void init() {
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                int what = msg.what;
                String info = msg.obj == null ? "" : (String) msg.obj;
                notifyMsgListener(what, info);
            }
        };
    }

    public void release() {
        clearAllMsgListener();
        msgListeners = null;
        handler = null;
    }

    private void notifyMsgListener(int type, String hexInfo) {
        if (msgListeners == null) return;
        for (MsgListener l : msgListeners) {
            switch (type) {
                case MSG_WHAT_CMD_SEND:
                    l.onSendCmd(hexInfo);
                    break;
                case MSG_WHAT_CMD_ERROR:
                    l.onCmdError();
                    break;
                case MSG_WHAT_CMD_EXECUTE_FAIL:
                    l.onCmdExecuteFail(hexInfo);
                    break;
                case MSG_WHAT_DEVICE_TIME:
                    l.onDeviceTime(hexInfo);
                    break;
                case MSG_WHAT_DEVICE_TYPE:
                    l.onDeviceType(hexInfo);
                    break;
                case MSG_WHAT_DATA:
                    l.onReceivedHeartData(hexInfo);
                    break;
                case MSG_WHAT_STOP_TRANSFERRING_SUCCESS:
                    l.onStopTransferringSuccess();
                    break;
                case MSG_WHAT_DEVICE_NOT_IDLE:
                    l.onDeviceNotIdle(hexInfo);
                default:
                    break;
            }
        }
    }

    public synchronized void addMsgListener(MsgListener l) {
        if (l == null) {
            return;
        }
        if (msgListeners == null) {
            msgListeners = new ArrayList<>();
        }
        msgListeners.add(l);
    }

    public synchronized void removeMsgListener(MsgListener l) {
        if (l == null || msgListeners == null) {
            return;
        }
        msgListeners.remove(l);
    }

    public synchronized void clearAllMsgListener() {
        if (msgListeners == null) return;
        msgListeners.clear();
    }

    public void sendMsg(Message msg) {
        handler.sendMessage(msg);
    }

    public void sendDelayMsg(Message msg, long delayMills) {
        if (delayMills < 0) {
            delayMills = 0;
        }
        handler.sendMessageDelayed(msg, delayMills);
    }

    public interface MsgListener {
        void onSendCmd(String hexCmd);

        void onDeviceType(String hexDeviceType);

        void onDeviceTime(String hexDeviceTime);

        /**
         * 设备非空闲（待机）状态
         *
         * @param status 具体状态，详见
         *               {@link QueryStatusCmdHandler#STATUS_COLLECTING_SYNC}
         *               {@link QueryStatusCmdHandler#STATUS_COLLECTING_SINGLE}
         *               {@link QueryStatusCmdHandler#STATUS_COLLECTING_REAL_TIME}
         */
        void onDeviceNotIdle(String status);

        /**
         * 指令有误（长度不足、命令字不存在、逻辑错误）
         */
        void onCmdError();

        /**
         * 指令执行失败（索要数据不存在、当前时刻不适合执行此命令、设备错误）
         */
        void onCmdExecuteFail(String hexStrResponse);

        void onReceivedHeartData(String hexData);

        void onStopTransferringSuccess();
    }

}
