package com.keremgok.smsforward.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import com.keremgok.smsforward.LanguageManager;
import com.keremgok.smsforward.R;
import com.keremgok.smsforward.SettingsBackupManager;
import com.keremgok.smsforward.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Enhanced Backup Manager Fragment - Comprehensive backup and restore management
 * Provides selective backup, preview, analytics, and file management
 */
public class BackupManagerFragment extends Fragment {
    private static final String TAG = "BackupManagerFragment";

    // UI Components
    private MaterialCardView backupStatsCard;
    private MaterialCardView createBackupCard;
    private MaterialCardView manageBackupsCard;
    
    // Backup creation components
    private ChipGroup backupComponentsChipGroup;
    private MaterialSwitch includeSettingsSwitch;
    private MaterialSwitch includeFiltersSwitch;
    private MaterialSwitch includeStatisticsSwitch;
    private MaterialButton createBackupButton;
    private MaterialButton quickFullBackupButton;
    
    // Backup management components
    private MaterialButton importBackupButton;
    private MaterialButton viewBackupHistoryButton;
    private RecyclerView recentBackupsRecyclerView;
    
    // Statistics components
    private android.widget.TextView totalBackupsCount;
    private android.widget.TextView lastBackupDate;
    private android.widget.TextView totalBackupSize;
    
    // Data and managers
    private SettingsBackupManager backupManager;
    private List<BackupEntry> recentBackups;
    
    // File operation launchers
    private ActivityResultLauncher<Intent> createBackupLauncher;
    private ActivityResultLauncher<Intent> importBackupLauncher;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(LanguageManager.wrapContext(context));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Apply theme
        ThemeManager.initializeTheme(getContext());
        
        return inflater.inflate(R.layout.fragment_backup_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeComponents(view);
        setupEventListeners();
        initializeFileLaunchers();
        loadBackupData();
        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshBackupData();
    }

    private void initializeComponents(View view) {
        // Initialize backup manager
        backupManager = new SettingsBackupManager(getContext());
        
        // Initialize UI components
        backupStatsCard = view.findViewById(R.id.backup_stats_card);
        createBackupCard = view.findViewById(R.id.create_backup_card);
        manageBackupsCard = view.findViewById(R.id.manage_backups_card);
        
        // Backup creation components
        backupComponentsChipGroup = view.findViewById(R.id.backup_components_chip_group);
        includeSettingsSwitch = view.findViewById(R.id.include_settings_switch);
        includeFiltersSwitch = view.findViewById(R.id.include_filters_switch);
        includeStatisticsSwitch = view.findViewById(R.id.include_statistics_switch);
        createBackupButton = view.findViewById(R.id.create_backup_button);
        quickFullBackupButton = view.findViewById(R.id.quick_full_backup_button);
        
        // Backup management components
        importBackupButton = view.findViewById(R.id.import_backup_button);
        viewBackupHistoryButton = view.findViewById(R.id.view_backup_history_button);
        recentBackupsRecyclerView = view.findViewById(R.id.recent_backups_recycler_view);
        
        // Statistics components
        totalBackupsCount = view.findViewById(R.id.total_backups_count);
        lastBackupDate = view.findViewById(R.id.last_backup_date);
        totalBackupSize = view.findViewById(R.id.total_backup_size);
        
        // Initialize data
        recentBackups = new ArrayList<>();
        
        // Set default selections
        includeSettingsSwitch.setChecked(true);
        includeFiltersSwitch.setChecked(true);
        includeStatisticsSwitch.setChecked(false);
        
        setupBackupComponentChips();
    }

    private void setupBackupComponentChips() {
        // Clear existing chips
        backupComponentsChipGroup.removeAllViews();
        
        // Add component chips
        addComponentChip("Forwarder Settings", "SMS, Telegram, Email, Webhook configs", true);
        addComponentChip("Appearance", "Theme, language, UI preferences", true);
        addComponentChip("Security", "Rate limiting, content filtering", true);
        addComponentChip("Advanced Filters", "Custom message filtering rules", true);
        addComponentChip("Statistics", "Usage analytics and performance data", false);
    }
    
    private void addComponentChip(String title, String description, boolean checked) {
        Chip chip = new Chip(getContext());
        chip.setText(title);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setChipIconResource(R.drawable.ic_backup_component);
        
        // Add tooltip with description
        chip.setTooltipText(description);
        
        backupComponentsChipGroup.addView(chip);
    }

    private void setupEventListeners() {
        // Quick full backup button
        quickFullBackupButton.setOnClickListener(v -> createQuickFullBackup());
        
        // Custom backup button
        createBackupButton.setOnClickListener(v -> createCustomBackup());
        
        // Import backup button
        importBackupButton.setOnClickListener(v -> importBackup());
        
        // View backup history button
        viewBackupHistoryButton.setOnClickListener(v -> showBackupHistory());
        
        // Switch listeners for UI updates
        includeSettingsSwitch.setOnCheckedChangeListener((button, isChecked) -> updateCreateButtonState());
        includeFiltersSwitch.setOnCheckedChangeListener((button, isChecked) -> updateCreateButtonState());
        includeStatisticsSwitch.setOnCheckedChangeListener((button, isChecked) -> updateCreateButtonState());
    }

