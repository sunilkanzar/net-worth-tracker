package com.kanzar.networthtracker.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.NumberPicker;

import androidx.appcompat.app.AlertDialog;

import com.kanzar.networthtracker.databinding.DialogCustomRangeBinding;

public class RangePickerHelper {

    public interface Callback {
        void onRangeSelected(Month start, Month end);
        void onCancel();
    }

    public static void show(Context context, Month initialStart, Month initialEnd, Callback callback) {
        DialogCustomRangeBinding binding = DialogCustomRangeBinding.inflate(LayoutInflater.from(context));
        
        Month first = new Month().getFirst();
        Month last = new Month().getLast();
        String[] months = new java.text.DateFormatSymbols().getShortMonths();
        
        setupMonthPicker(binding.startMonth, months);
        setupYearPicker(binding.startYear, first.getYear(), last.getYear());
        setupMonthPicker(binding.endMonth, months);
        setupYearPicker(binding.endYear, first.getYear(), last.getYear());

        binding.startMonth.setValue(initialStart != null ? initialStart.getMonth() : first.getMonth());
        binding.startYear.setValue(initialStart != null ? initialStart.getYear() : first.getYear());
        binding.endMonth.setValue(initialEnd != null ? initialEnd.getMonth() : last.getMonth());
        binding.endYear.setValue(initialEnd != null ? initialEnd.getYear() : last.getYear());

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Select Range")
                .setView(binding.getRoot())
                .setPositiveButton("Apply", (d, which) -> {
                    Month start = new Month(binding.startMonth.getValue(), binding.startYear.getValue());
                    Month end = new Month(binding.endMonth.getValue(), binding.endYear.getValue());
                    callback.onRangeSelected(start, end);
                })
                .setNegativeButton("Cancel", (d, which) -> callback.onCancel())
                .setOnCancelListener(d -> callback.onCancel())
                .create();
        
        Tools.styleDialog(dialog);
        dialog.show();
    }

    private static void setupMonthPicker(NumberPicker picker, String[] months) {
        picker.setMinValue(1);
        picker.setMaxValue(12);
        picker.setDisplayedValues(months);
    }

    private static void setupYearPicker(NumberPicker picker, int minYear, int maxYear) {
        picker.setMinValue(minYear);
        picker.setMaxValue(maxYear);
    }
}
