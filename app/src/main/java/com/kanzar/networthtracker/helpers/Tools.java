package com.kanzar.networthtracker.helpers;

import android.graphics.Color;
import com.kanzar.networthtracker.R;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;

public final class Tools {

    private Tools() {}

    public static int getTextChangeColor(double valueChange) {
        if (valueChange > 0) return R.color.positive;
        if (valueChange < 0) return R.color.negative;
        return R.color.text;
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

    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    public static String formatPercent(double price) {
        if (Double.isNaN(price)) return "0%";
        return new DecimalFormat("0.00").format(price) + "%";
    }

    public static String formatAmount(double amount) {
        return formatAmount(amount, false);
    }

    public static String formatAmount(double amount, boolean roundUp) {
        double value = roundUp ? Math.round(amount) : amount;
        String format = Prefs.getString(Prefs.PREFS_NUMBER_FORMAT, "NONE");
        String sep = Prefs.getString(Prefs.PREFS_NUMBER_SEPARATOR, " ");
        String currency = Prefs.getString(Prefs.PREFS_CURRENCY, "");
        String formatted;
        if ("NONE".equals(format)) {
            formatted = new DecimalFormat("0.##").format(value);
        } else {
            String pattern = "IN".equals(format) ? "#,##,##,###.##" : "#,###.##";
            DecimalFormat df = new DecimalFormat(pattern);
            DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
            symbols.setGroupingSeparator(sep.charAt(0));
            df.setDecimalFormatSymbols(symbols);
            formatted = df.format(value);
        }
        return currency.isEmpty() ? formatted : currency + " " + formatted;
    }
}
