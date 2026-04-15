package com.kanzar.networthtracker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.views.PercentView;
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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Asset asset = items.get(position);
        holder.bind(asset, context);

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
        private final TextView assetValueChange;
        private final PercentView percentView;
        private final RelativeLayout container;

        public ViewHolder(View v) {
            super(v);
            assetName = v.findViewById(R.id.assetName);
            assetValue = v.findViewById(R.id.assetValue);
            assetValueChange = v.findViewById(R.id.assetValueChange);
            percentView = v.findViewById(R.id.percentView);
            container = v.findViewById(R.id.container);
        }

        public void bind(Asset asset, Context context) {
            assetName.setText(asset.getName());
            assetValue.setText(Tools.formatAmount(asset.getValue()));

            Asset previous = asset.getPrevious();
            percentView.init(previous != null ? previous.getValue() : 0.0, asset.getValue());
            percentView.fillValueChange(assetValueChange);

            if (asset.isHelper()) {
                container.setBackgroundColor(ContextCompat.getColor(context, R.color.backgroundHelper));
                percentView.setVisibility(View.GONE);
                assetValue.setVisibility(View.GONE);
                assetValueChange.setVisibility(View.VISIBLE);

                String helperText = context.getString(R.string.helper_text, asset.getName());
                assetValueChange.setText(helperText);
                assetValueChange.setTextColor(ContextCompat.getColor(context, R.color.text));
            } else {
                container.setBackgroundColor(ContextCompat.getColor(context, R.color.backgroundContent));
                percentView.setVisibility(View.VISIBLE);
                assetValue.setVisibility(View.VISIBLE);
            }
        }
    }
}
