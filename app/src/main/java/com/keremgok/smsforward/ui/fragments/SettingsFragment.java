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
import com.keremgok.smsforward.ThemeManager;

/**
 * Settings Fragment - Gelişmiş uygulama ayarları
 * Filtreleme, ağ, performans ve görünüm ayarları
 */
public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";

    private TextView titleText;
    private TextView infoText;

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
        
        setupInitialData();
    }

    private void setupViews(View rootView) {
        // Create title
        titleText = new TextView(getContext());
        titleText.setText("⚙️ Gelişmiş Ayarlar");
        titleText.setTextSize(20f);
        titleText.setPadding(32, 32, 32, 16);
        
        // Create info text
        infoText = new TextView(getContext());
        infoText.setText("🚧 Phase 1 - Temel Navigasyon\n\n" +
                "Sonraki phase'lerde:\n\n" +
                "🔍 Filtreleme & Kontrol:\n" +
                "• İçerik filtreleme\n" +
                "• Hız sınırlaması\n" +
                "• Whitelist/Blacklist\n\n" +
                "🌐 Ağ & Performans:\n" +
                "• Ağ izleme ayarları\n" +
                "• Yeniden deneme politikaları\n" +
                "• Kuyruk yönetimi\n\n" +
                "🎨 Görünüm:\n" +
                "• Tema ayarları\n" +
                "• Dil seçimi\n" +
                "• Bildirim ayarları");
        infoText.setTextSize(16f);
        infoText.setPadding(32, 16, 32, 32);
        
        // Add views to root
        if (rootView instanceof ViewGroup) {
            ViewGroup container = (ViewGroup) rootView;
            container.addView(titleText);
            container.addView(infoText);
        }
    }

    private void setupInitialData() {
        // TODO: Phase 2'de mevcut MainActivity.SettingsFragment'ten 
        // gelişmiş ayarlar buraya taşınacak
    }
} 