package com.kanzar.networthtracker.helpers;

import android.graphics.Color;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.kanzar.networthtracker.R;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

public final class Tools {

    private Tools() {}

    public static final int[] ASSET_PALETTE = {
            0xFF3b82f6, // blue
            0xFF22d3ee, // cyan
            0xFF10b981, // emerald
            0xFF6366f1, // indigo
            0xFF14b8a6, // teal
            0xFF84cc16, // lime
            0xFF0ea5e9, // sky
            0xFF34d399, // emerald-light
            0xFF60a5fa, // blue-light
            0xFF818cf8, // indigo-light
    };

    public static final int[] LIABILITY_PALETTE = {
            0xFFef4444, // red
            0xFFf97316, // orange
            0xFFeab308, // yellow
            0xFFf43f5e, // rose
            0xFFfb923c, // orange-light
            0xFFfacc15, // yellow-light
            0xFFec4899, // pink
            0xFFf87171, // red-light
    };

    public static int getAssetColor(String name) {
        return getAssetColor(name, false);
    }

    public static int getAssetColor(String name, boolean isLiability) {
        if (name == null || name.isEmpty()) return isLiability ? LIABILITY_PALETTE[0] : ASSET_PALETTE[0];
        int hash = 0;
        for (char c : name.toLowerCase().toCharArray()) hash = hash * 31 + c;
        int[] palette = isLiability ? LIABILITY_PALETTE : ASSET_PALETTE;
        return palette[Math.abs(hash) % palette.length];
    }

    public static int getTextChangeColor(double valueChange) {
        if (valueChange >= 0) return R.color.positive;
        return R.color.negative;
    }

    public static double getPercent(double previous, double current) {
        if (current == previous) return 0.0;
        if (current >= 0 && previous <= 0) return 100.0;
        if (current <= 0 && previous >= 0) return -100.0;
        
        double percent = (current - previous) / previous * 100.0;
        return (current >= 0) ? percent : percent * -1.0;
    }

    public static long getUnixTime() {
        return Calendar.getInstance().getTimeInMillis() / 1000;
    }

    public static int getAngle(double percent, double max) {
        double factor = 90.0 / max;
        if (percent < 0) {
            int angle = (int) (percent * -factor);
            return Math.min(angle, 90);
        }
        int angle = (int) (percent * factor);
        angle = Math.min(angle, 90);
        return 360 - angle;
    }

    public static int getColor(int angle) {
        if (angle <= 90) {
            int value = Math.max(0, Math.min(255, (255 - (angle * 2)) - 34));
            return Color.rgb(221, value, value);
        }
        if (angle >= 270) {
            int value = Math.max(0, Math.min(255, (255 - ((360 - angle) * 2)) - 34));
            return Color.rgb(value, 221, value);
        }
        return Color.rgb(221, 221, 221);
    }

    public static final String[] ACCENT_KEYS = {"emerald", "blue", "indigo", "violet", "rose", "amber"};
    public static final int[] ACCENT_COLORS = {
            R.color.emerald, R.color.blue, R.color.indigo, R.color.violet, R.color.rose, R.color.amber
    };
    public static final int[] ACCENT_THEMES = {
            R.style.AppTheme_TimePicker_Emerald, R.style.AppTheme_TimePicker_Blue, R.style.AppTheme_TimePicker_Indigo,
            R.style.AppTheme_TimePicker_Violet, R.style.AppTheme_TimePicker_Rose, R.style.AppTheme_TimePicker_Amber
    };

    public static int getAccentColor() {
        String key = Prefs.getString(Prefs.PREFS_ACCENT_COLOR, Prefs.DEFAULT_ACCENT_COLOR);
        for (int i = 0; i < ACCENT_KEYS.length; i++) {
            if (ACCENT_KEYS[i].equals(key)) return ACCENT_COLORS[i];
        }
        return R.color.indigo;
    }

    public static int getTimePickerTheme() {
        String key = Prefs.getString(Prefs.PREFS_ACCENT_COLOR, Prefs.DEFAULT_ACCENT_COLOR);
        for (int i = 0; i < ACCENT_KEYS.length; i++) {
            if (ACCENT_KEYS[i].equals(key)) return ACCENT_THEMES[i];
        }
        return R.style.AppTheme_TimePicker_Indigo;
    }

    public static void styleDialog(AlertDialog dialog) {
        int accentColor = ContextCompat.getColor(dialog.getContext(), getAccentColor());
        dialog.setOnShowListener(d -> {
            Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (pos != null) pos.setTextColor(accentColor);
            Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (neg != null) neg.setTextColor(accentColor);
            Button neu = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (neu != null) neu.setTextColor(accentColor);
        });
    }

    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    public static String formatPercent(double price) {
        if (Double.isNaN(price)) return "0%";
        return new DecimalFormat("#.##").format(price) + "%";
    }

    public static String formatAmount(double amount) {
        return formatAmount(amount, false);
    }

    public static String formatAmount(double amount, boolean roundUp) {
        String currency = Prefs.getString(Prefs.PREFS_CURRENCY, Prefs.DEFAULT_CURRENCY);
        return prependCurrency(formatNumber(amount, roundUp), currency);
    }

    public static String formatCompact(double n) {
        return formatCompact(n, true);
    }

    public static String formatCompact(double n, boolean includeCurrency) {
        String currency = includeCurrency ? Prefs.getString(Prefs.PREFS_CURRENCY, Prefs.DEFAULT_CURRENCY) : "";
        double abs = Math.abs(n);
        String formatted;
        DecimalFormat df = new DecimalFormat("#.##");
        if (abs >= 10000000) formatted = df.format(n / 10000000.0) + " Cr";
        else if (abs >= 100000) formatted = df.format(n / 100000.0) + " L";
        else if (abs >= 1000) formatted = df.format(n / 1000.0) + "K";
        else formatted = String.valueOf((int)n);
        
        return prependCurrency(formatted, currency);
    }

    private static String prependCurrency(String formatted, String currency) {
        if (currency == null || currency.isEmpty()) return formatted;
        boolean isNegative = formatted.startsWith("-");
        String clean = isNegative ? formatted.substring(1) : formatted;
        return currency + (isNegative ? " -" : " ") + clean;
    }

    public static String formatNumber(double amount, boolean roundUp) {
        double value = roundUp ? Math.round(amount) : amount;
        String format = Prefs.getString(Prefs.PREFS_NUMBER_FORMAT, Prefs.DEFAULT_NUMBER_FORMAT);
        String sep = Prefs.getString(Prefs.PREFS_NUMBER_SEPARATOR, Prefs.DEFAULT_NUMBER_SEPARATOR);
        String formatted;
        if ("NONE".equals(format)) {
            DecimalFormat df = new DecimalFormat("#.##");
            formatted = df.format(value);
        } else {
            String pattern = "IN".equals(format) ? "#,##,##,###.##" : "#,###.##";
            DecimalFormat df = new DecimalFormat(pattern);
            DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
            symbols.setGroupingSeparator(sep.charAt(0));
            df.setDecimalFormatSymbols(symbols);
            formatted = df.format(value);
        }
        return formatted;
    }

    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String getMonthName(int month) {
        if (month < 1 || month > 12) return "";
        return new java.text.DateFormatSymbols().getMonths()[month - 1].toUpperCase();
    }
}
