package com.keremgok.smsforward.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import com.keremgok.smsforward.AdvancedContentFilter;
import com.keremgok.smsforward.LanguageManager;
import com.keremgok.smsforward.R;
import com.keremgok.smsforward.ThemeManager;
import com.keremgok.smsforward.ui.adapters.FilterRulesAdapter;
import com.keremgok.smsforward.ui.adapters.TestResultsAdapter;
import com.keremgok.smsforward.ui.dialogs.FilterEditorDialog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Advanced Filters Fragment - Gelişmiş filtre yönetimi
 * Kullanıcıların karmaşık filtre kuralları oluşturmasına, yönetmesine ve test etmesine olanak sağlar
 */
public class AdvancedFiltersFragment extends Fragment {
    private static final String TAG = "AdvancedFiltersFragment";

    // UI Components
    private MaterialSwitch enableAdvancedFiltersSwitch;
    private MaterialCardView filterStatsCard;
    private TextView totalFiltersCount;
    private TextView activeFiltersCount;
    private TextView categoriesCount;
    private MaterialButton addFilterButton;
    private MaterialButton testFiltersButton;
    private MaterialButton importFiltersButton;
    private MaterialButton exportFiltersButton;
    private MaterialButton clearAllFiltersButton;
    private RecyclerView filtersRecyclerView;
    private View emptyStateContainer;
    private MaterialCardView testPreviewCard;
    private TextInputEditText testSenderInput;
    private TextInputEditText testMessageInput;
    private MaterialButton runTestButton;
    private View testResultsContainer;
    private RecyclerView testResultsRecyclerView;

    // Data and Adapters
    private AdvancedContentFilter filterManager;
    private FilterRulesAdapter filterRulesAdapter;
    private TestResultsAdapter testResultsAdapter;
    private List<AdvancedContentFilter.FilterRule> filterRules;
    private List<TestResultsAdapter.TestResult> testResults;

    // File operation launchers
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(LanguageManager.wrapContext(context));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Apply theme
        ThemeManager.initializeTheme(getContext());
        
        return inflater.inflate(R.layout.fragment_advanced_filters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeComponents(view);
        setupRecyclerViews();
        setupEventListeners();
        initializeFileLaunchers();
        loadFilterData();
        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFilterData();
    }

    private void initializeComponents(View view) {
        // Initialize filter manager
        filterManager = new AdvancedContentFilter(getContext());
        
        // Initialize UI components
        enableAdvancedFiltersSwitch = view.findViewById(R.id.enable_advanced_filters_switch);
        filterStatsCard = view.findViewById(R.id.filter_stats_card);
        totalFiltersCount = view.findViewById(R.id.total_filters_count);
        activeFiltersCount = view.findViewById(R.id.active_filters_count);
        categoriesCount = view.findViewById(R.id.categories_count);
        addFilterButton = view.findViewById(R.id.add_filter_button);
        testFiltersButton = view.findViewById(R.id.test_filters_button);
        importFiltersButton = view.findViewById(R.id.import_filters_button);
        exportFiltersButton = view.findViewById(R.id.export_filters_button);
        clearAllFiltersButton = view.findViewById(R.id.clear_all_filters_button);
        filtersRecyclerView = view.findViewById(R.id.filters_recycler_view);
        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        testPreviewCard = view.findViewById(R.id.test_preview_card);
        testSenderInput = view.findViewById(R.id.test_sender_input);
        testMessageInput = view.findViewById(R.id.test_message_input);
        runTestButton = view.findViewById(R.id.run_test_button);
        testResultsContainer = view.findViewById(R.id.test_results_container);
        testResultsRecyclerView = view.findViewById(R.id.test_results_recycler_view);

        // Initialize data lists
        filterRules = new ArrayList<>();
        testResults = new ArrayList<>();

        // Set initial switch state
        enableAdvancedFiltersSwitch.setChecked(filterManager.isAdvancedFilteringEnabled());
    }

