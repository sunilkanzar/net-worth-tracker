package com.kanzar.networthtracker;

import android.graphics.Color;
import android.view.Gravity;
import androidx.appcompat.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
    private TextView assetTotalView;
    private TextView liabilityTotalView;
    private TextView monthName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allocation);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        int selectedMonth = getIntent().getIntExtra("month", new Month().getMonth());
        int selectedYear  = getIntent().getIntExtra("year",  new Month().getYear());
        month = new Month(selectedMonth, selectedYear);

        assetChart      = findViewById(R.id.assetChart);
        liabilityChart  = findViewById(R.id.liabilityChart);
        assetTotalView  = findViewById(R.id.assetTotal);
        liabilityTotalView = findViewById(R.id.liabilityTotal);
        monthName       = findViewById(R.id.monthName);

        findViewById(R.id.previousMonth).setOnClickListener(v -> {
            month.previous();
            loadData();
        });

        findViewById(R.id.nextMonth).setOnClickListener(v -> {
            month.next();
            loadData();
        });

        monthName.setOnClickListener(v -> showMonthYearPicker());

        loadData();
    }

    private void loadData() {
        monthName.setText(month.toString());

        List<Asset> all = Realm.getDefaultInstance()
                .where(Asset.class)
                .equalTo(AssetFields.MONTH, month.getMonth())
                .equalTo(AssetFields.YEAR, month.getYear())
                .sort(AssetFields.VALUE, Sort.DESCENDING)
                .findAll();

        List<PieEntry> assetEntries     = new ArrayList<>();
        List<PieEntry> liabilityEntries = new ArrayList<>();
        double assetTotal     = 0;
        double liabilityTotal = 0;

        for (Asset asset : all) {
            double val = asset.getValue();
            if (val > 0) {
                assetEntries.add(new PieEntry((float) val, asset.getName()));
                assetTotal += val;
            } else if (val < 0) {
                liabilityEntries.add(new PieEntry((float) Math.abs(val), asset.getName()));
                liabilityTotal += Math.abs(val);
            }
        }

        if (assetEntries.isEmpty()) {
            assetChart.setVisibility(View.INVISIBLE);
            assetTotalView.setText("—");
        } else {
            assetChart.setVisibility(View.VISIBLE);
            assetTotalView.setText(Tools.formatAmount(assetTotal, true));
            setupDonut(assetChart, assetEntries, ASSET_COLORS, Tools.formatAmount(assetTotal, true));
        }

        if (liabilityEntries.isEmpty()) {
            liabilityChart.setVisibility(View.INVISIBLE);
            liabilityTotalView.setText("—");
        } else {
            liabilityChart.setVisibility(View.VISIBLE);
            liabilityTotalView.setText("-" + Tools.formatAmount(liabilityTotal, true));
            setupDonut(liabilityChart, liabilityEntries, LIABILITY_COLORS,
                    "-" + Tools.formatAmount(liabilityTotal, true));
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
        yearPicker.setMinValue(2000);
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
        chart.setUsePercentValues(true);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setHoleRadius(56f);
        chart.setTransparentCircleRadius(61f);
        chart.setTransparentCircleColor(Color.argb(30, 255, 255, 255));
        chart.setDrawCenterText(true);
        chart.setCenterText(centerText);
        chart.setCenterTextColor(Color.WHITE);
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
        legend.setTextColor(Color.argb(200, 255, 255, 255));
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
        for (int i = 0; i < entries.size(); i++) {
            colorList.add(colors[i % colors.length]);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colorList);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(6f);
        dataSet.setValueLinePart1OffsetPercentage(80f);
        dataSet.setValueLinePart1Length(0.4f);
        dataSet.setValueLinePart2Length(0.3f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLineColor(Color.argb(120, 255, 255, 255));

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(chart));
        pieData.setValueTextSize(10f);
        pieData.setValueTextColor(Color.WHITE);
        pieData.setValueTypeface(Typeface.DEFAULT_BOLD);

        chart.setData(pieData);
        chart.animateY(700, Easing.EaseInOutQuad);
        chart.invalidate();
    }
}
