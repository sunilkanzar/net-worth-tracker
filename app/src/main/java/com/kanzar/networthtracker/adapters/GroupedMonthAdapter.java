package com.kanzar.networthtracker.adapters;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GroupedMonthAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MONTH = 1;

    private final List<Object> displayItems = new ArrayList<>();
    private final List<YearGroup> groups;
    private final Set<String> collapsedYears = new HashSet<>();
    private final MonthAdapter.OnItemClickListener listener;
    private final Map<Month, Month> prevMonthMap = new HashMap<>();

    public static class YearGroup {
        public String yearLabel;
        public List<Month> months = new ArrayList<>();
        
        // Pre-calculated stats
        public double latestValue;
        public double openingValue;
        public double min = Double.MAX_VALUE;
        public double max = -Double.MAX_VALUE;
        public double change;
        public double percent;

        public YearGroup(String yearLabel) {
            this.yearLabel = yearLabel;
        }

        public void calculateStats(Month lastOfPrev) {
            if (months.isEmpty()) {
                latestValue = 0;
                openingValue = 0;
                min = 0;
                max = 0;
                change = 0;
                percent = 0;
                return;
            }

            latestValue = months.get(0).getValue();
            openingValue = lastOfPrev != null ? lastOfPrev.getValue() : 0;
            
            change = latestValue - openingValue;
            percent = Tools.getPercent(openingValue, latestValue);
            
            for (Month m : months) {
                double val = m.getValue();
                if (val < min) min = val;
                if (val > max) max = val;
            }
        }
    }

    public GroupedMonthAdapter(List<YearGroup> groups, MonthAdapter.OnItemClickListener listener) {
        this.groups = groups;
        this.listener = listener;
        
        preCalculate();
        
        // Collapse all except the first year by default
        if (groups != null && groups.size() > 1) {
            for (int i = 1; i < groups.size(); i++) {
                collapsedYears.add(groups.get(i).yearLabel);
            }
        }
        
        flatten();
    }

    private void preCalculate() {
        if (groups == null) return;
        
        for (int i = 0; i < groups.size(); i++) {
            YearGroup group = groups.get(i);
            Month lastOfPrev = null;
            
            // The list is sorted descending (latest month first)
            // So months in a group are [Mar, Feb, Jan]
            // Next group's first month is the month before this group's last month
            if (i + 1 < groups.size()) {
                lastOfPrev = groups.get(i + 1).months.get(0);
            }
            
            group.calculateStats(lastOfPrev);

            // Populate prevMonthMap for $O(1)$ lookup during bind
            for (int j = 0; j < group.months.size(); j++) {
                Month current = group.months.get(j);
                Month previous;
                if (j + 1 < group.months.size()) {
                    previous = group.months.get(j + 1);
                } else if (i + 1 < groups.size()) {
                    previous = groups.get(i + 1).months.get(0);
                } else {
                    previous = new Month(0, 0); // Sentinel for "no previous data"
                }
                prevMonthMap.put(current, previous);
            }
        }
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
            Month lastMonth = prevMonthMap.get(currentMonth);
            if (lastMonth == null) lastMonth = new Month(0, 0);
            ((MonthViewHolder) holder).bind(currentMonth, lastMonth);
        }
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
            
            int accentColor = ContextCompat.getColor(itemView.getContext(), Tools.getAccentColor());
            binding.yearTitle.setTextColor(accentColor);
            
            binding.tvClosing.setText(Tools.formatAmount(group.latestValue, true));
            
            String changeStr = String.format(Locale.getDefault(), "%s%s (%s%.1f%%)", 
                    group.change >= 0 ? "+" : "-",
                    Tools.formatCompact(Math.abs(group.change), false),
                    group.change >= 0 ? "+" : "",
                    group.percent);
            binding.tvChange.setText(changeStr);
            binding.tvChange.setTextColor(ContextCompat.getColor(itemView.getContext(), 
                    group.change >= 0 ? R.color.positive : R.color.negative));

            binding.tvOpening.setText(itemView.getContext().getString(R.string.month_opening_value, 
                    Tools.formatCompact(group.openingValue, true)));

            binding.tvRange.setText(itemView.getContext().getString(R.string.month_range_label, 
                    Tools.formatAmount(group.min), 
                    Tools.formatAmount(group.max)));

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
                notifyItemChanged(pos);
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
