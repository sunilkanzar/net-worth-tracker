package com.kanzar.networthtracker;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.kanzar.networthtracker.adapters.GroupedMonthAdapter;
import com.kanzar.networthtracker.adapters.MonthAdapter;
import com.kanzar.networthtracker.databinding.ActivityMonthBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
import io.realm.Realm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MonthActivity extends AppCompatActivity implements MonthAdapter.OnItemClickListener {

    private GroupedMonthAdapter adapter;
    private ActivityMonthBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMonthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        try (Realm realm = Realm.getDefaultInstance()) {
            List<Month> months = new ArrayList<>();
            Month first = new Month().getFirst();
            Month last = new Month().getLast();
            months.add(first);

            while (!(first.getMonth() == last.getMonth() && first.getYear() == last.getYear())) {
                Month nextMonth = new Month(first.getMonth(), first.getYear());
                nextMonth.next(realm);
                months.add(nextMonth);
                first = nextMonth;
            }

            Collections.reverse(months);

            setupGroupedRecyclerView(months);
        }
    }

    private void setupGroupedRecyclerView(List<Month> months) {
        int fyStartMonth = Prefs.getInt(Prefs.PREFS_FY_START_MONTH, Prefs.DEFAULT_FY_START_MONTH) + 1;
        List<GroupedMonthAdapter.YearGroup> groups = new ArrayList<>();
        GroupedMonthAdapter.YearGroup currentGroup = null;

        for (Month m : months) {
            int fyYearStart = (m.getMonth() >= fyStartMonth) ? m.getYear() : m.getYear() - 1;
            String label;
            if (fyStartMonth == 1) {
                label = String.valueOf(fyYearStart);
            } else {
                label = getString(R.string.financial_year_label, fyYearStart, (fyYearStart + 1) % 100);
            }

            if (currentGroup == null || !currentGroup.yearLabel.equals(label)) {
                currentGroup = new GroupedMonthAdapter.YearGroup(label);
                groups.add(currentGroup);
            }
            currentGroup.months.add(m);
        }

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GroupedMonthAdapter(groups, this);
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(Month item) {
        Intent result = new Intent();
        result.putExtra("month", item.getMonth());
        result.putExtra("year", item.getYear());
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onItemLongClick(Month item) {
    }
}
