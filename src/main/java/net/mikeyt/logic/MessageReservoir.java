package net.mikeyt.logic;

import lombok.SneakyThrows;
import net.mikeyt.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

public class MessageReservoir implements Runnable, IMessageReservoir {
    private static Logger log = LoggerFactory.getLogger(MessageReservoir.class);
    private static final int QUEUE_POLLING_TIMEOUT_SECONDS = 3;
    private static final int TIMEOUT_SECONDS = 2;
    private static final int RESERVOIR_SOFT_MAX = 20;
    private static final int REQUEST_MAX = 10;

    private final MessageProviderMock messageProvider;
    private final ProcessShutdownState shutdownState;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final BlockingQueue<Message> reservoirQueue = new LinkedBlockingQueue<>(RESERVOIR_SOFT_MAX + REQUEST_MAX);

    public MessageReservoir(MessageProviderMock messageProviderMock, ProcessShutdownState shutdownState) {
        this.messageProvider = messageProviderMock;
        this.shutdownState = shutdownState;
    }

    @Override
    public void run() {
        executor.scheduleWithFixedDelay(new Thread(() -> runAsync()), 0, TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @SneakyThrows
    @Override
    public Message getNextMessage() {
        // Polling is used so processes have an opportunity to gracefully shutdown
        return reservoirQueue.poll(QUEUE_POLLING_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void runAsync() {
        try {
            if (shutdownState.isShutdown()) {
                log.info("MessageReservoir shutting down");
                executor.shutdown();
                return;
            }
            fillUntilFull();
        } catch (Exception ex) {
            log.info("MessageReservoir thread interrupted");
        }
    }

    private void fillUntilFull() {
        if (isFull()) {
            log.info("Reservoir is full, waiting " + TIMEOUT_SECONDS + " seconds to request more messages");
            return;
        }

        log.info("Filling message reservoir until full or until no messages are returned");
        while (true) {
            List<Message> newMessages = messageProvider.getMessages(REQUEST_MAX);
            if (newMessages.size() == 0) {
                log.info("No messages returned from provider, exiting fillUntilFull loop and waiting " + TIMEOUT_SECONDS + " seconds");
                return;
            }
            reservoirQueue.addAll(newMessages);
            log.info("reservoirQueue.remainingCapacity: " + reservoirQueue.remainingCapacity());
            if (reservoirQueue.remainingCapacity() < REQUEST_MAX) {
                log.info("Message reservoir full");
                return;
            }
        }
    }

    private boolean isFull() {
        return reservoirQueue.remainingCapacity() < REQUEST_MAX;
    }
}
