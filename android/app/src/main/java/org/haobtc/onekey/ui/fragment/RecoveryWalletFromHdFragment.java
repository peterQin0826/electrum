package org.haobtc.onekey.ui.fragment;

import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.Group;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.haobtc.onekey.R;
import org.haobtc.onekey.bean.BalanceInfo;
import org.haobtc.onekey.bean.FindOnceWalletEvent;
import org.haobtc.onekey.event.ExitEvent;
import org.haobtc.onekey.event.SelectedEvent;
import org.haobtc.onekey.ui.adapter.OnceWalletAdapter;
import org.haobtc.onekey.ui.base.BaseFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * @author liyan
 * @date 11/27/20
 */

public class RecoveryWalletFromHdFragment extends BaseFragment {

    @BindView(R.id.loaded_wallet)
    RelativeLayout loadedWallet;
    @BindView(R.id.wallet_rec)
    RecyclerView walletRec;
    @BindView(R.id.wallet_card)
    CardView walletCard;
    @BindView(R.id.recovery)
    Button recovery;
    @BindView(R.id.promote)
    TextView promote;
    @BindView(R.id.find_result_promote)
    TextView findResultPromote;
    @BindView(R.id.wallet_group)
    Group walletGroup;
    private OnceWalletAdapter adapter;
    private List<String> nameList;

    /**
     * init views
     *
     * @param view
     */
    @Override
    public void init(View view) {
        nameList = new ArrayList<>();
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onFind(FindOnceWalletEvent<BalanceInfo> event) {
        // avoid eventBus' needless deliver
        if (loadedWallet == null) {
            return;
        }
        loadedWallet.setVisibility(View.GONE);
        walletGroup.setVisibility(View.VISIBLE);
        if (event.getWallets().isEmpty()) {
            recovery.setText(R.string.back);
            findResultPromote.setText(R.string.not_found_once_wallet);
        } else {
            adapter = new OnceWalletAdapter(getContext(), event.getWallets());
            walletRec.setAdapter(adapter);
            walletRec.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter.setOnItemClickListener(new OnceWalletAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    refreshButton();
                }
            });
            adapter.notifyDataSetChanged();
        }
    }

    /***
     * init layout
     * @return
     */
    @Override
    public int getContentViewId() {
        return R.layout.looking_for_once_wallet;
    }

    private void refreshButton() {
        AtomicReference<Boolean> isChecked = new AtomicReference<>(false);
        adapter.getSelectMap().entrySet().forEach((entry) -> {
            if (entry.getValue().isChecked()) {
                isChecked.set(true);
            }
        });
        recovery.setEnabled(isChecked.get());
    }

    @OnClick(R.id.recovery)
    public void onViewClicked(View view) {
        if (recovery.getText().equals(getString(R.string.back))) {
            EventBus.getDefault().post(new ExitEvent());
        } else {
            adapter.getSelectMap().entrySet().forEach((entry) -> {
                if (entry.getValue().isChecked()) {
                    nameList.add(entry.getKey());
                }
            });
            if (nameList.isEmpty()) {
                showToast(R.string.recovery_wallet_select_promote);
            } else {
                EventBus.getDefault().post(new SelectedEvent(nameList));
            }
        }
    }

    @Override
    public boolean needEvents() {
        return true;
    }
}
