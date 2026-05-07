package com.timed;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.timed.utils.FirebaseInitializer;
import com.timed.utils.NetworkConnectivityMonitor;
import com.timed.utils.ThemeManager;

public class MyApplication extends Application {

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

        // 1. Khởi tạo toàn bộ Firebase và cấu hình Offline
        FirebaseInitializer.getInstance().initialize(this);

        // 2. Khởi tạo Network Monitor (lắng nghe trạng thái mạng toàn cục)
        NetworkConnectivityMonitor.getInstance(this);
    }
}