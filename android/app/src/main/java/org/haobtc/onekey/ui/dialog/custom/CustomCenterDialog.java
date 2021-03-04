package org.haobtc.onekey.ui.dialog.custom;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.common.base.Strings;
import com.lxj.xpopup.core.CenterPopupView;
import org.haobtc.onekey.R;
import org.jetbrains.annotations.NotNull;

public class CustomCenterDialog extends CenterPopupView {
    private String title, content, cancel, confirm;

    @BindView(R.id.title)
    TextView titleTV;

    @BindView(R.id.text_content)
    TextView contentTV;

    @BindView(R.id.text_i_know)
    TextView confirmTV;

    @BindView(R.id.text_back)
    TextView cancelTV;

    private onConfirmClick onConfirmClick;
    // 默认处理确认的回调
    private boolean dealClick = true;
    private boolean onlyConfirmVisible = false;

    public void setCancel(String cancel) {
        this.cancel = cancel;
    }

    public void setConfirm(String confirm) {
        this.confirm = confirm;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setOnlyConfirmVisible(boolean visible) {
        this.onlyConfirmVisible = visible;
    }

    public CustomCenterDialog(@NonNull Context context, onConfirmClick onConfirmClick) {
        super(context);
        this.onConfirmClick = onConfirmClick;
    }

    public CustomCenterDialog(@NotNull Context context, boolean dealClick) {
        super(context);
        this.dealClick = dealClick;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        ButterKnife.bind(this);
        if (!Strings.isNullOrEmpty(title)) {
            titleTV.setText(title);
        }
        if (!Strings.isNullOrEmpty(content)) {
            contentTV.setText(content);
        }
        if (!Strings.isNullOrEmpty(cancel)) {
            cancelTV.setText(cancel);
        }
        if (!Strings.isNullOrEmpty(confirm)) {
            confirmTV.setText(confirm);
        }
        if (onlyConfirmVisible) {
            cancelTV.setVisibility(GONE);
        }
    }

    @OnClick({R.id.text_i_know, R.id.text_back})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.text_i_know:
                dismiss();
                break;
            case R.id.text_back:
                if (!dealClick) {
                    dismiss();
                } else {
                    onConfirmClick.onConfirm();
                }
                break;
        }
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.center_dialog_tip;
    }

    public interface onConfirmClick {
        void onConfirm();
    }
}
