package com.kanzar.networthtracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.kanzar.networthtracker.databinding.FragmentChartTabBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import io.realm.Realm;

import android.widget.ArrayAdapter;
import android.widget.NumberPicker;
import androidx.appcompat.app.AlertDialog;
import com.kanzar.networthtracker.databinding.DialogCustomRangeBinding;

public class ChartTabFragment extends Fragment {

    private static final String ARG_TYPE = "arg_type";
    private FragmentChartTabBinding binding;
    private String type;
    private int currentMonthsToView = 12;
    private Month customStartMonth = null;
    private Month customEndMonth = null;

    private static final String[] RANGE_OPTIONS = {"1Y", "2Y", "3Y", "5Y", "All", "Custom"};

    public static ChartTabFragment newInstance(String type) {
        ChartTabFragment fragment = new ChartTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = getArguments().getString(ARG_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChartTabBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        double value;
        double change;
        double percent;
        try (Realm realm = Realm.getDefaultInstance()) {
            value = month.getValue(realm, type);
            Month prevMonth = month.getPreviousMonth(realm);
            double prevValue = prevMonth.getValue(realm, type);
            change = value - prevValue;
            percent = Tools.getPercent(prevValue, value);
        }

        boolean privacyMode = Prefs.getBoolean("privacy_mode", false);

        binding.selectionCard.setVisibility(View.VISIBLE);
        int color;
        if ("Liabilities".equals(type)) {
            color = ContextCompat.getColor(requireContext(), R.color.negative);
        } else if ("Assets".equals(type)) {
            color = ContextCompat.getColor(requireContext(), R.color.positive);
        } else {
            color = ContextCompat.getColor(requireContext(), Tools.getAccentColor());
        }
        binding.selectionColor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));

        binding.tvSelectedMonth.setText(month.toStringMMMYY().toUpperCase(Locale.getDefault()));
        binding.tvSelectedValue.setText(privacyMode ? "****" : Tools.formatAmount(value));

        String changeSign = change >= 0 ? "+" : "";
        String changeStr = privacyMode ? "****" : Tools.formatAmount(change);
        String pctStr = privacyMode ? "**%" : String.format(Locale.US, "%.1f%%", Math.abs(percent));
        
        binding.tvSelectedChange.setText(String.format("%s%s (%s)", changeSign, changeStr, pctStr));
        
