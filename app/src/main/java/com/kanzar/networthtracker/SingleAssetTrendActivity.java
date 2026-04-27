package com.kanzar.networthtracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.NumberPicker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.kanzar.networthtracker.databinding.ActivitySingleAssetTrendBinding;
import com.kanzar.networthtracker.databinding.DialogCustomRangeBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.models.AssetFields;
import com.kanzar.networthtracker.views.SelectionHighlightRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;

public class SingleAssetTrendActivity extends AppCompatActivity {

    public static final String EXTRA_ASSET_NAME = "asset_name";

    private ActivitySingleAssetTrendBinding binding;
    private String assetName;
    private final List<Month> allMonths = new ArrayList<>();
    private final List<Double> assetValues = new ArrayList<>();
    private int selectedRangeMonths = 12; // Default to 1Y
    private String lastSelectedRangeLabel = "1Y";
    private Month customStartMonth = null;
    private Month customEndMonth = null;

    private static final String[] RANGE_OPTIONS = {"1Y", "2Y", "3Y", "5Y", "All", "Custom"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySingleAssetTrendBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        assetName = getIntent().getStringExtra(EXTRA_ASSET_NAME);
        if (assetName == null) {
            finish();
            return;
        }

        binding.toolbar.setTitle(assetName);
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        setupChart(binding.trendChart);
        setupRangeSelector();
        fetchData();
        updateUI();
    }

