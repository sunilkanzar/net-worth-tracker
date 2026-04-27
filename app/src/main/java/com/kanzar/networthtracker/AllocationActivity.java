package com.kanzar.networthtracker;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.content.res.ColorStateList;
import android.view.Gravity;

import com.google.android.material.tabs.TabLayoutMediator;
import com.kanzar.networthtracker.databinding.ActivityAllocationBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Tools;

import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AllocationActivity extends AppCompatActivity {

    private Month month;
    private ActivityAllocationBinding binding;

    private boolean isTreemapMode = false;
    private MenuItem toggleMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAllocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        int selectedMonth = getIntent().getIntExtra("month", new Month().getMonth());
        int selectedYear = getIntent().getIntExtra("year", new Month().getYear());
        month = new Month(selectedMonth, selectedYear);

        setupViewPager();
        setupMonthNavigation();
        applyAccentColor();
        updateMonthUI();
    }

    private void setupViewPager() {
        binding.viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return AllocationPageFragment.newInstance(position == 0, month.getMonth(), month.getYear());
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            tab.setText(position == 0 ? R.string.allocation_assets : R.string.allocation_liabilities);
        }).attach();
    }

    private void setupMonthNavigation() {
        binding.previousMonth.setOnClickListener(v -> { month.previous(); updateMonthUI(); });
        binding.previousMonth.setOnLongClickListener(v -> goToCurrentMonth());

        binding.nextMonth.setOnClickListener(v -> { month.next(); updateMonthUI(); });
        binding.nextMonth.setOnLongClickListener(v -> goToCurrentMonth());

        binding.monthName.setOnClickListener(v -> showMonthYearPicker());
        binding.monthName.setOnLongClickListener(v -> goToCurrentMonth());

        // Swipe on bottom bar to change months
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(e1.getX() - e2.getX()) > Math.abs(e1.getY() - e2.getY())) {
                    if (e1.getX() - e2.getX() > 100) {
                        month.next();
                        updateMonthUI();
                        return true;
                    } else if (e2.getX() - e1.getX() > 100) {
                        month.previous();
                        updateMonthUI();
                        return true;
                    }
                }
                return false;
            }
        });

        binding.bottomAppBar.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyAccentColor();
    }

    private void applyAccentColor() {
        int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        ColorStateList accentList = ColorStateList.valueOf(accentColor);

        binding.previousMonth.setImageTintList(accentList);
        binding.nextMonth.setImageTintList(accentList);
        binding.tabLayout.setSelectedTabIndicatorColor(accentColor);
        binding.tabLayout.setTabTextColors(ContextCompat.getColor(this, R.color.text_3), accentColor);

        updateMonthNameColor();
    }

    private void updateMonthNameColor() {
        if (month == null) return;
        Calendar today = Calendar.getInstance();
        boolean isCurrent = (month.getMonth() == (today.get(Calendar.MONTH) + 1)) && (month.getYear() == today.get(Calendar.YEAR));
        int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        int defaultColor = ContextCompat.getColor(this, R.color.text);
        binding.monthName.setTextColor(isCurrent ? accentColor : defaultColor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_allocation, menu);
        toggleMenuItem = menu.findItem(R.id.action_toggle_chart);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_toggle_chart) {
            isTreemapMode = !isTreemapMode;
            applyChartMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applyChartMode() {
        if (toggleMenuItem != null) {
            toggleMenuItem.setIcon(isTreemapMode ? R.drawable.ic_donut : R.drawable.ic_treemap);
        }
        updateFragments();
    }

    private void updateMonthUI() {
        binding.monthName.setText(month.toString());
        updateMonthNameColor();
        updateFragments();
    }

    private void updateFragments() {
        AllocationPageFragment f0 = (AllocationPageFragment) getSupportFragmentManager().findFragmentByTag("f0");
        AllocationPageFragment f1 = (AllocationPageFragment) getSupportFragmentManager().findFragmentByTag("f1");

        if (f0 != null) {
            f0.setMonth(month.getMonth(), month.getYear());
            f0.setTreemapMode(isTreemapMode);
        }
        if (f1 != null) {
            f1.setMonth(month.getMonth(), month.getYear());
            f1.setTreemapMode(isTreemapMode);
        }
    }

    private void showMonthYearPicker() {
        String[] allMonths = new DateFormatSymbols().getMonths();
        String[] monthNames = Arrays.copyOf(allMonths, 12);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        NumberPicker monthPicker = new NumberPicker(this);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(monthNames);
        monthPicker.setValue(month.getMonth() - 1);
        monthPicker.setWrapSelectorWheel(true);

        NumberPicker yearPicker = new NumberPicker(this);
        yearPicker.setMinValue(1970);
        yearPicker.setMaxValue(currentYear + 5);
        yearPicker.setValue(month.getYear());

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        container.addView(monthPicker, params);
        container.addView(yearPicker, params);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Go to month")
                .setView(container)
                .setPositiveButton("Go", (d, which) -> {
                    month.setYear(yearPicker.getValue());
                    month.setMonth(monthPicker.getValue() + 1);
                    updateMonthUI();
                })
                .setNegativeButton("Cancel", null)
                .create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    private boolean goToCurrentMonth() {
        Calendar now = Calendar.getInstance();
        month = new Month(now.get(Calendar.MONTH) + 1, now.get(Calendar.YEAR));
        updateMonthUI();
        return true;
    }
}
