package com.timed.Features.FocusMode;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.timed.R;
import java.util.List;

public class FocusPresetAdapter extends RecyclerView.Adapter<FocusPresetAdapter.ViewHolder> {

    private List<FocusPreset> presetList;
    private OnPresetClickListener listener;

    public interface OnPresetClickListener {
        void onPresetClick(FocusPreset preset);
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
        holder.tvTitle.setText(preset.getTitle());
        holder.tvDesc.setText(preset.getDescription());
        holder.ivIcon.setImageResource(preset.getIconResId());

        holder.itemView.setOnClickListener(v -> {
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_preset_icon);
            tvTitle = itemView.findViewById(R.id.tv_preset_title);
            tvDesc = itemView.findViewById(R.id.tv_preset_desc);
        }
    }
}