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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.databinding.ItemAssetBinding;
import com.kanzar.networthtracker.databinding.ItemHeaderBinding;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.Asset;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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
    private static final int TYPE_COPY_ALL = 2;

    private static final ExecutorService sparklineExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Context context;
    private List<Object> displayItems = new ArrayList<>();
    private List<? extends Asset> originalItems;
    private final OnItemClickListener listener;
    private boolean showCopyAll = false;
    private String copyAllText = "";
    private Runnable onCopyAllClickListener;

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
        final List<Object> newDisplayItems = new ArrayList<>();

        if (showCopyAll) {
            newDisplayItems.add(new CopyAllItem(copyAllText));
        }

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
                newDisplayItems.add("ASSETS");
                newDisplayItems.addAll(assets);
            }
            if (!liabilities.isEmpty()) {
                newDisplayItems.add("LIABILITIES");
                newDisplayItems.addAll(liabilities);
            }
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AssetDiffCallback(this.displayItems, newDisplayItems));
        this.originalItems = items;
        this.displayItems = newDisplayItems;
        diffResult.dispatchUpdatesTo(this);
    }

    public void setCopyAllAction(boolean show, String text, Runnable action) {
        this.showCopyAll = show;
        this.copyAllText = text;
        this.onCopyAllClickListener = action;
    }

    private static class CopyAllItem {
        final String text;
        CopyAllItem(String text) { this.text = text; }
    }

    private static class AssetDiffCallback extends DiffUtil.Callback {
        private final List<Object> oldList;
        private final List<Object> newList;

        AssetDiffCallback(List<Object> oldList, List<Object> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Object oldItem = oldList.get(oldItemPosition);
            Object newItem = newList.get(newItemPosition);

            if (oldItem instanceof String && newItem instanceof String) {
                return oldItem.equals(newItem);
            } else if (oldItem instanceof Asset && newItem instanceof Asset) {
                return ((Asset) oldItem).getId().equals(((Asset) newItem).getId());
            } else if (oldItem instanceof CopyAllItem && newItem instanceof CopyAllItem) {
                return true;
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Object oldItem = oldList.get(oldItemPosition);
            Object newItem = newList.get(newItemPosition);

            if (oldItem instanceof String && newItem instanceof String) {
                return oldItem.equals(newItem);
            } else if (oldItem instanceof Asset && newItem instanceof Asset) {
                Asset oldAsset = (Asset) oldItem;
                Asset newAsset = (Asset) newItem;
                return oldAsset.getValue() == newAsset.getValue() &&
                        oldAsset.getName().equals(newAsset.getName()) &&
                        oldAsset.getUpdatedAt() == newAsset.getUpdatedAt();
            } else if (oldItem instanceof CopyAllItem && newItem instanceof CopyAllItem) {
                return ((CopyAllItem) oldItem).text.equals(((CopyAllItem) newItem).text);
            }
            return false;
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object item = displayItems.get(position);
        if (item instanceof String) return TYPE_HEADER;
        if (item instanceof CopyAllItem) return TYPE_COPY_ALL;
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            ItemHeaderBinding binding = ItemHeaderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new HeaderViewHolder(binding);
        } else if (viewType == TYPE_COPY_ALL) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_copy_all, parent, false);
            return new CopyAllViewHolder(view);
        } else {
            ItemAssetBinding binding = ItemAssetBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
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
            ((HeaderViewHolder) holder).binding.headerText.setText((String) item);
        } else if (holder instanceof CopyAllViewHolder) {
            CopyAllViewHolder h = (CopyAllViewHolder) holder;
            h.textView.setText(((CopyAllItem) item).text);
            h.itemView.setOnClickListener(v -> {
                if (onCopyAllClickListener != null) onCopyAllClickListener.run();
            });
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
        private final ItemHeaderBinding binding;

        public HeaderViewHolder(ItemHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class CopyAllViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public CopyAllViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.copyAllText);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAssetBinding binding;
        private String boundAssetKey = "";

        public ViewHolder(ItemAssetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            setupSparkline();
        }

        private void setupSparkline() {
            binding.assetSparkline.getDescription().setEnabled(false);
            binding.assetSparkline.getLegend().setEnabled(false);
            binding.assetSparkline.getAxisLeft().setEnabled(false);
            binding.assetSparkline.getAxisRight().setEnabled(false);
            binding.assetSparkline.getXAxis().setEnabled(false);
            binding.assetSparkline.setDrawGridBackground(false);
            binding.assetSparkline.setTouchEnabled(false);
            binding.assetSparkline.setViewPortOffsets(0, 0, 0, 0);
            binding.assetSparkline.setNoDataText("");
        }

        public void bind(Asset asset, Context context, double totalAssets, double totalLiabilities) {
            binding.assetName.setText(asset.getName());
            binding.assetValue.setText(Tools.formatAmount(asset.getValue()));

            Asset previous = asset.getPrevious();
            double prevVal = previous != null ? previous.getValue() : 0.0;

            if (asset.isHelper()) {
                binding.container.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
                binding.assetValue.setVisibility(View.GONE);
                binding.assetWeight.setVisibility(View.GONE);
                binding.assetChangeValue.setVisibility(View.VISIBLE);
                binding.assetChangePercent.setVisibility(View.GONE);
                binding.assetSparkline.setVisibility(View.GONE);

                String helperText = context.getString(R.string.helper_text, asset.getName());
                binding.assetChangeValue.setText(helperText);
                binding.assetChangeValue.setTextColor(ContextCompat.getColor(context, R.color.textSecondary));
            } else {
                binding.container.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
                binding.assetValue.setVisibility(View.VISIBLE);
                binding.assetWeight.setVisibility(View.VISIBLE);
                binding.assetChangeValue.setVisibility(View.VISIBLE);
                binding.assetChangePercent.setVisibility(View.VISIBLE);
                binding.assetSparkline.setVisibility(View.VISIBLE);

                // Change Row
                double change = asset.getValue() - prevVal;
                int color = ContextCompat.getColor(context, Tools.getTextChangeColor(change));
                
                binding.assetValue.setText(Tools.formatAmount(asset.getValue()));
                binding.assetChangeValue.setText(Tools.formatAmount(change));
                binding.assetChangeValue.setTextColor(color);
                
                binding.assetChangePercent.setText(Tools.formatPercent(Math.abs(Tools.getPercent(prevVal, asset.getValue()))));
                binding.assetChangePercent.setTextColor(color);

                // Weight Row
                boolean isAsset = asset.getValue() >= 0;
                double totalForCategory = isAsset ? totalAssets : totalLiabilities;
                double weight = totalForCategory > 0 ? (Math.abs(asset.getValue()) / totalForCategory) * 100.0 : 0.0;
                binding.assetWeight.setText(Tools.formatPercent(weight));
                binding.assetWeight.setBackgroundResource(isAsset ? R.drawable.bg_pill_asset : R.drawable.bg_pill_liability);
                binding.assetWeight.setTextColor(ContextCompat.getColor(context, isAsset ? R.color.colorAccent : R.color.negative));

                // Sparkline Data
                int chartColor = isAsset ? ContextCompat.getColor(context, R.color.colorAccent) : ContextCompat.getColor(context, R.color.negative);
                binding.assetSparkline.clear();
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
                    binding.assetSparkline.setData(lineData);
                    binding.assetSparkline.invalidate();
                });
            });
        }
    }
}
