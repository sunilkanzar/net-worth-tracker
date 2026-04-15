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
        super.drawHighlighted(c, indices);

        LineData lineData = mChart.getLineData();
        if (lineData == null) return;

        for (Highlight high : indices) {
            ILineDataSet set = lineData.getDataSetByIndex(high.getDataSetIndex());
            if (set == null || !set.isHighlightEnabled()) continue;

            Entry e = set.getEntryForXValue(high.getX(), high.getY());
            if (e == null) continue;

            float[] pts = {e.getX(), e.getY()};
            mChart.getTransformer(set.getAxisDependency()).pointValuesToPixel(pts);
            float x = pts[0];
            float y = pts[1];

            int lineColor = set.getColor();

            // Outer glow
            circlePaint.setColor(lineColor);
            circlePaint.setAlpha(50);
            c.drawCircle(x, y, 20f, circlePaint);

            // Selected dot (larger than normal 4dp)
            circlePaint.setAlpha(255);
            c.drawCircle(x, y, 8f, circlePaint);

            // Hole
            circlePaint.setColor(holeColor);
            c.drawCircle(x, y, 4f, circlePaint);
        }
    }
}
