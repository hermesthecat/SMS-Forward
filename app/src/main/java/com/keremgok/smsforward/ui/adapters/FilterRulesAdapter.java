package com.keremgok.smsforward.ui.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.keremgok.smsforward.AdvancedContentFilter;
import com.keremgok.smsforward.R;

import java.util.List;

/**
 * Adapter for displaying filter rules in RecyclerView
 */
public class FilterRulesAdapter extends RecyclerView.Adapter<FilterRulesAdapter.FilterRuleViewHolder> {
    
    private final List<AdvancedContentFilter.FilterRule> filterRules;
    private final OnFilterActionListener listener;
    
    public interface OnFilterActionListener {
        void onToggleFilter(AdvancedContentFilter.FilterRule rule, boolean enabled);
        void onEditFilter(AdvancedContentFilter.FilterRule rule);
        void onDeleteFilter(AdvancedContentFilter.FilterRule rule);
    }
    
    public FilterRulesAdapter(List<AdvancedContentFilter.FilterRule> filterRules, OnFilterActionListener listener) {
        this.filterRules = filterRules;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public FilterRuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_filter_rule, parent, false);
        return new FilterRuleViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FilterRuleViewHolder holder, int position) {
        AdvancedContentFilter.FilterRule rule = filterRules.get(position);
        holder.bind(rule, listener);
    }
    
    @Override
    public int getItemCount() {
        return filterRules.size();
    }
    
    static class FilterRuleViewHolder extends RecyclerView.ViewHolder {
        private final TextView filterName;
        private final MaterialSwitch filterEnabledSwitch;
        private final Chip filterTypeChip;
        private final Chip filterActionChip;
        private final Chip filterCategoryChip;
        private final TextView filterPattern;
        private final TextView filterDescription;
        private final TextView filterCreatedDate;
        private final MaterialButton filterEditButton;
        private final MaterialButton filterDeleteButton;
        
        public FilterRuleViewHolder(@NonNull View itemView) {
            super(itemView);
            filterName = itemView.findViewById(R.id.filter_name);
            filterEnabledSwitch = itemView.findViewById(R.id.filter_enabled_switch);
            filterTypeChip = itemView.findViewById(R.id.filter_type_chip);
            filterActionChip = itemView.findViewById(R.id.filter_action_chip);
            filterCategoryChip = itemView.findViewById(R.id.filter_category_chip);
            filterPattern = itemView.findViewById(R.id.filter_pattern);
            filterDescription = itemView.findViewById(R.id.filter_description);
            filterCreatedDate = itemView.findViewById(R.id.filter_created_date);
            filterEditButton = itemView.findViewById(R.id.filter_edit_button);
            filterDeleteButton = itemView.findViewById(R.id.filter_delete_button);
        }
        
        public void bind(AdvancedContentFilter.FilterRule rule, OnFilterActionListener listener) {
            Context context = itemView.getContext();
            
            // Basic info
            filterName.setText(rule.getName());
            filterEnabledSwitch.setChecked(rule.isEnabled());
            
            // Type and action chips
            filterTypeChip.setText(rule.getTypeDisplayName());
            filterActionChip.setText(rule.getActionDisplayName());
            filterCategoryChip.setText(rule.getCategory());
            
            // Pattern
            filterPattern.setText(rule.getPattern());
            
            // Description (show/hide based on availability)
            if (rule.getDescription() != null && !rule.getDescription().trim().isEmpty()) {
                filterDescription.setText(rule.getDescription());
                filterDescription.setVisibility(View.VISIBLE);
            } else {
                filterDescription.setVisibility(View.GONE);
            }
            
            // Created date
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    rule.getCreatedAt(),
                    System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS);
            filterCreatedDate.setText(context.getString(R.string.filter_created_format, relativeTime));
            
            // Color code chips based on action
            switch (rule.getAction()) {
                case BLOCK:
                    filterActionChip.setChipBackgroundColorResource(android.R.color.holo_red_light);
                    break;
                case SKIP:
                    filterActionChip.setChipBackgroundColorResource(android.R.color.holo_orange_light);
                    break;
                case TAG:
                    filterActionChip.setChipBackgroundColorResource(android.R.color.holo_blue_light);
                    break;
            }
            
            // Event listeners
            filterEnabledSwitch.setOnCheckedChangeListener((button, isChecked) -> {
                if (listener != null) {
                    listener.onToggleFilter(rule, isChecked);
                }
            });
            
            filterEditButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditFilter(rule);
                }
            });
            
            filterDeleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteFilter(rule);
                }
            });
        }
    }
} 