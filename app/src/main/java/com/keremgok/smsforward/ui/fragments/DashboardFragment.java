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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.keremgok.smsforward.LanguageManager;
import com.keremgok.smsforward.MessageStatsDbHelper;
import com.keremgok.smsforward.NetworkStatusManager;
import com.keremgok.smsforward.R;
import com.keremgok.smsforward.ThemeManager;

/**
 * Dashboard Fragment - Modernized dashboard with status cards and quick actions
 * Phase 3: UI Modernization - Material Design 3 with loading states and animations
 */
public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";

    private NetworkStatusManager networkStatusManager;
    private SharedViewModel sharedViewModel;
    
    // UI Components
    private TextView welcomeText;
    private TextView statusText;
    private TextView todayStatsCount;
    private TextView totalStatsCount;
    private MaterialCardView networkStatusCard;
    private TextView networkStatusIcon;
    private TextView networkStatusTitle;
    private TextView networkStatusDescription;
    private MaterialCardView securityStatusCard;
    private TextView securityStatusIcon;
    private TextView securityStatusTitle;
    private TextView securityStatusDescription;
    private MaterialButton testMessageButton;
    private MaterialButton viewStatsButton;
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

        // Inflate modern dashboard layout
        View rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);
        
        // Initialize views
        setupViews(rootView);
        
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize network status manager
        if (getContext() != null) {
            networkStatusManager = NetworkStatusManager.getInstance(getContext());
        }
        
        // Setup ViewModel
        setupViewModel();
        
        // Setup initial data
        setupInitialData();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Refresh data when fragment becomes visible
        if (sharedViewModel != null) {
            sharedViewModel.refreshAll();
        }
        
        // Update status
        updateStatus();
    }

    private void setupViews(View rootView) {
        // Initialize all views from the modern layout
        welcomeText = rootView.findViewById(R.id.welcomeText);
        statusText = rootView.findViewById(R.id.statusText);
        todayStatsCount = rootView.findViewById(R.id.todayStatsCount);
        totalStatsCount = rootView.findViewById(R.id.totalStatsCount);
        
        // Network status card
        networkStatusCard = rootView.findViewById(R.id.networkStatusCard);
        networkStatusIcon = rootView.findViewById(R.id.networkStatusIcon);
        networkStatusTitle = rootView.findViewById(R.id.networkStatusTitle);
        networkStatusDescription = rootView.findViewById(R.id.networkStatusDescription);
        
        // Security status card
        securityStatusCard = rootView.findViewById(R.id.securityStatusCard);
        securityStatusIcon = rootView.findViewById(R.id.securityStatusIcon);
        securityStatusTitle = rootView.findViewById(R.id.securityStatusTitle);
        securityStatusDescription = rootView.findViewById(R.id.securityStatusDescription);
        
        // Action buttons
        testMessageButton = rootView.findViewById(R.id.testMessageButton);
        viewStatsButton = rootView.findViewById(R.id.viewStatsButton);
        
        // Loading state
        loadingStateContainer = rootView.findViewById(R.id.loadingStateContainer);
        
        // Setup button listeners
        setupButtonListeners();
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

    private void setupButtonListeners() {
        if (testMessageButton != null) {
            testMessageButton.setOnClickListener(v -> {
                // Show loading and send test message
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Test message functionality coming soon!", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        if (viewStatsButton != null) {
            viewStatsButton.setOnClickListener(v -> {
                // Navigate to data/statistics tab
                if (getActivity() != null && getActivity() instanceof androidx.fragment.app.FragmentActivity) {
                    // Switch to Data tab (index 4)
                    // This would require MainActivity navigation integration
                    Toast.makeText(getContext(), "Navigate to Data tab for detailed statistics", Toast.LENGTH_SHORT).show();
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
        }, 1000);
    }
    
    private void showLoadingState(boolean show) {
        if (loadingStateContainer != null) {
            loadingStateContainer.setVisibility(show ? View.VISIBLE : View.GONE);
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
        if (networkStatusIcon == null || networkStatusDescription == null) return;
        
        String connectionType = sharedViewModel != null ? sharedViewModel.getConnectionType().getValue() : "Unknown";
        
        if (isConnected != null && isConnected) {
            networkStatusIcon.setText("🟢");
            networkStatusDescription.setText(getString(R.string.dashboard_status_ready));
        } else {
            networkStatusIcon.setText("🔴");
            networkStatusDescription.setText(getString(R.string.dashboard_status_cannot_forward));
        }
    }
    
    private void updateSecurityStatusCard(Boolean isSecurityEnabled) {
        if (securityStatusIcon == null || securityStatusDescription == null) return;
        
        if (isSecurityEnabled != null && isSecurityEnabled) {
            securityStatusIcon.setText("🔒");
            securityStatusDescription.setText(getString(R.string.dashboard_security_ready));
        } else {
            securityStatusIcon.setText("🔓");
            securityStatusDescription.setText(getString(R.string.dashboard_security_setup_needed));
        }
    }
    
    private void updateAllViews() {
        updateStatus();
        if (sharedViewModel != null) {
            updateTodayStats(sharedViewModel.getTodayStats().getValue());
            updateTotalStats(sharedViewModel.getTotalStats().getValue());
            updateNetworkStatusCard(sharedViewModel.getIsConnected().getValue());
            updateSecurityStatusCard(sharedViewModel.getSecurityEnabled().getValue());
        }
    }

    private void updateStatus() {
        if (statusText == null) {
            return;
        }
        
        StringBuilder statusBuilder = new StringBuilder();
        
        // Network status
        if (sharedViewModel != null) {
            Boolean isConnected = sharedViewModel.getIsConnected().getValue();
            String connectionType = sharedViewModel.getConnectionType().getValue();
            
            if (isConnected != null && connectionType != null) {
                statusBuilder.append("📡 Bağlantı: ");
                if (isConnected) {
                    statusBuilder.append("Online (").append(connectionType).append(")");
                } else {
                    statusBuilder.append("Offline");
                }
                statusBuilder.append("\n");
            }
            
            // Statistics
            MessageStatsDbHelper.DailyStats todayStats = sharedViewModel.getTodayStats().getValue();
            MessageStatsDbHelper.TotalStats totalStats = sharedViewModel.getTotalStats().getValue();
            
            if (todayStats != null) {
                statusBuilder.append("📊 Bugün: ").append(todayStats.totalCount).append(" mesaj\n");
            }
            if (totalStats != null) {
                statusBuilder.append("📈 Toplam: ").append(totalStats.totalCount).append(" mesaj\n");
            }
            
            // Security status
            Boolean securityEnabled = sharedViewModel.getSecurityEnabled().getValue();
            if (securityEnabled != null) {
                statusBuilder.append("🔒 Güvenlik: ");
                statusBuilder.append(securityEnabled ? "Aktif" : "Devre dışı");
                statusBuilder.append("\n");
            }
            
            // Ready status
            if (isConnected != null) {
                if (isConnected) {
                    statusBuilder.append("✅ Mesaj iletmeye hazır");
                } else {
                    statusBuilder.append("❌ Çevrimdışı - mesaj iletilemez");
                }
            }
        } else {
            statusBuilder.append("⏳ Yükleniyor...");
        }
        
        statusText.setText(statusBuilder.toString());
    }

    @Override
    public void onPause() {
        super.onPause();
        
        // Cleanup network monitoring if needed
        if (networkStatusManager != null) {
            // Note: Don't stop monitoring here as other components may need it
        }
    }
} 