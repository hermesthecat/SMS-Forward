package com.keremgok.smsforward.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.keremgok.smsforward.LanguageManager;
import com.keremgok.smsforward.MessageStatsDbHelper;
import com.keremgok.smsforward.NetworkStatusManager;
import com.keremgok.smsforward.R;
import com.keremgok.smsforward.ThemeManager;

/**
 * Dashboard Fragment - Ana sayfa görünümü
 * Status kartları, hızlı eylemler ve son aktiviteleri gösterir
 */
public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";

    private NetworkStatusManager networkStatusManager;
    private SharedViewModel sharedViewModel;
    private TextView welcomeText;
    private TextView statusText;

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

        // Create a simple layout programmatically for Phase 1
        View rootView = inflater.inflate(android.R.layout.activity_list_item, container, false);
        
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
        // For Phase 1, create basic text views programmatically
        // TODO: Replace with proper cards layout in Phase 3
        
        // Create welcome text
        welcomeText = new TextView(getContext());
        welcomeText.setText("📱 SMS Forward Dashboard");
        welcomeText.setTextSize(20f);
        welcomeText.setPadding(32, 32, 32, 16);
        
        // Create status text
        statusText = new TextView(getContext());
        statusText.setText("⏳ Loading status...");
        statusText.setTextSize(16f);
        statusText.setPadding(32, 16, 32, 32);
        
        // Add views to root (temporarily)
        if (rootView instanceof ViewGroup) {
            ViewGroup container = (ViewGroup) rootView;
            container.addView(welcomeText);
            container.addView(statusText);
        }
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

    private void setupInitialData() {
        if (welcomeText != null) {
            welcomeText.setText("📱 SMS Forward Dashboard\n✅ Phase 2 - Multi-Screen Navigation");
        }
        
        // Initial update
        updateStatus();
    }

    private void updateNetworkStatus(Boolean isConnected) {
        updateStatus();
    }
    
    private void updateConnectionType(String connectionType) {
        updateStatus();
    }
    
    private void updateTodayStats(MessageStatsDbHelper.DailyStats stats) {
        updateStatus();
    }
    
    private void updateTotalStats(MessageStatsDbHelper.TotalStats stats) {
        updateStatus();
    }
    
    private void updateSecurityStatus(Boolean isSecurityEnabled) {
        updateStatus();
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