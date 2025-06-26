package com.keremgok.smsforward;

import android.telephony.SmsManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public final class SmsForwarder implements Forwarder {
    private final String forwardToNumber;

    public SmsForwarder(String forwardToNumber) {
        this.forwardToNumber = forwardToNumber;
    }

    public static void sendSmsTo(String number, String content) {
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(content);
        smsManager.sendMultipartTextMessage(number, null, parts, null, null);
    }

    @Override
    public void forward(String fromNumber, String content) {
        // Backward compatibility - use current time
        forward(fromNumber, content, System.currentTimeMillis());
    }

    @Override
    public void forward(String fromNumber, String content, long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String formattedDate = dateFormat.format(new Date(timestamp));
        
        String message = String.format("From %s:\n%s\nReceived at: %s", fromNumber, content, formattedDate);
        SmsForwarder.sendSmsTo(forwardToNumber, message);
    }
} 