package com.example.barbershop.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.Schema;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

/** Calls Gemini through Firebase AI Logic for chat and booking-intent extraction. */
public class GeminiBookingService {

    private static final String TAG = "GeminiBooking";
    private static final int MAX_TRANSIENT_RETRIES = 2;
    private static final long RETRY_BASE_DELAY_MS = 1_000L;
    private static final String INTENT_BOOKING = "BOOKING";
    private static final String INTENT_CHAT = "CHAT";

    public interface Callback {
        void onSuccess(BookingAiReply reply);

        void onError(String message);
    }

    public static class BookingAiReply {
        private final String message;
        private final String intent;
        private final long serviceId;
        private final long barberId;
        private final String date;
        private final String time;

        public BookingAiReply(
                String message,
                String intent,
                long serviceId,
                long barberId,
                String date,
                String time
        ) {
            this.message = message;
            this.intent = intent;
            this.serviceId = serviceId;
            this.barberId = barberId;
            this.date = date;
            this.time = time;
        }

        public String getMessage() {
            return message;
        }

        public long getServiceId() {
            return serviceId;
        }

        public long getBarberId() {
            return barberId;
        }

        public String getDate() {
            return date;
        }

        public String getTime() {
            return time;
        }

        public boolean isBookingRequest() {
            return INTENT_BOOKING.equalsIgnoreCase(intent);
        }
    }

    private final GenerativeModelFutures model;
    private final Executor callbackExecutor;

    public GeminiBookingService(Executor callbackExecutor) {
        this.callbackExecutor = callbackExecutor;

        Schema responseSchema = Schema.obj(
                Map.of(
                        "message", Schema.str(),
                        "intent", Schema.str(),
                        "serviceId", Schema.numInt(),
                        "barberId", Schema.numInt(),
                        "date", Schema.str(),
                        "time", Schema.str()
                ),
                List.of("message", "intent", "serviceId", "barberId", "date", "time")
        );

        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.responseMimeType = "application/json";
        configBuilder.responseSchema = responseSchema;

        GenerativeModel generativeModel = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel("gemini-3.1-flash-lite", configBuilder.build());
        model = GenerativeModelFutures.from(generativeModel);
    }

    public void requestBookingAdvice(
            String userMessage,
            String verifiedBookingContext,
            String conversationHistory,
            Callback callback
    ) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String prompt = "Bạn là trợ lý đặt lịch của Art Barbershop. Luôn trả lời tiếng Việt, "
                + "ngắn gọn và thân thiện. Ngày hiện tại là " + today + ". "
                + "Bạn chỉ được dùng serviceId và barberId trong DỮ LIỆU ĐÃ XÁC THỰC. "
                + "Nếu khách yêu cầu đặt lịch và đã có đủ dịch vụ, ngày và giờ, đặt intent là BOOKING. "
                + "Hãy đổi ngày tương đối như 'ngày mai' thành yyyy-MM-dd và giờ thành HH:mm. "
                + "Nếu khách không chọn thợ hoặc nói thợ nào cũng được, barberId là -1. "
                + "Không bịa dịch vụ, thợ, giá hay lịch trống; ứng dụng sẽ tự kiểm tra lịch trống. "
                + "Nếu thiếu thông tin để đặt lịch, đặt intent là CHAT, serviceId và barberId là -1, "
                + "date và time là chuỗi rỗng, rồi hỏi ngắn gọn thông tin còn thiếu. "
                + "Chỉ trả về JSON hợp lệ với đủ các trường: message, intent, serviceId, barberId, date, time. "
                + "intent chỉ được là BOOKING hoặc CHAT.\n\n"
                + "DỮ LIỆU ĐÃ XÁC THỰC:\n" + verifiedBookingContext
                + "\n\nLỊCH SỬ HỘI THOẠI:\n" + conversationHistory
                + "\nTIN NHẮN MỚI CỦA KHÁCH:\n" + userMessage;

