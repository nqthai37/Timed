package com.timed.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;
import com.timed.models.Task;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> tasks;
    private final OnTaskClickListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskCheckChanged(Task task, boolean isChecked); // Xử lý khi user bấm tick hoàn thành
    }

    public TaskAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                // Bạn cần tạo file item_task.xml có layout gồm 1 CheckBox và 2 TextView (Title, Time)
                .inflate(R.layout.item_task, parent, false); 
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        
        holder.tvTaskTitle.setText(task.getTitle());
        holder.tvTaskTime.setText(task.getDue_date() != null ? timeFormat.format(task.getDue_date().toDate()) : "");
        
        // Bỏ listener cũ để tránh lỗi vòng lặp khi tái sử dụng view
        holder.cbTaskCompleted.setOnCheckedChangeListener(null);
        holder.cbTaskCompleted.setChecked(task.isIs_completed());

        // Gạch ngang chữ nếu đã hoàn thành
        if (task.isIs_completed()) {
            holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        // Bắt sự kiện bấm vào cả dòng (để sửa)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(task);
        });

        // Bắt sự kiện bấm vào ô Checkbox (để hoàn thành)
        holder.cbTaskCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onTaskCheckChanged(task, isChecked);
        });
    }

    @Override
    public int getItemCount() { return tasks.size(); }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbTaskCompleted;
        TextView tvTaskTitle, tvTaskTime;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ id từ item_task.xml
            cbTaskCompleted = itemView.findViewById(R.id.cbTaskCompleted);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskTime = itemView.findViewById(R.id.tvTaskTime);
        }
    }
}