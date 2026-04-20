package com.kanzar.networthtracker;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.kanzar.networthtracker.adapters.AssetAdapter;
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
    }

    private static final String ARG_MONTH = "arg_month";
    private static final String ARG_YEAR  = "arg_year";

    private Month month;
    private AssetAdapter adapter;
    private Listener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView monthValue, monthValueChange, headerPrevValue, headerAssetCount, commentPreview;
    private PercentView percentView;
    private MiniBarView miniBarChart;
    private View tutorial, mainContent;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerView;

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
        return inflater.inflate(R.layout.fragment_month_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        monthValue       = view.findViewById(R.id.monthValue);
        monthValueChange = view.findViewById(R.id.monthValueChange);
        headerPrevValue  = view.findViewById(R.id.headerPrevValue);
        headerAssetCount = view.findViewById(R.id.headerAssetCount);
        commentPreview   = view.findViewById(R.id.monthCommentPreview);
        percentView      = view.findViewById(R.id.percentView);
        miniBarChart     = view.findViewById(R.id.miniBarChart);
        tutorial         = view.findViewById(R.id.tutorial);
        mainContent      = view.findViewById(R.id.mainContent);
        shimmerView      = view.findViewById(R.id.shimmerView);

        view.findViewById(R.id.menu).setOnClickListener(v -> listener.onOpenDrawer());

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(() -> {
            refreshLayout.setRefreshing(false);
            listener.onRequestSync();
        });

        RecyclerView assetList = view.findViewById(R.id.assetList);
        adapter = new AssetAdapter(requireContext(), this);
        assetList.setLayoutManager(new LinearLayoutManager(requireContext()));
        assetList.setAdapter(adapter);

        refresh();
    }

    public void refresh() {
        refresh(true);
    }

    public void refresh(boolean showShimmer) {
        if (!isAdded() || getView() == null) return;

        if (showShimmer) {
            shimmerView.setVisibility(View.VISIBLE);
            shimmerView.startShimmer();
            mainContent.setVisibility(View.GONE);
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

            List<Float> history = new ArrayList<>();
            Month hm = new Month(month.getMonth(), month.getYear());
            history.add(0, (float) hm.getValue());
            for (int i = 0; i < 5; i++) {
                hm.previous();
                history.add(0, (float) hm.getValue());
            }

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || getView() == null) return;

                tutorial.setVisibility(noData ? View.VISIBLE : View.GONE);
                adapter.setItems(assets);
                monthValue.setText(Tools.formatAmount(value));
                percentView.init(prevValue, value);
                percentView.fillValueChange(monthValueChange);

                if (prevValue != 0) {
                    headerPrevValue.setVisibility(View.VISIBLE);
                    headerPrevValue.setText(getString(R.string.header_prev_value, Tools.formatAmount(prevValue)));
                } else {
                    headerPrevValue.setVisibility(View.GONE);
                }

                if (!countText.isEmpty()) {
                    headerAssetCount.setVisibility(View.VISIBLE);
                    headerAssetCount.setText(countText);
                } else {
                    headerAssetCount.setVisibility(View.GONE);
                }

                if (!note.isEmpty()) {
                    commentPreview.setVisibility(View.VISIBLE);
                    commentPreview.setText(note);
                } else {
                    commentPreview.setVisibility(View.GONE);
                }

                miniBarChart.setData(history, 5);

                shimmerView.stopShimmer();
                shimmerView.setVisibility(View.GONE);
                mainContent.setVisibility(View.VISIBLE);

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

    @Override
    public void onItemClick(Asset item) {
        listener.onEditAsset(item);
    }

    @Override
    public void onItemLongClick(Asset item) {
        listener.onLongPressAsset(item);
    }
}
