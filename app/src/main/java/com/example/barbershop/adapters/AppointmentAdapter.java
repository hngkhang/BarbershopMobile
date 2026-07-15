package com.example.barbershop.adapters;

import com.example.barbershop.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    public interface OnAppointmentClickListener {
        void onViewDetailsClick(AppointmentItem appointmentItem);
    }

    private final List<AppointmentItem> appointments = new ArrayList<>();
    private final OnAppointmentClickListener listener;

    public AppointmentAdapter(OnAppointmentClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AppointmentItem> nextAppointments) {
        appointments.clear();
        appointments.addAll(nextAppointments);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        holder.bind(appointments.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        private final TextView textDay;
        private final TextView textDate;
        private final TextView textTime;
        private final TextView textInitial;
        private final TextView textBarberName;
        private final TextView textService;
        private final TextView textPrice;
        private final TextView textStatus;
        private final TextView textPaymentStatus;
        private final AppCompatButton buttonDetails;

        AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            textDay = itemView.findViewById(R.id.textAppointmentDay);
            textDate = itemView.findViewById(R.id.textAppointmentDate);
            textTime = itemView.findViewById(R.id.textAppointmentTime);
            textInitial = itemView.findViewById(R.id.textAppointmentBarberInitial);
            textBarberName = itemView.findViewById(R.id.textAppointmentBarberName);
            textService = itemView.findViewById(R.id.textAppointmentService);
            textPrice = itemView.findViewById(R.id.textAppointmentPrice);
            textStatus = itemView.findViewById(R.id.textAppointmentStatus);
            textPaymentStatus = itemView.findViewById(R.id.textAppointmentPaymentStatus);
            buttonDetails = itemView.findViewById(R.id.buttonAppointmentDetails);
        }

        void bind(AppointmentItem appointmentItem, OnAppointmentClickListener listener) {
            textDay.setText(appointmentItem.day);
            textDate.setText(appointmentItem.date);
            textTime.setText(appointmentItem.time);
            textInitial.setText(appointmentItem.barberInitial);
            textInitial.setContentDescription(itemView.getContext().getString(
                    R.string.barber_avatar_content_description,
                    appointmentItem.barberName
            ));
            textBarberName.setText(appointmentItem.barberName);
            textService.setText(appointmentItem.serviceName);
            textPrice.setText(appointmentItem.price);
            textStatus.setText(appointmentItem.status);
            textStatus.setBackgroundResource(statusBackground(appointmentItem.status));
            textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), statusColor(appointmentItem.status)));
            textPaymentStatus.setText(appointmentItem.paymentStatus);
            textPaymentStatus.setTextColor(ContextCompat.getColor(
                    itemView.getContext(),
                    paymentColor(appointmentItem.paymentStatus)
            ));
            buttonDetails.setOnClickListener(v -> listener.onViewDetailsClick(appointmentItem));
        }

        private int statusBackground(String status) {
            if (AppointmentItem.STATUS_PENDING.equals(status)) {
                return R.drawable.bg_badge_status_pending;
            } else if (AppointmentItem.STATUS_COMPLETED.equals(status)) {
                return R.drawable.bg_badge_status_completed;
            } else if (AppointmentItem.STATUS_CANCELLED.equals(status)) {
                return R.drawable.bg_badge_status_cancelled;
            }
            return R.drawable.bg_badge_status_upcoming;
        }

        private int statusColor(String status) {
            if (AppointmentItem.STATUS_PENDING.equals(status)) {
                return R.color.color_warning;
            } else if (AppointmentItem.STATUS_COMPLETED.equals(status)) {
                return R.color.color_neutral;
            } else if (AppointmentItem.STATUS_CANCELLED.equals(status)) {
                return R.color.color_error;
            }
            return R.color.color_success;
        }

        private int paymentColor(String paymentStatus) {
            if ("Not Paid".equals(paymentStatus)) {
                return R.color.color_payment_unpaid;
            } else if ("Refunded".equals(paymentStatus)) {
                return R.color.color_error;
            }
            return R.color.color_payment_paid;
        }
    }

    public static class AppointmentItem {
        public static final String STATUS_UPCOMING = "Upcoming";
        public static final String STATUS_PENDING = "Pending";
        public static final String STATUS_COMPLETED = "Completed";
        public static final String STATUS_CANCELLED = "Cancelled";

        public final String id;
        public final String day;
        public final String date;
        public final String fullDate;
        public final String time;
        public final String endTime;
        public final String duration;
        public final String barberName;
        public final String barberInitial;
        public final String barberExperience;
        public final String barberSpecialty;
        public final String serviceName;
        public final String price;
        public final String status;
        public final String paymentStatus;
        public final String paymentMethod;
        public final String note;

        public AppointmentItem(
                String id,
                String day,
                String date,
                String fullDate,
                String time,
                String endTime,
                String duration,
                String barberName,
                String barberExperience,
                String barberSpecialty,
                String serviceName,
                String price,
                String status,
                String paymentStatus,
                String paymentMethod,
                String note
        ) {
            this.id = id;
            this.day = day;
            this.date = date;
            this.fullDate = fullDate;
            this.time = time;
            this.endTime = endTime;
            this.duration = duration;
            this.barberName = barberName;
            this.barberInitial = barberName == null || barberName.trim().isEmpty()
                    ? "A"
                    : barberName.trim().substring(0, 1).toUpperCase();
            this.barberExperience = barberExperience;
            this.barberSpecialty = barberSpecialty;
            this.serviceName = serviceName;
            this.price = price;
            this.status = status;
            this.paymentStatus = paymentStatus;
            this.paymentMethod = paymentMethod;
            this.note = note;
        }
    }
}
