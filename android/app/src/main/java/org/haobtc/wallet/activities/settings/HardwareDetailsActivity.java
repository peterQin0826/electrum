package org.haobtc.wallet.activities.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.haobtc.wallet.BuildConfig;
import org.haobtc.wallet.R;
import org.haobtc.wallet.activities.base.BaseActivity;
import org.haobtc.wallet.activities.service.CommunicationModeSelector;
import org.haobtc.wallet.activities.settings.recovery_set.RecoverySetActivity;
import org.haobtc.wallet.aop.SingleClick;
import org.haobtc.wallet.bean.HardwareFeatures;
import org.haobtc.wallet.bean.UpdateInfo;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HardwareDetailsActivity extends BaseActivity {

    public static final String TAG = "org.haobtc.wallet.activities.settings.HardwareDetailsActivity";
    @BindView(R.id.img_back)
    ImageView imgBack;
    @BindView(R.id.tet_keyName)
    TextView tetKeyName;
    @BindView(R.id.lin_OnckOne)
    LinearLayout linOnckOne;
    @BindView(R.id.tet_code)
    TextView tetCode;
    @BindView(R.id.lin_OnckTwo)
    LinearLayout linOnckTwo;
    @BindView(R.id.change_pin)
    LinearLayout changePin;
    @BindView(R.id.tet_noPasspay)
    TextView tetNoPasspay;
    @BindView(R.id.lin_OnckFour)
    LinearLayout linOnckFour;
    @BindView(R.id.wipe_device)
    LinearLayout wipe_device;
    public static boolean dismiss;
    private String bleName;
    private String firmwareVersion;
    private String device_id;
    private String bleVerson;
    private String label;

    @Override
    public int getLayoutId() {
        return R.layout.activity_somemore;
    }

    @Override
    public void initView() {
        ButterKnife.bind(this);

        inits();
    }

    private void inits() {
        Intent intent = getIntent();
        bleName = intent.getStringExtra("bleName");
        firmwareVersion = intent.getStringExtra("firmwareVersion");
        bleVerson = intent.getStringExtra("bleVerson");
        device_id = intent.getStringExtra("device_id");
        label = intent.getStringExtra("label");
        if (!TextUtils.isEmpty(label)) {
            tetKeyName.setText(label);
        } else {
            tetKeyName.setText(String.format("%s", "BixinKEY"));
        }
        tetCode.setText(firmwareVersion);

    }

    @Override
    public void initData() {

    }

    @SingleClick
    @OnClick({R.id.img_back, R.id.lin_OnckOne, R.id.lin_OnckTwo, R.id.change_pin, R.id.lin_OnckFour, R.id.wipe_device, R.id.tetBluetoothSet})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_back:
                finish();
                break;
            case R.id.lin_OnckOne:
                Intent intent = new Intent(HardwareDetailsActivity.this, BixinKeyMessageActivity.class);
                intent.putExtra("bleName", bleName);
                intent.putExtra("label", label);
                intent.putExtra("device_id", device_id);
                startActivity(intent);
                break;
            case R.id.lin_OnckTwo:
                getUpdateInfo();
            break;
            case R.id.change_pin:
                Intent intent1 = new Intent(this, CommunicationModeSelector.class);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                intent1.putExtra("tag", TAG);
                startActivity(intent1);
                break;
            case R.id.lin_OnckFour:
                mIntent(ConfidentialPaymentSettings.class);
                break;
            case R.id.wipe_device:
                mIntent(RecoverySetActivity.class);
                break;
            case R.id.tetBluetoothSet:
                mIntent(BixinKeyBluetoothSettingActivity.class);
                break;
        }
    }
    private void getUpdateInfo() {
        // version_testnet.json version_regtest.json
        String appId = BuildConfig.APPLICATION_ID;
        String urlPrefix = "https://key.bixin.com/";
        String url = "";
        if (appId.endsWith("mainnet")) {
            url = urlPrefix + "version.json";
        } else if (appId.endsWith("testnet")) {
            url = urlPrefix + "version_testnet.json";
        } else if(appId.endsWith("regnet")) {
            url = urlPrefix + "version_regtest.json";
        }
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Call call = okHttpClient.newCall(request);
        runOnUiThread(() -> Toast.makeText(this, "正在检查更新信息", Toast.LENGTH_LONG).show());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(HardwareDetailsActivity.this, "获取更新信息失败", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                assert response.body() != null;
                SharedPreferences preferences = getSharedPreferences("Preferences", MODE_PRIVATE);
                String locate = preferences.getString("language", "");
                String info = response.body().string();
                UpdateInfo updateInfo = UpdateInfo.objectFromData(info);
                String urlNrf = updateInfo.getNrf().getUrl();
                String urlStm32 = updateInfo.getStm32().getUrl();
                String versionNrf = updateInfo.getNrf().getVersion();
                String versionStm32 = updateInfo.getStm32().getVersion().toString().replace(",", ".");
                versionStm32 = versionStm32.substring(1, versionStm32.length() -1).replaceAll("\\s+", "");
                String descriptionNrf = "English".equals(locate) ? updateInfo.getNrf().getChangelogEn() : updateInfo.getNrf().getChangelogCn();
                String descriptionStm32 = "English".equals(locate) ? updateInfo.getStm32().getChangelogEn() : updateInfo.getNrf().getChangelogCn();
                Bundle bundle = new Bundle();
                bundle.putString("nrf_url", urlPrefix + urlNrf);
                bundle.putString("stm32_url", urlPrefix + urlStm32);
                bundle.putString("nrf_version", versionNrf);
                bundle.putString("stm32_version", versionStm32);
                bundle.putString("nrf_description", descriptionNrf);
                bundle.putString("stm32_description", descriptionStm32);
                Intent intentVersion = new Intent(HardwareDetailsActivity.this, VersionUpgradeActivity.class);
                intentVersion.putExtras(bundle);
                startActivity(intentVersion);
            }
        });
    }

}