# Hướng dẫn sử dụng Events & Reminders

## Tổng quan
Hệ thống mới này cho phép bạn làm việc trực tiếp với Events từ Firebase Firestore thay vì sử dụng bảng Reminders riêng.

## Cấu trúc dữ liệu Events
```
events/
  └── event_id
      ├── title: "Team Meeting"
      ├── description: "asasdasdasd"
      ├── location: "asdasdasd"
      ├── start_time: Timestamp
      ├── end_time: Timestamp
      ├── all_day: boolean
      ├── timezone: "Asia/Ho_Chi_Minh"
      ├── created_by: "user_id"
      ├── created_at: Timestamp
      ├── updated_at: Timestamp
      ├── reminders: [
      │   {
      │     "minutes_before": 10,
      │     "type": "push"
      │   }
      │ ]
      ├── recurrence_rule: "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO"
      ├── recurrence_exceptions: ["2026-06-01"]
      ├── participant_id: ["user1", "user2"]
      ├── participant_status: {
      │   "user1": "accepted",
      │   "user2": "pending"
      │ }
      ├── visibility: "public"
      ├── calendar_id: "calendar_id"
      ├── attachments: [
      │   {
      │     "name": "epstein_file.pdf",
      │     "url": "https://..."
      │   }
      │ ]
      └── instance_of: "parent_event_id"
```

## Những thành phần chính
- **Event Model** (`com.timed.models.Event`) - Model đại diện cho Event
- **EventsRepository** (`com.timed.repositories.EventsRepository`) - Tương tác với Firebase
- **EventsNotificationManager** (`com.timed.managers.EventsNotificationManager`) - Quản lý thông báo
- **EventsManager** (`com.timed.managers.EventsManager`) - Manager chính để quản lý events
- **EventNotificationReceiver** (`com.timed.services.EventNotificationReceiver`) - Xử lý thông báo

## Cách sử dụng

### 1. Tạo Event mới
```java
EventsManager eventsManager = EventsManager.getInstance(context);

Event event = new Event();
event.setTitle("Team Meeting");
event.setDescription("Discuss project updates");
event.setLocation("Conference Room");
event.setStartTime(new Timestamp(startDate));
event.setEndTime(new Timestamp(endDate));
event.setCreatedBy(userId);
event.setCalendarId(calendarId);
event.setTimezone("Asia/Ho_Chi_Minh");

// Thêm reminders
List<Event.EventReminder> reminders = new ArrayList<>();
reminders.add(new Event.EventReminder(10L, "push")); // 10 phút trước
reminders.add(new Event.EventReminder(30L, "push")); // 30 phút trước
event.setReminders(reminders);

// Thêm participants
event.getParticipantId().add(userId);
event.getParticipantStatus().put(userId, "accepted");

// Lưu vào Firebase
eventsManager.createEvent(event)
    .addOnSuccessListener(docRef -> {
        Toast.makeText(context, "Event created!", Toast.LENGTH_SHORT).show();
    })
    .addOnFailureListener(e -> {
        Log.e("Events", "Error creating event: " + e.getMessage());
    });
```

### 2. Lấy Events theo Calendar
```java
EventsManager eventsManager = EventsManager.getInstance(context);

eventsManager.getEventsByCalendarId(calendarId)
    .addOnSuccessListener(events -> {
        // Cập nhật UI với danh sách events
        eventAdapter.setevents(events);
    })
    .addOnFailureListener(e -> {
        Log.e("Events", "Error fetching events: " + e.getMessage());
    });
```

### 3. Lấy Events theo khoảng thời gian
```java
EventsManager eventsManager = EventsManager.getInstance(context);

Timestamp startDate = new Timestamp(System.currentTimeMillis());
Timestamp endDate = new Timestamp(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000); // 7 ngày sau

eventsManager.getEventsByDateRange(calendarId, startDate, endDate)
    .addOnSuccessListener(events -> {
        // Cập nhật UI
    });
```

### 4. Lấy Events sắp tới cho User
```java
EventsManager eventsManager = EventsManager.getInstance(context);

eventsManager.getUpcomingEventsForUser(userId)
    .addOnSuccessListener(events -> {
        // Cập nhật UI với events sắp tới
    });
```

### 5. Cập nhật Event
```java
EventsManager eventsManager = EventsManager.getInstance(context);

event.setTitle("Updated Title");
event.setDescription("Updated Description");

eventsManager.updateEvent(eventId, event)
    .addOnSuccessListener(aVoid -> {
        Toast.makeText(context, "Event updated!", Toast.LENGTH_SHORT).show();
        // Reminders sẽ tự động được lên lịch lại
    })
    .addOnFailureListener(e -> {
        Log.e("Events", "Error updating event: " + e.getMessage());
    });
```

