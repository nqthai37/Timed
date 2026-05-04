package com.timed.adapters;

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

import java.time.LocalDate;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private final List<CalendarDay> days;
    private final OnItemListener onItemListener;

    public CalendarAdapter(List<CalendarDay> days, OnItemListener onItemListener) {
        this.days = days;
        this.onItemListener = onItemListener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view, onItemListener, days);
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
                int primary = MaterialColors.getColor(holder.itemView,
                        androidx.appcompat.R.attr.colorPrimary);
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
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(12, 12); // Kích thước chấm
                params.setMargins(4, 0, 4, 0);
                dot.setLayoutParams(params);
                dot.setBackgroundResource(R.drawable.bg_circle_today);

                holder.layoutEventIndicators.addView(dot);
            }

        } else {
            int onSurfaceVariant = MaterialColors.getColor(holder.itemView,
                    com.google.android.material.R.attr.colorOnSurfaceVariant);
            holder.tvDayNumber.setTextColor(onSurfaceVariant);
            holder.tvDayNumber.setBackgroundResource(0);
        }

        holder.itemView.setOnClickListener(v -> {
            if (day.date != null) {
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

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        public ViewGroup layoutEventIndicators;
        TextView tvDayNumber;
        OnItemListener onItemListener;
        List<CalendarDay> days;

        public CalendarViewHolder(@NonNull View itemView, OnItemListener onItemListener, List<CalendarDay> days) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            this.onItemListener = onItemListener;

            layoutEventIndicators = itemView.findViewById(R.id.layoutEventIndicators);

            this.days = days;
        }
    }
}
