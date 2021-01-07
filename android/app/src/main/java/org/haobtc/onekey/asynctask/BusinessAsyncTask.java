package org.haobtc.onekey.asynctask;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import com.chaquo.python.Kwarg;

import org.greenrobot.eventbus.EventBus;
import org.haobtc.onekey.event.CheckReceiveAddress;
import org.haobtc.onekey.event.OperationTimeoutEvent;
import org.haobtc.onekey.event.WhiteListEnum;
import org.haobtc.onekey.exception.HardWareExceptions;
import org.haobtc.onekey.manager.PyEnv;
import org.haobtc.onekey.utils.Daemon;

import no.nordicsemi.android.dfu.DfuServiceInitiator;

import static org.haobtc.onekey.activities.service.CommunicationModeSelector.ble;
import static org.haobtc.onekey.activities.service.CommunicationModeSelector.nfc;
import static org.haobtc.onekey.activities.service.CommunicationModeSelector.protocol;

/**
 * @author liyan
 *
 */
public class BusinessAsyncTask extends AsyncTask<String, Void, String> {

    public final static String GET_EXTEND_PUBLIC_KEY = "get_xpub_from_hw";
    /**
     * @Deprecated Use {@link BusinessAsyncTask#GET_EXTEND_PUBLIC_KEY_PERSONAL} instead.
     * */
    @Deprecated
    public final static String GET_EXTEND_PUBLIC_KEY_SINGLE = "get_xpub_from_hw_single";
    public final static String SIGN_TX = "sign_tx";
    public static final String BACK_UP = "backup_wallet";
    public static final String RECOVER = "recovery_wallet";
    public final static String CHANGE_PIN = "reset_pin";
    public final static String WIPE_DEVICE = "wipe_device";
    public static final String SIGN_MESSAGE = "sign_message";
    public static final String INIT_DEVICE = "init";
    public static final String COUNTER_VERIFICATION = "hardware_verify";
    public static final String APPLY_SETTING = "apply_setting";
    public static final String SHOW_ADDRESS = "show_address";
    public static final String EDIT_WHITE_LIST = "bx_inquire_whitelist";
    public static final String ADD_AND_DELETE_WHITE_LIST = "bx_add_or_delete_whitelist";
    public static final String READ_MNEMONIC_FROM_HARDWARE = "bixin_backup_device";
    public static final String HARDWARE_RECOVERY = "recovery";
    public static final String GET_EXTEND_PUBLIC_KEY_PERSONAL = "create_hw_derived_wallet";
    /**
     * 导入助记词
     * */
    public static final String IMPORT_MNEMONIC = "bixin_load_device";
    /**
     * method used to pass-through the se message
     */
    public static final String SE_PROXY = "se_proxy";
    private final static String TAG = BusinessAsyncTask.class.getSimpleName();
    /**
     * used as callback
     */
    private Helper helper;

