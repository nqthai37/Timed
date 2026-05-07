package com.timed.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;
import com.timed.managers.UserManager;
import com.timed.models.CalendarModel;

import java.util.ArrayList;
import java.util.List;

public class CalendarDrawerAdapter extends RecyclerView.Adapter<CalendarDrawerAdapter.CalendarViewHolder> {

    private List<CalendarModel> calendars;
    private List<String> visibleCalendarIds;
    private OnCalendarActionListener listener;

    public interface OnCalendarActionListener {
        void onCalendarToggle(CalendarModel calendar, boolean isVisible);
        void onEditCalendar(CalendarModel calendar);
    }

    public CalendarDrawerAdapter(List<CalendarModel> calendars, List<String> visibleCalendarIds, OnCalendarActionListener listener) {
        this.calendars = calendars != null ? calendars : new ArrayList<>();
        this.visibleCalendarIds = visibleCalendarIds != null ? visibleCalendarIds : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_drawer, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarModel calendar = calendars.get(position);

        holder.tvCalendarName.setText(calendar.getName() != null ? calendar.getName() : "Calendar");

        String ownerName = calendar.getOwnerName();
        if (ownerName != null) {
            ownerName = ownerName.trim();
        }
        if (ownerName == null || ownerName.isEmpty()) {
            String ownerId = calendar.getOwnerId();
            if (ownerId != null && !ownerId.isEmpty()) {
                String currentUserId = UserManager.getInstance().getCurrentUser() != null
                        ? UserManager.getInstance().getCurrentUser().getUid()
                        : null;
                if (currentUserId != null && currentUserId.equals(ownerId)) {
                    ownerName = "You";
                } else {
                    ownerName = "Unknown";
                }
            }
        }

        if (ownerName != null && !ownerName.isEmpty()) {
            holder.tvCalendarOwner.setText("Owner: " + ownerName);
            holder.tvCalendarOwner.setVisibility(View.VISIBLE);
        } else {
            holder.tvCalendarOwner.setText("");
            holder.tvCalendarOwner.setVisibility(View.GONE);
        }

        boolean isVisible = visibleCalendarIds.contains(calendar.getId());
        holder.cbVisibility.setChecked(isVisible);
        holder.cbVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onCalendarToggle(calendar, isChecked);
            }
        });

        // Set color dot
        String colorHex = calendar.getColor();
        if (colorHex != null && !colorHex.isEmpty()) {
            try {
                holder.vColorDot.setBackgroundColor(Color.parseColor(colorHex));
            } catch (IllegalArgumentException e) {
                holder.vColorDot.setBackgroundColor(Color.parseColor("#9C27B0")); // Default purple
            }
        } else {
            holder.vColorDot.setBackgroundColor(Color.parseColor("#9C27B0")); // Default purple
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditCalendar(calendar);
            }
        });
    }

    @Override
    public int getItemCount() {
        return calendars.size();
    }

    public void updateData(List<CalendarModel> newCalendars, List<String> newVisibleIds) {
        this.calendars = newCalendars != null ? newCalendars : new ArrayList<>();
        this.visibleCalendarIds = newVisibleIds != null ? newVisibleIds : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbVisibility;
        View vColorDot;
        TextView tvCalendarName;
        TextView tvCalendarOwner;
        ImageButton btnEdit;

        CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            cbVisibility = itemView.findViewById(R.id.cbCalendarVisibility);
            vColorDot = itemView.findViewById(R.id.vCalendarColorDot);
            tvCalendarName = itemView.findViewById(R.id.tvCalendarName);
            tvCalendarOwner = itemView.findViewById(R.id.tvCalendarOwner);
            btnEdit = itemView.findViewById(R.id.btnEditCalendar);
        }
    }
}
