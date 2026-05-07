package com.timed.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Quản lý cấu hình Zoom API
 * 
 * Hỗ trợ tải credentials từ:
 * 1. SharedPreferences (được lưu từ backend hoặc cài đặt app)
 * 2. BuildConfig (được thiết lập tại build time)
 * 3. Environment variables/files
 */
public class ZoomConfigManager {
    private static final String TAG = "ZoomConfigManager";
    private static final String PREFS_NAME = "zoom_config";
    private static final String KEY_CLIENT_ID = "p1YMUwrKShSzNy5SeMRxA";
    private static final String KEY_CLIENT_SECRET = "rSmwl3A_SCqwi8RpyVkHJQ";
    private static final String KEY_ACCOUNT_ID = "zoom_account_id";

    private final SharedPreferences preferences;
    private static ZoomConfigManager instance;

    private ZoomConfigManager(Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Lấy singleton instance
     */
    public static synchronized ZoomConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new ZoomConfigManager(context);
        }
        return instance;
    }

    /**
     * Lấy Zoom Client ID
     */
    public String getClientId() {
        String clientId = preferences.getString(KEY_CLIENT_ID, null);
        
        // Ưu tiên lấy từ BuildConfig nếu không có trong Preferences
        if (clientId == null || clientId.isEmpty()) {
            clientId = com.timed.BuildConfig.ZOOM_CLIENT_ID;
        }

        if (clientId == null || clientId.equals("your_client_id_here") || clientId.isEmpty()) {
            Log.w(TAG, "Zoom Client ID not configured properly");
            return null;
        }
        return clientId;
    }

    /**
     * Lấy Zoom Client Secret
     */
    public String getClientSecret() {
        String clientSecret = preferences.getString(KEY_CLIENT_SECRET, null);

        // Ưu tiên lấy từ BuildConfig nếu không có trong Preferences
        if (clientSecret == null || clientSecret.isEmpty()) {
            clientSecret = com.timed.BuildConfig.ZOOM_CLIENT_SECRET;
        }

        if (clientSecret == null || clientSecret.equals("your_client_secret_here") || clientSecret.isEmpty()) {
            Log.w(TAG, "Zoom Client Secret not configured properly");
            return null;
        }
        return clientSecret;
    }

    /**
     * Lấy Zoom Account ID (tùy chọn)
     */
    public String getAccountId() {
        return preferences.getString(KEY_ACCOUNT_ID, null);
    }

    /**
     * Thiết lập Zoom Client ID
     */
    public void setClientId(String clientId) {
        preferences.edit().putString(KEY_CLIENT_ID, clientId).apply();
        Log.d(TAG, "Zoom Client ID updated");
    }

    /**
     * Thiết lập Zoom Client Secret
     */
    public void setClientSecret(String clientSecret) {
        preferences.edit().putString(KEY_CLIENT_SECRET, clientSecret).apply();
        Log.d(TAG, "Zoom Client Secret updated");
    }

    /**
     * Thiết lập Zoom Account ID
     */
    public void setAccountId(String accountId) {
        preferences.edit().putString(KEY_ACCOUNT_ID, accountId).apply();
        Log.d(TAG, "Zoom Account ID updated");
    }

    /**
     * Thiết lập tất cả credentials
     */
    public void setCredentials(String clientId, String clientSecret, String accountId) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_CLIENT_ID, clientId);
        editor.putString(KEY_CLIENT_SECRET, clientSecret);
        editor.putString(KEY_ACCOUNT_ID, accountId);
        editor.apply();
        Log.d(TAG, "Zoom credentials updated");
    }

    /**
     * Xóa tất cả credentials
     */
    public void clearCredentials() {
        preferences.edit()
                .remove(KEY_CLIENT_ID)
                .remove(KEY_CLIENT_SECRET)
                .remove(KEY_ACCOUNT_ID)
                .apply();
        Log.d(TAG, "Zoom credentials cleared");
    }

    /**
     * Kiểm tra xem credentials đã được cấu hình hay chưa
     */
    public boolean isConfigured() {
        String clientId = getClientId();
        String clientSecret = getClientSecret();
        return clientId != null && !clientId.isEmpty() &&
               clientSecret != null && !clientSecret.isEmpty();
    }
}
