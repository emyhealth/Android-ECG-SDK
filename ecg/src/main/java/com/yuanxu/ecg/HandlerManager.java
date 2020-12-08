package com.yuanxu.ecg;

import com.yuanxu.ecg.cmd.BaseCmd;
import com.yuanxu.ecg.cmd.BindUserIdCmd;
import com.yuanxu.ecg.cmd.QueryDeviceTimeCmd;
import com.yuanxu.ecg.cmd.QueryDeviceTypeCmd;
import com.yuanxu.ecg.cmd.QueryStatusCmd;
import com.yuanxu.ecg.cmd.SetDeviceTimeCmd;
import com.yuanxu.ecg.cmd.StartCollectingCmd;
import com.yuanxu.ecg.cmd.StartTransferringCmd;
import com.yuanxu.ecg.cmd.StopCollectingCmd;
import com.yuanxu.ecg.cmd.StopTransferringCmd;
import com.yuanxu.ecg.handle.BaseHandler;
import com.yuanxu.ecg.handle.HeartDataHandler;
import com.yuanxu.ecg.handle.cmdhandler.BaseCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.BindUserIdCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.QueryDeviceTimeCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.QueryDeviceTypeCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.QueryStatusCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.SetDeviceTimeCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.StartCollectingCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.StartTransferringCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.StopCollectingCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.StopTransferringCmdHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HandlerManager {
    private BaseHandler handlerHead, handlerMiddle;
    private CmdExecuteFlow cmdExecuteFlow;

    private HandlerManager() {

    }

    static HandlerManager getInstance() {
        return HandlerManagerHolder.sHandlerManager;
    }

    private static final class HandlerManagerHolder {
        static final HandlerManager sHandlerManager = new HandlerManager();
    }

    public void init() {
        if (handlerHead != null) {
            return;
        }
        initHandlerLink();
    }

    public void release() {
        handlerHead = null;
        handlerMiddle = null;
        cmdExecuteFlow = null;
    }

    /**
     * 获取handler链头结点
     */
    public BaseHandler getHandlerHead() {
        return handlerHead;
    }

    /**
     * 获取handler链中{@link #cmdExecuteFlow}链的上一节点。详
     * 见{@link #initHandlerLink()}代码
     */
    public BaseHandler getHandlerEnd() {
        return handlerMiddle;
    }

    /**
     * 获取handler链中指令处理链头结点
     */
    public BaseCmdHandler getCmdHandlerHead() {
        if (cmdExecuteFlow == null) {
            return getDefaultCmdExecutorFlow().getHead();
        }
        return cmdExecuteFlow.getHead();
    }

    public <T extends BaseHandler> T getHandler(Class<T> tClass) {
        try {
            BaseHandler current = handlerHead;
            while (current != null) {
                if (current.getClass().getSimpleName().equals(tClass.getSimpleName())) {
                    return (T) current;
                }
                current = current.getNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
            L.e("HandlerManager#getHandler异常：" + e.getMessage());
        }
        return null;
    }

    /**
     * 设置自定义指令执行链
     */
    public void setCustomCmdExecuteFlow(CmdExecuteFlow flow) {
        if (flow == null || flow.getHead() == null) {
            throw new IllegalArgumentException("CmdExecuteFlow is null");
        }
        if (handlerHead == null || handlerMiddle == null) {
            throw new IllegalStateException("You should call init() first");
        }
        if (!(flow.getHead() instanceof QueryStatusCmdHandler)) {
            L.w("自定义CmdExecuteFlow头结点不为QueryStatusCmdHandler，某些条件下执行该指令执行流可能出现问题");
        }
        this.cmdExecuteFlow = flow;
        handlerMiddle.setNext(cmdExecuteFlow.getHead());
        //检查handler链表中是否含有相同类型的handler
        checkHandlerLink(handlerHead);
    }

    /**
     * 获取当前的命令执行链
     */
    public CmdExecuteFlow getCmdExecuteFlow() {
        return cmdExecuteFlow;
    }


    private void initHandlerLink() {
        HeartDataHandler h0 = new HeartDataHandler();//数据处理handler
        StopTransferringCmdHandler h1 = new StopTransferringCmdHandler(new StopTransferringCmd());//停止实时传输handler
        StopCollectingCmdHandler h2 = new StopCollectingCmdHandler(new StopCollectingCmd());//停止采集handler
        QueryDeviceTimeCmdHandler h3 = new QueryDeviceTimeCmdHandler(new QueryDeviceTimeCmd());//查询设备时间handler
        QueryDeviceTypeCmdHandler h4 = new QueryDeviceTypeCmdHandler(new QueryDeviceTypeCmd());//查询设备类型handler

        h0.setNext(h1);
        h1.setNext(h2);//停止采集handler作停止传输handler的next，以便停止传输后停止采集
        h2.setNext(h3);
        h3.setNext(h4);

        if (cmdExecuteFlow == null) {
            cmdExecuteFlow = getDefaultCmdExecutorFlow();
        }
        h4.setNext(cmdExecuteFlow.getHead());

        handlerHead = h0;
        handlerMiddle = h4;
    }

    private CmdExecuteFlow getDefaultCmdExecutorFlow() {
        QueryStatusCmdHandler h1 = new QueryStatusCmdHandler(new QueryStatusCmd());
        SetDeviceTimeCmdHandler h2 = new SetDeviceTimeCmdHandler(new SetDeviceTimeCmd());
        BindUserIdCmdHandler h3 = new BindUserIdCmdHandler(new BindUserIdCmd());
        StartCollectingCmdHandler h4 = new StartCollectingCmdHandler(new StartCollectingCmd());
        StartTransferringCmdHandler h5 = new StartTransferringCmdHandler(new StartTransferringCmd());

        h1.setNextHandlerSendCmdWhenCanHandle(true);
        h2.setNextHandlerSendCmdWhenCanHandle(true);
        h3.setNextHandlerSendCmdWhenCanHandle(true);
        h4.setNextHandlerSendCmdWhenCanHandle(true);

        return CmdExecuteFlow
                .newInstance()
                .next(h1)
                .next(h2)
                .next(h3)
                .next(h4)
                .next(h5);
    }

    /**
     * 指令执行流
     * <p>
     * 注：本类只能添加的一些指定类型的指令handler，一般情况下我们不希望开发者
     * 改变类型限制，但若开发者用特殊需求或其他极其特殊原因需要添加其他类型指令
     * handler而又希望该类型能被CmdExecuteFlow接受，则使用反射调
     * 用{@link CmdExecuteFlow#addPermitType(Class)}添加对应类型即可，详见该
     * 方法注释
     */
    public static final class CmdExecuteFlow {
        private BaseCmdHandler head, current;
        private Set<String> typeSet;

        private CmdExecuteFlow() {
            restrictType();
        }

        public static CmdExecuteFlow newInstance() {
            return new CmdExecuteFlow();
        }

        public CmdExecuteFlow next(BaseCmdHandler cmdHandler) {
            if (cmdHandler == null) {
                throw new IllegalArgumentException("BaseCmdHandler is null");
            }
            String className = cmdHandler.getClass().getSimpleName();
            if (!typeSet.contains(className)) {
                throw new IllegalArgumentException("This " + className + " is not permitted here");
            }
            if (head == null) {
                head = cmdHandler;
                current = head;
            } else {
                current.setNext(cmdHandler);
                current = cmdHandler;
            }
            return this;
        }

        public BaseCmdHandler getHead() {
            return head;
        }

        public BaseCmdHandler getCurrent() {
            return current;
        }

        public <T extends BaseCmd> T getCmd(Class<T> tClass) {
            try {
                BaseCmdHandler current = head;
                while (current != null) {
                    String name = current.getCmd().getClass().getSimpleName();
                    if (name.equals(tClass.getSimpleName())) {
                        return (T) current.getCmd();
                    }
                    current = (BaseCmdHandler) current.getNext();
                }
            } catch (Exception e) {
                e.printStackTrace();
                L.e("CmdExecuteLink中getCmd异常：" + e.getMessage());
            }
            return null;
        }

        /**
         * 查询该指令handler是否是被允许的类型
         */
        public boolean isPermitted(BaseCmdHandler cmdHandler) {
            return typeSet.contains(cmdHandler.getClass().getSimpleName());
        }

        /**
         * 获取所有允许类型handler集合
         */
        public List<String> getAllPermitCmdHandler() {
            List<String> list = new ArrayList<>();
            list.addAll(typeSet);
            return list;
        }

        private void restrictType() {
            addPermitType(QueryStatusCmdHandler.class);
            addPermitType(SetDeviceTimeCmdHandler.class);
            addPermitType(BindUserIdCmdHandler.class);
            addPermitType(StartCollectingCmdHandler.class);
            addPermitType(StartTransferringCmdHandler.class);
        }

        /**
         * 添加允许的类型
         * <p>
         * 注：一般情况下不希望开发者改变类型限制，但若开发者用特殊需求或其他极其
         * 特殊原因需要添加其他类型指令handler而又希望该类型能被CmdExecuteFlow接受，
         * 则使用反射调用该方法添加对应类型即可
         */
        private <H extends BaseCmdHandler> void addPermitType(Class<H> tClass) {
            if (tClass == null) return;
            if (typeSet == null) {
                typeSet = new HashSet<>();
            }
            typeSet.add(tClass.getSimpleName());
        }

    }

    /**
     * 检查handler链表中是否含有相同类型的handler
     */
    private static void checkHandlerLink(BaseHandler head) {
        List<String> nameList = new ArrayList<>();
        BaseHandler current = head;
        while (current != null) {
            String curLinkName = current.getClass().getSimpleName();
            if (nameList.contains(curLinkName)) {
                throw new IllegalStateException("The handler link has contained this handler node: " + curLinkName);
            }
            nameList.add(curLinkName);
            current = current.getNext();
        }
    }
}
