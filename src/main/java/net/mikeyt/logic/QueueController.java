package net.mikeyt.logic;

import net.mikeyt.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class QueueController extends Thread {
    private static final Logger log = LoggerFactory.getLogger(QueueController.class);

    private final ProcessShutdownState shutdownState = new ProcessShutdownState();
    private final ExecutorService executor;

    public QueueController() {
        super("queue-controller");
        executor = Executors.newFixedThreadPool(3);
        addShutdownHook();
    }

    @Override
    public void run() {
        BlockingQueue<Message> queue = new LinkedBlockingQueue<>(QueueOptions.CONCURRENT_MESSAGE_HANDLERS);

        MessageReservoir reservoir = new MessageReservoir(new MessageProviderMock(), shutdownState);

        executor.submit(reservoir);
        executor.submit(new MessageProducer(queue, reservoir, shutdownState));
        executor.submit(new MessageConsumer(queue, shutdownState));
    }

    private void shutdownAndAwaitTermination() {
        log.info("Shutdown called, attempting to shutdown processes gracefully before interrupting");
        executor.shutdown();
        shutdownState.setShutdown(true);
        try {
            if (!executor.awaitTermination(QueueOptions.GRACEFUL_TERMINATION_SECONDS, TimeUnit.SECONDS)) {
                log.warn("Waited " + QueueOptions.GRACEFUL_TERMINATION_SECONDS + " seconds but processes still running - interrupting them now");
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownAndAwaitTermination));
    }
}
