package com.kanzar.networthtracker;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
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
import android.content.res.ColorStateList;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.tabs.TabLayout;
import com.kanzar.networthtracker.databinding.ActivityAllocationBinding;
import com.kanzar.networthtracker.databinding.DialogAssetSelectBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.realm.Realm;
import io.realm.Sort;

public class AllocationActivity extends AppCompatActivity {

    private static final int[] ASSET_COLORS = {
        0xFF3B82F6, 0xFF22D3EE, 0xFF10B981, 0xFFA855F7,
        0xFF6366F1, 0xFFEF4444, 0xFFEAB308, 0xFFF97316,
        0xFF14B8A6, 0xFF84CC16, 0xFFE11D48, 0xFF0EA5E9
    };

    private static final int[] LIABILITY_COLORS = {
        0xFFEF4444, 0xFFF97316, 0xFFEAB308, 0xFFF43F5E,
        0xFFFB923C, 0xFFFACC15, 0xFFEC4899, 0xFFFF7043
    };

    private Month month;
    private ActivityAllocationBinding binding;

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
        binding = ActivityAllocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        int selectedMonth = getIntent().getIntExtra("month", new Month().getMonth());
        int selectedYear  = getIntent().getIntExtra("year",  new Month().getYear());
        month = new Month(selectedMonth, selectedYear);

        binding.previousMonth.setOnClickListener(v -> { month.previous(); loadData(); });
        binding.nextMonth.setOnClickListener(v -> { month.next(); loadData(); });
        binding.monthName.setOnClickListener(v -> showMonthYearPicker());

