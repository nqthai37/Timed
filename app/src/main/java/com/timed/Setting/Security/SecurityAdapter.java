package com.timed.Setting.Security;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.timed.R;
import java.util.List;

public class SecurityAdapter extends RecyclerView.Adapter<SecurityAdapter.ViewHolder> {

    private List<SecurityOption> optionList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(SecurityOption option);
        void onSwitchChange(SecurityOption option, boolean isChecked);
    }

    public SecurityAdapter(List<SecurityOption> optionList, OnItemClickListener listener) {
        this.optionList = optionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_security, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SecurityOption option = optionList.get(position);
        holder.tvTitle.setText(option.getTitle());
        holder.ivIcon.setImageResource(option.getIconResId());

        // Xử lý ẩn/hiện đường viền dưới cùng
        holder.vDivider.setVisibility(position == optionList.size() - 1 ? View.GONE : View.VISIBLE);

        if (option.getType() == SecurityOption.TYPE_SWITCH) {
            holder.switchSec.setVisibility(View.VISIBLE);
            holder.ivArrow.setVisibility(View.GONE);
            holder.switchSec.setOnCheckedChangeListener(null); // Reset listener
            holder.switchSec.setChecked(option.isChecked());
            holder.switchSec.setOnCheckedChangeListener((btn, isChecked) -> {
                option.setChecked(isChecked);
                if (listener != null) listener.onSwitchChange(option, isChecked);
            });
            holder.itemView.setOnClickListener(v -> holder.switchSec.toggle());
        } else {
            holder.switchSec.setVisibility(View.GONE);
            holder.ivArrow.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(option);
            });
        }
    }

    @Override
    public int getItemCount() { return optionList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon, ivArrow;
        TextView tvTitle;
        SwitchMaterial switchSec;
        View vDivider;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_sec_icon);
            ivArrow = itemView.findViewById(R.id.iv_sec_arrow);
            tvTitle = itemView.findViewById(R.id.tv_sec_title);
            switchSec = itemView.findViewById(R.id.switch_sec);
            vDivider = itemView.findViewById(R.id.v_divider);
        }
    }
}