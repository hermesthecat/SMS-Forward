package com.keremgok.smsforward;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public final class TelegramForwarder extends AbstractWebForwarder {
    private static final String TAG = "TelegramForwarder";

    private final String chatId;
    private final Context context;

    public TelegramForwarder(String token, String chatId) {
        super(new Uri.Builder()
                .scheme("https")
                .authority("api.telegram.org")
                .appendPath("bot" + token)
                .appendPath("sendMessage")
                .build()
                .toString());
        this.chatId = chatId;
        this.context = null; // For backward compatibility
    }

    public TelegramForwarder(String token, String chatId, Context context) {
        super(new Uri.Builder()
                .scheme("https")
                .authority("api.telegram.org")
                .appendPath("bot" + token)
                .appendPath("sendMessage")
                .build()
                .toString());
        this.chatId = chatId;
        this.context = context;
    }

    @Override
    protected byte[] makeBody(String fromNumber, String content) {
        return makeBody(fromNumber, content, System.currentTimeMillis());
    }

    protected byte[] makeBody(String fromNumber, String content, long timestamp) {
        JSONObject body = new JSONObject();
        try {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss",
                    java.util.Locale.getDefault());
            String formattedDate = dateFormat.format(new java.util.Date(timestamp));

            String message;
            if (context != null) {
                message = context.getString(R.string.telegram_message_format, fromNumber, content, formattedDate);
            } else {
                // Fallback for backward compatibility
                message = String.format("Message from %s:\n%s\nReceived at: %s", fromNumber, content, formattedDate);
            }

            body.put("chat_id", chatId);
            body.put("text", message);
        } catch (JSONException e) {
            Log.wtf(TAG, e);
            throw new RuntimeException(e);
        }
        return body.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void forward(String fromNumber, String content, long timestamp) throws Exception {
        // Override the parent method to use timestamp-aware body creation
        byte[] body = makeBody(fromNumber, content, timestamp);

        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) endpoint.openConnection();
        try {
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            connection.setDoInput(false);
            connection.setRequestProperty("Content-Type", getContentType());

            try (java.io.OutputStream out = connection.getOutputStream()) {
                out.write(body);
                out.flush();
            }

            int status = connection.getResponseCode();
            Log.d(TAG, String.format("response: status=%d", status));
        } finally {
            connection.disconnect();
        }
    }

    @Override
    protected String getContentType() {
        return "application/json";
    }
}