package org.haobtc.wallet.activities;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.haobtc.wallet.R;
import org.haobtc.wallet.activities.base.BaseActivity;
import org.haobtc.wallet.activities.settings.AgentServerActivity;
import org.haobtc.wallet.activities.settings.AnyskServerSetActivity;
import org.haobtc.wallet.activities.settings.BlockChooseActivity;
import org.haobtc.wallet.activities.settings.ElectrumNodeChooseActivity;
import org.haobtc.wallet.activities.settings.QuotationServerActivity;
import org.haobtc.wallet.aop.SingleClick;
import org.haobtc.wallet.bean.DefaultNodeBean;
import org.haobtc.wallet.event.FirstEvent;
import org.haobtc.wallet.utils.Daemon;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ServerSettingActivity extends BaseActivity {

    @BindView(R.id.img_back)
    ImageView imgBack;
    @BindView(R.id.rel_quotationChoose)
    RelativeLayout relQuotationChoose;
    @BindView(R.id.rel_blockChoose)
    RelativeLayout relBlockChoose;
    @BindView(R.id.rel_Electrum_Choose)
    RelativeLayout relElectrumChoose;
    @BindView(R.id.tet_defaultServer)
    TextView tetDefaultServer;
    @BindView(R.id.testBlockcheck)
    TextView testBlockcheck;
    @BindView(R.id.testElectrumNode)
    TextView testElectrumNode;
    @BindView(R.id.testNodeType)
    TextView testNodeType;
    private SharedPreferences preferences;
    private String exchangeName;
    private String blockServerLine;

    public int getLayoutId() {
        return R.layout.server_setting;
    }

    @SuppressLint("CommitPrefEdits")
    public void initView() {
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        preferences = getSharedPreferences("Preferences", MODE_PRIVATE);
        exchangeName = preferences.getString("exchangeName", "");
        blockServerLine = preferences.getString("blockServerLine", "");
        String strAgentIP = preferences.getString("strAgentIP", "");
        String strPort = preferences.getString("strPort", "");
        if (!TextUtils.isEmpty(strAgentIP)) {
            testNodeType.setText(String.format("%s:%s", strAgentIP, strPort));
        } else {
            testNodeType.setText(getString(R.string.ip_port));
        }
        inits();

    }

    private void inits() {
        //get electrum list
        getElectrumData();
        //get default Server
        if (!TextUtils.isEmpty(exchangeName)) {
            tetDefaultServer.setText(exchangeName);
        } else {
            getdefaultServer();
        }
        //get block Browser
        if (!TextUtils.isEmpty(blockServerLine)) {
            testBlockcheck.setText(blockServerLine);
        }

    }

    private void getElectrumData() {
        try {
            PyObject defaultServer = Daemon.commands.callAttr("get_default_server");
            Gson gson = new Gson();
            DefaultNodeBean defaultNodeBean = gson.fromJson(defaultServer.toString(), DefaultNodeBean.class);
            testElectrumNode.setText(String.format("%s:%s", defaultNodeBean.getHost(), defaultNodeBean.getPort()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initData() {

    }

    //get default Server
    private void getdefaultServer() {
        PyObject get_exchanges = null;
        try {
            get_exchanges = Daemon.commands.callAttr("get_exchanges");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (get_exchanges != null) {
            Log.i("get_exchanges", "getExchangelist: " + get_exchanges);
            List<PyObject> pyObjects = get_exchanges.asList();
            String defalutServer = pyObjects.get(0).toString();
            tetDefaultServer.setText(defalutServer);
        }
    }

    @SingleClick
    @OnClick({R.id.img_back, R.id.rel_quotationChoose, R.id.rel_blockChoose, R.id.rel_Electrum_Choose, R.id.relAgent_Choose, R.id.testNodeType})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_back:
                finish();
                break;
            case R.id.rel_quotationChoose:
                mIntent(QuotationServerActivity.class);
                break;
            case R.id.rel_blockChoose:
                mIntent(BlockChooseActivity.class);
                break;
            case R.id.rel_Electrum_Choose:
                mIntent(ElectrumNodeChooseActivity.class);
                break;
            case R.id.relAgent_Choose:
                mIntent(AgentServerActivity.class);
                break;
            case R.id.testNodeType:
                mIntent(AnyskServerSetActivity.class);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void event(FirstEvent updataHint) {
        String msgVote = updataHint.getMsg();
        if (msgVote.equals("defaultServer")) {
            exchangeName = preferences.getString("exchangeName", "");
            tetDefaultServer.setText(exchangeName);
        } else if (msgVote.equals("block_check")) {
            blockServerLine = preferences.getString("blockServerLine", "");
            testBlockcheck.setText(blockServerLine);
        } else if (msgVote.equals("add_anysk_server")) {
            String strAgentIP = preferences.getString("strAgentIP", "");
            String strPort = preferences.getString("strPort", "");
            testNodeType.setText(String.format("%s:%s", strAgentIP, strPort));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

}