package com.timed;

public class SettingItem {
    private final int iconId;
    private String name;
    private Class<?> targetActivity;
    private String headerTitle; // Biến mới để lưu tên nhóm

    // Constructor 1: Dành cho item mở đầu nhóm (CÓ truyền tiêu đề nhóm)
    public SettingItem(String headerTitle, int iconId, String name, Class<?> targetActivity) {
        this.headerTitle = headerTitle;
        this.iconId = iconId;
        this.name = name;
        this.targetActivity = targetActivity;
    }

    // Constructor 2: Dành cho item bình thường (KHÔNG có tiêu đề nhóm)
    public SettingItem(int iconId, String name, Class<?> targetActivity) {
        this.headerTitle = null; // Gán null vì không có
        this.iconId = iconId;
        this.name = name;
        this.targetActivity = targetActivity;
    }

    public int getIconId() { return iconId; }
    public String getName() { return name; }
    public Class<?> getTargetActivity() { return targetActivity; }

    // Getter mới
    public String getHeaderTitle() { return headerTitle; }
}