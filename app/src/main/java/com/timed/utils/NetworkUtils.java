package com.timed.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public final class NetworkUtils {
    private NetworkUtils() {
    }

    public static boolean isOnline(Context context) {
        if (context == null) {
            return false;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        Network network = cm.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null) {
            return false;
        }
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
