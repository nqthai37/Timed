package com.timed.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.timed.models.ZoomMeetingRequest;
import com.timed.models.ZoomMeetingResponse;
import com.timed.models.ZoomOAuthToken;

import java.io.IOException;
import java.util.Base64;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service để tích hợp Zoom Meeting API với OAuth 2.0
 * 
 * Yêu cầu cấu hình trong local.properties hoặc file .env:
 * - ZOOM_CLIENT_ID
 * - ZOOM_CLIENT_SECRET
 * - ZOOM_ACCOUNT_ID (tùy chọn, có thể lấy từ API)
 */
public class ZoomMeetingService {
    private static final String TAG = "ZoomMeetingService";

    // Zoom API Endpoints
    private static final String ZOOM_OAUTH_TOKEN_URL = "https://zoom.us/oauth/token";
    private static final String ZOOM_CREATE_MEETING_URL = "https://api.zoom.us/v2/users/me/meetings";
    private static final String ZOOM_GET_MEETING_URL = "https://api.zoom.us/v2/meetings/";

    // Preferences keys
    private static final String PREFS_NAME = "zoom_meeting_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_TOKEN_EXPIRATION = "token_expiration";
    private static final String KEY_TOKEN_TYPE = "token_type";

    private final Context context;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final SharedPreferences preferences;

    private String clientId;
    private String clientSecret;
    private ZoomOAuthToken cachedToken;

    // Callback interface
    public interface ZoomMeetingCallback {
        void onSuccess(ZoomMeetingResponse response);
        void onFailure(String errorMessage);
    }

    public interface ZoomTokenCallback {
        void onSuccess(ZoomOAuthToken token);
        void onFailure(String errorMessage);
    }

    public ZoomMeetingService(Context context, String clientId, String clientSecret) {
        this.context = context;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.cachedToken = loadTokenFromPreferences();
    }

