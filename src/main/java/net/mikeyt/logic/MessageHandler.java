package net.mikeyt.logic;

import lombok.SneakyThrows;
import net.mikeyt.model.Message;

import java.util.Random;

public class MessageHandler implements Runnable {
    private static final int MIN_PROCESSING_MILLIS = 50;
    private static final int MAX_PROCESSING_MILLIS = 5000;

    private final Message message;

    public MessageHandler(Message message) {
        this.message = message;
    }

    @SneakyThrows
    public void processMessage() {
        long millis = new Random().nextInt((MAX_PROCESSING_MILLIS - MIN_PROCESSING_MILLIS) + MIN_PROCESSING_MILLIS);
        System.out.println(String.format("start processing message (%s milliseconds) %s", millis, message.toString()));
        Thread.sleep(millis); // Simulate processing time
        System.out.println(String.format("finished processing message (%s milliseconds) %s", millis, message.toString()));
    }

    @Override
    public void run() {
        processMessage();
    }
}