    private void setupRecyclerViews() {
        // Setup filter rules RecyclerView
        filterRulesAdapter = new FilterRulesAdapter(filterRules, new FilterRulesAdapter.OnFilterActionListener() {
            @Override
            public void onToggleFilter(AdvancedContentFilter.FilterRule rule, boolean enabled) {
                rule.setEnabled(enabled);
                updateFilterRule(rule);
                updateUI();
            }

            @Override
            public void onEditFilter(AdvancedContentFilter.FilterRule rule) {
                showEditFilterDialog(rule);
            }

            @Override
            public void onDeleteFilter(AdvancedContentFilter.FilterRule rule) {
                showDeleteConfirmation(rule);
            }
        });

        filtersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        filtersRecyclerView.setAdapter(filterRulesAdapter);

        // Setup test results RecyclerView
        testResultsAdapter = new TestResultsAdapter(testResults);
        testResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        testResultsRecyclerView.setAdapter(testResultsAdapter);
    }

    private void setupEventListeners() {
        // Enable/disable switch
        enableAdvancedFiltersSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            filterManager.setAdvancedFilteringEnabled(isChecked);
            updateUI();
            
            if (isChecked) {
                Toast.makeText(getContext(), "Advanced filtering enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Advanced filtering disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Quick action buttons
        addFilterButton.setOnClickListener(v -> showCreateFilterDialog());
        
        testFiltersButton.setOnClickListener(v -> {
            if (testPreviewCard.getVisibility() == View.GONE) {
                testPreviewCard.setVisibility(View.VISIBLE);
            } else {
                testPreviewCard.setVisibility(View.GONE);
            }
        });

        importFiltersButton.setOnClickListener(v -> importFilters());
        exportFiltersButton.setOnClickListener(v -> exportFilters());
        clearAllFiltersButton.setOnClickListener(v -> showClearAllConfirmation());

        // Test functionality
        runTestButton.setOnClickListener(v -> runFilterTest());
    }

    private void initializeFileLaunchers() {
        // Export launcher
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performExport(uri);
                        }
                    }
                });

        // Import launcher
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performImport(uri);
                        }
                    }
                });
    }

    private void loadFilterData() {
        filterRules.clear();
        filterRules.addAll(filterManager.getFilterRules());
        filterRulesAdapter.notifyDataSetChanged();
    }

    private void refreshFilterData() {
        loadFilterData();
        updateUI();
    }

    private void updateUI() {
        // Update switch state
        enableAdvancedFiltersSwitch.setChecked(filterManager.isAdvancedFilteringEnabled());
        
        // Update statistics
        updateFilterStatistics();
        
        // Update empty state
        if (filterRules.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            filtersRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            filtersRecyclerView.setVisibility(View.VISIBLE);
        }

        // Update button states
        boolean hasFilters = !filterRules.isEmpty();
        exportFiltersButton.setEnabled(hasFilters);
        clearAllFiltersButton.setEnabled(hasFilters);
        testFiltersButton.setEnabled(hasFilters);
    }

    private void updateFilterStatistics() {
        int totalCount = filterRules.size();
        int activeCount = filterManager.getActiveFilterCount();
        
        // Count unique categories
        Set<String> categories = new HashSet<>();
        for (AdvancedContentFilter.FilterRule rule : filterRules) {
            categories.add(rule.getCategory());
        }

        totalFiltersCount.setText(String.valueOf(totalCount));
        activeFiltersCount.setText(String.valueOf(activeCount));
        categoriesCount.setText(String.valueOf(categories.size()));
    }

    private void showCreateFilterDialog() {
        FilterEditorDialog dialog = new FilterEditorDialog(getContext(), null, new FilterEditorDialog.OnFilterSaveListener() {
            @Override
            public void onFilterSaved(AdvancedContentFilter.FilterRule rule) {
                filterManager.addFilterRule(rule);
                refreshFilterData();
                Toast.makeText(getContext(), getString(R.string.filter_save) + " completed", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void showEditFilterDialog(AdvancedContentFilter.FilterRule rule) {
        FilterEditorDialog dialog = new FilterEditorDialog(getContext(), rule, new FilterEditorDialog.OnFilterSaveListener() {
            @Override
            public void onFilterSaved(AdvancedContentFilter.FilterRule updatedRule) {
                updateFilterRule(updatedRule);
                refreshFilterData();
                Toast.makeText(getContext(), getString(R.string.filter_edit) + " completed", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void updateFilterRule(AdvancedContentFilter.FilterRule rule) {
        int index = filterRules.indexOf(rule);
        if (index >= 0) {
            filterManager.updateFilterRule(index, rule);
        }
    }

    private void showDeleteConfirmation(AdvancedContentFilter.FilterRule rule) {
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.filter_delete_confirm_title))
                .setMessage(getString(R.string.filter_delete_confirm_message))
                .setPositiveButton(getString(R.string.filter_delete), (dialog, which) -> {
                    filterManager.removeFilterRule(rule);
                    refreshFilterData();
                    Toast.makeText(getContext(), getString(R.string.filter_delete) + " completed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.filter_cancel), null)
                .show();
    }

    private void showClearAllConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.filter_clear_all_confirm_title))
                .setMessage(getString(R.string.filter_clear_all_confirm_message))
                .setPositiveButton(getString(R.string.filter_clear_all), (dialog, which) -> {
                    filterManager.clearAllFilters();
                    refreshFilterData();
                    Toast.makeText(getContext(), "All filters cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.filter_cancel), null)
                .show();
    }

    private void runFilterTest() {
        String senderNumber = testSenderInput.getText().toString().trim();
        String messageContent = testMessageInput.getText().toString().trim();

        if (TextUtils.isEmpty(senderNumber)) {
            senderNumber = "+1234567890"; // Default test number
        }

        if (TextUtils.isEmpty(messageContent)) {
            Toast.makeText(getContext(), "Please enter a test message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Test the message against all filters
        List<AdvancedContentFilter.FilterRule> matchingRules = filterManager.testMessage(senderNumber, messageContent);
        
        // Update test results
        testResults.clear();
        if (matchingRules.isEmpty()) {
            testResults.add(new TestResultsAdapter.TestResult("✅", getString(R.string.filter_no_matches), 
                getString(R.string.filter_test_allowed), ""));
        } else {
            for (AdvancedContentFilter.FilterRule rule : matchingRules) {
                String icon = rule.getAction() == AdvancedContentFilter.FilterAction.BLOCK ? "🚫" : "⚠️";
                String reason = "Matched pattern: '" + rule.getPattern() + "'";
                testResults.add(new TestResultsAdapter.TestResult(icon, rule.getName(), 
                    rule.getActionDisplayName(), reason));
            }
        }

        testResultsAdapter.notifyDataSetChanged();
        testResultsContainer.setVisibility(View.VISIBLE);

        // Show overall result
        if (!matchingRules.isEmpty()) {
            boolean wouldBlock = matchingRules.stream()
                    .anyMatch(rule -> rule.getAction() == AdvancedContentFilter.FilterAction.BLOCK);
            
            String resultMessage = wouldBlock ? getString(R.string.filter_test_blocked) : 
                                                getString(R.string.filter_test_allowed);
            Toast.makeText(getContext(), resultMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void exportFilters() {
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String filename = "sms_forward_filters_" + dateFormat.format(new Date()) + ".json";
            intent.putExtra(Intent.EXTRA_TITLE, filename);
            
            exportLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), 
                    String.format(getString(R.string.filter_export_error), e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void importFilters() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain"});
            
            importLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    String.format(getString(R.string.filter_import_error), e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void performExport(Uri uri) {
        try {
            String filterData = filterManager.exportFilters();
            
            try (OutputStream outputStream = getContext().getContentResolver().openOutputStream(uri)) {
                if (outputStream == null) {
                    throw new IOException("Failed to open output stream");
                }
                outputStream.write(filterData.getBytes("UTF-8"));
                outputStream.flush();
            }
            
            Toast.makeText(getContext(), getString(R.string.filter_export_success), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    String.format(getString(R.string.filter_export_error), e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void performImport(Uri uri) {
        try {
            StringBuilder jsonBuilder = new StringBuilder();
            
            try (InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line).append('\n');
                }
            }
            
            String jsonData = jsonBuilder.toString();
            int originalCount = filterRules.size();
            
            filterManager.importFilters(jsonData);
            refreshFilterData();
            
            int importedCount = filterRules.size() - originalCount;
            Toast.makeText(getContext(),
                    String.format(getString(R.string.filter_import_success), importedCount),
                    Toast.LENGTH_SHORT).show();
                    
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    String.format(getString(R.string.filter_import_error), e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }
} 