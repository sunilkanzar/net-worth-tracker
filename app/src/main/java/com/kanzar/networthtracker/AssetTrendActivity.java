package com.kanzar.networthtracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.kanzar.networthtracker.databinding.ActivityAssetTrendBinding;
import com.kanzar.networthtracker.databinding.ItemCursorAssetBinding;
import com.kanzar.networthtracker.databinding.ItemTrendLegendBinding;
import com.kanzar.networthtracker.databinding.LayoutTrendCursorCardBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.models.AssetFields;
import com.kanzar.networthtracker.views.SelectionHighlightRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.realm.Realm;
import io.realm.RealmResults;

public class AssetTrendActivity extends AppCompatActivity {

    private static final int[] LINE_COLORS = {
        0xFF42A5F5, 0xFFEF5350, 0xFF66BB6A, 0xFFFFCA28,
        0xFFAB47BC, 0xFF26C6DA, 0xFFFF7043, 0xFF9CCC65,
        0xFF5C6BC0, 0xFFEC407A, 0xFF29B6F6, 0xFF8D6E63,
        0xFF26A69A, 0xFFFFAB40, 0xFF7E57C2, 0xFF78909C
    };

    private ActivityAssetTrendBinding binding;
    private final List<Month> allMonths = new ArrayList<>();
    private List<String> allAssetNames = new ArrayList<>();
    private Map<String, Map<Integer, Float>> rawAssetValues = new LinkedHashMap<>();
    private final Set<String> selectedAssets = new HashSet<>();
    private int selectedRangeMonths = 24; // Default to 2Y as per layout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAssetTrendBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        setupChart(binding.trendChart);
        setupRangeSelector();
        binding.legendRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        fetchData();
        rebuildChart();
    }

    private void setupRangeSelector() {
        View.OnClickListener listener = v -> {
            int id = v.getId();
            if (id == R.id.btn6M) selectedRangeMonths = 6;
            else if (id == R.id.btn1Y) selectedRangeMonths = 12;
            else if (id == R.id.btn2Y) selectedRangeMonths = 24;
            else if (id == R.id.btnAll) selectedRangeMonths = 0;
            
            updateRangeSelectorUI();
            rebuildChart();
        };

        binding.btn6M.setOnClickListener(listener);
        binding.btn1Y.setOnClickListener(listener);
        binding.btn2Y.setOnClickListener(listener);
        binding.btnAll.setOnClickListener(listener);
        
        updateRangeSelectorUI();
    }

    private void updateRangeSelectorUI() {
        updateRangeButton(binding.btn6M, selectedRangeMonths == 6);
        updateRangeButton(binding.btn1Y, selectedRangeMonths == 12);
        updateRangeButton(binding.btn2Y, selectedRangeMonths == 24);
        updateRangeButton(binding.btnAll, selectedRangeMonths == 0);
    }

    private void updateRangeButton(TextView view, boolean selected) {
        view.setTextColor(ContextCompat.getColor(this, selected ? R.color.text : R.color.text_3));
        view.setBackgroundResource(selected ? R.drawable.bg_range_selected : 0);
        view.setElevation(selected ? 1f : 0f);
    }

    private void setupChart(LineChart chart) {
        int labelColor = ContextCompat.getColor(this, R.color.text_3);
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
        axisLeft.setLabelCount(6, false);
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
                binding.cursorCard.getRoot().setVisibility(View.GONE);
            }
        });
    }

    private void showCursorCard(Entry e) {
        int index = (int) e.getX();
        int dataSize = allMonths.size();
        int startIndex = selectedRangeMonths == 0 ? 0 : Math.max(0, dataSize - selectedRangeMonths);
        int actualIndex = startIndex + index;

        if (actualIndex >= 0 && actualIndex < allMonths.size()) {
            Month m = allMonths.get(actualIndex);
            LayoutTrendCursorCardBinding cursorBinding = LayoutTrendCursorCardBinding.bind(binding.cursorCard.getRoot());
            cursorBinding.cursorDate.setText(m.toStringMMMYY().toUpperCase(java.util.Locale.US));
            
            cursorBinding.cursorGrid.removeAllViews();
            
            int count = 0;
            // Iterate over selected assets and add up to 6 to the cursor card
            for (int i = 0; i < allAssetNames.size() && count < 6; i++) {
                String name = allAssetNames.get(i);
                if (selectedAssets.contains(name)) {
                    Map<Integer, Float> vals = rawAssetValues.get(name);
                    if (vals != null && vals.containsKey(actualIndex)) {
                        Float val = vals.get(actualIndex);
                        if (val != null) {
                            addCursorItem(cursorBinding.cursorGrid, name, val, LINE_COLORS[i % LINE_COLORS.length]);
                            count++;
                        }
                    }
                }
            }

            binding.cursorCard.getRoot().setVisibility(View.VISIBLE);
        }
    }

    private void addCursorItem(android.widget.GridLayout grid, String name, float value, int color) {
        ItemCursorAssetBinding itemBinding = ItemCursorAssetBinding.inflate(getLayoutInflater(), grid, false);
        itemBinding.cursorAssetColor.setBackgroundColor(color);
        itemBinding.cursorAssetName.setText(name);
        itemBinding.cursorAssetValue.setText(Prefs.getBoolean("privacy_mode", false) ? "****" : Tools.formatCompact(value));
        grid.addView(itemBinding.getRoot());
    }

    private void fetchData() {
        allMonths.clear();
        rawAssetValues = new LinkedHashMap<>();

        try (Realm realm = Realm.getDefaultInstance()) {
            Month first = new Month().getFirst();
            Month last  = new Month().getLast();
            Month current = first;

            while (current != null) {
                allMonths.add(current);
                if (current.getMonth() == last.getMonth() && current.getYear() == last.getYear()) break;
                Month next = new Month(current.getMonth(), current.getYear());
                next.next();
                current = next;
            }

            if (allMonths.isEmpty()) return;

            Set<String> nameSet = new HashSet<>();
            for (int i = 0; i < allMonths.size(); i++) {
                Month m = allMonths.get(i);
                RealmResults<Asset> assets = realm.where(Asset.class)
                        .equalTo(AssetFields.MONTH, m.getMonth())
                        .equalTo(AssetFields.YEAR, m.getYear())
                        .findAll();
                for (Asset asset : assets) {
                    String name = asset.getName();
                    nameSet.add(name);
                    rawAssetValues.computeIfAbsent(name, k -> new TreeMap<>())
                            .put(i, (float) asset.getValue());
                }
            }

            allAssetNames = new ArrayList<>(nameSet);
            Collections.sort(allAssetNames);

            // Selection logic: Top 10 assets by value from the latest month.
            // If less than 5 assets have data for the latest month, fallback to previous months.
            int targetIdx = allMonths.size() - 1;
            while (targetIdx > 0) {
                int count = 0;
                for (String name : allAssetNames) {
                    Map<Integer, Float> vals = rawAssetValues.get(name);
                    if (vals != null && vals.containsKey(targetIdx)) count++;
                }
                if (count >= 5) break;
                targetIdx--;
            }

            final int sortIdx = Math.max(0, targetIdx);
            List<String> byValue = new ArrayList<>(allAssetNames);
            byValue.sort((a, b) -> {
                Map<Integer, Float> valsA = rawAssetValues.get(a);
                Map<Integer, Float> valsB = rawAssetValues.get(b);
                
                Float fA = (valsA != null) ? valsA.get(sortIdx) : null;
                Float fB = (valsB != null) ? valsB.get(sortIdx) : null;
                
                float vA = fA != null ? Math.abs(fA) : 0f;
                float vB = fB != null ? Math.abs(fB) : 0f;

                return Float.compare(vB, vA);
            });

            selectedAssets.clear();
            selectedAssets.addAll(byValue.subList(0, Math.min(10, byValue.size())));
        }
    }

    private void rebuildChart() {
        if (allMonths.isEmpty() || selectedAssets.isEmpty()) return;

        int dataSize = allMonths.size();
        int startIndex = selectedRangeMonths == 0 ? 0 : Math.max(0, dataSize - selectedRangeMonths);

        List<ILineDataSet> datasets = new ArrayList<>();
        
        for (int i = 0; i < allAssetNames.size(); i++) {
            String name = allAssetNames.get(i);
            if (!selectedAssets.contains(name)) continue;

            Map<Integer, Float> indexedVals = rawAssetValues.get(name);
            if (indexedVals == null || indexedVals.isEmpty()) continue;

            List<Entry> entries = new ArrayList<>();
            for (int j = startIndex; j < dataSize; j++) {
                Float val = indexedVals.get(j);
                if (val != null) {
                    entries.add(new Entry(j - startIndex, val));
                }
            }

            if (entries.isEmpty()) continue;

            int color = LINE_COLORS[i % LINE_COLORS.length];

            LineDataSet ds = new LineDataSet(entries, name);
            ds.setColor(color);
            ds.setLineWidth(2f);
            ds.setDrawCircles(false);
            ds.setDrawValues(false);
            ds.setMode(LineDataSet.Mode.LINEAR);
            ds.setHighLightColor(color);
            ds.setDrawHorizontalHighlightIndicator(false);
            ds.setDrawHighlightIndicators(true);
            ds.setHighlightLineWidth(1.5f);

            datasets.add(ds);
        }

        if (datasets.isEmpty()) return;

        List<String> xLabels = new ArrayList<>();
        for (int i = startIndex; i < dataSize; i++) {
            xLabels.add(allMonths.get(i).toStringMMMYY());
        }

        binding.trendChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        binding.trendChart.getXAxis().setLabelCount(5, false);

        binding.trendChart.setData(new LineData(datasets));
        binding.trendChart.animateY(900);
        binding.trendChart.invalidate();

        updateLegend(startIndex);
        updateSummary();
    }

    private void updateSummary() {
        binding.summarySubtitle.setText(String.format(java.util.Locale.US, "PLOTTING %d ASSET%s", 
                selectedAssets.size(), selectedAssets.size() == 1 ? "" : "S"));
        String rangeText = selectedRangeMonths == 0 ? "All-time" : selectedRangeMonths + "-month";
        binding.summaryTitle.setText(String.format(java.util.Locale.US, "%s trends", rangeText));
    }

    private void updateLegend(int startIndex) {
        List<LegendItem> legendItems = new ArrayList<>();
        int dataSize = allMonths.size();

        for (int i = 0; i < allAssetNames.size(); i++) {
            String name = allAssetNames.get(i);
            boolean active = selectedAssets.contains(name);
            Map<Integer, Float> vals = rawAssetValues.get(name);
            
            float lastVal = 0;
            double pctChange = 0;

            if (vals != null && !vals.isEmpty()) {
                List<Integer> keys = new ArrayList<>(vals.keySet());
                int lastKey = keys.get(keys.size() - 1);
                lastVal = vals.get(lastKey);

                // Calculate percentage change over the visible range
                float firstValInRange = 0;
                boolean foundFirst = false;

                for (int j = startIndex; j < dataSize; j++) {
                    Float v = vals.get(j);
                    if (v != null) {
                        firstValInRange = v;
                        foundFirst = true;
                        // If the first value in range is the ONLY value (latest month), 
                        // try to find the value immediately preceding the range
                        if (j == lastKey && j > 0) {
                            Float vPrev = vals.get(j - 1);
                            if (vPrev != null) {
                                firstValInRange = vPrev;
                            } else {
                                firstValInRange = 0;
                            }
                        }
                        break;
                    }
                }

                if (foundFirst) {
                    pctChange = Tools.getPercent(firstValInRange, lastVal);
                }
            }
            legendItems.add(new LegendItem(name, lastVal, pctChange, LINE_COLORS[i % LINE_COLORS.length], active));
        }
        
        Collections.sort(legendItems, (a, b) -> {
            if (a.active != b.active) return a.active ? -1 : 1;
            return a.name.compareTo(b.name);
        });

        binding.legendRecyclerView.setAdapter(new LegendAdapter(legendItems));
    }

    private static class LegendItem {
        String name;
        float value;
        double pctChange;
        int color;
        boolean active;

        LegendItem(String name, float value, double pctChange, int color, boolean active) {
            this.name = name;
            this.value = value;
            this.pctChange = pctChange;
            this.color = color;
            this.active = active;
        }
    }

    private class LegendAdapter extends RecyclerView.Adapter<LegendAdapter.ViewHolder> {
        private final List<LegendItem> items;

        LegendAdapter(List<LegendItem> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemTrendLegendBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LegendItem item = items.get(position);
            holder.binding.legendName.setText(item.name);
            holder.binding.legendValue.setText(Prefs.getBoolean("privacy_mode", false) ? "****" : Tools.formatCompact(item.value));
            holder.binding.legendColor.setBackgroundColor(item.color);
            
            String changeText = (item.pctChange >= 0 ? "↑ " : "↓ ") + Tools.formatPercent(Math.abs(item.pctChange));
            holder.binding.legendChange.setText(changeText);
            holder.binding.legendChange.setTextColor(ContextCompat.getColor(AssetTrendActivity.this, 
                    item.pctChange >= 0 ? R.color.positive : R.color.negative));
            
            holder.binding.getRoot().setAlpha(item.active ? 1.0f : 0.5f);
            holder.binding.getRoot().setStrokeColor(item.active ? item.color : ContextCompat.getColor(AssetTrendActivity.this, R.color.divider));

            holder.binding.getRoot().setOnClickListener(v -> {
                if (selectedAssets.contains(item.name)) {
                    if (selectedAssets.size() > 1) selectedAssets.remove(item.name);
                    else Toast.makeText(AssetTrendActivity.this, R.string.trend_select_none, Toast.LENGTH_SHORT).show();
                } else {
                    selectedAssets.add(item.name);
                }
                rebuildChart();
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ItemTrendLegendBinding binding;
            ViewHolder(ItemTrendLegendBinding binding) { super(binding.getRoot()); this.binding = binding; }
        }
    }
}
