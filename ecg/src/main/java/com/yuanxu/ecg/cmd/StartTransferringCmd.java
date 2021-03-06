package com.yuanxu.ecg.cmd;

import com.yuanxu.ecg.utils.ByteUtils;

public class StartTransferringCmd extends BaseCmd {
    /**
     * 指令头(十六进制字符串形式)
     */
    public static final String CMD_PREFIX = "E820";

    @Override
    public byte[] getCmd() {
        return ByteUtils.hexStr2Bytes(CMD_PREFIX + "0136EE80");
    }

    @Override
    public String getHexStrCmdPrefix() {
        return CMD_PREFIX;
    }
}
