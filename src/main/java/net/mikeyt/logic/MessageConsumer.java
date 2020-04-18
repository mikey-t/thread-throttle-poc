package net.mikeyt.logic;

import net.mikeyt.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MessageConsumer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);
    private static final int NUM_CONCURRENT_HANDLERS = 20;
    private static final int TERMINATION_MAX_SECONDS = 10;
    private static final int QUEUE_POLLING_TIMEOUT_SECONDS = 3;

    private final BlockingQueue<Message> messageQueue;
    private final ProcessShutdownState shutdownState;
    private final ExecutorService executor;

    public MessageConsumer(BlockingQueue<Message> messageQueue, ProcessShutdownState shutdownState) {
        this.messageQueue = messageQueue;
        this.shutdownState = shutdownState;
        executor = Executors.newFixedThreadPool(NUM_CONCURRENT_HANDLERS);
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (shutdownState.isShutdown()) {
                    log.info("MessageConsumer shutting down, attempting to shutdown handlers and exiting MessageConsumer loop");
                    shutdown();
                    return;
                }

                // Polling is used so processes have an opportunity to gracefully shutdown
                Message message = (Message) messageQueue.poll(QUEUE_POLLING_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (shutdownState.isShutdown()) {
                    continue;
                }

                if (message != null) {
                    executor.submit(new MessageHandler(message));
                }
            }
        } catch (InterruptedException ex) {
            log.info("MessageConsumer thread interrupted");
        }
    }

    private void shutdown() {
        log.warn("MessageConsumer attempting to shutdown handlers");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(TERMINATION_MAX_SECONDS, TimeUnit.SECONDS)) {
                log.warn("Handler shutdown took too long, calling shutdownNow");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("Handler shutdown interrupted");
            executor.shutdownNow();
        }
    }
}
