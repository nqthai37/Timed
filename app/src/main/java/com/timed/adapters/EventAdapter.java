package com.timed.adapters;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.firebase.Timestamp;
import com.timed.R;
import com.timed.Setting.Timezone.TimezoneHelper;
import com.timed.models.Event;
import com.timed.utils.ThemeManager;

import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> events;
    private final OnEventClickListener onEventClickListener;
    private final boolean useThemeTimeColor;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventAdapter(List<Event> events) {
        this(events, null, false);
    }

    public EventAdapter(List<Event> events, OnEventClickListener onEventClickListener) {
        this(events, onEventClickListener, false);
    }

    public EventAdapter(List<Event> events, OnEventClickListener onEventClickListener, boolean useThemeTimeColor) {
        this.events = events;
        this.onEventClickListener = onEventClickListener;
        this.useThemeTimeColor = useThemeTimeColor;
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
        holder.tvEventTime.setText(formatTime(holder.itemView.getContext(), event.getStartTime()));
        holder.tvEventTitle.setText(event.getTitle());
        holder.tvEventDetails.setText(buildDetails(event));
        holder.tvEventTime.setTextColor(resolveEventTimeColor(holder.itemView, event.getColor()));

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

    private String formatTime(android.content.Context context, Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        // Use user-selected timezone for time display
        return TimezoneHelper.formatTime24h(context, timestamp.toDate());
    }

    private int resolveEventTimeColor(View view, String hexColor) {
        if (useThemeTimeColor) {
            return ThemeManager.getPrimaryColor(view.getContext());
        }
        if (!TextUtils.isEmpty(hexColor)) {
            try {
                return Color.parseColor(hexColor);
            } catch (IllegalArgumentException ignored) {
                // Fall back to existing visual style color.
            }
        }
        return MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary);
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
