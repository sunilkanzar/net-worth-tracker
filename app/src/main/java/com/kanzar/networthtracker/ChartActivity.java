package com.kanzar.networthtracker;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.google.android.material.tabs.TabLayoutMediator;
import com.kanzar.networthtracker.databinding.ActivityChartBinding;

public class ChartActivity extends AppCompatActivity {

    private ActivityChartBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        setupViewPager();
    }

    private void setupViewPager() {
        ChartPagerAdapter adapter = new ChartPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setOffscreenPageLimit(2);

        String[] tabTitles = {
                getString(R.string.tab_consolidated),
                getString(R.string.tab_assets),
                getString(R.string.tab_liabilities)
        };
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();
    }

    private static class ChartPagerAdapter extends FragmentStateAdapter {
        public ChartPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 1:
                    return ChartTabFragment.newInstance("Assets");
                case 2:
                    return ChartTabFragment.newInstance("Liabilities");
                default:
                    return ChartTabFragment.newInstance("Consolidated");
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
