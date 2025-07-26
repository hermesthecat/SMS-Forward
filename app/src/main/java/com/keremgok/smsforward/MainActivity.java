package com.keremgok.smsforward;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * New MainActivity with multi-screen fragment navigation.
 * Preserves all original authentication and security functionality.
 */
public class MainActivity extends AppCompatActivity {

    private SecurityManager securityManager;
    private static final int REQUEST_CODE_AUTHENTICATION = 1001;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme before calling super.onCreate()
        ThemeManager.initializeTheme(this);

        super.onCreate(savedInstanceState);
        
        // Initialize security manager
        securityManager = new SecurityManager(this);
        
        // Check if authentication is required
        if (securityManager.needsAuthentication()) {
            // Start authentication activity
            Intent authIntent = AuthenticationActivity.createIntent(this, AuthenticationActivity.AUTH_TYPE_STARTUP);
            startActivityForResult(authIntent, REQUEST_CODE_AUTHENTICATION);
            return; // Don't continue with normal initialization until authenticated
        }
        
        // Continue with normal initialization
        initializeMainActivity();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_AUTHENTICATION) {
            if (resultCode == RESULT_OK) {
                // Authentication successful, continue with initialization
                initializeMainActivity();
            } else {
                // Authentication failed or cancelled, close the app
                finishAffinity();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check authentication when app comes back to foreground
        if (securityManager != null && securityManager.needsAuthentication()) {
            Intent authIntent = AuthenticationActivity.createIntent(this, AuthenticationActivity.AUTH_TYPE_STARTUP);
            startActivityForResult(authIntent, REQUEST_CODE_AUTHENTICATION);
        }
    }
    
    private void initializeMainActivity() {
        setContentView(R.layout.activity_main);

        // Request permissions
        requestPermissions(new String[] {
                Manifest.permission.SEND_SMS,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
        }, 0);

        // Initialize bottom navigation
        setupBottomNavigation();

        // Load default fragment (Dashboard) if no fragment is currently loaded
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            loadFragment(new DashboardFragment());
        }
    }

    /**
     * Set up bottom navigation view and fragment switching
     */
    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                Fragment selectedFragment = null;
                
                int itemId = item.getItemId();
                if (itemId == R.id.nav_dashboard_item) {
                    selectedFragment = new DashboardFragment();
                } else if (itemId == R.id.nav_platforms_item) {
                    selectedFragment = new PlatformsFragment();
                } else if (itemId == R.id.nav_security_item) {
                    // TODO: Implement SecurityFragment  
                    selectedFragment = createPlaceholderFragment("Security", "Security settings will be implemented next");
                } else if (itemId == R.id.nav_monitor_item) {
                    // TODO: Implement MonitorFragment
                    selectedFragment = createPlaceholderFragment("Monitor", "Monitoring tools will be implemented next");
                } else if (itemId == R.id.nav_about_item) {
                    selectedFragment = new AboutFragment();
                }
                
                if (selectedFragment != null) {
                    return loadFragment(selectedFragment);
                }
                
                return false;
            });
            
            // Set default selection to Dashboard
            bottomNavigationView.setSelectedItemId(R.id.nav_dashboard_item);
        }
    }

    /**
     * Load a fragment into the container
     */
    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    /**
     * Create a placeholder fragment for fragments not yet implemented
     */
    private Fragment createPlaceholderFragment(String title, String message) {
        return PlaceholderFragment.newInstance(title, message);
    }

    /**
     * Simple placeholder fragment for unimplemented screens
     */
    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_TITLE = "title";
        private static final String ARG_MESSAGE = "message";

        public static PlaceholderFragment newInstance(String title, String message) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putString(ARG_TITLE, title);
            args.putString(ARG_MESSAGE, message);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, 
                android.view.ViewGroup container, Bundle savedInstanceState) {
            
            // Create a simple layout programmatically
            android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setPadding(32, 32, 32, 32);
            layout.setGravity(android.view.Gravity.CENTER);

            // Title
            android.widget.TextView titleView = new android.widget.TextView(getContext());
            String title = getArguments() != null ? getArguments().getString(ARG_TITLE, "Title") : "Title";
            titleView.setText(title);
            titleView.setTextSize(24);
            titleView.setTypeface(null, android.graphics.Typeface.BOLD);
            titleView.setPadding(0, 0, 0, 16);
            layout.addView(titleView);

            // Message
            android.widget.TextView messageView = new android.widget.TextView(getContext());
            String message = getArguments() != null ? getArguments().getString(ARG_MESSAGE, "Coming soon...") : "Coming soon...";
            messageView.setText(message);
            messageView.setTextSize(16);
            messageView.setGravity(android.view.Gravity.CENTER);
            layout.addView(messageView);

            return layout;
        }
    }
}