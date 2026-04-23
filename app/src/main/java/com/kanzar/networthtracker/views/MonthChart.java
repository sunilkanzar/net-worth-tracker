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
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;

import java.util.ArrayList;
import java.util.List;

public final class MonthChart extends LineChart {

    private List<Month> mMonths = new ArrayList<>();

    public MonthChart(Context context) {
        super(context);
    }

    public MonthChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MonthChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public List<Month> getMonths() {
        return mMonths;
    }

    @Override
    protected void init() {
        super.init();
        setRenderer(new SelectionHighlightRenderer(this, mAnimator, mViewPortHandler,
                ContextCompat.getColor(getContext(), R.color.bg_elev)));

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
        setHighlightPerTapEnabled(true);

        setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

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
                if (Prefs.getBoolean("privacy_mode", false)) {
                    return "****";
                }
                return Tools.formatCompact(value);
            }
        });

        // Y axis (right) — disabled
        getAxisRight().setEnabled(false);
    }

    public void updateData() {
        updateData(-1);
    }

    public void updateData(int monthsToView) {
        setVisibility(VISIBLE);

        int accentColor  = ContextCompat.getColor(getContext(), Tools.getAccentColor());

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        mMonths.clear();

        Month last = new Month().getLast();
        Month first;
        if (monthsToView > 0) {
            first = new Month(last.getMonth(), last.getYear());
            for (int i = 0; i < monthsToView - 1; i++) {
                first.previous();
            }
        } else {
            first = new Month().getFirst();
        }

        Month current = first;
        int index = 0;

        while (current != null) {
            labels.add(current.toStringMMMYY());
            mMonths.add(current);
            double value = current.getValue();
            entries.add(new Entry(index++, (float) value));
            
            if (current.getMonth() == last.getMonth() && current.getYear() == last.getYear()) break;
            
            Month next = new Month(current.getMonth(), current.getYear());
            next.next();
            current = next;
        }

        getAxisLeft().resetAxisMinimum();

        // Handle X label density to avoid overlap
        int totalLabels = labels.size();
        int labelCount = Math.min(totalLabels, 5); // Limit to 5 labels to prevent overlapping
        
        getXAxis().setLabelCount(labelCount, false);
        getXAxis().setGranularity(1.0f);
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

        setData(lineData);

        if (!entries.isEmpty()) {
            Highlight lastHighlight = new Highlight(entries.get(entries.size() - 1).getX(), entries.get(entries.size() - 1).getY(), 0);
            highlightValue(lastHighlight, true);
        }

        MonthMarkerView marker = new MonthMarkerView(getContext(), mMonths);
        marker.setChartView(this);
        setMarker(marker);

        notifyDataSetChanged();
        animateY(800);
    }
}
