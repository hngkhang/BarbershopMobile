package com.example.barbershop.adapters;

import com.example.barbershop.R;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barbershop.services.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    public interface OnServiceClickListener {
        void onBookNowClick(ServiceItem serviceItem);
    }

    private final List<ServiceItem> services = new ArrayList<>();
    private final OnServiceClickListener listener;

    public ServiceAdapter(OnServiceClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ServiceItem> nextServices) {
        services.clear();
        services.addAll(nextServices);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        ServiceItem serviceItem = services.get(position);
        holder.bind(serviceItem, listener);
    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    static class ServiceViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout imageContainer;
        private final ImageView imageServiceIcon;
        private final TextView textServiceName;
        private final TextView textServiceDescription;
        private final TextView textServiceDuration;
        private final TextView textServicePrice;
        private final AppCompatButton buttonBookNow;

        ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            imageContainer = itemView.findViewById(R.id.imageServiceContainer);
            imageContainer.setClipToOutline(true);
            imageServiceIcon = itemView.findViewById(R.id.imageServiceIcon);
            textServiceName = itemView.findViewById(R.id.textServiceName);
            textServiceDescription = itemView.findViewById(R.id.textServiceDescription);
            textServiceDuration = itemView.findViewById(R.id.textServiceDuration);
            textServicePrice = itemView.findViewById(R.id.textServicePrice);
            buttonBookNow = itemView.findViewById(R.id.buttonBookNow);
        }

        void bind(ServiceItem serviceItem, OnServiceClickListener listener) {
            imageContainer.setBackgroundResource(serviceItem.imageBackgroundRes);
            configureServiceImage(serviceItem);
            ImageLoader.loadImage(imageServiceIcon, serviceItem.imageUrl, serviceItem.iconRes);
            imageServiceIcon.setContentDescription(
                    itemView.getContext().getString(R.string.service_image_content_description, serviceItem.name)
            );
            textServiceName.setText(serviceItem.name);
            textServiceDescription.setText(serviceItem.description);
            textServiceDuration.setText(serviceItem.duration);
            textServicePrice.setText(serviceItem.price);
            buttonBookNow.setOnClickListener(v -> listener.onBookNowClick(serviceItem));
        }

        private void configureServiceImage(ServiceItem serviceItem) {
            boolean hasRemoteImage = !TextUtils.isEmpty(serviceItem.imageUrl);
            FrameLayout.LayoutParams layoutParams =
                    (FrameLayout.LayoutParams) imageServiceIcon.getLayoutParams();

            if (hasRemoteImage) {
                layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;
                layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT;
                imageServiceIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                int iconSize = itemView.getResources().getDimensionPixelSize(R.dimen.space_40);
                layoutParams.width = iconSize;
                layoutParams.height = iconSize;
                imageServiceIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }

            imageServiceIcon.setLayoutParams(layoutParams);
        }
    }

    public static class ServiceItem {
        public final String name;
        public final String description;
        public final String duration;
        public final String price;
        public final String category;
        public final String imageUrl;
        public final int imageBackgroundRes;
        public final int iconRes;

        public ServiceItem(
                String name,
                String description,
                String duration,
                String price,
                String category,
                String imageUrl,
                int imageBackgroundRes,
                int iconRes
        ) {
            this.name = name;
            this.description = description;
            this.duration = duration;
            this.price = price;
            this.category = category;
            this.imageUrl = imageUrl;
            this.imageBackgroundRes = imageBackgroundRes;
            this.iconRes = iconRes;
        }
    }
}
