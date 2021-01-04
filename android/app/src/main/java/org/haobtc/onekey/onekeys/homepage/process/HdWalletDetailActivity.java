package org.haobtc.onekey.onekeys.homepage.process;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;

import com.chaquo.python.Kwarg;
import com.chaquo.python.PyObject;
import com.google.common.base.Strings;
import com.lxj.xpopup.XPopup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.haobtc.onekey.R;
import org.haobtc.onekey.activities.base.BaseActivity;
import org.haobtc.onekey.aop.SingleClick;
import org.haobtc.onekey.bean.CurrentAddressDetail;
import org.haobtc.onekey.bean.LocalWalletInfo;
import org.haobtc.onekey.bean.PyResponse;
import org.haobtc.onekey.constant.Constant;
import org.haobtc.onekey.constant.StringConstant;
import org.haobtc.onekey.event.FixWalletNameEvent;
import org.haobtc.onekey.event.GotPassEvent;
import org.haobtc.onekey.event.LoadOtherWalletEvent;
import org.haobtc.onekey.event.SecondEvent;
import org.haobtc.onekey.manager.PreferencesManager;
import org.haobtc.onekey.manager.PyEnv;
import org.haobtc.onekey.onekeys.backup.BackupGuideActivity;
import org.haobtc.onekey.onekeys.homepage.mindmenu.DeleteWalletActivity;
import org.haobtc.onekey.ui.activity.SoftPassActivity;
import org.haobtc.onekey.ui.dialog.BackupRequireDialog;
import org.haobtc.onekey.ui.dialog.DeleteWalletTipsDialog;
import org.haobtc.onekey.ui.dialog.ExportTipsDialog;
import org.haobtc.onekey.ui.dialog.custom.CustomBackupDialog;
import org.haobtc.onekey.ui.dialog.custom.CustomReSetBottomPopup;
import org.haobtc.onekey.utils.ClipboardUtils;
import org.haobtc.onekey.utils.Daemon;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.haobtc.onekey.constant.Constant.CURRENT_SELECTED_WALLET_NAME;
import static org.haobtc.onekey.constant.Constant.CURRENT_SELECTED_WALLET_TYPE;

public class HdWalletDetailActivity extends BaseActivity {

    @BindView(R.id.text_wallet_name)
    TextView textWalletName;
    @BindView(R.id.text_address)
    TextView textAddress;
    @BindView(R.id.lin_hd_wallet_show)
    LinearLayout linHdWalletShow;
    @BindView(R.id.lin_single_show)
    LinearLayout linSingleShow;
    @BindView(R.id.text_hd_wallet)
    TextView textHdWallet;
    @BindView(R.id.lin_single)
    LinearLayout linSingle;
    @BindView(R.id.text_sign)
    TextView textSign;
    @BindView(R.id.lin_hardware)
    LinearLayout linHardware;
    @BindView(R.id.text_addr)
    TextView textAddr;
    @BindView(R.id.text_content_type)
    TextView textContentType;
    @BindView(R.id.rel_delete_wallet)
    RelativeLayout deleteLayout;
    @BindView(R.id.delete_tv)
    TextView deleteTV;
    private String type;
    private boolean isBackup;
    private SharedPreferences preferences;
    private String showWalletType;
    private int currentOperation;
    private static final int DELETE_HD_DERIVED = 100;

    @Override
    public int getLayoutId() {
        return R.layout.activity_hd_wallet_detail;
    }

    @Override
    public void initView() {
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        inits();
    }

