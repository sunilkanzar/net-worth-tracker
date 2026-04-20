package com.kanzar.networthtracker.adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.Asset;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.kanzar.networthtracker.helpers.Month;
import io.realm.Realm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AssetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private static final ExecutorService sparklineExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Context context;
    private List<Object> displayItems = new ArrayList<>();
    private List<? extends Asset> originalItems;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Asset item);
        void onItemLongClick(Asset item);
    }

    public AssetAdapter(Context context) {
        this.context = context;
        this.listener = (OnItemClickListener) context;
    }

    public AssetAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }


    @SuppressWarnings("unchecked")
    public List<Asset> getItems() {
        return (List<Asset>) originalItems;
    }

    public void setItems(List<? extends Asset> items) {
        this.originalItems = items;
        this.displayItems = new ArrayList<>();

        if (items != null && !items.isEmpty()) {
            List<Asset> assets = new ArrayList<>();
            List<Asset> liabilities = new ArrayList<>();

            for (Asset asset : items) {
                if (asset.getValue() >= 0) {
                    assets.add(asset);
                } else {
                    liabilities.add(asset);
                }
            }

            Comparator<Asset> valueComparator = (a1, a2) -> Double.compare(Math.abs(a2.getValue()), Math.abs(a1.getValue()));
            Collections.sort(assets, valueComparator);
            Collections.sort(liabilities, valueComparator);

            if (!assets.isEmpty()) {
                displayItems.add("ASSETS");
                displayItems.addAll(assets);
            }
            if (!liabilities.isEmpty()) {
                displayItems.add("LIABILITIES");
                displayItems.addAll(liabilities);
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asset, parent, false);
            return new ViewHolder(v);
        }
    }

    private double getTotalAssets() {
        double sum = 0.0;
        if (originalItems != null) {
            for (Asset asset : originalItems) {
                if (!asset.isHelper() && asset.getValue() > 0) {
                    sum += asset.getValue();
                }
            }
        }
        return sum;
    }

    private double getTotalLiabilities() {
        double sum = 0.0;
        if (originalItems != null) {
            for (Asset asset : originalItems) {
                if (!asset.isHelper() && asset.getValue() < 0) {
                    sum += asset.getValue();
                }
            }
        }
        return Math.abs(sum);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = displayItems.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).headerText.setText((String) item);
        } else if (holder instanceof ViewHolder) {
            final Asset asset = (Asset) item;
            ((ViewHolder) holder).bind(asset, context, getTotalAssets(), getTotalLiabilities());

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(asset);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClick(asset);
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = (TextView) itemView.findViewById(R.id.headerText);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView assetName;
        private final TextView assetValue;
        private final TextView assetWeight;
        private final TextView assetChangeValue;
        private final TextView assetChangePercent;
        private final LineChart assetSparkline;
        private final android.view.ViewGroup container;
        private String boundAssetKey = "";

        public ViewHolder(View v) {
            super(v);
            assetName = v.findViewById(R.id.assetName);
            assetValue = v.findViewById(R.id.assetValue);
            assetWeight = v.findViewById(R.id.assetWeight);
            assetChangeValue = v.findViewById(R.id.assetChangeValue);
            assetChangePercent = v.findViewById(R.id.assetChangePercent);
            assetSparkline = v.findViewById(R.id.assetSparkline);
            container = v.findViewById(R.id.container);
            setupSparkline();
        }

        private void setupSparkline() {
            assetSparkline.getDescription().setEnabled(false);
            assetSparkline.getLegend().setEnabled(false);
            assetSparkline.getAxisLeft().setEnabled(false);
            assetSparkline.getAxisRight().setEnabled(false);
            assetSparkline.getXAxis().setEnabled(false);
            assetSparkline.setDrawGridBackground(false);
            assetSparkline.setTouchEnabled(false);
            assetSparkline.setViewPortOffsets(0, 0, 0, 0);
        }

        public void bind(Asset asset, Context context, double totalAssets, double totalLiabilities) {
            assetName.setText(asset.getName());
            assetValue.setText(Tools.formatAmount(asset.getValue()));

            Asset previous = asset.getPrevious();
            double prevVal = previous != null ? previous.getValue() : 0.0;

            if (asset.isHelper()) {
                container.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
                assetValue.setVisibility(View.GONE);
                assetWeight.setVisibility(View.GONE);
                assetChangeValue.setVisibility(View.VISIBLE);
                assetChangePercent.setVisibility(View.GONE);
                assetSparkline.setVisibility(View.GONE);

                String helperText = context.getString(R.string.helper_text, asset.getName());
                assetChangeValue.setText(helperText);
                assetChangeValue.setTextColor(ContextCompat.getColor(context, R.color.textSecondary));
            } else {
                container.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
                assetValue.setVisibility(View.VISIBLE);
                assetWeight.setVisibility(View.VISIBLE);
                assetChangeValue.setVisibility(View.VISIBLE);
                assetChangePercent.setVisibility(View.VISIBLE);
                assetSparkline.setVisibility(View.VISIBLE);

                // Change Row
                double change = asset.getValue() - prevVal;
                int color = ContextCompat.getColor(context, Tools.getTextChangeColor(change));
                
                assetValue.setText(Tools.formatAmount(asset.getValue()));
                assetChangeValue.setText(Tools.formatAmount(change));
                assetChangeValue.setTextColor(color);
                
                assetChangePercent.setText(Tools.formatPercent(Math.abs(Tools.getPercent(prevVal, asset.getValue()))));
                assetChangePercent.setTextColor(color);

                // Weight Row
                boolean isAsset = asset.getValue() >= 0;
                double totalForCategory = isAsset ? totalAssets : totalLiabilities;
                double weight = totalForCategory > 0 ? (Math.abs(asset.getValue()) / totalForCategory) * 100.0 : 0.0;
                assetWeight.setText(Tools.formatPercent(weight));
                assetWeight.setBackgroundResource(isAsset ? R.drawable.bg_pill_asset : R.drawable.bg_pill_liability);
                assetWeight.setTextColor(ContextCompat.getColor(context, isAsset ? R.color.colorAccent : R.color.negative));

                // Sparkline Data
                int chartColor = isAsset ? ContextCompat.getColor(context, R.color.colorAccent) : ContextCompat.getColor(context, R.color.negative);
                assetSparkline.clear();
                loadSparklineDataAsync(asset, chartColor);
            }
        }

        private void loadSparklineDataAsync(Asset asset, int color) {
            final String key = asset.getName() + asset.getMonth() + asset.getYear();
            boundAssetKey = key;

            sparklineExecutor.execute(() -> {
                List<Entry> entries = new ArrayList<>();
                int mo = asset.getMonth(), yr = asset.getYear();
                try (Realm realm = Realm.getDefaultInstance()) {
                    for (int i = 0; i < 12; i++) {
                        Asset historical = realm.where(Asset.class)
                                .equalTo("name", asset.getName())
                                .equalTo("month", mo)
                                .equalTo("year", yr)
                                .findFirst();
                        entries.add(new Entry(i, (float) (historical != null ? historical.getValue() : 0.0)));
                        if (mo == 1) { mo = 12; yr--; } else { mo--; }
                    }
                }
                Collections.reverse(entries);
                for (int i = 0; i < entries.size(); i++) entries.get(i).setX(i);

                LineDataSet dataSet = new LineDataSet(entries, "");
                dataSet.setColor(color);
                dataSet.setLineWidth(1f);
                dataSet.setDrawCircles(false);
                dataSet.setDrawValues(false);
                dataSet.setMode(LineDataSet.Mode.LINEAR);
                dataSet.setDrawFilled(false);
                LineData lineData = new LineData(dataSet);

                mainHandler.post(() -> {
                    if (!key.equals(boundAssetKey)) return; // view recycled
                    assetSparkline.setData(lineData);
                    assetSparkline.invalidate();
                });
            });
        }
    }
}
