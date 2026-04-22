package com.timed.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.timed.R;

import java.util.ArrayList;
import java.util.List;

public class ReminderPickerDialog {

    public interface ReminderPickerCallback {
        void onRemindersSelected(List<Long> selectedMinutes);
    }

    public static void show(Context context, List<Long> currentSelection, ReminderPickerCallback callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_reminder_picker, null);

        RadioGroup rgPresetTimes = view.findViewById(R.id.rgPresetTimes);
        RadioButton rbCustom = view.findViewById(R.id.rbCustom);

        EditText etCustomHour = view.findViewById(R.id.etCustomHour);
        EditText etCustomMinute = view.findViewById(R.id.etCustomMinute);
//        EditText etCustomSecond = view.findViewById(R.id.etCustomSecond); // Thêm ô Giây

        // --- 1. Hiển thị lại giá trị đã chọn trước đó ---
        if (currentSelection != null && !currentSelection.isEmpty()) {
            long current = currentSelection.get(0);
            if (current == 5) rgPresetTimes.check(R.id.rb5Min);
            else if (current == 10) rgPresetTimes.check(R.id.rb10Min);
            else if (current == 15) rgPresetTimes.check(R.id.rb15Min);
            else if (current == 30) rgPresetTimes.check(R.id.rb30Min);
            else if (current == 60) rgPresetTimes.check(R.id.rb1Hour);
            else if (current == 120) rgPresetTimes.check(R.id.rb2Hours);
            else if (current == 1440) rgPresetTimes.check(R.id.rb1Day);
            else {
                // Khôi phục giá trị Custom
                rgPresetTimes.check(R.id.rbCustom);
                long h = current / 60;
                long m = current % 60;
                etCustomHour.setText(String.format("%02d", h));
                etCustomMinute.setText(String.format("%02d", m));
//                etCustomSecond.setText("00");
            }
        }

        // Khởi tạo Dialog (KHÔNG đặt chức năng tự động đóng cho nút Done)
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle("Set Reminders")
                .setView(view)
                .setPositiveButton("Done", null)
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        // --- 2. CÁCH FIX LỖI TỰ ĐÓNG: DÙNG ONCLICK LISTENER THAY VÌ ONCHECKEDCHANGE ---

        // Tạo một listener chung cho các nút Preset. Chỉ khi bấm vật lý vào nút thì mới đóng dialog.
        View.OnClickListener presetClickListener = v -> {
            processSelection(context, rgPresetTimes, etCustomHour, etCustomMinute, callback);
            dialog.dismiss();
        };

        // Gắn sự kiện click này vào từng nút Preset cụ thể
        view.findViewById(R.id.rb5Min).setOnClickListener(presetClickListener);
        view.findViewById(R.id.rb10Min).setOnClickListener(presetClickListener);
        view.findViewById(R.id.rb15Min).setOnClickListener(presetClickListener);
        view.findViewById(R.id.rb30Min).setOnClickListener(presetClickListener);
        view.findViewById(R.id.rb1Hour).setOnClickListener(presetClickListener);
        view.findViewById(R.id.rb2Hours).setOnClickListener(presetClickListener);
        view.findViewById(R.id.rb1Day).setOnClickListener(presetClickListener);

        // Khi người dùng chạm thẳng vào nút radio "Custom Time" -> Chuyển focus vào ô Giờ
        rbCustom.setOnClickListener(v -> etCustomHour.requestFocus());

        // Khi người dùng bấm thẳng vào 3 ô nhập Giờ, Phút, Giây -> Tự động nhảy tick Custom
        View.OnFocusChangeListener customFocusListener = (v, hasFocus) -> {
            if (hasFocus) rgPresetTimes.check(R.id.rbCustom);
        };
        View.OnClickListener customClickListener = v -> rgPresetTimes.check(R.id.rbCustom);

        etCustomHour.setOnFocusChangeListener(customFocusListener);
        etCustomHour.setOnClickListener(customClickListener);

        etCustomMinute.setOnFocusChangeListener(customFocusListener);
        etCustomMinute.setOnClickListener(customClickListener);

//        etCustomSecond.setOnFocusChangeListener(customFocusListener);
//        etCustomSecond.setOnClickListener(customClickListener);

        // Hiển thị dialog
        dialog.show();

        // --- 3. KIỂM TRA LỖI KHI BẤM "DONE" CHO PHẦN CUSTOM ---
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Nếu người dùng lỡ nhập số mà chưa check vào radio Custom, tự động check hộ luôn
            if (!etCustomHour.getText().toString().isEmpty() ||
                    !etCustomMinute.getText().toString().isEmpty() ) {
                rgPresetTimes.check(R.id.rbCustom);
            }

            // Gửi dữ liệu. Nếu dữ liệu đúng thì mới cho phép đóng hộp thoại
            boolean isValid = processSelection(context, rgPresetTimes, etCustomHour, etCustomMinute, callback);
            if (isValid) {
                dialog.dismiss();
            }
        });
    }

    /**
     * Tính toán tổng số phút. Trả về True nếu thành công, False nếu dữ liệu không hợp lệ.
     */
    private static boolean processSelection(Context context, RadioGroup rg, EditText etHour, EditText etMinute, ReminderPickerCallback callback) {
        int checkedId = rg.getCheckedRadioButtonId();
        long minutes = 0;

        if (checkedId == R.id.rb5Min) minutes = 5;
        else if (checkedId == R.id.rb10Min) minutes = 10;
        else if (checkedId == R.id.rb15Min) minutes = 15;
        else if (checkedId == R.id.rb30Min) minutes = 30;
        else if (checkedId == R.id.rb1Hour) minutes = 60;
        else if (checkedId == R.id.rb2Hours) minutes = 120;
        else if (checkedId == R.id.rb1Day) minutes = 1440;
        else if (checkedId == R.id.rbCustom) {
            String hourStr = etHour.getText().toString().trim();
            String minStr = etMinute.getText().toString().trim();
//            String secStr = etSecond.getText().toString().trim();

            long h = hourStr.isEmpty() ? 0 : Long.parseLong(hourStr);
            long m = minStr.isEmpty() ? 0 : Long.parseLong(minStr);
//            long s = secStr.isEmpty() ? 0 : Long.parseLong(secStr);

            minutes = (h * 60) + m;

            // Xử lý thông minh: Nếu người dùng chỉ gõ Giây (ví dụ 30s) mà chưa đủ 1 phút, làm tròn thành 1 phút
//            if (minutes == 0 && s > 0) {
//                minutes = 1;
//            }
        }

        if (minutes > 0) {
            List<Long> result = new ArrayList<>();
            result.add(minutes);
            callback.onRemindersSelected(result);
            return true; // Cho phép đóng Dialog
        } else {
            Toast.makeText(context, "Please enter a valid time (> 0)", Toast.LENGTH_SHORT).show();
            return false; // Chặn đóng Dialog, bắt người dùng sửa lại
        }
    }
}