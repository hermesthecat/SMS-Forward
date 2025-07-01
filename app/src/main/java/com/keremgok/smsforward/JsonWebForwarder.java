package com.keremgok.smsforward;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public final class JsonWebForwarder extends AbstractWebForwarder {
    private static final String TAG = "JsonWebForwarder";

    public JsonWebForwarder(String endpoint) {
        super(endpoint);
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

            body.put("from", fromNumber);
            body.put("message", content);
            body.put("received_at", formattedDate);
            body.put("timestamp", timestamp);
        } catch (JSONException e) {
            Log.wtf(TAG, e);
            throw new RuntimeException(e);
        }
        return body.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void forward(String fromNumber, String content, long timestamp) throws Exception {
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