# ECG-SDK
  ECG-SDK是由颐麦科技开发，主要用于简化对接BW-ECG-CHA型号心电检测设备的流程以及数据采集
## 1.添加依赖
```gradle
allprojects {
    repositories {
	    maven { url 'https://jitpack.io' }
    }
}


dependencies {
    implementation 'implementation 'com.github.emyhealth:Android-ECG-SDK:1.0.1''
}
```

## 2.基础用法
### 1).获取ECGManager并初始化
```java
    ECGManager
        .getInstance()
        .setLog(true, "ECG")//是否开启log，默认开启
        .setAutoReconnect(true)//是否自动重连(非主动断开且检测任务未完成时会自动重连)，默认true
        .init(application);
```
### 2).执行检测
```java
    //需要绑定的用户
    UserInfo bindUserInfo = new UserInfo("欧阳修a", false, 20, 170, 65);
    //执行检测
    ECGManager
        .getInstance()
        .execute("00:81:F9:62:50:65", bindUserInfo,
                new ExecuteCallback() {
                    @Override
                    public void onSuccess() {//本轮检测任务成功
                        Log.e("TAG", "onSuccess");
                    }

                    @Override
                    public void onFailure(int failCode, String info) {//本轮检测任务失败
                        switch (failCode) {
                            case BaseCallback.FAIL_BLUETOOTH_NOT_AVAILABLE:
                                //蓝牙不可用
                                break;
                            case BaseCallback.FAIL_OTHER:
                                //其他原因
                                break;
                            case ExecuteCallback.FAIL_CONNECTION_ALREADY_ESTABLISHED:
                                //本机已连接到其他硬件设备
                                break;
                            case ExecuteCallback.FAIL_CONNECTION_START_FAIL:
                                //连接未正常开始
                                break;
                        }
                        Log.e("TAG", "onFailure---" + failCode + "     info=" + info);
                    }

                    @Override
                    public void onProcess(int process, String info) {//检测任务进度
                        switch (process) {
                            case PROCESS_CONNECT_START://连接开始
                                break;
                            case PROCESS_CONNECT_FAIL://连接失败
                                break;
                            case PROCESS_CONNECTED://已连接
                                break;
                            case PROCESS_DISCONNECTED://连接断开
                                break;
                            case PROCESS_NOTIFY_START://开始notify
                                break;
                            case PROCESS_NOTIFY_SUCCESS://notify成功
                                break;
                            case PROCESS_NOTIFY_FAIL://notify失败
                                break;
                            case PROCESS_CMD_SENDING://正在发送相关指令
                                break;
                            case PROCESS_DATA_RECEIVING://正在接收硬件发回的心电数据
                                break;
                            case PROCESS_IDLE://任务结束（成功或失败），达到空闲状态
                                break;
                        }
                        Log.e("TAG", "onProcess---" + process + "     info=" + info);
                    }

                    @Override
                    public void onReceivedOriginalData(byte[] data) {//原始心电检测数据
                        Log.e("TAG", "onReceivedOriginalData");
                    }
                });

```
### 3).停止检测
```java
    //停止当前正在进行的检测（并不会断开与设备的连接）,在执行了任意时间的数据采集
    //后，用该方法结束数据采集，完成本轮检测任务
    ECGManager.getInstance().stop();
```
### 4).断开连接
```java
    //断开当前已连接的设备
    ECGManager.getInstance().disconnect();
```
### 5).释放资源
```java
    //释放，当不再使用该库或因其他原因暂不使用（如内存吃紧且暂不使用该SDK）时调用
    //注意：调用该方法后若要继续使用该库时，则需重新初始化即调用{@link #init(Application)}
    ECGManager.getInstance().release();
```

## 3.进阶用法
### 1)自定义指令处理流顺序
>调用execute()时默认指令发送流程如下：
**查询设备状态—>设置设备时间—>绑定用户—>开始采集—>开始传输**
（注意：查询设备状态后发现是待机状态下才执行后续操作，否则会先发送停止采集指令使设备恢复至待机状态，然后重新执行后续流程）

开发者若需要自己自定义处理流程时，可通过以下方法设置
注意：**查询设备状态、绑定用户、开始采集、开始传输**这几个指令操作不可省略
```java
    /**
     *这里假设我们不想设置设备时间，且想让设备先绑定用户再执行后续操作，即
     *绑定用户—>查询设备状态—>开始采集—>开始传输
     */

    //1.创建指令handler
    BindUserIdCmdHandler h1 = new BindUserIdCmdHandler(new BindUserIdCmd());
    QueryStatusCmdHandler h2 = new QueryStatusCmdHandler(new QueryStatusCmd());
    StartCollectingCmdHandler h3 = new StartCollectingCmdHandler(new StartCollectingCmd());
    StartTransferringCmdHandler h4 = new StartTransferringCmdHandler(new StartTransferringCmd());

    //2.设置当本级handler能够处理且处理完硬件响应后让本级的下级Handler发送命令。
    //必须设置，因hanlder流是一个链表，只有设置该值为true，下一级才能发送指令（详
    //见方法注释说明）
    h1.setNextHandlerSendCmdWhenCanHandle(true);
    h2.setNextHandlerSendCmdWhenCanHandle(true);
    h3.setNextHandlerSendCmdWhenCanHandle(true);

    //3.按顺序设置自定义处理流（注意：next()方法中指令handler类型被限制为查询设
    //备状态、绑定用户等5个，一般情况下传入不被接受的类型时会抛出异常，若因极其
    //特殊原因开发者想添加其他类型的handler，查看CmdExecuteFlow#addPermitType(Class)说明
    ECGManager.getInstance().setCustomCmdExecuteFlow(HandlerManager.CmdExecuteFlow
        .newInstance()
        .next(h1)
        .next(h2)
        .next(h3)
        .next(h4));
```
### 2)仅使用该SDK提供的指令
当开发者不想使用本SDK执行检测，仅想使用本库提供的指令时，直接new相关指令或使用以下指令工厂得到相应指令即可
```java
    //获取指令工厂
    CmdFactory factory = CmdFactory.newInstance();

    //生成指令
    BaseCmd queryStatus = factory.createCmd(CmdType.QUERY_STATUS);
    BaseCmd queryDeviceTime = factory.createCmd(CmdType.QUERY_DEVICE_TIME);

    /**
     * 一些指令需要设置参数的，记得转换指令类型后设置参数，如绑定用户、设
     * 置设备时间、开始采集等指令需要设置一些参数
     */
    BindUserIdCmd bindUserId = ((BindUserIdCmd) factory.createCmd(CmdType.BIND_USER_ID))
        .setName("欧阳修a");
        .setWeight(60);
        .setHeight(181);
        .setGender(false);
        .setAge(38);

    SetDeviceTimeCmd setDeviceTime = (SetDeviceTimeCmd) factory.createCmd(CmdType.SET_DEVICE_TIME);
    StartCollectingCmd collecting = (StartCollectingCmd) factory.createCmd(CmdType.START_COLLECTING);
    setDeviceTime.setDeviceTimestamp(System.currentTimeMillis());//设置设备时间
    collecting.setTimestamp(System.currentTimeMillis());//设置采集时间
```
### 3)本SDK已包含BLE库[EasyBle](https://github.com/Ficat/EasyBle)，可无需依赖直接使用
### 其他api
|Method|Description|
|------|-----------|
|**queryDeviceTime**(DeviceTimeCallback callback)|查询设备时间|
|**queryDeviceType**(DeviceTypeCallback callback)|查询设备型号|
|getBleManager()|获取SDK内部的ble管理器，可通过获取的BleManager进行扫描、连接等|




