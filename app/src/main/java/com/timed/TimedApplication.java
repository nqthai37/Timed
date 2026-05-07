package com.timed;

import android.app.Application;
import android.util.Log;
import com.timed.utils.FirebaseInitializer;
import com.timed.utils.NetworkConnectivityMonitor;

public class TimedApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            FirebaseInitializer.getInstance().initialize(this);
            NetworkConnectivityMonitor.getInstance(this);
            Log.d("TimedApp", "Global infrastructure initialized");
        } catch (Exception e) {
            Log.e("TimedApp", "Firebase init failed", e);
        }
    }
}
