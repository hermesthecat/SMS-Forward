package com.keremgok.smsforward.ui.dialogs;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.keremgok.smsforward.AdvancedContentFilter;
import com.keremgok.smsforward.R;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Dialog for creating and editing filter rules
 */
public class FilterEditorDialog {

    public interface OnFilterSaveListener {
        void onFilterSaved(AdvancedContentFilter.FilterRule rule);
    }

    private final Context context;
    private final AdvancedContentFilter.FilterRule existingRule;
    private final OnFilterSaveListener saveListener;
    private AlertDialog dialog;

    // UI Components
    private TextInputEditText nameEditText;
    private TextInputEditText descriptionEditText;
    private TextInputEditText patternEditText;
    private TextInputEditText categoryEditText;
    private AutoCompleteTextView typeDropdown;
    private AutoCompleteTextView actionDropdown;
    private TextInputLayout patternLayout;

    public FilterEditorDialog(Context context, AdvancedContentFilter.FilterRule existingRule, OnFilterSaveListener saveListener) {
        this.context = context;
        this.existingRule = existingRule;
        this.saveListener = saveListener;
    }

    public void show() {
        // Inflate custom layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_filter_editor, null);

        initializeViews(dialogView);
        setupDropdowns();
        populateFields();
        setupValidation();

        // Create dialog
        String title = existingRule != null ?
                context.getString(R.string.filter_edit_title) :
                context.getString(R.string.filter_create_title);

        dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.filter_save, null) // Set to null initially to prevent auto-dismiss
                .setNegativeButton(R.string.filter_cancel, (d, which) -> d.dismiss())
                .create();

        dialog.show();

