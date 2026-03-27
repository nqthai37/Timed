package com.timed.Setting.AI;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.timed.R;
import java.util.List;

public class AiScheduleAdapter extends RecyclerView.Adapter<AiScheduleAdapter.ViewHolder> {

    private List<AiSchedule> scheduleList;

    public AiScheduleAdapter(List<AiSchedule> scheduleList) {
        this.scheduleList = scheduleList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AiSchedule schedule = scheduleList.get(position);
        holder.tvTitle.setText(schedule.getTitle());
        holder.tvStatus.setText(schedule.getStatus());

        // Đổi màu chữ trạng thái: Xanh nếu thành công, Đỏ/Cam nếu lỗi
        if (schedule.isSuccess()) {
            holder.tvStatus.setTextColor(Color.parseColor("#10B981")); // Màu xanh lá
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#F59E0B")); // Màu cam cảnh báo
        }
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_schedule_title);
            tvStatus = itemView.findViewById(R.id.tv_schedule_status);
        }
    }
}