package com.yuanxu.ecg.handle.cmdhandler;

import android.os.Message;

import com.yuanxu.ecg.MsgCenter;
import com.yuanxu.ecg.cmd.BaseCmd;
import com.yuanxu.ecg.handle.BaseHandler;
import com.yuanxu.ecg.L;

public abstract class BaseCmdHandler<T extends BaseCmd> extends BaseHandler {
    protected static String sHexStrCmdErrorResponse = "E8FF00000000";

    protected T cmd;

    /**
     * 当本级指令handler能够处理且处理完硬件返回msg后，是否让下一级指令Handler发送命令
     * <p>
     * 应用场景：当发送状态指令后，收到硬件返回的状态msg，若硬件处于待机状态，则继续发送
     * 如绑定用户或单机采集等指令进行绑定或采集流程
     * <p>
     * 注意：该值设置为true应谨慎，例如若开始单机采集handler的next为停止采集handler，此
     * 时若单机采集handler的nextHandlerSendCmdWhenCanHandle设置为true，则会出现在收到
     * 单机采集指令的回复后立刻发送停止采集指令
     */
    protected boolean nextHandlerSendCmdWhenCanHandle;

    public BaseCmdHandler(T cmd) {
        if (cmd == null) {
            throw new IllegalStateException("cmd is null");
        }
        this.cmd = cmd;
    }

    /**
     * 设置当本级指令handler能够处理且处理完硬件返回msg后，是否让下一级指令Handler发送
     * 命令，详细见{@link #nextHandlerSendCmdWhenCanHandle}注释说明
     */
    public BaseCmdHandler setNextHandlerSendCmdWhenCanHandle(boolean nextHandlerSendCmdWhenCanHandle) {
        this.nextHandlerSendCmdWhenCanHandle = nextHandlerSendCmdWhenCanHandle;
        return this;
    }

    public boolean isNextHandlerSendCmdWhenCanHandle() {
        return nextHandlerSendCmdWhenCanHandle;
    }

    /**
     * 获取命令
     */
    public T getCmd() {
        return cmd;
    }

    /**
     * 发送指令至设备
     */
    public void sendCmdToDevice() {
        Message msg = Message.obtain();
        msg.what = MsgCenter.MSG_WHAT_CMD_SEND;
        msg.obj = cmd.getHexStringCmd();
        MsgCenter.getInstance().sendMsg(msg);
    }

    @Override
    protected boolean handle(String hexResponse) {
        //指令有误（长度不足、命令字不存在、逻辑错误）
        if (hexResponse.equalsIgnoreCase(sHexStrCmdErrorResponse) || hexResponse.startsWith(sHexStrCmdErrorResponse)) {
            L.e("指令有误");
            handleCmdError();
            return true;
        }
        //指令执行失败（索要数据不存在、当前时刻不适合执行此命令、设备错误）
        if (hexResponse.equalsIgnoreCase(getCmdExecuteFailResponse())) {
            L.e(cmd.getClass().getSimpleName() + "指令执行失败");
            handleCmdExecuteFail(hexResponse);
            return true;
        }
        return handleCmdResponse(hexResponse);
    }

    /**
     * 处理指令回复msg
     *
     * @param hexResponse 下位机（硬件）返回的十六进制字符串形式的指令回复msg
     * @return 是否为本指令的回复msg（即是否能够处理），true表示能处理，false反之
     */
    protected abstract boolean handleCmdResponse(String hexResponse);


    /**
     * 处理指令有误（长度不足、命令字不存在、逻辑错误）
     */
    protected void handleCmdError() {
        Message msg = Message.obtain();
        msg.what = MsgCenter.MSG_WHAT_CMD_ERROR;
        MsgCenter.getInstance().sendMsg(msg);
    }

    /**
     * 指令执行失败（索要数据不存在、当前时刻不适合执行此命令、设备错误）
     */
    protected void handleCmdExecuteFail(String originalHexResponse) {
        Message msg = Message.obtain();
        msg.what = MsgCenter.MSG_WHAT_CMD_EXECUTE_FAIL;
        msg.obj = originalHexResponse;
        MsgCenter.getInstance().sendMsg(msg);
    }

    /**
     * 获取指令发送失败回复（索要数据不存在、当前时刻不适合执行此命令、设备错误）
     * <p>
     * 若硬件改变了协议回复内容，则重写该方法即可
     */
    protected String getCmdExecuteFailResponse() {
        return cmd.getHexStrCmdPrefix() + "00000000";
    }

}
