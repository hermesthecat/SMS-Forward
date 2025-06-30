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

import com.keremgok.smsforward.LanguageManager;
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
        
        // Setup initial data
        setupInitialData();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Update status when fragment becomes visible
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

    private void setupInitialData() {
        // TODO: Initialize ViewModels and data sources in Phase 4
        // For now, just show basic info
        
        if (welcomeText != null) {
            welcomeText.setText("📱 SMS Forward Dashboard\n🚧 Phase 1 - Basic Navigation");
        }
    }

    private void updateStatus() {
        if (statusText == null || networkStatusManager == null) {
            return;
        }
        
        // Update network status
        networkStatusManager.updateNetworkStatus();
        
        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("📡 Bağlantı: ").append(networkStatusManager.getConnectionStatus()).append("\n");
        statusBuilder.append("🌐 Kalite: ").append(networkStatusManager.getNetworkQuality()).append("\n");
        
        if (networkStatusManager.canForwardMessages()) {
            statusBuilder.append("✅ Mesaj iletmeye hazır");
        } else {
            statusBuilder.append("❌ Çevrimdışı - mesaj iletilemez");
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