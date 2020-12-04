package com.yuanxu.ecg.callback;

public interface ExecuteCallback extends BaseCallback {
    /**
     * 进度相关常量
     */
    //连接相关
    int PROCESS_CONNECT_START = 201;//连接开始
    int PROCESS_CONNECT_FAIL = 202;//连接失败
    int PROCESS_CONNECTED = 203;//已连接
    int PROCESS_DISCONNECTED = 204;//连接断开

    //notify相关
    int PROCESS_NOTIFY_START = 205;//开始notify
    int PROCESS_NOTIFY_SUCCESS = 206;//notify成功
    int PROCESS_NOTIFY_FAIL = 207;//notify失败

    //其他
    int PROCESS_CMD_SENDING = 208;//正在发送相关指令
    int PROCESS_DATA_RECEIVING = 209;//正在接收硬件发回的心电数据
    int PROCESS_IDLE = 300;//任务结束，达到空闲状态


    //注：当前版本暂不提供数据分析、报告生成功能，故暂时注释掉
//    int PROCESS_DATA_RECEIVE_FINISH = 210;//心电数据接收结束
//    int PROCESS_DATA_ANALYSIS = 211;//数据分析中
//    int PROCESS_DATA_REPORT_GENERATING = 212;//报告生成中


    /**
     * 失败常量
     */
    int FAIL_CONNECTION_ALREADY_ESTABLISHED = 301;//连接被占用（连接早已建立）
    int FAIL_CONNECTION_START_FAIL = 302;//连接开始失败


    /**
     * 成功
     */
    void onSuccess();

    /**
     * 进度
     *
     * @param process 进度
     */
    void onProcess(int process, String info);

    /**
     * 原始心电数据
     */
    void onReceivedOriginalData(byte[] data);
}
