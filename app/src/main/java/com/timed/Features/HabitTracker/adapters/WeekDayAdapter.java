package com.timed.Features.HabitTracker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying week day selectors with mini indicators.
 * Shows 7 days with completion status dots for a specific habit.
 */
public class WeekDayAdapter extends RecyclerView.Adapter<WeekDayAdapter.DayViewHolder> {
    
    private final Context context;
    private final long[] weekDates; // Array of 7 dates (Monday to Sunday)
    private final List<Integer> completionStatuses; // 0 = not completed, 1 = completed, -1 = grace day used
    private int selectedPosition = 6; // Default to today (last item)
    private OnDayClickListener listener;
    
    public interface OnDayClickListener {
        void onDayClick(int position, long date);
    }
    
    public WeekDayAdapter(Context context, long[] weekDates, List<Integer> completionStatuses) {
        this.context = context;
        this.weekDates = weekDates;
        this.completionStatuses = completionStatuses;
    }
    
    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_week_day, parent, false);
        return new DayViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        long date = weekDates[position];
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        
        // Set day name (Mon, Tue, etc.)
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        holder.tvDayName.setText(dayFormat.format(new Date(date)));
        
        // Set date number
        holder.tvDateNumber.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
        
        // Set completion status indicator
        int status = position < completionStatuses.size() ? completionStatuses.get(position) : 0;
        updateDayIndicator(holder, status, position);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onDayClick(position, date);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return weekDates.length;
    }
    
    /**
     * Updates the visual indicator based on completion status.
     * 0 = grey (not completed)
     * 1 = green (completed)
     * -1 = partial (grace day used)
     */
    private void updateDayIndicator(@NonNull DayViewHolder holder, int status, int position) {
        if (status == 1) {
            // Completed
            holder.indicatorView.setBackgroundResource(R.drawable.bg_circle_selected);
        } else if (status == -1) {
            // Grace day used
            holder.indicatorView.setBackgroundColor(context.getResources().getColor(R.color.slate_400));
        } else {
            // Not completed
            holder.indicatorView.setBackgroundColor(context.getResources().getColor(R.color.slate_400, null));
        }
        
        // Highlight selected day
        if (position == selectedPosition) {
            holder.itemView.setAlpha(1.0f);
            holder.tvDayName.setTextColor(context.getResources().getColor(R.color.slate_900));
        } else {
            holder.itemView.setAlpha(0.6f);
            holder.tvDayName.setTextColor(context.getResources().getColor(R.color.slate_500));
        }
    }
    
    /**
     * Sets the completion status for a specific day.
     */
    public void setCompletionStatus(int position, int status) {
        if (position >= 0 && position < completionStatuses.size()) {
            completionStatuses.set(position, status);
            notifyItemChanged(position);
        }
    }
    
    /**
     * Sets the click listener for day selection.
     */
    public void setOnDayClickListener(OnDayClickListener listener) {
        this.listener = listener;
    }
    
    /**
     * Gets currently selected position.
     */
    public int getSelectedPosition() {
        return selectedPosition;
    }
    
    /**
     * Sets currently selected position.
     */
    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }
    
    public static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayName;
        TextView tvDateNumber;
        View indicatorView;
        
        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tv_day_name);
            tvDateNumber = itemView.findViewById(R.id.tv_date_number);
            indicatorView = itemView.findViewById(R.id.v_indicator);
        }
    }
}