        int textChangeColor;
        if ("Liabilities".equals(type)) {
            textChangeColor = change > 0 ? R.color.negative : R.color.positive;
        } else {
            textChangeColor = change >= 0 ? R.color.positive : R.color.negative;
        }
        binding.tvSelectedChange.setTextColor(ContextCompat.getColor(requireContext(), textChangeColor));
    }

    private void updateUI() {
        if (binding == null) return;
        binding.chart.updateData(currentMonthsToView, type, customStartMonth, customEndMonth);

        try (Realm realm = Realm.getDefaultInstance()) {
            Month lastMonth = customEndMonth != null ? customEndMonth : new Month().getLast();
            double currentVal = lastMonth.getValue(realm, type);
            binding.tvCurrentValue.setText(Tools.formatAmount(currentVal));

            // Calculate change for the selected period
            Month firstInPeriod;
            String periodLabel;
            if (customStartMonth != null) {
                firstInPeriod = customStartMonth;
                int totalMonths = 0;
                Month temp = new Month(firstInPeriod.getMonth(), firstInPeriod.getYear());
                while (true) {
                    totalMonths++;
                    if (temp.getMonth() == lastMonth.getMonth() && temp.getYear() == lastMonth.getYear()) break;
                    temp.next(realm);
                }
                periodLabel = totalMonths + " mo";
            } else if (currentMonthsToView > 0) {
                firstInPeriod = new Month(lastMonth.getMonth(), lastMonth.getYear());
                for (int i = 0; i < currentMonthsToView - 1; i++) {
                    firstInPeriod.previous(realm);
                }
                periodLabel = currentMonthsToView + " mo";
            } else {
                firstInPeriod = new Month().getFirst();
                int totalMonths = 0;
                Month temp = new Month(firstInPeriod.getMonth(), firstInPeriod.getYear());
                while (true) {
                    totalMonths++;
                    if (temp.getMonth() == lastMonth.getMonth() && temp.getYear() == lastMonth.getYear()) break;
                    temp.next(realm);
                }
                periodLabel = totalMonths + " mo";
            }
            
            double startVal = firstInPeriod.getValue(realm, type);
            double diff = currentVal - startVal;
            double pct = Tools.getPercent(startVal, currentVal);

            String changeSign = diff >= 0 ? "+" : "";
            String formattedDiff = Tools.formatAmount(diff);
            String pctStr = String.format(Locale.US, "%.1f%%", pct);
            
            String changeText = changeSign + formattedDiff + " (" + pctStr + ") · " + periodLabel;
            binding.tvChangeInfo.setText(changeText);
            
            int diffColor;
            if ("Liabilities".equals(type)) {
                diffColor = diff > 0 ? R.color.negative : R.color.positive;
            } else {
                diffColor = diff >= 0 ? R.color.positive : R.color.negative;
            }
            binding.tvChangeInfo.setTextColor(ContextCompat.getColor(requireContext(), diffColor));

            // Stats
            List<Double> values = new ArrayList<>();
            List<Double> pcts = new ArrayList<>();
            Month m = new Month(firstInPeriod.getMonth(), firstInPeriod.getYear());
            double sum = 0;
            int count = 0;
            
            while (true) {
                double val = m.getValue(realm, type);
                values.add(val);
                sum += val;
                count++;
                
                Month prevMonth = m.getPreviousMonth(realm);
                if (prevMonth.hasAssets(realm)) {
                    pcts.add(Tools.getPercent(prevMonth.getValue(realm, type), val));
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
                
                int bestMonthColor;
                if ("Liabilities".equals(type)) {
                    bestMonthColor = maxPct < 0 ? R.color.positive : R.color.negative;
                } else {
                    bestMonthColor = maxPct >= 0 ? R.color.positive : R.color.negative;
                }
                binding.tvStatBestMonth.setTextColor(ContextCompat.getColor(requireContext(), bestMonthColor));
                
                double mean = sum / count;
                double temp = 0;
                for (double v : values) temp += (v - mean) * (v - mean);
                double sd = Math.sqrt(temp / count);
                double volatility = (mean != 0) ? (sd / mean) : 0;
                
                String volText = volatility < 0.05 ? "Low" : (volatility < 0.15 ? "Moderate" : "High");
                binding.tvStatVolatility.setText(volText);
                binding.tvStatVolatility.setTextColor(ContextCompat.getColor(requireContext(), volatility < 0.05 ? R.color.positive : (volatility < 0.15 ? R.color.amber : R.color.negative)));

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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, RANGE_OPTIONS);
        binding.rangeDropdown.setAdapter(adapter);
        binding.rangeDropdown.setText(RANGE_OPTIONS[0], false);
        binding.rangeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selection = RANGE_OPTIONS[position];
            customStartMonth = null;
            customEndMonth = null;
            switch (selection) {
                case "1Y": currentMonthsToView = 12; break;
                case "2Y": currentMonthsToView = 24; break;
                case "3Y": currentMonthsToView = 36; break;
                case "5Y": currentMonthsToView = 60; break;
                case "All": currentMonthsToView = -1; break;
                case "Custom": 
                    showCustomRangeDialog();
                    return;
            }
            updateUI();
        });
    }

    private void showCustomRangeDialog() {
        DialogCustomRangeBinding dialogBinding = DialogCustomRangeBinding.inflate(getLayoutInflater());
        
        Month first = new Month().getFirst();
        Month last = new Month().getLast();
        
        String[] months = new java.text.DateFormatSymbols().getShortMonths();
        
        setupMonthPicker(dialogBinding.startMonth, months);
        setupYearPicker(dialogBinding.startYear, first.getYear(), last.getYear());
        setupMonthPicker(dialogBinding.endMonth, months);
        setupYearPicker(dialogBinding.endYear, first.getYear(), last.getYear());

        // Default values
        dialogBinding.startMonth.setValue(first.getMonth());
        dialogBinding.startYear.setValue(first.getYear());
        dialogBinding.endMonth.setValue(last.getMonth());
        dialogBinding.endYear.setValue(last.getYear());

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Select Range")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Apply", (d, which) -> {
                    customStartMonth = new Month(dialogBinding.startMonth.getValue(), dialogBinding.startYear.getValue());
                    customEndMonth = new Month(dialogBinding.endMonth.getValue(), dialogBinding.endYear.getValue());
                    updateUI();
                })
                .setNegativeButton("Cancel", null)
                .show();
        Tools.styleDialog(dialog);
    }

    private void setupMonthPicker(NumberPicker picker, String[] months) {
        picker.setMinValue(1);
        picker.setMaxValue(12);
        picker.setDisplayedValues(months);
    }

    private void setupYearPicker(NumberPicker picker, int minYear, int maxYear) {
        picker.setMinValue(minYear);
        picker.setMaxValue(maxYear);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
