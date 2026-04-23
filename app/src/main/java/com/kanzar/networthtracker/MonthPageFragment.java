package com.kanzar.networthtracker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kanzar.networthtracker.adapters.AssetAdapter;
import com.kanzar.networthtracker.databinding.FragmentMonthPageBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.views.MiniBarView;
import com.kanzar.networthtracker.views.PercentView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.realm.Realm;

public class MonthPageFragment extends Fragment implements AssetAdapter.OnItemClickListener {

    public interface Listener {
        void onOpenDrawer();
        void onRequestSync();
        void onEditAsset(Asset asset);
        void onLongPressAsset(Asset asset);
        void onMonthReady(Month month);
        void onDataChanged();
    }

    private static final String ARG_MONTH = "arg_month";
    private static final String ARG_YEAR  = "arg_year";

    private FragmentMonthPageBinding binding;
    private Month month;
    private AssetAdapter assetAdapter;
    private AssetAdapter liabAdapter;
    private Listener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static MonthPageFragment newInstance(int month, int year) {
        MonthPageFragment f = new MonthPageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MONTH, month);
        args.putInt(ARG_YEAR,  year);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (Listener) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int m = requireArguments().getInt(ARG_MONTH);
        int y = requireArguments().getInt(ARG_YEAR);
        month = new Month(m, y);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMonthPageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.menu.setOnClickListener(v -> listener.onOpenDrawer());

        binding.privacyToggle.setOnClickListener(v -> {
            boolean hidden = !Prefs.getBoolean("privacy_mode", false);
            Prefs.save("privacy_mode", hidden);
            listener.onDataChanged();
        });

        binding.copyPrompt.setOnClickListener(v -> {
            Month prevMonth = month.getPreviousMonth();
            copyFromPrevious(prevMonth);
        });

        binding.refreshLayout.setOnRefreshListener(() -> {
            binding.refreshLayout.setRefreshing(false);
            listener.onRequestSync();
        });

        assetAdapter = new AssetAdapter(requireContext(), this);
        binding.assetList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.assetList.setAdapter(assetAdapter);

        liabAdapter = new AssetAdapter(requireContext(), this);
        binding.liabList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.liabList.setAdapter(liabAdapter);

        binding.btnSort.setOnClickListener(v -> showSortDialog());
        binding.btnSortLiab.setOnClickListener(v -> showSortDialog());

