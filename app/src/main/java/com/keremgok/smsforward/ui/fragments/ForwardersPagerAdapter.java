package com.keremgok.smsforward.ui.fragments;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.keremgok.smsforward.R;
import com.keremgok.smsforward.ui.fragments.forwarders.EmailForwarderTabFragment;
import com.keremgok.smsforward.ui.fragments.forwarders.SmsForwarderTabFragment;
import com.keremgok.smsforward.ui.fragments.forwarders.TelegramForwarderTabFragment;
import com.keremgok.smsforward.ui.fragments.forwarders.WebhookForwarderTabFragment;

/**
 * ViewPager2 adapter for forwarder tabs
 * ✅ Performance optimized v1.21.0 with lazy fragment loading
 */
public class ForwardersPagerAdapter extends FragmentStateAdapter {
    
    // Tab configuration
    public static final String[] TAB_TITLES = {
        "📱 SMS",
        "📢 Telegram", 
        "📧 Email",
        "🌐 Webhook"
    };
    
    public static final int TAB_COUNT = TAB_TITLES.length;
    
    // Tab indices
    public static final int TAB_SMS = 0;
    public static final int TAB_TELEGRAM = 1;
    public static final int TAB_EMAIL = 2;
    public static final int TAB_WEBHOOK = 3;
    
    // ✅ Performance optimization - Fragment cache for lazy loading
    private final Fragment[] fragmentCache = new Fragment[TAB_COUNT];
    private final boolean[] fragmentCreated = new boolean[TAB_COUNT];
    
    public ForwardersPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // ✅ Performance optimization - Lazy fragment creation with caching
        if (fragmentCache[position] != null) {
            return fragmentCache[position];
        }

        Fragment fragment;
        switch (position) {
            case TAB_SMS:
                fragment = new SmsForwarderTabFragment();
                break;
            case TAB_TELEGRAM:
                fragment = new TelegramForwarderTabFragment();
                break;
            case TAB_EMAIL:
                fragment = new EmailForwarderTabFragment();
                break;
            case TAB_WEBHOOK:
                fragment = new WebhookForwarderTabFragment();
                break;
            default:
                // Fallback to SMS tab
                fragment = new SmsForwarderTabFragment();
                break;
        }

        // Cache the created fragment
        fragmentCache[position] = fragment;
        fragmentCreated[position] = true;

        return fragment;
    }
    
    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
    
    /**
     * Get tab title for position
     */
    public String getTabTitle(int position) {
        if (position >= 0 && position < TAB_TITLES.length) {
            return TAB_TITLES[position];
        }
        return "Unknown";
    }

    /**
     * ✅ Performance optimization - Check if fragment is created and cached
     */
    public boolean isFragmentCreated(int position) {
        return position >= 0 && position < TAB_COUNT && fragmentCreated[position];
    }

    /**
     * ✅ Performance optimization - Get cached fragment without creating new one
     */
    public Fragment getCachedFragment(int position) {
        if (position >= 0 && position < TAB_COUNT) {
            return fragmentCache[position];
        }
        return null;
    }

    /**
     * ✅ Performance optimization - Preload next fragment for smoother navigation
     */
    public void preloadNextFragment(int currentPosition) {
        int nextPosition = currentPosition + 1;
        if (nextPosition < TAB_COUNT && !fragmentCreated[nextPosition]) {
            // Create next fragment in background to improve perceived performance
            createFragment(nextPosition);
        }
    }

    /**
     * ✅ Performance optimization - Clear fragment cache for memory management
     */
    public void clearCache() {
        for (int i = 0; i < TAB_COUNT; i++) {
            fragmentCache[i] = null;
            fragmentCreated[i] = false;
        }
    }
} 