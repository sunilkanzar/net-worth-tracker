package com.kanzar.networthtracker.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.kanzar.networthtracker.helpers.Tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TreemapView extends View {

    public static class Item {
        public final String name;
        public final float value;
        public final int color;
        final RectF rect = new RectF();

        public Item(String name, float value, int color) {
            this.name = name;
            this.value = value;
            this.color = color;
        }
    }

    private List<Item> items = new ArrayList<>();
    private float[] layoutAreas;

    private final Paint fillPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public TreemapView(Context context) {
        super(context);
        init();
    }

    public TreemapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        valuePaint.setColor(0xCCFFFFFF);
        valuePaint.setTypeface(Typeface.DEFAULT);
    }

    /** Items should be positive-value; caller controls color. */
    public void setItems(List<Item> newItems) {
        // Sort descending by value for squarified algorithm
        items = new ArrayList<>(newItems);
        Collections.sort(items, (a, b) -> Float.compare(b.value, a.value));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (items.isEmpty() || getWidth() == 0 || getHeight() == 0) return;

        float totalValue = 0;
        for (Item item : items) totalValue += item.value;
        if (totalValue == 0) return;

        float canvasArea = (float) getWidth() * getHeight();
        layoutAreas = new float[items.size()];
        for (int i = 0; i < items.size(); i++) {
            layoutAreas[i] = items.get(i).value / totalValue * canvasArea;
        }

        squarify(0, items.size(), new RectF(0, 0, getWidth(), getHeight()));

        float gap = 2f;
        for (Item item : items) {
            RectF r = new RectF(
                    item.rect.left + gap,
                    item.rect.top + gap,
                    item.rect.right - gap,
                    item.rect.bottom - gap);
            if (r.width() <= 0 || r.height() <= 0) continue;

            fillPaint.setColor(item.color);
            canvas.drawRect(r, fillPaint);

            drawLabel(canvas, item, r);
        }
    }

    private void drawLabel(Canvas canvas, Item item, RectF r) {
        float w = r.width();
        float h = r.height();
        if (w < 24 || h < 14) return;

        float pad = 5f;
        float maxW = w - pad * 2;

        // Scale font to fit width and height
        float fontSize = Math.min(w / (item.name.length() * 0.65f), h * 0.32f);
        fontSize = Math.min(fontSize, 15f);
        fontSize = Math.max(fontSize, 8f);
        textPaint.setTextSize(fontSize);

        String label = fitText(textPaint, item.name, maxW);
        float cx = r.left + w / 2f;
        float cy = r.top + h / 2f;

        boolean showValue = h >= 38;

        if (showValue) {
            String valStr = Tools.formatAmount((double) item.value, true);
            valuePaint.setTextSize(fontSize * 0.78f);
            float valW = valuePaint.measureText(valStr);

            float nameY = cy - fontSize * 0.15f;
            float valY  = cy + fontSize * 1.0f;

            canvas.drawText(label, cx - textPaint.measureText(label) / 2f, nameY, textPaint);
            if (valW <= maxW) {
                canvas.drawText(valStr, cx - valW / 2f, valY, valuePaint);
            }
        } else {
            canvas.drawText(label, cx - textPaint.measureText(label) / 2f,
                    cy + fontSize * 0.35f, textPaint);
        }
    }

    private String fitText(Paint paint, String text, float maxWidth) {
        if (paint.measureText(text) <= maxWidth) return text;
        while (text.length() > 1 && paint.measureText(text + "…") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text.length() > 1 ? text + "…" : text;
    }

    // ── Squarified treemap ──────────────────────────────────────────────────

    private void squarify(int start, int end, RectF bounds) {
        if (start >= end) return;
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            for (int i = start; i < end; i++) items.get(i).rect.set(bounds);
            return;
        }
        if (end - start == 1) {
            items.get(start).rect.set(bounds);
            return;
        }

        float shortSide = Math.min(bounds.width(), bounds.height());

        // Greedily build current row: keep adding while aspect ratio improves
        int rowEnd = start + 1;
        float rowArea = layoutAreas[start];

        while (rowEnd < end) {
            float extended = rowArea + layoutAreas[rowEnd];
            if (worstRatio(start, rowEnd, rowArea, shortSide)
                    <= worstRatio(start, rowEnd + 1, extended, shortSide)) {
                break; // adding next item makes worst ratio worse
            }
            rowArea += layoutAreas[rowEnd];
            rowEnd++;
        }

        // Lay out items[start..rowEnd) in the row
        boolean horizontal = bounds.width() <= bounds.height();
        float rowThickness = rowArea / shortSide;
        float offset = horizontal ? bounds.left : bounds.top;

        for (int i = start; i < rowEnd; i++) {
            float itemLen = rowArea > 0 ? layoutAreas[i] / rowArea * shortSide : 0;
            if (horizontal) {
                items.get(i).rect.set(offset, bounds.top, offset + itemLen, bounds.top + rowThickness);
            } else {
                items.get(i).rect.set(bounds.left, offset, bounds.left + rowThickness, offset + itemLen);
            }
            offset += itemLen;
        }

        // Recurse on remaining area
        RectF remaining = horizontal
                ? new RectF(bounds.left, bounds.top + rowThickness, bounds.right, bounds.bottom)
                : new RectF(bounds.left + rowThickness, bounds.top, bounds.right, bounds.bottom);
        squarify(rowEnd, end, remaining);
    }

    /** Worst aspect ratio of items[start..end) placed in a row of thickness rowArea/shortSide. */
    private float worstRatio(int start, int end, float rowArea, float shortSide) {
        if (rowArea == 0 || shortSide == 0) return Float.MAX_VALUE;
        float thickness = rowArea / shortSide;
        float worst = 0;
        for (int i = start; i < end; i++) {
            float len = layoutAreas[i] / rowArea * shortSide;
            if (len == 0) continue;
            float ratio = Math.max(thickness / len, len / thickness);
            if (ratio > worst) worst = ratio;
        }
        return worst;
    }

    // ── Color helpers ────────────────────────────────────────────────────────

    /** Lerp between two ARGB colors. t=0 → colorA, t=1 → colorB. */
    public static int lerpColor(int colorA, int colorB, float t) {
        t = Math.max(0, Math.min(1, t));
        return Color.argb(
                (int) (Color.alpha(colorA) + (Color.alpha(colorB) - Color.alpha(colorA)) * t),
                (int) (Color.red(colorA)   + (Color.red(colorB)   - Color.red(colorA))   * t),
                (int) (Color.green(colorA) + (Color.green(colorB) - Color.green(colorA)) * t),
                (int) (Color.blue(colorA)  + (Color.blue(colorB)  - Color.blue(colorA))  * t));
    }
}
