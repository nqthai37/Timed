package com.timed.adapters;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.timed.R;
import com.timed.models.CalendarDay;
import com.timed.utils.ThemeManager;

import java.time.LocalDate;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
    private final List<CalendarDay> days;
    private final OnItemListener onItemListener;

    public CalendarAdapter(List<CalendarDay> days, OnItemListener onItemListener) {
        this.days = days;
        this.onItemListener = onItemListener;
    }

    public void setSelectedDate(LocalDate selectedDate) {
        for (CalendarDay day : days) {
            day.isSelected = day.date != null && day.isCurrentMonth && day.date.equals(selectedDate);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        holder.tvDayNumber.setText(String.valueOf(day.date.getDayOfMonth()));
        holder.layoutEventIndicators.removeAllViews();

        if (day.isCurrentMonth) {
            if (day.isSelected) {
                int onPrimary = MaterialColors.getColor(holder.itemView,
                        com.google.android.material.R.attr.colorOnPrimary);
                holder.tvDayNumber.setTextColor(onPrimary);
                holder.tvDayNumber.setBackgroundResource(R.drawable.bg_circle_selected);
            } else if (day.isToday) {
                int primary = ThemeManager.getPrimaryColor(holder.itemView.getContext());
                holder.tvDayNumber.setTextColor(primary);
                holder.tvDayNumber.setBackgroundResource(R.drawable.bg_circle_today);
            } else {
                int onSurface = MaterialColors.getColor(holder.itemView,
                        com.google.android.material.R.attr.colorOnSurface);
                holder.tvDayNumber.setTextColor(onSurface);
                holder.tvDayNumber.setBackgroundResource(0);
            }

            int dotCount = Math.min(day.eventCount, 3);
            for (int i = 0; i < dotCount; i++) {
                View dot = new View(holder.itemView.getContext());
                int size = dpToPx(holder.itemView, 7);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(dpToPx(holder.itemView, 2), 0, dpToPx(holder.itemView, 2), 0);
                dot.setLayoutParams(params);
                dot.setBackground(makeDotDrawable(ThemeManager.getPrimaryColor(holder.itemView.getContext())));
                holder.layoutEventIndicators.addView(dot);
            }
        } else {
            int onSurfaceVariant = MaterialColors.getColor(holder.itemView,
                    com.google.android.material.R.attr.colorOnSurfaceVariant);
            holder.tvDayNumber.setTextColor(onSurfaceVariant);
            holder.tvDayNumber.setBackgroundResource(0);
        }

        holder.itemView.setOnClickListener(v -> {
            if (day.date != null && onItemListener != null) {
                onItemListener.onItemClick(position, day.date);
            }
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public interface OnItemListener {
        void onItemClick(int position, LocalDate date);
    }

    private static GradientDrawable makeDotDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private static int dpToPx(View view, int dp) {
        return Math.round(dp * view.getResources().getDisplayMetrics().density);
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        final ViewGroup layoutEventIndicators;
        final TextView tvDayNumber;

        CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            layoutEventIndicators = itemView.findViewById(R.id.layoutEventIndicators);
        }
    }
}