    private void inits() {
        preferences = getSharedPreferences("Preferences", MODE_PRIVATE);
        showWalletType = preferences.getString(CURRENT_SELECTED_WALLET_TYPE, "");
        String hdWalletName = getIntent().getStringExtra("hdWalletName");
        isBackup = getIntent().getBooleanExtra("isBackup", false);
        textWalletName.setText(hdWalletName);
        if (StringConstant.Btc_Derived_Standard.equals(showWalletType)) {
            textHdWallet.setText(getString(R.string.hd_wallet));
            //HD wallet detail and derive wallet
            linHdWalletShow.setVisibility(View.VISIBLE);
            linSingleShow.setVisibility(View.GONE);
            deleteTV.setText(R.string.delete_wallet_single);
        } else if (showWalletType.contains(StringConstant.HW)) {
            textHdWallet.setText(getString(R.string.hardware_wallet));
            linSingleShow.setVisibility(View.VISIBLE);
            linSingle.setVisibility(View.GONE);
            deleteTV.setText(R.string.delete_wallet_single);
//            linHardware.setVisibility(View.VISIBLE);
//            type = showWalletType.substring(showWalletType.indexOf("hw-") + 3);
//            textSign.setText(String.format("%s %s", type, getString(R.string.sign_num)));
//            textContentType.setText(getString(R.string.sign_num_tip));
        } else if (showWalletType.contains(StringConstant.Watch)) {
            textHdWallet.setText(getString(R.string.watch_wallet));
            deleteTV.setText(R.string.delete_wallet_single);
            linSingle.setVisibility(View.GONE);
        } else if (StringConstant.Btc_Standard.equals(showWalletType) || StringConstant.Btc_Private_Standard.equals(showWalletType)) {
            //Independent Wallet
            linHdWalletShow.setVisibility(View.GONE);
            linSingleShow.setVisibility(View.VISIBLE);
            textHdWallet.setText(getString(R.string.single_wallet));
        }
    }

    @Override
    public void initData() {
        //get receive address
        getAddressInfo();
    }

    private void getAddressInfo() {
        PyResponse<CurrentAddressDetail> response = PyEnv.getCurrentAddressInfo();
        String error = response.getErrors();
        if (Strings.isNullOrEmpty(error)) {
            String addr = response.getResult().getAddr();
            String front6 = addr.substring(0, 6);
            String after6 = addr.substring(addr.length() - 6);
            textAddr.setText(addr);
            textAddress.setText(String.format("%s…%s", front6, after6));
        }
    }

