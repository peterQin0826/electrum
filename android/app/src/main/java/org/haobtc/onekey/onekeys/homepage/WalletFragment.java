package org.haobtc.onekey.onekeys.homepage;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.chaquo.python.PyObject;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.common.Constant;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.haobtc.onekey.R;
import org.haobtc.onekey.activities.sign.SignActivity;
import org.haobtc.onekey.aop.SingleClick;
import org.haobtc.onekey.bean.HardwareFeatures;
import org.haobtc.onekey.bean.LocalWalletInfo;
import org.haobtc.onekey.bean.MainSweepcodeBean;
import org.haobtc.onekey.business.wallet.SystemConfigManager;
import org.haobtc.onekey.event.BackupCompleteEvent;
import org.haobtc.onekey.event.BackupEvent;
import org.haobtc.onekey.event.BleConnectedEvent;
import org.haobtc.onekey.event.BleConnectionEx;
import org.haobtc.onekey.event.FixWalletNameEvent;
import org.haobtc.onekey.event.GotPassEvent;
import org.haobtc.onekey.event.RefreshEvent;
import org.haobtc.onekey.event.SecondEvent;
import org.haobtc.onekey.manager.BleManager;
import org.haobtc.onekey.manager.PreferencesManager;
import org.haobtc.onekey.manager.PyEnv;
import org.haobtc.onekey.onekeys.backup.BackupGuideActivity;
import org.haobtc.onekey.onekeys.dialog.RecoverHdWalletActivity;
import org.haobtc.onekey.onekeys.homepage.process.DetailTransactionActivity;
import org.haobtc.onekey.onekeys.homepage.process.HdWalletDetailActivity;
import org.haobtc.onekey.onekeys.homepage.process.ReceiveHDActivity;
import org.haobtc.onekey.onekeys.homepage.process.SendHdActivity;
import org.haobtc.onekey.onekeys.homepage.process.TransactionDetailWalletActivity;
import org.haobtc.onekey.ui.activity.SearchDevicesActivity;
import org.haobtc.onekey.ui.activity.SoftPassActivity;
import org.haobtc.onekey.ui.base.BaseFragment;
import org.haobtc.onekey.ui.dialog.BackupDialog;
import org.haobtc.onekey.utils.Daemon;
import org.haobtc.onekey.utils.NavUtils;
import org.haobtc.onekey.viewmodel.AppWalletViewModel;
import org.haobtc.onekey.viewmodel.NetworkViewModel;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import io.reactivex.functions.Consumer;

import static android.app.Activity.RESULT_OK;
import static org.haobtc.onekey.constant.Constant.CURRENT_CURRENCY_GRAPHIC_SYMBOL;
import static org.haobtc.onekey.constant.Constant.CURRENT_SELECTED_WALLET_TYPE;
import static org.haobtc.onekey.constant.Constant.WALLET_BALANCE;

/**
 * @author jinxiaomin
 */
public class WalletFragment extends BaseFragment implements TextWatcher {

