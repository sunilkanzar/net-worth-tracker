package com.kanzar.networthtracker.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.databinding.ViewTrendMarkerBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;

import java.util.List;

public class TrendMarkerView extends MarkerView {

    private final List<Month> months;
    private final List<String> assetNames;
    private final ViewTrendMarkerBinding binding;

    public TrendMarkerView(Context context) {
        this(context, (AttributeSet) null);
    }

    public TrendMarkerView(Context context, AttributeSet attrs) {
        super(context, R.layout.view_trend_marker);
        this.months     = new java.util.ArrayList<>();
        this.assetNames = new java.util.ArrayList<>();
        this.binding = ViewTrendMarkerBinding.bind(getChildAt(0));
    }

    public TrendMarkerView(Context context, List<Month> months, List<String> assetNames) {
        super(context, R.layout.view_trend_marker);
        this.months     = months;
        this.assetNames = assetNames;
        this.binding = ViewTrendMarkerBinding.bind(getChildAt(0));
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int xIndex       = (int) e.getX();
        int datasetIndex = highlight.getDataSetIndex();

        String assetName = (datasetIndex >= 0 && datasetIndex < assetNames.size())
                ? assetNames.get(datasetIndex) : "";
        String monthLabel = (xIndex >= 0 && xIndex < months.size())
                ? months.get(xIndex).toString() : "";

        float value = e.getY();
        boolean isLiability = value < 0;

        boolean privacyMode = Prefs.getBoolean("privacy_mode", false);

        binding.markerAsset.setText(assetName);
        binding.markerMonth.setText(monthLabel);
        binding.markerValue.setText(privacyMode ? "****" : Tools.formatAmount(value, true));
        binding.markerValue.setTextColor(ContextCompat.getColor(getContext(), isLiability ? R.color.negative : R.color.positive));

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 16f);
    }
}
