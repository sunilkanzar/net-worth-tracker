package com.kanzar.networthtracker;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.kanzar.networthtracker.databinding.FragmentAllocationPageBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.models.AssetFields;
import com.kanzar.networthtracker.views.TreemapView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.realm.Realm;
import io.realm.Sort;

public class AllocationPageFragment extends Fragment {

    private static final String ARG_IS_ASSET = "is_asset";
    private static final String ARG_MONTH = "month";
    private static final String ARG_YEAR = "year";

    private FragmentAllocationPageBinding binding;
    private boolean isAsset;
    private int monthNum, year;
    private boolean isTreemapMode = false;

    private List<Asset> allItems = new ArrayList<>();
    private Map<String, Double> prevValues = new HashMap<>();
    private final Set<String> selectionSet = new HashSet<>();

    public static AllocationPageFragment newInstance(boolean isAsset, int month, int year) {
        AllocationPageFragment fragment = new AllocationPageFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_ASSET, isAsset);
        args.putInt(ARG_MONTH, month);
        args.putInt(ARG_YEAR, year);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isAsset = getArguments().getBoolean(ARG_IS_ASSET);
            monthNum = getArguments().getInt(ARG_MONTH);
            year = getArguments().getInt(ARG_YEAR);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAllocationPageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.allocationTitle.setText(isAsset ? R.string.allocation_assets : R.string.allocation_liabilities);
        binding.allocationTotal.setTextColor(ContextCompat.getColor(requireContext(), isAsset ? Tools.getAccentColor() : R.color.negative));
        refresh();
    }

    public void setMonth(int month, int year) {
        this.monthNum = month;
        this.year = year;
        refresh();
    }

    public void setTreemapMode(boolean enabled) {
        this.isTreemapMode = enabled;
        updateChartMode();
        updateUI();
    }

    private void updateChartMode() {
        if (binding == null) return;
        boolean empty = selectionSet.isEmpty() || allItems.isEmpty();
        binding.pieChart.setVisibility((!isTreemapMode && !empty) ? View.VISIBLE : View.GONE);
        binding.treemapView.setVisibility((isTreemapMode && !empty) ? View.VISIBLE : View.GONE);
    }

    public void refresh() {
        if (!isAdded()) return;

        try (Realm realm = Realm.getDefaultInstance()) {
            Month prevMonth = new Month(monthNum, year);
            prevMonth.previous();

            prevValues.clear();
            for (Asset a : realm.where(Asset.class)
                    .equalTo(AssetFields.MONTH, prevMonth.getMonth())
                    .equalTo(AssetFields.YEAR, prevMonth.getYear())
                    .findAll()) {
                prevValues.put(a.getName(), a.getValue());
            }

            List<Asset> all = realm.copyFromRealm(
                    realm.where(Asset.class)
                            .equalTo(AssetFields.MONTH, monthNum)
                            .equalTo(AssetFields.YEAR, year)
                            .sort(AssetFields.VALUE, Sort.DESCENDING)
                            .findAll());

            allItems.clear();
            for (Asset a : all) {
                if (isAsset && a.getValue() > 0) allItems.add(a);
                else if (!isAsset && a.getValue() < 0) allItems.add(a);
            }

            if (!isAsset) {
                java.util.Collections.sort(allItems, (x, y) -> Double.compare(Math.abs(y.getValue()), Math.abs(x.getValue())));
            }
        }

        selectionSet.clear();
        for (Asset a : allItems) selectionSet.add(a.getName());

        updateUI();
    }

    public void updateUI() {
        if (binding == null) return;

        double totalSelectedValue = 0;
        List<PieEntry> entries = new ArrayList<>();
        List<Asset> filteredItems = new ArrayList<>();
        for (Asset a : allItems) {
            if (selectionSet.contains(a.getName())) {
                double val = Math.abs(a.getValue());
                entries.add(new PieEntry((float) val, a.getName()));
                totalSelectedValue += val;
                filteredItems.add(a);
            }
        }

        boolean privacyMode = Prefs.getBoolean("privacy_mode", false);
        int[] palette = isAsset ? Tools.ASSET_PALETTE : Tools.LIABILITY_PALETTE;

        if (entries.isEmpty()) {
            binding.pieChart.setVisibility(View.INVISIBLE);
            binding.treemapView.setVisibility(View.INVISIBLE);
            binding.allocationTotal.setText("—");
        } else {
            String prefix = isAsset ? "" : "-";
            binding.allocationTotal.setText(privacyMode ? "****" : prefix + Tools.formatAmount(totalSelectedValue, true));
            setupDonut(binding.pieChart, entries, palette, privacyMode ? "****" : prefix + Tools.formatCompact(totalSelectedValue));
            binding.treemapView.setItems(buildTreemapItems(filteredItems));
            updateChartMode();
        }
        populateLegend(binding.legendContainer, allItems, palette, totalSelectedValue);
    }

    private void populateLegend(LinearLayout container, List<Asset> items, int[] colors, double totalSelectedValue) {
        if (getContext() == null) return;
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
                updateUI();
            });

            container.addView(view);
        }
    }

    private List<TreemapView.Item> buildTreemapItems(List<Asset> assets) {
        List<TreemapView.Item> items = new ArrayList<>();
        for (Asset a : assets) {
            double current = a.getValue();
            Double prevVal = prevValues.get(a.getName());
            double prev = (prevVal != null) ? prevVal : 0.0;
            double absCurr = Math.abs(current);
            double absPrev = Math.abs(prev);
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

    private void setupDonut(PieChart chart, List<PieEntry> entries, int[] colors, String centerText) {
        if (getContext() == null) return;
        int labelColor = ContextCompat.getColor(requireContext(), R.color.textPrimary);
        int secondaryLabelColor = ContextCompat.getColor(requireContext(), R.color.textSecondary);

        chart.setUsePercentValues(true);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setHoleRadius(72f);
        chart.setTransparentCircleRadius(0f);
        chart.setDrawCenterText(true);

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