    private static final int REQUEST_CODE = 0;
    private static int currentAction;
    @BindView(R.id.text_wallet_name)
    TextView textWalletName;
    @BindView(R.id.img_type)
    ImageView imgType;
    @BindView(R.id.rel_check_wallet)
    RelativeLayout relCheckWallet;
    @BindView(R.id.img_scan)
    ImageView imgScan;
    @BindView(R.id.img_Add)
    ImageView imgAdd;
    @BindView(R.id.rel_create_hd)
    RelativeLayout relCreateHd;
    @BindView(R.id.rel_recovery_hd)
    RelativeLayout relRecoveryHd;
    @BindView(R.id.rel_pair_hard)
    RelativeLayout relPairHard;
    @BindView(R.id.lin_no_wallet)
    LinearLayout linearNoWallet;
    @BindView(R.id.img_bottom)
    ImageView imgBottom;
    @BindView(R.id.text_hard)
    TextView textHard;
    @BindView(R.id.text_amount)
    TextView tetAmount;
    @BindView(R.id.text_amount_star)
    TextView textStar;
    @BindView(R.id.img_check_money)
    CheckBox imgCheckMoney;
    @BindView(R.id.rel_wallet_detail)
    RelativeLayout relWalletDetail;
    @BindView(R.id.linear_send)
    LinearLayout linearSend;
    @BindView(R.id.linear_receive)
    LinearLayout linearReceive;
    @BindView(R.id.linear_sign)
    LinearLayout linearSign;
    @BindView(R.id.lin_have_wallet)
    LinearLayout linearHaveWallet;
    @BindView(R.id.money)
    ImageView money;
    @BindView(R.id.rel_now_back_up)
    android.widget.RelativeLayout relNowBackUp;
    @BindView(R.id.text_btc_amount)
    TextView textBtcAmount;
    @BindView(R.id.text_btc_amount_stars)
    TextView amountStars;
    @BindView(R.id.text_dollar)
    TextView textDollar;
    @BindView(R.id.text_dollar_stars)
    TextView dollarStars;
    @BindView(R.id.rel_bi_detail)
    android.widget.RelativeLayout relBiDetail;
    @BindView(R.id.recl_hd_list)
    RecyclerView reclHdList;
    @BindView(R.id.lin_wallet_list)
    LinearLayout linearWalletList;
    private SharedPreferences preferences;
    private String num;
    private String changeBalance;
    private String name;
    private SharedPreferences.Editor edit;
    private RxPermissions rxPermissions;
    private String nowType;
    private boolean isBackup;
    private String bleMac;
    private boolean isRecovery;
    private boolean isAddHd;
    private NetworkViewModel mNetworkViewModel;
    private AppWalletViewModel mAppWalletViewModel;
    private SystemConfigManager mSystemConfigManager;
    private BackupDialog mBackupDialog;

    /**
     * init views
     *
     * @param view
     */
    @Override
    public void init(View view) {
        mNetworkViewModel = getApplicationViewModel(NetworkViewModel.class);
        mAppWalletViewModel = getApplicationViewModel(AppWalletViewModel.class);
        mSystemConfigManager = new SystemConfigManager(requireContext());

        rxPermissions = new RxPermissions(this);
        preferences = requireActivity().getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        edit = preferences.edit();
        tetAmount.addTextChangedListener(this);
        initViewModelValue();
    }

    /***
     * init layout
     * @return
     */
    @Override
    public int getContentViewId() {
        return R.layout.fragment_wallet;
    }

