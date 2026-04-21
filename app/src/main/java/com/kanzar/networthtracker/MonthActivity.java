package com.kanzar.networthtracker;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.kanzar.networthtracker.adapters.MonthAdapter;
import com.kanzar.networthtracker.databinding.ActivityMonthBinding;
import com.kanzar.networthtracker.helpers.Month;
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

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MonthAdapter(months, this);
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
