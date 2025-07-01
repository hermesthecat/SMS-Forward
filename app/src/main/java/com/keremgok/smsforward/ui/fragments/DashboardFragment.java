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
import com.keremgok.smsforward.ui.utils.AnimationUtils;

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
        
        // Apply entrance animations to cards
        setupCardAnimations();
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
    
    private void setupCardAnimations() {
        // Apply staggered fade-in animation to all cards
        ViewGroup mainContainer = (ViewGroup) getView();
        if (mainContainer != null) {
            // Delay the animation slightly to ensure views are measured
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Animate cards with staggered entrance
                if (networkStatusCard != null) {
                    AnimationUtils.slideUp(networkStatusCard, AnimationUtils.DURATION_MEDIUM, null);
                }
                
                if (securityStatusCard != null) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        AnimationUtils.slideUp(securityStatusCard, AnimationUtils.DURATION_MEDIUM, null);
                    }, 100);
                }
                
                // Animate buttons with bounce effect
                if (testMessageButton != null) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        AnimationUtils.bounceIn(testMessageButton);
                    }, 200);
                }
                
                if (refreshButton != null) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        AnimationUtils.bounceIn(refreshButton);
                    }, 250);
                }
                
                if (viewStatsButton != null) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        AnimationUtils.bounceIn(viewStatsButton);
                    }, 300);
                }
                
                if (viewHistoryButton != null) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        AnimationUtils.bounceIn(viewHistoryButton);
                    }, 350);
                }
                
                // Add hover effects to cards
                setupCardHoverEffects();
            }, 150);
        }
    }
    
    private void setupCardHoverEffects() {
        // Add touch animation to network status card
        if (networkStatusCard != null) {
            networkStatusCard.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        AnimationUtils.elevateCard(v, true);
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        AnimationUtils.elevateCard(v, false);
                        break;
                }
                return false;
            });
        }
        
        // Add touch animation to security status card
        if (securityStatusCard != null) {
            securityStatusCard.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        AnimationUtils.elevateCard(v, true);
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        AnimationUtils.elevateCard(v, false);
                        break;
                }
                return false;
            });
        }
    }

    private void setupButtonListeners() {
        if (testMessageButton != null) {
            testMessageButton.setOnClickListener(v -> {
                // Apply button press animation
                AnimationUtils.animateButtonPress(v);
                
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
                // Apply button press animation and rotation
                AnimationUtils.animateButtonPress(v);
                AnimationUtils.rotate(v);
                
                refreshAllData();
                showSnackbar(getString(R.string.dashboard_refreshing));
            });
        }
        
        if (viewStatsButton != null) {
            viewStatsButton.setOnClickListener(v -> {
                // Apply button press animation
                AnimationUtils.animateButtonPress(v);
                
                // Navigate to Data tab (index 4 in bottom navigation)
                navigateToTab(R.id.nav_data);
            });
        }
        
        if (viewHistoryButton != null) {
            viewHistoryButton.setOnClickListener(v -> {
                // Apply button press animation
                AnimationUtils.animateButtonPress(v);
                
                // Navigate to Data tab and show history
                navigateToTab(R.id.nav_data);
                showSnackbar("Message history available in Data tab");
            });
        }
        
        // Help button listener
        if (helpButton != null) {
            helpButton.setOnClickListener(v -> {
                // Apply button press animation
                AnimationUtils.animateButtonPress(v);
                
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
            if (show) {
                loadingStateContainer.setVisibility(View.VISIBLE);
                AnimationUtils.fadeIn(loadingStateContainer);
                AnimationUtils.pulse(loadingStateContainer);
            } else {
                AnimationUtils.stopPulse(loadingStateContainer);
                AnimationUtils.fadeOut(loadingStateContainer, AnimationUtils.DURATION_MEDIUM, () -> {
                    if (loadingStateContainer != null) {
                        loadingStateContainer.setVisibility(View.GONE);
                    }
                });
            }
        }
    }
    
    private void showLiveIndicator(boolean show) {
        if (networkLiveIndicator != null) {
            if (show) {
                networkLiveIndicator.setVisibility(View.VISIBLE);
                AnimationUtils.fadeIn(networkLiveIndicator);
                AnimationUtils.pulse(networkLiveIndicator);
            } else {
                AnimationUtils.stopPulse(networkLiveIndicator);
                AnimationUtils.fadeOut(networkLiveIndicator, AnimationUtils.DURATION_SHORT, () -> {
                    if (networkLiveIndicator != null) {
                        networkLiveIndicator.setVisibility(View.GONE);
                    }
                });
            }
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
            // Animate number change
            AnimationUtils.fadeOut(todayStatsCount, AnimationUtils.DURATION_SHORT, () -> {
                todayStatsCount.setText(String.valueOf(stats.totalCount));
                AnimationUtils.fadeIn(todayStatsCount);
                
                // Add pulse effect for updated stats
                AnimationUtils.animateSuccess(todayStatsCount);
            });
        }
    }
    
    private void updateTotalStats(MessageStatsDbHelper.TotalStats stats) {
        if (totalStatsCount != null && stats != null) {
            // Animate number change
            AnimationUtils.fadeOut(totalStatsCount, AnimationUtils.DURATION_SHORT, () -> {
                totalStatsCount.setText(String.valueOf(stats.totalCount));
                AnimationUtils.fadeIn(totalStatsCount);
                
                // Add pulse effect for updated stats
                AnimationUtils.animateSuccess(totalStatsCount);
            });
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
        
        // Animate icon change
        AnimationUtils.fadeOut(networkStatusIcon, AnimationUtils.DURATION_SHORT, () -> {
            if (isConnected != null && isConnected) {
                networkStatusIcon.setText("🟢");
                networkStatusTitle.setText(getString(R.string.dashboard_network_status));
                networkStatusDescription.setText(String.format(getString(R.string.dashboard_status_online), "🟢", connectionType));
                
                // Success animation for connection
                AnimationUtils.animateSuccess(networkStatusCard);
            } else {
                networkStatusIcon.setText("🔴");
                networkStatusTitle.setText(getString(R.string.dashboard_network_status));
                networkStatusDescription.setText(getString(R.string.dashboard_status_offline));
                
                // Error animation for disconnection
                AnimationUtils.animateError(networkStatusCard);
            }
            
            AnimationUtils.fadeIn(networkStatusIcon);
        });
        
        // Animate description change
        AnimationUtils.fadeOut(networkStatusDescription, AnimationUtils.DURATION_SHORT, () -> {
            AnimationUtils.fadeIn(networkStatusDescription);
        });
    }

    private void updateSecurityStatusCard(Boolean isSecurityEnabled) {
        if (securityStatusIcon == null || securityStatusTitle == null || securityStatusDescription == null) {
            return;
        }
        
        // Animate icon change
        AnimationUtils.fadeOut(securityStatusIcon, AnimationUtils.DURATION_SHORT, () -> {
            if (isSecurityEnabled != null && isSecurityEnabled) {
                securityStatusIcon.setText("🔒");
                securityStatusTitle.setText(getString(R.string.dashboard_security_status));
                securityStatusDescription.setText(getString(R.string.dashboard_security_ready));
                
                // Success animation for enabled security
                AnimationUtils.animateSuccess(securityStatusCard);
            } else {
                securityStatusIcon.setText("🔓");
                securityStatusTitle.setText(getString(R.string.dashboard_security_status));
                securityStatusDescription.setText(getString(R.string.dashboard_security_setup_needed));
                
                // Warning animation for disabled security
                AnimationUtils.animateError(securityStatusCard);
            }
            
            AnimationUtils.fadeIn(securityStatusIcon);
        });
        
        // Animate description change
        AnimationUtils.fadeOut(securityStatusDescription, AnimationUtils.DURATION_SHORT, () -> {
            AnimationUtils.fadeIn(securityStatusDescription);
        });
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
            Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT);
            
            // Add slide animation to snackbar
            View snackbarView = snackbar.getView();
            snackbarView.setTranslationY(snackbarView.getHeight());
            snackbarView.animate()
                    .translationY(0f)
                    .setDuration(AnimationUtils.DURATION_MEDIUM)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
                    
            snackbar.show();
        }
    }
    
    private void showNetworkStatusChange(boolean isConnected, String connectionType) {
        String message = isConnected 
            ? String.format("🟢 Connected via %s", connectionType)
            : "🔴 Connection lost";
        
        // Add visual feedback to network status card
        if (networkStatusCard != null) {
            if (isConnected) {
                AnimationUtils.animateSuccess(networkStatusCard);
            } else {
                AnimationUtils.animateError(networkStatusCard);
            }
        }
        
        showSnackbar(message);
    }
} 