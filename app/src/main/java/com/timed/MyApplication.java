package com.timed;

import android.app.Application;
import com.timed.utils.FirebaseInitializer;
import com.timed.utils.NetworkConnectivityMonitor;

public class MyApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 1. Khởi tạo toàn bộ Firebase và cấu hình Offline
        FirebaseInitializer.getInstance().initialize(this);
        
        // 2. Khởi tạo Network Monitor (lắng nghe trạng thái mạng toàn cục)
        NetworkConnectivityMonitor.getInstance(this);
    }
}