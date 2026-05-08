package com.timed.utils;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public final class SystemBarInsets {

    private SystemBarInsets() {
    }

    public static void applyTo(Activity activity, View root) {
        if (activity == null || root == null) {
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

        int baseLeft = root.getPaddingLeft();
        int baseTop = root.getPaddingTop();
        int baseRight = root.getPaddingRight();
        int baseBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    baseLeft + bars.left,
                    baseTop + bars.top,
                    baseRight + bars.right,
                    baseBottom + bars.bottom
            );
            return insets;
        });
    }
}
