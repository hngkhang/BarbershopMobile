package com.example.barbershop.adapters;

import com.example.barbershop.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ServiceSuggestionAdapter extends RecyclerView.Adapter<ServiceSuggestionAdapter.ServiceSuggestionViewHolder> {

    public interface OnServiceSuggestionClickListener {
        void onServiceSuggestionClick(ServiceSuggestion serviceSuggestion);
    }

    private final List<ServiceSuggestion> services = new ArrayList<>();
    private final OnServiceSuggestionClickListener listener;
    private int selectedIndex = 0;

    public ServiceSuggestionAdapter(OnServiceSuggestionClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ServiceSuggestion> nextServices) {
        services.clear();
        services.addAll(nextServices);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ServiceSuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service_suggestion, parent, false);
        return new ServiceSuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceSuggestionViewHolder holder, int position) {
        holder.bind(services.get(position), position == selectedIndex, listener);
    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    public void setSelectedSuggestion(ServiceSuggestion serviceSuggestion) {
        int previousIndex = selectedIndex;
        selectedIndex = services.indexOf(serviceSuggestion);
        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
        notifyItemChanged(previousIndex);
        notifyItemChanged(selectedIndex);
    }

    public void setSelectedServiceId(long serviceId) {
        for (ServiceSuggestion service : services) {
            if (service.serviceId == serviceId) {
                setSelectedSuggestion(service);
                return;
            }
        }
    }

    static class ServiceSuggestionViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final FrameLayout imageContainer;
        private final ImageView imageIcon;
        private final TextView textName;
        private final TextView textDetail;
        private final TextView textPrice;

        ServiceSuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            imageContainer = itemView.findViewById(R.id.imageServiceSuggestionContainer);
            imageIcon = itemView.findViewById(R.id.imageServiceSuggestionIcon);
            textName = itemView.findViewById(R.id.textServiceSuggestionName);
            textDetail = itemView.findViewById(R.id.textServiceSuggestionDetail);
            textPrice = itemView.findViewById(R.id.textServiceSuggestionPrice);
        }

        void bind(ServiceSuggestion serviceSuggestion, boolean selected, OnServiceSuggestionClickListener listener) {
            imageContainer.setBackgroundResource(serviceSuggestion.imageBackgroundRes);
            imageIcon.setImageResource(serviceSuggestion.iconRes);
            imageIcon.setContentDescription(
                    itemView.getContext().getString(R.string.service_image_content_description, serviceSuggestion.name)
            );
            textName.setText(serviceSuggestion.name);
            textDetail.setText(serviceSuggestion.detail);
            textPrice.setText(serviceSuggestion.price);
            cardView.setStrokeColor(ContextCompat.getColor(
                    itemView.getContext(),
                    selected ? R.color.color_primary : R.color.color_border
            ));
            cardView.setStrokeWidth(itemView.getResources().getDimensionPixelSize(
                    selected ? R.dimen.stroke_medium : R.dimen.stroke_thin
            ));
            itemView.setOnClickListener(v -> listener.onServiceSuggestionClick(serviceSuggestion));
        }
    }

    public static class ServiceSuggestion {
        public final long serviceId;
        public final String name;
        public final String detail;
        public final String duration;
        public final String price;
        public final double totalPrice;
        public final int imageBackgroundRes;
        public final int iconRes;

        public ServiceSuggestion(
                long serviceId,
                String name,
                String detail,
                String duration,
                String price,
                double totalPrice,
                int imageBackgroundRes,
                int iconRes
        ) {
            this.serviceId = serviceId;
            this.name = name;
            this.detail = detail;
            this.duration = duration;
            this.price = price;
            this.totalPrice = totalPrice;
            this.imageBackgroundRes = imageBackgroundRes;
            this.iconRes = iconRes;
        }
    }
}
