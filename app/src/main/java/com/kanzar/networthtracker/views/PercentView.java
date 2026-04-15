package com.kanzar.networthtracker.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.helpers.Configuration;
import com.kanzar.networthtracker.helpers.Tools;

public class PercentView extends LinearLayout {

    private double percent;
    private int angle;
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
        this.angle = Tools.getAngle(this.percent, new Configuration().getMONTH_MAX_PERCENTAGE());
        
        ImageView percentIcon = findViewById(R.id.percentIcon);
        percentIcon.setColorFilter(Tools.getColor(this.angle));
        percentIcon.setRotation(this.angle);
        
        TextView percentValue = findViewById(R.id.percentValue);
        percentValue.setText(Tools.formatPercent(this.percent));
        
        this.valueChange = current - previous;
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

    public double getPercent() { return percent; }
    public int getAngle() { return angle; }
    public double getValueChange() { return valueChange; }
}
