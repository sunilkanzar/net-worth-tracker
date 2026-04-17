package com.kanzar.networthtracker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;

import java.util.Calendar;
import java.util.Locale;

public class GoalActivity extends AppCompatActivity {

    private EditText goal1y, goal3y, goal5y;
    private TextView rate1y, rate3y, rate5y;
    private double currentNetWorth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        goal1y = findViewById(R.id.goal1y);
        goal3y = findViewById(R.id.goal3y);
        goal5y = findViewById(R.id.goal5y);
        rate1y = findViewById(R.id.rate1y);
        rate3y = findViewById(R.id.rate3y);
        rate5y = findViewById(R.id.rate5y);

        // Current net worth passed from MainActivity
        currentNetWorth = getIntent().getDoubleExtra("current_net_worth", 0.0);
        ((TextView) findViewById(R.id.currentNetWorth)).setText(Tools.formatAmount(currentNetWorth));

        // Target year labels
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int setYear = Prefs.getInt(Prefs.PREFS_GOAL_SET_YEAR, currentYear);
        ((TextView) findViewById(R.id.label1y)).setText(getString(R.string.goal_1y_label_with_year, setYear + 1));
        ((TextView) findViewById(R.id.label3y)).setText(getString(R.string.goal_3y_label_with_year, setYear + 3));
        ((TextView) findViewById(R.id.label5y)).setText(getString(R.string.goal_5y_label_with_year, setYear + 5));

        // Pre-fill existing goals
        float g1 = Prefs.getFloat(Prefs.PREFS_GOAL_1Y, 0f);
        float g3 = Prefs.getFloat(Prefs.PREFS_GOAL_3Y, 0f);
        float g5 = Prefs.getFloat(Prefs.PREFS_GOAL_5Y, 0f);
        if (g1 > 0) goal1y.setText(formatGoalValue(g1));
        if (g3 > 0) goal3y.setText(formatGoalValue(g3));
        if (g5 > 0) goal5y.setText(formatGoalValue(g5));

        goal1y.addTextChangedListener(rateWatcher(rate1y, 1));
        goal3y.addTextChangedListener(rateWatcher(rate3y, 3));
        goal5y.addTextChangedListener(rateWatcher(rate5y, 5));

        // Show rates for pre-filled values
        updateRateHint(rate1y, g1, 1);
        updateRateHint(rate3y, g3, 3);
        updateRateHint(rate5y, g5, 5);

        findViewById(R.id.btnSaveGoals).setOnClickListener(v -> saveGoals());
    }

    private TextWatcher rateWatcher(TextView rateView, int years) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                float goal = 0f;
                try { goal = Float.parseFloat(s.toString().trim()); } catch (NumberFormatException ignored) {}
                updateRateHint(rateView, goal, years);
            }
        };
    }

    private void updateRateHint(TextView rateView, float goal, int years) {
        if (goal <= 0 || currentNetWorth <= 0) {
            rateView.setVisibility(View.GONE);
            return;
        }

        double rate = Math.pow(goal / currentNetWorth, 1.0 / years) - 1.0;
        double pct = rate * 100.0;

        String label;
        int color;

        if (pct < 0) {
            label = String.format(Locale.getDefault(), "Already exceeds goal (%.1f%% decline tolerated)", Math.abs(pct));
            color = 0xFF66BB6A; // green
        } else if (pct < 15) {
            label = String.format(Locale.getDefault(), "~%.1f%% annual growth needed  ·  Moderate", pct);
            color = 0xFF66BB6A; // green
        } else if (pct < 35) {
            label = String.format(Locale.getDefault(), "~%.1f%% annual growth needed  ·  Aggressive", pct);
            color = 0xFFFFCA28; // amber
        } else {
            label = String.format(Locale.getDefault(), "~%.1f%% annual growth needed  ·  Very aggressive", pct);
            color = 0xFFEF5350; // red
        }

        rateView.setText(label);
        rateView.setTextColor(color);
        rateView.setVisibility(View.VISIBLE);
    }

    private void saveGoals() {
        float g1 = parseInput(goal1y);
        float g3 = parseInput(goal3y);
        float g5 = parseInput(goal5y);

        Prefs.save(Prefs.PREFS_GOAL_1Y, g1);
        Prefs.save(Prefs.PREFS_GOAL_3Y, g3);
        Prefs.save(Prefs.PREFS_GOAL_5Y, g5);

        // Only update set year if goals weren't previously set
        if (!Prefs.contains(Prefs.PREFS_GOAL_SET_YEAR)) {
            Prefs.save(Prefs.PREFS_GOAL_SET_YEAR, Calendar.getInstance().get(Calendar.YEAR));
        }

        Toast.makeText(this, R.string.goal_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private float parseInput(EditText field) {
        try {
            String text = field.getText().toString().trim();
            if (text.isEmpty()) return 0f;
            return Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private String formatGoalValue(float value) {
        if (Float.isInfinite(value) || Float.isNaN(value)) return "0";
        java.math.BigDecimal bd = java.math.BigDecimal.valueOf(value);
        return bd.stripTrailingZeros().toPlainString();
    }
}
