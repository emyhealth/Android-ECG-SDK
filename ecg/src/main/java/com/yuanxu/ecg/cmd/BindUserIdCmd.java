package com.yuanxu.ecg.cmd;

import android.text.TextUtils;

import com.yuanxu.ecg.utils.ByteUtils;

import java.nio.charset.StandardCharsets;

public class BindUserIdCmd extends BaseCmd {
    /**
     * 指令头(十六进制字符串形式)
     */
    public static final String CMD_PREFIX = "E841";

    /**
     * 用户信息部分的指令前缀(十六进制字符串形式)
     */
    public static final String USER_INFO_PREFIX = "AA";

    /**
     * 最大姓名字节长度
     */
    public static final int MAX_NAME_BYTE_LENGTH = 12;


    private String hexName;//姓名（最多12字节）
    private String hexGender;//性别 0x00表示女，0x01表示男
    private String hexAge;//年龄（1个字节）
    private String hexHeight;//身高/cm（1个字节）
    private String hexWeight;//体重/kg（1个字节）

    @Override
    public byte[] getCmd() {
        checkCmd();
        StringBuilder builder = new StringBuilder();
        //指令头
        builder.append(CMD_PREFIX);
        //user信息前缀
        builder.append(USER_INFO_PREFIX);
        //姓名（姓名字节长度 + 姓名 + 姓名不足12字节时占位符）
        builder.append(getHexStrLengthOfNameBytes());
        builder.append(hexName);
        builder.append(generateHexStringCmdPlaceholder(MAX_NAME_BYTE_LENGTH * 2 - hexName.length()));
        //性别、年龄、身高、体重等
        builder.append(hexGender);
        builder.append(hexAge);
        builder.append(hexHeight);
        builder.append(hexWeight);
        return ByteUtils.hexStr2Bytes(builder.toString());
    }

    @Override
    public String getHexStrCmdPrefix() {
        return CMD_PREFIX;
    }

    /**
     * 是否为合法的名字字节长度，名字的字节长度不超过{@link #MAX_NAME_BYTE_LENGTH}
     */
    public static boolean isValidNameBytesLength(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        return name.getBytes(StandardCharsets.UTF_8).length <= BindUserIdCmd.MAX_NAME_BYTE_LENGTH;
    }

    public BindUserIdCmd setName(String name) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("name is null");
        }
        byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_NAME_BYTE_LENGTH) {
            throw new IllegalArgumentException("the length of this name(" + name + ") is greater than max byte length(" + MAX_NAME_BYTE_LENGTH + ")");
        }
        hexName = ByteUtils.bytes2HexStr(bytes);
        return this;
    }

    public BindUserIdCmd setGender(boolean female) {
        hexGender = female ? "00" : "01";
        return this;
    }

    public BindUserIdCmd setAge(int age) {
        hexAge = getHexStrOfInt(age);
        return this;
    }

    public BindUserIdCmd setHeight(int height) {
        hexHeight = getHexStrOfInt(height);
        return this;
    }

    public BindUserIdCmd setWeight(int weight) {
        hexWeight = getHexStrOfInt(weight);
        return this;
    }

    public String getHexGender() {
        return hexGender == null ? "" : hexGender;
    }

    public String getHexName() {
        return hexName == null ? "" : hexName;
    }

    public String getHexAge() {
        return hexAge == null ? "" : hexAge;
    }

    public String getHexHeight() {
        return hexHeight == null ? "" : hexHeight;
    }

    public String getHexWeight() {
        return hexWeight == null ? "" : hexWeight;
    }

    private String getHexStrLengthOfNameBytes() {
        return getHexStrOfInt(hexName.length() / 2);
    }

    /**
     * 获取int型数据的最后一字节所标识的十六进制字符串
     */
    private String getHexStrOfInt(int value) {
        byte[] bytes = ByteUtils.int2Bytes(value);
        String str = ByteUtils.bytes2HexStr(bytes);
        return str.substring(str.length() - 2);
    }

    private void checkCmd() {
        if (TextUtils.isEmpty(hexName) || TextUtils.isEmpty(hexGender) ||
                TextUtils.isEmpty(hexAge) || TextUtils.isEmpty(hexHeight) ||
                TextUtils.isEmpty(hexWeight)) {
            throw new IllegalStateException("Please make sure that you have set all user info(name,gender,age,height,weight)");
        }
    }
}
