package com.kanzar.networthtracker;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.card.MaterialCardView;
import com.kanzar.networthtracker.databinding.ActivityChartBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import io.realm.Realm;

public class ChartActivity extends AppCompatActivity {

    private ActivityChartBinding binding;
    private int currentMonthsToView = 12; // Default to 1Y

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        setupChartSelection();
        setupRangeSelector();
        updateUI();
    }

    private void setupChartSelection() {
        binding.chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                updateSelectionInfo(e);
            }

            @Override
            public void onNothingSelected() {
                binding.selectionCard.setVisibility(View.GONE);
            }
        });
    }

    private void updateSelectionInfo(Entry e) {
        int index = (int) e.getX();
        List<Month> months = binding.chart.getMonths();
        if (index < 0 || index >= months.size()) return;

        Month month = months.get(index);
        double value = month.getValue();
        double change;
        double percent;
        try (Realm realm = Realm.getDefaultInstance()) {
            change = month.getValueChange(realm);
            percent = month.getPercent(realm);
        }

        boolean privacyMode = Prefs.getBoolean("privacy_mode", false);

        binding.selectionCard.setVisibility(View.VISIBLE);
        int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        binding.selectionColor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));

        binding.tvSelectedMonth.setText(month.toStringMMMYY().toUpperCase(Locale.getDefault()));
        binding.tvSelectedValue.setText(privacyMode ? "****" : Tools.formatAmount(value));

        String changeSign = change >= 0 ? "+" : "";
        String changeStr = privacyMode ? "****" : Tools.formatAmount(change);
        String pctStr = privacyMode ? "**%" : String.format(Locale.US, "%.1f%%", Math.abs(percent));
        
        binding.tvSelectedChange.setText(String.format("%s%s (%s)", changeSign, changeStr, pctStr));
        binding.tvSelectedChange.setTextColor(ContextCompat.getColor(this, Tools.getTextChangeColor(change)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        binding.chart.updateData(currentMonthsToView);

        try (Realm realm = Realm.getDefaultInstance()) {
            Month lastMonth = new Month().getLast();
            double currentVal = lastMonth.getValue(realm);
            binding.tvCurrentValue.setText(Tools.formatAmount(currentVal));

            // Calculate change for the selected period
            Month firstInPeriod;
            String periodLabel;
            if (currentMonthsToView > 0) {
                firstInPeriod = new Month(lastMonth.getMonth(), lastMonth.getYear());
                for (int i = 0; i < currentMonthsToView - 1; i++) {
                    firstInPeriod.previous(realm);
                }
                periodLabel = currentMonthsToView + " mo";
            } else {
                firstInPeriod = new Month().getFirst();
                // Count total months for label
                int totalMonths = 0;
                Month temp = new Month(firstInPeriod.getMonth(), firstInPeriod.getYear());
                while (true) {
                    totalMonths++;
                    if (temp.getMonth() == lastMonth.getMonth() && temp.getYear() == lastMonth.getYear()) break;
                    temp.next(realm);
                }
                periodLabel = totalMonths + " mo";
            }
            
            double startVal = firstInPeriod.getValue(realm);
            double diff = currentVal - startVal;
            double pct = Tools.getPercent(startVal, currentVal);

            String changeSign = diff >= 0 ? "+" : "";
            String formattedDiff = Tools.formatAmount(diff);
            String pctStr = String.format(Locale.US, "%.1f%%", pct);
            
            String changeText = changeSign + formattedDiff + " (" + pctStr + ") · " + periodLabel;
            binding.tvChangeInfo.setText(changeText);
            binding.tvChangeInfo.setTextColor(ContextCompat.getColor(this, diff >= 0 ? R.color.positive : R.color.negative));

            // Stats
            List<Double> values = new ArrayList<>();
            List<Double> pcts = new ArrayList<>();
            Month m = new Month(firstInPeriod.getMonth(), firstInPeriod.getYear());
            double sum = 0;
            int count = 0;
            
            while (true) {
                double val = m.getValue(realm);
                values.add(val);
                sum += val;
                count++;
                
                Month prevMonth = m.getPreviousMonth(realm);
                if (prevMonth.hasAssets(realm)) {
                    pcts.add(Tools.getPercent(prevMonth.getValue(realm), val));
                }

                if (m.getMonth() == lastMonth.getMonth() && m.getYear() == lastMonth.getYear()) break;
                m.next(realm);
            }

            if (count > 0) {
                binding.tvStatAverage.setText(Tools.formatAmount(sum / count));
                
                double maxPct = pcts.isEmpty() ? 0 : Collections.max(pcts);
                String bestMonthSign = maxPct >= 0 ? "+" : "";
                String bestMonthStr = String.format(Locale.US, "%.2f%%", maxPct);
                binding.tvStatBestMonth.setText(String.format("%s%s", bestMonthSign, bestMonthStr));
                binding.tvStatBestMonth.setTextColor(ContextCompat.getColor(this, maxPct >= 0 ? R.color.positive : R.color.negative));
                
                // Simple Volatility calculation: SD / Mean
                double mean = sum / count;
                double temp = 0;
                for (double v : values) temp += (v - mean) * (v - mean);
                double sd = Math.sqrt(temp / count);
                double volatility = (mean != 0) ? (sd / mean) : 0;
                
                String volText = volatility < 0.05 ? "Low" : (volatility < 0.15 ? "Moderate" : "High");
                binding.tvStatVolatility.setText(volText);
                binding.tvStatVolatility.setTextColor(ContextCompat.getColor(this, volatility < 0.05 ? R.color.positive : (volatility < 0.15 ? R.color.amber : R.color.negative)));

                // CAGR (approx)
                if (count > 1 && startVal > 0 && currentVal > 0) {
                    double cagrValue = (Math.pow(currentVal / startVal, 1.0 / (count / 12.0)) - 1) * 100;
                    String cagrSign = cagrValue >= 0 ? "+" : "";
                    String cagrStr = String.format(Locale.US, "%.1f%%", cagrValue);
                    binding.tvStatCAGR.setText(String.format("%s%s", cagrSign, cagrStr));
                } else {
                    binding.tvStatCAGR.setText("N/A");
                }
            }
        }
    }

    private void setupRangeSelector() {
        binding.btn3M.setOnClickListener(v -> handleRangeClick(v, 3));
        binding.btn6M.setOnClickListener(v -> handleRangeClick(v, 6));
        binding.btn1Y.setOnClickListener(v -> handleRangeClick(v, 12));
        binding.btnAll.setOnClickListener(v -> handleRangeClick(v, -1));
    }

    private void handleRangeClick(View v, int months) {
        currentMonthsToView = months;
        updateRangeSelection(v.getId());
        updateUI();
    }

    private void updateRangeSelection(int id) {
        TextView[] btns = {binding.btn3M, binding.btn6M, binding.btn1Y, binding.btnAll};
        int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        for (TextView b : btns) {
            if (b.getId() == id) {
                b.setTextColor(ContextCompat.getColor(this, R.color.text));
                b.setBackgroundResource(R.drawable.bg_range_selected);
                b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
                b.setElevation(4f);
            } else {
                b.setTextColor(ContextCompat.getColor(this, R.color.text_3));
                b.setBackground(null);
                b.setElevation(0f);
            }
        }
    }
}
