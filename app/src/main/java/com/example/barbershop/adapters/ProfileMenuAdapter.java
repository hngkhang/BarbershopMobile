package com.example.barbershop.adapters;

import com.example.barbershop.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ProfileMenuAdapter extends RecyclerView.Adapter<ProfileMenuAdapter.ProfileMenuViewHolder> {

    public interface OnProfileMenuClickListener {
        void onProfileMenuClick(ProfileMenuItem profileMenuItem);
    }

    private final List<ProfileMenuItem> menuItems = new ArrayList<>();
    private final OnProfileMenuClickListener listener;

    public ProfileMenuAdapter(OnProfileMenuClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ProfileMenuItem> nextItems) {
        menuItems.clear();
        menuItems.addAll(nextItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProfileMenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile_menu, parent, false);
        return new ProfileMenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileMenuViewHolder holder, int position) {
        holder.bind(menuItems.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    static class ProfileMenuViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final ImageView chevron;
        private final TextView title;

        ProfileMenuViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.imageProfileMenuIcon);
            chevron = itemView.findViewById(R.id.imageProfileMenuChevron);
            title = itemView.findViewById(R.id.textProfileMenuTitle);
        }

        void bind(ProfileMenuItem item, OnProfileMenuClickListener listener) {
            int color = ContextCompat.getColor(
                    itemView.getContext(),
                    item.danger ? R.color.color_error : R.color.color_text_secondary
            );
            icon.setImageResource(item.iconRes);
            icon.setColorFilter(color);
            title.setText(item.title);
            title.setTextColor(ContextCompat.getColor(
                    itemView.getContext(),
                    item.danger ? R.color.color_error : R.color.color_text_primary
            ));
            chevron.setVisibility(item.danger ? View.GONE : View.VISIBLE);
            itemView.setContentDescription(item.title);
            itemView.setOnClickListener(v -> listener.onProfileMenuClick(item));
        }
    }

    public static class ProfileMenuItem {
        public final String title;
        public final int iconRes;
        public final String action;
        public final boolean danger;

        public ProfileMenuItem(String title, int iconRes, String action, boolean danger) {
            this.title = title;
            this.iconRes = iconRes;
            this.action = action;
            this.danger = danger;
        }
    }
}
