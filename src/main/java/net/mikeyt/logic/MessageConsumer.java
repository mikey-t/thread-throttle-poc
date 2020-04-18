package net.mikeyt.logic;

import lombok.SneakyThrows;
import net.mikeyt.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class MessageConsumer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);

    private final BlockingQueue<Message> messageQueue;
    private final ProcessShutdownState shutdownState;
    private final ExecutorService executor;
    private final Semaphore semaphore = new Semaphore(QueueOptions.CONCURRENT_MESSAGE_HANDLERS);

    public MessageConsumer(BlockingQueue<Message> messageQueue, ProcessShutdownState shutdownState) {
        this.messageQueue = messageQueue;
        this.shutdownState = shutdownState;
        executor = Executors.newFixedThreadPool(QueueOptions.CONCURRENT_MESSAGE_HANDLERS);
    }

    @SneakyThrows
    @Override
    public void run() {
        try {
            while (true) {
                if (shutdownState.isShutdown()) {
                    shutdown();
                    return;
                }

                // Polling is used so processes have an opportunity to gracefully shutdown
                Message message = messageQueue.poll(QueueOptions.QUEUE_POLLING_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (shutdownState.isShutdown() || message == null) {
                    continue;
                }

                boolean acquired;
                do {
                    if (shutdownState.isShutdown()) {
                        shutdown();
                        return;
                    }

                    acquired = semaphore.tryAcquire(QueueOptions.SHUTDOWN_SAFE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } while (!acquired);

                CompletableFuture.runAsync(new MessageHandler(message, shutdownState), executor)
                        .whenCompleteAsync((v, t) -> semaphore.release(), executor);
            }
        } catch (InterruptedException ex) {
            log.info("MessageConsumer thread interrupted");
        }
    }

    private void shutdown() {
        log.warn("MessageConsumer shutting down and attempting to shutdown handlers");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(QueueOptions.SHUTDOWN_SAFE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("Handler shutdown took too long, calling shutdownNow");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("Handler shutdown interrupted");
            executor.shutdownNow();
        }
    }
}
