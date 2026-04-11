package com.timed.Setting.ConflictResolver;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;

import java.util.List;

public class ConflictEventAdapter extends RecyclerView.Adapter<ConflictEventAdapter.ViewHolder> {

    // --- MODEL DỮ LIỆU ĐÃ ĐƯỢC ĐỊNH NGHĨA SẴN CỦA BẠN (Rất tốt) ---
    public static class ConflictEvent {
        private String title;
        private String timeRange;
        private String description; // Nội dung Conflicts with...
        private String id;
        private boolean isConflicting;
        private Class<?> activity;

        public ConflictEvent(String title, String timeRange, String description, String id, boolean isConflicting, Class<?> activity) {
            this.title = title;
            this.timeRange = timeRange;
            this.description = description;
            this.id = id;
            this.isConflicting = isConflicting;
            this.activity = activity;
        }

        public String getTitle() { return title; }
        public String getTimeRange() { return timeRange; }
        public String getDescription() { return description; }
        public String getId() { return id; }
        public boolean isConflicting() { return isConflicting; }
        public Class<?> getActivity() { return activity; }
    }
    // -------------------------------------------------------------

    private Context context;
    private List<ConflictEvent> conflictEventList;

    public ConflictEventAdapter(Context context, List<ConflictEvent> conflictEventList) {
        this.context = context;
        this.conflictEventList = conflictEventList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conflict_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConflictEvent event = conflictEventList.get(position);

        // --- BỔ SUNG LÓGIC ĐỔ DỮ LIỆU VÀO ĐÂY ---
        // 1. Set tên sự kiện
        holder.tvTitle.setText(event.getTitle());
        
        // 2. Set khoảng thời gian
        holder.tvTime.setText(event.getTimeRange());
        
        // 3. Set mô tả xung đột (Dùng nối chuỗi để hiện chữ "Conflicts with: ...")
        holder.tvConflictDescription.setText("Conflicts with: " + event.getDescription());

        // Xử lý click item (Giữ nguyên của bạn)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, event.getActivity());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return conflictEventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // --- ÁNH XẠ ĐỦ CÁC VIEW TỪ XML ---
        TextView tvTitle;
        TextView tvTime;
        TextView tvConflictDescription;
        ImageView ivWarningTime;
        ImageView ivMore;
        View viewIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ đúng ID từ file item_conflict_event.xml mới
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvConflictDescription = itemView.findViewById(R.id.tv_conflict_description);
            ivWarningTime = itemView.findViewById(R.id.iv_warning_time);
            ivMore = itemView.findViewById(R.id.iv_more);
            viewIndicator = itemView.findViewById(R.id.viewIndicator);
        }
    }
}