package com.kanzar.networthtracker;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.kanzar.networthtracker.views.SelectionHighlightRenderer;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.models.AssetFields;
import com.kanzar.networthtracker.views.TrendMarkerView;

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

    private LineChart chart;
    private List<Month> months;
    private List<String> allAssetNames = new ArrayList<>();
    private Map<String, Map<Integer, Float>> rawAssetValues = new LinkedHashMap<>();
    private Set<String> selectedAssets = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_trend);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        chart = findViewById(R.id.trendChart);
        setupChart(chart);
        fetchData();
        rebuildChart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_trend, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_select_assets) {
            showSelectionDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSelectionDialog() {
        if (allAssetNames.isEmpty()) return;

        // Master checked state indexed by position in allAssetNames
        boolean[] checked = new boolean[allAssetNames.size()];
        for (int i = 0; i < allAssetNames.size(); i++) {
            checked[i] = selectedAssets.contains(allAssetNames.get(i));
        }

        // filteredIndices maps ListView row → index in allAssetNames
        List<Integer> filteredIndices = new ArrayList<>();
        for (int i = 0; i < allAssetNames.size(); i++) filteredIndices.add(i);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_asset_select, null);
        EditText search   = dialogView.findViewById(R.id.searchAsset);
        ListView listView = dialogView.findViewById(R.id.assetList);
        Button btnSelectAll = dialogView.findViewById(R.id.btnSelectAll);
        Button btnClearAll  = dialogView.findViewById(R.id.btnClearAll);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_multiple_choice);
        for (int idx : filteredIndices) adapter.add(allAssetNames.get(idx));
        listView.setAdapter(adapter);

        // Restore checked state into ListView
        for (int row = 0; row < filteredIndices.size(); row++) {
            listView.setItemChecked(row, checked[filteredIndices.get(row)]);
        }

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < filteredIndices.size()) {
                int masterIdx = filteredIndices.get(position);
                checked[masterIdx] = listView.isItemChecked(position);
            }
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                filteredIndices.clear();
                for (int i = 0; i < allAssetNames.size(); i++) {
                    if (query.isEmpty() || allAssetNames.get(i).toLowerCase().contains(query)) {
                        filteredIndices.add(i);
                    }
                }
                adapter.clear();
                for (int idx : filteredIndices) adapter.add(allAssetNames.get(idx));
                adapter.notifyDataSetChanged();
                for (int row = 0; row < filteredIndices.size(); row++) {
                    listView.setItemChecked(row, checked[filteredIndices.get(row)]);
                }
            }
        });

        btnSelectAll.setOnClickListener(v -> {
            for (int row = 0; row < filteredIndices.size(); row++) {
                checked[filteredIndices.get(row)] = true;
                listView.setItemChecked(row, true);
            }
        });

        btnClearAll.setOnClickListener(v -> {
            for (int row = 0; row < filteredIndices.size(); row++) {
                checked[filteredIndices.get(row)] = false;
                listView.setItemChecked(row, false);
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.trend_select_title)
                .setView(dialogView)
                .setPositiveButton(R.string.trend_show, (dialog, which) -> {
                    Set<String> newSelection = new HashSet<>();
                    for (int i = 0; i < allAssetNames.size(); i++) {
                        if (checked[i]) newSelection.add(allAssetNames.get(i));
                    }
                    if (newSelection.isEmpty()) {
                        Toast.makeText(this, R.string.trend_select_none, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    selectedAssets = newSelection;
                    rebuildChart();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setupChart(LineChart chart) {
        int labelColor = ContextCompat.getColor(this, R.color.textSecondary);
        int gridColor  = ContextCompat.getColor(this, R.color.divider);

        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(true);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setDrawGridBackground(false);
        chart.setHighlightPerDragEnabled(false);

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);

        chart.setExtraOffsets(8f, 16f, 8f, 8f);

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
                if (Math.abs(value) >= 1_000_000) {
                    return Tools.formatAmount(value / 1_000_000.0) + "M";
                } else if (Math.abs(value) >= 1_000) {
                    return Math.round(value / 1_000.0) + "k";
                }
                return Tools.formatAmount(value);
            }
        });

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);

        chart.setRenderer(new SelectionHighlightRenderer(chart, chart.getAnimator(),
                chart.getViewPortHandler(), ContextCompat.getColor(this, R.color.backgroundContent)));
    }

    private void fetchData() {
        months = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        rawAssetValues = new LinkedHashMap<>();

        try (Realm realm = Realm.getDefaultInstance()) {
            Month first = new Month().getFirst();
            Month last  = new Month().getLast();
            Month current = first;

            while (current != null) {
                months.add(current);
                xLabels.add(current.toStringMMYY());
                if (current.getMonth() == last.getMonth() && current.getYear() == last.getYear()) break;
                Month next = new Month(current.getMonth(), current.getYear());
                next.next();
                current = next;
            }

            if (months.isEmpty()) return;

            Set<String> nameSet = new HashSet<>();
            for (int i = 0; i < months.size(); i++) {
                Month m = months.get(i);
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

            // Pre-select top 10 by peak absolute value
            List<String> byValue = new ArrayList<>(allAssetNames);
            byValue.sort((a, b) -> {
                float maxA = 0, maxB = 0;
                for (float v : rawAssetValues.get(a).values()) maxA = Math.max(maxA, Math.abs(v));
                for (float v : rawAssetValues.get(b).values()) maxB = Math.max(maxB, Math.abs(v));
                return Float.compare(maxB, maxA);
            });
            selectedAssets = new HashSet<>(byValue.subList(0, Math.min(10, byValue.size())));

            int labelCount = Math.min(xLabels.size(), 8);
            chart.getXAxis().setLabelCount(labelCount, false);
            chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        }
    }

    private void rebuildChart() {
        if (months == null || months.isEmpty() || selectedAssets.isEmpty()) return;

        List<String> displayNames = new ArrayList<>();
        for (String name : allAssetNames) {
            if (selectedAssets.contains(name)) displayNames.add(name);
        }

        List<ILineDataSet> datasets = new ArrayList<>();
        int colorIndex = 0;

        for (String name : displayNames) {
            Map<Integer, Float> indexedVals = rawAssetValues.get(name);
            if (indexedVals == null || indexedVals.isEmpty()) continue;

            List<Entry> entries = new ArrayList<>();
            for (Map.Entry<Integer, Float> e : indexedVals.entrySet()) {
                entries.add(new Entry(e.getKey(), e.getValue()));
            }

            int color = LINE_COLORS[colorIndex % LINE_COLORS.length];
            colorIndex++;

            LineDataSet ds = new LineDataSet(entries, name);
            ds.setColor(color);
            ds.setLineWidth(2f);
            ds.setDrawCircles(false);
            ds.setDrawValues(false);
            ds.setMode(LineDataSet.Mode.LINEAR);
            ds.setHighLightColor(ContextCompat.getColor(this, R.color.colorAccent));
            ds.setDrawHorizontalHighlightIndicator(false);
            ds.setDrawHighlightIndicators(true);
            ds.setHighlightLineWidth(1.5f);

            datasets.add(ds);
        }

        if (datasets.isEmpty()) return;

        TrendMarkerView marker = new TrendMarkerView(this, months, displayNames);
        marker.setChartView(chart);
        chart.setMarker(marker);

        chart.clear();
        chart.setData(new LineData(datasets));
        chart.animateY(900);
    }
}
