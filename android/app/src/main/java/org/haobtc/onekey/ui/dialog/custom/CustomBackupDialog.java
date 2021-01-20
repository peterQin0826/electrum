package org.haobtc.onekey.ui.dialog.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.lxj.xpopup.core.BottomPopupView;

import org.haobtc.onekey.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * @Description: 需要备份的弹框
 * @Author: peter Qin
 */

public class CustomBackupDialog extends BottomPopupView {
    private Unbinder bind;
    @BindView(R.id.btn_back)
    Button backBtn;
    private Context context;
    private onClick onClick;

    public CustomBackupDialog (@NonNull Context context, onClick onClick) {
        super(context);
        this.context = context;
        this.onClick = onClick;
    }

    @Override
    protected void onCreate () {
        super.onCreate();
        bind = ButterKnife.bind(this);
        backBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick (View v) {
                dismiss();
                onClick.onBack();
            }
        });
    }

    @Override
    protected int getImplLayoutId () {
        return R.layout.confrim_delete_hdwallet;
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        bind.unbind();
    }

    public interface onClick {
        void onBack ();

    }

}
