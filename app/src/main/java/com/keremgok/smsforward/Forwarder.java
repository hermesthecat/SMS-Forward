package com.keremgok.smsforward;

public interface Forwarder {
    void forward(String fromNumber, String content) throws Exception;
    void forward(String fromNumber, String content, long timestamp) throws Exception;
} 