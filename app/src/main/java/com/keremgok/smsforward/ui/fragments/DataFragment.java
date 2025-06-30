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
import com.keremgok.smsforward.MessageHistoryDbHelper;
import com.keremgok.smsforward.MessageStatsDbHelper;
import com.keremgok.smsforward.R;
import com.keremgok.smsforward.ThemeManager;

/**
 * Data Fragment - Veri yönetimi
 * Geçmiş, istatistik ve veri yedekleme/geri yükleme
 */
public class DataFragment extends Fragment {
    private static final String TAG = "DataFragment";

    private TextView titleText;
    private TextView statsText;
    private TextView infoText;
    private MessageHistoryDbHelper historyDbHelper;
    private MessageStatsDbHelper statsDbHelper;

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
        
        // Initialize database helpers
        if (getContext() != null) {
            historyDbHelper = new MessageHistoryDbHelper(getContext());
            statsDbHelper = new MessageStatsDbHelper(getContext());
        }
        
        setupInitialData();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDataStats();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Cleanup database helpers
        if (historyDbHelper != null) {
            historyDbHelper.close();
        }
        if (statsDbHelper != null) {
            statsDbHelper.close();
        }
    }

    private void setupViews(View rootView) {
        // Create title
        titleText = new TextView(getContext());
        titleText.setText("📊 Veri & Geçmiş");
        titleText.setTextSize(20f);
        titleText.setPadding(32, 32, 32, 16);
        
        // Create stats
        statsText = new TextView(getContext());
        statsText.setText("📈 Veri istatistikleri yükleniyor...");
        statsText.setTextSize(16f);
        statsText.setPadding(32, 8, 32, 16);
        
        // Create info text
        infoText = new TextView(getContext());
        infoText.setText("🚧 Phase 1 - Temel Navigasyon\n\n" +
                "Mevcut veri özellikleri:\n" +
                "• ✅ Mesaj geçmişi (son 100)\n" +
                "• ✅ İstatistikler\n" +
                "• ✅ Ayar yedekleme/geri yükleme\n" +
                "• ✅ Veri temizleme\n\n" +
                "Sonraki phase'lerde:\n" +
                "• 📊 Görsel istatistik grafikleri\n" +
                "• 🔍 Gelişmiş filtreleme\n" +
                "• 📁 Detaylı veri dışa aktarma\n" +
                "• 📈 Performans analizi\n" +
                "• 🗂️ Kategori bazlı geçmiş");
        infoText.setTextSize(16f);
        infoText.setPadding(32, 16, 32, 32);
        
        // Add views to root
        if (rootView instanceof ViewGroup) {
            ViewGroup container = (ViewGroup) rootView;
            container.addView(titleText);
            container.addView(statsText);
            container.addView(infoText);
        }
    }

    private void setupInitialData() {
        // TODO: Phase 2'de charts ve advanced data management
    }

    private void updateDataStats() {
        if (statsText == null || historyDbHelper == null || statsDbHelper == null) {
            return;
        }
        
        try {
            // Get statistics
            MessageHistoryDbHelper.HistoryStats historyStats = historyDbHelper.getHistoryStats();
            MessageStatsDbHelper.TotalStats totalStats = statsDbHelper.getTotalStats();
            MessageStatsDbHelper.DailyStats todayStats = statsDbHelper.getTodayStats();
            
            StringBuilder statsBuilder = new StringBuilder();
            
            // Today's stats
            if (todayStats != null && todayStats.totalCount > 0) {
                statsBuilder.append("📅 Bugün: ").append(todayStats.totalCount).append(" mesaj\n");
            } else {
                statsBuilder.append("📅 Bugün: Henüz mesaj yok\n");
            }
            
            // Total stats
            statsBuilder.append("📈 Toplam: ").append(totalStats.totalCount).append(" mesaj\n");
            
            if (totalStats.totalCount > 0) {
                statsBuilder.append("✅ Başarı oranı: ").append(String.format("%.1f%%", totalStats.getSuccessRate())).append("\n");
                statsBuilder.append("📊 Aktif günler: ").append(totalStats.activeDays).append("\n");
            }
            
            // History stats
            if (historyStats.totalCount > 0) {
                statsBuilder.append("📝 Geçmiş kayıtları: ").append(historyStats.totalCount).append("\n");
                statsBuilder.append("⏰ ").append(historyStats.getTimeSpanDescription());
            } else {
                statsBuilder.append("📝 Geçmiş kayıtları: Yok");
            }
            
            statsText.setText(statsBuilder.toString());
            
        } catch (Exception e) {
            statsText.setText("❌ Veri istatistikleri yüklenirken hata oluştu");
        }
    }
} 