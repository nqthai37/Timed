package com.timed.Features.HabitTracker; // Nhớ đổi đúng package của bạn

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.timed.R;
import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    private List<Habit> habitList;

    public HabitAdapter(List<Habit> habitList) {
        this.habitList = habitList;
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
        
        holder.tvTitle.setText(habit.getTitle());
        holder.tvSubtitle.setText(habit.getSubtitle());
        holder.ivIcon.setImageResource(habit.getIconDrawableId());

        // Kiểm tra xem đã hoàn thành chưa để đổi icon tick tương ứng
        if (habit.isCompleted()) {
            holder.ivCheckButton.setImageResource(R.drawable.ic_check_blue); // Icon xanh
        } else {
            holder.ivCheckButton.setImageResource(R.drawable.ic_check_blue);  // Icon xám
        }

        // Sự kiện khi bấm vào nút tick
        holder.ivCheckButton.setOnClickListener(v -> {
            habit.setCompleted(!habit.isCompleted()); // Đảo ngược trạng thái
            notifyItemChanged(position); // Báo cho Adapter cập nhật lại ô này
        });
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    public static class HabitViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon, ivCheckButton;
        TextView tvTitle, tvSubtitle;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_habit_icon);
            ivCheckButton = itemView.findViewById(R.id.iv_check_button);
            tvTitle = itemView.findViewById(R.id.tv_habit_title);
            tvSubtitle = itemView.findViewById(R.id.tv_habit_subtitle);
        }
    }
}