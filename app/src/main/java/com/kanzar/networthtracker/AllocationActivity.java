package com.kanzar.networthtracker;

import android.graphics.Color;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.models.AssetFields;
import com.kanzar.networthtracker.views.TreemapView;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import io.realm.Realm;
import io.realm.Sort;

public class AllocationActivity extends AppCompatActivity {

    private static final int[] ASSET_COLORS = {
        0xFF42A5F5, 0xFF26C6DA, 0xFF66BB6A, 0xFFAB47BC,
        0xFF29B6F6, 0xFF26A69A, 0xFF9CCC65, 0xFF7E57C2,
        0xFF5C6BC0, 0xFF00BCD4, 0xFF43A047, 0xFF8D6E63
    };

    private static final int[] LIABILITY_COLORS = {
        0xFFEF5350, 0xFFFF7043, 0xFFFFCA28, 0xFFEC407A,
        0xFFFF5252, 0xFFFF6D00, 0xFFFFD600, 0xFFD81B60,
        0xFFE53935, 0xFFF4511E, 0xFFFFAB00, 0xFFC62828
    };


    private Month month;
    private PieChart assetChart;
    private PieChart liabilityChart;
    private TreemapView assetTreemap;
    private TreemapView liabilityTreemap;
    private TextView assetTotalView;
    private TextView liabilityTotalView;
    private TextView monthName;

