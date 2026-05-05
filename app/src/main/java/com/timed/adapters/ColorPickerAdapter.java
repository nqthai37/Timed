package com.timed.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;
import com.timed.managers.CalendarColorManager;

import java.util.List;

public class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder> {

    private List<CalendarColorManager.CalendarColor> colors;
    private String selectedColor;
    private OnColorSelectedListener listener;

    public interface OnColorSelectedListener {
        void onColorSelected(String colorHex);
    }

    public ColorPickerAdapter(List<CalendarColorManager.CalendarColor> colors, String selectedColor, OnColorSelectedListener listener) {
        this.colors = colors;
        this.selectedColor = selectedColor;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_color_picker, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        CalendarColorManager.CalendarColor color = colors.get(position);

        try {
            holder.vColor.setBackgroundColor(Color.parseColor(color.getHex()));
        } catch (IllegalArgumentException e) {
            holder.vColor.setBackgroundColor(Color.parseColor("#9C27B0"));
        }

        boolean isSelected = selectedColor != null && selectedColor.equalsIgnoreCase(color.getHex());
        holder.ivCheckmark.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            selectedColor = color.getHex();
            notifyDataSetChanged();
            if (listener != null) {
                listener.onColorSelected(color.getHex());
            }
        });
    }

    @Override
    public int getItemCount() {
        return colors.size();
    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        View vColor;
        ImageView ivCheckmark;

        ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            vColor = itemView.findViewById(R.id.vColorOption);
            ivCheckmark = itemView.findViewById(R.id.ivCheckmark);
        }
    }
}
