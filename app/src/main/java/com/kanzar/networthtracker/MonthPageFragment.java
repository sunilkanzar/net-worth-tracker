package com.kanzar.networthtracker;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private AssetAdapter adapter;
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

        binding.refreshLayout.setOnRefreshListener(() -> {
            binding.refreshLayout.setRefreshing(false);
            listener.onRequestSync();
        });

        adapter = new AssetAdapter(requireContext(), this);
        binding.assetList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.assetList.setAdapter(adapter);

        refresh();
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

        if (showShimmer) {
            binding.shimmerView.setVisibility(View.VISIBLE);
            binding.shimmerView.startShimmer();
            binding.mainContent.setVisibility(View.GONE);
        }

        executor.execute(() -> {
            List<Asset> assets = month.getAssets();

            boolean noData;
            try (Realm realm = Realm.getDefaultInstance()) {
                noData = realm.where(Asset.class).count() == 0;
            }

            double value     = month.getValue();
            double prevValue = month.getPreviousMonth().getValue();

            int assetCount = 0, liabilityCount = 0;
            for (Asset a : assets) {
                if (!a.isHelper()) {
                    if (a.getValue() >= 0) assetCount++;
                    else liabilityCount++;
                }
            }
            final int ac = assetCount, lc = liabilityCount;
            String countText = buildCountText(ac, lc);
            String note = Prefs.getString(CommentActivity.noteKey(month.getMonth(), month.getYear()), "");

            boolean hasCurrentData = month.hasAssets();
            Month prevMonth = month.getPreviousMonth();
            boolean hasPreviousData = prevMonth.hasAssets();

            List<Float> history = new ArrayList<>();
            Month hm = new Month(month.getMonth(), month.getYear());
            history.add(0, (float) hm.getValue());
            for (int i = 0; i < 5; i++) {
                hm.previous();
                history.add(0, (float) hm.getValue());
            }

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || binding == null) return;

                binding.tutorial.setVisibility(noData ? View.VISIBLE : View.GONE);

                // Month-specific empty state logic
                if (!noData && !hasCurrentData && hasPreviousData) {
                    adapter.setCopyAllAction(true, 
                            getString(R.string.empty_month_copy_action, prevMonth.toString()), 
                            () -> copyFromPrevious(prevMonth));
                } else {
                    adapter.setCopyAllAction(false, "", null);
                }

                adapter.setItems(assets);
                binding.monthValue.setText(Tools.formatAmount(value));
                binding.percentView.init(prevValue, value);
                binding.percentView.fillValueChange(binding.monthValueChange);

                if (prevValue != 0) {
                    binding.headerPrevValue.setVisibility(View.VISIBLE);
                    binding.headerPrevValue.setText(getString(R.string.header_prev_value, Tools.formatAmount(prevValue)));
                } else {
                    binding.headerPrevValue.setVisibility(View.GONE);
                }

                if (!countText.isEmpty()) {
                    binding.headerAssetCount.setVisibility(View.VISIBLE);
                    binding.headerAssetCount.setText(countText);
                } else {
                    binding.headerAssetCount.setVisibility(View.GONE);
                }

                if (!note.isEmpty()) {
                    binding.monthCommentPreview.setVisibility(View.VISIBLE);
                    binding.monthCommentPreview.setText(note);
                } else {
                    binding.monthCommentPreview.setVisibility(View.GONE);
                }

                binding.miniBarChart.setData(history, 5);

                binding.shimmerView.stopShimmer();
                binding.shimmerView.setVisibility(View.GONE);
                binding.mainContent.setVisibility(View.VISIBLE);

                listener.onMonthReady(month);
            });
        });
    }

    private String buildCountText(int assetCount, int liabilityCount) {
        if (assetCount == 0 && liabilityCount == 0) return "";
        StringBuilder sb = new StringBuilder();
        if (assetCount > 0)
            sb.append(getString(assetCount == 1 ? R.string.asset_count_one : R.string.asset_count_many, assetCount));
        if (liabilityCount > 0) {
            if (sb.length() > 0) sb.append(getString(R.string.count_separator));
            sb.append(getString(liabilityCount == 1 ? R.string.liability_count_one : R.string.liability_count_many, liabilityCount));
        }
        return sb.toString();
    }

    public Month getMonth() { return month; }

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
