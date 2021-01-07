package org.haobtc.onekey.manager;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.chaquo.python.Kwarg;
import com.chaquo.python.PyObject;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.orhanobut.logger.Logger;

import org.greenrobot.eventbus.EventBus;
import org.haobtc.onekey.BuildConfig;
import org.haobtc.onekey.R;
import org.haobtc.onekey.activities.base.MyApplication;
import org.haobtc.onekey.bean.BalanceInfo;
import org.haobtc.onekey.bean.CreateWalletBean;
import org.haobtc.onekey.bean.CurrentAddressDetail;
import org.haobtc.onekey.bean.FindOnceWalletEvent;
import org.haobtc.onekey.bean.HardwareFeatures;
import org.haobtc.onekey.bean.MakeTxResponseBean;
import org.haobtc.onekey.bean.PyResponse;
import org.haobtc.onekey.bean.TemporaryTxInfo;
import org.haobtc.onekey.bean.TransactionInfoBean;
import org.haobtc.onekey.constant.Constant;
import org.haobtc.onekey.constant.PyConstant;
import org.haobtc.onekey.event.CreateSuccessEvent;
import org.haobtc.onekey.event.ExitEvent;
import org.haobtc.onekey.event.RefreshEvent;
import org.haobtc.onekey.exception.HardWareExceptions;
import org.haobtc.onekey.onekeys.HomeOneKeyActivity;
import org.haobtc.onekey.ui.base.BaseActivity;
import org.haobtc.onekey.utils.Daemon;
import org.haobtc.onekey.utils.Global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import dr.android.utils.LogUtil;

import static org.haobtc.onekey.activities.service.CommunicationModeSelector.protocol;

/**
 * @author liyan
 */
public final class PyEnv {

