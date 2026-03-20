package com.timed;

public class SettingItem {
    private String name;
    private Class<?> targetActivity;

    public SettingItem(String name, Class<?> targetActivity) {
        this.name = name;
        this.targetActivity = targetActivity;
    }

    public String getName() {
        return name;
    }

    public Class<?> getTargetActivity() {
        return targetActivity;
    }
}