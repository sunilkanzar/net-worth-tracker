package com.kanzar.networthtracker.adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AssetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    // Sparkline executor and handler
    private static final ExecutorService sparklineExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Context context;
    private List<Object> displayItems = new ArrayList<>();
    private List<? extends Asset> originalItems;
    private double cachedTotalAssets = 0.0;
    private double cachedTotalLiabilities = 0.0;
    private final OnItemClickListener listener;
    private boolean privacyMode = false;
    private String lastFormatKey = "";

    public enum SortOrder {
        VALUE_DESC, VALUE_ASC, NAME_ASC, NAME_DESC, CHANGE_DESC, CHANGE_ASC, PERCENT_DESC, PERCENT_ASC
    }

    public interface OnItemClickListener {
        void onItemClick(Asset item);
        void onItemLongClick(Asset item);
    }

    private SortOrder currentSortOrder = SortOrder.VALUE_DESC;

    public void setSortOrder(SortOrder sortOrder) {
        this.currentSortOrder = sortOrder;
        if (originalItems != null) {
            setItems(originalItems);
        }
    }

    public AssetAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }


    public void setItems(List<? extends Asset> items) {
        final List<Object> newDisplayItems = new ArrayList<>();
        double totalAssets = 0.0;
        double totalLiabilities = 0.0;

        if (items != null && !items.isEmpty()) {
            List<Asset> assets = new ArrayList<>();
            List<Asset> liabilities = new ArrayList<>();

            for (Asset asset : items) {
                if (asset.getValue() >= 0) {
                    assets.add(asset);
                    if (!asset.isHelper()) {
                        totalAssets += asset.getValue();
                    }
                } else {
                    liabilities.add(asset);
                    if (!asset.isHelper()) {
                        totalLiabilities += asset.getValue();
                    }
                }
            }

            Comparator<Asset> comparator;
            switch (currentSortOrder) {
                case VALUE_ASC:
                    comparator = (a1, a2) -> Double.compare(Math.abs(a1.getValue()), Math.abs(a2.getValue()));
                    break;
                case NAME_ASC:
                    comparator = (a1, a2) -> a1.getName().compareToIgnoreCase(a2.getName());
                    break;
                case NAME_DESC:
                    comparator = (a1, a2) -> a2.getName().compareToIgnoreCase(a1.getName());
                    break;
                case CHANGE_DESC:
                    comparator = (a1, a2) -> {
                        double c1 = a1.getValue() - (a1.hasPrevValue() ? a1.getPrevValue() : 0.0);
                        double c2 = a2.getValue() - (a2.hasPrevValue() ? a2.getPrevValue() : 0.0);
                        return Double.compare(c2, c1);
                    };
                    break;
                case CHANGE_ASC:
                    comparator = (a1, a2) -> {
                        double c1 = a1.getValue() - (a1.hasPrevValue() ? a1.getPrevValue() : 0.0);
                        double c2 = a2.getValue() - (a2.hasPrevValue() ? a2.getPrevValue() : 0.0);
                        return Double.compare(c1, c2);
                    };
                    break;
                case PERCENT_DESC:
                    comparator = (a1, a2) -> {
                        double p1 = Tools.getPercent(a1.hasPrevValue() ? a1.getPrevValue() : 0.0, a1.getValue());
                        double p2 = Tools.getPercent(a2.hasPrevValue() ? a2.getPrevValue() : 0.0, a2.getValue());
                        return Double.compare(p2, p1);
                    };
                    break;
                case PERCENT_ASC:
                    comparator = (a1, a2) -> {
                        double p1 = Tools.getPercent(a1.hasPrevValue() ? a1.getPrevValue() : 0.0, a1.getValue());
                        double p2 = Tools.getPercent(a2.hasPrevValue() ? a2.getPrevValue() : 0.0, a2.getValue());
                        return Double.compare(p1, p2);
                    };
                    break;
                case VALUE_DESC:
                default:
                    comparator = (a1, a2) -> Double.compare(Math.abs(a2.getValue()), Math.abs(a1.getValue()));
                    break;
            }
            
            Collections.sort(assets, comparator);
            Collections.sort(liabilities, comparator);

            if (!assets.isEmpty()) {
                newDisplayItems.addAll(assets);
            }
            if (!liabilities.isEmpty()) {
                newDisplayItems.addAll(liabilities);
            }
        }

        String formatKey = com.kanzar.networthtracker.helpers.Prefs.getString(com.kanzar.networthtracker.helpers.Prefs.PREFS_NUMBER_FORMAT, "")
                + com.kanzar.networthtracker.helpers.Prefs.getString(com.kanzar.networthtracker.helpers.Prefs.PREFS_NUMBER_SEPARATOR, "")
                + com.kanzar.networthtracker.helpers.Prefs.getString(com.kanzar.networthtracker.helpers.Prefs.PREFS_CURRENCY, "");
        boolean formatChanged = !Objects.equals(formatKey, this.lastFormatKey);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AssetDiffCallback(this.displayItems, newDisplayItems), false);
        boolean totalChanged = totalAssets != this.cachedTotalAssets || Math.abs(totalLiabilities) != this.cachedTotalLiabilities;
        this.originalItems = items;
        this.displayItems = newDisplayItems;
        this.cachedTotalAssets = totalAssets;
        this.cachedTotalLiabilities = Math.abs(totalLiabilities);
        this.lastFormatKey = formatKey;
        if (totalChanged || formatChanged) {
            notifyDataSetChanged();
        } else {
            diffResult.dispatchUpdatesTo(this);
        }
    }

    public void setPrivacyMode(boolean privacyMode) {
        if (this.privacyMode == privacyMode) return;
        this.privacyMode = privacyMode;
        notifyDataSetChanged();
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
                return Objects.equals(oldItem, newItem);
            } else if (oldItem instanceof Asset && newItem instanceof Asset) {
                return Objects.equals(((Asset) oldItem).getId(), ((Asset) newItem).getId());
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Object oldItem = oldList.get(oldItemPosition);
            Object newItem = newList.get(newItemPosition);

            if (oldItem instanceof String && newItem instanceof String) {
                return Objects.equals(oldItem, newItem);
            } else if (oldItem instanceof Asset && newItem instanceof Asset) {
                Asset oldAsset = (Asset) oldItem;
                Asset newAsset = (Asset) newItem;
                return oldAsset.getValue() == newAsset.getValue() &&
                        Objects.equals(oldAsset.getName(), newAsset.getName()) &&
                        oldAsset.getUpdatedAt() == newAsset.getUpdatedAt();
            }
            return false;
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object item = displayItems.get(position);
        if (item instanceof String) return TYPE_HEADER;
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_HEADER:
                ItemHeaderBinding headerBinding = ItemHeaderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new HeaderViewHolder(headerBinding);
            case TYPE_ITEM:
            default:
                ItemAssetBinding assetBinding = ItemAssetBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new ViewHolder(assetBinding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = displayItems.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).binding.headerText.setText((String) item);
        } else if (holder instanceof ViewHolder) {
            final Asset asset = (Asset) item;
            ((ViewHolder) holder).bind(asset, context, cachedTotalAssets, cachedTotalLiabilities, privacyMode);

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
            binding.assetSparkline.setHardwareAccelerationEnabled(true);
        }

        public void bind(Asset asset, Context context, double totalAssets, double totalLiabilities, boolean privacyMode) {
            binding.assetName.setText(asset.getName());
            binding.assetLetter.setText(asset.getName().substring(0, 1).toUpperCase());
            binding.assetName.setTextColor(ContextCompat.getColor(context, R.color.text));
            binding.assetChevron.setVisibility(View.GONE);
            binding.assetWeight.setVisibility(View.VISIBLE);
            binding.assetSparkline.setVisibility(View.VISIBLE);
            binding.assetValue.setVisibility(View.VISIBLE);
            binding.assetChangePercent.setVisibility(View.VISIBLE);
            
            // Reset card stroke
            binding.assetCard.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.border)));

            // Color: deterministic from name hash over design palette
            int itemColor = Tools.getAssetColor(asset.getName(), asset.getValue() < 0);
            int itemBg = android.graphics.Color.argb(0x22,
                    android.graphics.Color.red(itemColor),
                    android.graphics.Color.green(itemColor),
                    android.graphics.Color.blue(itemColor));

            binding.assetLetter.setTextColor(itemColor);
            android.graphics.drawable.GradientDrawable bg = (android.graphics.drawable.GradientDrawable) binding.assetLetter.getBackground();
            if (bg != null) {
                bg.setColor(itemBg);
            }

            double prevVal = asset.hasPrevValue() ? asset.getPrevValue() : 0.0;

            if (asset.isHelper()) {
                binding.assetCard.setStrokeWidth(0); // Hide default stroke
                binding.assetCard.setBackgroundResource(R.drawable.bg_item_placeholder);
                binding.assetLetter.setText("+");
                binding.assetLetter.setTextColor(ContextCompat.getColor(context, R.color.text_4));
                if (bg != null) {
                    bg.setColor(ContextCompat.getColor(context, R.color.chip));
                }

                binding.assetName.setTextColor(ContextCompat.getColor(context, R.color.text_2));
                binding.assetValueContainer.setVisibility(View.GONE);
                binding.assetWeight.setVisibility(View.GONE);
                binding.assetSparkline.setVisibility(View.GONE);
                binding.assetChevron.setVisibility(View.VISIBLE);

                binding.assetCompactValue.setText(R.string.tap_to_add);
                binding.assetCompactValue.setTextColor(ContextCompat.getColor(context, R.color.text_4));
            } else {
                binding.assetValueContainer.setVisibility(View.VISIBLE);
                binding.assetChevron.setVisibility(View.GONE);
                binding.assetCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.bg_elev));
                binding.assetCard.setCardElevation(0f);
                binding.assetCard.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.border)));
                binding.assetCard.setStrokeWidth((int) (1 * context.getResources().getDisplayMetrics().density));

                String amount = Tools.formatAmount(asset.getValue());
                binding.assetValue.setText(privacyMode ? "****" : amount);

                // Change Row
                double change = asset.getValue() - prevVal;
                int color = ContextCompat.getColor(context, Tools.getTextChangeColor(change));
                String arrow = change >= 0 ? "↑ " : "↓ ";

                binding.assetCompactValue.setText(privacyMode ? "****" : Tools.formatAmount(change));
                binding.assetCompactValue.setTextColor(color);
                
                binding.assetChangePercent.setText(privacyMode ? "****" : arrow + Tools.formatPercent(Math.abs(Tools.getPercent(prevVal, asset.getValue()))));
                binding.assetChangePercent.setTextColor(color);

                // Weight Row
                boolean isAsset = asset.getValue() >= 0;
                double totalForCategory = isAsset ? totalAssets : totalLiabilities;
                double weight = totalForCategory > 0 ? (Math.abs(asset.getValue()) / totalForCategory) * 100.0 : 0.0;
                binding.assetWeight.setText(Tools.formatPercent(weight));

                // Sparkline Data — use same palette color as the letter icon
                if (privacyMode) {
                    binding.assetSparkline.setVisibility(View.INVISIBLE);
                    binding.assetSparkline.clear();
                } else {
                    binding.assetSparkline.setVisibility(View.VISIBLE);
                    // Optimization: check if hardware acceleration is enabled
                    if (!binding.assetSparkline.isHardwareAccelerated()) {
                        binding.assetSparkline.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    }
                    
                    final String key = asset.getName() + asset.getMonth() + asset.getYear();
                    List<Entry> cachedEntries = com.kanzar.networthtracker.helpers.SparklineHelper.getCachedData(asset.getName(), asset.getMonth(), asset.getYear());
                    
                    if (cachedEntries != null) {
                        // If cached, apply immediately and DON'T clear
                        applySparklineData(cachedEntries, itemColor, key);
                    } else {
                        // Only clear and load if we don't have it
                        binding.assetSparkline.clear();
                        loadSparklineDataAsync(asset, itemColor);
                    }
                }
            }
        }

        private void applySparklineData(List<Entry> entries, int color, String key) {
            boundAssetKey = key;
            LineDataSet dataSet = new LineDataSet(entries, "");
            dataSet.setColor(color);
            dataSet.setLineWidth(1.5f);
            dataSet.setDrawCircles(false);
            dataSet.setDrawValues(false);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setDrawFilled(true);
            dataSet.setFillAlpha(20);
            dataSet.setFillColor(color);
            LineData lineData = new LineData(dataSet);
            binding.assetSparkline.setData(lineData);
            binding.assetSparkline.invalidate();
        }

        private void loadSparklineDataAsync(Asset asset, int color) {
            final String name = asset.getName();
            final int month = asset.getMonth();
            final int year = asset.getYear();
            final String key = name + month + year;
            boundAssetKey = key;

            sparklineExecutor.execute(() -> {
                List<Entry> entries = com.kanzar.networthtracker.helpers.SparklineHelper.getSparklineData(name, month, year);

                mainHandler.post(() -> {
                    if (!Objects.equals(key, boundAssetKey)) return;
                    applySparklineData(entries, color, key);
                });
            });
        }
    }
}
