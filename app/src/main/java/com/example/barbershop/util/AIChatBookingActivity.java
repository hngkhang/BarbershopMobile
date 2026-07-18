package com.example.barbershop.util;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barbershop.R;
import com.example.barbershop.adapters.ChatMessageAdapter;
import com.example.barbershop.data.AppointmentRepository;
import com.example.barbershop.data.BarberRepository;
import com.example.barbershop.data.ServiceRepository;
import com.example.barbershop.models.Appointment;
import com.example.barbershop.models.Barber;
import com.example.barbershop.models.BarberSchedule;
import com.example.barbershop.models.ShopService;
import com.example.barbershop.services.GeminiBookingService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Booking chat that prepares a real, available appointment for the normal confirmation flow. */
public class AIChatBookingActivity extends AppCompatActivity {

    private final List<ShopService> services = new ArrayList<>();
    private final List<Barber> barbers = new ArrayList<>();
    private final List<ConversationTurn> conversationHistory = new ArrayList<>();
    private final Map<Long, BookingDraft> bookingDrafts = new HashMap<>();

    private ChatMessageAdapter chatMessageAdapter;
    private RecyclerView recyclerChatMessages;
    private EditText editMessage;
    private ServiceRepository serviceRepository;
    private BarberRepository barberRepository;
    private AppointmentRepository appointmentRepository;
    private GeminiBookingService geminiBookingService;
    private String verifiedBookingContext = "";
    private long nextBookingDraftId = 1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat_booking);

        serviceRepository = new ServiceRepository();
        barberRepository = new BarberRepository();
        appointmentRepository = new AppointmentRepository();
        geminiBookingService = new GeminiBookingService(ContextCompat.getMainExecutor(this));

        setupChatList();
        setupActions();
        addGreeting();
        loadVerifiedBookingData();
    }

    private void setupChatList() {
        chatMessageAdapter = new ChatMessageAdapter();
        chatMessageAdapter.setOnBookingReviewClickListener(this::openBookingReview);
        recyclerChatMessages = findViewById(R.id.recyclerChatMessages);
        recyclerChatMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerChatMessages.setAdapter(chatMessageAdapter);
    }

    private void setupActions() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        editMessage = findViewById(R.id.editMessage);
        findViewById(R.id.buttonSend).setOnClickListener(v -> sendMessageFromInput());

        findViewById(R.id.chipPromptHaircut).setOnClickListener(v ->
                fillPrompt(R.string.ai_prompt_book_haircut)
        );
        findViewById(R.id.chipPromptTomorrow).setOnClickListener(v ->
                fillPrompt(R.string.ai_prompt_tomorrow)
        );
        findViewById(R.id.chipPromptBarber).setOnClickListener(v ->
                fillPrompt(R.string.ai_prompt_barber)
        );
    }

    private void addGreeting() {
        addAiMessage(getString(R.string.ai_message_greeting));
    }

    private void loadVerifiedBookingData() {
        serviceRepository.getAllServices(new BarberRepository.RepositoryCallback<List<ShopService>>() {
            @Override
            public void onSuccess(List<ShopService> loadedServices) {
                services.clear();
                services.addAll(loadedServices);
                loadBarbers();
            }

            @Override
            public void onError(Exception exception) {
                showDataLoadingError();
            }
        });
    }

    private void loadBarbers() {
        barberRepository.getAllBarbers(new BarberRepository.RepositoryCallback<List<Barber>>() {
            @Override
            public void onSuccess(List<Barber> loadedBarbers) {
                barbers.clear();
                barbers.addAll(loadedBarbers);
                verifiedBookingContext = buildVerifiedBookingContext();
            }

            @Override
            public void onError(Exception exception) {
                showDataLoadingError();
            }
        });
    }

    private String buildVerifiedBookingContext() {
        StringBuilder context = new StringBuilder("Dịch vụ hiện có:\n");
        for (ShopService service : services) {
            long serviceId = parseId(service.getServiceId());
            if (serviceId <= 0L) {
                continue;
            }
            context.append("- serviceId=").append(serviceId)
                    .append(", tên=").append(service.getName())
                    .append(", loại=").append(service.getCategory())
                    .append(", giá=").append(formatPrice(service.getPrice()))
                    .append(", thời lượng=").append(service.getTimeMinutes()).append(" phút\n");
        }
        context.append("Thợ đang hoạt động:\n");
        for (Barber barber : barbers) {
            long barberId = parseId(barber.getBarberId());
            if (barberId <= 0L) {
                continue;
            }
            context.append("- barberId=").append(barberId)
                    .append(", tên=").append(barber.getName())
                    .append(", kinh nghiệm=").append(barber.getExperience())
                    .append(", đánh giá=").append(barber.getDisplayRating()).append("\n");
        }
        return context.toString();
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
        if (verifiedBookingContext.isEmpty()) {
            Toast.makeText(this, R.string.ai_catalogue_loading, Toast.LENGTH_SHORT).show();
            return;
        }

        String previousConversation = buildConversationHistory();
        addUserMessage(message);
        editMessage.setText("");
        chatMessageAdapter.addMessage(new ChatMessageAdapter.ChatMessage(
                ChatMessageAdapter.TYPE_THINKING, getString(R.string.ai_thinking), ""
        ));
        scrollChatToBottom();

        geminiBookingService.requestBookingAdvice(
                message,
                verifiedBookingContext,
                previousConversation,
                new GeminiBookingService.Callback() {
                    @Override
                    public void onSuccess(GeminiBookingService.BookingAiReply reply) {
                        chatMessageAdapter.removeLastThinkingMessage();
                        if (reply.isBookingRequest()) {
                            prepareBookingDraft(reply);
                        } else {
                            addAiMessage(reply.getMessage());
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        chatMessageAdapter.removeLastThinkingMessage();
                        addAiMessage(errorMessage);
                    }
                }
        );
    }

    private void prepareBookingDraft(GeminiBookingService.BookingAiReply reply) {
        ShopService service = findService(reply.getServiceId());
        long requestedStartAt = parseRequestedStart(reply.getDate(), reply.getTime());
        if (service == null || requestedStartAt <= System.currentTimeMillis()) {
            addAiMessage(getString(R.string.ai_booking_details_missing));
            return;
        }

        List<Barber> candidateBarbers = getCandidateBarbers(reply.getBarberId());
        if (candidateBarbers.isEmpty()) {
            addAiMessage(getString(R.string.ai_booking_no_available));
            return;
        }

        addAiMessage(getString(R.string.ai_booking_checking));
        findAvailableBooking(candidateBarbers, 0, service, requestedStartAt);
    }

    private List<Barber> getCandidateBarbers(long requestedBarberId) {
        List<Barber> candidates = new ArrayList<>();
        if (requestedBarberId > 0L) {
            Barber requestedBarber = findBarber(requestedBarberId);
            if (requestedBarber != null) {
                candidates.add(requestedBarber);
            }
            return candidates;
        }
        candidates.addAll(barbers);
        return candidates;
    }

    private void findAvailableBooking(
            List<Barber> candidates,
            int candidateIndex,
            ShopService service,
            long requestedStartAt
    ) {
        if (candidateIndex >= candidates.size()) {
            addAiMessage(getString(R.string.ai_booking_no_available));
            return;
        }

        Barber candidate = candidates.get(candidateIndex);
        long barberId = parseId(candidate.getBarberId());
        if (barberId <= 0L) {
            findAvailableBooking(candidates, candidateIndex + 1, service, requestedStartAt);
            return;
        }

        barberRepository.getBarberSchedule(barberId,
                new BarberRepository.RepositoryCallback<List<BarberSchedule>>() {
                    @Override
                    public void onSuccess(List<BarberSchedule> schedules) {
                        appointmentRepository.getAppointmentsForBarber(barberId,
                                new AppointmentRepository.RepositoryCallback<List<Appointment>>() {
                                    @Override
                                    public void onSuccess(List<Appointment> appointments) {
                                        long endAt = requestedStartAt
                                                + service.getTimeMinutes() * 60_000L;
                                        if (isSlotAvailable(requestedStartAt, endAt, schedules, appointments)) {
                                            showBookingDraft(service, candidate, requestedStartAt, endAt);
                                        } else {
                                            findAvailableBooking(
                                                    candidates,
                                                    candidateIndex + 1,
                                                    service,
                                                    requestedStartAt
                                            );
                                        }
                                    }

                                    @Override
                                    public void onError(Exception exception) {
                                        showBookingAvailabilityError();
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(Exception exception) {
                        showBookingAvailabilityError();
                    }
                }
        );
    }

    private boolean isSlotAvailable(
            long requestedStartAt,
            long requestedEndAt,
            List<BarberSchedule> schedules,
            List<Appointment> appointments
    ) {
        boolean withinWorkingSchedule = false;
        for (BarberSchedule schedule : schedules) {
            if (schedule.getStartAt() == null || schedule.getEndAt() == null) {
                continue;
            }
            long scheduleStart = schedule.getStartAt().toDate().getTime();
            long scheduleEnd = schedule.getEndAt().toDate().getTime();
            if (requestedStartAt >= scheduleStart && requestedEndAt <= scheduleEnd) {
                withinWorkingSchedule = true;
                break;
            }
        }
        if (!withinWorkingSchedule) {
            return false;
        }

        for (Appointment appointment : appointments) {
            if (appointment.getStartAt() == null || appointment.getEndAt() == null) {
                continue;
            }
            long appointmentStart = appointment.getStartAt().toDate().getTime();
            long appointmentEnd = appointment.getEndAt().toDate().getTime();
            if (appointmentStart < requestedEndAt && appointmentEnd > requestedStartAt) {
                return false;
            }
        }
        return true;
    }

    private void showBookingDraft(ShopService service, Barber barber, long startAt, long endAt) {
        long barberId = parseId(barber.getBarberId());
        long serviceId = parseId(service.getServiceId());
        if (barberId <= 0L || serviceId <= 0L) {
            showBookingAvailabilityError();
            return;
        }

        long bookingDraftId = nextBookingDraftId++;
        bookingDrafts.put(bookingDraftId, new BookingDraft(
                serviceId,
                barberId,
                barber.getName(),
                barber.getExperience(),
                startAt,
                endAt
        ));
        addAiMessage(
                getString(
                        R.string.ai_booking_review_ready,
                        service.getName(),
                        barber.getName(),
                        formatBookingDateTime(startAt)
                ),
                bookingDraftId
        );
    }

    private void showBookingAvailabilityError() {
        addAiMessage(getString(R.string.ai_booking_load_failed));
    }

    private void openBookingReview(long bookingDraftId) {
        BookingDraft draft = bookingDrafts.get(bookingDraftId);
        if (draft == null) {
            Toast.makeText(this, R.string.ai_booking_draft_missing, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra(BookingActivity.EXTRA_SERVICE_ID, draft.serviceId);
        intent.putExtra(BookingActivity.EXTRA_BARBER_ID, draft.barberId);
        intent.putExtra(BookingActivity.EXTRA_BARBER_NAME, draft.barberName);
        intent.putExtra(BookingActivity.EXTRA_BARBER_EXPERIENCE, draft.barberExperience);
        intent.putExtra(BookingActivity.EXTRA_START_AT_MILLIS, draft.startAt);
        intent.putExtra(BookingActivity.EXTRA_END_AT_MILLIS, draft.endAt);
        intent.putExtra(BookingActivity.EXTRA_AI_BOOKING, true);
        startActivity(intent);
    }

    private void addUserMessage(String message) {
        chatMessageAdapter.addMessage(new ChatMessageAdapter.ChatMessage(
                ChatMessageAdapter.TYPE_USER, message, currentTime()
        ));
        conversationHistory.add(new ConversationTurn("Khách", message));
        scrollChatToBottom();
    }

    private void addAiMessage(String message) {
        addAiMessage(message, -1L);
    }

    private void addAiMessage(String message, long bookingDraftId) {
        chatMessageAdapter.addMessage(new ChatMessageAdapter.ChatMessage(
                ChatMessageAdapter.TYPE_AI, message, currentTime(), bookingDraftId
        ));
        conversationHistory.add(new ConversationTurn("Trợ lý", message));
        scrollChatToBottom();
    }

    private String buildConversationHistory() {
        int firstTurn = Math.max(0, conversationHistory.size() - 8);
        StringBuilder history = new StringBuilder();
        for (int index = firstTurn; index < conversationHistory.size(); index++) {
            ConversationTurn turn = conversationHistory.get(index);
            history.append(turn.role).append(": ").append(turn.message).append("\n");
        }
        return history.toString();
    }

    private ShopService findService(long serviceId) {
        for (ShopService service : services) {
            if (parseId(service.getServiceId()) == serviceId) {
                return service;
            }
        }
        return null;
    }

    private Barber findBarber(long barberId) {
        for (Barber barber : barbers) {
            if (parseId(barber.getBarberId()) == barberId) {
                return barber;
            }
        }
        return null;
    }

    private long parseRequestedStart(String date, String time) {
        if (date == null || time == null || date.trim().isEmpty() || time.trim().isEmpty()) {
            return -1L;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        formatter.setLenient(false);
        try {
            Date requestedStart = formatter.parse(date.trim() + " " + time.trim());
            return requestedStart == null ? -1L : requestedStart.getTime();
        } catch (ParseException exception) {
            return -1L;
        }
    }

    private long parseId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }

    private void showDataLoadingError() {
        Toast.makeText(this, R.string.state_error_placeholder, Toast.LENGTH_SHORT).show();
    }

    private String formatPrice(double value) {
        return String.format(Locale.US, "$%.2f", value);
    }

    private String formatBookingDateTime(long timeMillis) {
        return new SimpleDateFormat("HH:mm 'ngày' dd/MM/yyyy", Locale.US)
                .format(new Date(timeMillis));
    }

    private String currentTime() {
        return new SimpleDateFormat("h:mm a", Locale.US).format(new Date());
    }

    private void scrollChatToBottom() {
        recyclerChatMessages.post(() -> {
            int itemCount = chatMessageAdapter.getItemCount();
            if (itemCount > 0) {
                recyclerChatMessages.smoothScrollToPosition(itemCount - 1);
            }
        });
    }

    private static class ConversationTurn {
        final String role;
        final String message;

        ConversationTurn(String role, String message) {
            this.role = role;
            this.message = message;
        }
    }

    private static class BookingDraft {
        final long serviceId;
        final long barberId;
        final String barberName;
        final String barberExperience;
        final long startAt;
        final long endAt;

        BookingDraft(
                long serviceId,
                long barberId,
                String barberName,
                String barberExperience,
                long startAt,
                long endAt
        ) {
            this.serviceId = serviceId;
            this.barberId = barberId;
            this.barberName = barberName;
            this.barberExperience = barberExperience;
            this.startAt = startAt;
            this.endAt = endAt;
        }
    }
}
