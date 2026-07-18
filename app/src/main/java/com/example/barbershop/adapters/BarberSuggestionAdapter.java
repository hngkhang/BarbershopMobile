package com.example.barbershop.adapters;

import com.example.barbershop.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class BarberSuggestionAdapter extends RecyclerView.Adapter<BarberSuggestionAdapter.BarberSuggestionViewHolder> {

    public interface OnBarberSuggestionClickListener {
        void onBarberSuggestionClick(BarberSuggestion barberSuggestion);
    }

    private final List<BarberSuggestion> barbers = new ArrayList<>();
    private final OnBarberSuggestionClickListener listener;
    private int selectedIndex = 0;

    public BarberSuggestionAdapter(OnBarberSuggestionClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<BarberSuggestion> nextBarbers) {
        barbers.clear();
        barbers.addAll(nextBarbers);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BarberSuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_barber_suggestion, parent, false);
        return new BarberSuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BarberSuggestionViewHolder holder, int position) {
        holder.bind(barbers.get(position), position == selectedIndex, listener);
    }

    @Override
    public int getItemCount() {
        return barbers.size();
    }

    public void setSelectedSuggestion(BarberSuggestion barberSuggestion) {
        int previousIndex = selectedIndex;
        selectedIndex = barbers.indexOf(barberSuggestion);
        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
        notifyItemChanged(previousIndex);
        notifyItemChanged(selectedIndex);
    }

    public void setSelectedBarberId(long barberId) {
        for (BarberSuggestion barber : barbers) {
            if (barber.barberId == barberId) {
                setSelectedSuggestion(barber);
                return;
            }
        }
    }

    static class BarberSuggestionViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView textInitial;
        private final TextView textName;
        private final TextView textExperience;
        private final TextView textRating;

        BarberSuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            textInitial = itemView.findViewById(R.id.textBarberSuggestionInitial);
            textName = itemView.findViewById(R.id.textBarberSuggestionName);
            textExperience = itemView.findViewById(R.id.textBarberSuggestionExperience);
            textRating = itemView.findViewById(R.id.textBarberSuggestionRating);
        }

        void bind(BarberSuggestion barberSuggestion, boolean selected, OnBarberSuggestionClickListener listener) {
            textInitial.setText(barberSuggestion.initial);
            textInitial.setContentDescription(
                    itemView.getContext().getString(R.string.barber_avatar_content_description, barberSuggestion.name)
            );
            textName.setText(barberSuggestion.name);
            textExperience.setText(barberSuggestion.experience);
            textRating.setText(barberSuggestion.rating);
            cardView.setStrokeColor(ContextCompat.getColor(
                    itemView.getContext(),
                    selected ? R.color.color_primary : R.color.color_border
            ));
            cardView.setStrokeWidth(itemView.getResources().getDimensionPixelSize(
                    selected ? R.dimen.stroke_medium : R.dimen.stroke_thin
            ));
            itemView.setOnClickListener(v -> listener.onBarberSuggestionClick(barberSuggestion));
        }
    }

    public static class BarberSuggestion {
        public final long barberId;
        public final String name;
        public final String initial;
        public final String experience;
        public final String rating;
        public final String specialty;

        public BarberSuggestion(
                long barberId,
                String name,
                String initial,
                String experience,
                String rating,
                String specialty
        ) {
            this.barberId = barberId;
            this.name = name;
            this.initial = initial;
            this.experience = experience;
            this.rating = rating;
            this.specialty = specialty;
        }
    }
}