    private void initViewModelValue() {
        mNetworkViewModel.haveNet().observe(this, state -> {
            if (state) {
                mAppWalletViewModel.refresh();
            }
        });
        mAppWalletViewModel.existsWallet.observe(this, exists -> {
            if (exists) {
                //have wallet
                linearNoWallet.setVisibility(View.GONE);
                imgBottom.setVisibility(View.GONE);
                linearHaveWallet.setVisibility(View.VISIBLE);
                linearWalletList.setVisibility(View.VISIBLE);
                imgScan.setVisibility(View.VISIBLE);
            } else {
                //no wallet
                try {
                    Daemon.commands.callAttr("set_currency", "CNY");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                imgType.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.loco_round));
                linearNoWallet.setVisibility(View.VISIBLE);
                imgBottom.setVisibility(View.VISIBLE);
                linearHaveWallet.setVisibility(View.GONE);
                linearWalletList.setVisibility(View.GONE);
                imgScan.setVisibility(View.GONE);
                relNowBackUp.setVisibility(View.GONE);
                PreferencesManager.put(getContext(), "Preferences", org.haobtc.onekey.constant.Constant.HAS_LOCAL_HD, false);
                textWalletName.setText(R.string.no_use_wallet);
                mSystemConfigManager.setNeedPopBackUpDialog(true);
            }
        });
        mAppWalletViewModel.currentWalletInfo.observe(this, localWalletInfo -> {
            if (localWalletInfo != null) {
                textWalletName.setText(localWalletInfo.getLabel());
                showTypeInfo(localWalletInfo);
                whetherBackup();
            }

            mAppWalletViewModel.refreshBalance();
            mAppWalletViewModel.refreshExistsWallet();
        });
        mAppWalletViewModel.currentWalletBalance.observe(this, balance -> {
            textBtcAmount.setText(balance);
        });
        mAppWalletViewModel.currentWalletFiatBalance.observe(this, balance -> {
            String currencySymbol = preferences.getString(CURRENT_CURRENCY_GRAPHIC_SYMBOL, "¥");
            tetAmount.setText(String.format("%s %s", currencySymbol, Strings.isNullOrEmpty(balance) ? getString(R.string.zero) : balance));
            textDollar.setText(String.format("≈ %s %s", currencySymbol, Strings.isNullOrEmpty(balance) ? getString(R.string.zero) : balance));
        });
    }

    private void showTypeInfo(LocalWalletInfo localWalletInfo) {
        nowType = localWalletInfo.getType();
        if (org.haobtc.onekey.constant.Constant.Btc_Derived_Standard.equals(nowType)) {
            PreferencesManager.put(getContext(), "Preferences", org.haobtc.onekey.constant.Constant.HAS_LOCAL_HD, true);
        }
        edit.putString(CURRENT_SELECTED_WALLET_TYPE, nowType);
        edit.apply();
        if (nowType.contains("btc")) {
            imgType.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.token_btc));
        } else if (nowType.contains("eth")) {
            imgType.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.token_eth));
        } else {
            imgType.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.loco_round));
        }
        if (nowType.contains("hw")) {
            textHard.setVisibility(View.VISIBLE);
            linearSign.setVisibility(View.VISIBLE);
            String deviceId = localWalletInfo.getDeviceId();
            String deviceInfo = PreferencesManager.get(getContext(), org.haobtc.onekey.constant.Constant.DEVICES, deviceId, "").toString();
            if (!Strings.isNullOrEmpty(deviceInfo)) {
                HardwareFeatures info = HardwareFeatures.objectFromData(deviceInfo);
                String bleName = info.getBleName();
                String label = info.getLabel();
                bleMac = PreferencesManager.get(getContext(), org.haobtc.onekey.constant.Constant.BLE_INFO, bleName, "").toString();
                textHard.setText(Strings.isNullOrEmpty(label) ? bleName : label);
            } else {
                textHard.setText(deviceId);
            }
        } else {
            linearSign.setVisibility(View.GONE);
            textHard.setVisibility(View.GONE);
        }
    }

    /**
     * 判断当前钱包是否需要备份
     */
    private void whetherBackup() {
        try {
            isBackup = PyEnv.hasBackup(getActivity());
            if (isBackup) {
                relNowBackUp.setVisibility(View.GONE);
            } else {
                //no back up
                relNowBackUp.setVisibility(View.VISIBLE);
                whetherBackupDialog();
            }
        } catch (Exception e) {
            e.printStackTrace();
            relNowBackUp.setVisibility(View.GONE);
        }
    }

    /**
     * 判断备份提示弹窗是否需要出现
     */
    private void whetherBackupDialog() {
        //whether to pop backup dialog
        if (mSystemConfigManager.getNeedPopBackUpDialog()) {
            if (mBackupDialog != null && !mBackupDialog.isDetached()) {
                mBackupDialog.dismiss();
            }
            mBackupDialog = new BackupDialog();
            mBackupDialog.show(getChildFragmentManager(), "backup");
        }
    }

    /**
     * 统一处理硬件连接
     */
    private void deal(@IdRes int id) {
        if (org.haobtc.onekey.constant.Constant.WALLET_TYPE_HARDWARE.equals(nowType)) {
            currentAction = id;
            if (Strings.isNullOrEmpty(bleMac)) {
                Toast.makeText(getContext(), getString(R.string.not_found_device_msg), Toast.LENGTH_SHORT).show();
            } else {
                Intent intent2 = new Intent(getActivity(), SearchDevicesActivity.class);
                intent2.putExtra(org.haobtc.onekey.constant.Constant.SEARCH_DEVICE_MODE, org.haobtc.onekey.constant.Constant.SearchDeviceMode.MODE_PREPARE);
                startActivity(intent2);
                BleManager.getInstance(getActivity()).connDevByMac(bleMac);
            }
            return;
        }
        toNext(id);
    }

    /**
     * 处理具体业务
     */
    private void toNext(int id) {
        switch (id) {
            case R.id.linear_send:
                Intent intent2 = new Intent(getActivity(), SendHdActivity.class);
                intent2.putExtra(WALLET_BALANCE, changeBalance);
                intent2.putExtra("hdWalletName", textWalletName.getText().toString());
                startActivity(intent2);
                break;
            case R.id.linear_receive:
                Intent intent3 = new Intent(getActivity(), ReceiveHDActivity.class);
                if (org.haobtc.onekey.constant.Constant.WALLET_TYPE_HARDWARE.equals(nowType)) {
                    intent3.putExtra(org.haobtc.onekey.constant.Constant.WALLET_TYPE, org.haobtc.onekey.constant.Constant.WALLET_TYPE_HARDWARE_PERSONAL);
                }
                startActivity(intent3);
                break;
            case R.id.linear_sign:
                Intent intent8 = new Intent(getActivity(), SignActivity.class);
                intent8.putExtra(org.haobtc.onekey.constant.Constant.WALLET_LABEL, textWalletName.getText().toString());
                if (org.haobtc.onekey.constant.Constant.WALLET_TYPE_HARDWARE.equals(nowType)) {
                    intent8.putExtra(org.haobtc.onekey.constant.Constant.WALLET_TYPE, org.haobtc.onekey.constant.Constant.WALLET_TYPE_HARDWARE_PERSONAL);
                }
                startActivity(intent8);
                break;
            default:
        }
    }

    /**
     * 备份钱包响应
     */
    @Subscribe
    public void onBack(BackupEvent event) {
        Intent intent = new Intent(getActivity(), BackupGuideActivity.class);
        intent.putExtra(CURRENT_SELECTED_WALLET_TYPE, nowType);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnected(BleConnectedEvent event) {
        toNext(currentAction);
        currentAction = 0;
    }

    /**
     * 连接硬件超时响应
     */
    @Subscribe
    public void onConnectionTimeout(BleConnectionEx connectionEx) {
        if (connectionEx == BleConnectionEx.BLE_CONNECTION_EX_TIMEOUT) {
            Toast.makeText(getContext(), R.string.ble_connect_timeout, Toast.LENGTH_SHORT).show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void fixName(FixWalletNameEvent event) {
        mAppWalletViewModel.refreshWalletInfo();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Scan QR code / barcode return
        if (requestCode == 0 && resultCode == RESULT_OK) {
            if (data != null) {
                String content = data.getStringExtra(Constant.CODED_CONTENT);
                if (!TextUtils.isEmpty(content)) {
                    PyObject parseQr;
                    try {
                        parseQr = Daemon.commands.callAttr("parse_pr", content);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), e.getMessage().replace("BaseException:", ""), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (parseQr.toString().length() > 2) {
                        String strParse = parseQr.toString();
                        String substring = strParse.substring(20);
                        String detailScan = substring.substring(0, substring.length() - 1);
                        try {
                            JSONObject jsonObject = new JSONObject(strParse);
                            int type = jsonObject.getInt("type");
                            Gson gson = new Gson();
                            if (type == 1) {
                                MainSweepcodeBean mainSweepcodeBean = gson.fromJson(strParse, MainSweepcodeBean.class);
                                MainSweepcodeBean.DataBean listData = mainSweepcodeBean.getData();
                                String address = listData.getAddress();
                                SendHdActivity.start(getActivity(), changeBalance, textWalletName.getText().toString(), address, listData.getAmount());
                            } else if (type == 2) {
                                Intent intent = new Intent(getActivity(), DetailTransactionActivity.class);
                                intent.putExtra("scanDetail", detailScan);
                                intent.putExtra("detailType", "homeScanDetail");
                                startActivity(intent);
                            } else {
                                Toast.makeText(getActivity(), getString(R.string.address_wrong), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), getString(R.string.address_wrong), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }
    }

    @OnCheckedChanged(R.id.img_check_money)
    public void onCheckedChange(boolean checked) {
        if (checked) {
            tetAmount.setVisibility(View.VISIBLE);
            textStar.setVisibility(View.GONE);
            textBtcAmount.setVisibility(View.VISIBLE);
            textDollar.setVisibility(View.VISIBLE);
            amountStars.setVisibility(View.GONE);
            dollarStars.setVisibility(View.GONE);
        } else {
            tetAmount.setVisibility(View.GONE);
            textStar.setVisibility(View.VISIBLE);
            textBtcAmount.setVisibility(View.GONE);
            textDollar.setVisibility(View.GONE);
            amountStars.setVisibility(View.VISIBLE);
            dollarStars.setVisibility(View.VISIBLE);
        }
    }

    @SingleClick(value = 1000L)
    @OnClick({R.id.rel_check_wallet, R.id.img_scan, R.id.img_Add, R.id.rel_create_hd, R.id.rel_recovery_hd, R.id.rel_pair_hard, R.id.rel_wallet_detail, R.id.linear_send, R.id.linear_receive, R.id.linear_sign, R.id.rel_now_back_up, R.id.rel_bi_detail})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rel_check_wallet:
                Intent intent1 = new Intent(getActivity(), WalletListActivity.class);
                startActivity(intent1);
                break;
            case R.id.img_scan:
                rxPermissions
                        .request(Manifest.permission.CAMERA)
                        .subscribe(new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean granted) throws Exception {
                                if (granted) {
                                    // Always true pre-M
                                    //If you have already authorized it, you can directly jump to the QR code scanning interface
                                    Intent intent2 = new Intent(getActivity(), CaptureActivity.class);
                                    ZxingConfig config = new ZxingConfig();
                                    config.setPlayBeep(true);
                                    config.setShake(true);
                                    config.setDecodeBarCode(false);
                                    config.setFullScreenScan(true);
                                    config.setShowAlbum(false);
                                    config.setShowbottomLayout(false);
                                    intent2.putExtra(Constant.INTENT_ZXING_CONFIG, config);
                                    startActivityForResult(intent2, REQUEST_CODE);
                                } else {
                                    Toast.makeText(getActivity(), R.string.photopersion, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                break;
            case R.id.img_Add:
            case R.id.rel_create_hd:
                isAddHd = true;
                NavUtils.gotoSoftPassActivity(getActivity(), SoftPassActivity.SET, 0);
                break;
            case R.id.rel_recovery_hd:
                isRecovery = true;
                Intent intent = new Intent(getActivity(), RecoverHdWalletActivity.class);
                startActivity(intent);
                break;
            case R.id.rel_pair_hard:
                Intent pair = new Intent(getActivity(), SearchDevicesActivity.class);
                startActivity(pair);
                break;
            case R.id.rel_wallet_detail:
                Intent intent4 = new Intent(getActivity(), HdWalletDetailActivity.class);
                intent4.putExtra("hdWalletName", textWalletName.getText().toString());
                intent4.putExtra("isBackup", isBackup);
                startActivity(intent4);
                break;
            case R.id.linear_send:
            case R.id.linear_receive:
            case R.id.linear_sign:
                deal(view.getId());
                break;
            case R.id.rel_now_back_up:
                Intent intent6 = new Intent(getActivity(), BackupGuideActivity.class);
                intent6.putExtra(CURRENT_SELECTED_WALLET_TYPE, nowType);
                startActivity(intent6);
                break;
            case R.id.rel_bi_detail:
                if (!TextUtils.isEmpty(nowType)) {
                    Intent intent5 = new Intent(getActivity(), TransactionDetailWalletActivity.class);
                    if (nowType.contains("hw")) {
                        intent5.putExtra(org.haobtc.onekey.constant.Constant.BLE_MAC, bleMac);
                    }
                    intent5.putExtra("walletBalance", changeBalance);
                    intent5.putExtra("walletDollar", textDollar.getText().toString());
                    intent5.putExtra("hdWalletName", textWalletName.getText().toString());
                    startActivity(intent5);
                }
                break;
            default:
                break;
        }
    }

    private boolean shouldResponsePassEvent() {
        return (linearNoWallet.getVisibility() == View.VISIBLE && isAddHd);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGotPass(GotPassEvent event) {
        if (shouldResponsePassEvent()) {
            if (event.fromType == 0) {
                mSystemConfigManager.setNeedPopBackUpDialog(true);
                PyEnv.createLocalHd(event.getPassword(), null);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefresh(RefreshEvent refreshEvent) {
        if (shouldResponsePassEvent()) {
            mAppWalletViewModel.refresh();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBackupCompleteEvent(BackupCompleteEvent event) {
        whetherBackup();
    }

    /**
     * 注册EventBus
     */
    @Override
    public boolean needEvents() {
        return true;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (s.length() > 11) {
            tetAmount.setTextSize(26);
        } else {
            tetAmount.setTextSize(32);
        }
    }
}
