package com.kanzar.networthtracker;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.kanzar.networthtracker.databinding.ActivityGoalBinding;
import com.kanzar.networthtracker.databinding.ItemGoalRowV2Binding;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.Goal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.Sort;

public class GoalActivity extends AppCompatActivity {

    private ActivityGoalBinding binding;
    private double currentNetWorth;
    private final List<Goal> goalList = new ArrayList<>();
    private int accentColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        currentNetWorth = getIntent().getDoubleExtra("current_net_worth", 0.0);
        binding.currentNetWorth.setText(Tools.formatAmount(currentNetWorth));

        accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        binding.btnSaveGoals.setBackgroundTintList(ColorStateList.valueOf(accentColor));
        binding.btnAddGoal.setTextColor(accentColor);
        binding.btnAddGoal.setIconTint(ColorStateList.valueOf(accentColor));

        loadGoals();

        binding.btnAddGoal.setOnClickListener(v -> {
            if (goalList.size() >= 3) {
                Toast.makeText(this, "Maximum 3 goals allowed", Toast.LENGTH_SHORT).show();
                return;
            }
            addNewGoal();
        });

        binding.btnSaveGoals.setOnClickListener(v -> saveGoals());
        updateSummary();
    }

    private void loadGoals() {
        try (Realm realm = Realm.getDefaultInstance()) {
            List<Goal> goals = realm.copyFromRealm(realm.where(Goal.class).sort("targetYear", Sort.ASCENDING).findAll());
            goalList.addAll(goals);
        }
        renderGoals();
    }

    private void addNewGoal() {
        int year = Calendar.getInstance().get(Calendar.YEAR) + 1;
        if (!goalList.isEmpty()) {
            year = goalList.get(goalList.size() - 1).getTargetYear() + 1;
        }
        goalList.add(new Goal(year, 0));
        renderGoals();
        updateSummary();
    }

    private void renderGoals() {
        binding.goalsContainer.removeAllViews();
        for (int i = 0; i < goalList.size(); i++) {
            Goal goal = goalList.get(i);
            ItemGoalRowV2Binding rowBinding = ItemGoalRowV2Binding.inflate(LayoutInflater.from(this), binding.goalsContainer, false);
            setupRow(rowBinding, goal, i);
            binding.goalsContainer.addView(rowBinding.getRoot());
        }
        binding.btnAddGoal.setVisibility(goalList.size() >= 3 ? View.GONE : View.VISIBLE);
    }

    private void setupRow(ItemGoalRowV2Binding row, Goal goal, int index) {
        row.goalLabel.setText("TARGET YEAR: " + goal.getTargetYear());
        if (goal.getTargetValue() > 0) {
            row.goalInput.setText(formatGoalValue((float) goal.getTargetValue()));
        }

        row.goalLabelContainer.setOnClickListener(v -> pickYear(row, goal));
        row.goalInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                float val = parseInput(row.goalInput);
                goal.setTargetValue(val);
                updateAnalysis(row, val, goal.getTargetYear());
                updateSummary();
            }
        });

        row.btnDeleteGoal.setOnClickListener(v -> {
            goalList.remove(index);
            renderGoals();
            updateSummary();
        });

        applyAccent(row, accentColor);
        updateAnalysis(row, (float) goal.getTargetValue(), goal.getTargetYear());
    }

    private void applyAccent(ItemGoalRowV2Binding row, int color) {
        row.goalPercent.setTextColor(color);
        row.goalProgress.setProgressTintList(ColorStateList.valueOf(color));
    }

    private void pickYear(ItemGoalRowV2Binding row, Goal goal) {
        final NumberPicker picker = new NumberPicker(this);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        picker.setMinValue(currentYear);
        picker.setMaxValue(currentYear + 50);
        picker.setValue(goal.getTargetYear());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select Target Year")
                .setView(picker)
                .setPositiveButton("OK", (d, w) -> {
                    goal.setTargetYear(picker.getValue());
                    row.goalLabel.setText("TARGET YEAR: " + goal.getTargetYear());
                    updateAnalysis(row, (float) goal.getTargetValue(), goal.getTargetYear());
                    updateSummary();
                })
                .setNegativeButton("Cancel", null)
                .create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    private void updateAnalysis(ItemGoalRowV2Binding itemBinding, float goalValue, int targetYear) {
        if (goalValue <= 0 || currentNetWorth <= 0) {
            itemBinding.analysisLayout.setVisibility(View.GONE);
            itemBinding.goalProgress.setProgress(0);
            itemBinding.goalPercent.setText("0%");
            return;
        }

        itemBinding.analysisLayout.setVisibility(View.VISIBLE);
        int progress = (int) Math.min(Math.max(currentNetWorth / goalValue * 100, 0), 100);
        itemBinding.goalProgress.setProgress(progress);
        itemBinding.goalPercent.setText(progress + "%");

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int years = Math.max(1, targetYear - currentYear);

        double rate = Math.pow(goalValue / currentNetWorth, 1.0 / years) - 1.0;
        double pct = rate * 100.0;
        double remaining = goalValue - currentNetWorth;

        String intensity;
        if (pct < 0) intensity = "Already reached!";
        else if (pct < 10) intensity = "Steady";
        else if (pct < 20) intensity = "Moderate";
        else if (pct < 35) intensity = "Aggressive";
        else intensity = "Very aggressive";

        StringBuilder sb = new StringBuilder();
        if (remaining > 0) {
            sb.append("Gap: ").append(Tools.formatAmount(remaining));
            sb.append("\nReq. Annual Growth: ").append(String.format(Locale.getDefault(), "%.1f%%", pct));
            sb.append(" (").append(intensity).append(")");
            sb.append("\nEst. Monthly Addition: ").append(Tools.formatAmount(remaining / (years * 12.0)));
        } else {
            sb.append("Milestone achieved! You are ").append(Tools.formatAmount(Math.abs(remaining))).append(" ahead.");
        }
        itemBinding.goalAnalysis.setText(sb.toString());
    }

    private void updateSummary() {
        if (goalList.isEmpty()) {
            binding.goalSummaryText.setText("Set your future milestones to visualize the growth needed to reach your financial freedom.");
            return;
        }

        double maxGoal = 0;
        for (Goal g : goalList) {
            if (g.getTargetValue() > maxGoal) maxGoal = g.getTargetValue();
        }

        if (maxGoal <= 0) {
            binding.goalSummaryText.setText("Set your future milestones to visualize the growth needed to reach your financial freedom.");
            return;
        }

        double multiplier = maxGoal / currentNetWorth;
        binding.goalSummaryText.setText(String.format(Locale.getDefault(),
                "To reach your ultimate goal of %s, your wealth needs to grow by %.1fx from its current level.",
                Tools.formatAmount(maxGoal), multiplier));
    }

    private void saveGoals() {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                // Delete all existing goals first to handle removals
                r.delete(Goal.class);
                
                // Save valid goals from the list
                for (Goal goal : goalList) {
                    if (goal.getTargetValue() > 0) {
                        goal.updateTimestamp();
                        r.copyToRealmOrUpdate(goal);
                    }
                }
            });
        }
        Toast.makeText(this, R.string.goal_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private float parseInput(EditText field) {
        try {
            String text = field.getText().toString().trim();
            return text.isEmpty() ? 0f : Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private String formatGoalValue(float value) {
        if (Float.isInfinite(value) || Float.isNaN(value)) return "0";
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
