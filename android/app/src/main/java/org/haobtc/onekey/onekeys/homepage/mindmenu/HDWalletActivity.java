package org.haobtc.onekey.onekeys.homepage.mindmenu;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.haobtc.onekey.R;
import org.haobtc.onekey.activities.base.BaseActivity;
import org.haobtc.onekey.adapter.WalletListAdapter;
import org.haobtc.onekey.aop.SingleClick;
import org.haobtc.onekey.bean.LocalWalletInfo;
import org.haobtc.onekey.constant.Constant;
import org.haobtc.onekey.event.LoadWalletlistEvent;
import org.haobtc.onekey.manager.PreferencesManager;
import org.haobtc.onekey.ui.dialog.HdWalletIntroductionDialog;
import org.haobtc.onekey.utils.NavUtils;

import java.util.ArrayList;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class HDWalletActivity extends BaseActivity {

    @BindView(R.id.img_back)
    ImageView imgBack;
    @BindView(R.id.text_wallet_num)
    TextView textWalletNum;
    @BindView(R.id.recl_wallet_list)
    RecyclerView reclWalletList;
    @BindView(R.id.lin_not_wallet)
    LinearLayout linNotWallet;
    @BindView(R.id.recl_add_wallet)
    RelativeLayout reclAddWallet;
    @BindView(R.id.text_manage)
    TextView textManage;
    private ArrayList<LocalWalletInfo> hdWalletList;
    private WalletListAdapter walletListAdapter;
    private String deleteHdWalletName = "";

    @Override
    public int getLayoutId() {
        return R.layout.activity_h_d_wallet;
    }

    @Override
    public void initView() {
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
    }

    @Override
    public void initData() {
        reclWalletList.setNestedScrollingEnabled(false);
        //wallet name and balance list
        hdWalletList = new ArrayList<>();
        walletListAdapter = new WalletListAdapter(hdWalletList);
        reclWalletList.setAdapter(walletListAdapter);
        getHomeWalletList();
    }

    @SingleClick
    @OnClick({R.id.img_back, R.id.text_manage, R.id.recl_add_wallet, R.id.img_what_hd, R.id.recl_add_hd_wallet, R.id.recl_recovery_wallet})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_back:
                finish();
                break;
            case R.id.text_manage:
                Intent intent1 = new Intent(HDWalletActivity.this, WalletManageActivity.class);
                intent1.putExtra("hd_num", hdWalletList.size());
                intent1.putExtra("deleteHdWalletName", deleteHdWalletName);
                startActivity(intent1);
                finish();
                break;
            case R.id.recl_add_wallet:
                NavUtils.gotoCreateDeriveChooseTypeActivity(mContext, false);
                break;
            case R.id.img_what_hd:
                new HdWalletIntroductionDialog().show(getSupportFragmentManager(), "");
                break;
            case R.id.recl_add_hd_wallet:
                break;
            case R.id.recl_recovery_wallet:
                break;
        }
    }

    private void getHomeWalletList() {
        hdWalletList.clear();
        Map<String, ?> wallets = PreferencesManager.getAll(this, Constant.WALLETS);
        if (wallets.isEmpty()) {
            textWalletNum.setText(String.valueOf(hdWalletList.size()));
            reclAddWallet.setVisibility(View.GONE);
            linNotWallet.setVisibility(View.VISIBLE);
            textManage.setVisibility(View.GONE);
        } else {
            wallets.entrySet().forEach(stringEntry -> {
                LocalWalletInfo info = LocalWalletInfo.objectFromData(stringEntry.getValue().toString());
                String type = info.getType();
                String name = info.getName();
                if ( "btc-derived-standard".equals(type)) {
                    hdWalletList.add(info);
                    deleteHdWalletName = name;
                }

            });
            textWalletNum.setText(String.valueOf(hdWalletList.size()));
            if (hdWalletList != null && hdWalletList.size() > 0) {
                walletListAdapter.notifyDataSetChanged();
            } else {
                reclAddWallet.setVisibility(View.GONE);
                linNotWallet.setVisibility(View.VISIBLE);
                textManage.setVisibility(View.GONE);
            }
        }
    }

    @Subscribe
    public void onLoad (LoadWalletlistEvent event) {
        getHomeWalletList();
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

}
