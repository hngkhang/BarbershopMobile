package com.example.barbershop.adapters;

import com.example.barbershop.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_USER = 1;
    public static final int TYPE_AI = 2;
    public static final int TYPE_THINKING = 3;

    private final List<ChatMessage> messages = new ArrayList<>();
    private OnBookingReviewClickListener onBookingReviewClickListener;

    public interface OnBookingReviewClickListener {
        void onBookingReviewClick(long bookingDraftId);
    }

    public void setOnBookingReviewClickListener(
            OnBookingReviewClickListener onBookingReviewClickListener
    ) {
        this.onBookingReviewClickListener = onBookingReviewClickListener;
    }

    public void submitList(List<ChatMessage> nextMessages) {
        messages.clear();
        messages.addAll(nextMessages);
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void removeLastThinkingMessage() {
        for (int index = messages.size() - 1; index >= 0; index--) {
            if (messages.get(index).type == TYPE_THINKING) {
                messages.remove(index);
                notifyItemRemoved(index);
                return;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_user, parent, false);
            return new UserMessageViewHolder(view);
        }

        View view = inflater.inflate(R.layout.item_chat_ai, parent, false);
        return new AiMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof AiMessageViewHolder) {
            ((AiMessageViewHolder) holder).bind(message, onBookingReviewClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage;
        private final TextView textTime;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textChatMessage);
            textTime = itemView.findViewById(R.id.textChatTime);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.message);
            textTime.setText(message.time);
        }
    }

    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage;
        private final TextView textTime;
        private final LinearLayout layoutThinkingDots;
        private final View buttonReviewBooking;

        AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textChatMessage);
            textTime = itemView.findViewById(R.id.textChatTime);
            layoutThinkingDots = itemView.findViewById(R.id.layoutThinkingDots);
            buttonReviewBooking = itemView.findViewById(R.id.buttonReviewBooking);
        }

        void bind(ChatMessage message, OnBookingReviewClickListener listener) {
            boolean thinking = message.type == TYPE_THINKING;
            textMessage.setText(message.message);
            textTime.setText(message.time);
            layoutThinkingDots.setVisibility(thinking ? View.VISIBLE : View.GONE);
            boolean hasBookingReview = message.bookingDraftId > 0L && !thinking;
            buttonReviewBooking.setVisibility(hasBookingReview ? View.VISIBLE : View.GONE);
            buttonReviewBooking.setOnClickListener(hasBookingReview && listener != null
                    ? v -> listener.onBookingReviewClick(message.bookingDraftId)
                    : null);
        }
    }

    public static class ChatMessage {
        public final int type;
        public final String message;
        public final String time;
        public final long bookingDraftId;

        public ChatMessage(int type, String message, String time) {
            this(type, message, time, -1L);
        }

        public ChatMessage(int type, String message, String time, long bookingDraftId) {
            this.type = type;
            this.message = message;
            this.time = time;
            this.bookingDraftId = bookingDraftId;
        }
    }
}
