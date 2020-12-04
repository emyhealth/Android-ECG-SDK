package com.yuanxu.ecg;

import com.yuanxu.ecg.bean.CmdType;
import com.yuanxu.ecg.cmd.BaseCmd;

import java.lang.reflect.Constructor;

/**
 * 指令工厂类，对外提供
 */
public class CmdFactory {
    private CmdFactory() {

    }

    public static CmdFactory newInstance() {
        return new CmdFactory();
    }

    public BaseCmd createCmd(CmdType cmdType) {
        String absoluteClassName = cmdType.getCmdAbsoluteClassName();
        try {
            Class<?> clasz = Class.forName(absoluteClassName);
            Constructor<?> constructor = clasz.getConstructor();
            BaseCmd d = (BaseCmd) constructor.newInstance();
            return d;
        } catch (Exception e) {
            L.e("CmdFactory生成" + cmdType.getCmdClassName() + "对象出错：" + e.getMessage());
            return null;
        }
    }

}
