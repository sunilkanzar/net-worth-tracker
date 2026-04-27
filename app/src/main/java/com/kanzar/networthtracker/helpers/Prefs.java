package com.kanzar.networthtracker.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.kanzar.networthtracker.CoreApplication;

public final class Prefs {
    public static final String PREFS_AUTO_IMPORT_PERMISSION = "auto_import_permission";
    public static final String PREFS_AUTOSAVE_FREQUENCY = "autosave_frequency";
    public static final String PREFS_LAST_AUTO_BACKUP_TIME = "last_auto_backup_time";
    public static final String PREFS_LAST_SYNC = "last_sync";
    public static final String PREFS_TOKEN = "token";
    public static final String PREFS_USER_EMAIL = "email";
    public static final String PREFS_GOAL_1Y = "goal_1y";
    public static final String PREFS_GOAL_3Y = "goal_3y";
    public static final String PREFS_GOAL_5Y = "goal_5y";
    public static final String PREFS_GOAL_SET_YEAR = "goal_set_year";
    public static final String PREFS_CURRENCY = "currency_symbol";
    public static final String PREFS_NUMBER_FORMAT = "number_format";
    public static final String PREFS_NUMBER_SEPARATOR = "number_separator";
    public static final String PREFS_THEME = "app_theme";
    public static final String PREFS_ACCENT_COLOR = "accent_color";
    public static final String PREFS_SORT_ORDER = "sort_order";
    public static final String PREFS_REMINDERS_ENABLED = "reminders_enabled";
    public static final String PREFS_REMINDER_DAY = "reminder_day";
    public static final String PREFS_REMINDER_HOUR = "reminder_hour";
    public static final String PREFS_REMINDER_MINUTE = "reminder_minute";
    public static final String PREFS_FY_START_MONTH = "fy_start_month";
    private static final String CORE_PREFS = "CORE_PREFS";

    public static final String DEFAULT_CURRENCY = "";
    public static final String DEFAULT_NUMBER_FORMAT = "INT";
    public static final String DEFAULT_NUMBER_SEPARATOR = " ";
    public static final int DEFAULT_THEME = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    public static final String DEFAULT_ACCENT_COLOR = "emerald";
    public static final String DEFAULT_AUTOSAVE_FREQUENCY = "daily";
    public static final boolean DEFAULT_REMINDERS_ENABLED = true;
    public static final int DEFAULT_REMINDER_DAY = 0;
    public static final int DEFAULT_REMINDER_HOUR = 20;
    public static final int DEFAULT_REMINDER_MINUTE = 0;
    public static final int DEFAULT_FY_START_MONTH = 0;

    private Prefs() {}

    private static SharedPreferences getSp() {
        return CoreApplication.getContext().getSharedPreferences(CORE_PREFS, Context.MODE_PRIVATE);
    }

    public static void save(String key, Object value) {
        SharedPreferences.Editor editor = getSp().edit();
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        }
        editor.apply();
    }

    public static void delete(String key) {
        getSp().edit().remove(key).apply();
    }

    public static void clear() {
        getSp().edit().clear().apply();
    }

    public static boolean contains(String key) {
        return getSp().contains(key);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return getSp().getBoolean(key, defaultValue);
    }

    public static String getString(String key, String defaultValue) {
        return getSp().getString(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        return getSp().getInt(key, defaultValue);
    }

    public static float getFloat(String key, float defaultValue) {
        return getSp().getFloat(key, defaultValue);
    }

    public static long getLong(String key, long defaultValue) {
        return getSp().getLong(key, defaultValue);
    }
}
