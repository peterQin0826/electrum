package org.haobtc.onekey.adapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class FragmentMainAdapter extends FragmentPagerAdapter {
    private String[] mTitles;

    public FragmentMainAdapter (FragmentManager fm, String[] mTitles) {
        super(fm);
        this.mTitles = mTitles;
    }

    @NonNull
    @Override
    public Fragment getItem (int position) {
        return null;
    }

    @Override
    public int getCount () {
        return mTitles.length;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle (int position) {
        return mTitles[position];
    }

}
