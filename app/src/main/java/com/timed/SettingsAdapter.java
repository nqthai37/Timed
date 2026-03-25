package com.timed;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.SettingViewHolder> {

    private Context context;
    private List<SettingItem> settingList;

    public SettingsAdapter(Context context, List<SettingItem> settingList) {
        this.context = context;
        this.settingList = settingList;
    }

    @NonNull
    @Override
    public SettingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_setting, parent, false);
        return new SettingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingViewHolder holder, int position) {
        SettingItem item = settingList.get(position);

        holder.tvTitle.setText(item.getName());
        holder.ivIcon.setImageResource(item.getIconId());

        // CHÌA KHÓA Ở ĐÂY: Nếu có tên nhóm -> HIỆN. Nếu không -> ẨN.
        if (item.getHeaderTitle() != null && !item.getHeaderTitle().isEmpty()) {
            holder.tvHeaderTitle.setVisibility(View.VISIBLE);
            holder.tvHeaderTitle.setText(item.getHeaderTitle());
        } else {
            holder.tvHeaderTitle.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, item.getTargetActivity());
            context.startActivity(intent);
        });
    }
    @Override
    public int getItemCount() {
        return settingList.size();
    }

    public static class SettingViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        ImageView ivIcon;
        TextView tvHeaderTitle; // Khai báo thêm

        public SettingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.title);
            ivIcon = itemView.findViewById(R.id.icon);
            tvHeaderTitle = itemView.findViewById(R.id.tvHeaderTitle); // Ánh xạ
        }
    }
}