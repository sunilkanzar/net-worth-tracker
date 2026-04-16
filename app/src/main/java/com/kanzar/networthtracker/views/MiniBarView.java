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
        dimPaint.setColor(0x30FFFFFF);  // faint white for previous months
        hlPaint.setColor(0xCCFFCA28);   // gold for current month
    }

    public void setData(List<Float> values, int highlightIndex) {
        this.values = values;
        this.highlightIndex = highlightIndex;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (values == null || values.isEmpty()) return;
        int n = values.size();
        float w = getWidth();
        float h = getHeight();
        float gap = 4f;
        float barW = (w - gap * (n - 1)) / n;
        float radius = 3f;

        float max = 0;
        for (float v : values) if (Math.abs(v) > max) max = Math.abs(v);
        if (max == 0) return;

        for (int i = 0; i < n; i++) {
            float barH = Math.max(4f, Math.abs(values.get(i)) / max * h);
            float left = i * (barW + gap);
            rect.set(left, h - barH, left + barW, h);
            canvas.drawRoundRect(rect, radius, radius, i == highlightIndex ? hlPaint : dimPaint);
        }
    }
}
