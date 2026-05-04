package com.timed.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class Task {
    private String id; // ID của document trên Firestore (không lưu trực tiếp làm field)
    private String title;
    private String description;
    private boolean is_completed;
    private boolean is_all_day;
    private Timestamp due_date;
    private Timestamp created_at;
    private Timestamp updated_at;
    private String priority;
    private String created_by;
    private String list_id;
    private List<TaskReminder> reminders;

    // Bắt buộc phải có constructor rỗng cho Firebase
    public Task() {}

    public Task(String title, String desc, Timestamp deadline, String high, String userId) {}

    // Constructor dùng khi bạn tạo Task mới từ Code
    public Task(String title, String description, Timestamp due_date, boolean is_all_day,
                String priority, String created_by, String list_id, List<TaskReminder> reminders) {
        this.title = title;
        this.description = description;
        this.due_date = due_date;
        this.is_all_day = is_all_day;
        this.priority = priority;
        this.created_by = created_by;
        this.list_id = list_id;
        this.reminders = reminders;

        // Các giá trị mặc định khi tạo mới
        this.is_completed = false;
        this.created_at = Timestamp.now();
        this.updated_at = Timestamp.now();
    }

    // --- GETTERS & SETTERS ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isIs_completed() { return is_completed; }
    public void setIs_completed(boolean is_completed) { this.is_completed = is_completed; }

    public boolean isIs_all_day() { return is_all_day; }
    public void setIs_all_day(boolean is_all_day) { this.is_all_day = is_all_day; }

    public Timestamp getDue_date() { return due_date; }
    public void setDue_date(Timestamp due_date) { this.due_date = due_date; }

    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }

    public Timestamp getUpdated_at() { return updated_at; }
    public void setUpdated_at(Timestamp updated_at) { this.updated_at = updated_at; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getCreated_by() { return created_by; }
    public void setCreated_by(String created_by) { this.created_by = created_by; }

    public String getList_id() { return list_id; }
    public void setList_id(String list_id) { this.list_id = list_id; }

    public List<TaskReminder> getReminders() { return reminders; }
    public void setReminders(List<TaskReminder> reminders) { this.reminders = reminders; }

    // ==========================================================
    // CLASS CON: Dùng để map với Array 'reminders' trong Firestore
    // ==========================================================
    public static class TaskReminder {
        private String method;
        private int minutes_before;

        public TaskReminder() {}

        public TaskReminder(String method, int minutes_before) {
            this.method = method;
            this.minutes_before = minutes_before;
        }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public int getMinutes_before() { return minutes_before; }
        public void setMinutes_before(int minutes_before) { this.minutes_before = minutes_before; }
    }
}