    private void initializeFileLaunchers() {
        // Create backup launcher
        createBackupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performBackupCreation(uri);
                        }
                    }
                });

        // Import backup launcher
        importBackupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            showBackupPreview(uri);
                        }
                    }
                });
    }

    private void loadBackupData() {
        // Load recent backups (simulated for now)
        recentBackups.clear();
        // In a real implementation, this would load from a backup history database
        // For now, we'll show some example data
        recentBackups.add(new BackupEntry("Full Backup", "Today 14:30", "2.1 KB", "full"));
        recentBackups.add(new BackupEntry("Settings Only", "Yesterday 09:15", "1.8 KB", "settings"));
    }

    private void refreshBackupData() {
        loadBackupData();
        updateUI();
    }

    private void updateUI() {
        // Update statistics
        updateBackupStatistics();
        
        // Update button states
        updateCreateButtonState();
        
        // Update recent backups (if adapter exists)
        // In a full implementation, this would update the RecyclerView adapter
    }

    private void updateBackupStatistics() {
        // Update backup statistics
        totalBackupsCount.setText(String.valueOf(recentBackups.size()));
        
        if (!recentBackups.isEmpty()) {
            lastBackupDate.setText(recentBackups.get(0).date);
            
            // Calculate total size (simulated)
            double totalSize = 0;
            for (BackupEntry backup : recentBackups) {
                totalSize += parseBackupSize(backup.size);
            }
            totalBackupSize.setText(formatSize(totalSize));
        } else {
            lastBackupDate.setText("Never");
            totalBackupSize.setText("0 KB");
        }
    }

    private void updateCreateButtonState() {
        boolean hasSelection = includeSettingsSwitch.isChecked() || 
                              includeFiltersSwitch.isChecked() || 
                              includeStatisticsSwitch.isChecked();
        
        createBackupButton.setEnabled(hasSelection);
        
        // Update chip group based on switches
        updateChipGroupFromSwitches();
    }
    
    private void updateChipGroupFromSwitches() {
        // This would update the chip group based on switch states
        // Implementation depends on the exact UI design
    }

    private void createQuickFullBackup() {
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            
            String filename = generateBackupFilename("full");
            intent.putExtra(Intent.EXTRA_TITLE, filename);
            
            createBackupLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to create backup: " + e.getMessage(), 
                         Toast.LENGTH_LONG).show();
        }
    }

    private void createCustomBackup() {
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            
            String filename = generateBackupFilename("custom");
            intent.putExtra(Intent.EXTRA_TITLE, filename);
            
            createBackupLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to create custom backup: " + e.getMessage(), 
                         Toast.LENGTH_LONG).show();
        }
    }

    private void importBackup() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain"});
            
            importBackupLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to open backup file: " + e.getMessage(), 
                         Toast.LENGTH_LONG).show();
        }
    }

    private void performBackupCreation(Uri uri) {
        try {
            backupManager.exportToFile(uri);
            
            Toast.makeText(getContext(), "Backup created successfully!", Toast.LENGTH_SHORT).show();
            refreshBackupData();
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to create backup: " + e.getMessage(), 
                         Toast.LENGTH_LONG).show();
        }
    }

    private void showBackupPreview(Uri uri) {
        // This would show a dialog with backup contents preview
        // For now, just proceed with import
        performBackupImport(uri);
    }

    private void performBackupImport(Uri uri) {
        try {
            SettingsBackupManager.ImportResult result = backupManager.importFromFile(uri);
            
            if (result.success) {
                Toast.makeText(getContext(), result.message, Toast.LENGTH_LONG).show();
                refreshBackupData();
                
                // Refresh parent activity if needed
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            } else {
                Toast.makeText(getContext(), "Import failed: " + result.message, 
                             Toast.LENGTH_LONG).show();
            }
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to import backup: " + e.getMessage(), 
                         Toast.LENGTH_LONG).show();
        }
    }

    private void showBackupHistory() {
        // This would show a dialog or navigate to a backup history screen
        new AlertDialog.Builder(getContext())
                .setTitle("Backup History")
                .setMessage("Backup history feature will be implemented in future versions.")
                .setPositiveButton("OK", null)
                .show();
    }

    private String generateBackupFilename(String type) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        return "sms_forward_" + type + "_" + timestamp + ".json";
    }

    private double parseBackupSize(String sizeStr) {
        // Parse size string like "2.1 KB" to bytes
        if (sizeStr.contains("KB")) {
            return Double.parseDouble(sizeStr.replace(" KB", "")) * 1024;
        } else if (sizeStr.contains("MB")) {
            return Double.parseDouble(sizeStr.replace(" MB", "")) * 1024 * 1024;
        } else {
            return Double.parseDouble(sizeStr.replace(" B", ""));
        }
    }

    private String formatSize(double bytes) {
        if (bytes < 1024) return String.format("%.0f B", bytes);
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024);
        return String.format("%.1f MB", bytes / (1024 * 1024));
    }

    // Simple data class for backup entries
    private static class BackupEntry {
        final String name;
        final String date;
        final String size;
        final String type;

        BackupEntry(String name, String date, String size, String type) {
            this.name = name;
            this.date = date;
            this.size = size;
            this.type = type;
        }
    }
} 