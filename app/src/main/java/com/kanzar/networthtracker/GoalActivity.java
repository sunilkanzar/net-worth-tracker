package com.kanzar.networthtracker;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;

import java.util.Calendar;

public class GoalActivity extends AppCompatActivity {

    private EditText goal1y, goal3y, goal5y;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        goal1y = findViewById(R.id.goal1y);
        goal3y = findViewById(R.id.goal3y);
        goal5y = findViewById(R.id.goal5y);

        // Current net worth passed from MainActivity
        double currentNetWorth = getIntent().getDoubleExtra("current_net_worth", 0.0);
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

        findViewById(R.id.btnSaveGoals).setOnClickListener(v -> saveGoals());
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
