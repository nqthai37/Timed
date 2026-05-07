# Zoom Meeting API Integration - Tóm Tắt

## Tổng Quan

Tích hợp Zoom Meeting API vào ứng dụng Timed với OAuth 2.0 Server-to-Server authentication. Người dùng có thể tạo các cuộc họp Zoom cho sự kiện/nhiệm vụ và nhận được link tham gia.

## Cấu Trúc Tệp Được Tạo

### Services (`com/timed/services/`)
- **ZoomMeetingService.java** - Service chính xử lý tất cả API Zoom
  - OAuth 2.0 token management
  - Tạo cuộc họp
  - Lấy thông tin cuộc họp
  - Auto token refresh

### Managers (`com/timed/managers/`)
- **ZoomMeetingManager.java** - Manager tích hợp vào app logic
  - Singleton pattern
  - Wrapper cho ZoomMeetingService
  - Tích hợp với Event/Task models

### Models (`com/timed/models/`)
- **ZoomMeetingRequest.java** - Model cho request (topic, time, duration, settings)
- **ZoomMeetingResponse.java** - Model cho response (join_url, id, etc.)
- **ZoomOAuthToken.java** - Model cho OAuth token với expiration tracking

### Utils (`com/timed/utils/`)
- **ZoomConfigManager.java** - Quản lý Zoom credentials
  - Lưu/load từ SharedPreferences
  - Kiểm tra cấu hình
  - Singleton pattern

### Examples & Documentation
- **ZoomMeetingExampleActivity.java** - Ví dụ đầy đủ cách sử dụng
- **ZOOM_INTEGRATION_GUIDE.md** - Hướng dẫn cấu hình chi tiết
- **ZOOM_TESTING_GUIDE.md** - Hướng dẫn testing và debugging

## Tính Năng

✅ **OAuth 2.0 Server-to-Server**
- Automatic token generation
- Token caching & persistence
- Auto-refresh khi hết hạn

✅ **Meeting Management**
- Tạo scheduled meetings
- Lấy thông tin meeting
- Tùy chỉnh cài đặt (video, waiting room, etc.)

✅ **Error Handling**
- Network error handling
- Invalid credentials handling
- Rate limiting handling

✅ **Performance**
- Thread-safe token management
- Async API calls (không block UI)
- Efficient caching

✅ **User-Friendly**
- Simple API interface
- Clear error messages
- Vietnamese language support

## Quick Start

### 1. Cấu Hình Zoom OAuth

```java
// Lấy ClientId và ClientSecret từ Zoom Developer Console
ZoomConfigManager.getInstance(context).setCredentials(
    "YOUR_CLIENT_ID",
    "YOUR_CLIENT_SECRET",
    null
);
```

### 2. Tạo Cuộc Họp

```java
ZoomMeetingManager zoomManager = ZoomMeetingManager.getInstance(context);

zoomManager.createMeetingForEvent(
    "event_id",
    "Event Name",
    startTime,      // milliseconds
    endTime,        // milliseconds
    new ZoomMeetingManager.ZoomMeetingCallback() {
        @Override
        public void onSuccess(ZoomMeetingResponse response) {
            String joinUrl = response.getJoinUrl();
            // Hiển thị cho user hoặc lưu vào database
        }

        @Override
        public void onFailure(String errorMessage) {
            // Xử lý lỗi
        }
    }
);
```

### 3. Mở Link Zoom

```java
Intent intent = new Intent(Intent.ACTION_VIEW);
intent.setData(Uri.parse(joinUrl));
startActivity(intent);
```

## API Reference

### ZoomMeetingManager

#### `getInstance(Context context)`
Lấy singleton instance

#### `createMeetingForEvent(String eventId, String eventName, long startTime, long endTime, ZoomMeetingCallback callback)`
Tạo cuộc họp Zoom cho sự kiện

| Parameter | Type | Description |
|-----------|------|-------------|
| eventId | String | ID sự kiện |
| eventName | String | Tên sự kiện |
| startTime | long | Thời gian bắt đầu (ms) |
| endTime | long | Thời gian kết thúc (ms) |
| callback | ZoomMeetingCallback | Callback kết quả |

#### `getMeetingDetails(long meetingId, ZoomMeetingCallback callback)`
Lấy thông tin cuộc họp

#### `isZoomConfigured()`
Kiểm tra xem Zoom đã cấu hình hay chưa

#### `updateZoomCredentials(String clientId, String clientSecret)`
Cập nhật credentials

### ZoomConfigManager

#### `setCredentials(String clientId, String clientSecret, String accountId)`
Lưu Zoom credentials

#### `getClientId()`, `getClientSecret()`
Lấy credentials

#### `isConfigured()`
Kiểm tra cấu hình

#### `clearCredentials()`
Xóa credentials

## Data Models

### ZoomMeetingRequest
```java
new ZoomMeetingRequest(
    "Họp Đội Phát Triển",     // topic
    "2024-05-20T14:00:00Z",   // startTime (ISO 8601)
    60                         // duration (minutes)
);
```

