package com.timed.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class TimelineRenderer {
    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static void addEventCardToTimeline(Context context, RelativeLayout container, int hourHeightPx, String title,
                                        String details, int startHour, int startMinute, int durationMinutes, int backgroundResId,
                                        String titleColorHex, String detailsColorHex, Integer tintColor) {
        addEventCardToTimeline(context, container, hourHeightPx, title, details, startHour, startMinute,
                durationMinutes, backgroundResId, titleColorHex, detailsColorHex, tintColor, null);
    }

    public static void addEventCardToTimeline(Context context, RelativeLayout container, int hourHeightPx, String title,
                                        String details, int startHour, int startMinute, int durationMinutes, int backgroundResId,
                                        String titleColorHex, String detailsColorHex, Integer tintColor,
                                        View.OnClickListener clickListener) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(backgroundResId);
        if (tintColor != null && card.getBackground() != null) {
            card.getBackground().mutate().setTint(tintColor);
        }
        card.setPadding(dpToPx(context,12), dpToPx(context,12), dpToPx(context,12), dpToPx(context,12));
        card.setElevation(dpToPx(context,4));
        applyClickBehavior(card, clickListener);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextColor(android.graphics.Color.parseColor(titleColorHex));
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvTitle);

        TextView tvDetails = new TextView(context);
        tvDetails.setText(details);
        tvDetails.setTextColor(android.graphics.Color.parseColor(detailsColorHex));
        tvDetails.setTextSize(12f);
        tvDetails.setPadding(0, dpToPx(context,4), 0, 0);
        card.addView(tvDetails);

        int topMargin = (startHour * hourHeightPx) + (startMinute * hourHeightPx / 60);
        int cardHeight = (durationMinutes * hourHeightPx / 60);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, cardHeight);
        params.topMargin = topMargin;
        params.leftMargin = dpToPx(context,60);
        params.rightMargin = dpToPx(context,16);

        container.addView(card, params);
    }

    public static void addEventTo3Days(Context context, RelativeLayout container, int hourHeightPx, int timeOffset,
                                 int colWidth, int dayIndex, String title, String details, int startHour, int startMinute, int durationMins,
                                 int bgRes, String titleHex, String detailHex, Integer tintColor) {
        addEventTo3Days(context, container, hourHeightPx, timeOffset, colWidth, dayIndex, title, details,
                startHour, startMinute, durationMins, bgRes, titleHex, detailHex, tintColor, null);
    }

    public static void addEventTo3Days(Context context, RelativeLayout container, int hourHeightPx, int timeOffset,
                                 int colWidth, int dayIndex, String title, String details, int startHour, int startMinute, int durationMins,
                                 int bgRes, String titleHex, String detailHex, Integer tintColor,
                                 View.OnClickListener clickListener) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(bgRes);
        if (tintColor != null && card.getBackground() != null) {
            card.getBackground().mutate().setTint(tintColor);
        }
        card.setPadding(dpToPx(context,8), dpToPx(context,8), dpToPx(context,8), dpToPx(context,8));
        card.setElevation(dpToPx(context,2));
        applyClickBehavior(card, clickListener);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor(titleHex));
        tvTitle.setTextSize(11f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        card.addView(tvTitle);

        if (!details.isEmpty()) {
            TextView tvDetails = new TextView(context);
            tvDetails.setText(details);
            tvDetails.setTextColor(Color.parseColor(detailHex));
            tvDetails.setTextSize(9f);
            card.addView(tvDetails);
        }

        int topMargin = (startHour * hourHeightPx) + (startMinute * hourHeightPx / 60) + dpToPx(context,16);
        int cardHeight = (durationMins * hourHeightPx / 60);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                colWidth - dpToPx(context,4), cardHeight);
        params.topMargin = topMargin;
        params.leftMargin = timeOffset + (dayIndex * colWidth) + dpToPx(context,2);

        container.addView(card, params);
    }

    public static void addEventToWeekGrid(Context context, RelativeLayout container, int hourHeightPx, int timeOffset,
                                    int colWidth, int dayIndex, String shortTitle, int startHour, int startMinute, int durationMins,
                                    int bgRes, Integer tintColor) {
        addEventToWeekGrid(context, container, hourHeightPx, timeOffset, colWidth, dayIndex, shortTitle,
                startHour, startMinute, durationMins, bgRes, tintColor, null);
    }

    public static void addEventToWeekGrid(Context context, RelativeLayout container, int hourHeightPx, int timeOffset,
                                    int colWidth, int dayIndex, String shortTitle, int startHour, int startMinute, int durationMins,
                                    int bgRes, Integer tintColor, View.OnClickListener clickListener) {
        android.widget.TextView card = new android.widget.TextView(context);
        card.setBackgroundResource(bgRes);
        if (tintColor != null && card.getBackground() != null) {
            card.getBackground().mutate().setTint(tintColor);
        }
        card.setText(shortTitle);
        if (tintColor != null && !isDarkColor(tintColor)) {
            card.setTextColor(Color.parseColor("#334155"));
        } else {
            card.setTextColor(Color.WHITE);
        }
        card.setTextSize(9f);
        card.setTypeface(null, Typeface.BOLD);
        card.setPadding(dpToPx(context, 4), dpToPx(context,4), dpToPx(context,2), dpToPx(context,2));
        card.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.setMaxLines(2);
        applyClickBehavior(card, clickListener);

        int topMargin = (startHour * hourHeightPx) + (startMinute * hourHeightPx / 60) + dpToPx(context,10);
        int cardHeight = (durationMins * hourHeightPx / 60);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                colWidth - dpToPx(context,2), cardHeight);
        params.topMargin = topMargin;
        params.leftMargin = timeOffset + (dayIndex * colWidth) + dpToPx(context,1);

        container.addView(card, params);
    }

    private static void applyClickBehavior(View view, View.OnClickListener clickListener) {
        if (clickListener == null) {
            return;
        }
        view.setClickable(true);
        view.setFocusable(true);
        TypedValue outValue = new TypedValue();
        view.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        view.setForeground(androidx.appcompat.content.res.AppCompatResources.getDrawable(
                view.getContext(), outValue.resourceId));
        view.setOnClickListener(clickListener);
    }

    public static boolean isDarkColor(int color) {
        double darkness = 1 - (0.299 * android.graphics.Color.red(color)
                + 0.587 * android.graphics.Color.green(color)
                + 0.114 * android.graphics.Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

}
