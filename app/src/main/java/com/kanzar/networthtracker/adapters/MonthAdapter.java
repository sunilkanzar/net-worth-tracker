package com.kanzar.networthtracker.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.databinding.ItemMonthBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.views.PercentView;
import java.util.List;

public class MonthAdapter extends RecyclerView.Adapter<MonthAdapter.ViewHolder> {

    private final List<Month> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Month item);
        void onItemLongClick(Month item);
    }

    public MonthAdapter(List<Month> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMonthBinding binding = ItemMonthBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Month currentMonth = items.get(position);
        Month lastMonth;
        if (position == items.size() - 1) {
            lastMonth = new Month(0, 0); // Sentinel or initial month
        } else {
            lastMonth = items.get(position + 1);
        }

        holder.bind(currentMonth, lastMonth);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentMonth);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(currentMonth);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMonthBinding binding;

        public ViewHolder(ItemMonthBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Month month, Month lastMonth) {
            binding.monthName.setText(month.toString());
            binding.monthValue.setText(Tools.formatAmount(month.getValue(), true));
            
            binding.percentView.init(lastMonth.getValue(), month.getValue());
            binding.percentView.fillValueChange(binding.monthValueChange, true);
            binding.percentView.fillPercent(binding.monthPercentage);
        }
    }
}
