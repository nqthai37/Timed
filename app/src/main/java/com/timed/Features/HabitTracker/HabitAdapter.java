package com.timed.Features.HabitTracker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.Features.HabitTracker.models.Habit;
import com.timed.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Adapter for displaying list of habits for today.
 * Shows habit details with completion checkbox and streak information.
 */
public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    private final Context context;
    private final List<Habit> habitList = new ArrayList<>();
    private OnHabitClickListener listener;
    private Set<Integer> completedIds = new HashSet<>();
    
    public interface OnHabitClickListener {
        void onCheckClick(int habitId, int position);
        void onHabitLongClick(int habitId, Habit habit);
    }

    public HabitAdapter(Context context, List<Habit> habitList) {
        this.context = context;
        if (habitList != null) {
            this.habitList.addAll(habitList);
        }
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habitList.get(position);
        
        // Set basic info
        holder.tvTitle.setText(habit.getTitle());
        holder.tvDescription.setText(habit.getDescription());
        holder.ivIcon.setImageResource(habit.getIconDrawableId());
        
        // Set icon background color
        holder.ivIcon.setBackgroundColor(android.graphics.Color.parseColor(habit.getColor()));

        // Set streak display
        if (habit.getCurrentStreak() > 0) {
            holder.tvStreak.setText("🔥 " + habit.getCurrentStreak() + " days");
            holder.tvStreak.setVisibility(View.VISIBLE);
        } else {
            holder.tvStreak.setVisibility(View.GONE);
        }

        // Update check button status
        boolean isCompleted = completedIds.contains(habit.getId()) || isHabitCompletedToday(habit.getId());
        updateCheckButton(holder, isCompleted);

        // Set click listeners
        holder.ivCheckButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCheckClick(habit.getId(), position);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onHabitLongClick(habit.getId(), habit);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    /**
     * Updates the visual state of the check button.
     */
    private void updateCheckButton(@NonNull HabitViewHolder holder, boolean isCompleted) {
        if (isCompleted) {
            holder.ivCheckButton.setImageResource(R.drawable.ic_check_blue);
            holder.ivCheckButton.setAlpha(1.0f);
        } else {
            holder.ivCheckButton.setImageResource(R.drawable.ic_check_gray);
            holder.ivCheckButton.setAlpha(0.5f);
        }
    }

    /**
     * Placeholder for checking if habit was completed today.
     * This should be replaced with actual repository check.
     */
    private boolean isHabitCompletedToday(int habitId) {
        // TODO: Implement actual completion check from repository
        return false;
    }

    /**
     * Sets the set of habit ids that are completed for the selected date.
     */
    public void setCompletedIds(Set<Integer> ids) {
        if (ids == null) {
            this.completedIds.clear();
        } else {
            this.completedIds = ids;
        }
        notifyDataSetChanged();
    }

    /**
     * Sets the click listener.
     */
    public void setOnHabitClickListener(OnHabitClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates habit in list and refreshes view.
     */
    public void updateHabit(Habit habit, int position) {
        if (position >= 0 && position < habitList.size()) {
            habitList.set(position, habit);
            notifyItemChanged(position);
        }
    }

    /**
     * Replaces the current list data with a fresh copy from LiveData.
     */
    public void updateList(List<Habit> newList) {
        habitList.clear();
        if (newList != null) {
            habitList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public static class HabitViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon, ivCheckButton;
        TextView tvTitle, tvDescription, tvStreak;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_habit_icon);
            ivCheckButton = itemView.findViewById(R.id.iv_check_button);
            tvTitle = itemView.findViewById(R.id.tv_habit_title);
            tvDescription = itemView.findViewById(R.id.tv_habit_subtitle);
            tvStreak = itemView.findViewById(R.id.tv_streak);
        }
    }
}