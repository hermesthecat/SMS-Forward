package com.keremgok.smsforward.ui.fragments;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.keremgok.smsforward.ui.fragments.forwarders.EmailForwarderTabFragment;
import com.keremgok.smsforward.ui.fragments.forwarders.SmsForwarderTabFragment;
import com.keremgok.smsforward.ui.fragments.forwarders.TelegramForwarderTabFragment;
import com.keremgok.smsforward.ui.fragments.forwarders.WebhookForwarderTabFragment;

/**
 * ViewPager2 adapter for managing forwarder tabs
 */
public class ForwardersPagerAdapter extends FragmentStateAdapter {
    
    // Tab titles and emojis
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
    
    public ForwardersPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_SMS:
                return new SmsForwarderTabFragment();
            case TAB_TELEGRAM:
                return new TelegramForwarderTabFragment();
            case TAB_EMAIL:
                return new EmailForwarderTabFragment();
            case TAB_WEBHOOK:
                return new WebhookForwarderTabFragment();
            default:
                throw new IllegalArgumentException("Invalid tab position: " + position);
        }
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
        return "";
    }
} 