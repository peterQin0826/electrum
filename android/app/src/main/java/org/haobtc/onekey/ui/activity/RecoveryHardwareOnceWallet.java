package org.haobtc.onekey.ui.activity;

import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.haobtc.onekey.R;
import org.haobtc.onekey.activities.base.MyApplication;
import org.haobtc.onekey.asynctask.BusinessAsyncTask;
import org.haobtc.onekey.constant.PyConstant;
import org.haobtc.onekey.event.ButtonRequestEvent;
import org.haobtc.onekey.event.ChangePinEvent;
import org.haobtc.onekey.event.CreateSuccessEvent;
import org.haobtc.onekey.event.ExitEvent;
import org.haobtc.onekey.event.SelectedEvent;
import org.haobtc.onekey.manager.PyEnv;
import org.haobtc.onekey.onekeys.HomeOneKeyActivity;
import org.haobtc.onekey.ui.base.BaseActivity;
import org.haobtc.onekey.ui.fragment.DevicePINFragment;
import org.haobtc.onekey.ui.fragment.RecoveryWalletFromHdFragment;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * @author liyan
 * @date 12/2/20
 */

public class RecoveryHardwareOnceWallet extends BaseActivity implements BusinessAsyncTask.Helper {

    @BindView(R.id.img_back)
    ImageView imgBack;
    private RecoveryWalletFromHdFragment fromHdFragment;

    /**
     * init
     */
    @Override
    public void init() {
        updateTitle(R.string.recovery_only);
        fromHdFragment = new RecoveryWalletFromHdFragment();
        getXpubP2wpkh();
        startFragment(fromHdFragment);
    }

    /***
     * init layout
     * @return
     */
    @Override
    public int getContentViewId() {
        return R.layout.activity_title_container;
    }

    /**
     * 获取用于个人钱包的扩展公钥
     */
    private void getXpubP2wpkh() {
        new BusinessAsyncTask().setHelper(this).execute(BusinessAsyncTask.GET_EXTEND_PUBLIC_KEY_PERSONAL,
                MyApplication.getInstance().getDeviceWay(), PyConstant.ADDRESS_TYPE_P2WPKH);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onChangePin(ChangePinEvent event) {
        startFragment(fromHdFragment);
        // 回写PIN码
        PyEnv.setPin(event.getPinNew());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onButtonRequest(ButtonRequestEvent event) {
        if (PyConstant.PIN_CURRENT == event.getType()) {
            startFragment(new DevicePINFragment(PyConstant.PIN_CURRENT));
        }
    }

    /**
     * 恢复指定钱包
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecovery(SelectedEvent event) {
        boolean success = PyEnv.recoveryConfirm(event.getNameList(), true);
        if (!success) {
            showToast(R.string.recovery_failed);
        } else {
            EventBus.getDefault().post(new CreateSuccessEvent(event.getNameList().get(0)));
            startActivity(new Intent(this, HomeOneKeyActivity.class));
        }
        finish();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Subscribe
    public void onFinish(ExitEvent exitEvent) {
        if (hasWindowFocus()) {
            finish();
        }
    }

    @Override
    public void onPreExecute() {

    }

    @Override
    public void onException(Exception e) {
        showToast(e.getMessage());
        if (!hasWindowFocus()) {
            EventBus.getDefault().post(new ExitEvent());
        }
        finish();
    }

    @Override
    public void onResult(String s) {
        if (!hasWindowFocus()) {
            EventBus.getDefault().post(new ExitEvent());
        }
        String xpubs = "[[\"" + s + "\", \"" + FindNormalDeviceActivity.deviceId + "\"]]";
        PyEnv.recoveryWallet(this, xpubs, true);
    }

    @Override
    public void onCancelled() {

    }

    @Override
    public void currentMethod(String methodName) {

    }

    @Override
    public boolean needEvents() {
        return true;
    }

    @OnClick(R.id.img_back)
    public void onViewClicked(View view) {
        PyEnv.cancelRecovery();
        PyEnv.cancelPinInput();
        PyEnv.cancelAll();
        finish();
    }
}
