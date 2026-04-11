package com.timed.Setting.SyncStorage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.timed.R;
import java.util.List;

public class SyncAdapter extends RecyclerView.Adapter<SyncAdapter.ViewHolder> {

    private List<SyncOption> optionList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onSwitchChange(SyncOption option, boolean isChecked);
    }

    public SyncAdapter(List<SyncOption> optionList, OnItemClickListener listener) {
        this.optionList = optionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sync_storage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SyncOption option = optionList.get(position);
        holder.tvTitle.setText(option.getTitle());

        // Ẩn đường viền ở mục cuối cùng
        holder.vDivider.setVisibility(position == optionList.size() - 1 ? View.GONE : View.VISIBLE);

        if (option.getType() == SyncOption.TYPE_SWITCH) {
            holder.switchSync.setVisibility(View.VISIBLE);
            holder.tvValue.setVisibility(View.GONE);
            holder.switchSync.setOnCheckedChangeListener(null); 
            holder.switchSync.setChecked(option.isChecked());
            holder.switchSync.setOnCheckedChangeListener((btn, isChecked) -> {
                option.setChecked(isChecked);
                if (listener != null) listener.onSwitchChange(option, isChecked);
            });
            holder.itemView.setOnClickListener(v -> holder.switchSync.toggle());
        } else {
            holder.switchSync.setVisibility(View.GONE);
            holder.tvValue.setVisibility(View.VISIBLE);
            holder.tvValue.setText(option.getValue());
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() { return optionList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvValue;
        SwitchMaterial switchSync;
        View vDivider;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_sync_title);
            tvValue = itemView.findViewById(R.id.tv_sync_value);
            switchSync = itemView.findViewById(R.id.switch_sync);
            vDivider = itemView.findViewById(R.id.v_divider);
        }
    }
}