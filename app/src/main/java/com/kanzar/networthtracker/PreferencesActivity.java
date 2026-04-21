package com.kanzar.networthtracker;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.kanzar.networthtracker.databinding.ActivityPreferencesBinding;
import com.kanzar.networthtracker.databinding.ItemPreferenceRowBinding;
import com.kanzar.networthtracker.helpers.Prefs;

import io.realm.Realm;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PreferencesActivity extends AppCompatActivity {

    private ActivityPreferencesBinding binding;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPreferencesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        setupClickListeners();
        updateUI();

        binding.btnClearAll.setOnClickListener(v -> confirmClearAllData());
    }

    private void setupClickListeners() {
        binding.rowCurrency.getRoot().setOnClickListener(v -> showCurrencyDialog());
        binding.rowFormat.getRoot().setOnClickListener(v -> showFormatDialog());
        binding.rowSeparator.getRoot().setOnClickListener(v -> showSeparatorDialog());
        binding.rowTheme.getRoot().setOnClickListener(v -> showThemeDialog());
    }

    private void updateUI() {
        setRowData(binding.rowCurrency, getString(R.string.pref_currency_label), getCurrencySummary());
        setRowData(binding.rowFormat, getString(R.string.pref_format_label), getFormatSummary());
        setRowData(binding.rowSeparator, getString(R.string.pref_separator_label), getSeparatorSummary());
        setRowData(binding.rowTheme, getString(R.string.pref_theme_label), getThemeSummary());
    }

    private void setRowData(ItemPreferenceRowBinding rowBinding, String title, String summary) {
        rowBinding.prefTitle.setText(title);
        rowBinding.prefSummary.setText(summary);
    }

    private void showCurrencyDialog() {
        String[] options = {
            getString(R.string.pref_currency_inr),
            getString(R.string.pref_currency_usd),
            getString(R.string.pref_currency_eur),
            getString(R.string.pref_currency_gbp),
            getString(R.string.pref_currency_jpy),
            getString(R.string.pref_currency_none)
        };
        String[] values = {"₹", "$", "€", "£", "¥", ""};
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.pref_currency_label)
            .setItems(options, (dialog, which) -> {
                Prefs.save(Prefs.PREFS_CURRENCY, values[which]);
                updateUI();
            }).show();
    }

    private void showFormatDialog() {
        String[] options = {
            getString(R.string.pref_format_in),
            getString(R.string.pref_format_int),
            getString(R.string.pref_format_none)
        };
        String[] values = {"IN", "INT", "NONE"};

        new AlertDialog.Builder(this)
            .setTitle(R.string.pref_format_label)
            .setItems(options, (dialog, which) -> {
                Prefs.save(Prefs.PREFS_NUMBER_FORMAT, values[which]);
                updateUI();
            }).show();
    }

    private void showSeparatorDialog() {
        String[] options = {
            getString(R.string.pref_sep_space),
            getString(R.string.pref_sep_comma)
        };
        String[] values = {" ", ","};

        new AlertDialog.Builder(this)
            .setTitle(R.string.pref_separator_label)
            .setItems(options, (dialog, which) -> {
                Prefs.save(Prefs.PREFS_NUMBER_SEPARATOR, values[which]);
                updateUI();
            }).show();
    }

    private void showThemeDialog() {
        String[] options = {
            getString(R.string.pref_theme_system),
            getString(R.string.pref_theme_light),
            getString(R.string.pref_theme_dark)
        };
        int[] values = {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
        };

        new AlertDialog.Builder(this)
            .setTitle(R.string.pref_theme_label)
            .setItems(options, (dialog, which) -> {
                Prefs.save(Prefs.PREFS_THEME, values[which]);
                AppCompatDelegate.setDefaultNightMode(values[which]);
                updateUI();
            }).show();
    }

    private String getCurrencySummary() {
        String val = Prefs.getString(Prefs.PREFS_CURRENCY, Prefs.DEFAULT_CURRENCY);
        if (val.isEmpty()) return getString(R.string.pref_currency_none);
        switch (val) {
            case "$": return getString(R.string.pref_currency_usd);
            case "€": return getString(R.string.pref_currency_eur);
            case "£": return getString(R.string.pref_currency_gbp);
            case "¥": return getString(R.string.pref_currency_jpy);
            default: return getString(R.string.pref_currency_inr);
        }
    }

    private String getFormatSummary() {
        String val = Prefs.getString(Prefs.PREFS_NUMBER_FORMAT, Prefs.DEFAULT_NUMBER_FORMAT);
        switch (val) {
            case "INT": return getString(R.string.pref_format_int);
            case "NONE": return getString(R.string.pref_format_none);
            default: return getString(R.string.pref_format_in);
        }
    }

    private String getSeparatorSummary() {
        String val = Prefs.getString(Prefs.PREFS_NUMBER_SEPARATOR, Prefs.DEFAULT_NUMBER_SEPARATOR);
        return ",".equals(val) ? getString(R.string.pref_sep_comma) : getString(R.string.pref_sep_space);
    }

    private String getThemeSummary() {
        int val = Prefs.getInt(Prefs.PREFS_THEME, Prefs.DEFAULT_THEME);
        if (val == AppCompatDelegate.MODE_NIGHT_NO) return getString(R.string.pref_theme_light);
        if (val == AppCompatDelegate.MODE_NIGHT_YES) return getString(R.string.pref_theme_dark);
        return getString(R.string.pref_theme_system);
    }

    private void confirmClearAllData() {
        int a = (int) (Math.random() * 9) + 1;
        int b = (int) (Math.random() * 9) + 1;
        int answer = a + b;

        int dp = (int) getResources().getDisplayMetrics().density;

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(24 * dp, 8 * dp, 24 * dp, 8 * dp);

        android.widget.TextView label = new android.widget.TextView(this);
        label.setText("To confirm, solve: " + a + " + " + b + " = ?");
        label.setTextSize(13f);
        label.setTextColor(0xFF888888);
        label.setPadding(0, 0, 0, 6 * dp);
        container.addView(label);

        android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Your answer");
        input.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
        input.setTextSize(18f);
        container.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.clear_all_data_confirm_title)
                .setMessage(R.string.clear_all_data_confirm_msg)
                .setView(container)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String entered = input.getText().toString().trim();
            if (entered.isEmpty()) {
                input.setError("Enter the answer");
                return;
            }
            try {
                if (Integer.parseInt(entered) != answer) {
                    input.setError("Wrong answer, try again");
                    return;
                }
            } catch (NumberFormatException e) {
                input.setError("Invalid number");
                return;
            }
            dialog.dismiss();
            performClearAllData();
        }));

        dialog.show();
    }

    private void performClearAllData() {
        executorService.execute(() -> {
            try (Realm realm = Realm.getDefaultInstance()) {
                realm.executeTransaction(r -> r.deleteAll());
            } catch (Exception e) {
                Log.e("PrefsActivity", "Clear local data error", e);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, R.string.clear_all_data_success, Toast.LENGTH_SHORT).show();
            });
        });
    }
}
