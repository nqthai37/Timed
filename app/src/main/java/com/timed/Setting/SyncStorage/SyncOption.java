package com.timed.Setting.SyncStorage;

public class SyncOption {
    public static final int TYPE_SWITCH = 0;
    public static final int TYPE_VALUE = 1;

    private String title;
    private int type;
    private boolean isChecked; // Dành cho nút gạt Switch
    private String value;      // Dành cho Text (VD: "Today, 10:42 AM")

    public SyncOption(String title, int type, boolean isChecked, String value) {
        this.title = title;
        this.type = type;
        this.isChecked = isChecked;
        this.value = value;
    }

    public String getTitle() { return title; }
    public int getType() { return type; }
    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }
    public String getValue() { return value; }
}