package com.keremgok.smsforward;

import android.content.Context;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public final class EmailForwarder implements Forwarder {
    private static final Pattern SENDER_NAME_PATTERN = Pattern.compile("^【(.+)】.*");

    private final InternetAddress fromAddress;
    private final InternetAddress[] toAddresses;
    private final Properties props;
    private final Authenticator authenticator;
    private final Context context;

    public EmailForwarder(InternetAddress fromAddress, InternetAddress[] toAddresses, String smtpHost, short port,
            String username, String password) {
        this.fromAddress = fromAddress;
        this.toAddresses = toAddresses;
        this.context = null; // For backward compatibility

        props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.connectiontimeout", "10000");
        props.setProperty("mail.smtp.timeout", "10000");
        props.setProperty("mail.smtp.writetimeout", "10000");
        props.setProperty("mail.smtp.allow8bitmime", "true");
        props.setProperty("mail.smtp.host", smtpHost);
        props.setProperty("mail.smtp.port", Short.toString(port));
        props.setProperty("mail.smtp.auth", "true");

        // https://www.oracle.com/docs/tech/java/sslnotes142.txt
        props.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        if (port == 465) {
            // Implicit TLS
            props.setProperty("mail.smtp.ssl.enable", "true");
        } else {
            props.setProperty("mail.smtp.starttls.enable", "true");
            props.setProperty("mail.smtp.starttls.required", "true");
        }

        PasswordAuthentication authentication = new PasswordAuthentication(username, password);
        authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return authentication;
            }
        };
    }

    public EmailForwarder(InternetAddress fromAddress, InternetAddress[] toAddresses, String smtpHost, short port,
            String username, String password, Context context) {
        this.fromAddress = fromAddress;
        this.toAddresses = toAddresses;
        this.context = context;

        props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.connectiontimeout", "10000");
        props.setProperty("mail.smtp.timeout", "10000");
        props.setProperty("mail.smtp.writetimeout", "10000");
        props.setProperty("mail.smtp.allow8bitmime", "true");
        props.setProperty("mail.smtp.host", smtpHost);
        props.setProperty("mail.smtp.port", Short.toString(port));
        props.setProperty("mail.smtp.auth", "true");

        // https://www.oracle.com/docs/tech/java/sslnotes142.txt
        props.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        if (port == 465) {
            // Implicit TLS
            props.setProperty("mail.smtp.ssl.enable", "true");
        } else {
            props.setProperty("mail.smtp.starttls.enable", "true");
            props.setProperty("mail.smtp.starttls.required", "true");
        }

        PasswordAuthentication authentication = new PasswordAuthentication(username, password);
        authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return authentication;
            }
        };
    }

    @Override
    public void forward(String fromNumber, String content) throws MessagingException {
        forward(fromNumber, content, System.currentTimeMillis());
    }

    @Override
    public void forward(String fromNumber, String content, long timestamp) throws MessagingException {
        Matcher senderNameMatcher = SENDER_NAME_PATTERN.matcher(content);
        String senderName = senderNameMatcher.matches() ? senderNameMatcher.group(1) : fromNumber;
        InternetAddress prettyFromAddress;
        try {
            prettyFromAddress = new InternetAddress(fromAddress.getAddress(), senderName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            prettyFromAddress = fromAddress;
        }

        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss",
                java.util.Locale.getDefault());
        String formattedDate = dateFormat.format(new java.util.Date(timestamp));

        String emailBody;
        String subject;
        if (context != null) {
            emailBody = context.getString(R.string.email_body_format, content, formattedDate);
            subject = context.getString(R.string.email_subject_format, fromNumber);
        } else {
            // Fallback for backward compatibility
            emailBody = content + "\n\nReceived at: " + formattedDate;
            subject = "SMS from: " + fromNumber;
        }

        Session session = Session.getInstance(props, authenticator);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(prettyFromAddress);
        message.addRecipients(Message.RecipientType.TO, toAddresses);
        message.setSubject(subject);
        message.setText(emailBody, "UTF-8");
        Transport.send(message);
    }
}