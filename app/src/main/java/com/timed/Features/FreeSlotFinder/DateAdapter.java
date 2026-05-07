package com.timed.Features.FreeSlotFinder;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;

import java.util.List;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.ViewHolder> {
    private Context context;
    private List<Date> dateList;
    private int selectedPosition = 0; // Default to the first item ("Today")
    private OnDateSelectedListener listener;

    public interface OnDateSelectedListener {
        void onDateSelected(Date date);
    }

    public DateAdapter(Context context, List<Date> dateList, OnDateSelectedListener listener) {
        this.context = context;
        this.dateList = dateList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_date_selector, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Date date = dateList.get(position);
        holder.tvDate.setText(date.getDisplayTitle());

        if (position == selectedPosition) {
            holder.tvDate.setBackgroundResource(R.drawable.bg_slot_selected);
            holder.tvDate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#3B82F6")));
            holder.tvDate.setTextColor(Color.WHITE);
        } else {
            holder.tvDate.setBackgroundResource(R.drawable.bg_rounded);
            holder.tvDate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
            holder.tvDate.setTextColor(Color.parseColor("#64748B"));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            listener.onDateSelected(date);
        });
    }

    @Override
    public int getItemCount() { return dateList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
        }
    }
}
