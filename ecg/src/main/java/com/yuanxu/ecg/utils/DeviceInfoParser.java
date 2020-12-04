package com.yuanxu.ecg.utils;

import java.nio.charset.StandardCharsets;

public class DeviceInfoParser {
    public static String parseDeviceTime(String hexDeviceTime) {
        return parseDeviceTime(ByteUtils.hexStr2Bytes(hexDeviceTime));
    }

    public static String parseDeviceTime(byte[] bytes) {
        String str = "";
        try {
            if (bytes != null) {
                int year = bytes[0] + 2000;
                int month = bytes[1];
                int day = bytes[2];
                int hour = bytes[3];
                int minute = bytes[4];
                int second = bytes[5];
                str = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
            }
            return str;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String parseDeviceType(String hexDeviceType) {
        return parseDeviceType(ByteUtils.hexStr2Bytes(hexDeviceType));
    }

    public static String parseDeviceType(byte[] bytes) {
        try {
            if (bytes != null) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
