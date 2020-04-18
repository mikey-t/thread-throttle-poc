package net.mikeyt.logic;

import net.mikeyt.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class MessageProducer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MessageProducer.class);

    private final BlockingQueue<Message> messageQueue;
    private final MessageReservoir reservoir;
    private final ProcessShutdownState shutdownState;

    public MessageProducer(BlockingQueue<Message> messageQueue, MessageReservoir reservoir, ProcessShutdownState shutdownState) {
        this.messageQueue = messageQueue;
        this.reservoir = reservoir;
        this.shutdownState = shutdownState;
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (shutdownState.isShutdown()) {
                    shutdown();
                    return;
                }

                // Note the reservoir.getNextMessage method is doing polling and returning null when none available within timeout period
                Message message = reservoir.getNextMessage();
                if (message != null) {
                    boolean putSuccess;
                    do {
                        if (shutdownState.isShutdown()) {
                            shutdown();
                            return;
                        }
                        putSuccess = messageQueue.offer(message, QueueOptions.SHUTDOWN_SAFE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } while (!putSuccess);
                }
            }
        } catch (InterruptedException ex) {
            log.info("MessageProducer thread interrupted");
        }
    }

    private void shutdown() {
        log.info("MessageProducer shutting down");
    }
}
