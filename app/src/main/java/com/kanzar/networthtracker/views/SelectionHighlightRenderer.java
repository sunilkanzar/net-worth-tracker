package com.kanzar.networthtracker.views;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.renderer.LineChartRenderer;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class SelectionHighlightRenderer extends LineChartRenderer {

    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int holeColor;

    public SelectionHighlightRenderer(LineChart chart, ChartAnimator animator,
                                      ViewPortHandler viewPortHandler, int holeColor) {
        super(chart, animator, viewPortHandler);
        this.holeColor = holeColor;
        circlePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void drawHighlighted(Canvas c, Highlight[] indices) {
        LineData lineData = mChart.getLineData();
        if (lineData == null || indices.length == 0) return;

        // Get the X value of the first highlight
        float xVal = indices[0].getX();

        // Draw vertical highlight line if enabled for the first dataset
        ILineDataSet mainSet = lineData.getDataSetByIndex(indices[0].getDataSetIndex());
        if (mainSet != null && mainSet.isVerticalHighlightIndicatorEnabled()) {
            drawVerticalHighlightLine(c, xVal, mainSet);
        }

        // Draw dots for all datasets at this X value
        for (int i = 0; i < lineData.getDataSetCount(); i++) {
            ILineDataSet set = lineData.getDataSetByIndex(i);
            if (set == null || !set.isHighlightEnabled()) continue;

            Entry e = set.getEntryForXValue(xVal, Float.NaN);
            if (e == null || e.getX() != xVal) continue;

            float[] pts = {e.getX(), e.getY()};
            mChart.getTransformer(set.getAxisDependency()).pointValuesToPixel(pts);
            float x = pts[0];
            float y = pts[1];

            if (!mViewPortHandler.isInBounds(x, y)) continue;

            int lineColor = set.getColor();

            // Selected dot background (the "hole")
            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setColor(holeColor);
            c.drawCircle(x, y, 14f, circlePaint);

            // Selected dot border
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setColor(lineColor);
            circlePaint.setStrokeWidth(7f);
            c.drawCircle(x, y, 14f, circlePaint);
        }
    }

    private void drawVerticalHighlightLine(Canvas c, float xVal, ILineDataSet set) {
        mHighlightPaint.setColor(set.getHighLightColor());
        mHighlightPaint.setStrokeWidth(set.getHighlightLineWidth());
        mHighlightPaint.setPathEffect(set.getDashPathEffectHighlight());

        float[] pts = {xVal, 0};
        mChart.getTransformer(set.getAxisDependency()).pointValuesToPixel(pts);

        c.drawLine(pts[0], mViewPortHandler.contentTop(), pts[0], mViewPortHandler.contentBottom(), mHighlightPaint);
    }
}