### ZoomMeetingResponse
```java
response.getJoinUrl();      // https://zoom.us/j/123456789?pwd=...
response.getId();           // Meeting ID
response.getTopic();        // Tên cuộc họp
response.getPassword();     // Mật khẩu (nếu có)
response.getStartTime();    // Thời gian bắt đầu
response.getDuration();     // Thời lượng (phút)
```

## Integration Examples

### Tích hợp vào Event/Task Activity

```java
public class EventDetailActivity extends AppCompatActivity {
    private ZoomMeetingManager zoomManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        
        zoomManager = ZoomMeetingManager.getInstance(this);
        
        Button btnCreateZoom = findViewById(R.id.btnCreateZoom);
        btnCreateZoom.setOnClickListener(v -> createZoomMeeting());
    }
    
    private void createZoomMeeting() {
        if (!zoomManager.isZoomConfigured()) {
            showConfigurationDialog();
            return;
        }
        
        zoomManager.createMeetingForEvent(
            event.getId(),
            event.getTitle(),
            event.getStartTime(),
            event.getEndTime(),
            new ZoomMeetingManager.ZoomMeetingCallback() {
                @Override
                public void onSuccess(ZoomMeetingResponse response) {
                    event.setZoomJoinUrl(response.getJoinUrl());
                    saveEventToDatabase();
                    showSuccessMessage();
                }

                @Override
                public void onFailure(String errorMessage) {
                    showErrorMessage(errorMessage);
                }
            }
        );
    }
}
```

## Database Integration

Thêm fields vào Event/Task model:

```java
public class Event {
    private String id;
    private String title;
    private long startTime;
    private long endTime;
    
    // Zoom fields
    private long zoomMeetingId;
    private String zoomJoinUrl;
    private String zoomPassword;
    
    // getters/setters...
}
```

Lưu vào Firestore:

```java
db.collection("events")
    .document(event.getId())
    .update(
        "zoomMeetingId", response.getId(),
        "zoomJoinUrl", response.getJoinUrl(),
        "zoomPassword", response.getPassword()
    );
```

## Error Handling

Các lỗi phổ biến:

| Lỗi | Nguyên Nhân | Giải Pháp |
|-----|----------|---------|
| 401 Unauthorized | Invalid credentials | Kiểm tra Client ID/Secret |
| Network error | Không có internet | Kiểm tra kết nối mạng |
| 400 Bad Request | Invalid parameters | Kiểm tra format startTime |
| Service not initialized | Credentials chưa set | Setup credentials trước |

## Performance Tips

1. **Caching**: Token được tự động cache trong SharedPreferences
2. **Async**: Tất cả API calls chạy trên background thread
3. **Retry Logic**: Implement exponential backoff cho network errors
4. **Batch Operations**: Tạo multiple meetings cùng lúc nếu cần

## Security Considerations

🔒 **Bảo Mật**
- Client Secret không được hardcode
- Sử dụng BuildConfig hoặc SharedPreferences
- HTTPS tất cả requests
- Validate input parameters
- Không log sensitive data

## Permissions

Thêm vào `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Dependencies

Đã có sẵn trong project:
- OkHttp3 (HTTP client)
- Gson (JSON serialization)
- Firebase (authentication)

Không cần thêm dependencies!

## Testing

Xem `ZOOM_TESTING_GUIDE.md` cho:
- Unit tests
- Manual testing checklist
- Test scenarios
- Debugging commands

## Troubleshooting

### Token không hoạt động
```
→ Xóa app cache
→ Xoá SharedPreferences
→ Kiểm tra credentials
```

### Meeting không được tạo
```
→ Kiểm tra internet connection
→ Xác minh startTime format (ISO 8601)
→ Kiểm tra Zoom API quota
```

### UI frozen
```
→ Tất cả API calls đã chạy trên background thread
→ Kiểm tra Logcat cho ANR
```

## Next Steps

1. ✅ Tạo Zoom OAuth App
2. ✅ Integrate vào Event/Task activities
3. ✅ Thêm UI buttons cho "Create Meeting"
4. ✅ Lưu join_url vào database
5. ✅ Display join link trong event details
6. ✅ Test end-to-end
7. ✅ Deploy!

## Support & Documentation

- [Zoom API Docs](https://developers.zoom.us/docs/api/meetings/)
- [OAuth 2.0 Guide](https://developers.zoom.us/docs/internal-apps/s2s-oauth/)
- [Android Documentation](https://developer.android.com/)

## Files Summary

```
📦 Created Files:
├── services/
│   └── ZoomMeetingService.java          (300+ lines)
├── managers/
│   └── ZoomMeetingManager.java          (250+ lines)
├── models/
│   ├── ZoomMeetingRequest.java          (100+ lines)
│   ├── ZoomMeetingResponse.java         (150+ lines)
│   └── ZoomOAuthToken.java              (80+ lines)
├── utils/
│   └── ZoomConfigManager.java           (120+ lines)
├── examples/
│   └── ZoomMeetingExampleActivity.java  (300+ lines)
├── ZOOM_INTEGRATION_GUIDE.md            (Comprehensive guide)
└── ZOOM_TESTING_GUIDE.md                (Testing procedures)
```

---

**Tổng cộng**: ~1500+ lines of code + comprehensive documentation

Tất cả đã sẵn sàng để tích hợp vào ứng dụng Timed! 🚀
