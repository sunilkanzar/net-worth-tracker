package com.kanzar.networthtracker.helpers;

import android.graphics.Color;
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
        0xFFa855f7, // purple
        0xFF6366f1, // indigo
        0xFFeab308, // yellow
        0xFFf97316, // orange
        0xFF14b8a6, // teal
        0xFF64748b, // slate
        0xFF84cc16, // lime
        0xFF0ea5e9, // sky
        0xFF94a3b8, // slate-light
    };

    public static int getAssetColor(String name) {
        if (name == null || name.isEmpty()) return ASSET_PALETTE[0];
        int hash = 0;
        for (char c : name.toLowerCase().toCharArray()) hash = hash * 31 + c;
        return ASSET_PALETTE[Math.abs(hash) % ASSET_PALETTE.length];
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

    public static int getAccentColor() {
        String key = Prefs.getString(Prefs.PREFS_ACCENT_COLOR, Prefs.DEFAULT_ACCENT_COLOR);
        switch (key) {
            case "blue": return R.color.blue;
            case "indigo": return R.color.indigo;
            case "violet": return R.color.violet;
            case "rose": return R.color.rose;
            case "amber": return R.color.amber;
            default: return R.color.indigo;
        }
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
        String formatted = formatNumber(amount, roundUp);
        return currency.isEmpty() ? formatted : currency + (formatted.startsWith("-") ? " -" : " ") + formatted.replace("-", "");
    }

    public static String formatCompact(double n) {
        String currency = Prefs.getString(Prefs.PREFS_CURRENCY, Prefs.DEFAULT_CURRENCY);
        double abs = Math.abs(n);
        String formatted;
        DecimalFormat df = new DecimalFormat("#.##");
        if (abs >= 10000000) formatted = df.format(n / 10000000.0) + " Cr";
        else if (abs >= 100000) formatted = df.format(n / 100000.0) + " L";
        else if (abs >= 1000) formatted = df.format(n / 1000.0) + "K";
        else formatted = String.valueOf((int)n);
        
        return currency.isEmpty() ? formatted : currency + " " + formatted;
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
}
