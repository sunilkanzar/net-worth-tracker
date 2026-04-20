package com.kanzar.networthtracker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MonthPagerAdapter extends FragmentStateAdapter {

    private static final int START_YEAR  = 2010;
    private static final int START_MONTH = 1;
    static final int PAGE_COUNT = 30 * 12; // Jan 2010 – Dec 2039

    public MonthPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    public static int positionOf(int month, int year) {
        return (year - START_YEAR) * 12 + (month - START_MONTH);
    }

    public static int[] monthYearAt(int position) {
        int month = START_MONTH + (position % 12);
        int year  = START_YEAR  + (position / 12);
        return new int[]{month, year};
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        int[] my = monthYearAt(position);
        return MonthPageFragment.newInstance(my[0], my[1]);
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}
