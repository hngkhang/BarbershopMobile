package com.example.barbershop.adapters;

import com.example.barbershop.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barbershop.models.Barber;
import com.example.barbershop.services.ImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BarberAdapter extends RecyclerView.Adapter<BarberAdapter.BarberViewHolder> {

    public interface OnBarberClickListener {
        void onBarberActionClick(Barber barber);
    }

    private final List<Barber> barbers = new ArrayList<>();
    private final OnBarberClickListener listener;

    public BarberAdapter(OnBarberClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Barber> nextBarbers) {
        barbers.clear();
        if (nextBarbers != null) {
            barbers.addAll(nextBarbers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BarberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_barber, parent, false);
        return new BarberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BarberViewHolder holder, int position) {
        Barber barber = barbers.get(position);
        holder.bind(barber, listener);
    }

    @Override
    public int getItemCount() {
        return barbers.size();
    }

    static class BarberViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageBarberAvatar;
        private final TextView textBarberInitial;
        private final TextView textBarberName;
        private final TextView textBarberExperience;
        private final TextView textBarberSpecialty;
        private final TextView textBarberRating;
        private final TextView textBarberStatus;
        private final TextView textBarberAvailableTime;
        private final AppCompatButton buttonBarberAction;

        BarberViewHolder(@NonNull View itemView) {
            super(itemView);
            imageBarberAvatar = itemView.findViewById(R.id.imageBarberAvatar);
            textBarberInitial = itemView.findViewById(R.id.textBarberInitial);
            textBarberName = itemView.findViewById(R.id.textBarberName);
            textBarberExperience = itemView.findViewById(R.id.textBarberExperience);
            textBarberSpecialty = itemView.findViewById(R.id.textBarberSpecialty);
            textBarberRating = itemView.findViewById(R.id.textBarberRating);
            textBarberStatus = itemView.findViewById(R.id.textBarberStatus);
            textBarberAvailableTime = itemView.findViewById(R.id.textBarberAvailableTime);
            buttonBarberAction = itemView.findViewById(R.id.buttonBarberAction);
        }

        void bind(Barber barber, OnBarberClickListener listener) {
            ImageLoader.loadAvatar(
                    imageBarberAvatar,
                    textBarberInitial,
                    barber.getAvatarUrl(),
                    barber.getInitial()
            );
            textBarberInitial.setContentDescription(
                    itemView.getContext().getString(R.string.barber_avatar_content_description, barber.getName())
            );
            textBarberName.setText(barber.getName());
            textBarberExperience.setText(formatExperience(barber.getExperience()));
            textBarberSpecialty.setText(R.string.barber_specialty_not_available);
            textBarberRating.setText(String.format(Locale.US, "%.1f", barber.getDisplayRating()));
            textBarberStatus.setText(R.string.barber_status_active);
            textBarberAvailableTime.setText(R.string.barber_schedule_view_details);
            buttonBarberAction.setText(R.string.action_view_profile);
            buttonBarberAction.setOnClickListener(v -> notifyBarberClick(barber, listener));
            itemView.setOnClickListener(v -> notifyBarberClick(barber, listener));
        }

        private void notifyBarberClick(Barber barber, OnBarberClickListener listener) {
            if (listener != null && barber.isActive()) {
                listener.onBarberActionClick(barber);
            }
        }

        private String formatExperience(String experience) {
            if (experience == null || experience.trim().isEmpty()) {
                return itemView.getContext().getString(R.string.barber_experience_unknown);
            }

            String trimmedExperience = experience.trim();
            if (trimmedExperience.toLowerCase(Locale.US).contains("year")) {
                return trimmedExperience;
            }

            return itemView.getContext().getString(
                    R.string.barber_experience_years_format,
                    trimmedExperience
            );
        }
    }
}