        applyAccentColor();

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    binding.assetCard.setVisibility(View.VISIBLE);
                    binding.liabilityCard.setVisibility(View.GONE);
                } else {
                    binding.assetCard.setVisibility(View.GONE);
                    binding.liabilityCard.setVisibility(View.VISIBLE);
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadData();
    }

    private void applyAccentColor() {
        int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        ColorStateList accentList = ColorStateList.valueOf(accentColor);
        
        binding.previousMonth.setImageTintList(accentList);
        binding.nextMonth.setImageTintList(accentList);
        binding.tabLayout.setSelectedTabIndicatorColor(accentColor);
        binding.tabLayout.setTabTextColors(ContextCompat.getColor(this, R.color.text_3), accentColor);
        
        binding.assetTotal.setTextColor(accentColor);
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void applyChartMode() {
        if (toggleMenuItem != null) {
            toggleMenuItem.setIcon(isTreemapMode ? R.drawable.ic_donut : R.drawable.ic_treemap);
        }
        boolean assetsEmpty      = selectedAssetNames.isEmpty()     || allAssets.isEmpty();
        boolean liabilitiesEmpty = selectedLiabilityNames.isEmpty() || allLiabilities.isEmpty();

        binding.assetChart.setVisibility((!isTreemapMode && !assetsEmpty) ? View.VISIBLE : View.GONE);
        binding.assetTreemap.setVisibility((isTreemapMode && !assetsEmpty) ? View.VISIBLE : View.GONE);
        binding.liabilityChart.setVisibility((!isTreemapMode && !liabilitiesEmpty) ? View.VISIBLE : View.GONE);
        binding.liabilityTreemap.setVisibility((isTreemapMode && !liabilitiesEmpty) ? View.VISIBLE : View.GONE);
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    private void loadData() {
        binding.monthName.setText(month.toString());

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
        double assetTotal = 0;
        List<PieEntry> assetEntries = new ArrayList<>();
        List<Asset> filteredAssets = new ArrayList<>();
        for (Asset a : allAssets) {
            if (selectedAssetNames.contains(a.getName())) {
                assetEntries.add(new PieEntry((float) a.getValue(), a.getName()));
                assetTotal += a.getValue();
                filteredAssets.add(a);
            }
        }

        boolean privacyMode = Prefs.getBoolean("privacy_mode", false);
        if (assetEntries.isEmpty()) {
            binding.assetChart.setVisibility(View.INVISIBLE);
            binding.assetTreemap.setVisibility(View.INVISIBLE);
            binding.assetTotal.setText("—");
        } else {
            binding.assetTotal.setText(privacyMode ? "****" : Tools.formatAmount(assetTotal, true));
            setupDonut(binding.assetChart, assetEntries, ASSET_COLORS, privacyMode ? "****" : Tools.formatCompact(assetTotal));
            binding.assetTreemap.setItems(buildTreemapItems(filteredAssets, true));
            applyChartMode();
        }
        populateLegend(binding.assetLegendContainer, allAssets, ASSET_COLORS, selectedAssetNames, assetTotal);

        // ── Liabilities ──
        double liabilityTotal = 0;
        List<PieEntry> liabilityEntries = new ArrayList<>();
        List<Asset> filteredLiabilities = new ArrayList<>();
        for (Asset a : allLiabilities) {
            if (selectedLiabilityNames.contains(a.getName())) {
                liabilityEntries.add(new PieEntry((float) Math.abs(a.getValue()), a.getName()));
                liabilityTotal += Math.abs(a.getValue());
                filteredLiabilities.add(a);
            }
        }

        if (liabilityEntries.isEmpty()) {
            binding.liabilityChart.setVisibility(View.INVISIBLE);
            binding.liabilityTreemap.setVisibility(View.INVISIBLE);
            binding.liabilityTotal.setText("—");
        } else {
            binding.liabilityTotal.setText(privacyMode ? "****" : "-" + Tools.formatAmount(liabilityTotal, true));
            setupDonut(binding.liabilityChart, liabilityEntries, LIABILITY_COLORS,
                    privacyMode ? "****" : "-" + Tools.formatCompact(liabilityTotal));
            binding.liabilityTreemap.setItems(buildTreemapItems(filteredLiabilities, false));
            applyChartMode();
        }
        populateLegend(binding.liabilityLegendContainer, allLiabilities, LIABILITY_COLORS, selectedLiabilityNames, liabilityTotal);
    }

    private void populateLegend(LinearLayout container, List<Asset> items, int[] colors, Set<String> selectionSet, double totalSelectedValue) {
        container.removeAllViews();
        boolean privacyMode = Prefs.getBoolean("privacy_mode", false);

        for (int i = 0; i < items.size(); i++) {
            Asset item = items.get(i);
            View view = getLayoutInflater().inflate(R.layout.item_allocation_legend, container, false);

            View colorIndicator = view.findViewById(R.id.legendColor);
            TextView nameText = view.findViewById(R.id.legendName);
            TextView pctText = view.findViewById(R.id.legendPercentage);
            TextView valueText = view.findViewById(R.id.legendValue);

            int color = colors[i % colors.length];
            if (colorIndicator.getBackground() != null) {
                colorIndicator.getBackground().mutate().setTint(color);
            }

            nameText.setText(item.getName());
            valueText.setText(privacyMode ? "****" : Tools.formatCompact(Math.abs(item.getValue())));

            boolean isSelected = selectionSet.contains(item.getName());
            if (isSelected) {
                view.setAlpha(1.0f);
                float pct = totalSelectedValue > 0 ? (float) (Math.abs(item.getValue()) / totalSelectedValue) * 100f : 0f;
                pctText.setText(String.format(Locale.US, "%.1f%%", pct));
            } else {
                view.setAlpha(0.3f);
                pctText.setText("0.0%");
            }

            view.setOnClickListener(v -> {
                if (isSelected) selectionSet.remove(item.getName());
                else selectionSet.add(item.getName());
                rebuildCharts();
            });

            container.addView(view);
        }
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

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Go to month")
                .setView(container)
                .setPositiveButton("Go", (d, which) -> {
                    month.setYear(yearPicker.getValue());
                    month.setMonth(monthPicker.getValue() + 1);
                    loadData();
                })
                .setNegativeButton("Cancel", null)
                .create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    // ── Donut setup ───────────────────────────────────────────────────────────

    private void setupDonut(PieChart chart, List<PieEntry> entries, int[] colors, String centerText) {
        int labelColor          = ContextCompat.getColor(this, R.color.textPrimary);
        int secondaryLabelColor = ContextCompat.getColor(this, R.color.textSecondary);

        chart.setUsePercentValues(true);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setHoleRadius(72f);
        chart.setTransparentCircleRadius(0f);
        chart.setDrawCenterText(true);

        // Multi-line center text: "TOTAL" small above, amount large below
        SpannableString s = new SpannableString("TOTAL\n" + centerText);
        s.setSpan(new RelativeSizeSpan(0.6f), 0, 5, 0);
        s.setSpan(new ForegroundColorSpan(secondaryLabelColor), 0, 5, 0);
        s.setSpan(new StyleSpan(Typeface.BOLD), 0, 5, 0);
        s.setSpan(new RelativeSizeSpan(1.4f), 6, s.length(), 0);
        s.setSpan(new ForegroundColorSpan(labelColor), 6, s.length(), 0);
        s.setSpan(new StyleSpan(Typeface.BOLD), 6, s.length(), 0);

        chart.setCenterText(s);
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setDrawEntryLabels(false);
        chart.setExtraOffsets(0f, 0f, 0f, 0f);

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);

        Legend legend = chart.getLegend();
        legend.setEnabled(false);

        List<Integer> colorList = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) colorList.add(colors[i % colors.length]);

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colorList);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(8f);
        dataSet.setDrawValues(false);

        PieData pieData = new PieData(dataSet);
        chart.setData(pieData);
        chart.animateY(700, Easing.EaseInOutQuad);
        chart.invalidate();
    }
}
