package org.haobtc.onekey.ui.dialog;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.haobtc.onekey.R;
import org.haobtc.onekey.aop.SingleClick;
import org.haobtc.onekey.business.wallet.SystemConfigManager;
import org.haobtc.onekey.event.BackupEvent;
import org.haobtc.onekey.ui.base.BaseDialogFragment;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * @author liyan
 * @date 12/9/20
 */

public class BackupDialog extends BaseDialogFragment {

    @BindView(R.id.img_close)
    ImageView imgClose;
    @BindView(R.id.btn_next_backup)
    Button btnNextBackup;
    @BindView(R.id.btn_now_backup)
    Button btnNowBackup;
    private SystemConfigManager mSystemConfigManager;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSystemConfigManager = new SystemConfigManager(view.getContext());
    }

    /***
     * init layout
     * @return
     */
    @Override
    public int getContentViewId() {
        return R.layout.backup_wallet;
    }

    @SingleClick
    @OnClick({R.id.img_close, R.id.btn_next_backup, R.id.btn_now_backup})
    public void onViewClicked(View view) {
        mSystemConfigManager.setNeedPopBackUpDialog(false);
        switch (view.getId()) {
            case R.id.img_close:
            case R.id.btn_next_backup:
                dismiss();
                break;
            case R.id.btn_now_backup:
                EventBus.getDefault().post(new BackupEvent());
                dismiss();
                break;
        }
    }
}
