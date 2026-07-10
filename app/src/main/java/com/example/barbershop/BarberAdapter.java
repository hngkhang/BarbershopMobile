package com.example.barbershop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BarberAdapter extends RecyclerView.Adapter<BarberAdapter.BarberViewHolder> {

    public interface OnBarberClickListener {
        void onBarberActionClick(BarberItem barberItem);
    }

    private final List<BarberItem> barbers = new ArrayList<>();
    private final OnBarberClickListener listener;

    public BarberAdapter(OnBarberClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<BarberItem> nextBarbers) {
        barbers.clear();
        barbers.addAll(nextBarbers);
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
        BarberItem barberItem = barbers.get(position);
        holder.bind(barberItem, listener);
    }

    @Override
    public int getItemCount() {
        return barbers.size();
    }

    static class BarberViewHolder extends RecyclerView.ViewHolder {
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
            textBarberInitial = itemView.findViewById(R.id.textBarberInitial);
            textBarberName = itemView.findViewById(R.id.textBarberName);
            textBarberExperience = itemView.findViewById(R.id.textBarberExperience);
            textBarberSpecialty = itemView.findViewById(R.id.textBarberSpecialty);
            textBarberRating = itemView.findViewById(R.id.textBarberRating);
            textBarberStatus = itemView.findViewById(R.id.textBarberStatus);
            textBarberAvailableTime = itemView.findViewById(R.id.textBarberAvailableTime);
            buttonBarberAction = itemView.findViewById(R.id.buttonBarberAction);
        }

        void bind(BarberItem barberItem, OnBarberClickListener listener) {
            textBarberInitial.setText(barberItem.initial);
            textBarberInitial.setContentDescription(
                    itemView.getContext().getString(R.string.barber_avatar_content_description, barberItem.name)
            );
            textBarberName.setText(barberItem.name);
            textBarberExperience.setText(barberItem.experience);
            textBarberSpecialty.setText(barberItem.specialty);
            textBarberRating.setText(barberItem.rating);
            textBarberStatus.setText(barberItem.status);
            textBarberAvailableTime.setText(barberItem.availableTime);
            buttonBarberAction.setText(barberItem.actionLabel);
            buttonBarberAction.setOnClickListener(v -> listener.onBarberActionClick(barberItem));
        }
    }

    public static class BarberItem {
        public final String name;
        public final String initial;
        public final String experience;
        public final String specialty;
        public final String rating;
        public final String status;
        public final String availableTime;
        public final String actionLabel;
        public final boolean topRated;
        public final boolean available;

        public BarberItem(
                String name,
                String initial,
                String experience,
                String specialty,
                String rating,
                String status,
                String availableTime,
                String actionLabel,
                boolean topRated,
                boolean available
        ) {
            this.name = name;
            this.initial = initial;
            this.experience = experience;
            this.specialty = specialty;
            this.rating = rating;
            this.status = status;
            this.availableTime = availableTime;
            this.actionLabel = actionLabel;
            this.topRated = topRated;
            this.available = available;
        }
    }
}
