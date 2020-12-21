package com.yuanxu.ecg;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.ficat.easyble.BleDevice;
import com.ficat.easyble.BleManager;
import com.ficat.easyble.gatt.bean.CharacteristicInfo;
import com.ficat.easyble.gatt.bean.ServiceInfo;
import com.ficat.easyble.gatt.callback.BleConnectCallback;
import com.ficat.easyble.gatt.callback.BleNotifyCallback;
import com.ficat.easyble.gatt.callback.BleWriteCallback;
import com.yuanxu.ecg.bean.UserInfo;
import com.yuanxu.ecg.callback.BaseCallback;
import com.yuanxu.ecg.callback.DeviceTimeCallback;
import com.yuanxu.ecg.callback.DeviceTypeCallback;
import com.yuanxu.ecg.callback.ExecuteCallback;
import com.yuanxu.ecg.cmd.BindUserIdCmd;
import com.yuanxu.ecg.cmd.SetDeviceTimeCmd;
import com.yuanxu.ecg.cmd.StartCollectingCmd;
import com.yuanxu.ecg.cmd.StopCollectingCmd;
import com.yuanxu.ecg.handle.cmdhandler.QueryDeviceTimeCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.QueryDeviceTypeCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.StopCollectingCmdHandler;
import com.yuanxu.ecg.handle.cmdhandler.StopTransferringCmdHandler;
import com.yuanxu.ecg.utils.ByteUtils;
import com.yuanxu.ecg.utils.DeviceInfoParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ECGManager {
    private static final String SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_UUID_V1 = "0000fff1-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_UUID_V2 = "0000fff2-0000-1000-8000-00805f9b34fb";

    private BleManager manager;
    private ExecuteCallback executeCallback;
    private DeviceTimeCallback deviceTimeCallback;
    private DeviceTypeCallback deviceTypeCallback;
    private BleDevice curConnectedDevice;//当前已连接的设备
    private int currentProcess = ExecuteCallback.PROCESS_IDLE;//当前进度，默认空闲
    private boolean autoReconnect = true;//当连接非用户主动断开时是否自动重连，默认为true
    private boolean disconnectByUser;//是否由用户主动断开


    /**
     * 硬件版本map，key为address，value表示是否为旧版本心电贴设备
     * <p>
     * 注：旧版本的通知特征通道uuid为CHARACTERISTIC_UUID_V1为notify，写入特征
     * 通道uuid为CHARACTERISTIC_UUID_V2；新版本反之
     */
    private Map<String, Boolean> versionMap;

    /**
     * 消息中心MsgCenter的监听器回调
     */
    private MsgCenter.MsgListener msgListener = new MsgCenter.MsgListener() {
        @Override
        public void onSendCmd(String hexCmd) {
            if (curConnectedDevice == null) {
                //本SDK目前仅支持单连接,故直接使用连接的第一个设备即可
                List<BleDevice> list = manager.getConnectedDevices();
                if (list.size() > 0) {
                    curConnectedDevice = list.get(0);
                }
            }
            write(curConnectedDevice, ByteUtils.hexStr2Bytes(hexCmd));
        }

        @Override
        public void onCmdError() {
            //指令有误导致任务异常结束，重置进度为空闲，防止下次执行任务时execute()时
            //进度判断失效
            updateProcess(ExecuteCallback.PROCESS_IDLE, "指令有误，请检查后重试");
            if (executeCallback != null) {
                executeCallback.onFailure(BaseCallback.FAIL_OTHER, "指令有误（长度不足、命令字不存在、逻辑错误）等");
            }
            //任务结束，清理所有callback
            clearAllCallback();
        }

        @Override
        public void onCmdExecuteFail(String hexStrResponse) {
            if (!TextUtils.isEmpty(hexStrResponse)) {
                String cmPrefix = hexStrResponse.substring(0, 4);
                if (cmPrefix.equalsIgnoreCase(StopCollectingCmd.CMD_PREFIX)) {
                    return;
                }
            }
            //指令执行失败导致任务异常结束，重置进度为空闲，防止下次执行任务时execute()
            //时进度判断失效
            updateProcess(ExecuteCallback.PROCESS_IDLE, "指令执行失败，错误指令：" + hexStrResponse);
            if (executeCallback != null) {
                executeCallback.onFailure(BaseCallback.FAIL_OTHER, "指令(" + hexStrResponse +
                        ")执行失败（索要数据不存在、当前时刻不适合执行此命令、设备错误）");
            }
            //任务结束，清理所有callback
            clearAllCallback();
        }

        @Override
        public void onStopTransferringSuccess() {
            //只有在当前状态为数据接收中时，停止传输成功才代表数据已经采集完成，因
            //开发者可主动调用stop()或在设备状态非空闲时会自动先stop()再重试
            if (currentProcess == ExecuteCallback.PROCESS_DATA_RECEIVING) {
                if (executeCallback != null) {
                    executeCallback.onSuccess();
                }
                //任务成功结束，更新进度为空闲
                updateProcess(ExecuteCallback.PROCESS_IDLE, "停止传输命令发送成功，数据传输终止，达到空闲状态");
            }
        }

        @Override
        public void onDeviceNotIdle(String status) {
            //硬件非空闲状态下停止传输与采集
            stop();
            //延时T秒后重新执行命令。若在发送停止传输和停止采集指令成功后调用重新执行命令，
            //则需相应判断（如是否为设备非空闲才调用stop指令），此处简化为仅做相应延时即可
            new Handler(Looper.getMainLooper())
                    .postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            retryExecuteCmd();
                        }
                    }, 1200);
        }

        @Override
        public void onDeviceType(String hexDeviceType) {
            if (deviceTypeCallback != null) {
                String type = DeviceInfoParser.parseDeviceType(hexDeviceType);
                deviceTypeCallback.onDeviceType(type);
            }
        }

        @Override
        public void onDeviceTime(String hexDeviceTime) {
            if (deviceTimeCallback != null) {
                String result = DeviceInfoParser.parseDeviceTime(hexDeviceTime);
                deviceTimeCallback.onDeviceTime(result);
            }
        }

        @Override
        public void onReceivedHeartData(String hexData) {
            //当前进度不为空闲且不为正在接收时更改进度。防止顺序错乱（如发送停止cmd且currentProcess已更新
            //为空闲，但可能出现onReceivedHeartData因次数较多随后再次被回调造成进度被修改为接收中）
            if (currentProcess != ExecuteCallback.PROCESS_IDLE && currentProcess != ExecuteCallback.PROCESS_DATA_RECEIVING) {
                updateProcess(ExecuteCallback.PROCESS_DATA_RECEIVING, "接收硬件数据中");
            }
            if (executeCallback != null) {
                executeCallback.onReceivedOriginalData(ByteUtils.hexStr2Bytes(hexData));
            }
        }
    };

    private ECGManager() {

    }

    public static ECGManager getInstance() {
        return ECGManagerHolder.sManager;
    }

    private static final class ECGManagerHolder {
        static final ECGManager sManager = new ECGManager();
    }

    public ECGManager init(Application application) {
        if (manager != null) {
            L.d("ECGManager已初始化，无需重复调用");
            return this;
        }
        checkNotNull(application, Application.class);
        //准备BleManager并初始化
        manager = BleManager
                .getInstance()
                .setConnectionOptions(BleManager.ConnectOptions
                        .newInstance()
                        .connectTimeout(12000))//扫描连接超时时间默认12秒
                .init(application);
        //handler管理器初始化
        HandlerManager.getInstance().init();
        //初始化消息中心
        MsgCenter.getInstance().init();
        MsgCenter.getInstance().addMsgListener(msgListener);
        versionMap = new HashMap<>();
        return this;
    }

    public ECGManager setLog(boolean enable, String tag) {
        L.SHOW_LOG = enable;
        if (!TextUtils.isEmpty(tag)) {
            L.TAG = tag;
        }
        return this;
    }

    public ECGManager setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
        return this;
    }

    public void execute(final String address, UserInfo userInfo, ExecuteCallback callback) {
        if (manager == null) {
            throw new IllegalStateException("You should call init() first");
        }
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            throw new IllegalArgumentException("Invalid address:" + address);
        }
        checkNotNull(userInfo, UserInfo.class);
        checkNotNull(callback, ExecuteCallback.class);
        if (!BleManager.isBluetoothOn()) {
            callback.onFailure(BaseCallback.FAIL_BLUETOOTH_NOT_AVAILABLE, "蓝牙尚未打开，请打开蓝牙后重试");
            return;
        }
        if (hasConnectedToOtherDevice(address)) {
            callback.onFailure(ExecuteCallback.FAIL_CONNECTION_ALREADY_ESTABLISHED, "本机已连接到其他硬件设备，请先断开连接后重试");
            return;
        }
        if (manager.isConnecting(address)) {
            return;
        }
        if (manager.isConnected(address)) {
            //当前设备处于连接且空闲状态，直接执行新的检测任务
            if (currentProcess == ExecuteCallback.PROCESS_IDLE) {
                disconnectByUser = false;
                executeCallback = callback;
                executeCmd(userInfo);
            } else {
                L.w("当前存在其他尚未结束的检测任务");
            }
        } else {
            disconnectByUser = false;
            executeCallback = callback;
            //1.未连接状态下
            //2.上轮检测任务时出现连接断开后且无法成功重连（如用户禁止重连或用户手动关闭
            //了蓝牙）时，任务已异常结束。
            //以上2中情况下当前任务进度是非空闲的（即状态为已断开），重置任务进度
            currentProcess = ExecuteCallback.PROCESS_IDLE;
            //未连接状态，连接并执行任务
            connect(address, userInfo);
        }
    }

    /**
     * 查询设备时间
     */
    public void queryDeviceTime(DeviceTimeCallback callback) {
        checkNotNull(callback, DeviceTimeCallback.class);
        if (manager == null) {
            throw new IllegalStateException("You should call init() first");
        }
        if (!BleManager.isBluetoothOn()) {
            callback.onFailure(BaseCallback.FAIL_BLUETOOTH_NOT_AVAILABLE, "蓝牙尚未打开，请打开蓝牙并连接成功后重试");
            return;
        }
        if (curConnectedDevice == null) {
            callback.onFailure(BaseCallback.FAIL_OTHER, "无已连接的硬件设备，请确保有连接成功的设备后重试");
            return;
        }
        deviceTimeCallback = callback;
        HandlerManager
                .getInstance()
                .getHandler(QueryDeviceTimeCmdHandler.class)
                .sendCmdToDevice();
    }

    /**
     * 查询设备类型
     */
    public void queryDeviceType(DeviceTypeCallback callback) {
        checkNotNull(callback, DeviceTypeCallback.class);
        if (manager == null) {
            throw new IllegalStateException("You should call init() first");
        }
        if (!BleManager.isBluetoothOn()) {
            callback.onFailure(BaseCallback.FAIL_BLUETOOTH_NOT_AVAILABLE, "蓝牙尚未打开，请打开蓝牙并连接成功后重试");
            return;
        }
        if (curConnectedDevice == null) {
            callback.onFailure(BaseCallback.FAIL_OTHER, "无已连接的硬件设备，请确保有连接成功的设备后重试");
            return;
        }
        deviceTypeCallback = callback;
        HandlerManager
                .getInstance()
                .getHandler(QueryDeviceTypeCmdHandler.class)
                .sendCmdToDevice();
    }

    public void setCustomCmdExecuteFlow(HandlerManager.CmdExecuteFlow flow) {
        if (manager == null) {
            throw new IllegalStateException("You should call init() first");
        }
        if (flow == null) {
            return;
        }
        HandlerManager
                .getInstance()
                .setCustomCmdExecuteFlow(flow);
    }

    /**
     * 停止本轮测试采集
     * <p>
     * 注意：并不断开连接
     */
    public void stop() {
        if (manager.getConnectedDevices().size() <= 0) {
            return;
        }
        //若停止传输指令handler的next不是停止采集指令handler，则
        //直接发送停止采集指令；否则先发送停止传输指令后再让停止传
        //输指令handler收到硬件回复后自动发送停止采集指令
        StopTransferringCmdHandler handler = HandlerManager
                .getInstance()
                .getHandler(StopTransferringCmdHandler.class);
        if (handler.getNext() instanceof StopCollectingCmdHandler) {
            handler.setNextHandlerSendCmdWhenCanHandle(true);
            handler.sendCmdToDevice();
        } else {
            HandlerManager
                    .getInstance()
                    .getHandler(StopCollectingCmdHandler.class)
                    .sendCmdToDevice();
        }
    }

    /**
     * 断开当前所有已连接设备
     */
    public void disconnect() {
        if (manager != null) {
            manager.disconnectAll();
        }
        disconnectByUser = true;
        curConnectedDevice = null;
        //确保断开正在进行中的连接时callback也会被正常clear
        clearAllCallback();
    }

    /**
     * 释放资源，当不再使用该库或因其他原因暂不使用（如内存吃紧且暂不使用该SDK）时调用，若
     * 调用该方法后要继续使用该库，则需重新调用{@link #init(Application)}
     */
    public void release() {
        if (manager == null) {
            return;
        }
        disconnect();
        manager.destroy();
        MsgCenter.getInstance().release();
        HandlerManager.getInstance().release();
        versionMap.clear();
        versionMap = null;
        manager = null;
        curConnectedDevice = null;
        clearAllCallback();
        currentProcess = ExecuteCallback.PROCESS_IDLE;
        autoReconnect = true;
    }

    /**
     * 获取BleManager，通过获取该BleManager，开发者可配置蓝牙相关参数（如扫描、连接参数）
     * 或其他功能
     * <p>
     * 注意：若尚未调用{@link #init(Application)}，则会返回null
     */
    public BleManager getBleManager() {
        return manager;
    }

    private void connect(final String address, final UserInfo userInfo) {
        manager.connect(address, new BleConnectCallback() {
            @Override
            public void onStart(boolean startConnectSuccess, String info, BleDevice device) {
                L.d("开始连接=" + startConnectSuccess + "    info=" + info);
                if (startConnectSuccess) {
                    updateProcess(ExecuteCallback.PROCESS_CONNECT_START, "连接开始，" + info);
                } else {
                    if (executeCallback != null) {
                        executeCallback.onFailure(ExecuteCallback.FAIL_CONNECTION_START_FAIL, "连接无法正常开始，" + info);
                        clearAllCallback();
                    }
                }
            }

            @Override
            public void onConnected(BleDevice device) {
                L.d("已连接");
                updateProcess(ExecuteCallback.PROCESS_CONNECTED, "已连接至设备(" + device.address + ")");
                setNotify(device, userInfo);
                curConnectedDevice = device;
            }

            @Override
            public void onDisconnected(String info, int status, BleDevice device) {
                L.d("连接断开    info=" + info + "    status=" + status);
                curConnectedDevice = null;
                int preProcess = currentProcess;
                updateProcess(ExecuteCallback.PROCESS_DISCONNECTED, "连接断开，" + info);
                //主动断开或空闲状态下断开后不再执行重连
                if (disconnectByUser || preProcess == ExecuteCallback.PROCESS_IDLE) {
                    L.i(disconnectByUser ? "已主动断开连接" : "设备空闲状态下连接断开，不再执行重连");
                    //清理callback
                    clearAllCallback();
                    return;
                }
                if (autoReconnect) {//非空闲状态（如数据尚未接收完成等）且允许重连时
                    //1.清空原接收到的数据（当前版本SDK暂无）
                    //2.重连重新执行检测任务
                    manager.connect(address, this);
                } else {//非空闲状态且不允许重连时
                    if (executeCallback != null) {
                        executeCallback.onFailure(BaseCallback.FAIL_OTHER, "任务执行过程中连接异常断开，且您不允许自动重连，本次检测任务失败");
                    }
                    clearAllCallback();
                }
            }

            @Override
            public void onFailure(int failCode, String info, BleDevice device) {
                L.d(failCode == BleConnectCallback.FAIL_CONNECT_TIMEOUT ? "连接超时" : "连接失败");
                updateProcess(ExecuteCallback.PROCESS_CONNECT_FAIL, info);
                curConnectedDevice = null;
                if (autoReconnect && !disconnectByUser) {
                    manager.connect(address, this);
                }
            }
        });
    }

    private void setNotify(BleDevice device, final UserInfo userInfo) {
        distinguishVersion(device);
        Boolean b = versionMap.get(device.address);
        boolean oldVersion = b == null ? false : b;
        updateProcess(ExecuteCallback.PROCESS_NOTIFY_START, "notify开始");
        manager.notify(device, SERVICE_UUID, oldVersion ? CHARACTERISTIC_UUID_V1 : CHARACTERISTIC_UUID_V2,
                new BleNotifyCallback() {
                    @Override
                    public void onCharacteristicChanged(byte[] data, BleDevice device) {
                        HandlerManager
                                .getInstance()
                                .getHandlerHead()
                                .handleResponse(ByteUtils.bytes2HexStr(data));
                    }

                    @Override
                    public void onNotifySuccess(String notifySuccessUuid, BleDevice device) {
                        L.d("notify成功  notifyUuid=" + notifySuccessUuid);
                        updateProcess(ExecuteCallback.PROCESS_NOTIFY_SUCCESS, "notify成功");
                        executeCmd(userInfo);
                    }

                    @Override
                    public void onFailure(int failCode, String info, BleDevice device) {
                        L.d("notify失败  info=" + info);
                        updateProcess(ExecuteCallback.PROCESS_NOTIFY_FAIL, info);
                    }
                });
    }

    private void executeCmd(UserInfo userInfo) {
        HandlerManager.CmdExecuteFlow flow = HandlerManager
                .getInstance()
                .getCmdExecuteFlow();
        //设置某些指令所需信息
        BindUserIdCmd bindUserId = flow.getCmd(BindUserIdCmd.class);
        SetDeviceTimeCmd setDeviceTime = flow.getCmd(SetDeviceTimeCmd.class);
        StartCollectingCmd collecting = flow.getCmd(StartCollectingCmd.class);
        if (bindUserId != null) {
            bindUserId.setName(userInfo.getName());
            bindUserId.setAge(userInfo.getAge());
            bindUserId.setGender(userInfo.isFemale());
            bindUserId.setHeight(userInfo.getHeight());
            bindUserId.setWeight(userInfo.getWeight());
        }
        long timestamp = System.currentTimeMillis();
        if (setDeviceTime != null) {
            setDeviceTime.setDeviceTimestamp(timestamp);
        }
        if (collecting != null) {
            collecting.setTimestamp(timestamp);
        }
        L.d("开始依命令执行流发送命令");
        //根据指令执行流开始发送命令
        flow.getHead().sendCmdToDevice();
        updateProcess(ExecuteCallback.PROCESS_CMD_SENDING, "命令发送中");
    }

    /**
     * 当检查设备状态时发现设备不为空闲（待机）状态，则会重新发送指令流
     */
    private void retryExecuteCmd() {
        HandlerManager
                .getInstance()
                .getCmdExecuteFlow()
                .getHead()
                .sendCmdToDevice();
        L.d("重新依命令执行流发送命令");
        updateProcess(ExecuteCallback.PROCESS_CMD_SENDING, "命令发送中");
    }

    private void write(BleDevice device, final byte[] data) {
        if (manager == null || device == null || data == null
                || !manager.isConnected(device.address)) {
            return;
        }
        distinguishVersion(device);
        if (!versionMap.containsKey(device.address) || versionMap.get(device.address) == null) {
            L.d("写入时无法判断当前设备型号进而无法区分notify和write特征uuid");
            return;
        }
        Boolean b = versionMap.get(device.address);
        boolean oldVersion = b == null ? false : b;
        manager.write(device, SERVICE_UUID, oldVersion ? CHARACTERISTIC_UUID_V2 : CHARACTERISTIC_UUID_V1,
                data, new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(byte[] data, BleDevice device) {
                    }

                    @Override
                    public void onFailure(int failCode, String info, BleDevice device) {
                        L.d("写入失败：" + ByteUtils.bytes2HexStr(data) + "   detail=" + info);
                    }
                });
    }

    private void updateProcess(int process, String info) {
        currentProcess = process;
        if (executeCallback != null) {
            executeCallback.onProcess(process, info);
        }
    }

    /**
     * 通过Characteristic的属性区分新旧设备
     * <p>
     * 注：
     * 1.旧版本的通知特征通道uuid为CHARACTERISTIC_UUID_V1为notify，写入特征通道uuid为
     * CHARACTERISTIC_UUID_V2；新版本反之
     * 2.目前使用该方式主要因两款设备已进入市场使用，而硬件即为提供型号区分且暂无法修改
     */
    private void distinguishVersion(BleDevice device) {
        if (manager == null || !manager.isConnected(device.address)
                || versionMap.containsKey(device.address)) {
            return;
        }
        Map<ServiceInfo, List<CharacteristicInfo>> info = manager.getDeviceServices(device);
        for (Map.Entry<ServiceInfo, List<CharacteristicInfo>> e : info.entrySet()) {
            if (!e.getKey().uuid.equals(SERVICE_UUID)) {
                continue;
            }
            List<CharacteristicInfo> list = e.getValue();
            for (CharacteristicInfo i : list) {
                if (i.uuid.equals(CHARACTERISTIC_UUID_V1)) {
                    boolean oldVersion = i.notify;
                    versionMap.put(device.address, oldVersion);
                    return;
                }
            }
        }
    }

    private void clearAllCallback() {
        executeCallback = null;
        deviceTimeCallback = null;
        deviceTypeCallback = null;
    }

    /**
     * 当前设备是否已连接除targetAddress外的其他设备
     *
     * @param targetAddress 目标设备
     */
    private boolean hasConnectedToOtherDevice(String targetAddress) {
        if (manager == null || manager.getConnectedDevices().size() <= 0) {
            return false;
        }
        for (BleDevice d : manager.getConnectedDevices()) {
            if (!d.address.equals(targetAddress)) {
                return true;
            }
        }
        return false;
    }

    private void checkNotNull(Object object, Class<?> clasz) {
        if (object == null) {
            String claszSimpleName = clasz.getSimpleName();
            throw new IllegalArgumentException(claszSimpleName + " is null");
        }
    }

}
