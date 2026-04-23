package com.kanzar.networthtracker.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class MiniBarView extends View {

    private List<Float> values;
    private int highlightIndex = -1;

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hlPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public MiniBarView(Context context) { super(context); init(); }
    public MiniBarView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public MiniBarView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); init(); }

    private void init() {
        dimPaint.setColor(0x2EFFFFFF);  // 18% white
        hlPaint.setColor(0xFF10B981);   // Emerald (accent)
    }

    public void setData(List<Float> values, int highlightIndex) {
        this.values = values;
        this.highlightIndex = highlightIndex;
        invalidate();
    }

    public void setHighlightColor(int color) {
        hlPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (values == null || values.isEmpty()) return;
        int n = values.size();
        float w = getWidth();
        float h = getHeight();
        float gap = 3f;
        float barW = (w - gap * (n - 1)) / n;
        float radius = 2f;

        float max = 0;
        float min = Float.MAX_VALUE;
        for (float v : values) {
            if (v > max) max = v;
            if (v < min) min = v;
        }
        float range = max - min;
        if (range == 0) range = 1;

        for (int i = 0; i < n; i++) {
            float barH = ((values.get(i) - min) / range) * (h - 6) + 6;
            float left = i * (barW + gap);
            rect.set(left, h - barH, left + barW, h);
            canvas.drawRoundRect(rect, radius, radius, i == highlightIndex ? hlPaint : dimPaint);
        }
    }
}
