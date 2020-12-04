package com.yuanxu.ecg.cmd;

import com.yuanxu.ecg.utils.ByteUtils;

public class StopTransferringCmd extends BaseCmd {
    /**
     * 指令头(十六进制字符串形式)
     */
    public static final String CMD_PREFIX = "E826";

    @Override
    public byte[] getCmd() {
        return ByteUtils.hexStr2Bytes(CMD_PREFIX + generateHexStringCmdPlaceholder(36));
    }

    @Override
    public String getHexStrCmdPrefix() {
        return CMD_PREFIX;
    }
}