    public BusinessAsyncTask setHelper(Helper helper) {
        this.helper = helper;
        return this;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        helper.onPreExecute();
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected String doInBackground(String... strings) {
        helper.currentMethod(strings[0]);
        System.out.println(String.format("method==%s===in thread===%d", strings[0], Thread.currentThread().getId()));
        String result = "";
        switch (strings[0]) {
            case GET_EXTEND_PUBLIC_KEY_SINGLE:
            case SIGN_TX:
            case COUNTER_VERIFICATION:
            case SE_PROXY:
            case GET_EXTEND_PUBLIC_KEY_PERSONAL:
                try {
                    result = Daemon.commands.callAttr(strings[0].endsWith("single") ? GET_EXTEND_PUBLIC_KEY : strings[0], strings[1], strings[2]).toString();
                } catch (Exception e) {
                    cancel(true);
                    onException(e);
                }
                break;
            case RECOVER:
            case SIGN_MESSAGE:
                try {
                    result = Daemon.commands.callAttr(strings[0], strings[1], strings[2], strings[3]).toString();
                } catch (Exception e) {
                    cancel(true);
                    onException(e);
                }
                break;
            case GET_EXTEND_PUBLIC_KEY:
            case CHANGE_PIN:
            case WIPE_DEVICE:
            case BACK_UP:
            case READ_MNEMONIC_FROM_HARDWARE:
                try {
                    result = Daemon.commands.callAttr(strings[0], strings[1]).toString();
                } catch (Exception e) {
                    cancel(true);
                    onException(e);
                }
                break;
            case INIT_DEVICE:
            case IMPORT_MNEMONIC:
                try {
                    result = Daemon.commands.callAttr(strings[0], strings[1], strings[2], strings[3], strings[4]).toString();
                } catch (Exception e) {
                    cancel(true);
                    onException(e);
                }
                break;
            case APPLY_SETTING:
                try {
                    if ("setBluetooth".equals(strings[2])) {
                        result = Daemon.commands.callAttr(strings[0], strings[1], "one".equals(strings[3]) ? new Kwarg("use_ble", true) : new Kwarg("use_ble", false)).toString();
                    } else if ("fastPay".equals(strings[2])) {
                        int moneyLimit = Integer.parseInt(strings[3]);
                        int moneyTimes = Integer.parseInt(strings[4]);
                        result = Daemon.commands.callAttr(strings[0], strings[1], new Kwarg("fastpay_money_limit", moneyLimit), new Kwarg("fastpay_times", moneyTimes), "true".equals(strings[5]) ? new Kwarg("fastpay_pin", true) : new Kwarg("fastpay_pin", false), "true".equals(strings[6]) ? new Kwarg("fastpay_confirm", true) : new Kwarg("fastpay_confirm", false)).toString();
                    } else if ("label".equals(strings[2])) {
                        result = Daemon.commands.callAttr(strings[0], strings[1], new Kwarg("label", strings[3])).toString();
                    } else if ("shutdown_time".equals(strings[2])) {
                        int shutdownTime = Integer.parseInt(strings[3]);
                        result = Daemon.commands.callAttr(strings[0], strings[1], new Kwarg("auto_lock_delay_ms", shutdownTime * 1000)).toString();
                    } else if ("fix_hardware_language".equals(strings[2])) {
                        result = Daemon.commands.callAttr(strings[0], strings[1], new Kwarg("language", strings[3])).toString();
                    }
                } catch (Exception e) {
                    cancel(true);
                    onException(e);
                }
                break;
            case SHOW_ADDRESS:
                try {
                   result = Daemon.commands.callAttr(strings[0], strings[1], strings[2]).toString();
                } catch (Exception e) {
                    onException(e);
                }
                EventBus.getDefault().post(new CheckReceiveAddress("getResult"));
                break;
            case EDIT_WHITE_LIST:
                try {
                    result = Daemon.commands.callAttr(strings[0], strings[1], new Kwarg("type", WhiteListEnum.Inquire.getWhiteListType())).toString();
                } catch (Exception e) {
                    onException(e);
                }
                break;
            case ADD_AND_DELETE_WHITE_LIST:
                try {
                    if ("Add".equals(strings[2])) {
                        result = Daemon.commands.callAttr(strings[0], strings[1], new Kwarg("type", WhiteListEnum.Add.getWhiteListType()), new Kwarg("addr_in", strings[3])).toString();
                    }else if ("Delete".equals(strings[2])){
                        result = Daemon.commands.callAttr(strings[0], strings[1], new Kwarg("type", WhiteListEnum.Delete.getWhiteListType()), new Kwarg("addr_in", strings[3])).toString();
                    }
                } catch (Exception e) {
                    onException(e);

                }
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + strings[0]);
        }
        return result;
    }

    private void onException(Exception e) {
        if (HardWareExceptions.PASSPHRASE_OPERATION_TIMEOUT.getMessage().equals(e.getMessage()) || HardWareExceptions.PIN_OPERATION_TIMEOUT.getMessage().equals(e.getMessage())) {
            EventBus.getDefault().post(new OperationTimeoutEvent());
        } else if (HardWareExceptions.USER_CANCEL.getMessage().equals(e.getMessage())) {
            Log.d(TAG, "cancel by user");
        } else {
            helper.onException(HardWareExceptions.exceptionConvert(e));
        }
        if (ble != null) {
            PyEnv.cancelAll();
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        helper.onCancelled();
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        helper.onResult(s);
    }

    public interface Helper {
        void onPreExecute();

        void onException(Exception e);

        void onResult(String s);

        void onCancelled();

        void currentMethod(String methodName);
    }

}
