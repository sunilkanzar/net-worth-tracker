package com.kanzar.networthtracker;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // Full data for current month (unmanaged Realm copies)
    private List<Asset> allAssets      = new ArrayList<>();
    private List<Asset> allLiabilities = new ArrayList<>();
    private Map<String, Double> prevValues = new HashMap<>();

    // Active filter sets (names)
    private Set<String> selectedAssetNames     = new HashSet<>();
    private Set<String> selectedLiabilityNames = new HashSet<>();

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

        assetChart         = findViewById(R.id.assetChart);
        liabilityChart     = findViewById(R.id.liabilityChart);
        assetTreemap       = findViewById(R.id.assetTreemap);
        liabilityTreemap   = findViewById(R.id.liabilityTreemap);
        assetTotalView     = findViewById(R.id.assetTotal);
        liabilityTotalView = findViewById(R.id.liabilityTotal);
        monthName          = findViewById(R.id.monthName);

        findViewById(R.id.previousMonth).setOnClickListener(v -> { month.previous(); loadData(); });
        findViewById(R.id.nextMonth).setOnClickListener(v -> { month.next(); loadData(); });
        monthName.setOnClickListener(v -> showMonthYearPicker());

        loadData();
    }

    // ── Menu ─────────────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_allocation, menu);
        toggleMenuItem = menu.findItem(R.id.action_toggle_chart);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_toggle_chart) {
            isTreemapMode = !isTreemapMode;
            applyChartMode();
            return true;
        } else if (id == R.id.action_filter_allocation) {
            showFilterDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applyChartMode() {
        if (toggleMenuItem != null) {
            toggleMenuItem.setIcon(isTreemapMode ? R.drawable.ic_donut : R.drawable.ic_treemap);
        }
        boolean assetsEmpty      = selectedAssetNames.isEmpty()     || allAssets.isEmpty();
        boolean liabilitiesEmpty = selectedLiabilityNames.isEmpty() || allLiabilities.isEmpty();

        assetChart.setVisibility((!isTreemapMode && !assetsEmpty) ? View.VISIBLE : View.GONE);
        assetTreemap.setVisibility((isTreemapMode && !assetsEmpty) ? View.VISIBLE : View.GONE);
        liabilityChart.setVisibility((!isTreemapMode && !liabilitiesEmpty) ? View.VISIBLE : View.GONE);
        liabilityTreemap.setVisibility((isTreemapMode && !liabilitiesEmpty) ? View.VISIBLE : View.GONE);
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    private void loadData() {
        monthName.setText(month.toString());

        Month prevMonth = new Month(month.getMonth(), month.getYear());
        prevMonth.previous();

        try (Realm realm = Realm.getDefaultInstance()) {
            // Previous month lookup
            prevValues.clear();
            for (Asset a : realm.where(Asset.class)
                    .equalTo(AssetFields.MONTH, prevMonth.getMonth())
                    .equalTo(AssetFields.YEAR, prevMonth.getYear())
                    .findAll()) {
                prevValues.put(a.getName(), a.getValue());
            }

            // Current month — sorted descending by value
            List<Asset> all = realm.copyFromRealm(
                    realm.where(Asset.class)
                            .equalTo(AssetFields.MONTH, month.getMonth())
                            .equalTo(AssetFields.YEAR, month.getYear())
                            .sort(AssetFields.VALUE, Sort.DESCENDING)
                            .findAll());

            allAssets.clear();
            allLiabilities.clear();
            for (Asset a : all) {
                if (a.getValue() > 0)      allAssets.add(a);
                else if (a.getValue() < 0) allLiabilities.add(a);
            }
            // Liabilities: sort by abs value desc
            allLiabilities.sort((x, y) ->
                    Double.compare(Math.abs(y.getValue()), Math.abs(x.getValue())));
        }

        // Select all by default on each month load
        selectedAssetNames.clear();
        for (Asset a : allAssets) selectedAssetNames.add(a.getName());
        selectedLiabilityNames.clear();
        for (Asset a : allLiabilities) selectedLiabilityNames.add(a.getName());

        rebuildCharts();
    }

    private void rebuildCharts() {
        // ── Assets ──
        List<Asset> filteredAssets = new ArrayList<>();
        for (Asset a : allAssets) {
            if (selectedAssetNames.contains(a.getName())) filteredAssets.add(a);
        }

        if (filteredAssets.isEmpty()) {
            assetChart.setVisibility(View.INVISIBLE);
            assetTreemap.setVisibility(View.INVISIBLE);
            assetTotalView.setText("—");
        } else {
            double assetTotal = 0;
            List<PieEntry> assetEntries = new ArrayList<>();
            for (Asset a : filteredAssets) {
                assetEntries.add(new PieEntry((float) a.getValue(), a.getName()));
                assetTotal += a.getValue();
            }
            assetTotalView.setText(Tools.formatAmount(assetTotal, true));
            setupDonut(assetChart, assetEntries, ASSET_COLORS, Tools.formatAmount(assetTotal, true));
            assetTreemap.setItems(buildTreemapItems(filteredAssets, true));
            applyChartMode();
        }

        // ── Liabilities ──
        List<Asset> filteredLiabilities = new ArrayList<>();
        for (Asset a : allLiabilities) {
            if (selectedLiabilityNames.contains(a.getName())) filteredLiabilities.add(a);
        }

        if (filteredLiabilities.isEmpty()) {
            liabilityChart.setVisibility(View.INVISIBLE);
            liabilityTreemap.setVisibility(View.INVISIBLE);
            liabilityTotalView.setText("—");
        } else {
            double liabilityTotal = 0;
            List<PieEntry> liabilityEntries = new ArrayList<>();
            for (Asset a : filteredLiabilities) {
                liabilityEntries.add(new PieEntry((float) Math.abs(a.getValue()), a.getName()));
                liabilityTotal += Math.abs(a.getValue());
            }
            liabilityTotalView.setText("-" + Tools.formatAmount(liabilityTotal, true));
            setupDonut(liabilityChart, liabilityEntries, LIABILITY_COLORS,
                    "-" + Tools.formatAmount(liabilityTotal, true));
            liabilityTreemap.setItems(buildTreemapItems(filteredLiabilities, false));
            applyChartMode();
        }
    }

    // ── Filter dialog ────────────────────────────────────────────────────────

    private void showFilterDialog() {
        if (allAssets.isEmpty() && allLiabilities.isEmpty()) return;

        // Build combined display list: assets first, then liabilities with prefix
        // allItemNames[i] = display name
        // masterChecked[i] = checked state
        // isLiabilityItem[i] = whether it's a liability
        // realName[i] = actual asset name (no prefix)

        final List<String> allItemNames   = new ArrayList<>();
        final List<String> realNames      = new ArrayList<>();
        final List<Boolean> isLiability   = new ArrayList<>();

        for (Asset a : allAssets) {
            allItemNames.add(a.getName());
            realNames.add(a.getName());
            isLiability.add(false);
        }
        for (Asset a : allLiabilities) {
            allItemNames.add("[L]  " + a.getName());
            realNames.add(a.getName());
            isLiability.add(true);
        }

        final boolean[] masterChecked = new boolean[allItemNames.size()];
        for (int i = 0; i < allItemNames.size(); i++) {
            Set<String> sel = isLiability.get(i) ? selectedLiabilityNames : selectedAssetNames;
            masterChecked[i] = sel.contains(realNames.get(i));
        }

        // filteredIndices: ListView row → index in allItemNames
        final List<Integer> filteredIndices = new ArrayList<>();
        for (int i = 0; i < allItemNames.size(); i++) filteredIndices.add(i);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_asset_select, null);
        EditText search     = dialogView.findViewById(R.id.searchAsset);
        ListView listView   = dialogView.findViewById(R.id.assetList);
        Button btnSelectAll = dialogView.findViewById(R.id.btnSelectAll);
        Button btnClearAll  = dialogView.findViewById(R.id.btnClearAll);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_multiple_choice);
        for (int idx : filteredIndices) adapter.add(allItemNames.get(idx));
        listView.setAdapter(adapter);

        for (int row = 0; row < filteredIndices.size(); row++) {
            listView.setItemChecked(row, masterChecked[filteredIndices.get(row)]);
        }

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < filteredIndices.size()) {
                masterChecked[filteredIndices.get(position)] = listView.isItemChecked(position);
            }
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                String q = s.toString().toLowerCase().trim();
                filteredIndices.clear();
                for (int i = 0; i < allItemNames.size(); i++) {
                    if (q.isEmpty() || allItemNames.get(i).toLowerCase().contains(q))
                        filteredIndices.add(i);
                }
                adapter.clear();
                for (int idx : filteredIndices) adapter.add(allItemNames.get(idx));
                adapter.notifyDataSetChanged();
                for (int row = 0; row < filteredIndices.size(); row++) {
                    listView.setItemChecked(row, masterChecked[filteredIndices.get(row)]);
                }
            }
        });

        btnSelectAll.setOnClickListener(v -> {
            for (int row = 0; row < filteredIndices.size(); row++) {
                masterChecked[filteredIndices.get(row)] = true;
                listView.setItemChecked(row, true);
            }
        });

        btnClearAll.setOnClickListener(v -> {
            for (int row = 0; row < filteredIndices.size(); row++) {
                masterChecked[filteredIndices.get(row)] = false;
                listView.setItemChecked(row, false);
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.trend_select_title)
                .setView(dialogView)
                .setPositiveButton(R.string.trend_show, (dialog, which) -> {
                    Set<String> newAssets      = new HashSet<>();
                    Set<String> newLiabilities = new HashSet<>();
                    for (int i = 0; i < allItemNames.size(); i++) {
                        if (!masterChecked[i]) continue;
                        if (isLiability.get(i)) newLiabilities.add(realNames.get(i));
                        else                    newAssets.add(realNames.get(i));
                    }
                    if (newAssets.isEmpty() && newLiabilities.isEmpty()) {
                        Toast.makeText(this, R.string.trend_select_none, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    selectedAssetNames     = newAssets;
                    selectedLiabilityNames = newLiabilities;
                    rebuildCharts();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ── Treemap helpers ───────────────────────────────────────────────────────

    private List<TreemapView.Item> buildTreemapItems(List<Asset> assets, boolean isAsset) {
        List<TreemapView.Item> items = new ArrayList<>();
        for (Asset a : assets) {
            double current  = a.getValue();
            double prev     = prevValues.containsKey(a.getName()) ? prevValues.get(a.getName()) : 0.0;
            double absCurr  = Math.abs(current);
            double absPrev  = Math.abs(prev);
            double pctChange;
            if (absPrev > 0) {
                pctChange = (absCurr - absPrev) / absPrev * 100.0;
            } else {
                pctChange = absCurr > 0 ? 15.0 : 0.0;
            }
            double effectivePct = isAsset ? pctChange : -pctChange;
            items.add(new TreemapView.Item(a.getName(), (float) absCurr, changeToColor(effectivePct)));
        }
        return items;
    }

    private int changeToColor(double pct) {
        if (pct == 0.0) return 0xFF424242;
        float t = (float) Math.min(Math.abs(pct) / 15.0, 1.0);
        return pct > 0
                ? TreemapView.lerpColor(0xFF4CAF50, 0xFF1B5E20, t)
                : TreemapView.lerpColor(0xFFEF5350, 0xFF7F0000, t);
    }

    // ── Month picker ──────────────────────────────────────────────────────────

    private void showMonthYearPicker() {
        String[] allMonths  = new DateFormatSymbols().getMonths();
        String[] monthNames = Arrays.copyOf(allMonths, 12);
        int currentYear     = Calendar.getInstance().get(Calendar.YEAR);

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

    // ── Donut setup ───────────────────────────────────────────────────────────

    private void setupDonut(PieChart chart, List<PieEntry> entries, int[] colors, String centerText) {
        int labelColor          = ContextCompat.getColor(this, R.color.textPrimary);
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
