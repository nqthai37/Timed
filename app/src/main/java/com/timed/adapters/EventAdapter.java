package com.timed.adapters;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.timed.R;
import com.timed.models.Event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> events;
    private final OnEventClickListener onEventClickListener;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventAdapter(List<Event> events) {
        this(events, null);
    }

    public EventAdapter(List<Event> events, OnEventClickListener onEventClickListener) {
        this.events = events;
        this.onEventClickListener = onEventClickListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.tvEventTime.setText(formatTime(event.getStartTime()));
        holder.tvEventTitle.setText(event.getTitle());
        holder.tvEventDetails.setText(buildDetails(event));
        holder.tvEventTime.setTextColor(parseCalendarColor(event.getColor()));

        holder.itemView.setOnClickListener(v -> {
            if (onEventClickListener != null) {
                onEventClickListener.onEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    private String buildDetails(Event event) {
        String description = event.getDescription() != null ? event.getDescription().trim() : "";
        String location = event.getLocation() != null ? event.getLocation().trim() : "";
        String calendarName = event.getCalendarName() != null ? event.getCalendarName().trim() : "";

        StringBuilder details = new StringBuilder();
        if (!TextUtils.isEmpty(calendarName)) {
            details.append(calendarName);
        }
        if (!TextUtils.isEmpty(description)) {
            if (details.length() > 0) {
                details.append(" • ");
            }
            details.append(description);
        }
        if (!TextUtils.isEmpty(location)) {
            if (details.length() > 0) {
                details.append(" • ");
            }
            details.append(location);
        }

        return details.toString();
    }

    private String formatTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        Date date = timestamp.toDate();
        return timeFormat.format(date);
    }

    private int parseCalendarColor(String hexColor) {
        if (!TextUtils.isEmpty(hexColor)) {
            try {
                return Color.parseColor(hexColor);
            } catch (IllegalArgumentException ignored) {
                // Fall back to existing visual style color.
            }
        }
        return Color.parseColor("#741ce9");
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTime, tvEventTitle, tvEventDetails;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTime = itemView.findViewById(R.id.tvEventTime);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventDetails = itemView.findViewById(R.id.tvEventDetails);
        }
    }
}
