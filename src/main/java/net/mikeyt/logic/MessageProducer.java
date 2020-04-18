package net.mikeyt.logic;

import net.mikeyt.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

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
                    log.info("MessageProducer shutting down");
                    return;
                }
                // Polling is used so processes have an opportunity to gracefully shutdown
                Message message = reservoir.getNextMessage();
                if (message != null) {
                    messageQueue.put(reservoir.getNextMessage());
                }
            }
        } catch (InterruptedException ex) {
            log.info("MessageProducer thread interrupted");
        }
    }
}
