package com.kanzar.networthtracker.views;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Tools;

import java.util.ArrayList;
import java.util.List;

public final class MonthChart extends LineChart {

    public MonthChart(Context context) {
        super(context);
    }

    public MonthChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MonthChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();
        setRenderer(new SelectionHighlightRenderer(this, mAnimator, mViewPortHandler,
                ContextCompat.getColor(getContext(), R.color.backgroundContent)));

        int labelColor = ContextCompat.getColor(getContext(), R.color.textSecondary);
        int gridColor  = ContextCompat.getColor(getContext(), R.color.divider);

        setTouchEnabled(true);
        setDragEnabled(true);
        setScaleEnabled(false);
        setPinchZoom(false);
        setDoubleTapToZoomEnabled(false);
        setBackgroundColor(Color.TRANSPARENT);
        setGridBackgroundColor(Color.TRANSPARENT);
        setDrawGridBackground(false);
        setHighlightPerDragEnabled(true);

        Description description = new Description();
        description.setText("");
        setDescription(description);

        getLegend().setEnabled(false);

        setExtraOffsets(12f, 16f, 12f, 16f);

        // X axis
        XAxis xAxis = getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(labelColor);
        xAxis.setTextSize(9f);
        xAxis.setGranularity(1.0f);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setYOffset(10f);

        // Y axis (left)
        YAxis axisLeft = getAxisLeft();
        axisLeft.setDrawAxisLine(false);
        axisLeft.setDrawGridLines(true);
        axisLeft.setTextColor(labelColor);
        axisLeft.setTextSize(9f);
        axisLeft.setGridColor(gridColor);
        axisLeft.setGridLineWidth(0.5f);
        axisLeft.setLabelCount(5, false);
        axisLeft.setXOffset(10f);
        axisLeft.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (Math.abs(value) >= 1_000_000) {
                    return Tools.formatAmount(value / 1_000_000.0) + "M";
                } else if (Math.abs(value) >= 1_000) {
                    return Tools.formatAmount(value / 1_000.0) + "k";
                }
                return Tools.formatAmount(value);
            }
        });

        // Y axis (right) — disabled
        getAxisRight().setEnabled(false);
    }

    public void updateData() {
        setVisibility(VISIBLE);

        int accentColor  = ContextCompat.getColor(getContext(), R.color.colorAccent);
        int fillColor    = ContextCompat.getColor(getContext(), R.color.colorPrimary);

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Month> months = new ArrayList<>();

        Month last = new Month().getLast();
        Month current = new Month().getFirst();
        int index = 0;
        boolean hasNegative = false;

        while (current != null) {
            labels.add(current.toStringMMYY());
            months.add(current);
            double value = current.getValue();
            if (value < 0) hasNegative = true;
            entries.add(new Entry(index++, (float) value));
            if (current.getMonth() == last.getMonth() && current.getYear() == last.getYear()) break;
            Month next = new Month(current.getMonth(), current.getYear());
            next.next();
            current = next;
        }

        if (!hasNegative) {
            getAxisLeft().setAxisMinimum(0.0f);
        }

        // Reduce X label density if many months
        int labelCount = Math.min(labels.size(), 8);
        getXAxis().setLabelCount(labelCount, false);
        getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(accentColor);
        dataSet.setLineWidth(2.0f);
        dataSet.setDrawCircles(false);        // no dots on line
        dataSet.setDrawValues(false);         // no value labels on line
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(accentColor);
        dataSet.setFillAlpha(30);
        dataSet.setHighLightColor(accentColor);
        dataSet.setDrawHorizontalHighlightIndicator(false);
        // Show a single dot at the selected/highlighted point
        dataSet.setDrawHighlightIndicators(true);
        dataSet.setHighlightLineWidth(1.0f);

        LineData lineData = new LineData(dataSet);
        lineData.setDrawValues(false);

        MonthMarkerView marker = new MonthMarkerView(getContext(), months);
        marker.setChartView(this);
        setMarker(marker);

        setData(lineData);
        notifyDataSetChanged();
        animateY(800);
    }
}
