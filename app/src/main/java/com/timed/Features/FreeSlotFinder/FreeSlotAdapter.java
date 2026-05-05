package com.timed.Features.FreeSlotFinder;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;

import java.util.List;

public class FreeSlotAdapter extends RecyclerView.Adapter<FreeSlotAdapter.ViewHolder> {
    private Context context;
    private List<FreeSlot> slotList;
    private int selectedPosition = -1;
    private OnSlotSelectedListener listener;

    public interface OnSlotSelectedListener {
        void onSlotSelected(FreeSlot slot);
    }

    public FreeSlotAdapter(Context context, List<FreeSlot> slotList, OnSlotSelectedListener listener) {
        this.context = context;
        this.slotList = slotList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_free_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FreeSlot slot = slotList.get(position);

        holder.tvTimeRange.setText(slot.getTimeRange());
        holder.tvDurationType.setText(slot.getDurationType());

        boolean isSelected = (position == selectedPosition);

        if (isSelected) {
            holder.itemView.setBackgroundResource(R.drawable.bg_slot_selected);
            holder.ivAdd.setImageResource(R.drawable.ic_check);
            holder.ivAdd.setColorFilter(Color.parseColor("#3B82F6"));
            holder.ivAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EFF6FF")));
        } else {
            // Unselected State: Default white card
            holder.itemView.setBackgroundResource(R.drawable.bg_rounded);
            holder.ivAdd.setImageResource(R.drawable.ic_add);
            holder.ivAdd.setColorFilter(Color.parseColor("#94A3B8"));
            holder.ivAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
        }

        holder.itemView.setOnClickListener(v -> {
            int clickedPosition = holder.getAdapterPosition();
            int previousPosition = selectedPosition;

            if (clickedPosition == selectedPosition) {
                selectedPosition = -1;
                notifyItemChanged(previousPosition);

                listener.onSlotSelected(null);
            } else {
                selectedPosition = clickedPosition;

                notifyItemChanged(previousPosition);
                notifyItemChanged(selectedPosition);

                listener.onSlotSelected(slot);
            }
        });
    }

    @Override
    public int getItemCount() {
        return slotList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimeRange;
        TextView tvDurationType;
        ImageView ivAdd;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimeRange = itemView.findViewById(R.id.tv_time_range);
            tvDurationType = itemView.findViewById(R.id.tv_duration_type);
            ivAdd = itemView.findViewById(R.id.iv_add);
        }
    }
}
