package com.keremgok.smsforward;

import android.content.Context;
import android.telephony.SmsManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public final class SmsForwarder implements Forwarder {
    private final String forwardToNumber;
    private final Context context;

    public SmsForwarder(String forwardToNumber) {
        this.forwardToNumber = forwardToNumber;
        this.context = null; // For backward compatibility
    }

    public SmsForwarder(String forwardToNumber, Context context) {
        this.forwardToNumber = forwardToNumber;
        this.context = context;
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

        String message;
        if (context != null) {
            message = context.getString(R.string.sms_message_format, fromNumber, content, formattedDate);
        } else {
            // Fallback for backward compatibility
            message = String.format("From %s:\n%s\nReceived at: %s", fromNumber, content, formattedDate);
        }

        SmsForwarder.sendSmsTo(forwardToNumber, message);
    }
}