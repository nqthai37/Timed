package com.timed.Setting.FocusMode;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.timed.R;
import java.util.List;
import com.zerobranch.layout.SwipeLayout;

public class FocusPresetAdapter extends RecyclerView.Adapter<FocusPresetAdapter.ViewHolder> {

    private List<FocusPreset> presetList;
    private OnPresetClickListener listener;

    public interface OnPresetClickListener {
        void onPresetClick(FocusPreset preset);
        void onPresetDeleteClick(FocusPreset preset);
    }

    public FocusPresetAdapter(List<FocusPreset> presetList, OnPresetClickListener listener) {
        this.presetList = presetList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_focus_preset, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FocusPreset preset = presetList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(preset.getTitle());
        holder.tvDesc.setText(preset.getDescription());

        String iconName = preset.getIconName();
        if (iconName != null && !iconName.isEmpty()) {
            int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());

            if (resId != 0) {
                holder.ivIcon.setImageResource(resId);
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_clock);
            }
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_clock);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPresetClick(preset);
        });

        holder.flDelete.setOnClickListener(v -> {
            if (listener != null) {
                holder.swipeLayout.close();
                listener.onPresetDeleteClick(preset);
            }
        });

        holder.mainCard.setOnClickListener(v -> {
            if (listener != null) listener.onPresetClick(preset);
        });
    }

    @Override
    public int getItemCount() {
        return presetList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvDesc;

        SwipeLayout swipeLayout;
        View flDelete;
        View mainCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_preset_icon);
            tvTitle = itemView.findViewById(R.id.tv_preset_title);
            tvDesc = itemView.findViewById(R.id.tv_preset_desc);

            swipeLayout = itemView.findViewById(R.id.swipe_layout);
            flDelete = itemView.findViewById(R.id.fl_delete_layout);
            mainCard = itemView.findViewById(R.id.cl_main_card);
        }
    }
}