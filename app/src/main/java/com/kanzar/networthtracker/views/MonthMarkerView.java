package com.kanzar.networthtracker.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;
import java.util.Locale;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.databinding.ViewChartMarkerBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;
import io.realm.Realm;

import androidx.core.content.ContextCompat;

import java.util.List;

public class MonthMarkerView extends MarkerView {

    private final List<Month> months;
    private final ViewChartMarkerBinding binding;

    public MonthMarkerView(Context context) {
        this(context, (AttributeSet) null);
    }

    public MonthMarkerView(Context context, AttributeSet attrs) {
        super(context, R.layout.view_chart_marker);
        this.months = new java.util.ArrayList<>();
        this.binding = ViewChartMarkerBinding.bind(getChildAt(0));
    }

    public MonthMarkerView(Context context, List<Month> months) {
        super(context, R.layout.view_chart_marker);
        this.months = months;
        this.binding = ViewChartMarkerBinding.bind(getChildAt(0));
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();
        if (index < 0 || index >= months.size()) return;

        Month month = months.get(index);
        double value  = month.getValue();
        double change;
        double percent;
        double assets;
        double liabilities;
        try (Realm realm = Realm.getDefaultInstance()) {
            change = month.getValueChange(realm);
            percent = month.getPercent(realm);
            assets = month.getAssetsValue(realm);
            liabilities = month.getLiabilitiesValue(realm);
        }

        boolean privacyMode = Prefs.getBoolean("privacy_mode", false);

        binding.markerMonth.setText(month.toStringMMMYY());
        binding.markerValue.setText(privacyMode ? "****" : Tools.formatAmount(value, true));

        String arrow = change >= 0 ? "▲ " : "▼ ";
        String changeStr = privacyMode ? "****" : arrow + Tools.formatAmount(Math.abs(change), true);
        String pct = privacyMode ? "**%" : String.format(Locale.getDefault(), "%.1f%%", Math.abs(percent));
        binding.markerChange.setText(changeStr + "  (" + pct + ")");
        binding.markerChange.setTextColor(ContextCompat.getColor(getContext(), Tools.getTextChangeColor(change)));

        String assetsLabel = getContext().getString(R.string.allocation_assets);
        String liabilitiesLabel = getContext().getString(R.string.allocation_liabilities);

        binding.markerAssets.setText(assetsLabel + ": " + (privacyMode ? "****" : Tools.formatAmount(assets, true)));
        binding.markerLiabilities.setText(liabilitiesLabel + ": " + (privacyMode ? "****" : Tools.formatAmount(liabilities, true)));

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
        MPPointF offset = getOffset();
        float width = getWidth();

        // Position marker to the side of the point to avoid blocking the line and the user's thumb
        if (posX > getChartView().getWidth() / 2f) {
            // Right side of screen: show to the left of the point
            offset.x = -width - 24f;
        } else {
            // Left side of screen: show to the right of the point
            offset.x = 24f;
        }

        // Adjust vertical position: center it vertically relative to the point
        offset.y = -(getHeight() / 2f);

        // Ensure it doesn't go off screen vertically
        if (posY + offset.y < 0) {
            offset.y = -posY;
        } else if (posY + offset.y + getHeight() > getChartView().getHeight()) {
            offset.y = getChartView().getHeight() - posY - getHeight();
        }

        return offset;
    }

    @Override
    public MPPointF getOffset() {
        // Default offset, but getOffsetForDrawingAtPoint will override for better positioning
        return new MPPointF(0, 0);
    }

}
