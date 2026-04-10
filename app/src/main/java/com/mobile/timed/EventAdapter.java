package com.mobile.timed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> events;

    public EventAdapter(List<Event> events) {
        this.events = events;
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
        holder.tvEventTime.setText(event.getTime());
        holder.tvEventTitle.setText(event.getTitle());
        holder.tvEventDetails.setText(event.getDetails());
    }

    @Override
    public int getItemCount() {
        return events.size();
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
