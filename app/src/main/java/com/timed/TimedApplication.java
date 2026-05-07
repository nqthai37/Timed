package com.timed;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.timed.utils.FirebaseInitializer;
import com.timed.utils.NetworkConnectivityMonitor;
import com.timed.utils.ThemeManager;

public class TimedApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applyNightMode(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityPreCreated(Activity activity, Bundle savedInstanceState) {
                ThemeManager.applyTheme(activity);
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });

        // Initialize Firebase and connectivity monitoring for background activities.
        FirebaseInitializer.getInstance().initialize(this);
        NetworkConnectivityMonitor.getInstance(this);
    }
}
