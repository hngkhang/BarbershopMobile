package com.example.barbershop;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AIChatBookingActivity extends AppCompatActivity {

    private static final String TEMP_DATE_LABEL = "Tomorrow, May 25, 2025";
    private static final int TEMP_DURATION_MINUTES = 45;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ChatMessageAdapter chatMessageAdapter;
    private ServiceSuggestionAdapter serviceSuggestionAdapter;
    private BarberSuggestionAdapter barberSuggestionAdapter;
    private EditText editMessage;
    private ServiceSuggestionAdapter.ServiceSuggestion selectedService;
    private BarberSuggestionAdapter.BarberSuggestion selectedBarber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat_booking);

        setupChatList();
        setupSuggestionLists();
        setupActions();
        bindTemporaryUiData();
    }

    private void setupChatList() {
        chatMessageAdapter = new ChatMessageAdapter();
        RecyclerView recyclerChatMessages = findViewById(R.id.recyclerChatMessages);
        recyclerChatMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerChatMessages.setAdapter(chatMessageAdapter);
    }

    private void setupSuggestionLists() {
        serviceSuggestionAdapter = new ServiceSuggestionAdapter(serviceSuggestion -> {
            selectedService = serviceSuggestion;
            serviceSuggestionAdapter.setSelectedSuggestion(serviceSuggestion);
        });

        RecyclerView recyclerServiceSuggestions = findViewById(R.id.recyclerServiceSuggestions);
        recyclerServiceSuggestions.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        recyclerServiceSuggestions.setAdapter(serviceSuggestionAdapter);

        barberSuggestionAdapter = new BarberSuggestionAdapter(barberSuggestion -> {
            selectedBarber = barberSuggestion;
            barberSuggestionAdapter.setSelectedSuggestion(barberSuggestion);
        });

        RecyclerView recyclerBarberSuggestions = findViewById(R.id.recyclerBarberSuggestions);
        recyclerBarberSuggestions.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        recyclerBarberSuggestions.setAdapter(barberSuggestionAdapter);
    }

    private void setupActions() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonMore).setOnClickListener(v ->
                Toast.makeText(this, R.string.ai_chat_disclaimer, Toast.LENGTH_SHORT).show()
        );

        editMessage = findViewById(R.id.editMessage);
        findViewById(R.id.buttonSend).setOnClickListener(v -> sendMessageFromInput());
        editMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessageFromInput();
                return true;
            }
            return false;
        });

        findViewById(R.id.chipPromptHaircut).setOnClickListener(v -> fillPrompt(R.string.ai_prompt_book_haircut));
        findViewById(R.id.chipPromptTomorrow).setOnClickListener(v -> fillPrompt(R.string.ai_prompt_tomorrow));
        findViewById(R.id.chipPromptBarber).setOnClickListener(v -> fillPrompt(R.string.ai_prompt_barber));

        findViewById(R.id.chipTime1200).setOnClickListener(v -> openBookingConfirmation(getString(R.string.time_slot_1200)));
        findViewById(R.id.chipTime1300).setOnClickListener(v -> openBookingConfirmation(getString(R.string.time_slot_1300)));
        findViewById(R.id.chipTime1400).setOnClickListener(v -> openBookingConfirmation(getString(R.string.time_slot_1400)));
        findViewById(R.id.chipTime1500).setOnClickListener(v -> openBookingConfirmation(getString(R.string.time_slot_1500)));
        findViewById(R.id.chipTime1600).setOnClickListener(v -> openBookingConfirmation(getString(R.string.time_slot_1600)));
    }

    private void bindTemporaryUiData() {
        List<ChatMessageAdapter.ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessageAdapter.ChatMessage(
                ChatMessageAdapter.TYPE_USER,
                getString(R.string.ai_message_user_preview),
                "9:41 AM"
        ));
        messages.add(new ChatMessageAdapter.ChatMessage(
                ChatMessageAdapter.TYPE_AI,
                getString(R.string.ai_message_greeting),
                "9:41 AM"
        ));
        messages.add(new ChatMessageAdapter.ChatMessage(
                ChatMessageAdapter.TYPE_THINKING,
                getString(R.string.ai_thinking),
                ""
        ));
        messages.add(new ChatMessageAdapter.ChatMessage(
                ChatMessageAdapter.TYPE_AI,
                getString(R.string.ai_message_suggestion_intro),
                "9:42 AM"
        ));
        chatMessageAdapter.submitList(messages);

        List<ServiceSuggestionAdapter.ServiceSuggestion> serviceSuggestions = createTemporaryServices();
        selectedService = serviceSuggestions.get(0);
        serviceSuggestionAdapter.submitList(serviceSuggestions);

        List<BarberSuggestionAdapter.BarberSuggestion> barberSuggestions = createTemporaryBarbers();
        selectedBarber = barberSuggestions.get(0);
        barberSuggestionAdapter.submitList(barberSuggestions);

        scrollChatToBottom();
    }

    private List<ServiceSuggestionAdapter.ServiceSuggestion> createTemporaryServices() {
        List<ServiceSuggestionAdapter.ServiceSuggestion> services = new ArrayList<>();
        services.add(new ServiceSuggestionAdapter.ServiceSuggestion(
                "Haircut",
                "Classic Cut",
                "45 min",
                "$25.00",
                25.0,
                R.drawable.bg_service_tile_haircut,
                R.drawable.ic_scissors
        ));
        services.add(new ServiceSuggestionAdapter.ServiceSuggestion(
                "Haircut",
                "Modern Cut",
                "50 min",
                "$30.00",
                30.0,
                R.drawable.bg_service_tile_combo,
                R.drawable.ic_scissors
        ));
        services.add(new ServiceSuggestionAdapter.ServiceSuggestion(
                "Haircut",
                "Fade Cut",
                "45 min",
                "$28.00",
                28.0,
                R.drawable.bg_service_tile_coloring,
                R.drawable.ic_scissors
        ));
        return services;
    }

    private List<BarberSuggestionAdapter.BarberSuggestion> createTemporaryBarbers() {
        List<BarberSuggestionAdapter.BarberSuggestion> barbers = new ArrayList<>();
        barbers.add(new BarberSuggestionAdapter.BarberSuggestion("Michael", "M", "9+ yrs exp.", "4.9", "Classic Cut"));
        barbers.add(new BarberSuggestionAdapter.BarberSuggestion("David", "D", "6+ yrs exp.", "4.8", "Modern Cut"));
        barbers.add(new BarberSuggestionAdapter.BarberSuggestion("James", "J", "7+ yrs exp.", "4.8", "Fade Cut"));
        return barbers;
    }

    private void fillPrompt(int stringResId) {
        editMessage.setText(getString(stringResId));
        editMessage.setSelection(editMessage.length());
    }

    private void sendMessageFromInput() {
        String message = editMessage.getText().toString().trim();
        if (message.isEmpty()) {
            return;
        }

        chatMessageAdapter.addMessage(new ChatMessageAdapter.ChatMessage(
                ChatMessageAdapter.TYPE_USER,
                message,
                "Now"
        ));
        editMessage.setText("");
        showTemporaryAiThinkingState();
    }

    private void showTemporaryAiThinkingState() {
        // TODO: Replace this placeholder response with AIBookingService request/stream handling.
        chatMessageAdapter.addMessage(new ChatMessageAdapter.ChatMessage(
                ChatMessageAdapter.TYPE_THINKING,
                getString(R.string.ai_thinking),
                ""
        ));
        scrollChatToBottom();

        handler.postDelayed(() -> {
            chatMessageAdapter.removeLastThinkingMessage();
            chatMessageAdapter.addMessage(new ChatMessageAdapter.ChatMessage(
                    ChatMessageAdapter.TYPE_AI,
                    getString(R.string.ai_message_suggestion_intro),
                    "Now"
            ));
            scrollChatToBottom();
        }, 900);
    }

    private void openBookingConfirmation(String selectedTime) {
        // TODO: Use AIBookingService to create a pending booking proposal, then pass that proposal to confirmation.
        Intent intent = new Intent(this, BookingConfirmActivity.class);
        intent.putExtra("selectedServiceName", selectedService.name);
        intent.putExtra("selectedServicePrice", selectedService.price);
        intent.putExtra("addonServiceName", selectedService.detail);
        intent.putExtra("addonServicePrice", "$0.00");
        intent.putExtra("selectedBarberName", selectedBarber.name);
        intent.putExtra("selectedDateLabel", TEMP_DATE_LABEL);
        intent.putExtra("selectedStartTime", selectedTime);
        intent.putExtra("selectedEndTime", estimateEndTime(selectedTime));
        intent.putExtra("bookingNote", getString(R.string.ai_open_confirmation));
        intent.putExtra("totalDurationMinutes", TEMP_DURATION_MINUTES);
        intent.putExtra("totalPrice", selectedService.totalPrice);
        startActivity(intent);
    }

    private String estimateEndTime(String selectedTime) {
        if (selectedTime.equals(getString(R.string.time_slot_1200))) {
            return "12:45 PM";
        } else if (selectedTime.equals(getString(R.string.time_slot_1300))) {
            return "1:45 PM";
        } else if (selectedTime.equals(getString(R.string.time_slot_1400))) {
            return "2:45 PM";
        } else if (selectedTime.equals(getString(R.string.time_slot_1500))) {
            return "3:45 PM";
        } else if (selectedTime.equals(getString(R.string.time_slot_1600))) {
            return "4:45 PM";
        }
        return String.format(Locale.US, "%s + %d min", selectedTime, TEMP_DURATION_MINUTES);
    }

    private void scrollChatToBottom() {
        findViewById(R.id.aiChatScroll).post(() -> findViewById(R.id.aiChatScroll).scrollTo(0, Integer.MAX_VALUE));
    }
}
