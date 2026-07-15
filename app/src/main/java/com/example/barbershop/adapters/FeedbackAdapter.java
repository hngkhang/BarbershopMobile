package com.example.barbershop.adapters;

import com.example.barbershop.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barbershop.models.Feedback;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder> {
    private final List<Feedback> feedbacks = new ArrayList<>();

    public void submitList(List<Feedback> nextFeedbacks) {
        feedbacks.clear();
        feedbacks.addAll(nextFeedbacks);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FeedbackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback, parent, false);
        return new FeedbackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedbackViewHolder holder, int position) {
        holder.bind(feedbacks.get(position));
    }

    @Override
    public int getItemCount() {
        return feedbacks.size();
    }

    static class FeedbackViewHolder extends RecyclerView.ViewHolder {
        private final TextView textFeedbackUser;
        private final TextView textFeedbackRating;
        private final TextView textFeedbackContent;

        FeedbackViewHolder(@NonNull View itemView) {
            super(itemView);
            textFeedbackUser = itemView.findViewById(R.id.textFeedbackUser);
            textFeedbackRating = itemView.findViewById(R.id.textFeedbackRating);
            textFeedbackContent = itemView.findViewById(R.id.textFeedbackContent);
        }

        void bind(Feedback feedback) {
            String userLabel;
            if (feedback.hasCustomerName()) {
                userLabel = feedback.getCustomerName();
            } else if (feedback.getUserId().isEmpty()) {
                userLabel = itemView.getContext().getString(R.string.review_customer_unknown);
            } else {
                userLabel = itemView.getContext().getString(R.string.review_user_format, feedback.getUserId());
            }
            textFeedbackUser.setText(userLabel);
            textFeedbackRating.setText(feedback.hasCustomerRating()
                    ? itemView.getContext().getString(
                            R.string.review_customer_rating_format,
                            String.format(Locale.US, "%.1f", feedback.getCustomerRating())
                    )
                    : itemView.getContext().getString(R.string.review_customer_rating_empty));
            textFeedbackContent.setText(feedback.getContent());
        }
    }
}