    private boolean isTreemapMode = false;
    private MenuItem toggleMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allocation);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        int selectedMonth = getIntent().getIntExtra("month", new Month().getMonth());
        int selectedYear  = getIntent().getIntExtra("year",  new Month().getYear());
        month = new Month(selectedMonth, selectedYear);

        assetChart        = findViewById(R.id.assetChart);
        liabilityChart    = findViewById(R.id.liabilityChart);
        assetTreemap      = findViewById(R.id.assetTreemap);
        liabilityTreemap  = findViewById(R.id.liabilityTreemap);
        assetTotalView    = findViewById(R.id.assetTotal);
        liabilityTotalView = findViewById(R.id.liabilityTotal);
        monthName         = findViewById(R.id.monthName);

        findViewById(R.id.previousMonth).setOnClickListener(v -> { month.previous(); loadData(); });
        findViewById(R.id.nextMonth).setOnClickListener(v -> { month.next(); loadData(); });
        monthName.setOnClickListener(v -> showMonthYearPicker());

        loadData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_allocation, menu);
        toggleMenuItem = menu.findItem(R.id.action_toggle_chart);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_chart) {
            isTreemapMode = !isTreemapMode;
            applyChartMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applyChartMode() {
        if (toggleMenuItem != null) {
            toggleMenuItem.setIcon(isTreemapMode
                    ? R.drawable.ic_donut
                    : R.drawable.ic_treemap);
        }
        if (isTreemapMode) {
            assetChart.setVisibility(View.GONE);
            liabilityChart.setVisibility(View.GONE);
            assetTreemap.setVisibility(View.VISIBLE);
            liabilityTreemap.setVisibility(View.VISIBLE);
        } else {
            assetChart.setVisibility(View.VISIBLE);
            liabilityChart.setVisibility(View.VISIBLE);
            assetTreemap.setVisibility(View.GONE);
            liabilityTreemap.setVisibility(View.GONE);
        }
    }

    private void loadData() {
        monthName.setText(month.toString());

        // Previous month for change calculation
        Month prevMonth = new Month(month.getMonth(), month.getYear());
        prevMonth.previous();

        try (Realm realm = Realm.getDefaultInstance()) {
            List<Asset> all = realm
                    .where(Asset.class)
                    .equalTo(AssetFields.MONTH, month.getMonth())
                    .equalTo(AssetFields.YEAR, month.getYear())
                    .sort(AssetFields.VALUE, Sort.DESCENDING)
                    .findAll();

            // Build previous month lookup: name → raw value
            java.util.Map<String, Double> prevValues = new java.util.HashMap<>();
            for (Asset a : realm.where(Asset.class)
                    .equalTo(AssetFields.MONTH, prevMonth.getMonth())
                    .equalTo(AssetFields.YEAR, prevMonth.getYear())
                    .findAll()) {
                prevValues.put(a.getName(), a.getValue());
            }

            List<PieEntry> assetPieEntries     = new ArrayList<>();
            List<PieEntry> liabilityPieEntries = new ArrayList<>();
            List<Asset>    assetList           = new ArrayList<>();
            List<Asset>    liabilityList       = new ArrayList<>();
            double assetTotal = 0, liabilityTotal = 0;

            for (Asset asset : all) {
                double val = asset.getValue();
                if (val > 0) {
                    assetPieEntries.add(new PieEntry((float) val, asset.getName()));
                    assetList.add(realm.copyFromRealm(asset));
                    assetTotal += val;
                } else if (val < 0) {
                    liabilityPieEntries.add(new PieEntry((float) Math.abs(val), asset.getName()));
                    liabilityList.add(realm.copyFromRealm(asset));
                    liabilityTotal += Math.abs(val);
                }
            }

            // Assets
            if (assetPieEntries.isEmpty()) {
                assetChart.setVisibility(View.INVISIBLE);
                assetTreemap.setVisibility(View.INVISIBLE);
                assetTotalView.setText("—");
            } else {
                assetTotalView.setText(Tools.formatAmount(assetTotal, true));
                setupDonut(assetChart, assetPieEntries, ASSET_COLORS,
                        Tools.formatAmount(assetTotal, true));
                assetTreemap.setItems(buildTreemapItems(assetList, prevValues, true));
                if (isTreemapMode) {
                    assetChart.setVisibility(View.GONE);
                    assetTreemap.setVisibility(View.VISIBLE);
                } else {
                    assetChart.setVisibility(View.VISIBLE);
                    assetTreemap.setVisibility(View.GONE);
                }
            }

            // Liabilities
            if (liabilityPieEntries.isEmpty()) {
                liabilityChart.setVisibility(View.INVISIBLE);
                liabilityTreemap.setVisibility(View.INVISIBLE);
                liabilityTotalView.setText("—");
            } else {
                liabilityTotalView.setText("-" + Tools.formatAmount(liabilityTotal, true));
                setupDonut(liabilityChart, liabilityPieEntries, LIABILITY_COLORS,
                        "-" + Tools.formatAmount(liabilityTotal, true));
                liabilityTreemap.setItems(buildTreemapItems(liabilityList, prevValues, false));
                if (isTreemapMode) {
                    liabilityChart.setVisibility(View.GONE);
                    liabilityTreemap.setVisibility(View.VISIBLE);
                } else {
                    liabilityChart.setVisibility(View.VISIBLE);
                    liabilityTreemap.setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * Box size  = absolute value (weight/allocation).
     * Box color = % change vs previous month:
     *   green = gained value (asset up, liability down)
     *   red   = lost value   (asset down, liability up)
     *   intensity = magnitude of % change, capped at ±15%
     */
    private List<TreemapView.Item> buildTreemapItems(
            List<Asset> assets,
            java.util.Map<String, Double> prevValues,
            boolean isAsset) {

        List<TreemapView.Item> items = new ArrayList<>();
        for (Asset a : assets) {
            double current  = a.getValue();          // raw (negative for liabilities)
            double prev     = prevValues.containsKey(a.getName())
                    ? prevValues.get(a.getName()) : 0.0;

            // % change on absolute value
            double absCurrent = Math.abs(current);
            double absPrev    = Math.abs(prev);
            double pctChange;
            if (absPrev > 0) {
                pctChange = (absCurrent - absPrev) / absPrev * 100.0;
            } else {
                pctChange = absCurrent > 0 ? 15.0 : 0.0; // brand-new entry → treat as +15%
            }

            // For liabilities: debt growing is BAD (red), shrinking is GOOD (green) → invert
            double effectivePct = isAsset ? pctChange : -pctChange;

            int color = changeToColor(effectivePct);
            items.add(new TreemapView.Item(a.getName(), (float) absCurrent, color));
        }
        return items;
    }

    /**
     * Maps % change to a color, stock-market heatmap style.
     *
     *  pct > 0  → green family:  small gain = #4CAF50, large gain = #1B5E20
     *  pct < 0  → red family:    small loss = #EF5350, large loss = #7F0000
     *  pct == 0 → dark grey      #424242
     *
     * Intensity capped at ±15% for full saturation.
     */
    private int changeToColor(double pct) {
        if (pct == 0.0) return 0xFF424242;
        float t = (float) Math.min(Math.abs(pct) / 15.0, 1.0);
        if (pct > 0) {
            // light green → deep green
            return TreemapView.lerpColor(0xFF4CAF50, 0xFF1B5E20, t);
        } else {
            // light red → deep red
            return TreemapView.lerpColor(0xFFEF5350, 0xFF7F0000, t);
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

        new AlertDialog.Builder(this)
                .setTitle("Go to month")
                .setView(container)
                .setPositiveButton("Go", (dialog, which) -> {
                    month.setYear(yearPicker.getValue());
                    month.setMonth(monthPicker.getValue() + 1);
                    loadData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupDonut(PieChart chart, List<PieEntry> entries, int[] colors, String centerText) {
        int labelColor = ContextCompat.getColor(this, R.color.textPrimary);
        int secondaryLabelColor = ContextCompat.getColor(this, R.color.textSecondary);

        chart.setUsePercentValues(true);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setHoleRadius(56f);
        chart.setTransparentCircleRadius(61f);
        chart.setTransparentCircleColor(Color.argb(30, 255, 255, 255));
        chart.setDrawCenterText(true);
        chart.setCenterText(centerText);
        chart.setCenterTextColor(labelColor);
        chart.setCenterTextSize(13f);
        chart.setCenterTextTypeface(Typeface.DEFAULT_BOLD);
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setDrawEntryLabels(false);
        chart.setExtraOffsets(0f, 0f, 0f, 0f);

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);

        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(secondaryLabelColor);
        legend.setTextSize(11f);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setFormSize(10f);
        legend.setXEntrySpace(12f);
        legend.setYEntrySpace(4f);
        legend.setWordWrapEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        List<Integer> colorList = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) colorList.add(colors[i % colors.length]);

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colorList);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(6f);
        dataSet.setValueLinePart1OffsetPercentage(80f);
        dataSet.setValueLinePart1Length(0.4f);
        dataSet.setValueLinePart2Length(0.3f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLineColor(secondaryLabelColor);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(chart));
        pieData.setValueTextSize(10f);
        pieData.setValueTextColor(labelColor);
        pieData.setValueTypeface(Typeface.DEFAULT_BOLD);

        chart.setData(pieData);
        chart.animateY(700, Easing.EaseInOutQuad);
        chart.invalidate();
    }
}
