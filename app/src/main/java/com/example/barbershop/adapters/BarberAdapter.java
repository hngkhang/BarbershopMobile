package com.example.barbershop.adapters;

import com.example.barbershop.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BarberAdapter
        extends RecyclerView.Adapter<BarberAdapter.BarberViewHolder> {

    public interface OnBarberClickListener {
        void onBarberActionClick(BarberItem barberItem);
    }

    private final List<BarberItem> barbers =
            new ArrayList<>();

    private final OnBarberClickListener listener;

    public BarberAdapter(
            OnBarberClickListener listener
    ) {
        this.listener = listener;
    }

    /**
     * Cập nhật danh sách barber hiển thị.
     */
    public void submitList(
            List<BarberItem> nextBarbers
    ) {
        barbers.clear();

        if (nextBarbers != null) {
            barbers.addAll(nextBarbers);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BarberViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_barber,
                        parent,
                        false
                );

        return new BarberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull BarberViewHolder holder,
            int position
    ) {
        BarberItem barberItem =
                barbers.get(position);

        holder.bind(
                barberItem,
                listener
        );
    }

    @Override
    public int getItemCount() {
        return barbers.size();
    }

    static class BarberViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView textBarberInitial;
        private final TextView textBarberName;
        private final TextView textBarberExperience;
        private final TextView textBarberSpecialty;
        private final TextView textBarberRating;
        private final TextView textBarberStatus;
        private final TextView textBarberAvailableTime;

        private final AppCompatButton
                buttonBarberAction;

        BarberViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            textBarberInitial =
                    itemView.findViewById(
                            R.id.textBarberInitial
                    );

            textBarberName =
                    itemView.findViewById(
                            R.id.textBarberName
                    );

            textBarberExperience =
                    itemView.findViewById(
                            R.id.textBarberExperience
                    );

            textBarberSpecialty =
                    itemView.findViewById(
                            R.id.textBarberSpecialty
                    );

            textBarberRating =
                    itemView.findViewById(
                            R.id.textBarberRating
                    );

            textBarberStatus =
                    itemView.findViewById(
                            R.id.textBarberStatus
                    );

            textBarberAvailableTime =
                    itemView.findViewById(
                            R.id.textBarberAvailableTime
                    );

            buttonBarberAction =
                    itemView.findViewById(
                            R.id.buttonBarberAction
                    );
        }

        void bind(
                BarberItem barberItem,
                OnBarberClickListener listener
        ) {
            bindBasicInformation(barberItem);
            bindRating(barberItem);
            bindStatus(barberItem);
            bindActions(barberItem, listener);
        }

        /**
         * Hiển thị thông tin cơ bản của barber.
         */
        private void bindBasicInformation(
                BarberItem barberItem
        ) {
            String initial =
                    getInitial(barberItem.name);

            textBarberInitial.setText(initial);

            textBarberInitial.setContentDescription(
                    itemView.getContext().getString(
                            R.string.barber_avatar_content_description,
                            barberItem.name
                    )
            );

            textBarberName.setText(
                    barberItem.name
            );

            textBarberExperience.setText(
                    barberItem.experience
            );

            /*
             * Collection barbers hiện không có
             * trường specialty.
             */
            textBarberSpecialty.setVisibility(
                    View.GONE
            );

            /*
             * Collection barbers hiện không có
             * trường availableTime hoặc schedule.
             */
            textBarberAvailableTime.setVisibility(
                    View.GONE
            );
        }

        /**
         * Hiển thị rating trung bình.
         */
        private void bindRating(
                BarberItem barberItem
        ) {
            if (barberItem.ratingCount <= 0) {
                textBarberRating.setText(
                        "No ratings yet"
                );

                return;
            }

            String ratingText =
                    String.format(
                            Locale.US,
                            "%.1f (%d)",
                            barberItem.averageRating,
                            barberItem.ratingCount
                    );

            textBarberRating.setText(
                    ratingText
            );
        }

        /**
         * Trạng thái được suy ra trực tiếp
         * từ field active trong Firestore.
         */
        private void bindStatus(
                BarberItem barberItem
        ) {
            if (barberItem.active) {
                textBarberStatus.setVisibility(
                        View.VISIBLE
                );

                textBarberStatus.setText(
                        R.string.barber_status_available
                );
            } else {
                /*
                 * BarberListActivity thông thường chỉ
                 * load barber có active = true.
                 *
                 * Nếu vẫn nhận được barber inactive
                 * thì không hiển thị trạng thái Available.
                 */
                textBarberStatus.setVisibility(
                        View.GONE
                );
            }
        }

        /**
         * Bấm nút hoặc bấm card đều mở
         * trang Barber Profile.
         */
        private void bindActions(
                BarberItem barberItem,
                OnBarberClickListener listener
        ) {
            buttonBarberAction.setText(
                    R.string.action_view_profile
            );

            buttonBarberAction.setEnabled(
                    barberItem.active
            );

            buttonBarberAction.setOnClickListener(
                    view -> {
                        if (
                                listener != null
                                        && barberItem.active
                        ) {
                            listener.onBarberActionClick(
                                    barberItem
                            );
                        }
                    }
            );

            itemView.setOnClickListener(
                    view -> {
                        if (
                                listener != null
                                        && barberItem.active
                        ) {
                            listener.onBarberActionClick(
                                    barberItem
                            );
                        }
                    }
            );
        }

        /**
         * Lấy ký tự đầu của tên barber
         * để hiển thị avatar dạng chữ.
         */
        private static String getInitial(
                String name
        ) {
            if (
                    name == null
                            || name.trim().isEmpty()
            ) {
                return "B";
            }

            return name.trim()
                    .substring(0, 1)
                    .toUpperCase(Locale.US);
        }
    }

    public static class BarberItem {

        /*
         * Dữ liệu lấy từ document barbers.
         */
        public final long barberId;
        public final String name;
        public final String experience;
        public final String avatarUrl;
        public final boolean active;

        /*
         * Dữ liệu được tính từ collection rating,
         * không phải field của document barber.
         */
        public final double averageRating;
        public final long ratingCount;

        public BarberItem(
                long barberId,
                String name,
                String experience,
                String avatarUrl,
                boolean active,
                double averageRating,
                long ratingCount
        ) {
            this.barberId = barberId;
            this.name = name;
            this.experience = experience;
            this.avatarUrl = avatarUrl;
            this.active = active;
            this.averageRating = averageRating;
            this.ratingCount = ratingCount;
        }
    }
}