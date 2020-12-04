package com.yuanxu.ecg.cmd;

import android.text.TextUtils;

import com.yuanxu.ecg.utils.ByteUtils;
import com.yuanxu.ecg.L;

public abstract class BaseCmd {
    /**
     * 获取命令
     */
    public abstract byte[] getCmd();

    /**
     * 获取指令头（十六进制字符串）
     */
    public abstract String getHexStrCmdPrefix();

    /**
     * 获取十六进制字符串cmd
     */
    public String getHexStringCmd() {
        if (getCmd() == null || getCmd().length <= 0) {
            return "";
        }
        return ByteUtils.bytes2HexStr(getCmd());
    }


    protected String generateHexStringCmdPlaceholder(int count) {
        return generateHexStringCmdPlaceholder(count, "0");
    }

    /**
     * 生成十六进制字符串形式的命令占位符
     * <p>
     * 注意：两个十六进制字符代表一个字节，生成占位符时需注意此点
     *
     * @param count 占位符个数
     */
    protected String generateHexStringCmdPlaceholder(int count, String placeHolder) {
        if (count <= 0) {
            return "";
        }
        if (count % 2 != 0) {
            L.d("生成指令占位符个数为奇数");
        }
        if (TextUtils.isEmpty(placeHolder) || placeHolder.length() != 1 ||
                !placeHolder.matches("[0-9a-fA-F]")) {
            placeHolder = "f";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(placeHolder);
        }
        return builder.toString();
    }
}
