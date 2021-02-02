package net.mikeyt.logic;

public class QueueOptions {
    public static int CONCURRENT_MESSAGE_HANDLERS = 8;
    public static long GRACEFUL_TERMINATION_SECONDS = 15;
    public static final int QUEUE_POLLING_TIMEOUT_SECONDS = 3;
    public static final int SHUTDOWN_SAFE_TIMEOUT_SECONDS = 5;
}
