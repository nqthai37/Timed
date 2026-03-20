package com.timed;

public class SettingItem {
    private final String title;
    private final String subtitle;
    private final int iconRes;
    private final boolean isNew;

    public SettingItem(String title, String subtitle, int iconRes, boolean isNew) {
        this.title = title;
        this.subtitle = subtitle;
        this.iconRes = iconRes;
        this.isNew = isNew;
    }

    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public int getIconRes() { return iconRes; }
    public boolean isNew() { return isNew; }
}
