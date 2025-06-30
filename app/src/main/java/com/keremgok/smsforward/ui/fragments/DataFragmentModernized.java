package com.keremgok.smsforward.ui.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.keremgok.smsforward.LanguageManager;
import com.keremgok.smsforward.MessageStatsDbHelper;
import com.keremgok.smsforward.R;
import com.keremgok.smsforward.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * DataFragment - Modernized analytics and statistics view
 * Phase 3: UI Modernization with MPAndroidChart integration
 */
public class DataFragmentModernized extends Fragment {
    private static final String TAG = "DataFragmentModernized";

    private SharedViewModel sharedViewModel;
    
    // UI Components
    private TextView titleText;
    private TextView subtitleText;
    private TextView todayStatsNumber;
    private TextView successRateNumber;
    private TextView statsText;
    
    // Charts
    private LineChart dailyChart;
    private PieChart platformChart;
    
    // Loading states
    private View dailyChartLoading;
    private View dailyChartNoData;
    private View platformChartLoading;
    private View platformChartNoData;
    
    // Buttons
    private MaterialButton refreshStatsButton;
    private MaterialButton viewHistoryButton;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(LanguageManager.wrapContext(context));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Apply theme
        if (getActivity() != null) {
            ThemeManager.initializeTheme(getActivity());
        }

        // Inflate modernized layout
        View rootView = inflater.inflate(R.layout.fragment_data_modernized, container, false);
        
        // Initialize views
        setupViews(rootView);
        
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup ViewModel
        setupViewModel();
        
        // Setup charts
        setupCharts();
        
        // Load initial data
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Refresh data when fragment becomes visible
        if (sharedViewModel != null) {
            sharedViewModel.refreshAll();
        }
        
