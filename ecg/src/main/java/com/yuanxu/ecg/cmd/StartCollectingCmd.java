package com.yuanxu.ecg.cmd;

import com.yuanxu.ecg.utils.ByteUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StartCollectingCmd extends BaseCmd {
    /**
     * 指令头(十六进制字符串形式)
     */
    public static final String CMD_PREFIX = "E823";

    private long timestamp = Long.MIN_VALUE;

    @Override
    public byte[] getCmd() {
        return ByteUtils.hexStr2Bytes(CMD_PREFIX + getHexStrDeviceTime());
    }

    @Override
    public String getHexStrCmdPrefix() {
        return CMD_PREFIX;
    }

    protected String getHexStrDeviceTime() {
        if (timestamp <= Long.MIN_VALUE) {
            return generateHexStringCmdPlaceholder(12);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStr = sdf.format(new Date(timestamp));
        String[] arr = timeStr.split(" ");
        String[] date = arr[0].split("-");
        String[] time = arr[1].split(":");

        byte year = Byte.valueOf(date[0].substring(date[0].length() - 2));
        byte month = Byte.valueOf(date[1]);
        byte day = Byte.valueOf(date[2]);
        byte hour = Byte.valueOf(time[0]);
        byte minute = Byte.valueOf(time[1]);
        byte second = Byte.valueOf(time[2]);

        return ByteUtils.byte2HexStr(year) + ByteUtils.byte2HexStr(month) + ByteUtils.byte2HexStr(day)
                + ByteUtils.byte2HexStr(hour) + ByteUtils.byte2HexStr(minute) + ByteUtils.byte2HexStr(second);
    }

    public StartCollectingCmd setTimestamp(long mills) {
        this.timestamp = mills;
        return this;
    }
}
