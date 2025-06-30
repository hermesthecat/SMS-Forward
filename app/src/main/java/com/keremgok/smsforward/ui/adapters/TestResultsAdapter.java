package com.keremgok.smsforward.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.keremgok.smsforward.R;

import java.util.List;

/**
 * Adapter for displaying filter test results in RecyclerView
 */
public class TestResultsAdapter extends RecyclerView.Adapter<TestResultsAdapter.TestResultViewHolder> {

    // Inner class for test result data (moved here for simplicity)
    public static class TestResult {
        public final String icon;
        public final String filterName;
        public final String action;
        public final String reason;

        public TestResult(String icon, String filterName, String action, String reason) {
            this.icon = icon;
            this.filterName = filterName;
            this.action = action;
            this.reason = reason;
        }
    }

    private final List<TestResult> testResults;

    public TestResultsAdapter(List<TestResult> testResults) {
        this.testResults = testResults;
    }

    @NonNull
    @Override
    public TestResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_filter_test_result, parent, false);
        return new TestResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TestResultViewHolder holder, int position) {
        TestResult result = testResults.get(position);
        holder.bind(result);
    }

    @Override
    public int getItemCount() {
        return testResults.size();
    }

    static class TestResultViewHolder extends RecyclerView.ViewHolder {
        private final TextView resultIcon;
        private final TextView filterName;
        private final Chip actionChip;
        private final TextView matchReason;

        public TestResultViewHolder(@NonNull View itemView) {
            super(itemView);
            resultIcon = itemView.findViewById(R.id.result_icon);
            filterName = itemView.findViewById(R.id.filter_name);
            actionChip = itemView.findViewById(R.id.action_chip);
            matchReason = itemView.findViewById(R.id.match_reason);
        }

        public void bind(TestResult result) {
            resultIcon.setText(result.icon);
            filterName.setText(result.filterName);
            actionChip.setText(result.action);
            matchReason.setText(result.reason);

            // Color code the action chip based on action type
            if (result.action.contains("BLOCK") || result.action.contains("Block")) {
                actionChip.setChipBackgroundColorResource(android.R.color.holo_red_light);
            } else if (result.action.contains("SKIP") || result.action.contains("Skip")) {
                actionChip.setChipBackgroundColorResource(android.R.color.holo_orange_light);
            } else if (result.action.contains("TAG") || result.action.contains("Tag")) {
                actionChip.setChipBackgroundColorResource(android.R.color.holo_blue_light);
            } else {
                actionChip.setChipBackgroundColorResource(android.R.color.holo_green_light);
            }
        }
    }
} 