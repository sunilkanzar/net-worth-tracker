package com.kanzar.networthtracker;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.kanzar.networthtracker.databinding.ActivityGoalBinding;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;

import java.util.Calendar;
import java.util.Locale;

public class GoalActivity extends AppCompatActivity {

    private ActivityGoalBinding binding;
    private double currentNetWorth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Current net worth passed from MainActivity
        currentNetWorth = getIntent().getDoubleExtra("current_net_worth", 0.0);
        binding.currentNetWorth.setText(Tools.formatAmount(currentNetWorth));

        // Target year labels
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int setYear = Prefs.getInt(Prefs.PREFS_GOAL_SET_YEAR, currentYear);
        
        binding.item1y.goalLabel.setText(String.format("1 YEAR GOAL (%d)", setYear + 1));
        binding.item3y.goalLabel.setText(String.format("3 YEAR GOAL (%d)", setYear + 3));
        binding.item5y.goalLabel.setText(String.format("5 YEAR GOAL (%d)", setYear + 5));

        // Pre-fill existing goals
        float g1 = Prefs.getFloat(Prefs.PREFS_GOAL_1Y, 0f);
        float g3 = Prefs.getFloat(Prefs.PREFS_GOAL_3Y, 0f);
        float g5 = Prefs.getFloat(Prefs.PREFS_GOAL_5Y, 0f);
        
        if (g1 > 0) binding.item1y.goalInput.setText(formatGoalValue(g1));
        if (g3 > 0) binding.item3y.goalInput.setText(formatGoalValue(g3));
        if (g5 > 0) binding.item5y.goalInput.setText(formatGoalValue(g5));

        binding.item1y.goalInput.addTextChangedListener(rateWatcher(binding.item1y, 1));
        binding.item3y.goalInput.addTextChangedListener(rateWatcher(binding.item3y, 3));
        binding.item5y.goalInput.addTextChangedListener(rateWatcher(binding.item5y, 5));

        // Apply accent color
        int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        binding.btnSaveGoals.setBackgroundTintList(ColorStateList.valueOf(accentColor));
        
        binding.item1y.goalPercent.setTextColor(accentColor);
        binding.item1y.goalProgress.setProgressTintList(ColorStateList.valueOf(accentColor));
        
        binding.item3y.goalPercent.setTextColor(accentColor);
        binding.item3y.goalProgress.setProgressTintList(ColorStateList.valueOf(accentColor));
        
        binding.item5y.goalPercent.setTextColor(accentColor);
        binding.item5y.goalProgress.setProgressTintList(ColorStateList.valueOf(accentColor));

        // Show rates for pre-filled values
        updateAnalysis(binding.item1y, g1, 1);
        updateAnalysis(binding.item3y, g3, 3);
        updateAnalysis(binding.item5y, g5, 5);

        binding.btnSaveGoals.setOnClickListener(v -> saveGoals());
        
        updateSummary(g1, g3, g5);
    }

    private TextWatcher rateWatcher(com.kanzar.networthtracker.databinding.ItemGoalRowV2Binding itemBinding, int years) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                float val = 0f;
                try { val = Float.parseFloat(s.toString().trim()); } catch (NumberFormatException ignored) {}
                updateAnalysis(itemBinding, val, years);
                
                float g1 = parseInput(binding.item1y.goalInput);
                float g3 = parseInput(binding.item3y.goalInput);
                float g5 = parseInput(binding.item5y.goalInput);
                updateSummary(g1, g3, g5);
            }
        };
    }

    private void updateAnalysis(com.kanzar.networthtracker.databinding.ItemGoalRowV2Binding itemBinding, float goal, int years) {
        if (goal <= 0 || currentNetWorth <= 0) {
            itemBinding.analysisLayout.setVisibility(View.GONE);
            itemBinding.goalProgress.setProgress(0);
            itemBinding.goalPercent.setText("0%");
            return;
        }

        itemBinding.analysisLayout.setVisibility(View.VISIBLE);
        
        int progress = (int) Math.min(Math.max(currentNetWorth / goal * 100, 0), 100);
        itemBinding.goalProgress.setProgress(progress);
        itemBinding.goalPercent.setText(progress + "%");

        double rate = Math.pow(goal / currentNetWorth, 1.0 / years) - 1.0;
        double pct = rate * 100.0;
        double remaining = goal - currentNetWorth;

        String intensity;

        if (pct < 0) {
            intensity = "Already reached!";
        } else if (pct < 10) {
            intensity = "Steady";
        } else if (pct < 20) {
            intensity = "Moderate";
        } else if (pct < 35) {
            intensity = "Aggressive";
        } else {
            intensity = "Very aggressive";
        }

        StringBuilder sb = new StringBuilder();
        if (remaining > 0) {
            sb.append("Gap: ").append(Tools.formatAmount(remaining));
            sb.append("\nReq. Annual Growth: ").append(String.format(Locale.getDefault(), "%.1f%%", pct));
            sb.append(" (").append(intensity).append(")");
            
            double monthly = remaining / (years * 12.0);
            sb.append("\nEst. Monthly Addition: ").append(Tools.formatAmount(monthly));
        } else {
            sb.append("Milestone achieved! You are ").append(Tools.formatAmount(Math.abs(remaining))).append(" ahead.");
        }

        itemBinding.goalAnalysis.setText(sb.toString());
    }
    
    private void updateSummary(float g1, float g3, float g5) {
        if (g1 <= 0 && g3 <= 0 && g5 <= 0) {
            binding.goalSummaryText.setText("Set your future milestones to visualize the growth needed to reach your financial freedom.");
            return;
        }
        
        float maxGoal = Math.max(g1, Math.max(g3, g5));
        if (maxGoal > 0) {
            double multiplier = maxGoal / currentNetWorth;
            binding.goalSummaryText.setText(String.format(Locale.getDefault(), 
                "To reach your ultimate goal of %s, your wealth needs to grow by %.1fx from its current level.",
                Tools.formatAmount(maxGoal), multiplier));
        }
    }

    private void saveGoals() {
        float g1 = parseInput(binding.item1y.goalInput);
        float g3 = parseInput(binding.item3y.goalInput);
        float g5 = parseInput(binding.item5y.goalInput);

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
