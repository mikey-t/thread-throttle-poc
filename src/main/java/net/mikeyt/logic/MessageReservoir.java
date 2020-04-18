package net.mikeyt.logic;

import lombok.SneakyThrows;
import net.mikeyt.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

public class MessageReservoir implements Runnable, IMessageReservoir {
    private static final Logger log = LoggerFactory.getLogger(MessageReservoir.class);
    public static final int RESERVOIR_TIMEOUT_SECONDS = 2;
    public static final int RESERVOIR_SOFT_MAX = 20;
    public static final int REQUEST_MAX = 10;

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
        executor.scheduleWithFixedDelay(new Thread(this::runAsync), 0, RESERVOIR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @SneakyThrows
    @Override
    public Message getNextMessage() {
        // Polling is used so processes have an opportunity to gracefully shutdown
        return reservoirQueue.poll(QueueOptions.QUEUE_POLLING_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
            log.info("Reservoir is still full at beginning of new fill loop, waiting " + RESERVOIR_TIMEOUT_SECONDS + " seconds to request more messages");
            return;
        }

        log.info("Filling message reservoir until full or until no messages are returned");
        while (true) {
            List<Message> newMessages = messageProvider.getMessages(REQUEST_MAX);
            if (newMessages.size() == 0) {
                log.info("No messages returned from provider, exiting fill loop and waiting " + RESERVOIR_TIMEOUT_SECONDS + " seconds");
                return;
            }

            reservoirQueue.addAll(newMessages);

            if (isFull()) {
                log.info("Message reservoir full (remaining capacity less than REQUEST_MAX), exiting fill loop");
                return;
            }
        }
    }

    private boolean isFull() {
        log.info("reservoirQueue.remainingCapacity(): " + reservoirQueue.remainingCapacity());
        return reservoirQueue.remainingCapacity() < REQUEST_MAX;
    }
}
