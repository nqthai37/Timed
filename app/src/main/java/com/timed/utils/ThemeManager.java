package com.timed.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.timed.R;

public final class ThemeManager {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_APPEARANCE = "appearance";
    private static final String KEY_PALETTE = "palette";

    public static final String APPEARANCE_LIGHT = "light";
    public static final String APPEARANCE_DARK = "dark";
    public static final String APPEARANCE_SYSTEM = "system";

    public static final String PALETTE_BLUE = "blue";
    public static final String PALETTE_EMERALD = "emerald";
    public static final String PALETTE_SUNSET = "sunset";

    private ThemeManager() {
    }

    public static void applyNightMode(Context context) {
        String appearance = getAppearance(context);
        if (APPEARANCE_DARK.equals(appearance)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (APPEARANCE_LIGHT.equals(appearance)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public static void applyTheme(Activity activity) {
        applyNightMode(activity);
        activity.setTheme(getThemeResId(activity));
    }

    public static void setAppearance(Context context, String appearance) {
        getPrefs(context).edit().putString(KEY_APPEARANCE, appearance).apply();
    }

    public static String getAppearance(Context context) {
        return getPrefs(context).getString(KEY_APPEARANCE, APPEARANCE_SYSTEM);
    }

    public static void setPalette(Context context, String palette) {
        getPrefs(context).edit().putString(KEY_PALETTE, palette).apply();
    }

    public static String getPalette(Context context) {
        return getPrefs(context).getString(KEY_PALETTE, PALETTE_BLUE);
    }

    public static int getThemeResId(Context context) {
        String palette = getPalette(context);
        if (PALETTE_EMERALD.equals(palette)) {
            return R.style.Theme_Timed_Emerald;
        }
        if (PALETTE_SUNSET.equals(palette)) {
            return R.style.Theme_Timed_Sunset;
        }
        return R.style.Theme_Timed_Blue;
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
