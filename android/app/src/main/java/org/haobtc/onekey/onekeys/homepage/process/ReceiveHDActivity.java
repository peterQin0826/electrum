package org.haobtc.onekey.onekeys.homepage.process;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.res.ResourcesCompat;
import butterknife.BindView;
import butterknife.OnClick;
import com.chaquo.python.PyObject;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.lxj.xpopup.XPopup;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.yzq.zxinglibrary.encode.CodeCreator;
import io.reactivex.disposables.Disposable;
import java.io.File;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.haobtc.onekey.R;
import org.haobtc.onekey.activities.base.MyApplication;
import org.haobtc.onekey.aop.SingleClick;
import org.haobtc.onekey.asynctask.BusinessAsyncTask;
import org.haobtc.onekey.bean.CurrentAddressDetail;
import org.haobtc.onekey.constant.Constant;
import org.haobtc.onekey.constant.PyConstant;
import org.haobtc.onekey.constant.Vm;
import org.haobtc.onekey.event.ButtonRequestEvent;
import org.haobtc.onekey.event.ChangePinEvent;
import org.haobtc.onekey.event.ExitEvent;
import org.haobtc.onekey.event.SecondEvent;
import org.haobtc.onekey.exception.HardWareExceptions;
import org.haobtc.onekey.manager.PyEnv;
import org.haobtc.onekey.ui.activity.VerifyPinActivity;
import org.haobtc.onekey.ui.base.BaseActivity;
import org.haobtc.onekey.ui.dialog.UnBackupTipDialog;
import org.haobtc.onekey.ui.dialog.custom.CustomCenterDialog;
import org.haobtc.onekey.utils.ClipboardUtils;
import org.haobtc.onekey.utils.Daemon;
import org.haobtc.onekey.utils.ImageUtils;

/** @author liyan */
public class ReceiveHDActivity extends BaseActivity implements BusinessAsyncTask.Helper {

    @BindView(R.id.img_type)
    ImageView imgType;

    @BindView(R.id.iv_show_token_logo)
    ImageView imgShowTokenLogo;

    @BindView(R.id.tv_show_swipe_hint)
    TextView tvShowSwipeHint;

    @BindView(R.id.text_send_type)
    TextView textSendType;

    @BindView(R.id.img_share_qrcode)
    ImageView imgShareQrcode;

    @BindView(R.id.text_wallet_address_text)
    TextView textWalletAddressText;

    @BindView(R.id.text_wallet_address)
    TextView textWalletAddress;

    @BindView(R.id.share_layout)
    LinearLayout shareLayout;

    @BindView(R.id.img_back)
    ImageView imgBack;

    @BindView(R.id.img_Orcode)
    ImageView imgOrcode;

    @BindView(R.id.text_receive_address)
    TextView textReceiveAddress;

    @BindView(R.id.linear_copy)
    LinearLayout linearCopy;

    @BindView(R.id.linear_share)
    LinearLayout linearShare;

    @BindView(R.id.normal_layout)
    LinearLayout normalLayout;

    @BindView(R.id.verify_promote)
    TextView verifyPromote;

    @BindView(R.id.verify_start)
    Button verifyStart;

    @BindView(R.id.need_verify_layout)
    LinearLayout needVerifyLayout;

    @BindView(R.id.receive_payment_layout)
    RelativeLayout receivePaymentLayout;

    @BindView(R.id.verify_d)
    LinearLayout verifyD;

    private RxPermissions rxPermissions;
    private Bitmap bitmap;
    private int walletType;
    private String address;
    private Disposable subscriber;
    private String mShowWalletInfo;
    private Vm.CoinType mShowWalletType;

