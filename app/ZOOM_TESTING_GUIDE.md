# Testing Guide - Zoom Meeting API Integration

## Unit Tests

```java
package com.timed.tests;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.timed.managers.ZoomMeetingManager;
import com.timed.models.ZoomMeetingResponse;
import com.timed.services.ZoomMeetingService;
import com.timed.utils.ZoomConfigManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ZoomMeetingServiceTest {
    private Context context;
    private ZoomMeetingService zoomService;
    private ZoomConfigManager configManager;

    private static final String TEST_CLIENT_ID = "YOUR_TEST_CLIENT_ID";
    private static final String TEST_CLIENT_SECRET = "YOUR_TEST_CLIENT_SECRET";

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        configManager = ZoomConfigManager.getInstance(context);
        
        // Setup test credentials
        configManager.setCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET, null);
        
        zoomService = new ZoomMeetingService(context, TEST_CLIENT_ID, TEST_CLIENT_SECRET);
    }

    @Test
    public void testTokenGeneration() throws Exception {
        assertNotNull("Token should not be null", zoomService.getCurrentToken());
    }

    @Test
    public void testCreateMeeting() {
        String topic = "Test Meeting";
        String startTime = "2024-05-20T14:00:00Z";
        int duration = 60;

        zoomService.createZoomMeeting(topic, startTime, duration, 
            new ZoomMeetingService.ZoomMeetingCallback() {
                @Override
                public void onSuccess(ZoomMeetingResponse response) {
                    assertNotNull("Meeting ID should not be null", response.getId());
                    assertNotNull("Join URL should not be null", response.getJoinUrl());
                    assertTrue("Join URL should be valid", 
                        response.getJoinUrl().startsWith("https://zoom.us/j/"));
                }

                @Override
                public void onFailure(String errorMessage) {
                    fail("Meeting creation failed: " + errorMessage);
                }
            }
        );
    }

    @Test
    public void testZoomConfigManager() {
        configManager.setClientId("test_id");
        configManager.setClientSecret("test_secret");
        
        assertEquals("test_id", configManager.getClientId());
        assertEquals("test_secret", configManager.getClientSecret());
        assertTrue(configManager.isConfigured());
    }

    @Test
    public void testZoomConfigManagerClear() {
        configManager.setCredentials("id", "secret", null);
        configManager.clearCredentials();
        
        assertNull(configManager.getClientId());
        assertFalse(configManager.isConfigured());
    }
}
```

## Manual Testing Checklist

### Prerequisites
- [ ] Zoom OAuth App tạo thành công
- [ ] Client ID và Client Secret có sẵn
- [ ] Thiết bị có kết nối Internet
- [ ] Android API level ≥ 29

### Test Cases

#### 1. Configuration
- [ ] Set credentials thông qua dialog
- [ ] Verify credentials được lưu vào SharedPreferences
- [ ] Clear credentials
- [ ] Verify credentials được xóa

#### 2. Token Management
- [ ] Token được lấy lần đầu tiên khi tạo service
- [ ] Token được cache trong SharedPreferences
- [ ] Token tự động refresh khi hết hạn
- [ ] Token valid flag hoạt động đúng

#### 3. Create Meeting
- [ ] Tạo cuộc họp với parameters hợp lệ
- [ ] Join URL được trả về
- [ ] Meeting ID được trả về
- [ ] Cuộc họp xuất hiện trong Zoom calendar

#### 4. Error Handling
- [ ] Invalid credentials → Error message
- [ ] Network unavailable → Error message
- [ ] Invalid startTime format → Error message
- [ ] Missing required fields → Error message

#### 5. UI Integration
- [ ] Setup dialog hiển thị đúng
- [ ] Create meeting button hoạt động
- [ ] Success dialog hiển thị join URL
- [ ] Error dialog hiển thị lỗi
- [ ] Copy link button hoạt động
- [ ] Join link button mở Zoom meeting

#### 6. Network & Offline
- [ ] With WiFi: Meeting được tạo thành công
- [ ] With Mobile Data: Meeting được tạo thành công
- [ ] Offline: Error message hiển thị
- [ ] Network recovered: Retry hoạt động

#### 7. Performance
- [ ] First token fetch: < 2 seconds
- [ ] Cached token: < 100ms
- [ ] Create meeting: < 3 seconds
- [ ] No ANR (Application Not Responding)

## Test Scenarios

### Scenario 1: Normal Flow
```
1. Launch app
2. Setup Zoom credentials
3. Enter event name and duration
4. Click "Create Meeting"
5. Wait for success dialog
6. Verify join URL is valid
```

### Scenario 2: Token Expiration
```
1. Create first meeting (generates token)
2. Wait for token to expire (59+ minutes)
3. Create another meeting
4. Verify new token is generated automatically
```

### Scenario 3: Offline to Online
```
1. Disable internet
2. Try to create meeting → Error
3. Enable internet
4. Retry → Success
```

### Scenario 4: Configuration Update
```
1. Set credentials A
2. Create meeting (should work)
3. Update to credentials B
4. Create meeting with B (should work with new credentials)
```

## Debugging Commands

### View Zoom Logs
```bash
adb logcat | grep ZoomMeetingService
adb logcat | grep ZoomConfigManager
```

### Inspect SharedPreferences
```bash
adb shell
run-as com.mobile.timed
cat /data/data/com.mobile.timed/shared_prefs/zoom_config.xml
cat /data/data/com.mobile.timed/shared_prefs/zoom_meeting_prefs.xml
exit
```

### Monitor Network Requests
```bash
# Using Android Studio Profiler
# Or
adb shell tcpdump -i any -s 0 -w - | Wireshark
```

## Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| 401 Unauthorized | Wrong Client ID/Secret | Verify credentials in Zoom dashboard |
| 429 Too Many Requests | Rate limiting | Implement exponential backoff |
| Network timeout | Poor connection | Add timeout handling |
| Token not refreshing | Cache not cleared | Clear cache manually |
| UI frozen | Network call on main thread | Should use separate thread (service does this) |

## Sample Test Data

### Valid Meeting Request
```json
{
  "topic": "Test Meeting",
  "type": 2,
  "start_time": "2024-05-20T14:00:00Z",
  "duration": 60,
  "timezone": "UTC",
  "settings": {
    "host_video": true,
    "participant_video": true,
    "join_before_host": false,
    "mute_upon_entry": false
  }
}
```

### Sample Response
```json
{
  "id": 1234567890,
  "uuid": "ABCD1234EFGH5678IJKL",
  "topic": "Test Meeting",
  "type": 2,
  "start_time": "2024-05-20T14:00:00Z",
  "duration": 60,
  "created_at": "2024-05-15T10:00:00Z",
  "join_url": "https://zoom.us/j/1234567890?pwd=AB12CD34EF56GH78IJ9K",
  "password": "AB12CD34EF"
}
```

## Performance Benchmarks

Expected performance on typical Android device:

| Operation | Time | Target |
|-----------|------|--------|
| First token generation | 1-2s | < 3s ✓ |
| Token from cache | 10-50ms | < 100ms ✓ |
| Create meeting | 1-3s | < 5s ✓ |
| Database save | 50-100ms | < 200ms ✓ |

## Regression Testing Checklist

After each update:
- [ ] Token generation still works
- [ ] Meeting creation still works
- [ ] Error handling works
- [ ] Offline handling works
- [ ] UI updates correctly
- [ ] No new crashes
- [ ] No memory leaks
- [ ] Performance unchanged