        applySavedSortOrder();
        refresh();
    }

    private void applySavedSortOrder() {
        if (assetAdapter == null || liabAdapter == null) return;
        String sortName = Prefs.getString(Prefs.PREFS_SORT_ORDER, AssetAdapter.SortOrder.VALUE_DESC.name());
        AssetAdapter.SortOrder order = AssetAdapter.SortOrder.valueOf(sortName);
        assetAdapter.setSortOrder(order);
        liabAdapter.setSortOrder(order);
    }

    private void showSortDialog() {
        String[] options = {
                getString(R.string.sort_value_desc),
                getString(R.string.sort_value_asc),
                getString(R.string.sort_name_asc),
                getString(R.string.sort_name_desc),
                getString(R.string.sort_change_desc),
                getString(R.string.sort_change_asc),
                getString(R.string.sort_percent_desc),
                getString(R.string.sort_percent_asc)
        };

        AssetAdapter.SortOrder[] orders = {
                AssetAdapter.SortOrder.VALUE_DESC,
                AssetAdapter.SortOrder.VALUE_ASC,
                AssetAdapter.SortOrder.NAME_ASC,
                AssetAdapter.SortOrder.NAME_DESC,
                AssetAdapter.SortOrder.CHANGE_DESC,
                AssetAdapter.SortOrder.CHANGE_ASC,
                AssetAdapter.SortOrder.PERCENT_DESC,
                AssetAdapter.SortOrder.PERCENT_ASC
        };

        String currentSortName = Prefs.getString(Prefs.PREFS_SORT_ORDER, AssetAdapter.SortOrder.VALUE_DESC.name());
        int checkedItem = 0;
        for (int i = 0; i < orders.length; i++) {
            if (orders[i].name().equals(currentSortName)) {
                checkedItem = i;
                break;
            }
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.sort_by)
                .setSingleChoiceItems(options, checkedItem, (d, which) -> {
                    AssetAdapter.SortOrder selectedOrder = orders[which];
                    Prefs.save(Prefs.PREFS_SORT_ORDER, selectedOrder.name());
                    
                    // Update all fragments by notifying data changed
                    listener.onDataChanged();
                    d.dismiss();
                })
                .create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void refresh() {
        refresh(true);
    }

    public void refresh(boolean showShimmer) {
        if (!isAdded() || binding == null) return;

        applySavedSortOrder();

        if (showShimmer) {
            binding.shimmerView.setVisibility(View.VISIBLE);
            binding.shimmerView.startShimmer();
            binding.mainContent.setVisibility(View.GONE);
        }

        executor.execute(() -> {
            try (Realm realm = Realm.getDefaultInstance()) {
                final List<Asset> assets = month.getAssets(realm);
                final boolean noData = realm.where(Asset.class).count() == 0;
                
                final double value = month.getValue(realm);
                final Month prevMonth = month.getPreviousMonth(realm);
                final double prevValue = prevMonth.getValue();

                double totalAssets = 0, totalLiabilities = 0;
                int assetCount = 0, liabilityCount = 0;
                for (Asset a : assets) {
                    // Pre-fetch previous value to avoid UI thread DB access in Adapter
                    Asset prev = a.getPrevious(realm);
                    if (prev != null) {
                        a.setPrevValue(prev.getValue());
                    } else {
                        a.setPrevValue(0.0);
                    }

                    if (!a.isHelper()) {
                        if (a.getValue() >= 0) {
                            totalAssets += a.getValue();
                            assetCount++;
                        } else {
                            totalLiabilities += Math.abs(a.getValue());
                            liabilityCount++;
                        }
                    }
                }

                final int ac = assetCount, lc = liabilityCount;
                final double ta = totalAssets, tl = totalLiabilities;
                final String note = Prefs.getString(CommentActivity.noteKey(month.getMonth(), month.getYear()), "");

                final boolean hasCurrentData = month.hasAssets(realm);
                final boolean hasPreviousData = prevMonth.hasAssets(realm);

                final List<Float> history = new ArrayList<>();
                try {
                    Month hm = new Month(month.getMonth(), month.getYear(), false);
                    hm.calculateValues(realm);
                    history.add(0, (float) hm.getValue());
                    for (int i = 0; i < 7; i++) {
                        hm.previous(realm);
                        history.add(0, (float) hm.getValue());
                    }
                } catch (Exception e) {
                    Log.e("MonthPageFragment", "Error calculating history", e);
                }

                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded() || binding == null) return;

                    binding.tutorial.getRoot().setVisibility(noData ? View.VISIBLE : View.GONE);

                    // Month-specific empty state logic
                    if (!noData && !hasCurrentData && hasPreviousData) {
                        assetAdapter.setCopyAllAction(true, 
                                getString(R.string.empty_month_copy_action, prevMonth.toString()), 
                                () -> copyFromPrevious(prevMonth));
                    } else {
                        assetAdapter.setCopyAllAction(false, "", null);
                    }

                    List<Asset> assetItems = new ArrayList<>();
                    List<Asset> liabItems = new ArrayList<>();
                    for (Asset a : assets) {
                        if (a.getValue() >= 0) assetItems.add(a);
                        else liabItems.add(a);
                    }

                    assetAdapter.setItems(assetItems);
                    liabAdapter.setItems(liabItems);

                    String currency = Prefs.getString(Prefs.PREFS_CURRENCY, Prefs.DEFAULT_CURRENCY);
                    binding.currencySymbol.setText(currency);
                    binding.monthValue.setText(Tools.formatNumber(value, false));
                    binding.heroAssetValue.setText(currency + Tools.formatNumber(ta, true));
                    binding.heroLiabValue.setText(currency + Tools.formatNumber(tl, true));
                    binding.heroAssetCount.setText(String.valueOf(ac));
                    binding.heroLiabCount.setText(String.valueOf(lc));
                    binding.percentView.init(prevValue, value);
                    binding.percentView.fillValueChange(binding.monthValueChange);

                    // Update Hero Title
                    binding.heroTitle.setText(String.format("NET WORTH · %s", month.toString().toUpperCase()));

                    // Update counts
                    binding.assetSectionTitle.setText(String.format("ASSETS · %d", ac));
                    binding.liabSectionTitle.setText(String.format("LIABILITIES · %d", lc));
                    
                    binding.assetHeader.setVisibility(!assetItems.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.liabHeader.setVisibility(!liabItems.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.assetList.setVisibility(!assetItems.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.liabList.setVisibility(!liabItems.isEmpty() ? View.VISIBLE : View.GONE);

                    // Update Insight Cards
                    updateInsights(value, prevValue, ta, tl);

                    binding.percentText.setText(Tools.formatPercent(Math.abs(binding.percentView.getPercent())));
                    boolean positive = binding.percentView.getValueChange() >= 0;
                    binding.percentPill.setBackgroundResource(positive ? R.drawable.bg_hero_pill_positive : R.drawable.bg_hero_pill_negative);
                    binding.percentArrow.setRotation(positive ? 0 : 180);
                    binding.monthValueChangeArrow.setRotation(positive ? 0 : 180);
                    
                    int color = ContextCompat.getColor(requireContext(), positive ? R.color.positive : R.color.negative);
                    binding.percentArrow.setColorFilter(color);
                    binding.monthValueChangeArrow.setColorFilter(color);
                    binding.percentText.setTextColor(color);
                    binding.monthValueChange.setTextColor(color);

                    binding.vsLastMonthLabel.setText(getString(R.string.vs_month, prevMonth.toString().split(" ")[0]));

                    if (!note.isEmpty()) {
                        binding.monthCommentPreview.setVisibility(View.VISIBLE);
                        binding.monthCommentPreview.setText(note);
                    } else {
                        binding.monthCommentPreview.setVisibility(View.GONE);
                    }

                    binding.miniBarChart.setData(history, 7);

                    // Privacy mode
                    updatePrivacyMode(Prefs.getBoolean("privacy_mode", false));
                    
                    applyAccentColor();

                    binding.shimmerView.stopShimmer();
                    binding.shimmerView.setVisibility(View.GONE);
                    binding.mainContent.setVisibility(View.VISIBLE);

                    listener.onMonthReady(month);
                });
            }
        });
    }

    public Month getMonth() { return month; }

    private void applyAccentColor() {
        if (!isAdded() || binding == null) return;
        int accentColor = ContextCompat.getColor(requireContext(), Tools.getAccentColor());
        ColorStateList accentList = ColorStateList.valueOf(accentColor);
        
        binding.refreshLayout.setColorSchemeColors(accentColor);
        
        binding.assetSectionTitle.setTextColor(accentColor);
        binding.liabSectionTitle.setTextColor(accentColor);
        
        binding.insight2.insightTint.setBackgroundColor(accentColor);
        binding.insight2.insightProgress.setProgressTintList(accentList);
        
        binding.insight3.insightTint.setBackgroundColor(accentColor);
        binding.insight3.insightProgress.setProgressTintList(accentList);
        
        binding.copyPromptIcon.setImageTintList(accentList);
        
        binding.miniBarChart.setHighlightColor(accentColor);
    }

    private void updatePrivacyMode(boolean hidden) {
        binding.privacyIcon.setImageResource(hidden ? R.drawable.ic_eye_off_thin : R.drawable.ic_eye_thin);
        assetAdapter.setPrivacyMode(hidden);
        liabAdapter.setPrivacyMode(hidden);

        if (hidden) {
            String stars = "••••••••";
            String c = Prefs.getString(Prefs.PREFS_CURRENCY, Prefs.DEFAULT_CURRENCY);
            binding.monthValue.setText(stars);
            binding.heroAssetValue.setText(c + "••••");
            binding.heroLiabValue.setText(c + "••••");
            binding.heroAssetCount.setText("••");
            binding.heroLiabCount.setText("••");
            binding.monthValueChange.setText("••••");
            binding.percentText.setText("••%");
            binding.insight1.insightValue.setText("••••");
            binding.insight2.insightValue.setText("••••");
            binding.insight3.insightValue.setText("••••");
            binding.miniBarChart.setAlpha(0f);
        } else {
            binding.miniBarChart.setAlpha(1f);
        }
    }

    private void updateInsights(double netWorth, double prevNetWorth, double assets, double liabs) {
        // Insight 1: Monthly Change
        if (prevNetWorth != 0) {
            binding.insight1.getRoot().setVisibility(View.VISIBLE);
            double change = netWorth - prevNetWorth;
            binding.insight1.insightLabel.setText("MONTHLY CHANGE");
            binding.insight1.insightValue.setText(Tools.formatAmount(change));
            binding.insight1.insightSub.setText(change >= 0 ? "Growth" : "Decline");
            binding.insight1.insightTint.setBackgroundColor(ContextCompat.getColor(requireContext(), change >= 0 ? R.color.positive : R.color.negative));
        } else {
            binding.insight1.getRoot().setVisibility(View.GONE);
        }

        // Insight 2: Debt Ratio
        if (assets > 0 && liabs > 0) {
            binding.insight2.getRoot().setVisibility(View.VISIBLE);
            double debtRatio = (liabs / assets) * 100;
            binding.insight2.insightLabel.setText("DEBT RATIO");
            binding.insight2.insightValue.setText(String.format("%.1f%%", debtRatio));
            binding.insight2.insightSub.setText("Liabilities/Assets");
            binding.insight2.insightProgress.setVisibility(View.VISIBLE);
            binding.insight2.insightProgress.setProgress((int) Math.min(debtRatio, 100));
            binding.insight2.insightTint.setBackgroundColor(ContextCompat.getColor(requireContext(), Tools.getAccentColor()));
        } else {
            binding.insight2.getRoot().setVisibility(View.GONE);
        }

        // Insight 3: Goal progress (1Y)
        float g1 = Prefs.getFloat(Prefs.PREFS_GOAL_1Y, 0f);
        if (g1 > 0) {
            binding.insight3.getRoot().setVisibility(View.VISIBLE);
            binding.insight3.insightLabel.setText("1Y GOAL");
            int progress = (int) Math.min(Math.max(netWorth / g1 * 100, 0), 100);
            binding.insight3.insightValue.setText(String.format("%d%%", progress));
            binding.insight3.insightSub.setText(Tools.formatAmount(g1));
            binding.insight3.insightProgress.setVisibility(View.VISIBLE);
            binding.insight3.insightProgress.setProgress(progress);
            binding.insight3.insightTint.setBackgroundColor(0xFFF59E0B); // Amber
        } else {
            binding.insight3.getRoot().setVisibility(View.GONE);
        }
    }

    private void copyFromPrevious(Month prevMonth) {
        executor.execute(() -> {
            try (Realm realm = Realm.getDefaultInstance()) {
                List<Asset> prevAssets = realm.where(Asset.class)
                        .equalTo("month", prevMonth.getMonth())
                        .equalTo("year", prevMonth.getYear())
                        .findAll();

                if (prevAssets.isEmpty()) return;

                List<Asset> newAssets = new ArrayList<>();
                for (Asset old : prevAssets) {
                    newAssets.add(new Asset(old.getName(), old.getValue(), month.getMonth(), month.getYear()));
                }

                realm.executeTransaction(r -> r.copyToRealmOrUpdate(newAssets));
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    listener.onDataChanged();
                    android.widget.Toast.makeText(requireContext(),
                            getString(R.string.empty_month_copy_success, prevMonth.toString()),
                            android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onItemClick(Asset item) {
        listener.onEditAsset(item);
    }

    @Override
    public void onItemLongClick(Asset item) {
        listener.onLongPressAsset(item);
    }
}
