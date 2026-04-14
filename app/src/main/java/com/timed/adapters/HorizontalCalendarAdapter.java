package com.timed.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class HorizontalCalendarAdapter extends RecyclerView.Adapter<HorizontalCalendarAdapter.DayViewHolder> {

    private final List<LocalDate> dateList;
    private LocalDate selectedDate;
    private final OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(LocalDate date, int position);
    }

    public HorizontalCalendarAdapter(List<LocalDate> dateList, LocalDate selectedDate, OnDayClickListener listener) {
        this.dateList = dateList;
        this.selectedDate = selectedDate;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_horizontal_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        LocalDate date = dateList.get(position);

        // Hiển thị Thứ (Sun, Mon...) và Ngày (06, 07...)
        DateTimeFormatter dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
        holder.tvDayOfWeek.setText(date.format(dayOfWeekFormatter));
        holder.tvDayOfMonth.setText(String.format(Locale.getDefault(), "%02d", date.getDayOfMonth()));

        // Xử lý đổi màu (Đang chọn vs Bình thường)
        if (date.equals(selectedDate)) {
            // Ngày được chọn: Nền tím, chữ trắng, rõ nét 100%
            holder.layoutDayContainer.setBackgroundResource(R.drawable.bg_day_selected);
            holder.tvDayOfWeek.setTextColor(Color.WHITE);
            holder.tvDayOfMonth.setTextColor(Color.WHITE);
            holder.layoutDayContainer.setAlpha(1.0f);
        } else {
            // Ngày bình thường: Nền trong suốt, chữ đen mờ 50%
            holder.layoutDayContainer.setBackgroundResource(0);
            holder.tvDayOfWeek.setTextColor(Color.parseColor("#0f172a")); // slate-900
            holder.tvDayOfMonth.setTextColor(Color.parseColor("#0f172a"));
            holder.layoutDayContainer.setAlpha(0.5f); // opacity-50 giống HTML
        }

        // Bắt sự kiện Click
        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                // Cập nhật ngày đang chọn và báo cho RecyclerView vẽ lại
                selectedDate = date;
                notifyDataSetChanged();
                listener.onDayClick(date, currentPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutDayContainer;
        TextView tvDayOfWeek, tvDayOfMonth;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutDayContainer = itemView.findViewById(R.id.layoutDayContainer);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvDayOfMonth = itemView.findViewById(R.id.tvDayOfMonth);
        }
    }
}
