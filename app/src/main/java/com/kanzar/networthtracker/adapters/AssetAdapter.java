package com.kanzar.networthtracker.adapters;

import android.content.Context;
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
import java.util.List;

public class AssetAdapter extends RecyclerView.Adapter<AssetAdapter.ViewHolder> {

    private final Context context;
    private List<? extends Asset> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Asset item);
        void onItemLongClick(Asset item);
    }

    public AssetAdapter(Context context) {
        this.context = context;
        this.listener = (OnItemClickListener) context;
    }

    public AssetAdapter(List<? extends Asset> items, Context context, OnItemClickListener listener) {
        this.items = items;
        this.context = context;
        this.listener = listener;
    }

    public List<Asset> getItems() {
        return (List<Asset>) items;
    }

    public void setItems(List<? extends Asset> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asset, parent, false);
        return new ViewHolder(v);
    }

    private double getTotalAssets() {
        double sum = 0.0;
        if (items != null) {
            for (Asset asset : items) {
                if (!asset.isHelper() && asset.getValue() > 0) {
                    sum += asset.getValue();
                }
            }
        }
        return sum;
    }

    private double getTotalLiabilities() {
        double sum = 0.0;
        if (items != null) {
            for (Asset asset : items) {
                if (!asset.isHelper() && asset.getValue() < 0) {
                    sum += asset.getValue();
                }
            }
        }
        return Math.abs(sum);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Asset asset = items.get(position);
        holder.bind(asset, context, getTotalAssets(), getTotalLiabilities());

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

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView assetName;
        private final TextView assetValue;
        private final TextView assetWeight;
        private final TextView assetChangeValue;
        private final TextView assetChangePercent;
        private final LineChart assetSparkline;
        private final android.view.ViewGroup container;

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
                container.setBackgroundColor(ContextCompat.getColor(context, R.color.backgroundHelper));
                assetValue.setVisibility(View.GONE);
                assetWeight.setVisibility(View.GONE);
                assetChangeValue.setVisibility(View.VISIBLE);
                assetChangePercent.setVisibility(View.GONE);
                assetSparkline.setVisibility(View.GONE);

                String helperText = context.getString(R.string.helper_text, asset.getName());
                assetChangeValue.setText(helperText);
                assetChangeValue.setTextColor(ContextCompat.getColor(context, R.color.text));
            } else {
                container.setBackgroundColor(ContextCompat.getColor(context, R.color.backgroundContent));
                assetValue.setVisibility(View.VISIBLE);
                assetWeight.setVisibility(View.VISIBLE);
                assetChangeValue.setVisibility(View.VISIBLE);
                assetChangePercent.setVisibility(View.VISIBLE);
                assetSparkline.setVisibility(View.VISIBLE);

                // Change Row
                double change = asset.getValue() - prevVal;
                int color = ContextCompat.getColor(context, Tools.getTextChangeColor(change));
                
                assetValue.setText(Tools.formatAmount(asset.getValue()));
                assetChangeValue.setText((change >= 0 ? "+" : "") + Tools.formatAmount(change));
                assetChangeValue.setTextColor(color);
                
                assetChangePercent.setText(Tools.formatPercent(Tools.getPercent(prevVal, asset.getValue())));
                assetChangePercent.setTextColor(color);
                assetChangePercent.getBackground().setTint(Tools.adjustAlpha(color, 0.1f));

                // Weight Row
                boolean isAsset = asset.getValue() >= 0;
                double totalForCategory = isAsset ? totalAssets : totalLiabilities;
                double weight = totalForCategory > 0 ? (Math.abs(asset.getValue()) / totalForCategory) * 100.0 : 0.0;
                assetWeight.setText(Tools.formatPercent(weight));
                assetWeight.setBackgroundResource(isAsset ? R.drawable.bg_pill_asset : R.drawable.bg_pill_liability);
                assetWeight.setTextColor(ContextCompat.getColor(context, isAsset ? R.color.colorAccent : R.color.negative));

                // Sparkline Data
                int chartColor = ContextCompat.getColor(context, R.color.colorAccent);
                loadSparklineData(asset, chartColor);
            }
        }

        private void loadSparklineData(Asset asset, int color) {
            List<Entry> entries = new ArrayList<>();
            Month m = new Month(asset.getMonth(), asset.getYear());
            
            // Get last 12 months
            for (int i = 0; i < 12; i++) {
                Asset historical = Realm.getDefaultInstance().where(Asset.class)
                        .equalTo("name", asset.getName())
                        .equalTo("month", m.getMonth())
                        .equalTo("year", m.getYear())
                        .findFirst();
                
                entries.add(new Entry(11 - i, (float) (historical != null ? historical.getValue() : 0.0)));
                m.previous();
            }
            
            Collections.reverse(entries);
            // Re-index x to 0-11
            for(int i=0; i<entries.size(); i++) {
                entries.get(i).setX(i);
            }

            LineDataSet dataSet = new LineDataSet(entries, "");
            dataSet.setColor(color);
            dataSet.setLineWidth(2f);
            dataSet.setDrawCircles(false);
            dataSet.setDrawValues(false);
            dataSet.setMode(LineDataSet.Mode.LINEAR);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(color);
            dataSet.setFillAlpha(30);

            assetSparkline.setData(new LineData(dataSet));
            assetSparkline.invalidate();
        }
    }
}
