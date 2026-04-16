package com.kanzar.networthtracker;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.kanzar.networthtracker.views.SelectionHighlightRenderer;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_trend);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        LineChart chart = findViewById(R.id.trendChart);
        setupChart(chart);
        loadData(chart);
    }

    private void setupChart(LineChart chart) {
        int labelColor = Color.argb(180, 255, 255, 255);
        int gridColor  = Color.argb(30, 255, 255, 255);

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

        // X axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(labelColor);
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1.0f);
        xAxis.setAvoidFirstLastClipping(true);

        // Y axis left
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
                chart.getViewPortHandler(), ContextCompat.getColor(this, R.color.background)));
    }

    private void loadData(LineChart chart) {
        Realm realm = Realm.getDefaultInstance();

        // Collect all months from first to last (including future months if data exists)
        List<Month> months = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        Month first = new Month().getFirst();
        Month last = new Month().getLast();
        Month current = first;
        
        while (current != null) {
            months.add(current);
            labels.add(current.toStringMMYY());
            if (current.getMonth() == last.getMonth() && current.getYear() == last.getYear()) break;
            Month next = new Month(current.getMonth(), current.getYear());
            next.next();
            current = next;
        }

        if (months.isEmpty()) return;

        // Map: assetName -> (monthIndex -> value), only recorded months
        Map<String, Map<Integer, Float>> assetValues = new LinkedHashMap<>();

        for (int i = 0; i < months.size(); i++) {
            Month m = months.get(i);
            RealmResults<Asset> assets = realm.where(Asset.class)
                    .equalTo(AssetFields.MONTH, m.getMonth())
                    .equalTo(AssetFields.YEAR, m.getYear())
                    .findAll();
            for (Asset asset : assets) {
                String name = asset.getName();
                if (!assetValues.containsKey(name)) {
                    assetValues.put(name, new TreeMap<>());
                }
                Map<Integer, Float> monthMap = assetValues.get(name);
                if (monthMap != null) monthMap.put(i, (float) asset.getValue());
            }
        }

        // Keep only top 10 assets by highest absolute value across all months
        List<String> assetNames = new ArrayList<>(assetValues.keySet());
        assetNames.sort((a, b) -> {
            float maxA = 0, maxB = 0;
            for (float v : assetValues.get(a).values()) maxA = Math.max(maxA, Math.abs(v));
            for (float v : assetValues.get(b).values()) maxB = Math.max(maxB, Math.abs(v));
            return Float.compare(maxB, maxA);
        });
        if (assetNames.size() > 10) assetNames = assetNames.subList(0, 10);

        // Build datasets — entries only for months with actual data
        List<ILineDataSet> datasets = new ArrayList<>();
        int colorIndex = 0;

        for (String name : assetNames) {
            Map<Integer, Float> indexedVals = assetValues.get(name);
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
            ds.setDrawCircles(false);         // no dots on line
            ds.setDrawValues(false);          // no value labels
            ds.setMode(LineDataSet.Mode.LINEAR);
            ds.setHighLightColor(Color.argb(180, 255, 255, 255));
            ds.setDrawHorizontalHighlightIndicator(false);
            ds.setDrawHighlightIndicators(true);
            ds.setHighlightLineWidth(1.5f);

            datasets.add(ds);
        }

        if (datasets.isEmpty()) return;

        // X axis labels
        int labelCount = Math.min(labels.size(), 8);
        chart.getXAxis().setLabelCount(labelCount, false);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        // Marker
        TrendMarkerView marker = new TrendMarkerView(this, months, assetNames);
        marker.setChartView(chart);
        chart.setMarker(marker);

        LineData lineData = new LineData(datasets);
        chart.setData(lineData);
        chart.animateY(900);
    }
}
