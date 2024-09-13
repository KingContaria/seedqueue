package me.contaria.seedqueue.debug;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.mixin.accessor.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;

import java.util.Arrays;

public class SeedQueueWatchdog {
    private static final Object LOCK = new Object();
    private static volatile Thread watchdog = null;

    public static void start() {
        if (!SeedQueue.isActive()) {
            throw new IllegalStateException("Tried to stop SeedQueue Watchdog while SeedQueue isn't running!");
        }
        if (SeedQueue.config.useWatchdog && watchdog == null) {
            watchdog = createWatchdog();
        }
    }

    public static void stop() {
        if (SeedQueue.isActive()) {
            throw new IllegalStateException("Tried to stop SeedQueue Watchdog while SeedQueue is still running!");
        }
        if (watchdog == null) {
            return;
        }
        synchronized (LOCK) {
            LOCK.notify();
        }
        try {
            watchdog.join();
        } catch (InterruptedException e) {
            SeedQueue.LOGGER.warn("SeedQueue Watchdog was interrupted while stopping!");
        }
        watchdog = null;
    }

    private static Thread createWatchdog() {
        Thread thread = new Thread(() -> {
            try {
                while (SeedQueue.isActive()) {
                    synchronized (LOCK) {
                        Thread sqThread = SeedQueue.getThread();
                        if (sqThread != null) {
                            SeedQueue.LOGGER.info("WATCHDOG | Main: {}", Arrays.toString(((MinecraftClientAccessor) MinecraftClient.getInstance()).seedQueue$getThread().getStackTrace()));
                            SeedQueue.LOGGER.info("WATCHDOG | SeedQueue: {}", Arrays.toString(sqThread.getStackTrace()));
                        }
                        LOCK.wait(10000);
                    }
                }
            } catch (InterruptedException e) {
                SeedQueue.LOGGER.warn("SeedQueue Watchdog was interrupted while running!");
            }
        });
        thread.setDaemon(true);
        thread.setPriority(3);
        thread.setName("SeedQueue Watchdog");
        thread.start();
        return thread;
    }
}
