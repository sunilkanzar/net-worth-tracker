package com.kanzar.networthtracker.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.helpers.Tools;

public class PercentView extends LinearLayout {

    private double percent;
    private double valueChange;

    public PercentView(Context context) {
        super(context);
        initView();
    }

    public PercentView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public PercentView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        inflate(getContext(), R.layout.view_percent, this);
    }

    public void init(double previous, double current) {
        this.percent = Tools.getPercent(previous, current);
        this.valueChange = current - previous;

        TextView percentValue = findViewById(R.id.percentValue);

        String arrow = valueChange > 0 ? "▲ " : valueChange < 0 ? "▼ " : "● ";
        percentValue.setText(arrow + Tools.formatPercent(Math.abs(this.percent)));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(24f);
        bg.setColor(pillColor(this.percent, this.valueChange));
        percentValue.setBackground(bg);
    }

    public String getValueChangeString(boolean roundUp) {
        String sign = (this.valueChange > 0) ? "+" : "";
        double val = roundUp ? Math.round(this.valueChange) : this.valueChange;
        return sign + Tools.formatAmount(val);
    }

    private int getValueVisibility() {
        return (this.valueChange == 0) ? View.GONE : View.VISIBLE;
    }

    public void fillValueChange(TextView textView) {
        fillValueChange(textView, false);
    }

    public void fillValueChange(TextView textView, boolean roundUp) {
        textView.setVisibility(getValueVisibility());
        textView.setText(getValueChangeString(roundUp));
        textView.setTextColor(ContextCompat.getColor(getContext(), Tools.getTextChangeColor(this.valueChange)));
    }

    public void fillPercent(TextView textView) {
        textView.setVisibility(getValueVisibility());
        textView.setText(Tools.formatPercent(this.percent));
        textView.setTextColor(ContextCompat.getColor(getContext(), Tools.getTextChangeColor(this.valueChange)));
    }

    // neutral #546E7A → deep green #1B5E20 or deep red #7F0000
    // t = 0 at 0%, t = 1 at ±10% (MONTH_MAX_PERCENTAGE cap)
    private static int pillColor(double percent, double valueChange) {
        double t = Math.min(Math.abs(percent) / 5.0, 1.0);
        int nr = 84, ng = 110, nb = 122; // #546E7A neutral
        int tr, tg, tb;
        if (valueChange > 0) {
            tr = 27;  tg = 94;  tb = 32;  // #1B5E20 deep green
        } else if (valueChange < 0) {
            tr = 127; tg = 0;   tb = 0;   // #7F0000 deep red
        } else {
            return Color.rgb(nr, ng, nb);
        }
        return Color.rgb(
            (int)(nr + t * (tr - nr)),
            (int)(ng + t * (tg - ng)),
            (int)(nb + t * (tb - nb))
        );
    }

    public double getPercent() { return percent; }
    public double getValueChange() { return valueChange; }
}
