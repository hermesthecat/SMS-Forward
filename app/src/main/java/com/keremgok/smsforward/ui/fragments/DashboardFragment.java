package com.keremgok.smsforward.ui.fragments;

import android.content.Context;
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
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.keremgok.smsforward.HelpManager;
import com.keremgok.smsforward.LanguageManager;
import com.keremgok.smsforward.MainActivity;
import com.keremgok.smsforward.MessageStatsDbHelper;
import com.keremgok.smsforward.NetworkStatusManager;
import com.keremgok.smsforward.R;
import com.keremgok.smsforward.SecurityManager;
import com.keremgok.smsforward.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Interactive Dashboard Fragment - Phase 4: Enhanced Features
 * Features: SwipeRefresh, Auto-refresh, Enhanced quick actions, Live indicators
 */
public class DashboardFragment extends Fragment implements NetworkStatusManager.NetworkStatusListener {
    private static final String TAG = "DashboardFragment";
    private static final int AUTO_REFRESH_INTERVAL_MS = 30000; // 30 seconds

    private NetworkStatusManager networkStatusManager;
    private SharedViewModel sharedViewModel;
    private SecurityManager securityManager;
    private HelpManager helpManager;
    
    // Auto-refresh components
    private Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;
    private boolean isAutoRefreshEnabled = true;
    private long lastUpdateTime = 0;
    
    // UI Components
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialSwitch autoRefreshSwitch;
    private TextView lastUpdateText;
    private TextView welcomeText;
    private TextView statusText;
    private TextView todayStatsCount;
    private TextView totalStatsCount;
    private MaterialCardView networkStatusCard;
    private TextView networkStatusIcon;
    private TextView networkStatusTitle;
    private TextView networkStatusDescription;
    private CircularProgressIndicator networkLiveIndicator;
    private MaterialCardView securityStatusCard;
    private TextView securityStatusIcon;
    private TextView securityStatusTitle;
    private TextView securityStatusDescription;
    private MaterialButton testMessageButton;
    private MaterialButton refreshButton;
    private MaterialButton viewStatsButton;
    private MaterialButton viewHistoryButton;
    private MaterialButton helpButton;
    private View loadingStateContainer;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(LanguageManager.wrapContext(context));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Apply theme before creating view
        if (getActivity() != null) {
            ThemeManager.initializeTheme(getActivity());
        }