    /**
     * Tạo cuộc họp Zoom
     * 
     * @param topic: Tiêu đề cuộc họp
     * @param startTime: Thời gian bắt đầu (định dạng ISO 8601: yyyy-MM-ddTHH:mm:ssZ)
     * @param duration: Thời lượng cuộc họp (phút)
     * @param callback: Callback để xử lý kết quả
     */
    public void createZoomMeeting(String topic, String startTime, int duration, ZoomMeetingCallback callback) {
        new Thread(() -> {
            try {
                // Đảm bảo token hợp lệ
                ZoomOAuthToken token = ensureValidToken();
                if (token == null || token.getAccessToken() == null) {
                    callback.onFailure("Không thể lấy Zoom access token");
                    return;
                }

                // Tạo request body
                ZoomMeetingRequest meetingRequest = new ZoomMeetingRequest(topic, startTime, duration);
                String requestBody = gson.toJson(meetingRequest);

                Log.d(TAG, "Creating meeting with request: " + requestBody);

                // Tạo HTTP request
                Request request = new Request.Builder()
                        .url(ZOOM_CREATE_MEETING_URL)
                        .header("Authorization", "Bearer " + token.getAccessToken())
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                        .build();

                // Thực hiện request
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Meeting created successfully: " + responseBody);

                        ZoomMeetingResponse meetingResponse = gson.fromJson(responseBody, ZoomMeetingResponse.class);
                        callback.onSuccess(meetingResponse);
                    } else {
                        String errorBody = response.body().string();
                        Log.e(TAG, "Failed to create meeting. Status: " + response.code() + ", Body: " + errorBody);
                        callback.onFailure("Lỗi tạo cuộc họp Zoom: " + response.code() + " - " + errorBody);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException while creating meeting", e);
                callback.onFailure("Lỗi kết nối: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error while creating meeting", e);
                callback.onFailure("Lỗi bất ngờ: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Lấy thông tin cuộc họp Zoom
     * 
     * @param meetingId: ID của cuộc họp
     * @param callback: Callback để xử lý kết quả
     */
    public void getMeetingDetails(long meetingId, ZoomMeetingCallback callback) {
        new Thread(() -> {
            try {
                ZoomOAuthToken token = ensureValidToken();
                if (token == null || token.getAccessToken() == null) {
                    callback.onFailure("Không thể lấy Zoom access token");
                    return;
                }

                Request request = new Request.Builder()
                        .url(ZOOM_GET_MEETING_URL + meetingId)
                        .header("Authorization", "Bearer " + token.getAccessToken())
                        .get()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        ZoomMeetingResponse meetingResponse = gson.fromJson(responseBody, ZoomMeetingResponse.class);
                        callback.onSuccess(meetingResponse);
                    } else {
                        callback.onFailure("Lỗi lấy thông tin cuộc họp: " + response.code());
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException while getting meeting details", e);
                callback.onFailure("Lỗi kết nối: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Đảm bảo token hợp lệ (lấy token mới nếu cần)
     */
    private synchronized ZoomOAuthToken ensureValidToken() throws IOException {
        // Kiểm tra cached token
        if (cachedToken != null && cachedToken.isValid()) {
            Log.d(TAG, "Using cached valid token");
            return cachedToken;
        }

        // Kiểm tra token từ preferences
        cachedToken = loadTokenFromPreferences();
        if (cachedToken != null && cachedToken.isValid()) {
            Log.d(TAG, "Using valid token from preferences");
            return cachedToken;
        }

        // Lấy token mới
        Log.d(TAG, "Fetching new token");
        return refreshToken();
    }

    /**
     * Làm mới OAuth token
     */
    private ZoomOAuthToken refreshToken() throws IOException {
        try {
            // Tạo authorization header (Base64 của client_id:client_secret)
            String credentials = clientId + ":" + clientSecret;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

            // Tạo request body
            RequestBody body = RequestBody.create(
                    "grant_type=client_credentials",
                    MediaType.parse("application/x-www-form-urlencoded")
            );

            Request request = new Request.Builder()
                    .url(ZOOM_OAUTH_TOKEN_URL)
                    .header("Authorization", "Basic " + encodedCredentials)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Token refreshed successfully");

                    // Parse response
                    ZoomOAuthToken token = gson.fromJson(responseBody, ZoomOAuthToken.class);
                    cachedToken = token;

                    // Lưu token vào preferences
                    saveTokenToPreferences(token);

                    return token;
                } else {
                    String errorBody = response.body().string();
                    Log.e(TAG, "Failed to refresh token. Status: " + response.code() + ", Body: " + errorBody);
                    throw new IOException("Failed to get Zoom token: " + response.code() + " - " + errorBody);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException while refreshing token", e);
            throw e;
        }
    }

    /**
     * Lưu token vào SharedPreferences
     */
    private void saveTokenToPreferences(ZoomOAuthToken token) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_ACCESS_TOKEN, token.getAccessToken());
        editor.putString(KEY_TOKEN_TYPE, token.getTokenType());
        editor.putLong(KEY_TOKEN_EXPIRATION, token.getExpirationTime());
        editor.apply();
    }

    /**
     * Tải token từ SharedPreferences
     */
    private ZoomOAuthToken loadTokenFromPreferences() {
        String accessToken = preferences.getString(KEY_ACCESS_TOKEN, null);
        String tokenType = preferences.getString(KEY_TOKEN_TYPE, null);
        long expirationTime = preferences.getLong(KEY_TOKEN_EXPIRATION, 0);

        if (accessToken != null && tokenType != null && expirationTime > 0) {
            ZoomOAuthToken token = new ZoomOAuthToken();
            token.setAccessToken(accessToken);
            token.setTokenType(tokenType);
            token.setExpirationTime(expirationTime);
            return token;
        }

        return null;
    }

    /**
     * Xóa token từ cache
     */
    public void clearToken() {
        cachedToken = null;
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_ACCESS_TOKEN);
        editor.remove(KEY_TOKEN_TYPE);
        editor.remove(KEY_TOKEN_EXPIRATION);
        editor.apply();
    }

    /**
     * Cập nhật thông tin đăng nhập
     */
    public void updateCredentials(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        clearToken(); // Xóa token cũ để lấy token mới với credentials mới
    }

    /**
     * Lấy token hiện tại (không làm mới)
     */
    public ZoomOAuthToken getCurrentToken() {
        return cachedToken;
    }
}