### 6. Xóa Event
```java
EventsManager eventsManager = EventsManager.getInstance(context);

eventsManager.deleteEvent(eventId)
    .addOnSuccessListener(aVoid -> {
        Toast.makeText(context, "Event deleted!", Toast.LENGTH_SHORT).show();
    })
    .addOnFailureListener(e -> {
        Log.e("Events", "Error deleting event: " + e.getMessage());
    });
```

### 7. Cập nhật trạng thái Participant
```java
EventsManager eventsManager = EventsManager.getInstance(context);

eventsManager.updateParticipantStatus(eventId, userId, "accepted")
    .addOnSuccessListener(aVoid -> {
        Log.d("Events", "Status updated to accepted");
    });

// Hoặc "declined", "pending", "tentative"
```

### 8. Thêm Participant mới
```java
EventsManager eventsManager = EventsManager.getInstance(context);

eventsManager.addParticipant(eventId, newUserId, "pending")
    .addOnSuccessListener(aVoid -> {
        Log.d("Events", "Participant added");
    });
```

### 9. Lên lịch lại tất cả Reminders (khi app khởi động)
```java
// Trong onCreate của MainActivity hoặc Application
EventsManager eventsManager = EventsManager.getInstance(context);
String userId = getUserIdFromAuth(); // Lấy từ Firebase Auth

eventsManager.rescheduleAllReminders(userId);
```

## Hệ thống thông báo

### Cách RemindersManager hoạt động
1. Event được tạo/cập nhật
2. EventsManager gọi EventsNotificationManager.scheduleEventReminders()
3. Cho mỗi reminder trong event, AlarmManager được thiết lập để trigger lúc `start_time - minutes_before`
4. Lúc đó, EventNotificationReceiver sẽ nhận được broadcast
5. Notification được hiển thị cho user

### Loại thông báo
- **Push Notification** - Hiển thị trên notification bar
  ```
  [Event Title]
  📍 Location
  Description
  ```
- **Action Buttons** - Nếu event có participants
  - ✓ Accept
  - ✗ Decline

### Khi user chọn Action
- **Accept** - Cập nhật participant_status thành "accepted"
- **Decline** - Cập nhật participant_status thành "declined"
- Trạng thái tự động được lưu vào Firebase

## Kiểm tra Reminders sắp trigger
```java
EventsManager eventsManager = EventsManager.getInstance(context);

eventsManager.getEventsThatNeedReminders(userId)
    .addOnSuccessListener(events -> {
        Log.d("Events", "Found " + events.size() + " events with reminders");
        for (Event event : events) {
            Log.d("Events", "Event: " + event.getTitle() + 
                    " starts at " + event.getStartTime());
        }
    });
```

## Permissions - Đã được thêm
```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## Best Practices

1. **Lên lịch lại khi app khởi động**
   ```java
   class MyApplication extends Application {
       @Override
       public void onCreate() {
           super.onCreate();
           
           String userId = getUserId();
           if (userId != null) {
               EventsManager.getInstance(this).rescheduleAllReminders(userId);
           }
       }
   }
   ```

2. **Xử lý lỗi gracefully**
   ```java
   eventsManager.getEventsByCalendarId(calendarId)
       .addOnSuccessListener(events -> { /* ... */ })
       .addOnFailureListener(e -> {
           Log.e("Events", "Error: " + e.getMessage());
           // Hiển thị error dialog cho user
       });
   ```

3. **Cập nhật UI asynchronously**
   ```java
   eventsManager.getUpcomingEventsForUser(userId)
       .addOnSuccessListener(events -> {
           // Chỉ update UI trên main thread
           runOnUiThread(() -> {
               eventAdapter.setEvents(events);
               eventAdapter.notifyDataSetChanged();
           });
       });
   ```

## Troubleshooting

### Reminders không trigger
1. Kiểm tra permissions đã granted (đặc biệt là SCHEDULE_EXACT_ALARM)
2. Kiểm tra device battery optimization setting
3. Gọi rescheduleAllReminders() lại

### Notifications không hiển thị
1. Kiểm tra POST_NOTIFICATIONS permission đã granted (Android 13+)
2. Kiểm tra notification channel settings
3. Kiểm tra mute settings trên device

### Events không lưu được
1. Kiểm tra Firebase Firestore rules
2. Kiểm tra network connectivity
3. Kiểm tra console logs cho errors

## Tài liệu tham khảo
- Firebase Firestore: https://firebase.google.com/docs/firestore
- AlarmManager: https://developer.android.com/reference/android/app/AlarmManager
- Notifications: https://developer.android.com/develop/ui/views/notifications
