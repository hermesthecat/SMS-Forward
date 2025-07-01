package com.keremgok.smsforward.ui.fragments;

import android.content.Context;
import android.os.Bundle;
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
 */
public class ForwardersFragment extends Fragment {
    private static final String TAG = "ForwardersFragment";
    
    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ForwardersPagerAdapter pagerAdapter;
    private SharedViewModel sharedViewModel;

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
        
        return inflater.inflate(R.layout.fragment_forwarders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupViewModel();
        setupViewPager();
        setupTabLayout();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
    }
    
    private void setupViewModel() {
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        
        // Observe network status for enabling/disabling test buttons
        sharedViewModel.getIsConnected().observe(getViewLifecycleOwner(), isConnected -> {
            // Network status changes will be handled by individual tab fragments
        });
    }

    private void setupViewPager() {
        pagerAdapter = new ForwardersPagerAdapter(requireActivity());
        viewPager.setAdapter(pagerAdapter);
        
        // Use default off-screen page limit for better performance
        viewPager.setOffscreenPageLimit(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT);
    }

    private void setupTabLayout() {
        // Link TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(pagerAdapter.getTabTitle(position));
        }).attach();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        if (sharedViewModel != null) {
            sharedViewModel.refreshAll();
        }
    }
} 