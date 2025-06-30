package com.keremgok.smsforward.ui.fragments;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.keremgok.smsforward.MessageHistoryDbHelper;
import com.keremgok.smsforward.MessageStatsDbHelper;
import com.keremgok.smsforward.NetworkStatusManager;
import com.keremgok.smsforward.SecurityManager;

/**
 * Shared ViewModel for managing common state across fragments
 */
public class SharedViewModel extends AndroidViewModel implements NetworkStatusManager.NetworkStatusListener {
    private static final String TAG = "SharedViewModel";
    
    // Network status
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>();
    private final MutableLiveData<String> connectionType = new MutableLiveData<>();
    
    // Statistics
    private final MutableLiveData<MessageStatsDbHelper.DailyStats> todayStats = new MutableLiveData<>();
    private final MutableLiveData<MessageStatsDbHelper.TotalStats> totalStats = new MutableLiveData<>();
    
    // Security status
    private final MutableLiveData<Boolean> securityEnabled = new MutableLiveData<>();
    
    // Managers
    private NetworkStatusManager networkStatusManager;
    private MessageStatsDbHelper statsDbHelper;
    private SecurityManager securityManager;
    
    public SharedViewModel(@NonNull Application application) {
        super(application);
        initializeManagers();
    }
    
    private void initializeManagers() {
        networkStatusManager = NetworkStatusManager.getInstance(getApplication());
        statsDbHelper = new MessageStatsDbHelper(getApplication());
        securityManager = new SecurityManager(getApplication());
        
        // Set initial values
        updateNetworkStatus();
        updateStats();
        updateSecurityStatus();
        
        // Start monitoring
        networkStatusManager.addListener(this);
        networkStatusManager.startMonitoring();
    }
    
    // Network Status
    public LiveData<Boolean> getIsConnected() {
        return isConnected;
    }
    
    public LiveData<String> getConnectionType() {
        return connectionType;
    }
    
    public void updateNetworkStatus() {
        networkStatusManager.updateNetworkStatus();
        isConnected.setValue(networkStatusManager.isConnected());
        connectionType.setValue(networkStatusManager.getConnectionType());
    }
    
    @Override
    public void onNetworkStatusChanged(boolean connected, String type) {
        isConnected.postValue(connected);
        connectionType.postValue(type);
    }
    
    // Statistics
    public LiveData<MessageStatsDbHelper.DailyStats> getTodayStats() {
        return todayStats;
    }
    
    public LiveData<MessageStatsDbHelper.TotalStats> getTotalStats() {
        return totalStats;
    }
    
    public void updateStats() {
        try {
            todayStats.setValue(statsDbHelper.getTodayStats());
            totalStats.setValue(statsDbHelper.getTotalStats());
        } catch (Exception e) {
            // Handle error
        }
    }
    
    // Security
    public LiveData<Boolean> getSecurityEnabled() {
        return securityEnabled;
    }
    
    public void updateSecurityStatus() {
        securityEnabled.setValue(securityManager.isSecurityEnabled());
    }
    
    // Refresh all data
    public void refreshAll() {
        updateNetworkStatus();
        updateStats();
        updateSecurityStatus();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (networkStatusManager != null) {
            networkStatusManager.removeListener(this);
            networkStatusManager.stopMonitoring();
        }
        if (statsDbHelper != null) {
            statsDbHelper.close();
        }
    }
} 