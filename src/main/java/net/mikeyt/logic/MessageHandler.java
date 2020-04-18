package net.mikeyt.logic;

import lombok.SneakyThrows;
import net.mikeyt.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageHandler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);

    private final Message message;
    private final ProcessShutdownState shutdownState;

    public MessageHandler(Message message, ProcessShutdownState shutdownState) {
        this.message = message;
        this.shutdownState = shutdownState;
    }

    @SneakyThrows
    public void processMessage() {
        log.info("start processing message " + message.toString());
        Thread.sleep(message.getMockProcessingMillis()); // Simulate processing time
        log.info("finished processing message " + message.toString());
    }

    @Override
    public void run() {
        if (shutdownState.isShutdown()) {
            log.info("MessageHandler received shutdown state before starting, abandoning processing task for message: " + message.getId());
            return;
        }
        processMessage();
    }
}
