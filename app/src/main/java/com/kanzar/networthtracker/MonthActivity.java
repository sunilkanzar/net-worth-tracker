package com.kanzar.networthtracker;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.kanzar.networthtracker.adapters.MonthAdapter;
import com.kanzar.networthtracker.databinding.ActivityMonthBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Tools;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MonthActivity extends AppCompatActivity implements MonthAdapter.OnItemClickListener {

    private MonthAdapter adapter;
    private ActivityMonthBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMonthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        List<Month> months = new ArrayList<>();
        Month first = new Month().getFirst();
        Month last = new Month().getLast();
        months.add(first);

        while (!(first.getMonth() == last.getMonth() && first.getYear() == last.getYear())) {
            Month nextMonth = new Month(first.getMonth(), first.getYear());
            nextMonth.next();
            months.add(nextMonth);
            first = nextMonth;
        }

        Collections.reverse(months);

        setupHeroCard(months);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MonthAdapter(months, this);
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupHeroCard(List<Month> months) {
        if (months.isEmpty()) {
            binding.heroCard.setVisibility(android.view.View.GONE);
            return;
        }

        double latestTotal = months.get(0).getValue();
        double minVal = Double.MAX_VALUE;
        double maxVal = -Double.MAX_VALUE;

        // Calculate for the last 12 months (or fewer if not available)
        int range = Math.min(months.size(), 12);
        for (int i = 0; i < range; i++) {
            double val = months.get(i).getValue();
            if (val < minVal) minVal = val;
            if (val > maxVal) maxVal = val;
        }

        binding.heroTotal.setText(Tools.formatAmount(latestTotal, true));
        binding.heroDate.setText(months.get(0).toStringMMMYY());
        binding.heroMinMax.setText(String.format("Low %s · High %s",
                Tools.formatAmount(minVal), Tools.formatAmount(maxVal)));
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
