package com.keremgok.smsforward;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages network connectivity status and notifies listeners of changes.
 * Provides real-time monitoring of internet connectivity for the SMS Forward
 * app.
 */
public class NetworkStatusManager {
    private static final String TAG = "NetworkStatusManager";

    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final CopyOnWriteArrayList<NetworkStatusListener> listeners;
    private ConnectivityManager.NetworkCallback networkCallback;
    private volatile boolean isConnected = false;
    private volatile String connectionType = "Unknown";
    private static NetworkStatusManager instance;

    public interface NetworkStatusListener {
        void onNetworkStatusChanged(boolean isConnected, String connectionType);
    }

    private NetworkStatusManager(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listeners = new CopyOnWriteArrayList<>();

        // Initialize current status
        updateNetworkStatus();
    }

    /**
     * Get singleton instance of NetworkStatusManager
     */
    public static synchronized NetworkStatusManager getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkStatusManager(context);
        }
        return instance;
    }

    /**
     * Start monitoring network status changes
     */
    public void startMonitoring() {
        if (networkCallback != null) {
            Log.w(TAG, "Network monitoring already started");
            return;
        }

        Log.d(TAG, "Starting network status monitoring");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    Log.d(TAG, "Network available: " + network);
                    updateNetworkStatus();
                }

                @Override
                public void onLost(Network network) {
                    Log.d(TAG, "Network lost: " + network);
                    updateNetworkStatus();
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                    Log.d(TAG, "Network capabilities changed: " + network);
                    updateNetworkStatus();
                }
            };

            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        }

        // Also update status immediately
        updateNetworkStatus();
    }

    /**
     * Stop monitoring network status changes
     */
    public void stopMonitoring() {
        if (networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(TAG, "Stopping network status monitoring");
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    /**
     * Add a listener for network status changes
     */
    public void addListener(NetworkStatusListener listener) {
        listeners.add(listener);
        // Immediately notify new listener of current status
        listener.onNetworkStatusChanged(isConnected, connectionType);
    }

    /**
     * Remove a network status listener
     */
    public void removeListener(NetworkStatusListener listener) {
        listeners.remove(listener);
    }

    /**
     * Get current connection status
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Get current connection type
     */
    public String getConnectionType() {
        return connectionType;
    }

    /**
     * Get detailed connection status string
     */
    public String getConnectionStatus() {
        if (isConnected) {
            return "Online (" + connectionType + ")";
        } else {
            return "Offline";
        }
    }

    /**
     * Force update of network status
     */
    public void updateNetworkStatus() {
        boolean wasConnected = isConnected;
        String oldConnectionType = connectionType;

        if (connectivityManager == null) {
            isConnected = false;
            connectionType = "No ConnectivityManager";
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                isConnected = false;
                connectionType = "No Active Network";
            } else {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (capabilities == null) {
                    isConnected = false;
                    connectionType = "No Network Capabilities";
                } else {
                    isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    connectionType = getConnectionTypeFromCapabilities(capabilities);
                }
            }
        } else {
            // Fallback for older Android versions
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
            if (activeNetworkInfo != null) {
                connectionType = getConnectionTypeFromNetworkInfo(activeNetworkInfo);
            } else {
                connectionType = "No Network Info";
            }
        }

        Log.d(TAG, "Network status updated: " + getConnectionStatus());

        // Notify listeners if status changed
        if (wasConnected != isConnected || !oldConnectionType.equals(connectionType)) {
            notifyListeners();
        }
    }

    /**
     * Get connection type from NetworkCapabilities (API 23+)
     */
    private String getConnectionTypeFromCapabilities(NetworkCapabilities capabilities) {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return "WiFi";
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return "Mobile";
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return "Ethernet";
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
            return "Bluetooth";
        } else {
            return "Unknown";
        }
    }

    /**
     * Get connection type from NetworkInfo (Legacy)
     */
    private String getConnectionTypeFromNetworkInfo(android.net.NetworkInfo networkInfo) {
        switch (networkInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                return "WiFi";
            case ConnectivityManager.TYPE_MOBILE:
                return "Mobile";
            case ConnectivityManager.TYPE_ETHERNET:
                return "Ethernet";
            case ConnectivityManager.TYPE_BLUETOOTH:
                return "Bluetooth";
            default:
                return networkInfo.getTypeName();
        }
    }

    /**
     * Notify all listeners of status change
     */
    private void notifyListeners() {
        Log.d(TAG, "Notifying " + listeners.size() + " listeners of network status change");
        for (NetworkStatusListener listener : listeners) {
            try {
                listener.onNetworkStatusChanged(isConnected, connectionType);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying network status listener", e);
            }
        }
    }

    /**
     * Get network status emoji for UI display
     */
    public String getStatusEmoji() {
        if (!isConnected) {
            return "🔴"; // Red circle for offline
        }

        switch (connectionType) {
            case "WiFi":
                return "🟢"; // Green circle for WiFi
            case "Mobile":
                return "🟡"; // Yellow circle for mobile
            case "Ethernet":
                return "🔵"; // Blue circle for ethernet
            default:
                return "🟢"; // Green circle for other connected types
        }
    }

    /**
     * Check if we can forward messages (connected with internet capability)
     */
    public boolean canForwardMessages() {
        return isConnected;
    }

    /**
     * Get network quality indicator based on connection type
     */
    public String getNetworkQuality() {
        if (!isConnected) {
            return "No Connection";
        }

        switch (connectionType) {
            case "WiFi":
            case "Ethernet":
                return "Good";
            case "Mobile":
                return "Moderate";
            default:
                return "Unknown";
        }
    }
}