    @SingleClick
    @OnClick({R.id.img_back, R.id.img_copy, R.id.rel_export_word, R.id.rel_export_private_key,
            R.id.rel_export_keystore, R.id.rel_delete_wallet, R.id.text_wallet_name, R.id.text_sign})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_back:
                finish();
                break;
            case R.id.img_copy:
                //copy text
                ClipboardUtils.copyText(this, textAddr.getText().toString());
                break;
            case R.id.text_wallet_name:
                //fix wallet name
                fixWalletNameDialog(HdWalletDetailActivity.this, R.layout.edit_wallet_name);
                break;
            case R.id.rel_export_word:
            case R.id.rel_export_private_key:
            case R.id.rel_export_keystore:
                export(view.getId());
                break;
            case R.id.rel_delete_wallet:
                if (StringConstant.Btc_Derived_Standard.equals(showWalletType)) {
                    showDeleteDialog();
                } else {
                    if (showWalletType.contains(StringConstant.HW) || showWalletType.contains(StringConstant.Watch)) {
                        DeleteWalletTipsDialog dialog = new DeleteWalletTipsDialog();
                        if (showWalletType.contains(StringConstant.Watch)) {
                            Bundle bundle = new Bundle();
                            bundle.putInt(Constant.WALLET_TYPE, 1);
                            dialog.setArguments(bundle);
                        }
                        dialog.show(getSupportFragmentManager(), "");
                    } else {
                        Intent intent = new Intent(HdWalletDetailActivity.this, DeleteWalletActivity.class);
                        intent.putExtra("importHdword", "deleteSingleWallet");
                        intent.putExtra("walletName", textWalletName.getText().toString());
                        intent.putExtra("isBackup", isBackup);
                        intent.putExtra("delete_wallet_type", showWalletType);
                        startActivity(intent);
                    }
                }
                break;
            case R.id.text_sign:
//                Intent intent1 = new Intent(HdWalletDetailActivity.this, SignActivity.class);
//                intent1.putExtra("personceType", type);
//                startActivity(intent1);
//                break;
            default:
        }
    }

    private void showDeleteDialog() {
        new XPopup.Builder(mContext)
                .dismissOnTouchOutside(false)
                .isDestroyOnDismiss(true)
                .asCustom(new CustomReSetBottomPopup(mContext, new CustomReSetBottomPopup.onClick() {
                    @Override
                    public void onConfirm () {
                        if (!isBackup) {
                            showBackDialog();
                        } else {
                            doSelect();
                        }
                    }
                }, CustomReSetBottomPopup.deleteHdChildren)).show();
    }

    private String deleteHdWalletName;

    private void doSelect () {
        PyResponse<String> response = PyEnv.getDerivedNum("btc");
        if (Strings.isNullOrEmpty(response.getErrors())) {
            int coinNum = Integer.parseInt(response.getResult());
            if (coinNum <= 1) {
                showDeleteHDRootWallet();
            } else {
                showDeleteHdDeriveWallet();
            }
        }
    }

    private void showDeleteHDRootWallet () {
        new XPopup.Builder(mContext)
                .dismissOnTouchOutside(false)
                .isDestroyOnDismiss(true)
                .asCustom(new CustomReSetBottomPopup(mContext, new CustomReSetBottomPopup.onClick() {
                    @Override
                    public void onConfirm () {
                        deleteHdRoot();
                    }
                }, CustomReSetBottomPopup.deleteHdRoot)).show();
    }

    private void deleteHdRoot () {
        Map<String, ?> wallets = PreferencesManager.getAll(this, Constant.WALLETS);
        if (!wallets.isEmpty()) {
            wallets.entrySet().forEach(stringEntry -> {
                LocalWalletInfo info = LocalWalletInfo.objectFromData(stringEntry.getValue().toString());
                String type = info.getType();
                String name = info.getName();
//                if (Constant.WALLET_TYPE_LOCAL_HD.equals(type)) {
                deleteHdWalletName = name;
//                }
            });
        }
        hdWalletIsBackup();
    }

    private void hdWalletIsBackup () {
        Log.i("deleteHdWalletNamejxm", "hdWalletIsBackup: " + deleteHdWalletName);
        try {
            PyObject data = Daemon.commands.callAttr("get_backup_info", new Kwarg("name", deleteHdWalletName));
            boolean isBackup = data.toBoolean();
            if (isBackup) {
                Intent intent = new Intent(mContext, DeleteWalletActivity.class);
                intent.putExtra("deleteHdWalletName", deleteHdWalletName);
                startActivity(intent);
                finish();
            } else {
                //没备份提示备份
                new BackupRequireDialog(this).show(getSupportFragmentManager(), "");
            }
        } catch (Exception e) {
            mToast(e.getMessage());
            e.printStackTrace();
        }
    }

    private void showDeleteHdDeriveWallet () {
        currentOperation = DELETE_HD_DERIVED;
        startActivity(new Intent(this, SoftPassActivity.class));
    }

    private void showBackDialog () {
        new XPopup.Builder(mContext)
                .dismissOnTouchOutside(false)
                .isDestroyOnDismiss(true)
                .asCustom(new CustomBackupDialog(mContext, new CustomBackupDialog.onClick() {
                    @Override
                    public void onBack () {
                        finish();
                    }
                })).show();
    }

    private void export(int id) {
        switch (id) {
            case R.id.rel_export_word:
                currentOperation = R.id.rel_export_word;
                break;
            case R.id.rel_export_private_key:
                currentOperation = R.id.rel_export_private_key;
                break;
            case R.id.rel_export_keystore:
                currentOperation = R.id.rel_export_keystore;
                break;
        }
        new ExportTipsDialog().show(getSupportFragmentManager(), "export");
    }

    @Subscribe
    public void onGotPass(GotPassEvent event) {
        switch (currentOperation) {
            case R.id.rel_export_word:
                PyResponse<String> response = PyEnv.exportMnemonics(event.getPassword());
                String errors = response.getErrors();
                if (Strings.isNullOrEmpty(errors)) {
                    Intent intent = new Intent(this, BackupGuideActivity.class);
                    intent.putExtra("exportWord", response.getResult());
                    intent.putExtra("importHdword", "exportWord");
                    startActivity(intent);
                    finish();
                } else {
                    mlToast(errors);
                }
                break;
            case R.id.rel_export_private_key:
                PyResponse<String> response1 = PyEnv.exportPrivateKey(event.getPassword());
                String error = response1.getErrors();
                if (Strings.isNullOrEmpty(error)) {
                    Intent intent = new Intent(this, ExportPrivateActivity.class);
                    intent.putExtra("privateKey", response1.getResult());
                    startActivity(intent);
                    finish();
                } else {
                    mToast(error);
                }
                break;
            case R.id.rel_export_keystore:
                break;
            case DELETE_HD_DERIVED:
                deleteSingleWallet(event.getPassword());
                break;
        }
    }

    private void deleteSingleWallet (String password) {
        String keyName = PreferencesManager.get(this, "Preferences", Constant.CURRENT_SELECTED_WALLET_NAME, "").toString();
        PyResponse<Void> response = PyEnv.deleteWallet(password, keyName,false);
        String errors = response.getErrors();
        if (Strings.isNullOrEmpty(errors)) {
            onDeleteSuccess(keyName);
        } else {
            mlToast(errors);
        }
    }

    public void onDeleteSuccess (String walletName) {
        mToast(getString(R.string.delete_succse));
        PreferencesManager.remove(this, Constant.WALLETS, walletName);
        EventBus.getDefault().post(new LoadOtherWalletEvent());
        finish();
    }

    private void fixWalletNameDialog (Context context, @LayoutRes int resource) {
        //set see view
        View view = View.inflate(context, resource, null);
        Dialog dialogBtoms = new Dialog(context, R.style.dialog);
        EditText walletName = view.findViewById(R.id.edit_wallet_name);
        walletName.setText(textWalletName.getText().toString());
        walletName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged (CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 禁止EditText输入空格
                if (s.toString().contains(" ")) {
                    String[] str = s.toString().split(" ");
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < str.length; i++) {
                        sb.append(str[i]);
                    }
                    walletName.setText(sb.toString());
                    walletName.setSelection(start);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s.toString())) {
                    if (s.length() > 14) {
                        mToast(getString(R.string.name_lenth));
                    }
                }
            }
        });
        view.findViewById(R.id.btn_import).setOnClickListener(v -> {
            if (TextUtils.isEmpty(walletName.getText().toString())) {
                mToast(getString(R.string.please_input_walletname));
                return;
            }
            try {
                String keyName = PreferencesManager.get(this, "Preferences", CURRENT_SELECTED_WALLET_NAME, "").toString();
                Daemon.commands.callAttr("rename_wallet", keyName, walletName.getText().toString());
            } catch (Exception e) {
                e.printStackTrace();
                mToast(e.getMessage());
                return;
            }
            mToast(getString(R.string.fix_success));
            PyEnv.loadLocalWalletInfo(this);
            textWalletName.setText(walletName.getText().toString());
            EventBus.getDefault().post(new FixWalletNameEvent(walletName.getText().toString()));
            dialogBtoms.dismiss();
        });
        view.findViewById(R.id.img_cancel).setOnClickListener(v -> {
            dialogBtoms.dismiss();
        });
        dialogBtoms.setContentView(view);
        Window window = dialogBtoms.getWindow();
        //set pop_up size
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        //set locate
        window.setGravity(Gravity.BOTTOM);
        //set animal
        window.setWindowAnimations(R.style.AnimBottom);
        dialogBtoms.setCanceledOnTouchOutside(true);
        dialogBtoms.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void event(SecondEvent updataHint) {
        String msgVote = updataHint.getMsg();
        if ("finish".equals(msgVote)) {
            finish();
        }
    }
}
