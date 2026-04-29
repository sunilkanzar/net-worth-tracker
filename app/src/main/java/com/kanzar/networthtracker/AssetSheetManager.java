package com.kanzar.networthtracker;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.kanzar.networthtracker.databinding.ActivityMainBinding;
import com.kanzar.networthtracker.databinding.DialogCustomRangeBinding;
import com.kanzar.networthtracker.databinding.LayoutNewAssetSheetBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.models.AssetFields;
import com.kanzar.networthtracker.statistics.Events;
import com.kanzar.networthtracker.statistics.events.AssetAdded;
import com.kanzar.networthtracker.statistics.events.AssetDeleted;

import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.realm.Case;
import io.realm.Realm;

public class AssetSheetManager {

    public interface Listener {
        void onDataChanged();
        Month getCurrentMonth();
    }

    private final MainActivity activity;
    private final LayoutNewAssetSheetBinding sheetBinding;
    private final BottomSheetBehavior<View> behavior;
    private final Listener listener;
    
    private boolean updatingForm = false;
    private float currentSlideOffset = 0f;
    private int sheetRangeMonths = 12;
    private String lastSheetRangeLabel = "1Y";
    private Month sheetCustomStartMonth = null;
    private Month sheetCustomEndMonth = null;

    public AssetSheetManager(MainActivity activity, ActivityMainBinding binding, Listener listener) {
        this.activity = activity;
        this.sheetBinding = binding.newAssetSheet;
        this.listener = listener;
        this.behavior = BottomSheetBehavior.from(sheetBinding.newAssetLayout);
        
        setupBottomSheet();
        setupListeners();
    }

    private void applyAccentColors() {
        int accentColor = ContextCompat.getColor(activity, Tools.getAccentColor());
        
        sheetBinding.sheetStepLabel.setTextColor(accentColor);
        sheetBinding.btnValueReset.setTextColor(accentColor);
        
        // Also update segmented control if already initialized
        String signText = sheetBinding.newAssetChange.getText().toString();
        updateSign(!signText.startsWith("-"));

        syncTypeAndColor();
    }

    public void clearFocus() {
        sheetBinding.newAssetName.clearFocus();
        sheetBinding.newAssetValue.clearFocus();
        sheetBinding.newAssetNote.clearFocus();
    }

