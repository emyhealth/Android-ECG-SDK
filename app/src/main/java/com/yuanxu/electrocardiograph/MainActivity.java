package com.yuanxu.electrocardiograph;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.vmove.signalproc.SignalProc;
import com.yuanxu.ecg.ECGManager;
import com.yuanxu.ecg.bean.UserInfo;
import com.yuanxu.ecg.callback.BaseCallback;
import com.yuanxu.ecg.callback.ExecuteCallback;
import com.yuanxu.ecg.L;
import com.yuanxu.electrocardiograph.view.EcgView;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    int[] ecgData = new int[6];
    private boolean isWaiting = false;
    private byte[] bytes1 = new byte[20];
    private int sub;
    private EcgView ecgView;
    private SignalProc proc;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        proc = new SignalProc();
        proc.Init(100);
        ecgView = findViewById(R.id.ecgView);

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
                        .execute("00:81:F9:62:50:5A", userInfo, new ExecuteCallback() {
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
                                L.d("onProcess---" + process + "     info=" + info);
                            }

                            @Override
                            public void onReceivedOriginalData(byte[] data) {//原始心电数据
                                L.d("onReceivedOriginalData" + Arrays.toString(data));
                                parseRealTimeData(data);
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

    /**
     * 将采集到的实时数据 转换成 ecg Y轴的值
     * 并写入ecgView队列中
     * @param bytes
     */
    public void parseRealTimeData(byte[] bytes) {
        try {
            int i;
            if (bytes.length == 18) {
                for (i = 0; i < 18; i += 3) {
                    ecgData[i / 3] = (bytes[i] & 255) << 16 | (bytes[i + 1] & 255) << 8 | bytes[i + 2] & 255;
                }
                //向ecg视图写入数据
                //EcgView.addEcgData0(getV(ecgData));
                ecgView.addEcgData0(getV(ecgData));
            } else {
                for (i = 0; i < bytes.length; ++i) {
                    bytes1[i] = bytes[i];
                }
                sub = bytes.length;
                isWaiting = true;
            }
            if (isWaiting) {
                for (i = sub; i < 18; ++i) {
                    bytes1[i] = bytes[i - sub];
                }

                for (i = 0; i < 18; i += 3) {
                    ecgData[i / 3] = (bytes1[i] & 255) << 16 | (bytes1[i + 1] & 255) << 8 | bytes1[i + 2] & 255;
                }
                //向ecg视图写入数据
                ecgView.addEcgData0(getV(ecgData));
                isWaiting = false;
            }

        } catch (Exception var6) {
            var6.printStackTrace();
        }
    }

    /**
     * 计算Y轴的值
     * @param ecgData
     * @return
     */
    private int[] getV(int[] ecgData){
        int[] values = new int[ecgData.length];
        for (int i=0;i<ecgData.length;i++){
            int value= proc.Run(ecgData[i]);
            Log.d(this.getClass().getName(), "原始值："+ecgData[i] + "；计算后的值："+value);
            values[i] = value;
        }
        return values;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ECGManager.getInstance().release();
    }
}