        // Override positive button to add validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (validateAndSave()) {
                dialog.dismiss();
            }
        });
    }

    private void initializeViews(View dialogView) {
        nameEditText = dialogView.findViewById(R.id.filter_name_edit);
        descriptionEditText = dialogView.findViewById(R.id.filter_description_edit);
        patternEditText = dialogView.findViewById(R.id.filter_pattern_edit);
        categoryEditText = dialogView.findViewById(R.id.filter_category_edit);
        typeDropdown = dialogView.findViewById(R.id.filter_type_dropdown);
        actionDropdown = dialogView.findViewById(R.id.filter_action_dropdown);
        patternLayout = dialogView.findViewById(R.id.filter_pattern_layout);
    }

    private void setupDropdowns() {
        // Setup filter type dropdown
        String[] filterTypes = {
                context.getString(R.string.filter_type_keyword),
                context.getString(R.string.filter_type_regex),
                context.getString(R.string.filter_type_sender),
                context.getString(R.string.filter_type_contains),
                context.getString(R.string.filter_type_starts_with),
                context.getString(R.string.filter_type_ends_with)
        };

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, filterTypes);
        typeDropdown.setAdapter(typeAdapter);

        // Setup filter action dropdown
        String[] filterActions = {
                context.getString(R.string.filter_action_block),
                context.getString(R.string.filter_action_skip),
                context.getString(R.string.filter_action_tag)
        };

        ArrayAdapter<String> actionAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, filterActions);
        actionDropdown.setAdapter(actionAdapter);

        // Set default selections
        typeDropdown.setText(filterTypes[0], false);
        actionDropdown.setText(filterActions[0], false);

        // Add type change listener to update pattern hint
        typeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            updatePatternHint(position);
        });
    }

    private void updatePatternHint(int typePosition) {
        String hint;
        switch (typePosition) {
            case 0: // Keyword
                hint = "promotion, spam, offer";
                break;
            case 1: // Regex
                hint = "\\b(free|win|prize)\\b.*";
                break;
            case 2: // Sender
                hint = "+1234567890 or *marketing*";
                break;
            case 3: // Contains
                hint = "win now";
                break;
            case 4: // Starts with
                hint = "ALERT:";
                break;
            case 5: // Ends with
                hint = "unsubscribe";
                break;
            default:
                hint = context.getString(R.string.filter_pattern_hint);
        }
        patternLayout.setHelperText(hint);
    }

    private void populateFields() {
        if (existingRule != null) {
            nameEditText.setText(existingRule.getName());
            descriptionEditText.setText(existingRule.getDescription());
            patternEditText.setText(existingRule.getPattern());
            categoryEditText.setText(existingRule.getCategory());

            // Set dropdown selections based on existing rule
            typeDropdown.setText(existingRule.getTypeDisplayName(), false);
            actionDropdown.setText(existingRule.getActionDisplayName(), false);
        } else {
            // Set default category
            categoryEditText.setText("General");
        }
    }

    private void setupValidation() {
        // Real-time pattern validation for regex type
        typeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 1) { // Regex type
                patternEditText.setOnFocusChangeListener((v, hasFocus) -> {
                    if (!hasFocus) {
                        validateRegexPattern();
                    }
                });
            } else {
                patternEditText.setOnFocusChangeListener(null);
                patternLayout.setError(null);
            }
            updatePatternHint(position);
        });
    }

    private boolean validateRegexPattern() {
        String pattern = patternEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(pattern) && typeDropdown.getText().toString().equals(context.getString(R.string.filter_type_regex))) {
            try {
                Pattern.compile(pattern);
                patternLayout.setError(null);
                return true;
            } catch (PatternSyntaxException e) {
                patternLayout.setError(context.getString(R.string.filter_regex_invalid) + ": " + e.getDescription());
                return false;
            }
        }
        return true;
    }

    private boolean validateAndSave() {
        // Clear previous errors
        nameEditText.setError(null);
        patternLayout.setError(null);

        // Get values
        String name = nameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String pattern = patternEditText.getText().toString().trim();
        String category = categoryEditText.getText().toString().trim();
        String typeText = typeDropdown.getText().toString();
        String actionText = actionDropdown.getText().toString();

        // Validate required fields
        boolean isValid = true;

        if (TextUtils.isEmpty(name)) {
            nameEditText.setError(context.getString(R.string.filter_name_required));
            isValid = false;
        }

        if (TextUtils.isEmpty(pattern)) {
            patternLayout.setError(context.getString(R.string.filter_pattern_required));
            isValid = false;
        }

        // Validate regex pattern if type is regex
        if (typeText.equals(context.getString(R.string.filter_type_regex))) {
            if (!validateRegexPattern()) {
                isValid = false;
            }
        }

        if (!isValid) {
            return false;
        }

        // Convert string selections to enums
        AdvancedContentFilter.FilterType type = getFilterTypeFromString(typeText);
        AdvancedContentFilter.FilterAction action = getFilterActionFromString(actionText);

        if (type == null || action == null) {
            Toast.makeText(context, "Invalid filter type or action selected", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Set default category if empty
        if (TextUtils.isEmpty(category)) {
            category = "General";
        }

        // Create or update filter rule
        AdvancedContentFilter.FilterRule rule;
        if (existingRule != null) {
            // Update existing rule by modifying the existing instance
            existingRule.setName(name);
            existingRule.setDescription(description);
            existingRule.setType(type);
            existingRule.setPattern(pattern);
            existingRule.setAction(action);
            existingRule.setCategory(category);
            rule = existingRule;
        } else {
            // Create new rule
            rule = new AdvancedContentFilter.FilterRule(name, type, action, pattern);
            rule.setDescription(description);
            rule.setCategory(category);
        }

        // Notify listener
        if (saveListener != null) {
            saveListener.onFilterSaved(rule);
        }

        return true;
    }

    private AdvancedContentFilter.FilterType getFilterTypeFromString(String typeText) {
        if (typeText.equals(context.getString(R.string.filter_type_keyword))) {
            return AdvancedContentFilter.FilterType.KEYWORD;
        } else if (typeText.equals(context.getString(R.string.filter_type_regex))) {
            return AdvancedContentFilter.FilterType.REGEX;
        } else if (typeText.equals(context.getString(R.string.filter_type_sender))) {
            return AdvancedContentFilter.FilterType.SENDER;
        } else if (typeText.equals(context.getString(R.string.filter_type_contains))) {
            return AdvancedContentFilter.FilterType.CONTAINS;
        } else if (typeText.equals(context.getString(R.string.filter_type_starts_with))) {
            return AdvancedContentFilter.FilterType.STARTS_WITH;
        } else if (typeText.equals(context.getString(R.string.filter_type_ends_with))) {
            return AdvancedContentFilter.FilterType.ENDS_WITH;
        }
        return null;
    }

    private AdvancedContentFilter.FilterAction getFilterActionFromString(String actionText) {
        if (actionText.equals(context.getString(R.string.filter_action_block))) {
            return AdvancedContentFilter.FilterAction.BLOCK;
        } else if (actionText.equals(context.getString(R.string.filter_action_skip))) {
            return AdvancedContentFilter.FilterAction.SKIP;
        } else if (actionText.equals(context.getString(R.string.filter_action_tag))) {
            return AdvancedContentFilter.FilterAction.TAG;
        }
        return null;
    }
} 