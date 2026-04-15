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
                ContextCompat.getColor(getContext(), R.color.background)));

        int white      = Color.WHITE;
        int gridColor  = Color.argb(40, 255, 255, 255);  // very subtle white grid
        int labelColor = Color.argb(180, 255, 255, 255); // readable white labels

        setTouchEnabled(true);
        setDragEnabled(true);
        setScaleEnabled(true);
        setPinchZoom(true);
        setDoubleTapToZoomEnabled(true);
        setBackgroundColor(Color.TRANSPARENT);
        setGridBackgroundColor(Color.TRANSPARENT);
        setDrawGridBackground(false);
        setHighlightPerDragEnabled(true);

        Description description = new Description();
        description.setText("");
        setDescription(description);

        getLegend().setEnabled(false);

        setExtraOffsets(8f, 16f, 8f, 8f);

        // X axis
        XAxis xAxis = getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(labelColor);
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1.0f);
        xAxis.setAvoidFirstLastClipping(true);

        // Y axis (left)
        YAxis axisLeft = getAxisLeft();
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
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);        // no dots on line
        dataSet.setDrawValues(false);         // no value labels on line
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(fillColor);
        dataSet.setFillAlpha(60);
        dataSet.setHighLightColor(Color.argb(180, 255, 255, 255));
        dataSet.setDrawHorizontalHighlightIndicator(false);
        // Show a single dot at the selected/highlighted point
        dataSet.setDrawHighlightIndicators(true);
        dataSet.setHighlightLineWidth(1.5f);

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
