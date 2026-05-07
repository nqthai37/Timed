package com.timed.Features.AI;

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
    private final OnTemplateClickListener clickListener;
    private final OnTemplateLongClickListener longClickListener;

    public AiScheduleAdapter(List<AiSchedule> scheduleList, OnTemplateClickListener clickListener, OnTemplateLongClickListener longClickListener) {
        this.scheduleList = scheduleList;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public interface OnTemplateClickListener {
        void onTemplateClick(AiSchedule template);
    }

    public interface OnTemplateLongClickListener {
        void onTemplateLongClick(AiSchedule template);
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

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTemplateClick(schedule);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onTemplateLongClick(schedule);
            return true;
        });
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