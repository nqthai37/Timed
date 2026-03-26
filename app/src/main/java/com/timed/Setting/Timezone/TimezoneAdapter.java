package com.timed.Setting.Timezone;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;

import java.util.List;

// Changed class name to match TimezoneAdapter.java
public class TimezoneAdapter extends RecyclerView.Adapter<TimezoneAdapter.ViewHolder> {

    private List<SettingItemModel> itemList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SettingItemModel item);
    }

    public static class SettingItemModel {
        private String title;
        private String subtitle;
        private String id;

        public SettingItemModel(String title, String subtitle, String id) {
            this.title = title;
            this.subtitle = subtitle;
            this.id = id;
        }

        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getId() { return id; }
    }

    // Updated constructor name
    public TimezoneAdapter(List<SettingItemModel> itemList, OnItemClickListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timezone, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SettingItemModel item = itemList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvSubtitle.setText(item.getSubtitle());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvSubtitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_tz_title);
            tvSubtitle = itemView.findViewById(R.id.tv_tz_subtitle);
        }
    }
}