        Content content = new Content.Builder().addText(prompt).build();
        generateContentWithRetry(content, callback, 0);
    }

    private void generateContentWithRetry(
            Content content,
            Callback callback,
            int retryAttempt
    ) {
        ListenableFuture<GenerateContentResponse> future = model.generateContent(content);

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse response) {
                try {
                    String responseText = response == null ? null : response.getText();
                    if (responseText == null || responseText.trim().isEmpty()) {
                        callback.onError("AI chưa có câu trả lời. Vui lòng thử lại.");
                        return;
                    }
                    JSONObject json = new JSONObject(responseText);
                    callback.onSuccess(new BookingAiReply(
                            json.optString("message", "Tôi chưa hiểu rõ yêu cầu của bạn."),
                            json.optString("intent", INTENT_CHAT),
                            json.optLong("serviceId", -1L),
                            json.optLong("barberId", -1L),
                            json.optString("date", ""),
                            json.optString("time", "")
                    ));
                } catch (JSONException exception) {
                    Log.e(TAG, "Gemini returned invalid JSON", exception);
                    callback.onError("AI trả về dữ liệu chưa hợp lệ. Vui lòng thử lại.");
                }
            }

            @Override
            public void onFailure(@NonNull Throwable throwable) {
                if (isTransientError(throwable) && retryAttempt < MAX_TRANSIENT_RETRIES) {
                    long delayMs = RETRY_BASE_DELAY_MS * (1L << retryAttempt)
                            + ThreadLocalRandom.current().nextLong(250L, 751L);
                    Log.w(TAG, "Temporary Gemini failure. Retrying "
                            + (retryAttempt + 1) + "/" + MAX_TRANSIENT_RETRIES
                            + " in " + delayMs + " ms.", throwable);
                    new Handler(Looper.getMainLooper()).postDelayed(
                            () -> generateContentWithRetry(content, callback, retryAttempt + 1),
                            delayMs
                    );
                    return;
                }
                Log.e(TAG, "Gemini generateContent failed", throwable);
                callback.onError(getUserFriendlyError(throwable));
            }
        }, callbackExecutor);
    }

    private boolean isTransientError(Throwable throwable) {
        String error = getErrorMessage(throwable);
        return error.contains("high demand")
                || error.contains("temporarily overloaded")
                || error.contains("temporarily unavailable")
                || error.contains("503")
                || error.contains("timeout")
                || error.contains("server exception");
    }

    private String getUserFriendlyError(Throwable throwable) {
        String error = getErrorMessage(throwable);
        if (error.contains("app check") || error.contains("appcheck")
                || error.contains("attestation")) {
            return "AI chưa được xác thực App Check. Vui lòng kiểm tra cấu hình Firebase.";
        }
        if (error.contains("429") || error.contains("quota")
                || error.contains("resource exhausted")) {
            return "AI đang quá tải hoặc đã đạt giới hạn sử dụng. Vui lòng thử lại sau.";
        }
        if (error.contains("high demand") || error.contains("temporarily overloaded")
                || error.contains("temporarily unavailable") || error.contains("503")) {
            return "AI đang quá tải. Vui lòng thử lại sau ít phút.";
        }
        if (error.contains("401") || error.contains("403")
                || error.contains("permission denied") || error.contains("unauthenticated")) {
            return "Firebase từ chối yêu cầu AI. Vui lòng kiểm tra AI Logic và App Check.";
        }
        if (error.contains("404") || error.contains("api has not been used")
                || error.contains("api is not enabled") || error.contains("not found")) {
            return "Dịch vụ Gemini AI chưa sẵn sàng. Vui lòng kiểm tra Firebase AI Logic.";
        }
        if (error.contains("unknownhost") || error.contains("network")
                || error.contains("timeout") || error.contains("unable to resolve host")) {
            return "Không thể kết nối Internet tới AI. Vui lòng kiểm tra mạng và thử lại.";
        }
        return "Không thể kết nối AI. Vui lòng thử lại sau.";
    }

    private String getErrorMessage(Throwable throwable) {
        return throwable.getMessage() == null
                ? ""
                : throwable.getMessage().toLowerCase(Locale.US);
    }
}
