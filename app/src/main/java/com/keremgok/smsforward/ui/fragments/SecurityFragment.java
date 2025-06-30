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
import com.keremgok.smsforward.R;
import com.keremgok.smsforward.SecurityManager;
import com.keremgok.smsforward.ThemeManager;

/**
 * Security Fragment - Güvenlik ve erişim kontrolü
 * PIN, biometric authentication ve güvenlik ayarları
 */
public class SecurityFragment extends Fragment {
    private static final String TAG = "SecurityFragment";

    private TextView titleText;
    private TextView infoText;
    private TextView statusText;
    private SecurityManager securityManager;

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

        // Create simple layout for Phase 1
        View rootView = inflater.inflate(android.R.layout.activity_list_item, container, false);
        
        setupViews(rootView);
        
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize security manager
        if (getContext() != null) {
            securityManager = new SecurityManager(getContext());
        }
        
        setupInitialData();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSecurityStatus();
    }

    private void setupViews(View rootView) {
        // Create title
        titleText = new TextView(getContext());
        titleText.setText("🔐 Güvenlik & Gizlilik");
        titleText.setTextSize(20f);
        titleText.setPadding(32, 32, 32, 16);
        
        // Create status
        statusText = new TextView(getContext());
        statusText.setText("🔄 Güvenlik durumu yükleniyor...");
        statusText.setTextSize(16f);
        statusText.setPadding(32, 8, 32, 16);
        
        // Create info text
        infoText = new TextView(getContext());
        infoText.setText("🚧 Phase 1 - Temel Navigasyon\n\n" +
                "Mevcut güvenlik özellikleri:\n" +
                "• ✅ PIN koruması\n" +
                "• ✅ Biyometrik kimlik doğrulama\n" +
                "• ✅ Oturum zaman aşımı\n" +
                "• ✅ Güvenlik testi\n\n" +
                "Sonraki phase'lerde:\n" +
                "• 🔐 Dedikiye güvenlik ekranı\n" +
                "• 🛡️ Gelişmiş güvenlik ayarları\n" +
                "• 📊 Güvenlik raporu\n" +
                "• 🔑 Güvenlik anahtarı yönetimi");
        infoText.setTextSize(16f);
        infoText.setPadding(32, 16, 32, 32);
        
        // Add views to root
        if (rootView instanceof ViewGroup) {
            ViewGroup container = (ViewGroup) rootView;
            container.addView(titleText);
            container.addView(statusText);
            container.addView(infoText);
        }
    }

    private void setupInitialData() {
        // TODO: Phase 2'de mevcut security preferences buraya taşınacak
    }

    private void updateSecurityStatus() {
        if (statusText == null || securityManager == null) {
            return;
        }
        
        StringBuilder statusBuilder = new StringBuilder();
        
        // Security status
        if (securityManager.isSecurityEnabled()) {
            statusBuilder.append("🔒 Güvenlik: Etkin\n");
            
            if (securityManager.isPinEnabled()) {
                statusBuilder.append("📱 PIN: Ayarlanmış\n");
            }
            
            if (securityManager.isBiometricEnabled()) {
                statusBuilder.append("👆 Biyometrik: Etkin\n");
            }
            
            long timeout = securityManager.getAuthTimeout();
            long minutes = timeout / (60 * 1000);
            statusBuilder.append("⏱️ Zaman aşımı: ").append(minutes).append(" dakika");
            
        } else {
            statusBuilder.append("🔓 Güvenlik: Devre dışı\n");
            statusBuilder.append("ℹ️ Uygulama erişimi korunmuyor");
        }
        
        statusText.setText(statusBuilder.toString());
    }
} 