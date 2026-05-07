package com.timed.utils;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;

import java.util.Calendar;

/**
 * Helper class chuyên xử lý hiển thị DatePickerDialog và TimePickerDialog.
 * Giảm tải logic lặp lại từ Activity.
 */
public class DateTimePickerHelper {

    /**
     * Callback khi người dùng chọn xong ngày/giờ
     */
    public interface OnDateTimeSelectedListener {
        void onSelected(long newTimeMillis);
    }

    /**
     * Hiển thị DatePickerDialog để chọn ngày.
     *
     * @param context       Context hiện tại (Activity)
     * @param currentMillis Thời gian hiện tại (dùng để hiển thị mặc định trên dialog)
     * @param listener      Callback trả về thời gian mới sau khi người dùng chọn
     */
    public static void showDatePicker(Context context, long currentMillis, OnDateTimeSelectedListener listener) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentMillis);

        DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                (view, year, monthOfYear, dayOfMonth) -> {
                    calendar.set(year, monthOfYear, dayOfMonth);
                    if (listener != null) {
                        listener.onSelected(calendar.getTimeInMillis());
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    /**
     * Hiển thị TimePickerDialog để chọn giờ (24h format).
     *
     * @param context       Context hiện tại (Activity)
     * @param currentMillis Thời gian hiện tại (dùng để hiển thị mặc định trên dialog)
     * @param listener      Callback trả về thời gian mới sau khi người dùng chọn
     */
    public static void showTimePicker(Context context, long currentMillis, OnDateTimeSelectedListener listener) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentMillis);

        TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    if (listener != null) {
                        listener.onSelected(calendar.getTimeInMillis());
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);

        timePickerDialog.show();
    }

    /**
     * Đảm bảo khoảng thời gian hợp lệ: end luôn sau start.
     *
     * @param startTime       Thời gian bắt đầu (millis)
     * @param endTime         Thời gian kết thúc (millis)
     * @param autoFixDuration Khoảng thời gian tự động bù nếu end < start (millis)
     * @return mảng long[2] chứa { startTime, endTime } đã được điều chỉnh
     */
    public static long[] ensureValidTimeRange(long startTime, long endTime, long autoFixDuration) {
        if (startTime <= 0) {
            startTime = System.currentTimeMillis();
        }
        if (endTime < startTime) {
            endTime = startTime + autoFixDuration;
        }
        return new long[]{startTime, endTime};
    }
}
