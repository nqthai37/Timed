package com.timed.Features;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.models.Feature;
import com.timed.R;

import java.util.List;

public class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder> {

    private final List<Feature> featureList;
    private final Context context;

    public FeatureAdapter(Context context, List<Feature> featureList) {
        this.context = context;
        this.featureList = featureList;
    }

    @NonNull
    @Override
    public FeatureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Đảm bảo R.layout.feature_item là tên file XML layout của bạn
        View view = LayoutInflater.from(context).inflate(R.layout.feature_item, parent, false);
        return new FeatureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeatureViewHolder holder, int position) {
        Feature feature = featureList.get(position);

        // 1. Xử lý hiển thị Header (Invisible Title)
        if (feature.getHeaderName() != null) {
            holder.tvHeaderTitle.setVisibility(View.VISIBLE);
            holder.tvHeaderTitle.setText(feature.getHeaderName());
        } else {
            holder.tvHeaderTitle.setVisibility(View.GONE);
        }

        // 2. Gán dữ liệu vào các View
        holder.tvFeatureName.setText(feature.getName());
        holder.tvFeatureDescription.setText(feature.getDescription());
        
        if (feature.getIconDrawableId() != 0) {
            holder.ivFeatureIcon.setImageResource(feature.getIconDrawableId());
        }

        // 3. Sự kiện Click để mở Activity
        holder.itemView.setOnClickListener(v -> {
            if (feature.getActivityClass() != null) {
                Intent intent = new Intent(context, feature.getActivityClass());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return featureList.size();
    }

    public static class FeatureViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderTitle, tvFeatureName, tvFeatureDescription;
        ImageView ivFeatureIcon;

        public FeatureViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ đúng các ID từ file XML bạn đã cung cấp
            tvHeaderTitle = itemView.findViewById(R.id.tvHeaderTitle);
            tvFeatureName = itemView.findViewById(R.id.title);
            tvFeatureDescription = itemView.findViewById(R.id.subtitle);
            ivFeatureIcon = itemView.findViewById(R.id.icon);
        }
    }
}