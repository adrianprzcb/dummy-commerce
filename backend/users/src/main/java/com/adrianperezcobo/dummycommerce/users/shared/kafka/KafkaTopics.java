package com.adrianperezcobo.dummycommerce.users.shared.kafka;

public final class KafkaTopics {
    public static final String DLT_SUFFIX = ".DLT";
    public static String deadLetterTopic(String original) { return original + DLT_SUFFIX; }
    private KafkaTopics() { }
}
