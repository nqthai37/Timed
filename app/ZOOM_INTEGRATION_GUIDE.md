# Hướng dẫn Tích hợp Zoom Meeting API

## Giới thiệu

Phần này hướng dẫn cách tích hợp Zoom Meeting API vào ứng dụng Timed sử dụng OAuth 2.0 Server-to-Server authentication.

## Cấu trúc Tệp

```
services/
  ├── ZoomMeetingService.java       # Service chính - Xử lý API Zoom
  └── EventNotificationReceiver.java

managers/
  ├── ZoomMeetingManager.java       # Manager - Tích hợp vào app logic
  └── TasksManager.java

models/
  ├── ZoomMeetingRequest.java      # Model cho request
  ├── ZoomMeetingResponse.java     # Model cho response
  └── ZoomOAuthToken.java          # Model cho OAuth token

utils/
  ├── ZoomConfigManager.java       # Quản lý cấu hình Zoom
  └── NetworkUtils.java
```

## Bước 1: Cấu Hình Zoom OAuth

### 1.1 Tạo OAuth App trên Zoom

1. Truy cập [Zoom App Marketplace](https://marketplace.zoom.us)
2. Đăng nhập bằng tài khoản Zoom
3. Chọn **"Build App"** → **"Server-to-Server OAuth"**
4. Điền thông tin app:
   - **App Name**: "Timed App"
   - **Publish Status**: Keep as is
5. Sau khi tạo, lấy thông tin:
   - **Client ID**
   - **Client Secret**
   - **Account ID** (tùy chọn)

### 1.2 Thiết lập Credentials trong App

#### Cách 1: Sử dụng SharedPreferences (Dễ nhất)

```java
// Trong Activity hoặc ViewModel
ZoomConfigManager configManager = ZoomConfigManager.getInstance(context);
configManager.setCredentials(
    "YOUR_CLIENT_ID",
    "YOUR_CLIENT_SECRET",
    "YOUR_ACCOUNT_ID"
);
```

#### Cách 2: Sử dụng BuildConfig (Bảo mật hơn)

Thêm vào `local.properties`:
```properties
ZOOM_CLIENT_ID=your_client_id_here
ZOOM_CLIENT_SECRET=your_client_secret_here
```

Cập nhật `app/build.gradle.kts`:
```kotlin
val properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())

buildConfigField("String", "ZOOM_CLIENT_ID", "\"${properties.getProperty("ZOOM_CLIENT_ID")}\"")
buildConfigField("String", "ZOOM_CLIENT_SECRET", "\"${properties.getProperty("ZOOM_CLIENT_SECRET")}\"")
```

Sử dụng trong code:
```java
ZoomConfigManager configManager = ZoomConfigManager.getInstance(context);
configManager.setCredentials(BuildConfig.ZOOM_CLIENT_ID, BuildConfig.ZOOM_CLIENT_SECRET, null);
```

## Bước 2: Sử Dụng API

### 2.1 Tạo Cuộc Họp Zoom

```java
// Lấy manager
ZoomMeetingManager zoomManager = ZoomMeetingManager.getInstance(context);

// Kiểm tra cấu hình
if (!zoomManager.isZoomConfigured()) {
    Toast.makeText(context, "Zoom chưa được cấu hình", Toast.LENGTH_SHORT).show();
    return;
}

// Tạo cuộc họp
long startTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 giờ sau
long endTime = startTime + (90 * 60 * 1000); // 90 phút

zoomManager.createMeetingForEvent(
    "event_id_123",
    "Họp Định Kỳ Đội Phát Triển",
    startTime,
    endTime,
    new ZoomMeetingManager.ZoomMeetingCallback() {
        @Override
        public void onSuccess(ZoomMeetingResponse response) {
            String joinUrl = response.getJoinUrl();
            Log.d(TAG, "Cuộc họp được tạo: " + joinUrl);
            
            // Hiển thị URL cho người dùng
            // Hoặc lưu joinUrl vào database
            Toast.makeText(context, "Cuộc họp tạo thành công!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailure(String errorMessage) {
            Log.e(TAG, "Lỗi tạo cuộc họp: " + errorMessage);
            Toast.makeText(context, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
        }
    }
);
```

### 2.2 Lấy Thông Tin Cuộc Họp

```java
zoomManager.getMeetingDetails(
    meetingId,
    new ZoomMeetingManager.ZoomMeetingCallback() {
        @Override
        public void onSuccess(ZoomMeetingResponse response) {
            String joinUrl = response.getJoinUrl();
            String password = response.getPassword();
            Log.d(TAG, "Join URL: " + joinUrl);
        }

        @Override
        public void onFailure(String errorMessage) {
            Log.e(TAG, "Lỗi: " + errorMessage);
        }
    }
);
```

### 2.3 Tích hợp vào Activity

```java
public class EventDetailActivity extends AppCompatActivity {
    private ZoomMeetingManager zoomManager;
    private Button btnCreateZoomMeeting;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        
        zoomManager = ZoomMeetingManager.getInstance(this);
        btnCreateZoomMeeting = findViewById(R.id.btnCreateZoomMeeting);
        
        btnCreateZoomMeeting.setOnClickListener(v -> {
            // Lấy thông tin sự kiện
            String eventName = "Bối cảnh dự án";
            long startTime = event.getStartTime();
            long endTime = event.getEndTime();
            
            // Tạo cuộc họp
            zoomManager.createMeetingForEvent(
                event.getId(),
                eventName,
                startTime,
                endTime,
                handleZoomResponse()
            );
        });
    }
    
    private ZoomMeetingManager.ZoomMeetingCallback handleZoomResponse() {
        return new ZoomMeetingManager.ZoomMeetingCallback() {
            @Override
            public void onSuccess(ZoomMeetingResponse response) {
                // Lưu join_url vào database
                event.setZoomJoinUrl(response.getJoinUrl());
                updateEventInDatabase();
                
                // Hiển thị thông báo
                showSuccessDialog(response.getJoinUrl());
            }

            @Override
            public void onFailure(String errorMessage) {
                showErrorDialog(errorMessage);
            }
        };
    }
}
```

## Bước 3: Lưu Trữ Join URL

### 3.1 Thêm trường vào Model Event

```java
public class Event {
    private String id;
    private String title;
    private long startTime;
    private long endTime;
    private String zoomJoinUrl;      // THÊM DÒNG NÀY
    private long zoomMeetingId;      // THÊM DÒNG NÀY
    
    // ... getters and setters
}
```

### 3.2 Lưu vào Firestore

```java
private void updateEventInDatabase() {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    
    db.collection("events")
        .document(event.getId())
        .update(
            "zoomJoinUrl", event.getZoomJoinUrl(),
            "zoomMeetingId", event.getZoomMeetingId()
        )
        .addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Event updated with Zoom meeting info");
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Failed to update event", e);
        });
}
```

## Bước 4: Hiển Thị Join Link cho Người Dùng

### 4.1 Thêm Button tham gia cuộc họp

```xml
<!-- activity_event_detail.xml -->
<Button
    android:id="@+id/btnJoinZoomMeeting"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Tham gia Zoom Meeting"
    android:visibility="gone" />
```

### 4.2 Xử lý Click và Mở URL

```java
Button btnJoinZoomMeeting = findViewById(R.id.btnJoinZoomMeeting);

if (event.getZoomJoinUrl() != null) {
    btnJoinZoomMeeting.setVisibility(View.VISIBLE);
    btnJoinZoomMeeting.setOnClickListener(v -> {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(event.getZoomJoinUrl()));
        startActivity(intent);
    });
}
```

## Bước 5: Xử Lý Lỗi và Edge Cases

### 5.1 Kiểm tra Kết nối Mạng

```java
import com.timed.utils.NetworkUtils;

if (!NetworkUtils.isOnline(context)) {
    Toast.makeText(context, "Kiểm tra kết nối Internet", Toast.LENGTH_SHORT).show();
    return;
}
```

### 5.2 Xử Lý Token Hết Hạn

Service tự động xử lý token refresh. Nếu token hết hạn, service sẽ tự động lấy token mới.

### 5.3 Xóa Credentials (Logout)

```java
// Khi user logout
ZoomMeetingManager zoomManager = ZoomMeetingManager.getInstance(context);
zoomManager.clearZoomCredentials();
```

## Bước 6: Quyền Truy Cập (Permissions)

Thêm vào `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## API Endpoints

| Hành Động | Method | URL |
|----------|--------|-----|
| Tạo Cuộc Họp | POST | `https://api.zoom.us/v2/users/me/meetings` |
| Lấy Thông Tin | GET | `https://api.zoom.us/v2/meetings/{meetingId}` |
| Cập Nhật | PATCH | `https://api.zoom.us/v2/meetings/{meetingId}` |
| Xóa | DELETE | `https://api.zoom.us/v2/meetings/{meetingId}` |

## Tham Số Request

### createZoomMeeting(topic, startTime, duration)

**Topic**: Tên cuộc họp (ví dụ: "Họp Đội Phát Triển")

**StartTime**: Định dạng ISO 8601, ví dụ:
```
2024-05-15T14:00:00Z
```

**Duration**: Thời lượng tính bằng phút (ví dụ: 60)

## Đặc Tính Của ZoomMeetingService

- ✅ OAuth 2.0 Server-to-Server Authentication
- ✅ Tự động làm mới token khi hết hạn
- ✅ Lưu token vào SharedPreferences để tối ưu hiệu suất
- ✅ Xử lý lỗi toàn diện
- ✅ Thread-safe token management
- ✅ Hỗ trợ cài đặt cuộc họp (video, waiting room, etc.)

## Cài Đặt Cuộc Họp

Có thể tùy chỉnh các cài đặt qua `ZoomMeetingRequest.MeetingSettings`:

```java
ZoomMeetingRequest.MeetingSettings settings = new ZoomMeetingRequest.MeetingSettings();
settings.setHostVideo(true);           // Bật video chủ trì
settings.setParticipantVideo(true);    // Bật video người tham gia
settings.setJoinBeforeHost(false);     // Không cho tham gia trước chủ trì
settings.setMuteUponEntry(false);      // Không mute khi tham gia
settings.setWaitingRoom(false);        // Không dùng waiting room
settings.setAutoRecording("none");     // Không ghi âm tự động
```

## Debugging

### Bật Log Chi Tiết

```java
// ZoomMeetingService sử dụng android.util.Log
// Kiểm tra Logcat với TAG: "ZoomMeetingService"

adb logcat | grep ZoomMeetingService
```

### Kiểm Tra Token

```java
ZoomMeetingManager zoomManager = ZoomMeetingManager.getInstance(context);
ZoomOAuthToken token = zoomManager.getCurrentToken();
if (token != null) {
    Log.d(TAG, "Token valid: " + !token.isExpired());
}
```

## Lỗi Phổ Biến

| Lỗi | Nguyên Nhân | Giải Pháp |
|-----|----------|---------|
| 401 Unauthorized | Token không hợp lệ | Kiểm tra Client ID/Secret |
| 429 Too Many Requests | Vượt giới hạn request | Thêm rate limiting |
| 400 Bad Request | Định dạng startTime sai | Sử dụng ISO 8601 format |
| Network Error | Không có kết nối | Kiểm tra internet |

## Tài Liệu Tham Khảo

- [Zoom Meeting API Docs](https://developers.zoom.us/docs/api/meetings/create-a-meeting/)
- [Zoom OAuth 2.0 Docs](https://developers.zoom.us/docs/internal-apps/s2s-oauth/)
- [ISO 8601 Format](https://en.wikipedia.org/wiki/ISO_8601)