        // Inflate interactive dashboard layout
        View rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);
        
        // Initialize views
        setupViews(rootView);
        
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize managers
        if (getContext() != null) {
            networkStatusManager = NetworkStatusManager.getInstance(getContext());
            securityManager = new SecurityManager(getContext());
            helpManager = new HelpManager(getContext());
        }
        
        // Setup ViewModel
        setupViewModel();
        
        // Setup auto-refresh
        setupAutoRefresh();
        
        // Setup initial data
        setupInitialData();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Add network status listener
        if (networkStatusManager != null) {
            networkStatusManager.addListener(this);
        }
        
        // Refresh data when fragment becomes visible
        refreshAllData();
        
        // Start auto-refresh if enabled
        startAutoRefreshIfEnabled();
    }

    @Override
    public void onPause() {
        super.onPause();
        
        // Remove network status listener
        if (networkStatusManager != null) {
            networkStatusManager.removeListener(this);
        }
        
        // Stop auto-refresh
        stopAutoRefresh();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Cleanup
        stopAutoRefresh();
        if (networkStatusManager != null) {
            networkStatusManager.removeListener(this);
        }
    }

    @Override
    public void onNetworkStatusChanged(boolean isConnected, String connectionType) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                updateNetworkStatusCard(isConnected);
                showNetworkStatusChange(isConnected, connectionType);
            });
        }
    }

    private void setupViews(View rootView) {
        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::onSwipeRefresh);
        swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorSecondary,
            R.color.colorTertiary
        );
        
        // Auto-refresh controls
        autoRefreshSwitch = rootView.findViewById(R.id.autoRefreshSwitch);
        lastUpdateText = rootView.findViewById(R.id.lastUpdateText);
        
        // Main UI components
        welcomeText = rootView.findViewById(R.id.welcomeText);
        statusText = rootView.findViewById(R.id.statusText);
        todayStatsCount = rootView.findViewById(R.id.todayStatsCount);
        totalStatsCount = rootView.findViewById(R.id.totalStatsCount);
        
        // Network status card
        networkStatusCard = rootView.findViewById(R.id.networkStatusCard);
        networkStatusIcon = rootView.findViewById(R.id.networkStatusIcon);
        networkStatusTitle = rootView.findViewById(R.id.networkStatusTitle);
        networkStatusDescription = rootView.findViewById(R.id.networkStatusDescription);
        networkLiveIndicator = rootView.findViewById(R.id.networkLiveIndicator);
        
        // Security status card
        securityStatusCard = rootView.findViewById(R.id.securityStatusCard);
        securityStatusIcon = rootView.findViewById(R.id.securityStatusIcon);
        securityStatusTitle = rootView.findViewById(R.id.securityStatusTitle);
        securityStatusDescription = rootView.findViewById(R.id.securityStatusDescription);
        
        // Enhanced action buttons
        testMessageButton = rootView.findViewById(R.id.testMessageButton);
        refreshButton = rootView.findViewById(R.id.refreshButton);
        viewStatsButton = rootView.findViewById(R.id.viewStatsButton);
        viewHistoryButton = rootView.findViewById(R.id.viewHistoryButton);
        
        // Help button
        helpButton = rootView.findViewById(R.id.helpButton);
        
        // Loading state
        loadingStateContainer = rootView.findViewById(R.id.loadingStateContainer);
        
        // Setup button listeners
        setupButtonListeners();
        
        // Setup auto-refresh switch
        setupAutoRefreshSwitch();
    }

    private void setupViewModel() {
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        
        // Observe network status
        sharedViewModel.getIsConnected().observe(getViewLifecycleOwner(), this::updateNetworkStatus);
        sharedViewModel.getConnectionType().observe(getViewLifecycleOwner(), this::updateConnectionType);
        
        // Observe statistics
        sharedViewModel.getTodayStats().observe(getViewLifecycleOwner(), this::updateTodayStats);
        sharedViewModel.getTotalStats().observe(getViewLifecycleOwner(), this::updateTotalStats);
        
        // Observe security status
        sharedViewModel.getSecurityEnabled().observe(getViewLifecycleOwner(), this::updateSecurityStatus);
    }

    private void setupAutoRefresh() {
        autoRefreshHandler = new Handler(Looper.getMainLooper());
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAutoRefreshEnabled && isResumed()) {
                    refreshAllData();
                    scheduleNextAutoRefresh();
                }
            }
        };
    }

    private void setupAutoRefreshSwitch() {
        if (autoRefreshSwitch != null) {
            autoRefreshSwitch.setChecked(isAutoRefreshEnabled);
            autoRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isAutoRefreshEnabled = isChecked;
                if (isChecked) {
                    startAutoRefreshIfEnabled();
                    showSnackbar(getString(R.string.dashboard_auto_refresh_enabled));
                } else {
                    stopAutoRefresh();
                    showSnackbar(getString(R.string.dashboard_auto_refresh_disabled));
                }
                updateAutoRefreshSwitchText();
            });
        }
        updateAutoRefreshSwitchText();
    }

    private void setupButtonListeners() {
        if (testMessageButton != null) {
            testMessageButton.setOnClickListener(v -> {
                // Call MainActivity's test message functionality
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    // Get the SettingsFragment and call its test message method
                    // We'll implement this by showing a toast for now
                    showSnackbar("Test message functionality - Implementation needed");
                }
            });
        }
        
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> {
                refreshAllData();
                showSnackbar(getString(R.string.dashboard_refreshing));
            });
        }
        
        if (viewStatsButton != null) {
            viewStatsButton.setOnClickListener(v -> {
                // Navigate to Data tab (index 4 in bottom navigation)
                navigateToTab(R.id.nav_data);
            });
        }
        
        if (viewHistoryButton != null) {
            viewHistoryButton.setOnClickListener(v -> {
                // Navigate to Data tab and show history
                navigateToTab(R.id.nav_data);
                showSnackbar("Message history available in Data tab");
            });
        }
        
        // Help button listener
        if (helpButton != null) {
            helpButton.setOnClickListener(v -> {
                if (helpManager != null) {
                    helpManager.showHelpAlways(HelpManager.HelpType.DASHBOARD);
                }
            });
        }
    }

    private void setupInitialData() {
        // Show loading state initially
        showLoadingState(true);
        
        // Hide loading after 1 second to simulate loading
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            showLoadingState(false);
            updateAllViews();
            updateLastUpdateTime();
        }, 1000);
    }
    
    private void startAutoRefreshIfEnabled() {
        if (isAutoRefreshEnabled && autoRefreshHandler != null && autoRefreshRunnable != null) {
            scheduleNextAutoRefresh();
        }
    }
    
    private void scheduleNextAutoRefresh() {
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
            autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL_MS);
        }
    }
    
    private void stopAutoRefresh() {
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }
    
    private void onSwipeRefresh() {
        refreshAllData();
        
        // Hide refresh indicator after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 1000);
    }
    
    private void refreshAllData() {
        // Show live indicator temporarily
        showLiveIndicator(true);
        
        // Refresh shared view model data
        if (sharedViewModel != null) {
            sharedViewModel.refreshAll();
        }
        
        // Update network status
        if (networkStatusManager != null) {
            networkStatusManager.updateNetworkStatus();
        }
        
        // Update last update time
        updateLastUpdateTime();
        
        // Hide live indicator after refresh
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            showLiveIndicator(false);
        }, 1500);
    }
    
    private void showLoadingState(boolean show) {
        if (loadingStateContainer != null) {
            loadingStateContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    private void showLiveIndicator(boolean show) {
        if (networkLiveIndicator != null) {
            networkLiveIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void updateNetworkStatus(Boolean isConnected) {
        updateNetworkStatusCard(isConnected);
        updateAllViews();
    }
    
    private void updateConnectionType(String connectionType) {
        updateNetworkStatusCard(sharedViewModel.getIsConnected().getValue());
        updateAllViews();
    }
    
    private void updateTodayStats(MessageStatsDbHelper.DailyStats stats) {
        if (todayStatsCount != null && stats != null) {
            todayStatsCount.setText(String.valueOf(stats.totalCount));
        }
    }
    
    private void updateTotalStats(MessageStatsDbHelper.TotalStats stats) {
        if (totalStatsCount != null && stats != null) {
            totalStatsCount.setText(String.valueOf(stats.totalCount));
        }
    }
    
    private void updateSecurityStatus(Boolean isSecurityEnabled) {
        updateSecurityStatusCard(isSecurityEnabled);
    }

    private void updateNetworkStatusCard(Boolean isConnected) {
        if (networkStatusIcon == null || networkStatusTitle == null || networkStatusDescription == null) {
            return;
        }
        
        String connectionType = sharedViewModel != null ? sharedViewModel.getConnectionType().getValue() : "Unknown";
        
        if (isConnected != null && isConnected) {
            networkStatusIcon.setText("🟢");
            networkStatusTitle.setText(getString(R.string.dashboard_network_status));
            networkStatusDescription.setText(String.format(getString(R.string.dashboard_status_online), "🟢", connectionType));
        } else {
            networkStatusIcon.setText("🔴");
            networkStatusTitle.setText(getString(R.string.dashboard_network_status));
            networkStatusDescription.setText(getString(R.string.dashboard_status_offline));
        }
    }

    private void updateSecurityStatusCard(Boolean isSecurityEnabled) {
        if (securityStatusIcon == null || securityStatusTitle == null || securityStatusDescription == null) {
            return;
        }
        
        if (isSecurityEnabled != null && isSecurityEnabled) {
            securityStatusIcon.setText("🔒");
            securityStatusTitle.setText(getString(R.string.dashboard_security_status));
            securityStatusDescription.setText(getString(R.string.dashboard_security_ready));
        } else {
            securityStatusIcon.setText("🔓");
            securityStatusTitle.setText(getString(R.string.dashboard_security_status));
            securityStatusDescription.setText(getString(R.string.dashboard_security_setup_needed));
        }
    }

    private void updateAllViews() {
        if (sharedViewModel != null) {
            updateTodayStats(sharedViewModel.getTodayStats().getValue());
            updateTotalStats(sharedViewModel.getTotalStats().getValue());
            updateNetworkStatusCard(sharedViewModel.getIsConnected().getValue());
            updateSecurityStatusCard(sharedViewModel.getSecurityEnabled().getValue());
        }
        
        updateStatus();
    }

    private void updateStatus() {
        if (statusText == null || sharedViewModel == null) {
            return;
        }
        
        Boolean isConnected = sharedViewModel.getIsConnected().getValue();
        String connectionType = sharedViewModel.getConnectionType().getValue();
        
        if (isConnected != null && isConnected) {
            statusText.setText(getString(R.string.dashboard_status_ready));
            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
        } else {
            statusText.setText(getString(R.string.dashboard_status_cannot_forward));
            statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
        }
        
        // Get and display additional status info
        MessageStatsDbHelper.DailyStats todayStats = sharedViewModel.getTodayStats().getValue();
        MessageStatsDbHelper.TotalStats totalStats = sharedViewModel.getTotalStats().getValue();
        
        if (todayStats != null && totalStats != null) {
            // Additional status updates can be added here
        }
        
        // Update security status display
        Boolean securityEnabled = sharedViewModel.getSecurityEnabled().getValue();
        // Security status is already updated in updateSecurityStatusCard
    }
    
    private void updateLastUpdateTime() {
        lastUpdateTime = System.currentTimeMillis();
        updateLastUpdateText();
    }
    
    private void updateLastUpdateText() {
        if (lastUpdateText != null) {
            if (lastUpdateTime == 0) {
                lastUpdateText.setText(getString(R.string.dashboard_last_update_never));
            } else {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                String timeString = timeFormat.format(new Date(lastUpdateTime));
                lastUpdateText.setText(String.format(getString(R.string.dashboard_last_update_format), timeString));
            }
        }
    }
    
    private void updateAutoRefreshSwitchText() {
        // The switch text is already handled by the MaterialSwitch component
        // Additional status can be shown in lastUpdateText
    }
    
    private void navigateToTab(int tabId) {
        if (getActivity() != null && getView() != null) {
            try {
                Navigation.findNavController(getView()).navigate(tabId);
            } catch (Exception e) {
                // If navigation fails, try alternative method
                showSnackbar("Navigation to requested tab");
            }
        }
    }
    
    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }
    
    private void showNetworkStatusChange(boolean isConnected, String connectionType) {
        String message = isConnected 
            ? String.format("Connected via %s", connectionType)
            : "Connection lost";
        showSnackbar(message);
    }
} 