    private void setupBottomSheet() {
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    clearFocus();
                    sheetBinding.sheetScrollView.scrollTo(0, 0);
                    activity.onAssetViewClosed();
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    activity.showScrim(1f);
                    currentSlideOffset = 1f;
                    sheetBinding.sheetButtonsContainer.setTranslationY(0);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    currentSlideOffset = 0f;
                    updateFooterSticky(0f);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                currentSlideOffset = slideOffset;
                float alpha = (slideOffset + 1f);
                activity.showScrim(Math.min(1f, Math.max(0, alpha)));
                updateFooterSticky(slideOffset);
            }
        });

        sheetBinding.newAssetLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom != oldBottom || top != oldTop) {
                if (behavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
                    updateFooterSticky(currentSlideOffset);
                }
            }
        });
    }

    private void updateFooterSticky(float slideOffset) {
        if (Float.isNaN(slideOffset)) return;
        View sheet = sheetBinding.newAssetLayout;
        View footer = sheetBinding.sheetButtonsContainer;
        int peek = behavior.getPeekHeight();

        View frameLayout = (View) sheetBinding.sheetScrollView.getParent();
        int contentHeight = frameLayout.getHeight();

        if (contentHeight <= 0 || sheet.getHeight() <= 0) return;

        // visibleSheetHeight is how much of the sheet is actually visible on screen
        int visibleSheetHeight = (int) (peek + slideOffset * (sheet.getHeight() - peek));
        visibleSheetHeight = Math.min(visibleSheetHeight, sheet.getHeight());

        int visibleContentAreaHeight = visibleSheetHeight - frameLayout.getTop();

        if (contentHeight <= visibleContentAreaHeight || slideOffset >= 1f) {
            footer.setTranslationY(0);
        } else {
            footer.setTranslationY(Math.min(0, -(contentHeight - visibleContentAreaHeight)));
        }
    }

    private void setupListeners() {
        sheetBinding.newAssetName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) {
                    String[] names = getAssetNames(s.toString());
                    sheetBinding.newAssetName.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, names));
                }
                sheetBinding.assetInitial.setText(s.length() > 0 ? s.toString().substring(0, 1).toUpperCase() : "+");
                if (s.length() > 0) {
                    syncTypeAndColor();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        sheetBinding.newAssetChange.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updatingForm) return;
                String input = s.toString();
                if (input.equals("-")) { updateSign(false); return; }
                double change = toDouble(sheetBinding.newAssetChange);
                if (input.startsWith("-") || change < 0) updateSign(false);
                else if (!input.isEmpty() && change > 0) updateSign(true);

                Asset previous = getNewAsset().getPrevious();
                double prevValue = (previous != null) ? previous.getValue() : 0.0;
                BigDecimal newValue = BigDecimal.valueOf(prevValue).add(BigDecimal.valueOf(change));
                
                if (toDouble(sheetBinding.newAssetValue) != newValue.doubleValue()) {
                    updatingForm = true;
                    String valStr = formatStringValue(newValue.doubleValue());
                    sheetBinding.newAssetValue.setText(valStr);
                    sheetBinding.newAssetValue.setSelection(valStr.length());
                    updatingForm = false;
                    syncTypeAndColor();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        sheetBinding.newAssetValue.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updatingForm) return;
                syncTypeAndColor();

                double val = toDouble(sheetBinding.newAssetValue);
                Asset previous = getNewAsset().getPrevious();
                double prevValue = (previous != null) ? previous.getValue() : 0.0;
                BigDecimal change = BigDecimal.valueOf(val).subtract(BigDecimal.valueOf(prevValue));
                
                if (toDouble(sheetBinding.newAssetChange) != change.doubleValue()) {
                    updatingForm = true;
                    String changeStr = formatStringValue(change.doubleValue());
                    sheetBinding.newAssetChange.setText(changeStr);
                    sheetBinding.newAssetChange.setSelection(changeStr.length());
                    updateSign(change.doubleValue() >= 0);
                    updatingForm = false;
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        sheetBinding.newAssetNote.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                sheetBinding.newAssetNote.postDelayed(() -> {
                    if (sheetBinding.newAssetNote.hasFocus()) {
                        int scrollTo = sheetBinding.layoutNoteArea.getTop();
                        sheetBinding.sheetScrollView.smoothScrollTo(0, scrollTo);
                    }
                }, 200);
            }
        });

        sheetBinding.btnPlus.setOnClickListener(v -> {
            updateSign(true);
            String s = sheetBinding.newAssetChange.getText().toString();
            if (s.startsWith("-")) {
                sheetBinding.newAssetChange.setText(s.substring(1));
                sheetBinding.newAssetChange.setSelection(sheetBinding.newAssetChange.getText().length());
            }
        });

        sheetBinding.btnMinus.setOnClickListener(v -> {
            updateSign(false);
            String s = sheetBinding.newAssetChange.getText().toString();
            if (!s.startsWith("-")) {
                sheetBinding.newAssetChange.setText("-" + s);
                sheetBinding.newAssetChange.setSelection(sheetBinding.newAssetChange.getText().length());
            }
        });

        sheetBinding.btnValueReset.setOnClickListener(v -> {
            updatingForm = true;
            sheetBinding.newAssetChange.setText("0");
            sheetBinding.newAssetChange.setSelection(1);
            updateSign(true);
            Asset previous = getNewAsset().getPrevious();
            double prevValue = (previous != null) ? previous.getValue() : 0.0;
            String valStr = formatStringValue(prevValue);
            sheetBinding.newAssetValue.setText(valStr);
            sheetBinding.newAssetValue.setSelection(valStr.length());
            updatingForm = false;
        });

        sheetBinding.btnSaveAsset.setOnClickListener(v -> saveAsset());

        sheetBinding.btnTypeAsset.setOnClickListener(v -> {
            updateTypeToggle(true);
            String s = sheetBinding.newAssetValue.getText().toString();
            if (s.startsWith("-")) {
                sheetBinding.newAssetValue.setText(s.substring(1));
                sheetBinding.newAssetValue.setSelection(sheetBinding.newAssetValue.getText().length());
            }
        });

        sheetBinding.btnTypeLiability.setOnClickListener(v -> {
            updateTypeToggle(false);
            String s = sheetBinding.newAssetValue.getText().toString();
            if (!s.startsWith("-") && !s.isEmpty() && !s.equals("0")) {
                sheetBinding.newAssetValue.setText("-" + s);
                sheetBinding.newAssetValue.setSelection(sheetBinding.newAssetValue.getText().length());
            } else if (s.isEmpty() || s.equals("0")) {
                sheetBinding.newAssetValue.setText("-");
                sheetBinding.newAssetValue.setSelection(1);
            }
        });

        sheetBinding.btnDeleteAsset.setOnClickListener(v -> {
            String name = sheetBinding.newAssetName.getText().toString();
            if (name.isEmpty()) return;
            new AlertDialog.Builder(activity)
                .setTitle(R.string.menu_delete)
                .setMessage(activity.getString(R.string.delete_confirm, name))
                .setPositiveButton(R.string.backup_share_yes, (dialog, which) -> {
                    try (Realm realm = Realm.getDefaultInstance()) {
                        realm.executeTransaction(r -> {
                            Asset toDelete = r.where(Asset.class)
                                    .equalTo(AssetFields.MONTH, listener.getCurrentMonth().getMonth())
                                    .equalTo(AssetFields.YEAR, listener.getCurrentMonth().getYear())
                                    .equalTo(AssetFields.NAME, name).findFirst();
                            if (toDelete != null) {
                                new Events().send(new AssetDeleted(toDelete));
                                toDelete.deleteFromRealm();
                            }
                        });
                    }
                    listener.onDataChanged();
                    close();
                })
                .setNegativeButton(R.string.backup_share_no, null).show();
        });
    }

    public void open(@Nullable String name, @Nullable Double value) {
        updatingForm = true;
        applyAccentColors();
        Month m = listener.getCurrentMonth();
        boolean isNew = name == null;

        String currency = Prefs.getString(Prefs.PREFS_CURRENCY, Prefs.DEFAULT_CURRENCY);
        sheetBinding.currencySymbolValue.setText(currency);
        sheetBinding.currencySymbolChange.setText(currency);

        sheetBinding.sheetStepLabel.setText(isNew ? activity.getString(R.string.sheet_new_entry) : "Edit · " + m.toString());
        sheetBinding.sheetTitle.setText(isNew ? activity.getString(R.string.sheet_add_title) : name);
        sheetBinding.layoutAssetName.setVisibility(isNew ? View.VISIBLE : View.GONE);
        sheetBinding.btnDeleteAsset.setVisibility(isNew ? View.GONE : View.VISIBLE);
        sheetBinding.layoutTrend.setVisibility(isNew ? View.GONE : View.VISIBLE);
        sheetBinding.assetInitialCard.setVisibility(View.VISIBLE);
        sheetBinding.btnValueReset.setVisibility(isNew ? View.GONE : View.VISIBLE);

        // Adjust Save button margin based on Delete button visibility
        LinearLayout.LayoutParams saveParams = (LinearLayout.LayoutParams) sheetBinding.btnSaveAsset.getLayoutParams();
        saveParams.setMarginStart(isNew ? 0 : (int)(14 * activity.getResources().getDisplayMetrics().density));
        sheetBinding.btnSaveAsset.setLayoutParams(saveParams);

        if (!isNew) initSheetChart(name);

        sheetBinding.newAssetName.setText(name != null ? name : "");
        if (name != null) sheetBinding.newAssetName.setSelection(name.length());
        
        sheetBinding.newAssetValue.setText(value != null ? formatStringValue(value) : "");
        if (value != null) {
            String valStr = formatStringValue(value);
            sheetBinding.newAssetValue.setSelection(valStr.length());
        }
        
        String note = "";
        if (name != null && !name.isEmpty()) {
            try (Realm realm = Realm.getDefaultInstance()) {
                Asset a = realm.where(Asset.class)
                        .equalTo(AssetFields.MONTH, m.getMonth())
                        .equalTo(AssetFields.YEAR, m.getYear())
                        .equalTo(AssetFields.NAME, name).findFirst();
                if (a != null) note = a.getNote();
            }
            sheetBinding.assetInitial.setText(name.substring(0, 1).toUpperCase());
            Asset temp = new Asset(name, value, m.getMonth(), m.getYear());
            Asset prev = temp.getPrevious();
            double change = value - (prev != null ? prev.getValue() : 0.0);
            String changeStr = formatStringValue(change);
            sheetBinding.newAssetChange.setText(changeStr);
            sheetBinding.newAssetChange.setSelection(changeStr.length());
            updateSign(change >= 0);
        } else {
            sheetBinding.assetInitial.setText("+");
            sheetBinding.newAssetChange.setText("");
            updateSign(true);
        }
        syncTypeAndColor();
        sheetBinding.newAssetNote.setText(note != null ? note : "");
        if (note != null) sheetBinding.newAssetNote.setSelection(note.length());
        updatingForm = false;
        
        sheetBinding.newAssetLayout.post(() -> {
            int peek = sheetBinding.sheetHandle.getHeight() + sheetBinding.sheetHeader.getHeight() +
                       sheetBinding.layoutEditForm.getHeight() + sheetBinding.sheetButtonsContainer.getHeight() +
                       (int)(28 * activity.getResources().getDisplayMetrics().density);

            behavior.setSkipCollapsed(false);
            behavior.setPeekHeight(peek);
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            
            if (isNew) {
                sheetBinding.newAssetName.requestFocus();
            } else {
                sheetBinding.newAssetValue.requestFocus();
            }
            
            updateFooterSticky(0f);
            sheetBinding.sheetScrollView.scrollTo(0, 0);
        });
    }

    private void hideKeyboard() {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void close() {
        hideKeyboard();
        clearFocus();
        sheetBinding.sheetScrollView.scrollTo(0, 0);
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public boolean isVisible() {
        return behavior.getState() != BottomSheetBehavior.STATE_HIDDEN;
    }

    private void saveAsset() {
        String name = sheetBinding.newAssetName.getText().toString();
        if (name.isEmpty()) { sheetBinding.newAssetName.setError(activity.getString(R.string.new_asset_name_empty)); return; }
        if (sheetBinding.newAssetValue.getText().toString().isEmpty()) { sheetBinding.newAssetValue.setError(activity.getString(R.string.new_asset_value_empty)); return; }
        
        Asset asset = getNewAsset();
        new Events().send(new AssetAdded(asset));
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> r.copyToRealmOrUpdate(asset));
        }
        listener.onDataChanged();
        close();
        activity.checkAutoBackup();
    }

    private Asset getNewAsset() {
        return new Asset(sheetBinding.newAssetName.getText().toString(), toDouble(sheetBinding.newAssetValue),
                listener.getCurrentMonth().getMonth(), listener.getCurrentMonth().getYear(),
                sheetBinding.newAssetNote.getText().toString());
    }

    private void initSheetChart(String name) {
        sheetRangeMonths = 12; lastSheetRangeLabel = "1Y";
        sheetCustomStartMonth = null; sheetCustomEndMonth = null;

        List<Month> allMonths = new ArrayList<>();
        List<Double> assetValues = new ArrayList<>();
        try (Realm realm = Realm.getDefaultInstance()) {
            Month first = new Month().getFirst();
            Month last = new Month().getLast();
            Month curr = first;
            while (curr != null) {
                allMonths.add(curr);
                Asset a = realm.where(Asset.class).equalTo(AssetFields.MONTH, curr.getMonth()).equalTo(AssetFields.YEAR, curr.getYear()).equalTo(AssetFields.NAME, name).findFirst();
                assetValues.add(a != null ? a.getValue() : 0.0);
                if (curr.getMonth() == last.getMonth() && curr.getYear() == last.getYear()) break;
                Month next = new Month(curr.getMonth(), curr.getYear());
                next.next();
                curr = next;
            }
        }

        String[] rangeOptions = {"1Y", "2Y", "3Y", "5Y", "All", "Custom"};
        sheetBinding.sheetRangeDropdown.setAdapter(new ArrayAdapter<>(activity, R.layout.item_dropdown, rangeOptions));
        sheetBinding.sheetRangeDropdown.setText("1Y", false);
        sheetBinding.sheetRangeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String sel = rangeOptions[position];
            if (sel.equals("Custom")) { showCustomRangeDialog(name, allMonths, assetValues); return; }
            lastSheetRangeLabel = sel;
            sheetCustomStartMonth = null; sheetCustomEndMonth = null;
            switch (sel) {
                case "1Y": sheetRangeMonths = 12; break;
                case "2Y": sheetRangeMonths = 24; break;
                case "3Y": sheetRangeMonths = 36; break;
                case "5Y": sheetRangeMonths = 60; break;
                case "All": sheetRangeMonths = -1; break;
            }
            updateChartData(name, allMonths, assetValues);
        });

        setupChartStyle(sheetBinding.sheetChart);
        updateChartData(name, allMonths, assetValues);
    }

    private void setupChartStyle(LineChart chart) {
        chart.setTouchEnabled(true);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setHighlightPerDragEnabled(false);
        chart.setRenderer(new com.kanzar.networthtracker.views.SelectionHighlightRenderer(chart, chart.getAnimator(), chart.getViewPortHandler(), ContextCompat.getColor(activity, R.color.sheet_bg)));

        final boolean[] isLongPressed = {false};
        final GestureDetector gd = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                isLongPressed[0] = true;
                chart.getParent().requestDisallowInterceptTouchEvent(true);
                Highlight h = chart.getHighlightByTouchPoint(e.getX(), e.getY());
                if (h != null) chart.highlightValue(h, true);
            }

            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                Highlight h = chart.getHighlightByTouchPoint(e.getX(), e.getY());
                if (h != null) chart.highlightValue(h, true);
                return true;
            }
        });

        chart.setOnTouchListener((v, event) -> {
            gd.onTouchEvent(event);
            if (isLongPressed[0] && event.getAction() == MotionEvent.ACTION_MOVE) {
                Highlight h = chart.getHighlightByTouchPoint(event.getX(), event.getY());
                if (h != null) chart.highlightValue(h, true);
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                isLongPressed[0] = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                v.performClick();
            }
            return false;
        });

        int labelColor = ContextCompat.getColor(activity, R.color.sheet_text_muted);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); xAxis.setDrawGridLines(false); xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(labelColor); xAxis.setTextSize(9f); xAxis.setGranularity(1f);

        YAxis axisLeft = chart.getAxisLeft();
        axisLeft.setDrawAxisLine(false); axisLeft.setGridColor(ContextCompat.getColor(activity, R.color.sheet_border));
        axisLeft.setTextColor(labelColor); axisLeft.setTextSize(9f); axisLeft.setLabelCount(4);
        axisLeft.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override public String getFormattedValue(float value) { return Tools.formatCompact(value, false); }
        });
    }

    private void updateChartData(String name, List<Month> allMonths, List<Double> assetValues) {
        int dataSize = assetValues.size();
        final int startIdx, lastIdx;
        if (sheetCustomStartMonth != null && sheetCustomEndMonth != null) {
            int s = getMonthIndex(sheetCustomStartMonth, allMonths);
            int l = getMonthIndex(sheetCustomEndMonth, allMonths);
            startIdx = (s == -1) ? 0 : s; lastIdx = (l == -1) ? dataSize - 1 : l;
        } else {
            startIdx = sheetRangeMonths <= 0 ? 0 : Math.max(0, dataSize - sheetRangeMonths); lastIdx = dataSize - 1;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        for (int i = startIdx; i <= lastIdx; i++) {
            entries.add(new Entry(i - startIdx, assetValues.get(i).floatValue()));
            xLabels.add(allMonths.get(i).toStringMMMYY());
        }

        int accent = ContextCompat.getColor(activity, Tools.getAccentColor());
        LineDataSet ds = new LineDataSet(entries, name);
        ds.setColor(accent); ds.setLineWidth(2.5f); ds.setDrawCircles(false); ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER); ds.setDrawFilled(true);
        ds.setFillDrawable(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[]{androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 80), 0}));
        ds.setHighLightColor(accent); ds.setDrawHighlightIndicators(true); ds.setDrawHorizontalHighlightIndicator(false);

        sheetBinding.sheetChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(xLabels));
        sheetBinding.sheetChart.setData(new LineData(ds));
        sheetBinding.sheetChart.animateY(600);
        sheetBinding.sheetChart.invalidate();

        calculateStats(name, allMonths, assetValues, startIdx, lastIdx);

        sheetBinding.sheetChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override public void onValueSelected(Entry e, Highlight h) {
                updateCursorCard(e, startIdx, allMonths, name);
            }
            @Override public void onNothingSelected() {
                sheetBinding.sheetCursorCard.setVisibility(View.GONE);
            }
        });

        if (!entries.isEmpty()) {
            Highlight last = new Highlight(entries.get(entries.size() - 1).getX(), entries.get(entries.size() - 1).getY(), 0);
            sheetBinding.sheetChart.highlightValue(last, true);
        }
    }

    private void updateCursorCard(Entry e, int startIdx, List<Month> allMonths, String name) {
        int idx = (int) e.getX() + startIdx;
        if (idx >= 0 && idx < allMonths.size()) {
            Month m = allMonths.get(idx);
            sheetBinding.sheetCursorCard.setVisibility(View.VISIBLE);
            sheetBinding.sheetCursorDate.setText(m.toStringMMMYY());
            sheetBinding.sheetCursorValue.setText(Tools.formatAmount(e.getY()));

            double change = 0, percent = 0;
            String note = null;
            try (Realm realm = Realm.getDefaultInstance()) {
                Asset asset = realm.where(Asset.class).equalTo(AssetFields.MONTH, m.getMonth()).equalTo(AssetFields.YEAR, m.getYear()).equalTo(AssetFields.NAME, name).findFirst();
                if (asset != null) {
                    note = asset.getNote();
                    Month prev = m.getPreviousMonth(realm);
                    Asset prevAsset = realm.where(Asset.class).equalTo(AssetFields.MONTH, prev.getMonth()).equalTo(AssetFields.YEAR, prev.getYear()).equalTo(AssetFields.NAME, name).findFirst();
                    if (prevAsset != null) {
                        change = asset.getValue() - prevAsset.getValue();
                        percent = Tools.getPercent(prevAsset.getValue(), asset.getValue());
                    }
                }
            }
            
            if (note != null && !note.isEmpty()) {
                sheetBinding.sheetCursorNote.setVisibility(View.VISIBLE);
                sheetBinding.sheetCursorNote.setText(note);
            } else {
                sheetBinding.sheetCursorNote.setVisibility(View.GONE);
            }
            
            String changeSign = change >= 0 ? "+" : "";
            String pctStr = String.format(java.util.Locale.US, "%.1f%%", Math.abs(percent));
            sheetBinding.sheetCursorChange.setText(String.format("%s%s (%s)", changeSign, Tools.formatAmount(change), pctStr));
            sheetBinding.sheetCursorChange.setTextColor(ContextCompat.getColor(activity, change >= 0 ? R.color.positive : R.color.negative));
            sheetBinding.sheetCursorColor.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(activity, Tools.getAccentColor())));
        }
    }

    private void calculateStats(String name, List<Month> allMonths, List<Double> assetValues, int startIdx, int lastIdx) {
        double sum = 0; List<Double> pcts = new ArrayList<>(); List<Double> periodValues = new ArrayList<>();
        try (Realm realm = Realm.getDefaultInstance()) {
            for (int i = startIdx; i <= lastIdx; i++) {
                double val = assetValues.get(i); periodValues.add(val); sum += val;
                Month m = allMonths.get(i); Month prev = m.getPreviousMonth(realm);
                Asset prevAsset = realm.where(Asset.class).equalTo(AssetFields.MONTH, prev.getMonth()).equalTo(AssetFields.YEAR, prev.getYear()).equalTo(AssetFields.NAME, name).findFirst();
                if (prevAsset != null) pcts.add(Tools.getPercent(prevAsset.getValue(), val));
            }
        }
        int count = periodValues.size();
        if (count > 0) {
            sheetBinding.sheetStatAvg.setText(Tools.formatAmount(sum / count));
            double maxPct = pcts.isEmpty() ? 0 : java.util.Collections.max(pcts);
            sheetBinding.sheetStatBest.setText(String.format("%s%.1f%%", maxPct >= 0 ? "+" : "", maxPct));
            sheetBinding.sheetStatBest.setTextColor(ContextCompat.getColor(activity, maxPct >= 0 ? R.color.positive : R.color.negative));
            double mean = sum / count; double temp = 0;
            for (double v : periodValues) temp += (v - mean) * (v - mean);
            double volatility = (mean != 0) ? (Math.sqrt(temp / count) / mean) : 0;
            String volText = volatility < 0.05 ? "Low" : (volatility < 0.15 ? "Moderate" : "High");
            sheetBinding.sheetStatVol.setText(volText);
            sheetBinding.sheetStatVol.setTextColor(ContextCompat.getColor(activity, volatility < 0.05 ? R.color.positive : (volatility < 0.15 ? R.color.amber : R.color.negative)));
            double startVal = periodValues.get(0); double currentVal = periodValues.get(count - 1);
            if (count > 1 && startVal > 0 && currentVal > 0) {
                double cagr = (Math.pow(currentVal / startVal, 1.0 / (count / 12.0)) - 1) * 100;
                sheetBinding.sheetStatCAGR.setText(String.format("%.1f%%", cagr));
            } else sheetBinding.sheetStatCAGR.setText("N/A");
        }
    }

    private void showCustomRangeDialog(String name, List<Month> allMonths, List<Double> assetValues) {
        DialogCustomRangeBinding dialogBinding = DialogCustomRangeBinding.inflate(activity.getLayoutInflater());
        Month first = new Month().getFirst(); Month last = new Month().getLast();
        String[] months = new java.text.DateFormatSymbols().getShortMonths();
        setupNumberPicker(dialogBinding.startMonth, 1, 12, months); setupNumberPicker(dialogBinding.startYear, first.getYear(), last.getYear(), null);
        setupNumberPicker(dialogBinding.endMonth, 1, 12, months); setupNumberPicker(dialogBinding.endYear, first.getYear(), last.getYear(), null);
        dialogBinding.startMonth.setValue(first.getMonth()); dialogBinding.startYear.setValue(first.getYear());
        dialogBinding.endMonth.setValue(last.getMonth()); dialogBinding.endYear.setValue(last.getYear());

        new AlertDialog.Builder(activity).setTitle("Select Range").setView(dialogBinding.getRoot())
            .setPositiveButton("Apply", (d, which) -> {
                lastSheetRangeLabel = "Custom";
                sheetCustomStartMonth = new Month(dialogBinding.startMonth.getValue(), dialogBinding.startYear.getValue());
                sheetCustomEndMonth = new Month(dialogBinding.endMonth.getValue(), dialogBinding.endYear.getValue());
                updateChartData(name, allMonths, assetValues);
            })
            .setNegativeButton("Cancel", (d, which) -> sheetBinding.sheetRangeDropdown.setText(lastSheetRangeLabel, false))
            .setOnCancelListener(d -> sheetBinding.sheetRangeDropdown.setText(lastSheetRangeLabel, false))
            .show();
    }

    private void setupNumberPicker(NumberPicker picker, int min, int max, String[] displayed) {
        picker.setMinValue(min); picker.setMaxValue(max);
        if (displayed != null) picker.setDisplayedValues(displayed);
    }

    private int getMonthIndex(Month m, List<Month> list) {
        for (int i = 0; i < list.size(); i++) {
            Month am = list.get(i);
            if (am.getMonth() == m.getMonth() && am.getYear() == m.getYear()) return i;
        }
        return -1;
    }

    private void updateSign(boolean isPlus) {
        int accent = ContextCompat.getColor(activity, Tools.getAccentColor());
        int bgElev = ContextCompat.getColor(activity, R.color.sheet_input_bg);
        int textDim = ContextCompat.getColor(activity, R.color.sheet_text_dim);
        sheetBinding.btnPlus.setCardBackgroundColor(ColorStateList.valueOf(isPlus ? bgElev : Color.TRANSPARENT));
        sheetBinding.btnPlus.setCardElevation(isPlus ? 2f * activity.getResources().getDisplayMetrics().density : 0f);
        sheetBinding.txtPlus.setTextColor(isPlus ? accent : textDim);
        sheetBinding.btnMinus.setCardBackgroundColor(ColorStateList.valueOf(!isPlus ? bgElev : Color.TRANSPARENT));
        sheetBinding.btnMinus.setCardElevation(!isPlus ? 2f * activity.getResources().getDisplayMetrics().density : 0f);
        sheetBinding.txtMinus.setTextColor(!isPlus ? accent : textDim);
    }

    private void updateTypeToggle(boolean isAsset) {
        int accent = ContextCompat.getColor(activity, Tools.getAccentColor());
        int bgElev = ContextCompat.getColor(activity, R.color.sheet_input_bg);
        int textDim = ContextCompat.getColor(activity, R.color.sheet_text_dim);
        float density = activity.getResources().getDisplayMetrics().density;

        sheetBinding.btnTypeAsset.setCardBackgroundColor(ColorStateList.valueOf(isAsset ? bgElev : Color.TRANSPARENT));
        sheetBinding.btnTypeAsset.setCardElevation(isAsset ? 2f * density : 0f);
        sheetBinding.txtTypeAsset.setTextColor(isAsset ? accent : textDim);

        sheetBinding.btnTypeLiability.setCardBackgroundColor(ColorStateList.valueOf(!isAsset ? bgElev : Color.TRANSPARENT));
        sheetBinding.btnTypeLiability.setCardElevation(!isAsset ? 2f * density : 0f);
        sheetBinding.txtTypeLiability.setTextColor(!isAsset ? accent : textDim);
    }

    private void syncTypeAndColor() {
        String valText = sheetBinding.newAssetValue.getText().toString();
        double value = toDouble(sheetBinding.newAssetValue);
        boolean isLiability = valText.startsWith("-") || value < 0;
        boolean isAsset = !isLiability;

        updateTypeToggle(isAsset);
        sheetBinding.labelAssetName.setText(isAsset ? R.string.label_asset_name : R.string.label_liability_name);
        sheetBinding.newAssetName.setHint(isAsset ? R.string.new_asset_name_hint : R.string.new_liability_name_hint);
        
        String name = sheetBinding.newAssetName.getText().toString();
        int itemColor;
        if (!name.isEmpty()) {
            itemColor = Tools.getAssetColor(name, isLiability);
        } else {
            itemColor = ContextCompat.getColor(activity, Tools.getAccentColor());
        }
        sheetBinding.assetInitial.setTextColor(itemColor);
        sheetBinding.assetInitialCard.setCardBackgroundColor(ColorStateList.valueOf(Tools.adjustAlpha(itemColor, 0.133f)));
    }

    private String[] getAssetNames(String name) {
        try (Realm realm = Realm.getDefaultInstance()) {
            List<Asset> results = realm.where(Asset.class).distinct(AssetFields.NAME).contains(AssetFields.NAME, name, Case.INSENSITIVE).findAll();
            String[] names = new String[results.size()];
            for (int i = 0; i < results.size(); i++) names[i] = results.get(i).getName();
            return names;
        }
    }

    private double toDouble(EditText et) { try { return Double.parseDouble(et.getText().toString()); } catch (Exception e) { return 0.0; } }
    private String formatStringValue(double d) { return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString(); }
}
