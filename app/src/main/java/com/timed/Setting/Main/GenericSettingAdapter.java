package com.timed.Setting.Main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;

import java.util.List;

public class GenericSettingAdapter extends RecyclerView.Adapter<GenericSettingAdapter.SettingViewHolder> {

    private List<SettingItemModel> itemList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SettingItemModel item);
    }

    public GenericSettingAdapter(List<SettingItemModel> itemList, OnItemClickListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SettingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_generic_setting, parent, false);
        return new SettingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingViewHolder holder, int position) {
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
        return itemList.size();
    }

    public static class SettingViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle;
        ImageView ivArrow;

        public SettingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            ivArrow = itemView.findViewById(R.id.iv_arrow);
        }
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
}
