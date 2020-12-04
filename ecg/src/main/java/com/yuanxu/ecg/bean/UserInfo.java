package com.yuanxu.ecg.bean;

import android.text.TextUtils;

import com.yuanxu.ecg.cmd.BindUserIdCmd;

import java.io.Serializable;

public class UserInfo implements Serializable {
    private String name;//姓名
    private boolean female;//是否为女性
    private int age;//年龄
    private int height;//身高（cm）
    private int weight;//体重（kg）

    public UserInfo(String name, boolean female, int age, int height, int weight) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("name is null");
        }
        if (!BindUserIdCmd.isValidNameBytesLength(name)) {
            throw new IllegalArgumentException("the length of this name(" + name + ") is greater than max byte length(" + BindUserIdCmd.MAX_NAME_BYTE_LENGTH + ")");
        }
        this.name = name;
        this.female = female;
        this.age = age;
        this.height = height;
        this.weight = weight;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFemale() {
        return female;
    }

    public void setFemale(boolean female) {
        this.female = female;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
