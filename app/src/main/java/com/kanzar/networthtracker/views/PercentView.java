package com.kanzar.networthtracker.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.databinding.ViewPercentBinding;
import com.kanzar.networthtracker.helpers.Tools;

public class PercentView extends LinearLayout {

    private double percent;
    private double valueChange;
    private ViewPercentBinding binding;

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
        binding = ViewPercentBinding.inflate(LayoutInflater.from(getContext()), this, true);
    }

    public void init(double previous, double current) {
        this.percent = Tools.getPercent(previous, current);
        this.valueChange = current - previous;

        String arrow = valueChange >= 0 ? "▲ " : "▼ ";
        binding.percentValue.setText(arrow + Tools.formatPercent(Math.abs(this.percent)));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(24f);
        int color = pillColor(this.percent, this.valueChange);
        bg.setColor(color);
        binding.percentValue.setBackground(bg);

        // Ensure text color is vibrant and matches the state
        binding.percentValue.setTextColor(valueChange >= 0 ?
                ContextCompat.getColor(getContext(), R.color.positive) : 
                ContextCompat.getColor(getContext(), R.color.negative));
    }

    public String getValueChangeString(boolean roundUp) {
        double val = roundUp ? Math.round(this.valueChange) : this.valueChange;
        return Tools.formatAmount(val);
    }

    private int getValueVisibility() {
        return View.VISIBLE;
    }

    public void fillValueChange(TextView textView) {
        fillValueChange(textView, false);
    }

    public void fillValueChange(TextView textView, boolean roundUp) {
        textView.setVisibility(getValueVisibility());
        String formatted = getValueChangeString(roundUp);
        textView.setText(formatted);
        textView.setTextColor(ContextCompat.getColor(getContext(), Tools.getTextChangeColor(this.valueChange)));
    }

    public void fillPercent(TextView textView) {
        textView.setVisibility(getValueVisibility());
        String arrow = valueChange > 0 ? "↑ " : (valueChange < 0 ? "↓ " : "");
        textView.setText(arrow + Tools.formatPercent(Math.abs(this.percent)));
        textView.setTextColor(ContextCompat.getColor(getContext(), Tools.getTextChangeColor(this.valueChange)));
    }

    private int pillColor(double percent, double valueChange) {
        if (valueChange == 0) return 0xFF546E7A;
        int colorRes = valueChange > 0 ? R.color.positive : R.color.negative;
        int baseColor = ContextCompat.getColor(getContext(), colorRes);
        // Calculate a subtle shade by using 15% opacity of the base color
        // This creates a "tinted" background that matches the theme's premium look
        return Tools.adjustAlpha(baseColor, 0.15f);
    }

    public double getPercent() { return percent; }
    public double getValueChange() { return valueChange; }
}
