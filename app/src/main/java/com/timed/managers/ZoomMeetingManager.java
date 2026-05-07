package com.timed.managers;

import android.content.Context;
import android.util.Log;

import com.timed.models.ZoomMeetingResponse;
import com.timed.services.ZoomMeetingService;
import com.timed.utils.ZoomConfigManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Manager để tích hợp Zoom Meeting vào ứng dụng Timed
 * 
 * Sử dụng:
 * ZoomMeetingManager zoomManager = ZoomMeetingManager.getInstance(context);
 * zoomManager.createMeetingForEvent(eventId, eventName, startDate, endDate, callback);
 */
public class ZoomMeetingManager {
    private static final String TAG = "ZoomMeetingManager";
    private static ZoomMeetingManager instance;

    private final Context context;
    private final ZoomConfigManager configManager;
    private ZoomMeetingService zoomService;

    private ZoomMeetingManager(Context context) {
        this.context = context;
        this.configManager = ZoomConfigManager.getInstance(context);
        initializeZoomService();
    }

    /**
     * Lấy singleton instance
     */
    public static synchronized ZoomMeetingManager getInstance(Context context) {
        if (instance == null) {
            instance = new ZoomMeetingManager(context);
        }
        return instance;
    }

    /**
     * Khởi tạo Zoom Service
     */
    private void initializeZoomService() {
        if (configManager.isConfigured()) {
            zoomService = new ZoomMeetingService(
                    context,
                    configManager.getClientId(),
                    configManager.getClientSecret()
            );
            Log.d(TAG, "Zoom Service initialized successfully");
        } else {
            Log.w(TAG, "Zoom credentials not configured. Please set them before using Zoom features.");
        }
    }

    /**
     * Tạo cuộc họp Zoom cho sự kiện
     * 
     * @param eventId: ID của sự kiện
     * @param eventName: Tên sự kiện
     * @param startTime: Thời gian bắt đầu (milliseconds)
     * @param endTime: Thời gian kết thúc (milliseconds)
     * @param callback: Callback để xử lý kết quả
     */
    public void createMeetingForEvent(String eventId, String eventName, long startTime, long endTime, 
                                     ZoomMeetingCallback callback) {
        if (zoomService == null) {
            Log.e(TAG, "Zoom Service not initialized. Credentials are not configured.");
            callback.onFailure("Zoom chưa được cấu hình. Vui lòng kiểm tra cài đặt.");
            return;
        }

        try {
            // Tính toán thời lượng (phút)
            int durationMinutes = (int) ((endTime - startTime) / (1000 * 60));
            
            // Chuyển đổi thời gian thành định dạng ISO 8601
            String startTimeISO = convertToISO8601(startTime);
            
            // Tạo tiêu đề cuộc họp
            String meetingTopic = "Cuộc họp: " + eventName;

            Log.d(TAG, "Creating Zoom meeting - Event: " + eventName + ", Duration: " + durationMinutes + " min");

            // Gọi Zoom API
            zoomService.createZoomMeeting(meetingTopic, startTimeISO, durationMinutes,
                    new ZoomMeetingService.ZoomMeetingCallback() {
                        @Override
                        public void onSuccess(ZoomMeetingResponse response) {
                            Log.d(TAG, "Zoom meeting created successfully. Join URL: " + response.getJoinUrl());
                            callback.onSuccess(response);
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Log.e(TAG, "Failed to create Zoom meeting: " + errorMessage);
                            callback.onFailure(errorMessage);
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error creating Zoom meeting", e);
            callback.onFailure("Lỗi tạo cuộc họp: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin cuộc họp Zoom
     * 
     * @param meetingId: ID của cuộc họp
     * @param callback: Callback để xử lý kết quả
     */
    public void getMeetingDetails(long meetingId, ZoomMeetingCallback callback) {
        if (zoomService == null) {
            callback.onFailure("Zoom chưa được cấu hình");
            return;
        }

        zoomService.getMeetingDetails(meetingId, new ZoomMeetingService.ZoomMeetingCallback() {
            @Override
            public void onSuccess(ZoomMeetingResponse response) {
                callback.onSuccess(response);
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }

    /**
     * Cập nhật Zoom credentials (ví dụ: từ server)
     * 
     * @param clientId: Zoom Client ID
     * @param clientSecret: Zoom Client Secret
     */
    public void updateZoomCredentials(String clientId, String clientSecret) {
        configManager.setClientId(clientId);
        configManager.setClientSecret(clientSecret);
        
        // Khởi tạo lại service với credentials mới
        zoomService = new ZoomMeetingService(context, clientId, clientSecret);
        Log.d(TAG, "Zoom credentials updated");
    }

    /**
     * Kiểm tra xem Zoom đã được cấu hình hay chưa
     */
    public boolean isZoomConfigured() {
        return configManager.isConfigured();
    }

    /**
     * Xóa Zoom credentials
     */
    public void clearZoomCredentials() {
        configManager.clearCredentials();
        zoomService = null;
    }

    /**
     * Chuyển đổi thời gian (milliseconds) thành định dạng ISO 8601
     * Định dạng: yyyy-MM-ddTHH:mm:ssZ
     */
    private String convertToISO8601(long timeInMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(timeInMillis));
    }

    /**
     * Callback interface cho Zoom meeting operations
     */
    public interface ZoomMeetingCallback {
        void onSuccess(ZoomMeetingResponse response);
        void onFailure(String errorMessage);
    }
}
