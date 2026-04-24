package com.kanzar.networthtracker.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.databinding.ItemMonthBinding;
import com.kanzar.networthtracker.databinding.ItemYearHeaderBinding;
import com.kanzar.networthtracker.helpers.Month;
import com.kanzar.networthtracker.helpers.Tools;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GroupedMonthAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MONTH = 1;

    private final List<Object> displayItems = new ArrayList<>();
    private final List<YearGroup> groups;
    private final Set<String> collapsedYears = new HashSet<>();
    private final MonthAdapter.OnItemClickListener listener;

    public static class YearGroup {
        public String yearLabel;
        public List<Month> months = new ArrayList<>();

        public YearGroup(String yearLabel) {
            this.yearLabel = yearLabel;
        }
    }

    public GroupedMonthAdapter(List<YearGroup> groups, MonthAdapter.OnItemClickListener listener) {
        this.groups = groups;
        this.listener = listener;
        
        // Collapse all except the first year by default
        if (groups != null && groups.size() > 1) {
            for (int i = 1; i < groups.size(); i++) {
                collapsedYears.add(groups.get(i).yearLabel);
            }
        }
        
        flatten();
    }

    private void flatten() {
        displayItems.clear();
        for (YearGroup group : groups) {
            displayItems.add(group);
            if (!collapsedYears.contains(group.yearLabel)) {
                displayItems.addAll(group.months);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position) instanceof YearGroup ? TYPE_HEADER : TYPE_MONTH;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            ItemYearHeaderBinding binding = ItemYearHeaderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new HeaderViewHolder(binding);
        } else {
            ItemMonthBinding binding = ItemMonthBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new MonthViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = displayItems.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((YearGroup) item);
        } else if (holder instanceof MonthViewHolder) {
            Month currentMonth = (Month) item;
            Month lastMonth = findLastMonth(currentMonth);
            ((MonthViewHolder) holder).bind(currentMonth, lastMonth);
        }
    }

    private Month findLastMonth(Month current) {
        if (current == null) return new Month(0, 0);
        for (YearGroup group : groups) {
            for (int i = 0; i < group.months.size(); i++) {
                if (group.months.get(i) == current) {
                    if (i + 1 < group.months.size()) {
                        return group.months.get(i + 1);
                    } else {
                        int groupIdx = groups.indexOf(group);
                        if (groupIdx + 1 < groups.size()) {
                            return groups.get(groupIdx + 1).months.get(0);
                        }
                    }
                }
            }
        }
        return new Month(0, 0);
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemYearHeaderBinding binding;

        public HeaderViewHolder(ItemYearHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(YearGroup group) {
            binding.yearTitle.setText(group.yearLabel);
            
            // Set FY title to accent color
            int accentColor = ContextCompat.getColor(itemView.getContext(), Tools.getAccentColor());
            binding.yearTitle.setTextColor(accentColor);
            
            double latestValue = group.months.isEmpty() ? 0 : group.months.get(0).getValue();
            Month lastOfPrev = findLastMonth(group.months.isEmpty() ? null : group.months.get(group.months.size()-1));
            double openingValue = lastOfPrev.getValue();
            
            double change = latestValue - openingValue;
            double percent = Tools.getPercent(openingValue, latestValue);
            
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (Month m : group.months) {
                double val = m.getValue();
                if (val < min) min = val;
                if (val > max) max = val;
            }
            if (group.months.isEmpty()) {
                min = 0; max = 0;
            }

            binding.tvClosing.setText(Tools.formatAmount(latestValue, true));
            
            String changeStr = String.format(Locale.getDefault(), "%s%s (%s%.1f%%)", 
                    change >= 0 ? "+" : "-",
                    Tools.formatCompact(Math.abs(change), false),
                    change >= 0 ? "+" : "",
                    percent);
            binding.tvChange.setText(changeStr);
            binding.tvChange.setTextColor(ContextCompat.getColor(itemView.getContext(), 
                    change >= 0 ? R.color.positive : R.color.negative));

            binding.tvOpening.setText("Open: " + Tools.formatCompact(openingValue, true));

            binding.tvRange.setText(String.format("Low %s · High %s", 
                    Tools.formatAmount(min), 
                    Tools.formatAmount(max)));

            boolean isCollapsed = collapsedYears.contains(group.yearLabel);
            binding.expandIcon.animate().rotation(isCollapsed ? 0 : 180).setDuration(200).start();

            binding.yearHeader.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                if (isCollapsed) {
                    collapsedYears.remove(group.yearLabel);
                    flatten();
                    notifyItemRangeInserted(pos + 1, group.months.size());
                } else {
                    collapsedYears.add(group.yearLabel);
                    flatten();
                    notifyItemRangeRemoved(pos + 1, group.months.size());
                }
                notifyItemChanged(pos); // Update arrow and state
            });
        }
    }

    class MonthViewHolder extends RecyclerView.ViewHolder {
        private final ItemMonthBinding binding;

        public MonthViewHolder(ItemMonthBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Month month, Month lastMonth) {
            binding.monthName.setText(month.toStringMMMYY());
            binding.monthValue.setText(Tools.formatAmount(month.getValue(), true));
            binding.percentView.init(lastMonth.getValue(), month.getValue());
            binding.percentView.fillValueChange(binding.monthValueChange, true);
            binding.percentView.fillPercent(binding.monthPercentage);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(month);
            });
        }
    }
}
