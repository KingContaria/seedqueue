package me.contaria.seedqueue;

/**
 * Exception thrown when something goes wrong on the {@link SeedQueueThread}.
 */
public class SeedQueueException extends RuntimeException {
    public SeedQueueException(String message) {
        super(message);
    }

    public SeedQueueException(String message, Throwable cause) {
        super(message, cause);
    }
}
