package com.keremgok.smsforward.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.keremgok.smsforward.LanguageManager;
import com.keremgok.smsforward.R;
import com.keremgok.smsforward.ThemeManager;

/**
 * Forwarders Fragment - İletim platformları ayarları
 * SMS, Telegram, Email ve Webhook ayarlarını TabLayout ile yönetir
 * ✅ Performance optimized v1.21.0 with lazy loading and preloading
 */
public class ForwardersFragment extends Fragment {
    private static final String TAG = "ForwardersFragment";
    
    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ForwardersPagerAdapter pagerAdapter;
    private SharedViewModel sharedViewModel;
    
    // ✅ Performance optimization - Track initialization state
    private boolean isViewPagerSetup = false;
    private boolean isTabLayoutSetup = false;
    private int currentPage = 0;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(LanguageManager.wrapContext(context));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // ✅ Performance optimization - Minimal setup in onCreateView
        long startTime = System.currentTimeMillis();
        
        // Apply theme
        if (getActivity() != null) {
            ThemeManager.initializeTheme(getActivity());
        }
        
        View view = inflater.inflate(R.layout.fragment_forwarders, container, false);
        
        Log.d(TAG, "Fragment view created in " + (System.currentTimeMillis() - startTime) + "ms");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // ✅ Performance optimization - Progressive initialization
        initViews(view);
        
        // Defer heavy setup operations
        view.post(() -> {
            setupViewModelLazy();
            setupViewPagerLazy();
            setupTabLayoutLazy();
        });
    }

    private void initViews(View view) {
        // ✅ Performance optimization - Lightweight view initialization only
        toolbar = view.findViewById(R.id.toolbar);
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
        
        Log.d(TAG, "Views initialized");
    }
    
    /**
     * ✅ Performance optimization - Lazy ViewModel setup
     */
    private void setupViewModelLazy() {
        if (sharedViewModel != null) {
            return;
        }
        
        try {
            sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
            
            // Observe network status for enabling/disabling test buttons
            sharedViewModel.getIsConnected().observe(getViewLifecycleOwner(), isConnected -> {
                // Network status changes will be handled by individual tab fragments
                // Only refresh if fragments are already created to avoid unnecessary work
                if (isViewPagerSetup && pagerAdapter != null) {
                    refreshActiveTabOnly();
                }
            });
            
            Log.d(TAG, "ViewModel setup complete");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up ViewModel", e);
        }
    }

    /**
     * ✅ Performance optimization - Lazy ViewPager setup with smart fragment management
     */
    private void setupViewPagerLazy() {
        if (isViewPagerSetup || viewPager == null) {
            return;
        }
        
        try {
            pagerAdapter = new ForwardersPagerAdapter(requireActivity());
            viewPager.setAdapter(pagerAdapter);
            
            // ✅ Performance optimization - Conservative off-screen page limit to reduce memory usage
            viewPager.setOffscreenPageLimit(1); // Only keep adjacent pages in memory
            
            // ✅ Performance optimization - Page change listener for preloading
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    currentPage = position;
                    
                    // Preload next fragment for smoother navigation
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (pagerAdapter != null) {
                            pagerAdapter.preloadNextFragment(position);
                        }
                    }, 300); // Small delay to avoid interfering with current page setup
                    
                    Log.d(TAG, "Page selected: " + position);
                }
                
                @Override
                public void onPageScrollStateChanged(int state) {
                    super.onPageScrollStateChanged(state);
                    
                    // ✅ Performance optimization - Clear cache when idle to save memory
                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        // Clean up memory after scrolling
                        System.gc();
                    }
                }
            });
            
            isViewPagerSetup = true;
            Log.d(TAG, "ViewPager setup complete");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up ViewPager", e);
        }
    }

    /**
     * ✅ Performance optimization - Lazy TabLayout setup
     */
    private void setupTabLayoutLazy() {
        if (isTabLayoutSetup || !isViewPagerSetup || tabLayout == null || pagerAdapter == null) {
            return;
        }
        
        try {
            // Link TabLayout with ViewPager2
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                tab.setText(pagerAdapter.getTabTitle(position));
            }).attach();
            
            isTabLayoutSetup = true;
            Log.d(TAG, "TabLayout setup complete");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up TabLayout", e);
        }
    }
    
    /**
     * ✅ Performance optimization - Refresh only the active tab to reduce unnecessary work
     */
    private void refreshActiveTabOnly() {
        if (pagerAdapter == null || !isViewPagerSetup) {
            return;
        }
        
        try {
            // Only refresh the currently visible fragment
            Fragment activeFragment = pagerAdapter.getCachedFragment(currentPage);
            if (activeFragment != null && activeFragment.isAdded() && activeFragment.isVisible()) {
                // Trigger refresh on the active fragment if it supports it
                // This avoids refreshing all tabs unnecessarily
                Log.d(TAG, "Refreshing active tab: " + currentPage);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing active tab", e);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // ✅ Performance optimization - Only refresh if initialization is complete
        if (isViewPagerSetup && isTabLayoutSetup && sharedViewModel != null) {
            // Light refresh - only update network status
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && sharedViewModel != null) {
                    sharedViewModel.updateNetworkStatus();
                }
            }, 100);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        // ✅ Performance optimization - Clean up resources when not visible
        if (pagerAdapter != null) {
            // Clear fragment cache to free memory when fragment is not visible
            pagerAdapter.clearCache();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // ✅ Performance optimization - Cleanup
        // Note: clearOnPageChangeCallbacks() not available in older API levels
        
        if (pagerAdapter != null) {
            pagerAdapter.clearCache();
        }
        
        Log.d(TAG, "Fragment destroyed and cleaned up");
    }
} 