    public static PyObject sBle, sCustomerUI, sNfc, sUsb, sBleHandler, sNfcHandler, sBleTransport,
            sNfcTransport, sUsbTransport, sProtocol, sCommands;
    private static ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =  new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("PyEnv-schedule-%d").build(), new ThreadPoolExecutor.DiscardPolicy());
    public static ListeningScheduledExecutorService  mExecutorService = MoreExecutors.listeningDecorator(scheduledThreadPoolExecutor);

    static {
        sNfc = Global.py.getModule(PyConstant.TREZORLIB_TRANSPORT_NFC);
        sBle = Global.py.getModule(PyConstant.TREZORLIB_TRANSPORT_BLUETOOTH);
        sUsb = Global.py.getModule(PyConstant.TREZORLIB_TRANSPORT_ANDROID_USB);
        sProtocol = Global.py.getModule(PyConstant.TREZORLIB_TRANSPORT_PROTOCOL);
        sBleHandler = sBle.get(PyConstant.BLUETOOTH_HANDLER);
        sNfcHandler = sNfc.get(PyConstant.NFC_HANDLE);
        sUsbTransport = sUsb.get(PyConstant.ANDROID_USB_TRANSPORT);
        sNfcTransport = sNfc.get(PyConstant.NFC_TRANSPORT);
        sBleTransport = sBle.get(PyConstant.BLUETOOTH_TRANSPORT);
        sCustomerUI = Global.py.getModule(PyConstant.TREZORLIB_CUSTOMER_UI).get(PyConstant.CUSTOMER_UI);
    }

    public static void init(@NonNull Context context) {
        if (BuildConfig.net_type.equals(context.getString(R.string.TestNet))) {
            setTestNet();
        } else if (BuildConfig.net_type.equals(context.getString(R.string.RegTest))) {
            setRegNet();
        }
        Daemon.initCommands();
        sCommands = Daemon.commands;
        // 加载钱包信息
        sCommands.callAttr(PyConstant.LOAD_ALL_WALLET);
        loadLocalWalletInfo(context);
    }

    public static void cancelPinInput() {
        sCustomerUI.put(PyConstant.USER_CANCEL, 1);
    }

    /**
     * 设置硬件回调句柄
     */
    public static void setHandle(HardwareCallbackHandler handle) {
        sCustomerUI.put(PyConstant.UI_HANDLER, handle);
    }

    /**
     * 设置当前网络类型为公有测试网络
     */
    private static void setTestNet() {
        setNetType(PyConstant.SET_TEST_NETWORK);
    }

    /**
     * 设置当前网络类型为回归测试网络(私链)
     */
    private static void setRegNet() {
        setNetType(PyConstant.SET_REG_TEST_NETWORK);
    }

    private static void setNetType(String type) {
        Global.py.getModule(PyConstant.ELECTRUM_CONSTANTS_MODULE).callAttr(type);
    }

    /**
     * 提醒后台任务线程结束等待
     */
    public static void sNotify() {
        sProtocol.callAttr(PyConstant.NOTIFICATION);
    }

    public static void bleCancel() {
        sBle.put(PyConstant.IS_CANCEL, true);
    }

    public static void nfcCancel() {
        sNfc.put(PyConstant.IS_CANCEL, true);
    }

    public static void usbCancel() {
        sUsb.put(PyConstant.IS_CANCEL, true);
    }

    public static void bleReWriteResponse(String response) {
        sBleHandler.put(PyConstant.RESPONSE, response);
    }

    /**
     * 启用蓝牙，并做相关初始化准备
     */
    public static void bleEnable(BleDevice device, BleWriteCallback<BleDevice> mWriteCallBack) {
        sBleTransport.put(PyConstant.ENABLED, true);
        sNfcTransport.put(PyConstant.ENABLED, false);
        sUsbTransport.put(PyConstant.ENABLED, false);
        sBleHandler.put(PyConstant.BLE, Ble.getInstance());
        sBleHandler.put(PyConstant.BLE_DEVICE, device);
        sBleHandler.put(PyConstant.CALL_BACK, mWriteCallBack);
        sBle.put(PyConstant.WRITE_SUCCESS, true);
    }

    /**
     * 启用NFC，并做相关初始化准备
     */
    public static void nfcEnable() {
        sNfcTransport.put(PyConstant.ENABLED, true);
        sBleTransport.put(PyConstant.ENABLED, false);
        sUsbTransport.put(PyConstant.ENABLED, false);
    }

    /**
     * 启用USB，并做相关初始化准备
     */
    public static void usbEnable() {
        sUsbTransport.put(PyConstant.ENABLED, true);
        sBleTransport.put(PyConstant.ENABLED, false);
        sNfcTransport.put(PyConstant.ENABLED, false);
    }

    /**
     * 撤销正在执行的通信
     */
    public static void cancelAll() {
        bleCancel();
        nfcCancel();
        usbCancel();
        sNotify();
    }

    /**
     * 回传PIN码
     */
    public static void setPin(String pin) {
        sCustomerUI.put(PyConstant.PIN, pin);
    }

    public static void cancelRecovery() {
        sCommands.callAttr(PyConstant.CANCEL_RECOVERY);
    }

    /**
     * 获取硬件设备信息
     */

    public static void getFeature(Context context, Consumer<PyResponse<HardwareFeatures>> callback) {
        Logger.d("get feature");
        PyResponse<HardwareFeatures> response = new PyResponse<>();
        ListenableFuture<String> listenableFuture = Futures.withTimeout(mExecutorService.submit(
                () -> Daemon.commands.callAttr(PyConstant.GET_FEATURE, MyApplication.getInstance().getDeviceWay()).toString()),
                5, TimeUnit.SECONDS, mExecutorService);
        Futures.addCallback(listenableFuture, new FutureCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Logger.json(result);
                HardwareFeatures features = dealWithConnectedDevice(context, HardwareFeatures.objectFromData(result));
                response.setResult(features);
                callback.accept(response);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Logger.e(t.getMessage());
                if (sBle != null) {
                    cancelAll();
                }
                if (t instanceof TimeoutException) {
                    response.setErrors(context.getString(R.string.get_hard_msg_error));
                } else {
                    response.setErrors(t.getMessage());
                }
                callback.accept(response);
            }
        }, mExecutorService);
    }

    /**
     * 处理当前链接硬件的设备信息并保存
     * 1. 已激活，并且有备份信息或已验证的不能直接覆盖
     * 2. 除上中情形，直接覆盖原有信息
     */
    private static HardwareFeatures dealWithConnectedDevice(Context context, HardwareFeatures features) {
        if (features.isInitialized()) {
            HardwareFeatures old;
            String backupMessage = "";
            boolean isVerified = false;
            if (PreferencesManager.contains(context, Constant.DEVICES, features.getDeviceId())) {
                old = HardwareFeatures.objectFromData((String) PreferencesManager.get(context, Constant.DEVICES, features.getDeviceId(), ""));
                backupMessage = old.getBackupMessage();
                isVerified = old.isVerify();
            }
            if (isVerified) {
                features.setVerify(true);
            }
            if (!Strings.isNullOrEmpty(backupMessage)) {
                features.setBackupMessage(backupMessage);
            }
        } else if (features.isBootloaderMode()) {
            features.setBleName(BleManager.currentBleName);
            return features;
        }
        PreferencesManager.put(context, Constant.DEVICES, features.getDeviceId(), features.toString());
        return features;
    }

    /**
     * 通过xpub创建钱包
     */
    public static String createWallet(BaseActivity activity, String walletName, int m, int n, String xPubs) {
        String walletInfo = null;
        try {
            walletInfo = sCommands.callAttr(PyConstant.CREATE_WALLET_BY_XPUB, walletName, m, n, xPubs).toString();
            String name = CreateWalletBean.objectFromData(walletInfo).getWalletInfo().get(0).getName();
            EventBus.getDefault().post(new CreateSuccessEvent(name));
            return name;
        } catch (Exception e) {
            e.printStackTrace();
            String message = e.getMessage();
            assert message != null;
            if (HardWareExceptions.WALLET_ALREADY_EXIST.getMessage().equals(message)) {
                activity.showToast(R.string.changewalletname);
            } else {
                if (message.contains(HardWareExceptions.WALLET_ALREADY_EXIST_1.getMessage())) {
                    String haveWalletName = message.substring(message.indexOf("name=") + 5);
                    activity.showToast(activity.getString(R.string.xpub_have_wallet) + haveWalletName);
                }
            }
            activity.finish();
        }
        return null;
    }

    /**
     * 通过xpub恢复钱包
     */
    public static void recoveryWallet(BaseActivity activity, String xPubs, boolean hd) {
        List<BalanceInfo> infos = new ArrayList<>();
        try {
            mExecutorService.execute(() -> {
                String walletsInfo = null;
                try {
                    walletsInfo = sCommands.callAttr(PyConstant.CREATE_WALLET_BY_XPUB, "", 1, 1, xPubs, new Kwarg("hd", hd)).toString();
                } catch (Exception ignored) {
                    return;
                }
                CreateWalletBean.objectFromData(walletsInfo).getDerivedInfo().forEach(derivedInfoBean -> {
                    BalanceInfo info = new BalanceInfo();
                    info.setBalance(derivedInfoBean.getBlance());
                    info.setLabel(derivedInfoBean.getLabel());
                    info.setName(derivedInfoBean.getName());
                    // 去除本地存在的钱包
                    String wallet = PreferencesManager.get(activity, Constant.WALLETS, derivedInfoBean.getName(), "").toString();
                    if (Strings.isNullOrEmpty(wallet)) {
                        infos.add(info);
                    }
                });
                EventBus.getDefault().post(new FindOnceWalletEvent<>(infos));
            });
        } catch (Exception e) {
            activity.showToast(e.getMessage());
            EventBus.getDefault().post(new ExitEvent());
            e.printStackTrace();
        }
    }

    /**
     * 加载本地钱包信息
     */
    public static void loadLocalWalletInfo(Context context) {
        try {
            String walletsInfo = sCommands.callAttr(PyConstant.GET_WALLETS_INFO).toString();
            LogUtil.d("oneKey", "----->" + walletsInfo);
            if (!Strings.isNullOrEmpty(walletsInfo)) {
                JsonArray wallets = JsonParser.parseString(walletsInfo).getAsJsonArray();
                wallets.forEach((wallet) -> {
                    wallet.getAsJsonObject().keySet().forEach((walletName) -> PreferencesManager.put(context, Constant.WALLETS, walletName,
                            wallet.getAsJsonObject().get(walletName)));
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 选择要使用的钱包
     *
     * @param name 钱包名称
     */
    public static BalanceInfo selectWallet(@NonNull String name) {
        try {
            String info = sCommands.callAttr(PyConstant.GET_BALANCE, name).toString();
            return BalanceInfo.objectFromData(info);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 查看当前钱包的备份状态
     */
    public static boolean hasBackup(Context context) {
        String keyName = PreferencesManager.get(context, "Preferences", Constant.CURRENT_SELECTED_WALLET_NAME, "").toString();
        try {
            return sCommands.callAttr(PyConstant.HAS_BACKUP, keyName).toBoolean();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 校验扩展公钥格式
     *
     * @param xpub 待校验的扩展公钥
     */
    public static boolean validateXpub(String xpub) {
        if (Global.guiConsole != null) {
            return Global.guiConsole.callAttr(PyConstant.VALIDATE_XPUB, xpub).toBoolean();
        }
        return false;
    }

    /**
     * 校验地址格式
     *
     * @param address 待校验的地址
     */
    public static boolean verifyAddress(String address) {
        if (Global.guiConsole != null) {
            return Global.guiConsole.callAttr(PyConstant.VERIFY_ADDRESS, address).toBoolean();
        }
        return false;
    }

    /**
     * 创建HD钱包
     *
     * @param passwd    APP主密码
     * @param mnemonics 助记词
     */
    public static void createLocalHd(String passwd, String mnemonics) {
        mExecutorService.execute(() -> {
            try {
                List<BalanceInfo> infos = new ArrayList<>();
                String walletsInfo = sCommands.callAttr(PyConstant.CREATE_HD_WALLET, passwd, mnemonics, new Kwarg(Constant.Purpose, 49)).toString();
                CreateWalletBean walletBean = CreateWalletBean.objectFromData(walletsInfo);
                if (Strings.isNullOrEmpty(mnemonics)) {
                    EventBus.getDefault().post(new CreateSuccessEvent(walletBean.getWalletInfo().get(0).getName()));
                    EventBus.getDefault().post(new RefreshEvent());
                } else {
                    // HD 根钱包
                    Optional.ofNullable(walletBean.getWalletInfo()).ifPresent((walletInfos -> {
                        walletInfos.forEach((walletInfo -> {
                            String name = walletInfo.getName();
                            BalanceInfo info = PyEnv.selectWallet(name);
                            EventBus.getDefault().post(new CreateSuccessEvent(name));
                            // 现在的HD钱包的Label是BTC-1
                            info.setLabel("BTC-1");
                            infos.add(info);
                        }));
                    }));
                    walletBean.getDerivedInfo().forEach(derivedInfoBean -> {
                        BalanceInfo info = new BalanceInfo();
                        info.setBalance(derivedInfoBean.getBlance());
                        info.setLabel(derivedInfoBean.getLabel());
                        info.setName(derivedInfoBean.getName());
                        infos.add(info);
                    });
                    EventBus.getDefault().post(new FindOnceWalletEvent<>(infos));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Exception exception = HardWareExceptions.exceptionConvert(e);
                MyApplication.getInstance().toastErr(exception);
                EventBus.getDefault().post(new ExitEvent());
            }
        });
    }

    /**
     * 确认恢复
     *
     * @param nameList 要恢复钱包的名称列表
     */
    public static boolean recoveryConfirm(List<String> nameList, boolean isHw) {
        try {
            sCommands.callAttr(PyConstant.RECOVERY_CONFIRM, new Gson().toJson(nameList), isHw);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置固件升级进度的推送句柄
     */
    public static void setProgressReporter(AsyncTask<String, Object, Void> task) {
        sProtocol.put(PyConstant.PROCESS_REPORTER, task);
    }

    /**
     * 重置固件断点续传的状态
     */
    public static void clearUpdateStatus() {
        protocol.put(PyConstant.HTTP, false);
        protocol.put(PyConstant.OFFSET, 0);
        protocol.put(PyConstant.PROCESS_REPORTER, null);
    }

    /**
     * 固件升级接口
     *
     * @param path 升级文件路径
     */
    public static PyResponse<Void> firmwareUpdate(String path) {
        PyResponse<Void> response = new PyResponse<>();
        try {
            sCommands.callAttr(PyConstant.FIRMWARE_UPDATE, path, MyApplication.getInstance().getDeviceWay());
        } catch (Exception e) {
            e.printStackTrace();
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
        }
        return response;
    }

    /**
     * 获取交易详情
     *
     * @param rawTx
     */
    public static PyResponse<String> analysisRawTx(String rawTx) {
        PyResponse<String> response = new PyResponse<>();
        try {
            String txData = sCommands.callAttr(PyConstant.ANALYZE_TX, rawTx).toString();
            response.setResult(txData);
        } catch (Exception e) {
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 获取费率详情
     */
    public static PyResponse<String> getFeeInfo() {
        PyResponse<String> response = new PyResponse<>();
        try {
            String info = sCommands.callAttr(PyConstant.GET_DEFAULT_FEE_DETAILS).toString();
            response.setResult(info);
        } catch (Exception e) {
            e.printStackTrace();
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
        }
        return response;
    }

    /**
     * 获取自定义费率详情
     */
    public static PyResponse<String> getCustomFeeInfo(String value) {
        PyResponse<String> response = new PyResponse<>();
        try {
            String info = sCommands.callAttr(PyConstant.GET_DEFAULT_FEE_DETAILS, new Kwarg("feerate", value)).toString();
            Logger.d(info);
            response.setResult(info);
        } catch (Exception e) {
            e.printStackTrace();
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
        }
        return response;
    }

    /**
     * 获取交易费
     *
     * @param receiver 发送方
     * @param amount   发送金额
     * @param feeRate  当前费率
     * @return 临时交易
     */
    public static PyResponse<TemporaryTxInfo> getFeeByFeeRate(@NonNull String receiver, @NonNull String amount, @NonNull String feeRate) {
        PyResponse<TemporaryTxInfo> response = new PyResponse<>();
        ArrayList<Map<String, String>> arrayList = new ArrayList<>();
        Map<String, String> params = new HashMap<>(1);
        params.put(receiver, amount);
        arrayList.add(params);
        try {
            String result = Daemon.commands.callAttr(PyConstant.CALCULATE_FEE, new Gson().toJson(arrayList), "", feeRate).toString();
            TemporaryTxInfo temporaryTxInfo = TemporaryTxInfo.objectFromData(result);
            response.setResult(temporaryTxInfo);
        } catch (Exception e) {
            e.printStackTrace();
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
        }
        return response;
    }

    /**
     * 构建交易
     *
     * @param tempTx 临时交易
     * @return rawTx 待签名的交易
     */
    public static PyResponse<String> makeTx(String tempTx) {
        PyResponse<String> response = new PyResponse<>();
        try {
            String result = sCommands.callAttr(PyConstant.MAKE_TX, tempTx).toString();
            String rawTx = MakeTxResponseBean.objectFromData(result).getTx();
            response.setResult(rawTx);
        } catch (Exception e) {
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 汇率转换
     *
     * @param value value in btc
     * @return string value in cash
     */
    public static PyResponse<String> exchange(String value) {
        PyResponse<String> response = new PyResponse<>();
        try {
            String result = sCommands.callAttr(PyConstant.EXCHANGE_RATE_CONVERSION, "base", value).toString();
            response.setResult(result);
        } catch (Exception e) {
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 广播交易
     *
     * @param signedTx 已签名交易
     */
    public static PyResponse<Void> broadcast(String signedTx) {
        PyResponse<Void> response = new PyResponse<>();
        try {
            sCommands.callAttr(PyConstant.BROADCAST_TX, signedTx);
        } catch (Exception e) {
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 签名交易
     *
     * @param rawTx 未签名的交易
     * @return 签名后的交易详情
     */
    public static PyResponse<TransactionInfoBean> signTx(String rawTx, String password) {
        PyResponse<TransactionInfoBean> response = new PyResponse<>();
        try {
            String result = sCommands.callAttr(PyConstant.SIGN_TX, rawTx, new Kwarg("password", password)).toString();
            TransactionInfoBean txInfo = TransactionInfoBean.objectFromData(result);
            response.setResult(txInfo);
        } catch (Exception e) {
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 获取当前钱包的详细信息
     */
    public static PyResponse<CurrentAddressDetail> getCurrentAddressInfo() {
        PyResponse<CurrentAddressDetail> response = new PyResponse<>();
        try {
            String result = sCommands.callAttr(PyConstant.ADDRESS_INFO).toString();
            CurrentAddressDetail txInfo = CurrentAddressDetail.objectFromData(result);
            response.setResult(txInfo);
        } catch (Exception e) {
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 校验签名
     *
     * @param address   地址
     * @param message   原始信息
     * @param signature 签名
     */
    public static PyResponse<Boolean> verifySignature(String address, String message, String signature) {
        PyResponse<Boolean> response = new PyResponse<>();
        try {
            boolean verified = sCommands.callAttr(PyConstant.VERIFY_MESSAGE_SIGNATURE, address, message, signature).toBoolean();
            response.setResult(verified);
        } catch (Exception e) {
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 删除hd钱包的备份标记
     *
     * @param name 钱包名称
     */
    public static PyResponse<Void> clearHdBackupFlags(String name) {
        PyResponse<Void> response = new PyResponse<>();
        try {
            sCommands.callAttr(PyConstant.CLEAR_BACK_FLAGS, name);
        } catch (Exception e) {
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 校验APP主密码
     *
     * @param passwd APP主密码
     */
    public static PyResponse<Void> verifySoftPass(String passwd) {
        PyResponse<Void> response = new PyResponse<>();
        try {
            sCommands.callAttr(PyConstant.VERIFY_SOFT_PASS, passwd);
        } catch (Exception e) {
            System.out.println("verify xxxxxxx");
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 修改APP主密码
     *
     * @param passwdOrigin APP原主密码
     * @param passwdNew    APP新主密码
     */
    public static PyResponse<Void> changeSoftPass(String passwdOrigin, String passwdNew) {
        PyResponse<Void> response = new PyResponse<>();
        try {
            sCommands.callAttr(PyConstant.CHANGE_SOFT_PASS, passwdOrigin, passwdNew);
        } catch (Exception e) {
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 导出助记词
     *
     * @param password APP主密码
     */
    public static PyResponse<String> exportMnemonics(String password) {
        PyResponse<String> response = new PyResponse<>();
        try {
            String result = sCommands.callAttr(PyConstant.EXPORT_MNEMONICS, password, null).toString();
            response.setResult(result);
        } catch (Exception e) {
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 创建钱包
     *
     * @param context
     * @param password   APP主密码
     * @param walletName 钱包名称
     * @param mnemonics  助记词 optional
     * @param privateKey 私钥 optional
     */

    public static void createWallet(Context context, String walletName, String password, String privateKey, String mnemonics, int purpose) {
        try {
            String result = "";
            if (purpose != -1) {
                result = sCommands.callAttr(PyConstant.CREATE_WALLET, walletName, password, Strings.isNullOrEmpty(privateKey) ? new Kwarg("seed", mnemonics) : new Kwarg("privkeys", privateKey), new Kwarg(Constant.Purpose, purpose)).toString();
            } else {
                result = sCommands.callAttr(PyConstant.CREATE_WALLET, walletName, password, Strings.isNullOrEmpty(privateKey) ? new Kwarg("seed", mnemonics) : new Kwarg("privkeys", privateKey)).toString();
            }
            CreateWalletBean createWalletBean = CreateWalletBean.objectFromData(result);
            EventBus.getDefault().post(new CreateSuccessEvent(createWalletBean.getWalletInfo().get(0).getName()));
            context.startActivity(new Intent(context, HomeOneKeyActivity.class));
        } catch (Exception e) {
            EventBus.getDefault().post(new ExitEvent());
            if (e.getMessage() != null) {
                MyApplication.getInstance().toastErr(e);
            }
            e.printStackTrace();
        }
    }

    /**
     * 删除指定钱包
     *
     * @param password   APP 主密码
     * @param walletName 要删除的钱包名称
     */
    public static PyResponse<Void> deleteWallet(String password, String walletName,boolean allDelete) {
        PyResponse<Void> response = new PyResponse<>();
        try {
            if (Strings.isNullOrEmpty(password)) {
                sCommands.callAttr(PyConstant.DELETE_WALLET, new Kwarg("name", walletName));
            } else {
                if (allDelete) {
                    sCommands.callAttr(PyConstant.DELETE_WALLET, password, new Kwarg("name", walletName), new Kwarg("hd", true));
                } else {
                    sCommands.callAttr(PyConstant.DELETE_WALLET, password, new Kwarg("name", walletName));
                }
            }
        } catch (Exception e) {
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 创建派生钱包
     *
     * @param walletName   钱包名称
     * @param password     APP 主密码
     * @param currencyType 币种类型
     */
    public static PyResponse<Void> createDerivedWallet(String walletName, String password, String currencyType, int purpose) {
        PyResponse<Void> response = new PyResponse<>();
        try {
            String result = sCommands.callAttr("create_derived_wallet", walletName, password, currencyType, purpose).toString();
            CreateWalletBean createWalletBean = CreateWalletBean.objectFromData(result);
            EventBus.getDefault().post(new CreateSuccessEvent(createWalletBean.getWalletInfo().get(0).getName()));
        } catch (Exception e) {
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());            e.printStackTrace();
        }
        return response;
    }

    /**
     * 导出私钥
     *
     * @param password APP主密码
     */
    public static PyResponse<String> exportPrivateKey(String password) {
        PyResponse<String> response = new PyResponse<>();
        try {
            String result = sCommands.callAttr(PyConstant.EXPORT_PRIVATE_KEY, password).toString();
            response.setResult(result);
        } catch (Exception e) {
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());            e.printStackTrace();
        }
        return response;
    }

    /**
     * 获取派生HD钱包的个数
     *
     * @param coin
     * @return
     */
    public static PyResponse<String> getDerivedNum(String coin) {
        PyResponse<String> response = new PyResponse<>();
        try {
            String res = sCommands.callAttr(PyConstant.GET_DEVIRED_NUM, coin).toString();
            response.setResult(res);
        } catch (Exception e) {
            Exception exception = HardWareExceptions.exceptionConvert(e);
            response.setErrors(exception.getMessage());
        }
        return response;
    }
}