        // Reload chart data
        loadData();
    }

    private void setupViews(View rootView) {
        // Header
        titleText = rootView.findViewById(R.id.titleText);
        subtitleText = rootView.findViewById(R.id.subtitleText);
        
        // Stats cards
        todayStatsNumber = rootView.findViewById(R.id.todayStatsNumber);
        successRateNumber = rootView.findViewById(R.id.successRateNumber);
        statsText = rootView.findViewById(R.id.statsText);
        
        // Charts
        dailyChart = rootView.findViewById(R.id.dailyChart);
        platformChart = rootView.findViewById(R.id.platformChart);
        
        // Loading states
        dailyChartLoading = rootView.findViewById(R.id.dailyChartLoading);
        dailyChartNoData = rootView.findViewById(R.id.dailyChartNoData);
        platformChartLoading = rootView.findViewById(R.id.platformChartLoading);
        platformChartNoData = rootView.findViewById(R.id.platformChartNoData);
        
        // Buttons
        refreshStatsButton = rootView.findViewById(R.id.refreshStatsButton);
        viewHistoryButton = rootView.findViewById(R.id.viewHistoryButton);
        
        // Setup button listeners
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        if (refreshStatsButton != null) {
            refreshStatsButton.setOnClickListener(v -> {
                // Refresh all data
                loadData();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Statistics refreshed", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        if (viewHistoryButton != null) {
            viewHistoryButton.setOnClickListener(v -> {
                // Navigate to message history
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Message history functionality coming soon!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupViewModel() {
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        
        // Observe statistics
        sharedViewModel.getTodayStats().observe(getViewLifecycleOwner(), this::updateTodayStats);
        sharedViewModel.getTotalStats().observe(getViewLifecycleOwner(), this::updateTotalStats);
    }

    private void setupCharts() {
        setupDailyChart();
        setupPlatformChart();
    }

    private void setupDailyChart() {
        if (dailyChart == null) return;
        
        // Configure chart appearance
        dailyChart.setDrawGridBackground(false);
        dailyChart.setTouchEnabled(true);
        dailyChart.setDragEnabled(true);
        dailyChart.setScaleEnabled(false);
        dailyChart.setPinchZoom(false);
        
        // Remove description
        Description description = new Description();
        description.setText("");
        dailyChart.setDescription(description);
        
        // Configure X axis
        XAxis xAxis = dailyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        
        // Configure Y axes
        dailyChart.getAxisLeft().setDrawGridLines(true);
        dailyChart.getAxisLeft().setGranularity(1f);
        dailyChart.getAxisRight().setEnabled(false);
        
        // Configure legend
        dailyChart.getLegend().setEnabled(false);
    }

    private void setupPlatformChart() {
        if (platformChart == null) return;
        
        // Configure chart appearance
        platformChart.setUsePercentValues(true);
        platformChart.setDrawEntryLabels(false);
        platformChart.setDrawCenterText(true);
        platformChart.setCenterText("Platform\nDistribution");
        platformChart.setHoleRadius(40f);
        platformChart.setTransparentCircleRadius(45f);
        
        // Remove description
        Description description = new Description();
        description.setText("");
        platformChart.setDescription(description);
        
        // Configure legend
        platformChart.getLegend().setEnabled(true);
        platformChart.getLegend().setTextSize(12f);
    }

    private void loadData() {
        // Show loading states
        showChartLoading(true);
        
        // Simulate loading with delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            showChartLoading(false);
            
            // Load chart data
            loadDailyChartData();
            loadPlatformChartData();
            
            // Update detailed stats
            updateDetailedStats();
        }, 1500);
    }

    private void showChartLoading(boolean loading) {
        if (dailyChartLoading != null && dailyChart != null) {
            dailyChartLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            dailyChart.setVisibility(loading ? View.GONE : View.VISIBLE);
        }
        
        if (platformChartLoading != null && platformChart != null) {
            platformChartLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            platformChart.setVisibility(loading ? View.GONE : View.VISIBLE);
        }
    }

    private void loadDailyChartData() {
        if (dailyChart == null) return;
        
        // Generate sample data for last 7 days
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        
        // Sample data for demonstration
        int[] sampleData = {3, 7, 2, 8, 5, 9, 4};
        
        for (int i = 6; i >= 0; i--) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
            labels.add(sdf.format(cal.getTime()));
            entries.add(new Entry(6 - i, sampleData[6 - i]));
        }
        
        if (entries.isEmpty()) {
            showDailyChartNoData(true);
            return;
        }
        
        showDailyChartNoData(false);
        
        // Create dataset
        LineDataSet dataSet = new LineDataSet(entries, "Messages");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setCircleColor(Color.parseColor("#2196F3"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(12f);
        dataSet.setDrawValues(true);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        
        // Set data
        LineData lineData = new LineData(dataSet);
        dailyChart.setData(lineData);
        
        // Set labels
        dailyChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        dailyChart.getXAxis().setLabelCount(7);
        
        // Animate and refresh
        dailyChart.animateY(1000);
        dailyChart.invalidate();
    }

    private void loadPlatformChartData() {
        if (platformChart == null) return;
        
        // Sample platform distribution data
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(35f, "📱 SMS"));
        entries.add(new PieEntry(25f, "📢 Telegram"));
        entries.add(new PieEntry(25f, "📧 Email"));
        entries.add(new PieEntry(15f, "🌐 Webhook"));
        
        if (entries.isEmpty()) {
            showPlatformChartNoData(true);
            return;
        }
        
        showPlatformChartNoData(false);
        
        // Create dataset
        PieDataSet dataSet = new PieDataSet(entries, "");
        
        // Set colors
        int[] colors = {
            Color.parseColor("#2196F3"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#9C27B0")
        };
        dataSet.setColors(colors);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        
        // Set data
        PieData pieData = new PieData(dataSet);
        platformChart.setData(pieData);
        
        // Animate and refresh
        platformChart.animateY(1000);
        platformChart.invalidate();
    }

    private void showDailyChartNoData(boolean show) {
        if (dailyChartNoData != null && dailyChart != null) {
            dailyChartNoData.setVisibility(show ? View.VISIBLE : View.GONE);
            dailyChart.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showPlatformChartNoData(boolean show) {
        if (platformChartNoData != null && platformChart != null) {
            platformChartNoData.setVisibility(show ? View.VISIBLE : View.GONE);
            platformChart.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void updateTodayStats(MessageStatsDbHelper.DailyStats stats) {
        if (todayStatsNumber != null && stats != null) {
            todayStatsNumber.setText(String.valueOf(stats.totalCount));
        }
        updateSuccessRate();
    }

    private void updateTotalStats(MessageStatsDbHelper.TotalStats stats) {
        updateSuccessRate();
        updateDetailedStats();
    }

    private void updateSuccessRate() {
        if (successRateNumber == null || sharedViewModel == null) return;
        
        MessageStatsDbHelper.TotalStats totalStats = sharedViewModel.getTotalStats().getValue();
        if (totalStats != null && totalStats.totalCount > 0) {
            float successRate = ((float) totalStats.successCount / totalStats.totalCount) * 100;
            successRateNumber.setText(String.format(Locale.getDefault(), "%.1f%%", successRate));
        } else {
            successRateNumber.setText("0%");
        }
    }

    private void updateDetailedStats() {
        if (statsText == null || sharedViewModel == null) return;
        
        StringBuilder statsBuilder = new StringBuilder();
        
        MessageStatsDbHelper.DailyStats todayStats = sharedViewModel.getTodayStats().getValue();
        MessageStatsDbHelper.TotalStats totalStats = sharedViewModel.getTotalStats().getValue();
        
        if (todayStats != null) {
            statsBuilder.append("📅 Today's Messages:\n");
            statsBuilder.append("   Total: ").append(todayStats.totalCount).append("\n");
            statsBuilder.append("   Success: ").append(todayStats.successCount).append("\n");
            statsBuilder.append("   Failed: ").append(todayStats.failedCount).append("\n\n");
        }
        
        if (totalStats != null) {
            statsBuilder.append("📊 All Time Statistics:\n");
            statsBuilder.append("   Total Messages: ").append(totalStats.totalCount).append("\n");
            statsBuilder.append("   Successful: ").append(totalStats.successCount).append("\n");
            statsBuilder.append("   Failed: ").append(totalStats.failedCount).append("\n");
            
            if (totalStats.totalCount > 0) {
                float successRate = ((float) totalStats.successCount / totalStats.totalCount) * 100;
                statsBuilder.append("   Success Rate: ").append(String.format(Locale.getDefault(), "%.2f%%", successRate)).append("\n");
            }
        }
        
        if (statsBuilder.length() == 0) {
            statsBuilder.append("No statistics available yet.\nStart forwarding messages to see data here.");
        }
        
        statsText.setText(statsBuilder.toString());
    }
} 