    /** init */
    @Override
    public void init() {
        walletType = getIntent().getIntExtra(Constant.WALLET_TYPE, 0);
        SharedPreferences preferences = getSharedPreferences("Preferences", MODE_PRIVATE);
        mShowWalletInfo = preferences.getString(Constant.CURRENT_SELECTED_WALLET_TYPE, "");
        mShowWalletType = Vm.convertCoinType(mShowWalletInfo);
        rxPermissions = new RxPermissions(this);
        if (mShowWalletInfo.contains("eth")) {
            Drawable drawable =
                    ResourcesCompat.getDrawable(getResources(), R.drawable.token_eth, null);
            imgType.setImageDrawable(drawable);
            tvShowSwipeHint.setText(R.string.scan_input_eth);

            imgShowTokenLogo.setImageDrawable(drawable);
            textSendType.setText(String.format("%s ETH", getString(R.string.scan_send)));
            textWalletAddressText.setText(
                    String.format("ETH %s", getString(R.string.wallet_address)));
        } else {
            if (mShowWalletInfo.equals(Constant.BTC_WATCH)) {
                showWatchTipDialog();
            }
            textSendType.setText(String.format("%s BTC", getString(R.string.scan_send)));
            textWalletAddressText.setText(
                    String.format("BTC %s", getString(R.string.wallet_address)));
        }
        // whether backup
        try {
            boolean isBackup = PyEnv.hasBackup(getActivity());
            if (!isBackup) {
                new XPopup.Builder(mContext)
                        .dismissOnTouchOutside(false)
                        .isDestroyOnDismiss(true)
                        .asCustom(
                                new UnBackupTipDialog(
                                        this,
                                        getString(R.string.unbackup_tip_dialog),
                                        new UnBackupTipDialog.onClick() {
                                            @Override
                                            public void onBack() {
                                                finish();
                                            }
                                        }))
                        .show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // get receive address
        mGeneratecode();
    }

    private void showWatchTipDialog() {
        CustomCenterDialog centerDialog =
                new CustomCenterDialog(
                        mContext,
                        new CustomCenterDialog.onConfirmClick() {
                            @Override
                            public void onConfirm() {
                                finish();
                            }
                        });
        centerDialog.setContent(getString(R.string.watch_wallet_receive_tip));
        new XPopup.Builder(mContext).asCustom(centerDialog).show();
    }

    /**
     * * init layout
     *
     * @return
     */
    @Override
    public int getContentViewId() {
        return R.layout.activity_receive_h_d;
    }

    private void mGeneratecode() {
        PyObject walletAddressShowUi = null;
        try {
            walletAddressShowUi = Daemon.commands.callAttr("get_wallet_address_show_UI");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(
                            this,
                            Objects.requireNonNull(HardWareExceptions.getExceptionString(e)),
                            Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        if (walletAddressShowUi != null) {
            String strCode = walletAddressShowUi.toString();
            Log.i("strCode", "mGeneratecode: " + strCode);
            Gson gson = new Gson();
            CurrentAddressDetail currentAddressDetail =
                    gson.fromJson(strCode, CurrentAddressDetail.class);
            String qrData = currentAddressDetail.getQrData();
            address = currentAddressDetail.getAddr();
            textReceiveAddress.setText(address);
            textWalletAddress.setText(address);
            bitmap = CodeCreator.createQRCode(qrData, 250, 250, null);
            switch (walletType) {
                case Constant.WALLET_TYPE_SOFTWARE:
                    needVerifyLayout.setVisibility(View.GONE);
                    receivePaymentLayout.setVisibility(View.VISIBLE);
                    verifyD.setVisibility(View.GONE);
                    normalLayout.setVisibility(View.VISIBLE);
                    imgOrcode.setImageBitmap(bitmap);
                    imgShareQrcode.setImageBitmap(bitmap);
                    break;
                case Constant.WALLET_TYPE_HARDWARE_PERSONAL:
                    textReceiveAddress.setText(
                            String.format(
                                    Locale.ENGLISH,
                                    "%s....%s",
                                    address.substring(0, 6),
                                    address.substring(address.length() - 6)));
                    imgOrcode.setImageResource(R.drawable.qrcode_shade);
                    break;
            }
        }
    }

    @SuppressLint("CheckResult")
    @SingleClick
    @OnClick({R.id.img_back, R.id.linear_copy, R.id.linear_share, R.id.verify_start})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_back:
                finish();
                break;
            case R.id.linear_copy:
                // copy text
                ClipboardUtils.copyText(this, textReceiveAddress.getText().toString());
                break;
            case R.id.linear_share:
                try {
                    subscriber =
                            rxPermissions
                                    .request(
                                            Manifest.permission.READ_EXTERNAL_STORAGE,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    .subscribe(
                                            granted -> {
                                                if (granted) { // Always true pre-M
                                                    File shareImg = null;
                                                    try {
                                                        // 以地址作为文件名称
                                                        shareImg =
                                                                ImageUtils.viewSaveToImage(
                                                                        shareLayout, address);
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    if (shareImg.exists()) {
                                                        Uri uri =
                                                                Uri.parse(
                                                                        MediaStore.Images.Media
                                                                                .insertImage(
                                                                                        getContentResolver(),
                                                                                        shareImg
                                                                                                .getAbsolutePath(),
                                                                                        "",
                                                                                        null));
                                                        Intent shareIntent =
                                                                new Intent(Intent.ACTION_SEND);
                                                        shareIntent.addFlags(
                                                                Intent
                                                                                .FLAG_GRANT_READ_URI_PERMISSION
                                                                        | Intent
                                                                                .FLAG_GRANT_WRITE_URI_PERMISSION);
                                                        shareIntent.putExtra(
                                                                Intent.EXTRA_STREAM, uri);
                                                        shareIntent.setType("image/*");
                                                        shareIntent.putExtra(
                                                                Intent.EXTRA_TEXT,
                                                                textReceiveAddress
                                                                        .getText()
                                                                        .toString());
                                                        shareIntent.setFlags(
                                                                Intent.FLAG_ACTIVITY_NEW_TASK);
                                                        shareIntent =
                                                                Intent.createChooser(
                                                                        shareIntent,
                                                                        getString(
                                                                                R.string.share_to));
                                                        startActivity(shareIntent);
                                                    } else {
                                                        Toast.makeText(
                                                                        this,
                                                                        getString(
                                                                                R.string
                                                                                        .pictrue_fail),
                                                                        Toast.LENGTH_SHORT)
                                                                .show();
                                                    }
                                                } else { // Oups permission denied
                                                    Toast.makeText(
                                                                    this,
                                                                    R.string.reservatpion_photo,
                                                                    Toast.LENGTH_SHORT)
                                                            .show();
                                                }
                                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.verify_start:
                verifyAddress();
        }
    }

    @Override
    public boolean needEvents() {
        return true;
    }

    private void verifyAddress() {
        new BusinessAsyncTask()
                .setHelper(this)
                .execute(
                        BusinessAsyncTask.SHOW_ADDRESS,
                        address,
                        MyApplication.getInstance().getDeviceWay(),
                        mShowWalletType.callFlag);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onChangePin(ChangePinEvent event) {
        // 回写PIN码
        PyEnv.setPin(event.toString());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onButtonRequest(ButtonRequestEvent event) {
        switch (event.getType()) {
            case PyConstant.PIN_CURRENT:
                Intent intent = new Intent(this, VerifyPinActivity.class);
                startActivity(intent);
                break;
            case PyConstant.VERIFY_ADDRESS_CONFIRM:
                EventBus.getDefault().post(new ExitEvent());
                imgOrcode.setImageBitmap(bitmap);
                imgShareQrcode.setImageBitmap(bitmap);
                textReceiveAddress.setText(address);
                verifyStart.setVisibility(View.INVISIBLE);
                verifyPromote.setGravity(Gravity.CENTER);
                verifyPromote.setText(R.string.verifying);
            default:
        }
    }

    @Override
    public void onPreExecute() {}

    @Override
    public void onException(Exception e) {
        EventBus.getDefault().post(new ExitEvent());
        showToast(e.getMessage());
    }

    @Override
    public void onResult(String s) {
        if (!Strings.isNullOrEmpty(s)) {
            needVerifyLayout.setVisibility(View.GONE);
            normalLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCancelled() {}

    @Override
    public void currentMethod(String methodName) {}

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void event(SecondEvent updataHint) {
        String msgVote = updataHint.getMsg();
        if ("finish".equals(msgVote)) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        Optional.ofNullable(subscriber).ifPresent(Disposable::dispose);
    }
}
