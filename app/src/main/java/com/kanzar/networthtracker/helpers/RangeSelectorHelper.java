package com.kanzar.networthtracker.helpers;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.kanzar.networthtracker.R;

public class RangeSelectorHelper {

    public static final String[] RANGE_OPTIONS = {"1Y", "2Y", "3Y", "5Y", "All", "Custom"};

    public interface Callback {
        void onRangeSelected(int months, String label);
        void onCustomSelected();
    }

    public static void setup(AutoCompleteTextView dropdown, Callback callback) {
        Context context = dropdown.getContext();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.item_dropdown, RANGE_OPTIONS);
        dropdown.setAdapter(adapter);
        dropdown.setText(RANGE_OPTIONS[0], false);
        dropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selection = RANGE_OPTIONS[position];
            if (selection.equals("Custom")) {
                callback.onCustomSelected();
                return;
            }

            int months;
            switch (selection) {
                case "1Y": months = 12; break;
                case "2Y": months = 24; break;
                case "3Y": months = 36; break;
                case "5Y": months = 60; break;
                default: months = 0; break; // All
            }
            callback.onRangeSelected(months, selection);
        });
    }
}
