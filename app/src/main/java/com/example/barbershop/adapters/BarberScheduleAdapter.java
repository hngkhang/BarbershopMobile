package com.example.barbershop.adapters;

import com.example.barbershop.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barbershop.models.BarberSchedule;

import java.util.ArrayList;
import java.util.List;

public class BarberScheduleAdapter extends RecyclerView.Adapter<BarberScheduleAdapter.ScheduleViewHolder> {
    private final List<BarberSchedule> schedules = new ArrayList<>();

    public void submitList(List<BarberSchedule> nextSchedules) {
        schedules.clear();
        schedules.addAll(nextSchedules);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_barber_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        holder.bind(schedules.get(position));
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        private final TextView textScheduleDate;
        private final TextView textScheduleTime;

        ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            textScheduleDate = itemView.findViewById(R.id.textScheduleDate);
            textScheduleTime = itemView.findViewById(R.id.textScheduleTime);
        }

        void bind(BarberSchedule schedule) {
            textScheduleDate.setText(schedule.getDateLabel());
            textScheduleTime.setText(schedule.getTimeRangeLabel());
        }
    }
}
