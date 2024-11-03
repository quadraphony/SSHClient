package ssh2.matss.ph.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import ssh2.matss.ph.fragments.AboutFragment;
import ssh2.matss.ph.fragments.HomeFragment;
import ssh2.matss.ph.fragments.LogsFragment;
import ssh2.matss.ph.fragments.SettingsFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new HomeFragment();
            case 1:
                return new LogsFragment();
            case 2:
                return new AboutFragment();
            case 3:
                return new SettingsFragment();
            default:
                return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4; // Total number of fragments
    }
}
