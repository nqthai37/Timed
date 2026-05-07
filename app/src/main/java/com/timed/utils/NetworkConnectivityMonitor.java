package com.timed.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import androidx.annotation.NonNull;

public class NetworkConnectivityMonitor {
    
    private static NetworkConnectivityMonitor instance;
    private final ConnectivityManager connectivityManager;
    private final Context context;
    private boolean isConnected = false;
    private OnNetworkStatusChangeListener listener;
    
    private final ConnectivityManager.NetworkCallback networkCallback = 
            new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            isConnected = true;
            if (listener != null) {
                listener.onNetworkStatusChanged(true);
            }
        }
        
        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            isConnected = false;
            if (listener != null) {
                listener.onNetworkStatusChanged(false);
            }
        }
    };
    
    private NetworkConnectivityMonitor(Context context) {
        this.context = context;
        this.connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        startMonitoring();
    }
    
    public static synchronized NetworkConnectivityMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkConnectivityMonitor(context);
        }
        return instance;
    }
    
    private void startMonitoring() {
        if (connectivityManager != null) {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
            
            // Kiểm tra kết nối hiện tại
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = 
                        connectivityManager.getNetworkCapabilities(network);
                if (capabilities != null) {
                    isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                }
            }
        }
    }
    
    public boolean isNetworkConnected() {
        return isConnected;
    }
    
    public void setOnNetworkStatusChangeListener(OnNetworkStatusChangeListener listener) {
        this.listener = listener;
    }
    
    public void stopMonitoring() {
        if (connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
    
    public interface OnNetworkStatusChangeListener {
        void onNetworkStatusChanged(boolean isConnected);
    }
}
