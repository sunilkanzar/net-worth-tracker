package com.kanzar.networthtracker.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import java.util.Locale;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Tools;

import java.util.List;

public class MonthMarkerView extends MarkerView {

    private final List<Month> months;
    private final TextView tvMonth;
    private final TextView tvValue;
    private final TextView tvChange;

    public MonthMarkerView(Context context) {
        this(context, (AttributeSet) null);
    }

    public MonthMarkerView(Context context, AttributeSet attrs) {
        super(context, R.layout.view_chart_marker);
        this.months = new java.util.ArrayList<>();
        tvMonth  = null;
        tvValue  = null;
        tvChange = null;
    }

    public MonthMarkerView(Context context, List<Month> months) {
        super(context, R.layout.view_chart_marker);
        this.months = months;
        tvMonth  = findViewById(R.id.markerMonth);
        tvValue  = findViewById(R.id.markerValue);
        tvChange = findViewById(R.id.markerChange);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();
        if (index < 0 || index >= months.size()) return;

        Month month = months.get(index);
        double value  = month.getValue();
        double change = month.getValueChange();

        tvMonth.setText(month.toString());
        tvValue.setText(Tools.formatAmount(value, true));

        String sign = change >= 0 ? "+" : "";
        String changeStr = sign + Tools.formatAmount(change, true);
        String pct = String.format(Locale.getDefault(), "%.1f%%", month.getPercent());
        tvChange.setText(changeStr + "  (" + pct + ")");
        tvChange.setTextColor(change >= 0
                ? 0xFF00C853   // positive green
                : 0xFFFF5252); // negative red

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        // Center horizontally, place above the point
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 16f);
    }

}
