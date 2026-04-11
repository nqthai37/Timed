package com.timed.Setting.Security;

public class SecurityOption {
    public static final int TYPE_SWITCH = 0;
    public static final int TYPE_ARROW = 1;

    private String title;
    private int iconResId;
    private int type;
    private boolean isChecked; // Trạng thái của Switch

    public SecurityOption(String title, int iconResId, int type, boolean isChecked) {
        this.title = title;
        this.iconResId = iconResId;
        this.type = type;
        this.isChecked = isChecked;
    }

    public String getTitle() { return title; }
    public int getIconResId() { return iconResId; }
    public int getType() { return type; }
    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }
}