    private void setupRangeSelector() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown, RANGE_OPTIONS);
        binding.rangeDropdown.setAdapter(adapter);
        binding.rangeDropdown.setText(RANGE_OPTIONS[0], false);
        binding.rangeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selection = RANGE_OPTIONS[position];
            if (selection.equals("Custom")) {
                showCustomRangeDialog();
                return;
            }

            lastSelectedRangeLabel = selection;
            customStartMonth = null;
            customEndMonth = null;
            switch (selection) {
                case "1Y": selectedRangeMonths = 12; break;
                case "2Y": selectedRangeMonths = 24; break;
                case "3Y": selectedRangeMonths = 36; break;
                case "5Y": selectedRangeMonths = 60; break;
                case "All": selectedRangeMonths = -1; break;
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

        dialogBinding.startMonth.setValue(first.getMonth());
        dialogBinding.startYear.setValue(first.getYear());
        dialogBinding.endMonth.setValue(last.getMonth());
        dialogBinding.endYear.setValue(last.getYear());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select Range")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Apply", (d, which) -> {
                    lastSelectedRangeLabel = "Custom";
                    customStartMonth = new Month(dialogBinding.startMonth.getValue(), dialogBinding.startYear.getValue());
                    customEndMonth = new Month(dialogBinding.endMonth.getValue(), dialogBinding.endYear.getValue());
                    updateUI();
                })
                .setNegativeButton("Cancel", (d, which) -> {
                    binding.rangeDropdown.setText(lastSelectedRangeLabel, false);
                })
                .setOnCancelListener(d -> {
                    binding.rangeDropdown.setText(lastSelectedRangeLabel, false);
                })
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

    private void setupChart(LineChart chart) {
        int labelColor = ContextCompat.getColor(this, R.color.text_4);
        int gridColor  = ContextCompat.getColor(this, R.color.divider);

        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setDrawGridBackground(false);
        chart.setHighlightPerDragEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        chart.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);

        chart.setExtraOffsets(8f, 16f, 8f, 12f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(labelColor);
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1.0f);
        xAxis.setAvoidFirstLastClipping(true);

        YAxis axisLeft = chart.getAxisLeft();
        axisLeft.setDrawAxisLine(false);
        axisLeft.setTextColor(labelColor);
        axisLeft.setTextSize(10f);
        axisLeft.setGridColor(gridColor);
        axisLeft.setGridLineWidth(1f);
        axisLeft.setLabelCount(5, false);
        axisLeft.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (Prefs.getBoolean("privacy_mode", false)) return "****";
                return Tools.formatCompact(value);
            }
        });

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);

        chart.setRenderer(new SelectionHighlightRenderer(chart, chart.getAnimator(),
                chart.getViewPortHandler(), ContextCompat.getColor(this, R.color.bg_elev)));

        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                showCursorCard(e);
            }

            @Override
            public void onNothingSelected() {
                binding.cursorCard.setVisibility(View.GONE);
            }
        });
    }

    private void showCursorCard(Entry e) {
        int index = (int) e.getX();
        int dataSize = assetValues.size();
        int startIndex, endIndex;
        if (customStartMonth != null && customEndMonth != null) {
            startIndex = getMonthIndex(customStartMonth);
            endIndex = getMonthIndex(customEndMonth);
            if (startIndex == -1) startIndex = 0;
            if (endIndex == -1) endIndex = dataSize - 1;
        } else {
            startIndex = selectedRangeMonths <= 0 ? 0 : Math.max(0, dataSize - selectedRangeMonths);
            endIndex = dataSize - 1;
        }
        int actualIndex = startIndex + index;

        if (actualIndex >= 0 && actualIndex < allMonths.size()) {
            Month m = allMonths.get(actualIndex);
            binding.cursorDate.setText(m.toStringMMMYY().toUpperCase(java.util.Locale.US));
            binding.cursorValue.setText(Prefs.getBoolean("privacy_mode", false) ? "****" : Tools.formatAmount(e.getY()));

            double change = 0;
            double percent = 0;
            try (Realm realm = Realm.getDefaultInstance()) {
                Asset asset = realm.where(Asset.class)
                        .equalTo(AssetFields.MONTH, m.getMonth())
                        .equalTo(AssetFields.YEAR, m.getYear())
                        .equalTo(AssetFields.NAME, assetName)
                        .findFirst();
                
                if (asset != null) {
                    Month prev = m.getPreviousMonth(realm);
                    Asset prevAsset = realm.where(Asset.class)
                            .equalTo(AssetFields.MONTH, prev.getMonth())
                            .equalTo(AssetFields.YEAR, prev.getYear())
                            .equalTo(AssetFields.NAME, assetName)
                            .findFirst();
                    
                    if (prevAsset != null) {
                        change = asset.getValue() - prevAsset.getValue();
                        percent = Tools.getPercent(prevAsset.getValue(), asset.getValue());
                    }
                }
            }

            boolean privacyMode = Prefs.getBoolean("privacy_mode", false);
            String changeSign = change >= 0 ? "+" : "";
            String changeStr = privacyMode ? "****" : Tools.formatAmount(change);
            String pctStr = privacyMode ? "**%" : String.format(java.util.Locale.US, "%.1f%%", Math.abs(percent));
            
            binding.cursorChange.setText(String.format("%s%s (%s)", changeSign, changeStr, pctStr));
            binding.cursorChange.setTextColor(ContextCompat.getColor(this, Tools.getTextChangeColor(change)));

            int color = ContextCompat.getColor(this, Tools.getAccentColor());
            binding.cursorColor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));

            binding.cursorCard.setVisibility(View.VISIBLE);
        }
    }

    private void fetchData() {
        allMonths.clear();
        assetValues.clear();

        try (Realm realm = Realm.getDefaultInstance()) {
            Month first = new Month().getFirst();
            Month last  = new Month().getLast();
            Month current = first;

            while (current != null) {
                allMonths.add(current);
                
                Asset asset = realm.where(Asset.class)
                        .equalTo(AssetFields.MONTH, current.getMonth())
                        .equalTo(AssetFields.YEAR, current.getYear())
                        .equalTo(AssetFields.NAME, assetName)
                        .findFirst();
                
                assetValues.add(asset != null ? asset.getValue() : 0.0);

                if (current.getMonth() == last.getMonth() && current.getYear() == last.getYear()) break;
                Month next = new Month(current.getMonth(), current.getYear());
                next.next();
                current = next;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        if (assetValues.isEmpty()) return;

        int dataSize = assetValues.size();
        int startIndex, endIndex;
        if (customStartMonth != null && customEndMonth != null) {
            startIndex = getMonthIndex(customStartMonth);
            endIndex = getMonthIndex(customEndMonth);
            if (startIndex == -1) startIndex = 0;
            if (endIndex == -1) endIndex = dataSize - 1;
        } else {
            startIndex = selectedRangeMonths <= 0 ? 0 : Math.max(0, dataSize - selectedRangeMonths);
            endIndex = dataSize - 1;
        }

        double currentVal = assetValues.get(endIndex);
        binding.currentValueText.setText(Prefs.getBoolean("privacy_mode", false) ? "****" : Tools.formatAmount(currentVal));

        // Calculate change for the selected period
        double startVal = assetValues.get(startIndex);
        double diff = currentVal - startVal;
        double pct = Tools.getPercent(startVal, currentVal);

        String changeSign = diff >= 0 ? "+" : "";
        String formattedDiff = Tools.formatAmount(diff);
        String pctStr = String.format(java.util.Locale.US, "%.1f%%", pct);
        String periodLabel = (endIndex - startIndex + 1) + " mo";
        
        String changeText = changeSign + formattedDiff + " (" + pctStr + ") · " + periodLabel;
        binding.currentChangeText.setText(changeText);
        binding.currentChangeText.setTextColor(ContextCompat.getColor(this, diff >= 0 ? R.color.positive : R.color.negative));

        updateChart(startIndex, endIndex);
        updateStats(startIndex, endIndex);
    }

    private void updateChart(int startIndex, int endIndex) {
        if (assetValues.isEmpty()) return;
        
        List<Entry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        
        for (int i = startIndex; i <= endIndex; i++) {
            entries.add(new Entry(i - startIndex, assetValues.get(i).floatValue()));
            xLabels.add(allMonths.get(i).toStringMMMYY());
        }

        LineDataSet ds = new LineDataSet(entries, assetName);
        int color = ContextCompat.getColor(this, Tools.getAccentColor());
        ds.setColor(color);
        ds.setLineWidth(2.5f);
        ds.setDrawCircles(false);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ds.setDrawFilled(true);
        
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ColorUtils.setAlphaComponent(color, 80), ColorUtils.setAlphaComponent(color, 0)});
        ds.setFillDrawable(gd);
        
        ds.setHighLightColor(color);
        ds.setDrawHorizontalHighlightIndicator(false);
        ds.setDrawHighlightIndicators(true);
        ds.setHighlightLineWidth(1.5f);
        ds.enableDashedHighlightLine(10f, 10f, 0f);

        binding.trendChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        binding.trendChart.getXAxis().setLabelCount(Math.min(entries.size(), 5), false);
        
        binding.trendChart.setData(new LineData(ds));
        binding.trendChart.animateY(800);
        binding.trendChart.invalidate();

        if (!entries.isEmpty()) {
            Highlight lastHighlight = new Highlight(entries.get(entries.size() - 1).getX(), entries.get(entries.size() - 1).getY(), 0);
            binding.trendChart.highlightValue(lastHighlight, true);
        }
    }

    private void updateStats(int startIndex, int endIndex) {
        if (assetValues.isEmpty()) return;
        
        List<Double> periodValues = new ArrayList<>();
        List<Double> pcts = new ArrayList<>();
        double sum = 0;

        try (Realm realm = Realm.getDefaultInstance()) {
            for (int i = startIndex; i <= endIndex; i++) {
                double val = assetValues.get(i);
                periodValues.add(val);
                sum += val;

                Month m = allMonths.get(i);
                Month prev = m.getPreviousMonth(realm);
                Asset prevAsset = realm.where(Asset.class)
                        .equalTo(AssetFields.MONTH, prev.getMonth())
                        .equalTo(AssetFields.YEAR, prev.getYear())
                        .equalTo(AssetFields.NAME, assetName)
                        .findFirst();

                if (prevAsset != null) {
                    pcts.add(Tools.getPercent(prevAsset.getValue(), val));
                }
            }
        }

        int count = periodValues.size();
        if (count > 0) {
            binding.tvStatAverage.setText(Tools.formatAmount(sum / count));
            
            double maxPct = pcts.isEmpty() ? 0 : Collections.max(pcts);
            String bestMonthSign = maxPct >= 0 ? "+" : "";
            String bestMonthStr = String.format(java.util.Locale.US, "%.2f%%", maxPct);
            binding.tvStatBestMonth.setText(String.format("%s%s", bestMonthSign, bestMonthStr));
            binding.tvStatBestMonth.setTextColor(ContextCompat.getColor(this, maxPct >= 0 ? R.color.positive : R.color.negative));
            
            double mean = sum / count;
            double temp = 0;
            for (double v : periodValues) temp += (v - mean) * (v - mean);
            double sd = Math.sqrt(temp / count);
            double volatility = (mean != 0) ? (sd / mean) : 0;
            
            String volText = volatility < 0.05 ? "Low" : (volatility < 0.15 ? "Moderate" : "High");
            binding.tvStatVolatility.setText(volText);
            binding.tvStatVolatility.setTextColor(ContextCompat.getColor(this, volatility < 0.05 ? R.color.positive : (volatility < 0.15 ? R.color.amber : R.color.negative)));

            double startVal = periodValues.get(0);
            double currentVal = periodValues.get(count - 1);
            if (count > 1 && startVal > 0 && currentVal > 0) {
                double cagrValue = (Math.pow(currentVal / startVal, 1.0 / (count / 12.0)) - 1) * 100;
                String cagrSign = cagrValue >= 0 ? "+" : "";
                String cagrStr = String.format(java.util.Locale.US, "%.1f%%", cagrValue);
                binding.tvStatCAGR.setText(String.format("%s%s", cagrSign, cagrStr));
            } else {
                binding.tvStatCAGR.setText("N/A");
            }
        }
    }

    private int getMonthIndex(Month m) {
        for (int i = 0; i < allMonths.size(); i++) {
            Month am = allMonths.get(i);
            if (am.getMonth() == m.getMonth() && am.getYear() == m.getYear()) return i;
        }
        return -1;
    }
}
