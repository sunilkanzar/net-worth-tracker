package com.kanzar.networthtracker;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.kanzar.networthtracker.helpers.Prefs;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class PreferencesActivity extends AppCompatActivity {

    private static final double PREVIEW_VALUE = 1234567.89;

    private RadioGroup rgCurrency, rgFormat, rgSeparator;
    private TextView previewAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        rgCurrency = findViewById(R.id.rgCurrency);
        rgFormat = findViewById(R.id.rgFormat);
        rgSeparator = findViewById(R.id.rgSeparator);
        previewAmount = findViewById(R.id.previewAmount);

        loadSavedPrefs();
        updatePreview();

        rgCurrency.setOnCheckedChangeListener((group, checkedId) -> updatePreview());
        rgFormat.setOnCheckedChangeListener((group, checkedId) -> updatePreview());
        rgSeparator.setOnCheckedChangeListener((group, checkedId) -> updatePreview());

        findViewById(R.id.btnSavePrefs).setOnClickListener(v -> savePrefs());
    }

    private void loadSavedPrefs() {
        String currency = Prefs.getString(Prefs.PREFS_CURRENCY, "₹");
        String format = Prefs.getString(Prefs.PREFS_NUMBER_FORMAT, "IN");
        String sep = Prefs.getString(Prefs.PREFS_NUMBER_SEPARATOR, " ");

        switch (currency) {
            case "":   ((RadioButton) findViewById(R.id.rbCurrencyNone)).setChecked(true); break;
            case "$":  ((RadioButton) findViewById(R.id.rbCurrencyUsd)).setChecked(true);  break;
            case "€":  ((RadioButton) findViewById(R.id.rbCurrencyEur)).setChecked(true);  break;
            case "£":  ((RadioButton) findViewById(R.id.rbCurrencyGbp)).setChecked(true);  break;
            case "¥":  ((RadioButton) findViewById(R.id.rbCurrencyJpy)).setChecked(true);  break;
            default:   ((RadioButton) findViewById(R.id.rbCurrencyInr)).setChecked(true);  break;
        }

        switch (format) {
            case "IN":   ((RadioButton) findViewById(R.id.rbFormatIn)).setChecked(true);   break;
            case "INT":  ((RadioButton) findViewById(R.id.rbFormatInt)).setChecked(true);  break;
            default:     ((RadioButton) findViewById(R.id.rbFormatNone)).setChecked(true); break;
        }

        if (",".equals(sep)) {
            ((RadioButton) findViewById(R.id.rbSepComma)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.rbSepSpace)).setChecked(true);
        }
    }

    private void savePrefs() {
        Prefs.save(Prefs.PREFS_CURRENCY, getSelectedCurrency());
        Prefs.save(Prefs.PREFS_NUMBER_FORMAT, getSelectedFormat());
        Prefs.save(Prefs.PREFS_NUMBER_SEPARATOR, getSelectedSeparator());
        Toast.makeText(this, R.string.pref_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void updatePreview() {
        String currency = getSelectedCurrency();
        String format = getSelectedFormat();
        String sep = getSelectedSeparator();
        String formatted;
        if ("NONE".equals(format)) {
            formatted = new DecimalFormat("0.##").format(PREVIEW_VALUE);
        } else {
            String pattern = "IN".equals(format) ? "#,##,##,###.##" : "#,###.##";
            DecimalFormat df = new DecimalFormat(pattern);
            DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
            symbols.setGroupingSeparator(sep.charAt(0));
            df.setDecimalFormatSymbols(symbols);
            formatted = df.format(PREVIEW_VALUE);
        }
        previewAmount.setText(currency.isEmpty() ? formatted : currency + " " + formatted);
    }

    private String getSelectedCurrency() {
        int id = rgCurrency.getCheckedRadioButtonId();
        if (id == R.id.rbCurrencyNone) return "";
        if (id == R.id.rbCurrencyUsd)  return "$";
        if (id == R.id.rbCurrencyEur)  return "€";
        if (id == R.id.rbCurrencyGbp)  return "£";
        if (id == R.id.rbCurrencyJpy)  return "¥";
        return "₹";
    }

    private String getSelectedFormat() {
        int id = rgFormat.getCheckedRadioButtonId();
        if (id == R.id.rbFormatIn)  return "IN";
        if (id == R.id.rbFormatInt) return "INT";
        return "NONE";
    }

    private String getSelectedSeparator() {
        return rgSeparator.getCheckedRadioButtonId() == R.id.rbSepComma ? "," : " ";
    }
}
