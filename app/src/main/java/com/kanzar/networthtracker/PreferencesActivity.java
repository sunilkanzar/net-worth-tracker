package com.kanzar.networthtracker;

import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.kanzar.networthtracker.api.repositories.DriveServiceHelper;
import com.kanzar.networthtracker.databinding.ActivityPreferencesBinding;
import com.kanzar.networthtracker.databinding.DialogRemindersBinding;
import com.kanzar.networthtracker.databinding.ItemPreferenceRowBinding;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.reminders.ReminderManager;

import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.realm.Realm;

public class PreferencesActivity extends AppCompatActivity {

    private ActivityPreferencesBinding binding;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final String[] ACCENT_NAMES = {"Emerald", "Blue", "Indigo", "Violet", "Rose", "Amber"};
    private static final String[] ACCENT_KEYS = {"emerald", "blue", "indigo", "violet", "rose", "amber"};
    private static final int[] ACCENT_COLORS = {
            R.color.emerald, R.color.blue, R.color.indigo, R.color.violet, R.color.rose, R.color.amber
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPreferencesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        setupClickListeners();
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private void setupClickListeners() {
        binding.rowCurrency.getRoot().setOnClickListener(v -> showCurrencyDialog());
        binding.rowFormat.getRoot().setOnClickListener(v -> showFormatDialog());
        binding.rowSeparator.getRoot().setOnClickListener(v -> showSeparatorDialog());
        binding.rowFinancialYear.getRoot().setOnClickListener(v -> showFinancialYearDialog());
        binding.rowTheme.getRoot().setOnClickListener(v -> showThemeDialog());
        binding.rowAccent.getRoot().setOnClickListener(v -> showAccentColorDialog());

        binding.rowBackup.getRoot().setOnClickListener(v -> showAutosaveDialog());
        binding.rowReminders.getRoot().setOnClickListener(v -> showRemindersDialog());

        binding.rowClearAll.getRoot().setOnClickListener(v -> confirmClearAllData());
    }

    private void updateUI() {
        int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        applyAccentColor(accentColor);

        setRowData(binding.rowCurrency, getString(R.string.pref_currency_label), getCurrencySummary());
        setRowData(binding.rowFormat, getString(R.string.pref_format_label), getFormatSummary());
        setRowData(binding.rowSeparator, getString(R.string.pref_separator_label), getSeparatorSummary());
        setRowData(binding.rowFinancialYear, getString(R.string.pref_fy_label), getFinancialYearSummary());

        setRowData(binding.rowTheme, getString(R.string.pref_theme_label), getThemeSummary());

        // Accent Row
        String currentAccent = Prefs.getString(Prefs.PREFS_ACCENT_COLOR, Prefs.DEFAULT_ACCENT_COLOR);
        setRowData(binding.rowAccent, getString(R.string.pref_accent_label), capitalize(currentAccent));
        binding.rowAccent.prefActionIcon.setVisibility(View.GONE);
        binding.rowAccent.prefActionColor.setVisibility(View.VISIBLE);
        binding.rowAccent.prefActionColor.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, Tools.getAccentColor())));

        // Data Rows
        setRowData(binding.rowBackup, getString(R.string.pref_data_autosave), getAutosaveSummary());
        setRowData(binding.rowReminders, getString(R.string.pref_data_reminders), getReminderSummary());
        binding.rowReminders.prefActionIcon.setVisibility(View.VISIBLE);
        binding.rowReminders.prefActionSwitch.setVisibility(View.GONE);

        // Clear All Row
        setRowData(binding.rowClearAll, getString(R.string.pref_clear_all_data), getString(R.string.pref_clear_data_summary));
        int negativeColor = ContextCompat.getColor(this, R.color.negative);
        binding.rowClearAll.prefTitle.setTextColor(negativeColor);
        binding.rowClearAll.prefActionIcon.setImageResource(R.drawable.ic_delete);
        binding.rowClearAll.prefActionIcon.setImageTintList(ColorStateList.valueOf(negativeColor));
    }

    private void setRowData(ItemPreferenceRowBinding rowBinding, String title, String summary) {
        rowBinding.prefTitle.setText(title);
        rowBinding.prefSummary.setText(summary);
    }

    private void applyAccentColor(int color) {
        ViewGroup root = (ViewGroup) binding.getRoot();
        applyAccentToViews(root, color);
    }

    private void applyAccentToViews(ViewGroup group, int color) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof SwitchMaterial) {
                styleSwitch((SwitchMaterial) v, color);
            } else if (v instanceof TextView) {
                TextView tv = (TextView) v;
                if (tv.getLetterSpacing() >= 0.09f) {
                    tv.setTextColor(color);
                }
            } else if (v instanceof ViewGroup) {
                applyAccentToViews((ViewGroup) v, color);
            }
        }
    }

    private void styleSwitch(SwitchMaterial sw, int color) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] thumbColors = new int[]{color, Color.WHITE};
        int[] trackColors = new int[]{Tools.adjustAlpha(color, 0.3f), Color.LTGRAY};
        sw.setThumbTintList(new ColorStateList(states, thumbColors));
        sw.setTrackTintList(new ColorStateList(states, trackColors));
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void showAccentColorDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pref_accent_label)
                .setAdapter(new AccentAdapter(), (d, which) -> {
                    Prefs.save(Prefs.PREFS_ACCENT_COLOR, ACCENT_KEYS[which]);
                    updateUI();
                    recreate();
                })
                .create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    private class AccentAdapter extends BaseAdapter {
        @Override
        public int getCount() { return ACCENT_NAMES.length; }
        @Override
        public Object getItem(int position) { return ACCENT_NAMES[position]; }
        @Override
        public long getItemId(int position) { return position; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ItemPreferenceRowBinding rowBinding;
            View view = convertView;
            if (view == null) {
                rowBinding = ItemPreferenceRowBinding.inflate(LayoutInflater.from(PreferencesActivity.this), parent, false);
                view = rowBinding.getRoot();
            } else {
                rowBinding = ItemPreferenceRowBinding.bind(view);
            }
            
            view.setClickable(false);
            view.setFocusable(false);

            rowBinding.prefTitle.setText(ACCENT_NAMES[position]);
            rowBinding.prefSummary.setVisibility(View.GONE);
            rowBinding.prefActionIcon.setVisibility(View.GONE);
            rowBinding.prefActionColor.setVisibility(View.VISIBLE);
            rowBinding.prefActionColor.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(PreferencesActivity.this, ACCENT_COLORS[position])));
            
            return view;
        }
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
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.pref_currency_label)
            .setItems(options, (d, which) -> {
                Prefs.save(Prefs.PREFS_CURRENCY, values[which]);
                updateUI();
            }).create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    private void showFormatDialog() {
        String[] options = {
            getString(R.string.pref_format_in),
            getString(R.string.pref_format_int),
            getString(R.string.pref_format_none)
        };
        String[] values = {"IN", "INT", "NONE"};

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.pref_format_label)
            .setItems(options, (d, which) -> {
                Prefs.save(Prefs.PREFS_NUMBER_FORMAT, values[which]);
                updateUI();
            }).create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    private void showSeparatorDialog() {
        String[] options = {
            getString(R.string.pref_sep_space),
            getString(R.string.pref_sep_comma)
        };
        String[] values = {" ", ","};

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.pref_separator_label)
            .setItems(options, (d, which) -> {
                Prefs.save(Prefs.PREFS_NUMBER_SEPARATOR, values[which]);
                updateUI();
            }).create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    private void showFinancialYearDialog() {
        String[] options = {
                getString(R.string.pref_fy_jan),
                getString(R.string.pref_fy_feb),
                getString(R.string.pref_fy_mar),
                getString(R.string.pref_fy_apr),
                getString(R.string.pref_fy_may),
                getString(R.string.pref_fy_jun),
                getString(R.string.pref_fy_jul),
                getString(R.string.pref_fy_aug),
                getString(R.string.pref_fy_sep),
                getString(R.string.pref_fy_oct),
                getString(R.string.pref_fy_nov),
                getString(R.string.pref_fy_dec)
        };

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pref_fy_label)
                .setItems(options, (d, which) -> {
                    Prefs.save(Prefs.PREFS_FY_START_MONTH, which);
                    updateUI();
                }).create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    private String getFinancialYearSummary() {
        int val = Prefs.getInt(Prefs.PREFS_FY_START_MONTH, Prefs.DEFAULT_FY_START_MONTH);
        switch (val) {
            case 0: return getString(R.string.pref_fy_jan);
            case 1: return getString(R.string.pref_fy_feb);
            case 2: return getString(R.string.pref_fy_mar);
            case 3: return getString(R.string.pref_fy_apr);
            case 4: return getString(R.string.pref_fy_may);
            case 5: return getString(R.string.pref_fy_jun);
            case 6: return getString(R.string.pref_fy_jul);
            case 7: return getString(R.string.pref_fy_aug);
            case 8: return getString(R.string.pref_fy_sep);
            case 9: return getString(R.string.pref_fy_oct);
            case 10: return getString(R.string.pref_fy_nov);
            case 11: return getString(R.string.pref_fy_dec);
            default: return getString(R.string.pref_fy_jan);
        }
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

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.pref_theme_label)
            .setItems(options, (d, which) -> {
                Prefs.save(Prefs.PREFS_THEME, values[which]);
                AppCompatDelegate.setDefaultNightMode(values[which]);
                updateUI();
            }).create();
        Tools.styleDialog(dialog);
        dialog.show();
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

    private void showAutosaveDialog() {
        String[] options = {
                getString(R.string.pref_autosave_daily),
                getString(R.string.pref_autosave_weekly),
                getString(R.string.pref_autosave_monthly),
                getString(R.string.pref_autosave_quarterly),
                getString(R.string.pref_autosave_half_yearly),
                getString(R.string.pref_autosave_yearly)
        };
        String[] values = {"daily", "weekly", "monthly", "quarterly", "half_yearly", "yearly"};

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pref_data_autosave)
                .setItems(options, (d, which) -> {
                    Prefs.save(Prefs.PREFS_AUTOSAVE_FREQUENCY, values[which]);
                    updateUI();
                }).create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    private String getAutosaveSummary() {
        String val = Prefs.getString(Prefs.PREFS_AUTOSAVE_FREQUENCY, Prefs.DEFAULT_AUTOSAVE_FREQUENCY);
        String label;
        switch (val) {
            case "weekly": label = getString(R.string.pref_autosave_weekly); break;
            case "monthly": label = getString(R.string.pref_autosave_monthly); break;
            case "quarterly": label = getString(R.string.pref_autosave_quarterly); break;
            case "half_yearly": label = getString(R.string.pref_autosave_half_yearly); break;
            case "yearly": label = getString(R.string.pref_autosave_yearly); break;
            default: label = getString(R.string.pref_autosave_daily); break;
        }
        return label + " (Local CSV)";
    }

    private void showRemindersDialog() {
        DialogRemindersBinding dialogBinding = DialogRemindersBinding.inflate(getLayoutInflater());
        int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        applyAccentToViews(dialogBinding.getRoot(), accentColor);

        boolean enabled = Prefs.getBoolean(Prefs.PREFS_REMINDERS_ENABLED, Prefs.DEFAULT_REMINDERS_ENABLED);
        int day = Prefs.getInt(Prefs.PREFS_REMINDER_DAY, Prefs.DEFAULT_REMINDER_DAY);
        int hour = Prefs.getInt(Prefs.PREFS_REMINDER_HOUR, Prefs.DEFAULT_REMINDER_HOUR);
        int minute = Prefs.getInt(Prefs.PREFS_REMINDER_MINUTE, Prefs.DEFAULT_REMINDER_MINUTE);

        dialogBinding.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                }
            }
        });

        dialogBinding.switchEnabled.setChecked(enabled);

        String[] dayOptions = {
                getString(R.string.pref_reminders_day_start),
                getString(R.string.pref_reminders_day_end)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dayOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.spinnerDay.setAdapter(adapter);
        dialogBinding.spinnerDay.setSelection(day == 0 ? 1 : 0);

        final int[] selectedTime = {hour, minute};
        updateDialogTimeText(dialogBinding.tvTime, selectedTime[0], selectedTime[1]);

        dialogBinding.tvTime.setOnClickListener(v -> {
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(selectedTime[0])
                    .setMinute(selectedTime[1])
                    .setTitleText(R.string.pref_reminders_time_label)
                    .setTheme(Tools.getTimePickerTheme())
                    .build();

            picker.addOnPositiveButtonClickListener(v1 -> {
                selectedTime[0] = picker.getHour();
                selectedTime[1] = picker.getMinute();
                updateDialogTimeText(dialogBinding.tvTime, selectedTime[0], selectedTime[1]);
            });

            picker.show(getSupportFragmentManager(), "reminder_time_picker");
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pref_data_reminders)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    Prefs.save(Prefs.PREFS_REMINDERS_ENABLED, dialogBinding.switchEnabled.isChecked());
                    Prefs.save(Prefs.PREFS_REMINDER_DAY, dialogBinding.spinnerDay.getSelectedItemPosition() == 0 ? 1 : 0);
                    Prefs.save(Prefs.PREFS_REMINDER_HOUR, selectedTime[0]);
                    Prefs.save(Prefs.PREFS_REMINDER_MINUTE, selectedTime[1]);
                    ReminderManager.updateReminder(this);
                    updateUI();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    private void updateDialogTimeText(TextView tv, int hour, int minute) {
        tv.setText(formatTime(hour, minute));
    }

    private String formatTime(int hour, int minute) {
        String ampm = hour >= 12 ? "PM" : "AM";
        int h = hour % 12;
        if (h == 0) h = 12;
        return String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, ampm);
    }

    private String getReminderSummary() {
        boolean enabled = Prefs.getBoolean(Prefs.PREFS_REMINDERS_ENABLED, Prefs.DEFAULT_REMINDERS_ENABLED);
        if (!enabled) return getString(R.string.pref_reminders_disabled);

        int day = Prefs.getInt(Prefs.PREFS_REMINDER_DAY, Prefs.DEFAULT_REMINDER_DAY);
        int hour = Prefs.getInt(Prefs.PREFS_REMINDER_HOUR, Prefs.DEFAULT_REMINDER_HOUR);
        int minute = Prefs.getInt(Prefs.PREFS_REMINDER_MINUTE, Prefs.DEFAULT_REMINDER_MINUTE);

        String dayLabel = day == 1 ? getString(R.string.pref_reminders_day_start) : getString(R.string.pref_reminders_day_end);
        return getString(R.string.pref_reminders_summary_fmt, dayLabel, formatTime(hour, minute));
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

        dialog.setOnShowListener(d -> {
            int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(accentColor);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(accentColor);
            
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
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
            });
        });

        dialog.show();
    }

    private void performClearAllData() {
        if (executorService.isShutdown()) return;
        executorService.execute(() -> {
            try (Realm realm = Realm.getDefaultInstance()) {
                realm.executeTransaction(r -> r.deleteAll());
            } catch (Exception e) {
                Log.e("PrefsActivity", "Clear local data error", e);
            }

            if (Prefs.contains(Prefs.PREFS_TOKEN)) {
                try {
                    AuthorizationRequest authRequest = AuthorizationRequest.builder()
                            .setRequestedScopes(Collections.singletonList(new Scope(DriveScopes.DRIVE_APPDATA)))
                            .build();

                    com.google.android.gms.auth.api.identity.AuthorizationResult authResult = 
                            Tasks.await(Identity.getAuthorizationClient(this).authorize(authRequest));
                    
                    String accessToken = authResult.getAccessToken();
                    if (accessToken != null) {
                        Drive drive = new Drive.Builder(
                                new NetHttpTransport(),
                                new GsonFactory(),
                                request -> request.getHeaders().setAuthorization("Bearer " + accessToken))
                                .setApplicationName("Net Worth Tracker")
                                .build();

                        DriveServiceHelper driveHelper = new DriveServiceHelper(drive);
                        String fileId = Tasks.await(driveHelper.searchFile("assets.json"));
                        if (fileId != null) {
                            Tasks.await(driveHelper.deleteFile(fileId));
                        }
                    }
                } catch (Exception e) {
                    Log.e("PrefsActivity", "Clear Drive data error", e);
                }
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(this, R.string.clear_all_data_success, Toast.LENGTH_SHORT).show();
            });
        });
    }
}
