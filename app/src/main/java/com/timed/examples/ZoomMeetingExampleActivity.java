//package com.timed.examples;
//
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Toast;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.app.AlertDialog;
//
//import com.timed.R;
//import com.timed.managers.ZoomMeetingManager;
//import com.timed.models.ZoomMeetingResponse;
//import com.timed.utils.ZoomConfigManager;
//import android.view.View;
//
///**
// * Ví dụ về cách tích hợp Zoom Meeting API
// *
// * TÙICHỈNH:
// * - Thay R.layout.activity_zoom_example bằng layout của bạn
// * - Cập nhật IDs button/edittext theo layout của bạn
// */
//public class ZoomMeetingExampleActivity extends AppCompatActivity {
//    private static final String TAG = "ZoomMeetingExample";
//
//    private EditText etEventName;
//    private EditText etDuration;
//    private Button btnSetupZoom;
//    private Button btnCreateMeeting;
//    private Button btnGetDetails;
//    private EditText etMeetingId;
//
//    private ZoomMeetingManager zoomManager;
//    private ZoomConfigManager configManager;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        // setContentView(R.layout.activity_event_detail); // Thay bằng layout của bạn
//
//        // Khởi tạo Manager
//        zoomManager = ZoomMeetingManager.getInstance(this);
//        configManager = ZoomConfigManager.getInstance(this);
//
//        // Khởi tạo Views
//        initViews();
//        setupListeners();
//
//        // Kiểm tra cấu hình
//        checkZoomConfiguration();
//    }
//
//    private void initViews() {
//        // TODO: Thay đổi theo layout của bạn
//        /*
//        // etEventName = findViewById(R.id.etEventName);
//        // etDuration = findViewById(R.id.etDuration);
//        // btnSetupZoom = findViewById(R.id.btnSetupZoom);
//        // btnCreateMeeting = findViewById(R.id.btnCreateMeeting);
//        */
//    }
//*
//        // Setup Zoom Credentials
//        if (btnSetupZoom != null) {
//            btnSetupZoom.setOnClickListener(v -> showSetupZoomDialog());
//        }
//
//        // Create Meeting
//        if (btnCreateMeeting != null) {
//            btnCreateMeeting.setOnClickListener(v -> handleCreateMeeting());
//        }
//        */            btnCreateMeeting.setOnClickListener(v -> handleCreateMeeting());
//        }
//    }
//
//    /**
//     * Kiểm tra xem Zoom đã được cấu hình hay chưa
//     */
//    private void checkZoomConfiguration() {
//        if (configManager.isConfigured()) {
//            Log.d(TAG, "Zoom is configured");
//            Toast.makeText(this, "Zoom đã được cấu hình", Toast.LENGTH_SHORT).show();
//        } else {
//            Log.w(TAG, "Zoom is not configured");
//            Toast.makeText(this, "Zoom chưa được cấu hình. Vui lòng setup trước.", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    /**
//     * Hiển thị dialog để setup Zoom credentials
//     */
//    private void showSetupZoomDialog() {
//        View dialogView = getLayoutInflater().inflate(R.layout.dialog_zoom_setup, null);
//        EditText etClientId = dialogView.findViewById(R.id.etClientId);
//        EditText etClientSecret = dialogView.findViewById(R.id.etClientSecret);
//
//        // Load saved credentials (if any)
//        String savedClientId = configManager.getClientId();
//        String savedClientSecret = configManager.getClientSecret();
//
//        if (savedClientId != null) {
//            etClientId.setText(savedClientId);
//        }
//        if (savedClientSecret != null) {
//            etClientSecret.setText(savedClientSecret);
//        }
//
//        new AlertDialog.Builder(this)
//                .setTitle("Cấu hình Zoom API")
//                .setView(dialogView)
//                .setPositiveButton("Lưu", (dialog, which) -> {
//                    String clientId = etClientId.getText().toString().trim();
//                    String clientSecret = etClientSecret.getText().toString().trim();
//
//                    if (clientId.isEmpty() || clientSecret.isEmpty()) {
//                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    // Lưu credentials
//                    configManager.setCredentials(clientId, clientSecret, null);
//
//                    // Cập nhật Zoom Manager
//                    zoomManager.updateZoomCredentials(clientId, clientSecret);
//
//                    Toast.makeText(this, "Zoom đã được cấu hình thành công!", Toast.LENGTH_SHORT).show();
//                    Log.d(TAG, "Zoom credentials saved");
//                })
//                .setNegativeButton("Hủy", null)
//                .show();   */
//    }        Log.d(TAG, "showSetupZoomDialog: UI commented out."gativeButton("Hủy", null)
//         .show();
//    /**
//     * Xử lý tạo cuộc họp Zoom
//     */
//    private void handleCreateMeeting() {
//        // Kiểm tra cấu hình
//        if (!zoomManager.isZoomConfigured()) {
//            Toast.makeText(this, "Vui lòng cấu hình Zoom trước", Toast.LENGTH_SHORT).show();akeText(this, "Vui lòng cấu hình Zoom trước", Toast.LENGTH_SHORT).show();
//            return;   return;
//        }        }
//
//        // Lấy thông tin từ EditText
//        String eventName = etEventName.getText().toString().trim();
//        String durationStr = etDuration.getText().toString().trim();        String durationStr = etDuration.getText().toString().trim();
//
//        if (eventName.isEmpty() || durationStr.isEmpty()) {
//            Toast.makeText(this, "Vui lòng nhập tên sự kiện và thời lượng", Toast.LENGTH_SHORT).show();akeText(this, "Vui lòng nhập tên sự kiện và thời lượng", Toast.LENGTH_SHORT).show();
//            return;   return;
//        }        }
//
//        int duration = Integer.parseInt(durationStr);int duration = Integer.parseInt(durationStr);
//
//        // Tính toán thời gian
//        long startTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 giờ sau* 60 * 1000); // 1 giờ sau
//        long endTime = startTime + (duration * 60 * 1000);        long endTime = startTime + (duration * 60 * 1000);
//
//        // Hiển thị progress dialog
//        AlertDialog progressDialog = new AlertDialog.Builder(this)log.Builder(this)
//                .setTitle("Đang tạo cuộc họp...")p...")
//                .setMessage("Vui lòng chờ") chờ")
//                .setCancelable(false)elable(false)
//                .show();                .show();
//
//        // Gọi Zoom Manager
//        zoomManager.createMeetingForEvent(
//                "event_" + System.currentTimeMillis(), System.currentTimeMillis(),
//                eventName,
//                startTime,e,
//                endTime,
//                new ZoomMeetingManager.ZoomMeetingCallback() {ngManager.ZoomMeetingCallback() {
//                    @Override
//                    public void onSuccess(ZoomMeetingResponse response) {tingResponse response) {
//                        progressDialog.dismiss();
//                        handleMeetingSuccess(response);   handleMeetingSuccess(response);
//                    }                    }
//
//                    @Override
//                    public void onFailure(String errorMessage) {errorMessage) {
//                        progressDialog.dismiss();
//                        handleMeetingFailure(errorMessage);   handleMeetingFailure(errorMessage);
//                    }   }
//                }      }
//        );   );
//    }        */
// Log.d(TAG, "handleCreateMeeting: UI commented out."                handleMeetingFailure(errorMessage);
//    /**
//     * Xử lý khi tạo cuộc họp thành công         }
//     */
//    private void handleMeetingSuccess(ZoomMeetingResponse response) {
//        Log.d(TAG, "Meeting created: " + response.getJoinUrl());
//
//        // Hiển thị dialog với thông tin cuộc họpông
//        new AlertDialog.Builder(this)
//                .setTitle("✓ Cuộc họp được tạo thành công!"){
//                .setMessage("Topic: " + response.getTopic() + "\n" +
//                           "Join URL: " + response.getJoinUrl() + "\n" +
//                           "Meeting ID: " + response.getId())
//                .setPositiveButton("Tham gia Ngay", (dialog, which) -> {
//                    openZoomMeeting(response.getJoinUrl());etTitle("✓ Cuộc họp được tạo thành công!")
//                }).getTopic() + "\n" +
//                .setNegativeButton("Đóng", null)+
//                .setNeutralButton("Sao Chép Link", (dialog, which) -> {))
//                    copyToClipboard(response.getJoinUrl());etPositiveButton("Tham gia Ngay", (dialog, which) -> {
//                })ZoomMeeting(response.getJoinUrl());
//                .show();                })
//
//        Toast.makeText(this, "Cuộc họp được tạo thành công!", Toast.LENGTH_SHORT).show();           .setNeutralButton("Sao Chép Link", (dialog, which) -> {
//    }                    copyToClipboard(response.getJoinUrl());
//         })
//    /**
//     * Xử lý khi tạo cuộc họp thất bại
//     */, Toast.LENGTH_SHORT).show();
//    private void handleMeetingFailure(String errorMessage) {
//        Log.e(TAG, "Failed to create meeting: " + errorMessage);
//
//        new AlertDialog.Builder(this)
//                .setTitle("✗ Lỗi tạo cuộc họp")
//                .setMessage("Chi tiết: " + errorMessage)rrorMessage) {
//                .setPositiveButton("OK", null)ed to create meeting: " + errorMessage);
//                .show();
//
//        Toast.makeText(this, "Lỗi: " + errorMessage, Toast.LENGTH_LONG).show();           .setTitle("✗ Lỗi tạo cuộc họp")
//    }                .setMessage("Chi tiết: " + errorMessage)
//         .setPositiveButton("OK", null)
//    /**
//     * Mở URL Zoom Meeting
//     */e, Toast.LENGTH_LONG).show();
//    private void openZoomMeeting(String joinUrl) {
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setData(Uri.parse(joinUrl));
//        startActivity(intent);* Mở URL Zoom Meeting
//    }     */
//vate void openZoomMeeting(String joinUrl) {
//    /**(Intent.ACTION_VIEW);
//     * Sao chép link vào clipboard intent.setData(Uri.parse(joinUrl));
//     */
//    private void copyToClipboard(String text) {
//        android.content.ClipboardManager clipboard =
//            (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
//        android.content.ClipData clip = android.content.ClipData.newPlainText("Join URL", text);
//        clipboard.setPrimaryClip(clip);
//        Toast.makeText(this, "Link đã sao chép!", Toast.LENGTH_SHORT).show();rivate void copyToClipboard(String text) {
//    }       android.content.ClipboardManager clipboard =
//}            (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
//     android.content.ClipData clip = android.content.ClipData.newPlainText("Join URL", text);
///**
// * Layout XML tương ứng (activity_zoom_example.xml)     Toast.makeText(this, "Link đã sao chép!", Toast.LENGTH_SHORT).show();
// *
// * <?xml version="1.0" encoding="utf-8"?>
// * <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
// *     android:layout_width="match_parent"
// *     android:layout_height="match_parent"m_example.xml)
// *     android:orientation="vertical"
// *     android:padding="16dp"> <?xml version="1.0" encoding="utf-8"?>
// * xmlns:android="http://schemas.android.com/apk/res/android"
// *     <TextView
// *         android:layout_width="wrap_content"
// *         android:layout_height="wrap_content"
// *         android:text="Tạo Cuộc Họp Zoom"
// *         android:textSize="24sp"
// *         android:textStyle="bold"
// *         android:layout_marginBottom="16dp" />         android:layout_width="wrap_content"
// *roid:layout_height="wrap_content"
// *     <Buttonm"
// *         android:id="@+id/btnSetupZoom"
// *         android:layout_width="match_parent"
// *         android:layout_height="wrap_content"/>
// *         android:text="Cấu hình Zoom API" />
// *
// *     <EditText"
// *         android:id="@+id/etEventName"
// *         android:layout_width="match_parent"
// *         android:layout_height="wrap_content"m API" />
// *         android:hint="Tên sự kiện"
// *         android:layout_marginTop="16dp" />     <EditText
// *id:id="@+id/etEventName"
// *     <EditTextparent"
// *         android:id="@+id/etDuration""
// *         android:layout_width="match_parent"
// *         android:layout_height="wrap_content"/>
// *         android:hint="Thời lượng (phút)"
// *         android:inputType="number"
// *         android:layout_marginTop="8dp" />         android:id="@+id/etDuration"
// *roid:layout_width="match_parent"
// *     <Buttont"
// *         android:id="@+id/btnCreateMeeting"
// *         android:layout_width="match_parent"
// *         android:layout_height="wrap_content"dp" />
// *         android:text="Tạo Cuộc Họp"
// *         android:layout_marginTop="16dp" />
// * </LinearLayout>         android:id="@+id/btnCreateMeeting"
// *h_parent"
// * Dialog XML (dialog_zoom_setup.xml)        android:layout_height="wrap_content"
// *
// * <?xml version="1.0" encoding="utf-8"?>
// * <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
// *     android:layout_width="match_parent"
// *     android:layout_height="wrap_content"
// *     android:orientation="vertical"
// *     android:padding="16dp"> <?xml version="1.0" encoding="utf-8"?>
// * xmlns:android="http://schemas.android.com/apk/res/android"
// *     <EditTextnt"
// *         android:id="@+id/etClientId"
// *         android:layout_width="match_parent"
// *         android:layout_height="wrap_content"
// *         android:hint="Client ID"
// *         android:layout_marginBottom="8dp" />     <EditText
// *id:id="@+id/etClientId"
// *     <EditTextnt"
// *         android:id="@+id/etClientSecret""
// *         android:layout_width="match_parent"
// *         android:layout_height="wrap_content""8dp" />
// *         android:hint="Client Secret"
// *         android:inputType="text"
// *         android:layout_marginBottom="8dp" />:id="@+id/etClientSecret"
// * </LinearLayout>        android:layout_width="match_parent"
// */ *         android:layout_height="wrap_content"
//
// *         android:hint="Client Secret"
// *         android:inputType="text"
// *         android:layout_marginBottom="8dp" />
// * </LinearLayout>
// */
