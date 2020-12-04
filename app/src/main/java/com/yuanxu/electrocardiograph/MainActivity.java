package com.yuanxu.electrocardiograph;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.yuanxu.ecg.CmdFactory;
import com.yuanxu.ecg.ECGManager;
import com.yuanxu.ecg.HandlerManager;
import com.yuanxu.ecg.bean.CmdType;
import com.yuanxu.ecg.bean.UserInfo;
import com.yuanxu.ecg.callback.BaseCallback;
import com.yuanxu.ecg.callback.DeviceTimeCallback;
import com.yuanxu.ecg.callback.DeviceTypeCallback;
import com.yuanxu.ecg.callback.ExecuteCallback;
import com.yuanxu.ecg.cmd.BaseCmd;
import com.yuanxu.ecg.cmd.BindUserIdCmd;
import com.yuanxu.ecg.cmd.QueryStatusCmd;
import com.yuanxu.ecg.cmd.SetDeviceTimeCmd;
import com.yuanxu.ecg.cmd.StartCollectingCmd;
import com.yuanxu.ecg.cmd.StartTransferringCmd;
import com.yuanxu.ecg.handle.cmdhandler.BindUserIdCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.QueryStatusCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.StartCollectingCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.StartTransferringCmdHandler;
import com.yuanxu.ecg.L;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ECGManager
                .getInstance()
                .setLog(true, "ECG")
                .setAutoReconnect(true)
                .init(getApplication());

        findViewById(R.id.tv_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserInfo userInfo = new UserInfo("欧阳修a", false, 20, 170, 65);
                ECGManager
                        .getInstance()
                        .execute("00:81:F9:62:50:65", userInfo, new ExecuteCallback() {
                            @Override
                            public void onSuccess() {//本轮检测任务成功
                                L.e("onSuccess");
                            }

                            @Override
                            public void onFailure(int failCode, String info) {//本轮检测任务失败
                                switch (failCode) {
                                    case BaseCallback.FAIL_BLUETOOTH_NOT_AVAILABLE: //蓝牙不可用
                                        break;
                                    case BaseCallback.FAIL_OTHER:  //其他原因
                                        break;
                                    case ExecuteCallback.FAIL_CONNECTION_ALREADY_ESTABLISHED://本机已连接到其他硬件设备
                                        break;
                                    case ExecuteCallback.FAIL_CONNECTION_START_FAIL://连接未正常开始
                                        break;
                                }
                                L.e("onFailure---" + failCode + "     info=" + info);
                            }

                            @Override
                            public void onProcess(int process, String info) {//检测任务进度
                                switch (process) {
                                    /**
                                     *连接进度
                                     */
                                    case PROCESS_CONNECT_START://连接开始
                                        break;
                                    case PROCESS_CONNECT_FAIL://连接失败
                                        break;
                                    case PROCESS_CONNECTED://已连接
                                        break;
                                    case PROCESS_DISCONNECTED://连接断开
                                        break;

                                    /**
                                     * notify进度
                                     */
                                    case PROCESS_NOTIFY_START://开始notify
                                        break;
                                    case PROCESS_NOTIFY_SUCCESS://notify成功
                                        break;
                                    case PROCESS_NOTIFY_FAIL://notify失败
                                        break;

                                    /**
                                     *  其他如指令收发、数据接收等进度
                                     */
                                    case PROCESS_CMD_SENDING://正在发送相关指令
                                        break;
                                    case PROCESS_DATA_RECEIVING://正在接收硬件发回的心电数据
                                        break;
                                    case PROCESS_IDLE://任务结束（成功或失败），达到空闲状态
                                        break;
                                }
                                L.e("onProcess---" + process + "     info=" + info);
                            }

                            @Override
                            public void onReceivedOriginalData(byte[] data) {//原始心电数据
                                L.e("onReceivedOriginalData");
                            }
                        });

            }
        });

        findViewById(R.id.tv_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //停止当前正在进行的检测（注：改方法并不会断开与设备的连接）
                ECGManager.getInstance().stop();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ECGManager.getInstance().release();
    }
}
