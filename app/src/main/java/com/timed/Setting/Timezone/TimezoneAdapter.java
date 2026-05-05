package com.timed.Setting.Timezone;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying timezone items.
 * Shows the UTC offset, display name, representative cities,
 * current local time in that timezone, and highlights the selected timezone.
 */
public class TimezoneAdapter extends RecyclerView.Adapter<TimezoneAdapter.ViewHolder> {

    private List<TimezoneItem> itemList;
    private String selectedTimezoneId;
    private final OnTimezoneSelectedListener listener;

    public interface OnTimezoneSelectedListener {
        void onTimezoneSelected(TimezoneItem item);
    }

    public TimezoneAdapter(List<TimezoneItem> itemList, String selectedTimezoneId,
                           OnTimezoneSelectedListener listener) {
        this.itemList = new ArrayList<>(itemList);
        this.selectedTimezoneId = selectedTimezoneId;
        this.listener = listener;
    }

    public void updateList(List<TimezoneItem> newList) {
        this.itemList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void setSelectedTimezoneId(String timezoneId) {
        String oldId = this.selectedTimezoneId;
        this.selectedTimezoneId = timezoneId;

        // Find and update old and new positions for efficient UI refresh
        for (int i = 0; i < itemList.size(); i++) {
            String id = itemList.get(i).getTimezoneId();
            if (id.equals(oldId) || id.equals(timezoneId)) {
                notifyItemChanged(i);
            }
        }
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
        TimezoneItem item = itemList.get(position);
        boolean isSelected = item.getTimezoneId().equals(selectedTimezoneId);

        // Set UTC offset label
        holder.tvTitle.setText(item.getUtcOffset());

        // Set display name and cities as subtitle
        String subtitle = item.getDisplayName();
        if (item.getCities() != null && !item.getCities().isEmpty()) {
            subtitle += "\n" + item.getCities();
        }
        holder.tvSubtitle.setText(subtitle);

        // Set current time in this timezone
        holder.tvTime.setText(item.getCurrentTime());

        // Set icon tint based on offset (negative offsets = blue tones, positive = warm tones)
        int rawOffset = item.getRawOffset();
        int iconTintColor;
        if (rawOffset < 0) {
            iconTintColor = Color.parseColor("#3B82F6"); // Blue for negative offsets
        } else if (rawOffset == 0) {
            iconTintColor = Color.parseColor("#10B981"); // Green for UTC
        } else {
            iconTintColor = Color.parseColor("#F59E0B"); // Amber for positive offsets
        }

        // Highlight selected item
        if (isSelected) {
            holder.itemView.setBackgroundTintList(
                    ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.blue_50));
            holder.ivIcon.setImageResource(R.drawable.ic_check_blue);
            holder.tvTitle.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.primary));
        } else {
            holder.itemView.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.WHITE));
            holder.ivIcon.setImageResource(R.drawable.ic_globe);
            holder.ivIcon.setColorFilter(iconTintColor);
            holder.tvTitle.setTextColor(Color.parseColor("#0F172A"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                setSelectedTimezoneId(item.getTimezoneId());
                listener.onTimezoneSelected(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvSubtitle;
        TextView tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_tz_icon);
            tvTitle = itemView.findViewById(R.id.tv_tz_title);
            tvSubtitle = itemView.findViewById(R.id.tv_tz_subtitle);
            tvTime = itemView.findViewById(R.id.tv_tz_time);
        }
    }
}